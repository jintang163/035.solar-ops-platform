import request from '../utils/request'

export function getMaintenanceList(params) {
  return request({
    url: '/api/maintenance-records',
    method: 'get',
    params
  })
}

export function getMaintenanceListByAssetId(assetId) {
  return request({
    url: `/api/maintenance-records/asset/${assetId}`,
    method: 'get'
  })
}

export function getMaintenanceDetail(id) {
  return request({
    url: `/api/maintenance-records/${id}`,
    method: 'get'
  })
}

export function addMaintenance(data) {
  return request({
    url: '/api/maintenance-records',
    method: 'post',
    data
  })
}

export function addMaintenanceWithSpareParts(data) {
  return request({
    url: '/api/maintenance-records/with-spare-parts',
    method: 'post',
    data
  })
}

export function updateMaintenance(data) {
  return request({
    url: '/api/maintenance-records',
    method: 'put',
    data
  })
}

export function deleteMaintenance(id) {
  return request({
    url: `/api/maintenance-records/${id}`,
    method: 'delete'
  })
}

export function completeMaintenance(id) {
  return request({
    url: `/api/maintenance-records/${id}/complete`,
    method: 'put'
  })
}

export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/api/files/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function batchUploadFiles(files) {
  const formData = new FormData()
  files.forEach(file => {
    formData.append('files', file)
  })
  return request({
    url: '/api/files/batch-upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
