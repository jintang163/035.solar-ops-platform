import request from '../utils/request'

export function getEfficiencyRank(params) {
  return request({
    url: '/efficiency/rank',
    method: 'get',
    params
  })
}

export function getEfficiencyTrend(params) {
  return request({
    url: '/efficiency/trend',
    method: 'get',
    params
  })
}

export function getHealthAssessment(stationId) {
  return request({
    url: '/efficiency/health',
    method: 'get',
    params: { stationId }
  })
}

export function getPRStatistics(params) {
  return request({
    url: '/efficiency/pr',
    method: 'get',
    params
  })
}

export function getComparisonData(params) {
  return request({
    url: '/efficiency/comparison',
    method: 'get',
    params
  })
}
