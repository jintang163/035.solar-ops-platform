import { get, post } from './request'

export function getInventoryList(data) {
  return post('/spare-parts/inventory/page', data)
}

export function getInventoryDetail(id) {
  return get(`/spare-parts/inventory/${id}`)
}

export function scanSparePart(code) {
  return get(`/spare-parts/inventory/scan/${code}`)
}

export function getInventoryByType() {
  return get('/spare-parts/inventory/by-type')
}

export function sparePartOutbound(data) {
  return post('/spare-parts/inventory/outbound', data)
}

export function getInventoryDashboard() {
  return get('/spare-parts/inventory/dashboard')
}

export function getInOutRecordList(data) {
  return post('/spare-parts/in-out-record/page', data)
}

export function workOrderOutboundSpareParts(data) {
  return post('/maintenance-records/work-order-outbound', data)
}
