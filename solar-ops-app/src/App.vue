<script setup>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { getToken, isOnline } from '@/utils/auth'
import db from '@/utils/sqlite.js'
import syncService from '@/utils/sync-service.js'
import locationService from '@/utils/location-service.js'

onLaunch(() => {
  console.log('App Launch')
  checkLogin()
  initInspection()
  initLocationReport()
})

onShow(() => {
  console.log('App Show')
  if (getToken()) {
    locationService.startLocationReport()
  }
})

onHide(() => {
  console.log('App Hide')
  locationService.stopLocationReport()
})

function checkLogin() {
  const token = getToken()
  if (!token) {
    uni.redirectTo({
      url: '/pages/login/login'
    })
  }
}

async function initInspection() {
  try {
    await db.openDB()
    console.log('SQLite数据库初始化成功')
    
    syncService.init()
    console.log('离线同步服务初始化成功')
  } catch (e) {
    console.error('巡检模块初始化失败:', e)
  }
}

function initLocationReport() {
  const token = getToken()
  if (!token) {
    return
  }
  // #ifdef APP-PLUS
  locationService.startLocationReport()
  console.log('位置上报服务已启动')
  // #endif
}
</script>

<style>
page {
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>
