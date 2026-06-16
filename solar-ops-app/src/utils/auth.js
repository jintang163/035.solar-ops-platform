const TOKEN_KEY = 'solar_ops_token'
const USER_INFO_KEY = 'solar_ops_user_info'
let onlineStatus = true

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

export function isOnline() {
  return onlineStatus
}

export function updateNetworkStatus() {
  uni.getNetworkType({
    success: (res) => {
      onlineStatus = res.networkType !== 'none'
    }
  })
}

export function initNetworkListener() {
  updateNetworkStatus()
  uni.onNetworkStatusChange((res) => {
    onlineStatus = res.isConnected
  })
}
