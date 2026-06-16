const STORAGE_KEY = 'offline_map_tiles'
const TILE_DIR = '_doc/map_tiles/'

const tileLevels = {}

const tileStorage = {
  saveTileInfo(stationId, info) {
    const all = this.getAllTileInfo()
    all[stationId] = {
      ...info,
      stationId,
      updateTime: new Date().toISOString()
    }
    uni.setStorageSync(STORAGE_KEY, JSON.stringify(all))
  },

  getTileInfo(stationId) {
    const all = this.getAllTileInfo()
    return all[stationId] || null
  },

  getAllTileInfo() {
    try {
      const data = uni.getStorageSync(STORAGE_KEY)
      return data ? JSON.parse(data) : {}
    } catch (e) {
      return {}
    }
  },

  removeTileInfo(stationId) {
    const all = this.getAllTileInfo()
    delete all[stationId]
    uni.setStorageSync(STORAGE_KEY, JSON.stringify(all))
  }
}

export const mapTileUtil = {
  async downloadStationTiles(stationId, stationName, bounds, options = {}) {
    const {
      minZoom = 10,
      maxZoom = 16,
      onProgress,
      tileUrlTemplate = 'https://webrd0{1-4}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}'
    } = options

    return new Promise(async (resolve, reject) => {
      try {
        const tasks = []
        let totalTiles = 0
        let completedTiles = 0

        for (let z = minZoom; z <= maxZoom; z++) {
          const tileRange = this.lngLatBoundsToTileRange(bounds, z)
          for (let x = tileRange.minX; x <= tileRange.maxX; x++) {
            for (let y = tileRange.minY; y <= tileRange.maxY; y++) {
              totalTiles++
              tasks.push({ x, y, z })
            }
          }
        }

        if (totalTiles === 0) {
          reject(new Error('瓦片数量为0，请检查经纬度范围'))
          return
        }

        const stationDir = `${TILE_DIR}${stationId}/`
        await this.ensureDir(stationDir)

        let hasError = false
        let errorMsg = ''

        for (const task of tasks) {
          if (hasError) break

          try {
            const tileUrl = tileUrlTemplate
              .replace('{x}', task.x)
              .replace('{y}', task.y)
              .replace('{z}', task.z)
              .replace('{1-4}', ((task.x + task.y) % 4) + 1)

            const filePath = `${stationDir}${task.z}_${task.x}_${task.y}.png`
            await this.downloadTile(tileUrl, filePath)

            completedTiles++
            if (onProgress) {
              onProgress({
                stationId,
                current: completedTiles,
                total: totalTiles,
                percent: Math.round((completedTiles / totalTiles) * 100)
              })
            }
          } catch (tileErr) {
            console.warn(`瓦片下载失败 ${task.z}/${task.x}/${task.y}`, tileErr)
          }
        }

        if (hasError) {
          reject(new Error(errorMsg || '瓦片下载失败'))
          return
        }

        tileStorage.saveTileInfo(stationId, {
          stationName,
          bounds,
          minZoom,
          maxZoom,
          totalTiles,
          tileUrlTemplate,
          dirPath: stationDir
        })

        resolve({
          stationId,
          totalTiles,
          completedTiles
        })
      } catch (e) {
        reject(e)
      }
    })
  },

  downloadTile(url, filePath) {
    return new Promise((resolve, reject) => {
      // #ifdef APP-PLUS
      uni.downloadFile({
        url,
        success: (res) => {
          if (res.statusCode === 200) {
            plus.io.requestFileSystem(plus.io.PRIVATE_DOC, (fs) => {
              const relativePath = filePath.replace('_doc/', '')
              const dirPath = relativePath.substring(0, relativePath.lastIndexOf('/'))

              fs.root.getDirectory(dirPath, { create: true }, (dirEntry) => {
                plus.io.resolveLocalFileSystemURL(res.tempFilePath, (tempEntry) => {
                  const fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1)
                  tempEntry.copyTo(dirEntry, fileName, (newEntry) => {
                    resolve(newEntry.fullPath)
                  }, reject)
                }, reject)
              }, reject)
            }, reject)
          } else {
            reject(new Error(`下载失败: HTTP ${res.statusCode}`))
          }
        },
        fail: reject
      })
      // #endif

      // #ifndef APP-PLUS
      setTimeout(() => resolve(filePath), 50)
      // #endif
    })
  },

  ensureDir(dirPath) {
    return new Promise((resolve, reject) => {
      // #ifdef APP-PLUS
      plus.io.requestFileSystem(plus.io.PRIVATE_DOC, (fs) => {
        const relativePath = dirPath.replace('_doc/', '')
        fs.root.getDirectory(relativePath, { create: true }, () => {
          resolve()
        }, reject)
      }, reject)
      // #endif

      // #ifndef APP-PLUS
      resolve()
      // #endif
    })
  },

  getLocalTilePath(stationId, x, y, z) {
    const tileInfo = tileStorage.getTileInfo(stationId)
    if (!tileInfo) return null

    const filePath = `${TILE_DIR}${stationId}/${z}_${x}_${y}.png`
    return this.fileExists(filePath).then((exists) => {
      return exists ? filePath : null
    })
  },

  fileExists(filePath) {
    return new Promise((resolve) => {
      // #ifdef APP-PLUS
      plus.io.requestFileSystem(plus.io.PRIVATE_DOC, (fs) => {
        const relativePath = filePath.replace('_doc/', '')
        fs.root.getFile(relativePath, { create: false }, () => {
          resolve(true)
        }, () => {
          resolve(false)
        })
      }, () => {
        resolve(false)
      })
      // #endif

      // #ifndef APP-PLUS
      resolve(false)
      // #endif
    })
  },

  getTileUrl(stationId, x, y, z) {
    const tileInfo = tileStorage.getTileInfo(stationId)
    if (!tileInfo) return null

    // #ifdef APP-PLUS
    try {
      const relativePath = `map_tiles/${stationId}/${z}_${x}_${y}.png`
      return plus.io.convertLocalFileSystemURL(`_doc/${relativePath}`)
    } catch (e) {
      return null
    }
    // #endif

    // #ifndef APP-PLUS
    return null
    // #endif
  },

  getTilePath(stationId, x, y, z) {
    return `_doc/map_tiles/${stationId}/${z}_${x}_${y}.png`
  },

  hasOfflineTiles(stationId) {
    return !!tileStorage.getTileInfo(stationId)
  },

  isTileInRange(stationId, x, y, z) {
    const tileInfo = tileStorage.getTileInfo(stationId)
    if (!tileInfo) return false
    if (z < tileInfo.minZoom || z > tileInfo.maxZoom) return false
    const range = this.lngLatBoundsToTileRange(tileInfo.bounds, z)
    return x >= range.minX && x <= range.maxX && y >= range.minY && y <= range.maxY
  },

  getAllOfflineStations() {
    return Object.values(tileStorage.getAllTileInfo())
  },

  async deleteStationTiles(stationId) {
    tileStorage.removeTileInfo(stationId)
    // #ifdef APP-PLUS
    return new Promise((resolve) => {
      plus.io.requestFileSystem(plus.io.PRIVATE_DOC, (fs) => {
        fs.root.getDirectory(`map_tiles/${stationId}`, { create: false }, (dirEntry) => {
          dirEntry.removeRecursively(() => resolve(true), () => resolve(false))
        }, () => resolve(false))
      }, () => resolve(false))
    })
    // #endif
    // #ifndef APP-PLUS
    return true
    // #endif
  },

  lngLatBoundsToTileRange(bounds, zoom) {
    const { west, south, east, north } = bounds
    const n = Math.pow(2, zoom)

    const minX = Math.max(0, Math.floor(((west + 180) / 360) * n))
    const maxX = Math.min(n - 1, Math.floor(((east + 180) / 360) * n))

    const northRad = (north * Math.PI) / 180
    const southRad = (south * Math.PI) / 180

    const minY = Math.max(0, Math.floor(
      (1 - Math.log(Math.tan(northRad) + 1 / Math.cos(northRad)) / Math.PI) / 2 * n
    ))
    const maxY = Math.min(n - 1, Math.floor(
      (1 - Math.log(Math.tan(southRad) + 1 / Math.cos(southRad)) / Math.PI) / 2 * n
    ))

    return {
      minX,
      maxX,
      minY: Math.min(minY, maxY),
      maxY: Math.max(minY, maxY)
    }
  },

  async calculateTileCount(bounds, minZoom = 10, maxZoom = 16) {
    let total = 0
    for (let z = minZoom; z <= maxZoom; z++) {
      const range = this.lngLatBoundsToTileRange(bounds, z)
      const xCount = range.maxX - range.minX + 1
      const yCount = range.maxY - range.minY + 1
      total += xCount * yCount
    }
    return total
  }
}

export default mapTileUtil
