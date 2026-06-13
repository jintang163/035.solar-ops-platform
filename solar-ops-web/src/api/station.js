import request from '../utils/request'

export function getStationList(params) {
  return request({
    url: '/station/page',
    method: 'get',
    params
  })
}

export function getStationListAll() {
  return request({
    url: '/station/list',
    method: 'get'
  })
}

export function getStationDetail(id) {
  return request({
    url: `/station/${id}`,
    method: 'get'
  })
}

export function addStation(data) {
  return request({
    url: '/station',
    method: 'post',
    data
  })
}

export function updateStation(data) {
  return request({
    url: '/station',
    method: 'put',
    data
  })
}

export function deleteStation(id) {
  return request({
    url: `/station/${id}`,
    method: 'delete'
  })
}

export function getStationOverview() {
  return request({
    url: '/station/overview',
    method: 'get'
  })
}
