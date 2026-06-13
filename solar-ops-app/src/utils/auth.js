const TOKEN_KEY = 'solar_ops_token'
const USER_INFO_KEY = 'solar_ops_user_info'

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY)
}

export function setToken(token) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function removeToken() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_INFO_KEY)
}

export function getUserInfo() {
  return uni.getStorageSync(USER_INFO_KEY) || {}
}

export function setUserInfo(userInfo) {
  uni.setStorageSync(USER_INFO_KEY, userInfo)
}
