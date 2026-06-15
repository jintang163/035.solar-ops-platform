import request from '../utils/request'

export function getWorkspaceInfo() {
  return request({
    url: '/api/workspace/info',
    method: 'get'
  })
}

export function getStationTree() {
  return request({
    url: '/api/workspace/station-tree',
    method: 'get'
  })
}

export function switchWorkspace(stationId) {
  return request({
    url: '/api/workspace/switch',
    method: 'post',
    data: { stationId }
  })
}

export function getUserStations(userId) {
  return request({
    url: `/api/user-stations/${userId}`,
    method: 'get'
  })
}

export function assignUserStations(data) {
  return request({
    url: '/api/user-stations/assign',
    method: 'post',
    data
  })
}

export function getOrgList(params) {
  return request({
    url: '/api/orgs',
    method: 'get',
    params
  })
}

export function getOrgTree() {
  return request({
    url: '/api/orgs/tree',
    method: 'get'
  })
}

export function getOrgDetail(id) {
  return request({
    url: `/api/orgs/${id}`,
    method: 'get'
  })
}

export function createOrg(data) {
  return request({
    url: '/api/orgs',
    method: 'post',
    data
  })
}

export function updateOrg(data) {
  return request({
    url: '/api/orgs',
    method: 'put',
    data
  })
}

export function deleteOrg(id) {
  return request({
    url: `/api/orgs/${id}`,
    method: 'delete'
  })
}
