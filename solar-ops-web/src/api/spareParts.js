import request from '../utils/request'

export function getSparePartList(params) {
  return request({
    url: '/api/spare-parts',
    method: 'get',
    params
  })
}

export function getSparePartDetail(id) {
  return request({
    url: `/api/spare-parts/${id}`,
    method: 'get'
  })
}

export function getSparePartByCode(partCode) {
  return request({
    url: `/api/spare-parts/code/${partCode}`,
    method: 'get'
  })
}

export function addSparePart(data) {
  return request({
    url: '/api/spare-parts',
    method: 'post',
    data
  })
}

export function updateSparePart(data) {
  return request({
    url: '/api/spare-parts',
    method: 'put',
    data
  })
}

export function deleteSparePart(id) {
  return request({
    url: `/api/spare-parts/${id}`,
    method: 'delete'
  })
}

export function deleteBatchSpareParts(ids) {
  return request({
    url: '/api/spare-parts/batch',
    method: 'delete',
    data: ids
  })
}

export function sparePartInbound(data) {
  return request({
    url: '/api/spare-parts/inbound',
    method: 'post',
    data
  })
}

export function sparePartOutbound(data) {
  return request({
    url: '/api/spare-parts/outbound',
    method: 'post',
    data
  })
}

export function getSparePartQrCode(id) {
  return request({
    url: `/api/spare-parts/${id}/qrcode`,
    method: 'get'
  })
}

export function batchGenerateQrCode(ids) {
  return request({
    url: '/api/spare-parts/qrcode/batch',
    method: 'post',
    data: ids
  })
}

export function scanSparePart(partCode) {
  return request({
    url: `/api/spare-parts/scan/${partCode}`,
    method: 'get'
  })
}

export function getInventoryDashboard() {
  return request({
    url: '/api/spare-parts/dashboard',
    method: 'get'
  })
}

export function exportSpareParts(params) {
  return request({
    url: '/api/spare-parts/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function getInOutRecordList(params) {
  return request({
    url: '/api/spare-part-records',
    method: 'get',
    params
  })
}

export function getInOutRecordDetail(id) {
  return request({
    url: `/api/spare-part-records/${id}`,
    method: 'get'
  })
}

export function exportInOutRecords(params) {
  return request({
    url: '/api/spare-part-records/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function getStocktakeList(params) {
  return request({
    url: '/api/stocktakes',
    method: 'get',
    params
  })
}

export function getStocktakeDetail(id) {
  return request({
    url: `/api/stocktakes/${id}`,
    method: 'get'
  })
}

export function getStocktakeItems(id, params) {
  return request({
    url: `/api/stocktakes/${id}/items`,
    method: 'get',
    params
  })
}

export function createStocktake(data) {
  return request({
    url: '/api/stocktakes',
    method: 'post',
    data
  })
}

export function startStocktake(id) {
  return request({
    url: `/api/stocktakes/${id}/start`,
    method: 'put'
  })
}

export function updateStocktakeItem(data) {
  return request({
    url: '/api/stocktakes/item',
    method: 'put',
    data
  })
}

export function completeStocktake(id) {
  return request({
    url: `/api/stocktakes/${id}/complete`,
    method: 'put'
  })
}

export function cancelStocktake(id) {
  return request({
    url: `/api/stocktakes/${id}/cancel`,
    method: 'put'
  })
}

export function exportStocktakeDiff(id) {
  return request({
    url: `/api/stocktakes/${id}/diff/export`,
    method: 'get',
    responseType: 'blob'
  })
}

export function getPurchaseSuggestionList(params) {
  return request({
    url: '/api/purchase-suggestions',
    method: 'get',
    params
  })
}

export function getPurchaseSuggestionDetail(id) {
  return request({
    url: `/api/purchase-suggestions/${id}`,
    method: 'get'
  })
}

export function generatePurchaseSuggestions() {
  return request({
    url: '/api/purchase-suggestions/generate',
    method: 'post'
  })
}

export function processPurchaseSuggestion(id, status, processorName, remark) {
  return request({
    url: `/api/purchase-suggestions/${id}/process`,
    method: 'put',
    params: { status, processorName, remark }
  })
}

export function batchProcessPurchaseSuggestions(ids, status, processorName) {
  return request({
    url: '/api/purchase-suggestions/batch/process',
    method: 'put',
    params: { status, processorName },
    data: ids
  })
}

export function getPendingSuggestionCount() {
  return request({
    url: '/api/purchase-suggestions/pending/count',
    method: 'get'
  })
}

export function workOrderOutboundSpareParts(data) {
  return request({
    url: '/api/maintenance-records/work-order-outbound',
    method: 'post',
    data
  })
}
