import { get } from './request'

export function login(data) {
  return post('/auth/login', data)
}

export function getStationListAll() {
  return get('/stations/list')
}

export function getStationDetail(id) {
  return get(`/stations/${id}`)
}
