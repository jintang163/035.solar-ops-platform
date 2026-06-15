import { get, post } from './request'

export function getCleaningDashboard(params) {
  return get('/cleaning/statistics/dashboard', params)
}

export function getImprovementTrend(params) {
  return get('/cleaning/statistics/trend', params)
}

export function getReminderPage(data) {
  return post('/cleaning/reminder/page', data)
}

export function getReminderList(data) {
  return post('/cleaning/reminder/list', data)
}

export function getReminderDetail(id) {
  return get(`/cleaning/reminder/${id}`)
}

export function ignoreReminder(id, params) {
  return post(`/cleaning/reminder/ignore/${id}`, null, { params })
}

export function generateReminders(params) {
  return post('/cleaning/reminder/generate', null, { params })
}

export function getCleaningPlanPage(data) {
  return post('/cleaning/plan/page', data)
}

export function getCleaningPlanList(data) {
  return post('/cleaning/plan/list', data)
}

export function getCleaningPlanCalendar(params) {
  return get('/cleaning/plan/calendar', params)
}

export function getCleaningPlanDetail(id) {
  return get(`/cleaning/plan/${id}`)
}

export function createCleaningPlan(data, params) {
  return post('/cleaning/plan/create', data, { params })
}

export function updateCleaningPlan(data, params) {
  return post('/cleaning/plan/update', data, { params })
}

export function startCleaningPlan(data) {
  return post('/cleaning/plan/start', data)
}

export function completeCleaningPlan(data) {
  return post('/cleaning/plan/complete', data)
}

export function cancelCleaningPlan(id, params) {
  return post(`/cleaning/plan/cancel/${id}`, null, { params })
}

export function uploadCleaningPhotos(params) {
  return post('/cleaning/plan/upload-photos', null, { params })
}

export function uploadFile(filePath, formData = {}) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token') || ''
    uni.uploadFile({
      url: '/api/files/upload',
      filePath: filePath,
      name: 'file',
      formData: formData,
      header: {
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          if (data.code === 200 || data.code === 0) {
            resolve(data.data || data)
          } else {
            reject(new Error(data.message || '上传失败'))
          }
        } catch (e) {
          reject(new Error('解析响应失败'))
        }
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}
