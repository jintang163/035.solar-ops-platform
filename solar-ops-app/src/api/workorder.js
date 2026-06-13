import { get, post } from './request'

export function getWorkOrderPage(data) {
  return post('/workorder/page', data)
}

export function getWorkOrderDetail(id) {
  return get(`/workorder/${id}`)
}

export function getWorkOrderStatistics(params) {
  return get('/workorder/statistics', params)
}

export function createWorkOrder(data) {
  return post('/workorder/create', data)
}

export function acceptWorkOrder(data) {
  return post('/workorder/accept', data)
}

export function assignWorkOrder(data) {
  return post('/workorder/assign', data)
}

export function startProcessWorkOrder(data) {
  return post('/workorder/startProcess', data)
}

export function submitCheckWorkOrder(data) {
  return post('/workorder/submitCheck', data)
}

export function completeWorkOrder(data) {
  return post('/workorder/complete', data)
}

export function closeWorkOrder(data) {
  return post('/workorder/close', data)
}
