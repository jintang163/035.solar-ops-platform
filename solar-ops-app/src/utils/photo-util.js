export const photoUtil = {
  takePhoto(options = {}) {
    return new Promise((resolve, reject) => {
      const {
        sourceType = ['camera', 'album'],
        sizeType = ['compressed'],
        camera = 'back',
        count = 1
      } = options

      uni.chooseImage({
        count,
        sourceType,
        sizeType,
        camera,
        success: (res) => {
          resolve(res.tempFilePaths)
        },
        fail: (err) => {
          if (err.errMsg && err.errMsg.includes('cancel')) {
            resolve([])
          } else {
            reject(err)
          }
        }
      })
    })
  },

  takeCameraPhoto(options = {}) {
    return this.takePhoto({
      ...options,
      sourceType: ['camera']
    })
  },

  compressImage(imagePath, quality = 80, maxWidth = 1280, maxHeight = 1280) {
    return new Promise((resolve, reject) => {
      // #ifdef APP-PLUS || H5
      uni.getImageInfo({
        src: imagePath,
        success: (info) => {
          let width = info.width
          let height = info.height
          let scale = 1

          if (width > maxWidth || height > maxHeight) {
            const scaleW = maxWidth / width
            const scaleH = maxHeight / height
            scale = Math.min(scaleW, scaleH)
            width = Math.floor(width * scale)
            height = Math.floor(height * scale)
          }

          const canvasId = 'compress_canvas_' + Date.now()
          const ctx = uni.createCanvasContext(canvasId)
          
          ctx.drawImage(imagePath, 0, 0, width, height)
          ctx.draw(false, () => {
            uni.canvasToTempFilePath({
              canvasId,
              width,
              height,
              destWidth: width,
              destHeight: height,
              quality: quality / 100,
              success: (res) => {
                resolve({
                  tempFilePath: res.tempFilePath,
                  width,
                  height
                })
              },
              fail: reject
            })
          })
        },
        fail: reject
      })
      // #endif

      // #ifdef MP-WEIXIN
      uni.compressImage({
        src: imagePath,
        quality: quality,
        success: (res) => {
          resolve({
            tempFilePath: res.tempFilePath
          })
        },
        fail: reject
      })
      // #endif
    })
  },

  addWatermark(imagePath, options = {}) {
    return new Promise((resolve, reject) => {
      const {
        timestamp = new Date(),
        longitude = null,
        latitude = null,
        locationText = '',
        extraText = '',
        position = 'bottom',
        fontSize = 24,
        textColor = '#FFFFFF',
        bgColor = 'rgba(0, 0, 0, 0.5)'
      } = options

      uni.getImageInfo({
        src: imagePath,
        success: (info) => {
          const width = info.width
          const height = info.height
          const canvasId = 'watermark_canvas_' + Date.now()
          const ctx = uni.createCanvasContext(canvasId)

          ctx.drawImage(imagePath, 0, 0, width, height)

          const timeStr = this.formatDateTime(timestamp)
          const locationStr = longitude && latitude 
            ? `经度: ${longitude.toFixed(6)} 纬度: ${latitude.toFixed(6)}` 
            : (locationText || '')

          const lines = []
          lines.push(timeStr)
          if (locationStr) lines.push(locationStr)
          if (extraText) lines.push(extraText)

          const lineHeight = fontSize + 10
          const padding = 20
          const textWidth = Math.max(...lines.map(line => this.measureText(line, fontSize)))
          const bgHeight = lineHeight * lines.length + padding * 2
          const bgWidth = textWidth + padding * 2

          let bgY = 0
          let textY = 0

          if (position === 'top') {
            bgY = 20
            textY = 20 + padding + fontSize
          } else if (position === 'center') {
            bgY = (height - bgHeight) / 2
            textY = bgY + padding + fontSize
          } else {
            bgY = height - bgHeight - 20
            textY = bgY + padding + fontSize
          }

          ctx.setFillStyle(bgColor)
          ctx.fillRect(20, bgY, bgWidth, bgHeight)

          ctx.setFillStyle(textColor)
          ctx.setFontSize(fontSize)
          ctx.setTextBaseline('top')

          lines.forEach((line, index) => {
            ctx.fillText(line, 20 + padding, textY + index * lineHeight)
          })

          ctx.draw(false, () => {
            uni.canvasToTempFilePath({
              canvasId,
              width,
              height,
              destWidth: width,
              destHeight: height,
              quality: 0.9,
              success: (res) => {
                resolve({
                  tempFilePath: res.tempFilePath,
                  width,
                  height,
                  timestamp,
                  longitude,
                  latitude
                })
              },
              fail: reject
            })
          })
        },
        fail: reject
      })
    })
  },

  measureText(text, fontSize) {
    return text.length * fontSize * 0.6
  },

  formatDateTime(date) {
    const d = new Date(date)
    const pad = (n) => n.toString().padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  },

  getLocation() {
    return new Promise((resolve, reject) => {
      uni.getLocation({
        type: 'gcj02',
        success: (res) => {
          resolve({
            longitude: res.longitude,
            latitude: res.latitude,
            accuracy: res.accuracy,
            speed: res.speed
          })
        },
        fail: (err) => {
          console.warn('获取位置失败', err)
          resolve(null)
        }
      })
    })
  },

  async takeInspectionPhoto(options = {}) {
    const {
      photoType = 1,
      addWatermark = true,
      compress = true,
      maxWidth = 1280,
      quality = 80
    } = options

    try {
      const photos = await this.takeCameraPhoto({ count: 1 })
      if (photos.length === 0) {
        return null
      }

      let photoPath = photos[0]
      let compressedPath = photoPath
      let width = 0
      let height = 0

      if (compress) {
        const compressResult = await this.compressImage(photoPath, quality, maxWidth, maxWidth)
        compressedPath = compressResult.tempFilePath
        width = compressResult.width
        height = compressResult.height
      }

      const timestamp = new Date()
      let location = null
      
      try {
        location = await this.getLocation()
      } catch (e) {
        console.warn('获取位置失败，继续拍照', e)
      }

      let finalPath = compressedPath
      let watermarkInfo = null

      if (addWatermark) {
        const watermarkResult = await this.addWatermark(compressedPath, {
          timestamp,
          longitude: location ? location.longitude : null,
          latitude: location ? location.latitude : null,
          position: 'bottom',
          fontSize: 28
        })
        finalPath = watermarkResult.tempFilePath
        watermarkInfo = {
          timestamp,
          longitude: location ? location.longitude : null,
          latitude: location ? location.latitude : null
        }
      }

      const fileSize = await this.getFileSize(finalPath)

      return {
        photoPath: finalPath,
        originalPath: photoPath,
        compressedPath,
        width,
        height,
        fileSize,
        photoType,
        hasWatermark: addWatermark ? 1 : 0,
        watermarkTime: timestamp,
        longitude: location ? location.longitude : null,
        latitude: location ? location.latitude : null
      }
    } catch (e) {
      console.error('拍照失败', e)
      throw e
    }
  },

  getFileSize(filePath) {
    return new Promise((resolve) => {
      // #ifdef APP-PLUS
      plus.io.requestFileSystem(plus.io.PRIVATE_DOC, (fs) => {
        fs.root.getFile(filePath.replace('_doc/', ''), { create: false }, (entry) => {
          entry.getMetadata((meta) => {
            resolve(meta.size)
          }, () => {
            resolve(0)
          })
        }, () => {
          resolve(0)
        })
      }, () => {
        resolve(0)
      })
      // #endif
      
      // #ifndef APP-PLUS
      resolve(0)
      // #endif
    })
  },

  savePhotoToLocal(photoPath, taskId) {
    return new Promise((resolve, reject) => {
      // #ifdef APP-PLUS
      const targetDir = `_doc/inspection/${taskId}/`
      const fileName = `photo_${Date.now()}.jpg`
      const targetPath = targetDir + fileName

      plus.io.requestFileSystem(plus.io.PRIVATE_DOC, (fs) => {
        fs.root.getDirectory(`inspection/${taskId}`, { create: true }, () => {
          plus.io.resolveLocalFileSystemURL(photoPath, (entry) => {
            entry.copyTo(fs.root, `inspection/${taskId}/${fileName}`, (newEntry) => {
              resolve(newEntry.fullPath)
            }, reject)
          }, reject)
        }, reject)
      }, reject)
      // #endif

      // #ifndef APP-PLUS
      resolve(photoPath)
      // #endif
    })
  }
}

export default photoUtil
