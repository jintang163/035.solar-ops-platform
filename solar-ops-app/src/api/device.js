import { get } from './request'

export function getDashboardStats() {
  return get('/device/dashboard-stats')
}

export function getInverterList(params) {
  return get('/device/inverter/list', params)
}

export function getInverterDetail(id) {
  return get(`/device/inverter/${id}`)
}

export function getInverterRealtimeData(id) {
  return get(`/device/inverter/${id}/realtime`)
}

export function getDeviceRealtimeList(params) {
  return get('/device/realtime/list', params)
}
