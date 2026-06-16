import { get, post, put } from './request'

export const inspectionApi = {
  getDownloadTasks(userId) {
    return get('/inspection/tasks/download', { userId })
  },

  getTaskList(params) {
    return get('/inspection/tasks', params)
  },

  getTaskDetail(id) {
    return get(`/inspection/tasks/${id}`)
  },

  markTaskAsDownloaded(id) {
    return put(`/inspection/tasks/${id}/downloaded`)
  },

  startTask(id) {
    return put(`/inspection/tasks/${id}/start`)
  },

  completeTask(id) {
    return put(`/inspection/tasks/${id}/complete`)
  },

  submitResult(data) {
    return post('/inspection/results', data)
  },

  getResultList(params) {
    return get('/inspection/results', params)
  },

  getResultDetail(id) {
    return get(`/inspection/results/${id}`)
  },

  generateReport(id) {
    return post(`/inspection/results/${id}/report`)
  },

  uploadPhoto(filePath, taskId, resultItemId, photoType = 1) {
    return new Promise((resolve, reject) => {
      uni.uploadFile({
        url: '/api/inspection/photos',
        filePath: filePath,
        name: 'file',
        formData: {
          taskId,
          resultItemId,
          photoType
        },
        success: (res) => {
          try {
            const data = JSON.parse(res.data)
            if (data.code === 200 || data.code === 0) {
              resolve(data.data)
            } else {
              reject(new Error(data.message || '上传失败'))
            }
          } catch (e) {
            reject(e)
          }
        },
        fail: reject
      })
    })
  },

  uploadAudio(filePath, taskId, resultItemId) {
    return new Promise((resolve, reject) => {
      uni.uploadFile({
        url: '/api/inspection/audios',
        filePath: filePath,
        name: 'file',
        formData: {
          taskId,
          resultItemId
        },
        success: (res) => {
          try {
            const data = JSON.parse(res.data)
            if (data.code === 200 || data.code === 0) {
              resolve(data.data)
            } else {
              reject(new Error(data.message || '上传失败'))
            }
          } catch (e) {
            reject(e)
          }
        },
        fail: reject
      })
    })
  }
}

export default inspectionApi
