import { getToken, removeToken } from '@/utils/auth'

const BASE_URL = '/api'

const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const header = {
      'Content-Type': 'application/json',
      ...options.header
    }
    if (token) {
      header['Authorization'] = `Bearer ${token}`
    }

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: header,
      timeout: 15000,
      success: (res) => {
        const result = res.data
        if (res.statusCode === 200) {
          if (result.code === 200 || result.code === 0) {
            resolve(result.data || result)
          } else if (result.code === 401) {
            removeToken()
            uni.redirectTo({
              url: '/pages/login/login'
            })
            reject(new Error('登录已过期'))
          } else {
            uni.showToast({
              title: result.message || '请求失败',
              icon: 'none'
            })
            reject(new Error(result.message || '请求失败'))
          }
        } else {
          reject(new Error(`HTTP Error: ${res.statusCode}`))
        }
      },
      fail: (err) => {
        uni.showToast({
          title: '网络连接失败',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

export const get = (url, data, options = {}) => {
  return request({
    url,
    method: 'GET',
    data,
    ...options
  })
}

export const post = (url, data, options = {}) => {
  return request({
    url,
    method: 'POST',
    data,
    ...options
  })
}

export const put = (url, data, options = {}) => {
  return request({
    url,
    method: 'PUT',
    data,
    ...options
  })
}

export const del = (url, data, options = {}) => {
  return request({
    url,
    method: 'DELETE',
    data,
    ...options
  })
}

export default request
