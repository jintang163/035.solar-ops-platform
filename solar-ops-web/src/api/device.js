import request from '../utils/request'

export function getDeviceRealtimeData(deviceId) {
  return request({
    url: `/api/device/data/realtime/${deviceId}`,
    method: 'get'
  })
}

export function getAllDeviceRealtimeData() {
  return request({
    url: '/api/device/data/realtime/all',
    method: 'get'
  })
}

export function getDeviceHistoryData(deviceId, params) {
  return request({
    url: `/api/device/data/history/${deviceId}`,
    method: 'get',
    params
  })
}

export function getDeviceStatus(deviceId) {
  return request({
    url: `/api/device/data/status/${deviceId}`,
    method: 'get'
  })
}

export function getOnlineDeviceCount() {
  return request({
    url: '/api/device/data/online/count',
    method: 'get'
  })
}
