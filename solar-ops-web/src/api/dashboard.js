import request from '../utils/request'

export function getRealTimeDashboard() {
  return request({
    url: '/api/dashboard/realtime',
    method: 'get'
  })
}

export function getInverterMonitorByStation(stationId) {
  return request({
    url: `/api/dashboard/inverter/station/${stationId}`,
    method: 'get'
  })
}

export function getMobileDashboard() {
  return request({
    url: '/api/dashboard/mobile',
    method: 'get'
  })
}
