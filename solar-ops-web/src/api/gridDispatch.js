import request from '../utils/request'

export function getDispatchSummary() {
  return request({
    url: '/api/grid-dispatch/summary',
    method: 'get'
  })
}

export function getDispatchCommands(params) {
  return request({
    url: '/api/grid-dispatch/commands',
    method: 'get',
    params
  })
}

export function getDispatchCommandDetail(id) {
  return request({
    url: `/api/grid-dispatch/commands/${id}`,
    method: 'get'
  })
}

export function getDispatchCommandCurve(id) {
  return request({
    url: `/api/grid-dispatch/commands/${id}/curve`,
    method: 'get'
  })
}

export function getDispatchUploadRecords(params) {
  return request({
    url: '/api/grid-dispatch/upload-records',
    method: 'get',
    params
  })
}

export function createManualCommand(data) {
  return request({
    url: '/api/grid-dispatch/commands/manual',
    method: 'post',
    data
  })
}

export function cancelDispatchCommand(id) {
  return request({
    url: `/api/grid-dispatch/commands/${id}/cancel`,
    method: 'put'
  })
}

export function getProtocolConfigs() {
  return request({
    url: '/api/grid-dispatch/protocol-configs',
    method: 'get'
  })
}

export function testProtocolConnection(id) {
  return request({
    url: `/api/grid-dispatch/protocol-configs/${id}/test`,
    method: 'post'
  })
}
