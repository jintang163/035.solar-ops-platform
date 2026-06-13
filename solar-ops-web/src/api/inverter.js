import request from '../utils/request'

export function getInverterList(params) {
  return request({
    url: '/api/inverters',
    method: 'get',
    params
  })
}

export function getInverterListByStation(stationId) {
  return request({
    url: '/api/inverters/list',
    method: 'get',
    params: { stationId }
  })
}

export function getInverterDetail(id) {
  return request({
    url: `/api/inverters/${id}`,
    method: 'get'
  })
}

export function addInverter(data) {
  return request({
    url: '/api/inverters',
    method: 'post',
    data
  })
}

export function updateInverter(data) {
  return request({
    url: '/api/inverters',
    method: 'put',
    data
  })
}

export function deleteInverter(id) {
  return request({
    url: `/api/inverters/${id}`,
    method: 'delete'
  })
}
