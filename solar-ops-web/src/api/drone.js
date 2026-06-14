import request from '../utils/request'

export function getDroneTaskPage(data) {
  return request({
    url: '/api/drone/task/page',
    method: 'post',
    data
  })
}

export function getDroneTaskDetail(id) {
  return request({
    url: `/api/drone/task/${id}`,
    method: 'get'
  })
}

export function createDroneTask(data) {
  return request({
    url: '/api/drone/task/create',
    method: 'post',
    data
  })
}

export function updateDroneTask(data) {
  return request({
    url: '/api/drone/task/update',
    method: 'post',
    data
  })
}

export function deleteDroneTask(id) {
  return request({
    url: `/api/drone/task/delete/${id}`,
    method: 'post'
  })
}

export function getDroneTaskStatistics(params) {
  return request({
    url: '/api/drone/task/statistics',
    method: 'get',
    params
  })
}

export function getDroneImagePage(data) {
  return request({
    url: '/api/drone/image/page',
    method: 'post',
    data
  })
}

export function getDroneImageDetail(id) {
  return request({
    url: `/api/drone/image/${id}`,
    method: 'get'
  })
}

export function uploadDroneImage(taskId, file, imageType) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('taskId', taskId)
  if (imageType) {
    formData.append('imageType', imageType)
  }
  return request({
    url: '/api/drone/image/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function batchUploadDroneImages(taskId, files, imageType) {
  const formData = new FormData()
  formData.append('taskId', taskId)
  if (imageType) {
    formData.append('imageType', imageType)
  }
  files.forEach(file => {
    formData.append('files', file)
  })
  return request({
    url: '/api/drone/image/batchUpload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function triggerImageDetection(imageId) {
  return request({
    url: `/api/drone/image/${imageId}/detect`,
    method: 'post'
  })
}

export function triggerBatchDetection(taskId) {
  return request({
    url: `/api/drone/task/${taskId}/detectAll`,
    method: 'post'
  })
}

export function deleteDroneImage(id) {
  return request({
    url: `/api/drone/image/delete/${id}`,
    method: 'post'
  })
}

export function getImageDefects(imageId) {
  return request({
    url: `/api/drone/defect/image/${imageId}`,
    method: 'get'
  })
}

export function getTaskDefects(taskId) {
  return request({
    url: `/api/drone/defect/task/${taskId}`,
    method: 'get'
  })
}

export function getDroneDefectPage(data) {
  return request({
    url: '/api/drone/defect/page',
    method: 'post',
    data
  })
}

export function getDroneDefectDetail(id) {
  return request({
    url: `/api/drone/defect/${id}`,
    method: 'get'
  })
}

export function confirmDroneDefect(id, confirmed, data) {
  return request({
    url: '/api/drone/defect/confirm',
    method: 'post',
    data: {
      id,
      confirmed,
      ...data
    }
  })
}

export function batchConfirmDroneDefects(data) {
  return request({
    url: '/api/drone/defect/batchConfirm',
    method: 'post',
    data
  })
}

export function createWorkOrderFromDefect(defectId, handlerId, handlerName) {
  return request({
    url: '/api/drone/defect/generate-work-order',
    method: 'post',
    data: {
      id: defectId,
      handlerId,
      handlerName
    }
  })
}

export function getDroneDefectStatistics(params) {
  return request({
    url: '/api/drone/defect/statistics',
    method: 'get',
    params
  })
}

export function getTaskImages(taskId) {
  return request({
    url: `/api/drone/image/task/${taskId}`,
    method: 'get'
  })
}
