import { get, post, put } from './request'

export function getWorkOrderList(params) {
  return get('/workorder/list', params)
}

export function getWorkOrderDetail(id) {
  return get(`/workorder/${id}`)
}

export function getWorkOrderStats() {
  return get('/workorder/stats')
}

export function grabOrder(id) {
  return post(`/workorder/${id}/grab`)
}

export function acceptOrder(id) {
  return post(`/workorder/${id}/accept`)
}

export function submitHandle(data) {
  return put('/workorder/handle', data)
}

export function createWorkOrder(data) {
  return post('/workorder/create', data)
}
