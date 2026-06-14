import request from '../utils/request'

export function getPredictionCurve(params) {
  return request({
    url: '/api/prediction/curve',
    method: 'get',
    params
  })
}

export function getPredictionSummary(stationId) {
  return request({
    url: `/api/prediction/summary/${stationId}`,
    method: 'get'
  })
}

export function queryPredictions(params) {
  return request({
    url: '/api/prediction/list',
    method: 'get',
    params
  })
}

export function executePrediction(data) {
  return request({
    url: '/api/prediction/execute',
    method: 'post',
    params: data
  })
}

export function triggerModelTraining(data) {
  return request({
    url: '/api/prediction/train',
    method: 'post',
    params: data
  })
}

export function getWeatherOverview(stationId) {
  return request({
    url: `/api/prediction/weather/overview/${stationId}`,
    method: 'get'
  })
}

export function queryWeatherHistory(params) {
  return request({
    url: '/api/prediction/weather/history',
    method: 'get',
    params
  })
}

export function queryAlerts(params) {
  return request({
    url: '/api/prediction/alert/list',
    method: 'get',
    params
  })
}

export function handleAlert(alertId, data) {
  return request({
    url: `/api/prediction/alert/handle/${alertId}`,
    method: 'post',
    params: data
  })
}

export function countPendingAlerts(stationId) {
  return request({
    url: '/api/prediction/alert/pending/count',
    method: 'get',
    params: stationId ? { stationId } : {}
  })
}
