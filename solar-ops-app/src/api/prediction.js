import { get, post } from './request'

export function getPredictionCurve(params) {
  return get('/prediction/curve', params)
}

export function getPredictionSummary(stationId) {
  return get(`/prediction/summary/${stationId}`)
}

export function getWeatherOverview(stationId) {
  return get(`/prediction/weather/overview/${stationId}`)
}

export function queryAlerts(params) {
  return get('/prediction/alert/list', params)
}

export function countPendingAlerts(stationId) {
  return get('/prediction/alert/pending/count', stationId ? { stationId } : {})
}
