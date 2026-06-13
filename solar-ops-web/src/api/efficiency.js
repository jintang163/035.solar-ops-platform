import request from '../utils/request'

export function getEfficiencyRank(params) {
  return request({
    url: '/api/efficiency/rank',
    method: 'get',
    params
  })
}

export function getStationEfficiency(stationId, params) {
  return request({
    url: `/api/efficiency/station/${stationId}`,
    method: 'get',
    params
  })
}

export function getEfficiencyList(params) {
  return request({
    url: '/api/efficiency/list',
    method: 'get',
    params
  })
}

export function getLowEfficiencyInverters(stationId, params) {
  return request({
    url: `/api/efficiency/low-efficiency/${stationId}`,
    method: 'get',
    params
  })
}

export function calculatePr(params) {
  return request({
    url: '/api/efficiency/calculate',
    method: 'post',
    params
  })
}

export function getHealthAssessment(stationId) {
  return request({
    url: `/api/health/latest/${stationId}`,
    method: 'get'
  })
}

export function assessStationHealth(stationId) {
  return request({
    url: `/api/health/assess/${stationId}`,
    method: 'post'
  })
}

export function getStationHealthList(data) {
  return request({
    url: '/api/health/list',
    method: 'post',
    data
  })
}
