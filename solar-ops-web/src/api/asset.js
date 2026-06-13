import request from '../utils/request'

export function getAssetList(params) {
  return request({
    url: '/api/assets',
    method: 'get',
    params
  })
}

export function getAssetDetail(id) {
  return request({
    url: `/api/assets/${id}`,
    method: 'get'
  })
}

export function addAsset(data) {
  return request({
    url: '/api/assets',
    method: 'post',
    data
  })
}

export function updateAsset(data) {
  return request({
    url: '/api/assets',
    method: 'put',
    data
  })
}

export function deleteAsset(id) {
  return request({
    url: `/api/assets/${id}`,
    method: 'delete'
  })
}

export function deleteBatchAssets(ids) {
  return request({
    url: '/api/assets/batch',
    method: 'delete',
    data: { ids }
  })
}

export function retireAsset(id) {
  return request({
    url: `/api/assets/${id}/retire`,
    method: 'put'
  })
}

export function scrapAsset(id) {
  return request({
    url: `/api/assets/${id}/scrap`,
    method: 'put'
  })
}

export function getAssetQrCode(id) {
  return request({
    url: `/api/assets/${id}/qrcode`,
    method: 'get'
  })
}

export function batchGenerateQrCode(ids) {
  return request({
    url: '/api/assets/qrcode/batch',
    method: 'post',
    data: { ids }
  })
}

export function exportAssets(params) {
  return request({
    url: '/api/assets/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function getAssetByCode(code) {
  return request({
    url: `/api/assets/code/${code}`,
    method: 'get'
  })
}

export function importAsset(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/api/assets/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function scanAsset(code) {
  return request({
    url: `/api/assets/scan/${code}`,
    method: 'get'
  })
}
