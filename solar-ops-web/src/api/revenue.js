import request from '../utils/request'

export function getPriceSchemeList(data) {
  return request({
    url: '/api/revenue/price/page',
    method: 'post',
    data
  })
}

export function getPriceSchemeAll(params) {
  return request({
    url: '/api/revenue/price/list',
    method: 'get',
    params
  })
}

export function getPriceSchemeDetail(id) {
  return request({
    url: `/api/revenue/price/${id}`,
    method: 'get'
  })
}

export function createPriceScheme(data) {
  return request({
    url: '/api/revenue/price',
    method: 'post',
    data
  })
}

export function updatePriceScheme(data) {
  return request({
    url: '/api/revenue/price',
    method: 'put',
    data
  })
}

export function deletePriceScheme(id) {
  return request({
    url: `/api/revenue/price/${id}`,
    method: 'delete'
  })
}

export function getDefaultPriceScheme(params) {
  return request({
    url: '/api/revenue/price/default',
    method: 'get',
    params
  })
}

export function comparePriceSchemes(data) {
  return request({
    url: '/api/revenue/price/compare',
    method: 'post',
    data
  })
}

export function calcDailyRevenue(params) {
  return request({
    url: '/api/revenue/calc/daily',
    method: 'post',
    params
  })
}

export function calcMonthlyRevenue(params) {
  return request({
    url: '/api/revenue/calc/monthly',
    method: 'post',
    params
  })
}

export function calcBatchDailyRevenue(params) {
  return request({
    url: '/api/revenue/calc/batch',
    method: 'post',
    params
  })
}

export function getRevenueDashboard(params) {
  return request({
    url: '/api/revenue/statistics/dashboard',
    method: 'post',
    data: params
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
