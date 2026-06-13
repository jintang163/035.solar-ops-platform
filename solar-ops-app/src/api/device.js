import { get } from './request'

export function getInverterList(params) {
  return get('/inverters', params)
}

export function getInverterListByStation(stationId) {
  return get('/inverters/list', { stationId })
}

export function getInverterDetail(id) {
  return get(`/inverters/${id}`)
}

export function getDeviceRealtimeData(deviceId) {
  return get(`/device/data/realtime/${deviceId}`)
}

export function getAllDeviceRealtimeData() {
  return get('/device/data/realtime/all')
}

export function getDeviceHistoryData(deviceId, params) {
  return get(`/device/data/history/${deviceId}`, params)
}

export function getOnlineDeviceCount() {
  return get('/device/data/online/count')
}
