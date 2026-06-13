import request from '../utils/request'

export function getStationList(params) {
  return request({
    url: '/api/stations',
    method: 'get',
    params
  })
}

export function getStationListAll() {
  return request({
    url: '/api/stations/list',
    method: 'get'
  })
}

export function getStationDetail(id) {
  return request({
    url: `/api/stations/${id}`,
    method: 'get'
  })
}

export function addStation(data) {
  return request({
    url: '/api/stations',
    method: 'post',
    data
  })
}

export function updateStation(data) {
  return request({
    url: '/api/stations',
    method: 'put',
    data
  })
}

export function deleteStation(id) {
  return request({
    url: `/api/stations/${id}`,
    method: 'delete'
  })
}
