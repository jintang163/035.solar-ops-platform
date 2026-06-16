import { get, post, del } from './request'

export function getKnowledgePage(data) {
  return post('/knowledge/page', data)
}

export function getKnowledgeDetail(id) {
  return get(`/knowledge/${id}`)
}

export function getKnowledgeByCode(faultCode) {
  return get(`/knowledge/code/${faultCode}`)
}

export function getKnowledgeList() {
  return get('/knowledge/list')
}

export function addKnowledge(data) {
  return post('/knowledge/add', data)
}

export function updateKnowledge(data) {
  return post('/knowledge/update', data)
}

export function deleteKnowledge(id) {
  return del(`/knowledge/${id}`)
}

export function refreshKnowledgeCache() {
  return post('/knowledge/refreshCache')
}

export function recommendKnowledge(data) {
  return post('/knowledge/recommend', data)
}

export function submitKnowledgeFeedback(data) {
  return post('/knowledge/feedback', data)
}

export function getUserFeedback(knowledgeId, userId) {
  return get(`/knowledge/feedback/${knowledgeId}/${userId}`)
}

export function recordKnowledgeUsage(data) {
  return post('/knowledge/recordUsage', data)
}
