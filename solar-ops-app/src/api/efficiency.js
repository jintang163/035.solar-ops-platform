import { get } from './request'

export function getEfficiencyRank(params) {
  return get('/efficiency/rank', params)
}

export function getHealthScore(stationId) {
  return get(`/efficiency/health/${stationId}`)
}

export function getTrendData(params) {
  return get('/efficiency/trend', params)
}

export function getPrStats(params) {
  return get('/efficiency/pr-stats', params)
}
