<template>
  <view class="cleaning-dashboard-page">
    <view class="header-section">
      <view class="header-bg"></view>
      <view class="header-content">
        <text class="header-title">清洗管理</text>
        <text class="header-subtitle">智能检测 · 提升发电效率</text>
      </view>
      <view class="quick-actions">
        <view class="action-item" @click="goToCalendar">
          <view class="action-icon icon-blue">📅</view>
          <text class="action-text">日历</text>
        </view>
        <view class="action-item" @click="goToReminder">
          <view class="action-icon icon-orange">
            <text v-if="unhandledCount > 0" class="badge">{{ unhandledCount > 99 ? '99+' : unhandledCount }}</text>
            🔔
          </view>
          <text class="action-text">清洗提醒</text>
        </view>
        <view class="action-item" @click="goToCreatePlan">
          <view class="action-icon icon-green">➕</view>
          <text class="action-text">新建计划</text>
        </view>
        <view class="action-item" @click="goToPlanList">
          <view class="action-icon icon-purple">📋</view>
          <text class="action-text">计划列表</text>
        </view>
      </view>
    </view>

    <view class="stats-grid">
      <view class="stat-card">
        <view class="stat-label">累计清洗</view>
        <view class="stat-value blue">{{ dashboard?.totalCleaningCount || 0 }}</view>
        <view class="stat-unit">次</view>
      </view>
      <view class="stat-card">
        <view class="stat-label">本月清洗</view>
        <view class="stat-value purple">{{ dashboard?.monthlyCleaningCount || 0 }}</view>
        <view class="stat-unit">次</view>
      </view>
      <view class="stat-card">
        <view class="stat-label">待执行</view>
        <view class="stat-value orange">{{ dashboard?.pendingPlanCount || 0 }}</view>
        <view class="stat-unit">个</view>
      </view>
      <view class="stat-card">
        <view class="stat-label">执行中</view>
        <view class="stat-value cyan">{{ dashboard?.inProgressPlanCount || 0 }}</view>
        <view class="stat-unit">个</view>
      </view>
    </view>

    <view class="energy-card">
      <view class="energy-header">
        <view class="energy-title">
          <text class="energy-icon">⚡</text>
          <text>发电量提升</text>
        </view>
        <view class="energy-date">累计数据</view>
      </view>
      <view class="energy-main">
        <view class="energy-value">
          <text class="energy-number">{{ formatNumber(dashboard?.totalImprovedEnergy) }}</text>
          <text class="energy-unit">kWh</text>
        </view>
        <view class="energy-tag green">+{{ improvementRate }}%</view>
      </view>
      <view class="energy-details">
        <view class="detail-item">
          <view class="detail-label">本月提升</view>
          <view class="detail-value green">{{ formatNumber(dashboard?.monthlyImprovedEnergy) }} kWh</view>
        </view>
        <view class="divider"></view>
        <view class="detail-item">
          <view class="detail-label">节省费用</view>
          <view class="detail-value pink">¥ {{ formatNumber(dashboard?.totalSavedCost) }}</view>
        </view>
      </view>
    </view>

    <view class="dust-section" v-if="dustLevels.length > 0">
      <view class="section-header">
        <text class="section-title">🏭 积灰分布（近7日）</text>
      </view>
      <view class="dust-bars">
        <view class="dust-bar-item" v-for="item in dustLevels" :key="item.dustLevel">
          <view class="dust-label">{{ item.dustLevelDesc }}</view>
          <view class="dust-bar-bg">
            <view class="dust-bar-fill" :style="{ width: (item.ratio * 100) + '%', backgroundColor: item.color }"></view>
          </view>
          <view class="dust-count" :style="{ color: item.color }">{{ item.count }}次</view>
        </view>
      </view>
    </view>

    <view class="recent-section">
      <view class="section-header">
        <text class="section-title">📌 最近清洗计划</text>
        <text class="section-more" @click="goToPlanList">查看全部 ›</text>
      </view>
      <view class="plan-list" v-if="recentPlans.length > 0">
        <view class="plan-card" v-for="plan in recentPlans" :key="plan.id" @click="goToPlanDetail(plan.id)">
          <view class="plan-status" :class="getStatusClass(plan.status)">
            {{ getStatusText(plan.status) }}
          </view>
          <view class="plan-content">
            <view class="plan-title">{{ plan.title }}</view>
            <view class="plan-meta">
              <text class="meta-item">🏢 {{ plan.stationName || '未知电站' }}</text>
              <text class="meta-item">📍 {{ plan.arrayNumber || plan.inverterName || '-' }}</text>
            </view>
            <view class="plan-footer">
              <text class="plan-date">📅 {{ plan.planDate }}</text>
              <text class="plan-owner">👤 {{ plan.ownerName || '-' }}</text>
            </view>
          </view>
          <view class="plan-improvement" v-if="plan.status === 2 && plan.improvedEnergy > 0">
            <text class="improvement-tag">+{{ plan.improvedEnergy }} kWh</text>
          </view>
        </view>
      </view>
      <view class="empty-tip" v-else>
        <text class="empty-icon">📋</text>
        <text class="empty-text">暂无清洗计划</text>
        <view class="create-btn" @click="goToCreatePlan">
          <text>去创建</text>
        </view>
      </view>
    </view>

    <view class="bottom-space"></view>
  </view>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  getCleaningDashboard,
  getCleaningPlanList,
  getReminderPage,
  getDustLevelStats
} from '@/api/cleaning'

const loading = ref(false)
const dashboard = ref(null)
const recentPlans = ref([])
const unhandledCount = ref(0)
const dustLevels = ref([])

const formatNumber = (num) => {
  if (num == null) return '0'
  const n = Number(num)
  if (n >= 10000) {
    return (n / 10000).toFixed(1) + '万'
  }
  return n.toFixed(1)
}

const improvementRate = computed(() => {
  if (!dashboard.value) return '0.0'
  const improved = Number(dashboard.value.totalImprovedEnergy || 0)
  if (improved <= 0) return '0.0'
  return Math.min(100, (improved / 1000)).toFixed(1)
})

const getStatusClass = (status) => {
  const map = {
    0: 'status-pending',
    1: 'status-progress',
    2: 'status-done',
    3: 'status-cancel'
  }
  return map[status] || 'status-pending'
}

const getStatusText = (status) => {
  const map = { 0: '待执行', 1: '执行中', 2: '已完成', 3: '已取消' }
  return map[status] || '未知'
}

async function fetchDashboard() {
  try {
    const data = await getCleaningDashboard()
    dashboard.value = data || {}
  } catch (err) {
    console.error('获取仪表盘数据失败:', err)
  }
}

async function fetchRecentPlans() {
  try {
    const res = await getCleaningPlanList({ pageNum: 1, pageSize: 5 })
    recentPlans.value = res?.list || res || []
  } catch (err) {
    console.error('获取最近计划失败:', err)
    recentPlans.value = []
  }
}

async function fetchUnhandledCount() {
  try {
    const res = await getReminderPage({ pageNum: 1, pageSize: 1, status: 0 })
    unhandledCount.value = res?.total || 0
  } catch (err) {
    console.error('获取未处理提醒数失败:', err)
  }
}

async function fetchDustLevels() {
  try {
    const data = await getDustLevelStats()
    dustLevels.value = data || []
  } catch (err) {
    console.error('获取积灰分布失败:', err)
    dustLevels.value = []
  }
}

async function fetchAll() {
  loading.value = true
  try {
    await Promise.all([
      fetchDashboard(),
      fetchRecentPlans(),
      fetchUnhandledCount(),
      fetchDustLevels()
    ])
  } finally {
    loading.value = false
  }
}

function goToCalendar() {
  uni.navigateTo({ url: '/pages/cleaning/calendar' })
}

function goToReminder() {
  uni.navigateTo({ url: '/pages/cleaning/reminder' })
}

function goToCreatePlan() {
  uni.navigateTo({ url: '/pages/cleaning/plan-edit' })
}

function goToPlanList() {
  uni.navigateTo({ url: '/pages/cleaning/plan-list' })
}

function goToPlanDetail(id) {
  uni.navigateTo({ url: `/pages/cleaning/plan-detail?id=${id}` })
}

onShow(() => {
  fetchAll()
})

onMounted(() => {
  fetchAll()
})
</script>

<style lang="scss" scoped>
.cleaning-dashboard-page {
  min-height: 100vh;
  background-color: #f5f6fa;
}

.header-section {
  position: relative;
  padding-bottom: 40rpx;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 360rpx;
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
  border-bottom-left-radius: 40rpx;
  border-bottom-right-radius: 40rpx;
}

.header-content {
  position: relative;
  padding: 60rpx 40rpx 40rpx;
  color: #fff;

  .header-title {
    display: block;
    font-size: 44rpx;
    font-weight: 700;
    margin-bottom: 10rpx;
  }

  .header-subtitle {
    font-size: 26rpx;
    opacity: 0.85;
  }
}

.quick-actions {
  position: relative;
  display: flex;
  justify-content: space-around;
  margin: 0 30rpx;
  padding: 30rpx 20rpx;
  background-color: #fff;
  border-radius: 20rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
}

.action-item {
  text-align: center;

  .action-icon {
    width: 90rpx;
    height: 90rpx;
    line-height: 90rpx;
    border-radius: 50%;
    font-size: 44rpx;
    margin: 0 auto 14rpx;
    position: relative;

    &.icon-blue { background-color: #e6f7ff; }
    &.icon-orange { background-color: #fff7e6; }
    &.icon-green { background-color: #f6ffed; }
    &.icon-purple { background-color: #f9f0ff; }

    .badge {
      position: absolute;
      top: -10rpx;
      right: -10rpx;
      min-width: 36rpx;
      height: 36rpx;
      line-height: 36rpx;
      padding: 0 10rpx;
      background-color: #ff4d4f;
      color: #fff;
      font-size: 20rpx;
      border-radius: 18rpx;
      font-style: normal;
      z-index: 1;
    }
  }

  .action-text {
    font-size: 24rpx;
    color: #666;
  }
}

.stats-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 20rpx 30rpx;
  gap: 20rpx;
}

.stat-card {
  width: calc(50% - 10rpx);
  background-color: #fff;
  border-radius: 16rpx;
  padding: 28rpx 24rpx;
  box-sizing: border-box;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
  position: relative;

  .stat-label {
    font-size: 24rpx;
    color: #999;
    margin-bottom: 14rpx;
  }

  .stat-value {
    font-size: 48rpx;
    font-weight: 700;
    line-height: 1.1;
    margin-bottom: 4rpx;

    &.blue { color: #1890ff; }
    &.purple { color: #722ed1; }
    &.orange { color: #fa8c16; }
    &.cyan { color: #13c2c2; }
  }

  .stat-unit {
    font-size: 22rpx;
    color: #bbb;
    position: absolute;
    right: 24rpx;
    bottom: 28rpx;
  }
}

.energy-card {
  margin: 10rpx 30rpx;
  background: linear-gradient(135deg, #fff7e6 0%, #fffbe6 100%);
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(250, 173, 20, 0.08);

  .energy-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;
  }

  .energy-title {
    display: flex;
    align-items: center;
    font-size: 28rpx;
    font-weight: 600;
    color: #613400;

    .energy-icon {
      margin-right: 10rpx;
    }
  }

  .energy-date {
    font-size: 22rpx;
    color: #ad8b00;
  }

  .energy-main {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-bottom: 24rpx;
  }

  .energy-value {
    display: flex;
    align-items: baseline;
  }

  .energy-number {
    font-size: 64rpx;
    font-weight: 700;
    color: #d46b08;
    line-height: 1;
  }

  .energy-unit {
    font-size: 26rpx;
    color: #d46b08;
    margin-left: 10rpx;
    font-weight: 500;
  }

  .energy-tag {
    padding: 8rpx 20rpx;
    border-radius: 20rpx;
    font-size: 24rpx;
    font-weight: 600;

    &.green {
      background-color: #f6ffed;
      color: #389e0d;
    }
  }

  .energy-details {
    display: flex;
    background-color: rgba(255, 255, 255, 0.6);
    border-radius: 12rpx;
    padding: 20rpx;
  }

  .detail-item {
    flex: 1;
    text-align: center;
  }

  .detail-label {
    font-size: 22rpx;
    color: #8c8c8c;
    margin-bottom: 8rpx;
  }

  .detail-value {
    font-size: 28rpx;
    font-weight: 600;

    &.green { color: #389e0d; }
    &.pink { color: #c41d7f; }
  }

  .divider {
    width: 1rpx;
    background-color: #f0e0b2;
    margin: 0 10rpx;
  }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #262626;
}

.section-more {
  font-size: 24rpx;
  color: #1890ff;
}

.dust-section {
  margin: 20rpx 30rpx;
  background-color: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.dust-bars {
  .dust-bar-item {
    display: flex;
    align-items: center;
    margin-bottom: 20rpx;

    &:last-child { margin-bottom: 0; }
  }

  .dust-label {
    width: 140rpx;
    font-size: 24rpx;
    color: #666;
  }

  .dust-bar-bg {
    flex: 1;
    height: 16rpx;
    background-color: #f5f5f5;
    border-radius: 8rpx;
    overflow: hidden;
    margin: 0 20rpx;
  }

  .dust-bar-fill {
    height: 100%;
    border-radius: 8rpx;
    transition: width 0.3s;
  }

  .dust-count {
    width: 100rpx;
    text-align: right;
    font-size: 24rpx;
    font-weight: 500;
  }
}

.recent-section {
  margin: 20rpx 30rpx 0;
}

.plan-list {
  .plan-card {
    background-color: #fff;
    border-radius: 16rpx;
    padding: 28rpx;
    margin-bottom: 20rpx;
    box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
    position: relative;
    overflow: hidden;
  }

  .plan-status {
    position: absolute;
    top: 28rpx;
    right: 28rpx;
    padding: 6rpx 18rpx;
    border-radius: 6rpx;
    font-size: 22rpx;
    font-weight: 500;

    &.status-pending {
      background-color: #fff7e6;
      color: #fa8c16;
    }
    &.status-progress {
      background-color: #e6f7ff;
      color: #1890ff;
    }
    &.status-done {
      background-color: #f6ffed;
      color: #52c41a;
    }
    &.status-cancel {
      background-color: #f5f5f5;
      color: #8c8c8c;
    }
  }

  .plan-content {
    padding-right: 100rpx;
  }

  .plan-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #262626;
    margin-bottom: 16rpx;
    line-height: 1.4;
  }

  .plan-meta {
    display: flex;
    flex-wrap: wrap;
    margin-bottom: 16rpx;

    .meta-item {
      font-size: 24rpx;
      color: #8c8c8c;
      margin-right: 30rpx;
      margin-bottom: 8rpx;
    }
  }

  .plan-footer {
    display: flex;
    justify-content: space-between;
    font-size: 22rpx;
    color: #999;
  }

  .plan-improvement {
    margin-top: 16rpx;
    padding-top: 16rpx;
    border-top: 1rpx solid #f0f0f0;

    .improvement-tag {
      padding: 8rpx 18rpx;
      background-color: #f6ffed;
      color: #52c41a;
      border-radius: 8rpx;
      font-size: 24rpx;
      font-weight: 600;
    }
  }
}

.empty-tip {
  background-color: #fff;
  border-radius: 16rpx;
  padding: 80rpx 0;
  text-align: center;

  .empty-icon {
    display: block;
    font-size: 80rpx;
    margin-bottom: 20rpx;
  }

  .empty-text {
    display: block;
    font-size: 28rpx;
    color: #999;
    margin-bottom: 30rpx;
  }

  .create-btn {
    display: inline-block;
    padding: 18rpx 50rpx;
    background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
    color: #fff;
    border-radius: 40rpx;
    font-size: 28rpx;
  }
}

.bottom-space {
  height: 60rpx;
}
</style>
