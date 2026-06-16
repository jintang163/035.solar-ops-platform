<template>
  <view class="mine-page">
    <view class="user-header">
      <view class="user-bg"></view>
      <view class="user-info">
        <view class="avatar">
          <text class="avatar-icon">👤</text>
        </view>
        <view class="user-detail">
          <view class="username">{{ userInfo.username || '运维人员' }}</view>
          <view class="user-role">{{ userInfo.roleName || '运维工程师' }}</view>
        </view>
        <view class="setting-btn" @click="goToSetting">
          <text>⚙️</text>
        </view>
      </view>
    </view>
    
    <view class="stats-card">
      <view class="stat-item">
        <view class="stat-num">{{ userStats.totalOrders }}</view>
        <view class="stat-label">总工单</view>
      </view>
      <view class="stat-item">
        <view class="stat-num">{{ userStats.completedOrders }}</view>
        <view class="stat-label">已完成</view>
      </view>
      <view class="stat-item">
        <view class="stat-num">{{ userStats.processingOrders }}</view>
        <view class="stat-label">处理中</view>
      </view>
    </view>
    
    <view class="menu-section">
      <view class="menu-group">
        <view class="menu-item" @click="goToPage('workorder')">
          <view class="menu-icon">📋</view>
          <view class="menu-title">我的工单</view>
          <view class="menu-arrow">›</view>
        </view>
        <view class="menu-item" @click="goToPage('device')">
          <view class="menu-icon">📟</view>
          <view class="menu-title">我的设备</view>
          <view class="menu-arrow">›</view>
        </view>
        <view class="menu-item" @click="goToPage('station')">
          <view class="menu-icon">🏭</view>
          <view class="menu-title">电站管理</view>
          <view class="menu-arrow">›</view>
        </view>
        <view class="menu-item" @click="goToPage('drone')">
          <view class="menu-icon">🚁</view>
          <view class="menu-title">无人机巡检</view>
          <view class="menu-arrow">›</view>
        </view>
        <view class="menu-item" @click="goToPage('station-compare')">
          <view class="menu-icon">📊</view>
          <view class="menu-title">电站对比</view>
          <view class="menu-arrow">›</view>
        </view>
        <view class="menu-item" @click="goToPage('knowledge')">
          <view class="menu-icon">📚</view>
          <view class="menu-title">运维知识库</view>
          <view class="menu-tag">智能推荐</view>
          <view class="menu-arrow">›</view>
        </view>
      </view>
      
      <view class="menu-group">
        <view class="menu-item" @click="goToPage('message')">
          <view class="menu-icon">🔔</view>
          <view class="menu-title">消息通知</view>
          <view class="menu-badge">3</view>
          <view class="menu-arrow">›</view>
        </view>
        <view class="menu-item" @click="goToPage('report')">
          <view class="menu-icon">📊</view>
          <view class="menu-title">数据报表</view>
          <view class="menu-arrow">›</view>
        </view>
      </view>
      
      <view class="menu-group">
        <view class="menu-item" @click="goToPage('about')">
          <view class="menu-icon">ℹ️</view>
          <view class="menu-title">关于我们</view>
          <view class="menu-arrow">›</view>
        </view>
        <view class="menu-item" @click="goToPage('feedback')">
          <view class="menu-icon">💬</view>
          <view class="menu-title">意见反馈</view>
          <view class="menu-arrow">›</view>
        </view>
      </view>
    </view>
    
    <view class="logout-section">
      <view class="logout-btn" @click="handleLogout">
        退出登录
      </view>
    </view>
    
    <view class="version-info">
      <text>版本 v1.0.0</text>
    </view>
    
    <view class="bottom-space"></view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getUserInfo, logout } from '@/api/auth'
import { removeToken, getUserInfo as getStoredUserInfo, setUserInfo } from '@/utils/auth'

const userInfo = reactive({
  username: '',
  roleName: '',
  phone: '',
  avatar: ''
})

const userStats = reactive({
  totalOrders: 0,
  completedOrders: 0,
  processingOrders: 0
})

async function fetchUserInfo() {
  try {
    const data = await getUserInfo()
    if (data) {
      Object.assign(userInfo, data)
      setUserInfo(data)
    }
  } catch (err) {
    console.error('获取用户信息失败:', err)
    const stored = getStoredUserInfo()
    if (stored?.username) {
      userInfo.username = stored.username
    } else {
      userInfo.username = '运维人员'
    }
    userInfo.roleName = '运维工程师'
  }
}

function fetchUserStats() {
  userStats.totalOrders = 48
  userStats.completedOrders = 35
  userStats.processingOrders = 5
}

function goToSetting() {
  uni.showToast({ title: '设置', icon: 'none' })
}

function goToPage(page) {
  const pageMap = {
    workorder: '/pages/workorder/workorder',
    drone: '/pages/drone/drone',
    knowledge: '/pages/knowledge/knowledge',
    'station-compare': '/pages/station/station-compare'
  }
  if (pageMap[page]) {
    if (page === 'workorder') {
      uni.switchTab({ url: pageMap[page] })
    } else {
      uni.navigateTo({ url: pageMap[page] })
    }
  } else {
    uni.showToast({ title: '功能开发中', icon: 'none' })
  }
}

function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await logout()
        } catch (err) {
          console.error('退出登录失败:', err)
        }
        removeToken()
        uni.redirectTo({
          url: '/pages/login/login'
        })
      }
    }
  })
}

onMounted(() => {
  fetchUserInfo()
  fetchUserStats()
})
</script>

<style lang="scss" scoped>
.mine-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.user-header {
  position: relative;
  padding-top: 80rpx;
  padding-bottom: 80rpx;
}

.user-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 320rpx;
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
}

.user-info {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  padding: 0 30rpx;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  background-color: #ffffff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
}

.avatar-icon {
  font-size: 56rpx;
}

.user-detail {
  flex: 1;
  color: #ffffff;
}

.username {
  font-size: 36rpx;
  font-weight: 600;
  margin-bottom: 10rpx;
}

.user-role {
  font-size: 26rpx;
  opacity: 0.8;
}

.setting-btn {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
}

.stats-card {
  display: flex;
  margin: -50rpx 30rpx 30rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 30rpx 0;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 2;
}

.stat-item {
  flex: 1;
  text-align: center;
  border-right: 1rpx solid #f0f0f0;
  
  &:last-child {
    border-right: none;
  }
}

.stat-num {
  font-size: 40rpx;
  font-weight: 600;
  color: #1890ff;
  margin-bottom: 10rpx;
}

.stat-label {
  font-size: 24rpx;
  color: #999;
}

.menu-section {
  padding: 0 30rpx;
}

.menu-group {
  background-color: #ffffff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
  
  &:last-child {
    border-bottom: none;
  }
}

.menu-icon {
  font-size: 36rpx;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.menu-title {
  flex: 1;
  font-size: 30rpx;
  color: #333;
}

.menu-tag {
  font-size: 20rpx;
  color: #722ed1;
  background: #f9f0ff;
  padding: 4rpx 14rpx;
  border-radius: 20rpx;
  margin-right: 12rpx;
}

.menu-badge {
  min-width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  padding: 0 12rpx;
  background-color: #ff4d4f;
  color: #ffffff;
  font-size: 22rpx;
  border-radius: 18rpx;
  text-align: center;
  margin-right: 16rpx;
}

.menu-arrow {
  font-size: 36rpx;
  color: #ccc;
}

.logout-section {
  padding: 30rpx;
  margin-top: 20rpx;
}

.logout-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background-color: #ffffff;
  color: #ff4d4f;
  font-size: 30rpx;
  border-radius: 16rpx;
}

.version-info {
  text-align: center;
  padding: 30rpx;
  font-size: 24rpx;
  color: #ccc;
}

.bottom-space {
  height: 60rpx;
}
</style>
