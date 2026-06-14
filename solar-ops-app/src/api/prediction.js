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

export function getLifetimePrediction(params) {
  return get('/lifetime/predict', params)
}

export function getLatestLifetimePrediction(stationId, inverterId) {
  return get(`/lifetime/latest/${stationId}/${inverterId}`)
}

export function getSparePartAdvice(params) {
  return get('/lifetime/spare-part/advice', params)
}

export function queryLifetimeAlerts(params) {
  return get('/lifetime/alert/list', params)
}

export function countLifetimePendingAlerts(stationId) {
  return get('/lifetime/alert/pending/count', stationId ? { stationId } : {})
}
