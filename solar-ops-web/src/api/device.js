import request from '../utils/request'

export function getDeviceList(params) {
  return request({
    url: '/device/page',
    method: 'get',
    params
  })
}

export function getDeviceDetail(id) {
  return request({
    url: `/device/${id}`,
    method: 'get'
  })
}

export function getDeviceRealTimeData(id) {
  return request({
    url: `/device/${id}/realtime`,
    method: 'get'
  })
}

export function getDeviceStatistics() {
  return request({
    url: '/device/statistics',
    method: 'get'
  })
}

export function getDeviceDataHistory(params) {
  return request({
    url: '/device/data/history',
    method: 'get',
    params
  })
}
