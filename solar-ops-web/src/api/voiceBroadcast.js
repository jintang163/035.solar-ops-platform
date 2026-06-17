import request from '../utils/request'

export function getBroadcastHistory(params) {
  return request({
    url: '/api/voice-broadcast/history',
    method: 'get',
    params
  })
}

export function getBroadcastConfig() {
  return request({
    url: '/api/voice-broadcast/config',
    method: 'get'
  })
}

export function updateBroadcastConfig(data) {
  return request({
    url: '/api/voice-broadcast/config',
    method: 'put',
    data
  })
}

export function testBroadcast(content) {
  return request({
    url: '/api/voice-broadcast/trigger/test',
    method: 'post',
    params: { content }
  })
}

export function retryBroadcast(id) {
  return request({
    url: `/api/voice-broadcast/${id}/retry`,
    method: 'put'
  })
}
