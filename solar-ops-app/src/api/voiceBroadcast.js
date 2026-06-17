import { get, put, post } from './request'

export function getBroadcastHistory(params) {
  return get('/voice-broadcast/history', params)
}

export function getBroadcastConfig() {
  return get('/voice-broadcast/config')
}

export function updateBroadcastConfig(data) {
  return put('/voice-broadcast/config', data)
}

export function testBroadcast(content) {
  return post('/voice-broadcast/trigger/test', { content })
}

export function retryBroadcast(id) {
  return put(`/voice-broadcast/${id}/retry`)
}

export function getSpeakerDevices() {
  return get('/voice-broadcast/speakers')
}

export function testSpeaker(deviceId) {
  return post('/voice-broadcast/speakers/test', { deviceId })
}
