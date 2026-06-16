import { reportLocation } from '@/api/workorder'
import { isOnline } from '@/utils/auth'

const STORAGE_KEY = 'location_report_config'
const LAST_REPORT_KEY = 'last_location_report'
const DEFAULT_INTERVAL = 60000
const MIN_DISTANCE = 100

let reportTimer = null
let lastLocation = null
let isRunning = false

function getCurrentUser() {
  try {
    const userInfo = uni.getStorageSync('userInfo')
    if (userInfo) {
      const parsed = typeof userInfo === 'string' ? JSON.parse(userInfo) : userInfo
      return {
        userId: parsed.id || parsed.userId || null,
        userName: parsed.name || parsed.userName || parsed.nickname || ''
      }
    }
  } catch (e) {
    console.error('获取用户信息失败:', e)
  }
  return { userId: null, userName: '' }
}

function getDistance(lat1, lng1, lat2, lng2) {
  const radLat1 = lat1 * Math.PI / 180.0
  const radLat2 = lat2 * Math.PI / 180.0
  const a = radLat1 - radLat2
  const b = lng1 * Math.PI / 180.0 - lng2 * Math.PI / 180.0
  let s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
    Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)))
  s = s * 6371000
  return s
}

function shouldReport(location) {
  if (!lastLocation) {
    return true
  }

  const lastReportTime = uni.getStorageSync(LAST_REPORT_KEY) || 0
  const now = Date.now()
  if (now - lastReportTime < 30000) {
    return false
  }

  const distance = getDistance(
    lastLocation.latitude, lastLocation.longitude,
    location.latitude, location.longitude
  )

  return distance >= MIN_DISTANCE
}

async function doReport(location) {
  const { userId, userName } = getCurrentUser()
  if (!userId) {
    return false
  }

  if (!shouldReport(location)) {
    return false
  }

  try {
    const data = {
      userId,
      userName,
      longitude: location.longitude,
      latitude: location.latitude,
      accuracy: location.accuracy || 0,
      speed: location.speed || 0,
      heading: location.heading || 0,
      locationType: location.locationType || 'GPS'
    }

    await reportLocation(data)
    lastLocation = { ...location }
    uni.setStorageSync(LAST_REPORT_KEY, Date.now())
    return true
  } catch (e) {
    console.error('位置上报失败:', e)
    return false
  }
}

function getCurrentPosition() {
  return new Promise((resolve, reject) => {
    uni.getLocation({
      type: 'gcj02',
      isHighAccuracy: true,
      highAccuracyExpireTime: 3000,
      success: (res) => {
        resolve({
          longitude: res.longitude,
          latitude: res.latitude,
          accuracy: res.accuracy,
          speed: res.speed,
          heading: res.heading,
          locationType: 'GPS'
        })
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

async function reportOnce() {
  try {
    const location = await getCurrentPosition()
    await doReport(location)
    return location
  } catch (e) {
    console.error('单次位置上报失败:', e)
    return null
  }
}

function startLocationReport(interval = DEFAULT_INTERVAL) {
  if (isRunning) {
    console.log('位置上报已在运行中')
    return
  }

  isRunning = true
  console.log('启动位置上报, 间隔:', interval, 'ms')

  reportOnce()

  reportTimer = setInterval(async () => {
    if (!isOnline()) {
      console.log('网络不可用, 跳过位置上报')
      return
    }
    await reportOnce()
  }, interval)
}

function stopLocationReport() {
  if (reportTimer) {
    clearInterval(reportTimer)
    reportTimer = null
  }
  isRunning = false
  console.log('停止位置上报')
}

function getLastKnownLocation() {
  return lastLocation
}

function isReportRunning() {
  return isRunning
}

export const locationService = {
  startLocationReport,
  stopLocationReport,
  reportOnce,
  getCurrentPosition,
  getLastKnownLocation,
  isReportRunning,
  getDistance
}

export default locationService
