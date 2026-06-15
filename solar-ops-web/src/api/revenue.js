import request from '../utils/request'

export function getPriceSchemeList(data) {
  return request({
    url: '/api/revenue/price-scheme/page',
    method: 'post',
    data
  })
}

export function getPriceSchemeAll(data) {
  return request({
    url: '/api/revenue/price-scheme/list',
    method: 'post',
    data
  })
}

export function getPriceSchemeDetail(id) {
  return request({
    url: `/api/revenue/price-scheme/${id}`,
    method: 'get'
  })
}

export function createPriceScheme(data) {
  return request({
    url: '/api/revenue/price-scheme/save',
    method: 'post',
    data
  })
}

export function updatePriceScheme(data) {
  return request({
    url: '/api/revenue/price-scheme/update',
    method: 'post',
    data
  })
}

export function deletePriceScheme(id) {
  return request({
    url: `/api/revenue/price-scheme/${id}`,
    method: 'delete'
  })
}

export function getDefaultPriceScheme(params) {
  return request({
    url: '/api/revenue/price-scheme/default',
    method: 'get',
    params
  })
}

export function comparePriceSchemes(data, params) {
  return request({
    url: '/api/revenue/price-scheme/compare',
    method: 'post',
    data,
    params
  })
}

export function calcDailyRevenue(params) {
  return request({
    url: '/api/revenue/calculate/daily',
    method: 'post',
    params
  })
}

export function calcMonthlyRevenue(params) {
  return request({
    url: '/api/revenue/calculate/monthly',
    method: 'post',
    params
  })
}

export function calcBatchDailyRevenue(params) {
  return request({
    url: '/api/revenue/calculate/all-daily',
    method: 'post',
    params
  })
}

export function getRevenueDashboard(params) {
  return request({
    url: '/api/revenue/statistics/dashboard',
    method: 'get',
    params
  })
}

export function getRevenuePage(data) {
  return request({
    url: '/api/revenue/statistics/page',
    method: 'post',
    data
  })
}

export function getRevenueList(data) {
  return request({
    url: '/api/revenue/statistics/list',
    method: 'post',
    data
  })
}

export function getRevenueTrend(params) {
  return request({
    url: '/api/revenue/statistics/trend',
    method: 'get',
    params
  })
}

export function getRevenueStationRank(params) {
  return request({
    url: '/api/revenue/statistics/rank',
    method: 'get',
    params
  })
}

export function getTotalRevenue(data) {
  return request({
    url: '/api/revenue/statistics/total',
    method: 'post',
    data
  })
}
