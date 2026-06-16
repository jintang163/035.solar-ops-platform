import request from '../utils/request'

export function getWorkOrderPage(data) {
  return request({
    url: '/api/workorder/page',
    method: 'post',
    data
  })
}

export function getWorkOrderDetail(id) {
  return request({
    url: `/api/workorder/${id}`,
    method: 'get'
  })
}

export function createWorkOrder(data) {
  return request({
    url: '/api/workorder/create',
    method: 'post',
    data
  })
}

export function acceptWorkOrder(data) {
  return request({
    url: '/api/workorder/accept',
    method: 'post',
    data
  })
}

export function assignWorkOrder(data) {
  return request({
    url: '/api/workorder/assign',
    method: 'post',
    data
  })
}

export function startProcessWorkOrder(data) {
  return request({
    url: '/api/workorder/startProcess',
    method: 'post',
    data
  })
}

export function submitCheckWorkOrder(data) {
  return request({
    url: '/api/workorder/submitCheck',
    method: 'post',
    data
  })
}

export function completeWorkOrder(data) {
  return request({
    url: '/api/workorder/complete',
    method: 'post',
    data
  })
}

export function closeWorkOrder(data) {
  return request({
    url: '/api/workorder/close',
    method: 'post',
    data
  })
}

export function getWorkOrderStatistics(params) {
  return request({
    url: '/api/workorder/statistics',
    method: 'get',
    params
  })
}

export function getOperatorLocations(params) {
  return request({
    url: '/api/workorder/dispatch/operators/locations',
    method: 'get',
    params
  })
}

export function getRecommendOperators(params) {
  return request({
    url: '/api/workorder/dispatch/operators/recommend',
    method: 'get',
    params
  })
}

export function assignDispatchOrder(data) {
  return request({
    url: '/api/workorder/dispatch/assign',
    method: 'post',
    data
  })
}

export function autoAssignOrder(data) {
  return request({
    url: '/api/workorder/dispatch/autoAssign',
    method: 'post',
    data
  })
}

export function reportLocation(data) {
  return request({
    url: '/api/workorder/dispatch/location/report',
    method: 'post',
    data
  })
}
