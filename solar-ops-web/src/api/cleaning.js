import request from '../utils/request'

export function getDustRecordPage(data) {
  return request({
    url: '/api/cleaning/dust/page',
    method: 'post',
    data
  })
}

export function getDustRecordList(data) {
  return request({
    url: '/api/cleaning/dust/list',
    method: 'post',
    data
  })
}

export function detectDustAccumulation(params) {
  return request({
    url: '/api/cleaning/dust/detect',
    method: 'post',
    params
  })
}

export function getReminderPage(data) {
  return request({
    url: '/api/cleaning/reminder/page',
    method: 'post',
    data
  })
}

export function getReminderList(data) {
  return request({
    url: '/api/cleaning/reminder/list',
    method: 'post',
    data
  })
}

export function getReminderDetail(id) {
  return request({
    url: `/api/cleaning/reminder/${id}`,
    method: 'get'
  })
}

export function generateReminders(params) {
  return request({
    url: '/api/cleaning/reminder/generate',
    method: 'post',
    params
  })
}

export function ignoreReminder(id, params) {
  return request({
    url: `/api/cleaning/reminder/ignore/${id}`,
    method: 'post',
    params
  })
}

export function createCleaningPlan(data, params) {
  return request({
    url: '/api/cleaning/plan/create',
    method: 'post',
    data,
    params
  })
}

export function updateCleaningPlan(data, params) {
  return request({
    url: '/api/cleaning/plan/update',
    method: 'post',
    data,
    params
  })
}

export function getCleaningPlanPage(data) {
  return request({
    url: '/api/cleaning/plan/page',
    method: 'post',
    data
  })
}

export function getCleaningPlanList(data) {
  return request({
    url: '/api/cleaning/plan/list',
    method: 'post',
    data
  })
}

export function getCleaningPlanCalendar(params) {
  return request({
    url: '/api/cleaning/plan/calendar',
    method: 'get',
    params
  })
}

export function getCleaningPlanDetail(id) {
  return request({
    url: `/api/cleaning/plan/${id}`,
    method: 'get'
  })
}

export function startCleaningPlan(data) {
  return request({
    url: '/api/cleaning/plan/start',
    method: 'post',
    data
  })
}

export function completeCleaningPlan(data) {
  return request({
    url: '/api/cleaning/plan/complete',
    method: 'post',
    data
  })
}

export function cancelCleaningPlan(id, params) {
  return request({
    url: `/api/cleaning/plan/cancel/${id}`,
    method: 'post',
    params
  })
}

export function uploadCleaningPhotos(params) {
  return request({
    url: '/api/cleaning/plan/upload-photos',
    method: 'post',
    params
  })
}

export function getCleaningDashboard(params) {
  return request({
    url: '/api/cleaning/statistics/dashboard',
    method: 'get',
    params
  })
}

export function getDustLevelStats(params) {
  return request({
    url: '/api/cleaning/statistics/dust-level',
    method: 'get',
    params
  })
}

export function getImprovementTrend(params) {
  return request({
    url: '/api/cleaning/statistics/trend',
    method: 'get',
    params
  })
}

export function getStationCleaningRank(params) {
  return request({
    url: '/api/cleaning/statistics/rank',
    method: 'get',
    params
  })
}
