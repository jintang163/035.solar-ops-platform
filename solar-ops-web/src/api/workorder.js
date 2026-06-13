import request from '../utils/request'

export function getWorkOrderList(params) {
  return request({
    url: '/workorder/page',
    method: 'get',
    params
  })
}

export function getWorkOrderDetail(id) {
  return request({
    url: `/workorder/${id}`,
    method: 'get'
  })
}

export function createWorkOrder(data) {
  return request({
    url: '/workorder',
    method: 'post',
    data
  })
}

export function handleWorkOrder(data) {
  return request({
    url: '/workorder/handle',
    method: 'post',
    data
  })
}

export function getWorkOrderLogs(id) {
  return request({
    url: `/workorder/${id}/logs`,
    method: 'get'
  })
}

export function getWorkOrderStatistics() {
  return request({
    url: '/workorder/statistics',
    method: 'get'
  })
}

export function getWorkOrderStatusList() {
  return request({
    url: '/workorder/statusList',
    method: 'get'
  })
}
