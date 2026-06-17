import { get } from './request'

export function getMobileDashboard() {
  return get('/dashboard/mobile')
}

export function getRealTimeDashboard() {
  return get('/dashboard/realtime')
}

export function getInverterMonitorByStation(stationId) {
  return get(`/dashboard/inverter/station/${stationId}`)
}
