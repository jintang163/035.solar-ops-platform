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

export function getLifetimePrediction(params) {
  return request({
    url: '/api/lifetime/predict',
    method: 'get',
    params
  })
}

export function getLatestLifetimePrediction(stationId, inverterId) {
  return request({
    url: `/api/lifetime/latest/${stationId}/${inverterId}`,
    method: 'get'
  })
}

export function getLifetimePredictionHistory(params) {
  return request({
    url: '/api/lifetime/history',
    method: 'get',
    params
  })
}

export function getHealthHistory(params) {
  return request({
    url: '/api/lifetime/health/history',
    method: 'get',
    params
  })
}

export function triggerLifetimeTraining(data) {
  return request({
    url: '/api/lifetime/train',
    method: 'post',
    params: data
  })
}

export function getSparePartAdvice(params) {
  return request({
    url: '/api/lifetime/spare-part/advice',
    method: 'get',
    params
  })
}

export function queryLifetimeAlerts(params) {
  return request({
    url: '/api/lifetime/alert/list',
    method: 'get',
    params
  })
}

export function handleLifetimeAlert(alertId, data) {
  return request({
    url: `/api/lifetime/alert/handle/${alertId}`,
    method: 'post',
    params: data
  })
}

export function countLifetimePendingAlerts(stationId) {
  return request({
    url: '/api/lifetime/alert/pending/count',
    method: 'get',
    params: stationId ? { stationId } : {}
  })
}
