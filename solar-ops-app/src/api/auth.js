import { post, get } from './request'

export function login(data) {
  return post('/auth/login', data)
}

export function getSmsCode(phone) {
  return get('/auth/sms-code', { phone })
}

export function getUserInfo() {
  return get('/auth/user-info')
}

export function logout() {
  return post('/auth/logout')
}
