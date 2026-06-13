import { get, post, put } from './request'

export function getAssetList(params) {
  return get('/assets', params)
}

export function getAssetDetail(id) {
  return get(`/assets/${id}`)
}

export function getAssetByScan(code) {
  return get(`/assets/scan/${code}`)
}

export function addMaintenanceRecord(data) {
  return post('/maintenance-records', data)
}

export function addMaintenanceWithSpareParts(data) {
  return post('/maintenance-records/with-spare-parts', data)
}

export function getMaintenanceRecords(assetId) {
  return get('/maintenance-records', { assetId })
}

export function getMaintenanceDetail(id) {
  return get(`/maintenance-records/${id}`)
}

export function retireAsset(id) {
  return put(`/assets/${id}/retire`)
}

export function scrapAsset(id) {
  return put(`/assets/${id}/scrap`)
}

export function uploadFile(filePath) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.uploadFile({
      url: '/api/upload',
      filePath: filePath,
      name: 'file',
      header: {
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          if (data.code === 200 || data.code === 0) {
            resolve(data.data)
          } else {
            reject(new Error(data.message || '上传失败'))
          }
        } catch (e) {
          reject(e)
        }
      },
      fail: reject
    })
  })
}
