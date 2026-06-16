import request from '../utils/request'

export function getKnowledgePage(data) {
  return request({
    url: '/api/knowledge/page',
    method: 'post',
    data
  })
}

export function getKnowledgeDetail(id) {
  return request({
    url: `/api/knowledge/${id}`,
    method: 'get'
  })
}

export function getKnowledgeByCode(faultCode) {
  return request({
    url: `/api/knowledge/code/${faultCode}`,
    method: 'get'
  })
}

export function getKnowledgeList() {
  return request({
    url: '/api/knowledge/list',
    method: 'get'
  })
}

export function addKnowledge(data) {
  return request({
    url: '/api/knowledge/add',
    method: 'post',
    data
  })
}

export function updateKnowledge(data) {
  return request({
    url: '/api/knowledge/update',
    method: 'post',
    data
  })
}

export function deleteKnowledge(id) {
  return request({
    url: `/api/knowledge/${id}`,
    method: 'delete'
  })
}

export function refreshKnowledgeCache() {
  return request({
    url: '/api/knowledge/refreshCache',
    method: 'post'
  })
}

export function recommendKnowledge(data) {
  return request({
    url: '/api/knowledge/recommend',
    method: 'post',
    data
  })
}

export function submitKnowledgeFeedback(data) {
  return request({
    url: '/api/knowledge/feedback',
    method: 'post',
    data
  })
}

export function getUserFeedback(knowledgeId, userId) {
  return request({
    url: `/api/knowledge/feedback/${knowledgeId}/${userId}`,
    method: 'get'
  })
}

export function recordKnowledgeUsage(data) {
  return request({
    url: '/api/knowledge/recordUsage',
    method: 'post',
    data
  })
}
