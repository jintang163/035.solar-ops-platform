import { get, post } from './request'

export function getInspectionTaskPage(data) {
  return post('/drone/task/page', data)
}

export function getInspectionTaskDetail(id) {
  return get(`/drone/task/${id}`)
}

export function getInspectionStatistics(params) {
  return get('/drone/statistics', params)
}

export function getDefectPage(data) {
  return post('/drone/defect/page', data)
}

export function getDefectDetail(id) {
  return get(`/drone/defect/${id}`)
}

export function confirmDefect(data) {
  return post('/drone/defect/confirm', data)
}

export function generateWorkOrder(data) {
  return post('/drone/defect/generate-work-order', data)
}

export function getImageList(data) {
  return post('/drone/image/page', data)
}

export function getImageDetail(id) {
  return get(`/drone/image/${id}`)
}

export function getDefectsByImage(imageId) {
  return get(`/drone/defect/image/${imageId}`)
}
