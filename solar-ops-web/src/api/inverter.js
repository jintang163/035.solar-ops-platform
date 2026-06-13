import request from '../utils/request'

export function getInverterList(params) {
  return request({
    url: '/inverter/page',
    method: 'get',
    params
  })
}

export function getInverterDetail(id) {
  return request({
    url: `/inverter/${id}`,
    method: 'get'
  })
}

export function getInverterRealTimeData(id) {
  return request({
    url: `/inverter/${id}/realtime`,
    method: 'get'
  })
}

export function getInverterHistoryData(params) {
  return request({
    url: '/inverter/history',
    method: 'get',
    params
  })
}

export function getInverterStatistics() {
  return request({
    url: '/inverter/statistics',
    method: 'get'
  })
}

export function controlInverter(data) {
  return request({
    url: '/inverter/control',
    method: 'post',
    data
  })
}
