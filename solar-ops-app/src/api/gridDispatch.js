import { get, put, post } from './request'

export function getDispatchSummary() {
  return get('/grid-dispatch/summary')
}

export function getDispatchCommands(params) {
  return get('/grid-dispatch/commands', params)
}

export function getDispatchCommandDetail(id) {
  return get(`/grid-dispatch/commands/${id}`)
}

export function getDispatchCommandCurve(id) {
  return get(`/grid-dispatch/commands/${id}/curve`)
}

export function getDispatchUploadRecords(params) {
  return get('/grid-dispatch/upload-records', params)
}

export function createManualCommand(data) {
  return post('/grid-dispatch/commands/manual', data)
}

export function cancelDispatchCommand(id) {
  return put(`/grid-dispatch/commands/${id}/cancel`)
}

export function getProtocolConfigs() {
  return get('/grid-dispatch/protocol-configs')
}

export function testProtocolConnection(id) {
  return post(`/grid-dispatch/protocol-configs/${id}/test`)
}
