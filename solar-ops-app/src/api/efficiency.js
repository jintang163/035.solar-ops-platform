import { get, post } from './request'

export function getEfficiencyRank(params) {
  return get('/efficiency/rank', params)
}

export function getStationEfficiency(stationId, params) {
  return get(`/efficiency/station/${stationId}`, params)
}

export function getEfficiencyList(params) {
  return get('/efficiency/list', params)
}

export function getHealthAssessment(stationId) {
  return get(`/health/latest/${stationId}`)
}

export function assessStationHealth(stationId) {
  return post(`/health/assess/${stationId}`)
}

export function getStationHealthList(data) {
  return post('/health/list', data)
}

export function compareStations(params) {
  return post('/efficiency/compare', params)
}
