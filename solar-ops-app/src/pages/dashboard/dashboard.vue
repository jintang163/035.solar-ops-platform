<template>
  <view class="dashboard-page">
    <view class="dashboard-header">
      <view class="dashboard-title">运维驾驶舱</view>
      <view class="dashboard-time">{{ currentTime }}</view>
      <view class="dashboard-refresh" @click="loadData">
        <text class="refresh-icon" :class="{ spinning: loading }">⟳</text>
      </view>
    </view>

    <view class="dashboard-content" v-if="!loading && dashboardData">
      <view class="kpi-grid">
        <view class="kpi-card blue">
          <view class="kpi-icon">⚡</view>
          <view class="kpi-info">
            <view class="kpi-value">
              <text class="kpi-num">{{ formatNumber(dashboardData.totalPower) }}</text>
              <text class="kpi-unit">{{ getPowerUnit(dashboardData.totalPower) }}</text>
            </view>
            <view class="kpi-label">实时总功率</view>
            <view class="kpi-trend up" v-if="dashboardData.powerTrend">
              {{ dashboardData.powerTrend === 'up' ? '↑' : '↓' }} {{ dashboardData.powerChangePercent }}%
            </view>
          </view>
        </view>

        <view class="kpi-card green">
          <view class="kpi-icon">📈</view>
          <view class="kpi-info">
            <view class="kpi-value">
              <text class="kpi-num">{{ formatNumber(dashboardData.todayGeneration) }}</text>
              <text class="kpi-unit">{{ getEnergyUnit(dashboardData.todayGeneration) }}</text>
            </view>
            <view class="kpi-label">今日发电量</view>
            <view class="kpi-trend up" v-if="dashboardData.generationTrend">
              {{ dashboardData.generationTrend === 'up' ? '↑' : '↓' }} {{ dashboardData.generationChangePercent }}%
            </view>
          </view>
        </view>

        <view class="kpi-card cyan">
          <view class="kpi-icon">🌿</view>
          <view class="kpi-info">
            <view class="kpi-value">
              <text class="kpi-num">{{ formatNumber(dashboardData.totalEmissionReduction) }}</text>
              <text class="kpi-unit">{{ getWeightUnit(dashboardData.totalEmissionReduction) }}</text>
            </view>
            <view class="kpi-label">累计减排</view>
          </view>
        </view>

        <view class="kpi-card purple">
          <view class="kpi-icon">💚</view>
          <view class="kpi-info">
            <view class="kpi-value">
              <text class="kpi-num">{{ dashboardData.onlineRate?.toFixed(1) || '0' }}</text>
              <text class="kpi-unit">%</text>
            </view>
            <view class="kpi-label">设备在线率</view>
          </view>
        </view>

        <view class="kpi-card orange">
          <view class="kpi-icon">🔔</view>
          <view class="kpi-info">
            <view class="kpi-value">
              <text class="kpi-num">{{ dashboardData.alarmCount || 0 }}</text>
              <text class="kpi-unit">条</text>
            </view>
            <view class="kpi-label">告警数量</view>
          </view>
        </view>

        <view class="kpi-card red">
          <view class="kpi-icon">📋</view>
          <view class="kpi-info">
            <view class="kpi-value">
              <text class="kpi-num">{{ dashboardData.unhandledWorkOrderCount || 0 }}</text>
              <text class="kpi-unit">个</text>
            </view>
            <view class="kpi-label">未处理工单</view>
          </view>
        </view>
      </view>

      <view class="section-card">
        <view class="section-title">电站健康分布</view>
        <view class="health-stats">
          <view
            class="health-stat-item"
            v-for="item in dashboardData.stationHealthStats"
            :key="item.healthLevel"
          >
            <view class="health-stat-bar">
              <view
                class="health-stat-fill"
                :class="item.healthColor"
                :style="{ width: item.percentage + '%' }"
              />
            </view>
            <view class="health-stat-info">
              <view class="health-stat-dot" :class="item.healthColor" />
              <text class="health-stat-label">{{ item.healthLevelDesc }}</text>
              <text class="health-stat-count">{{ item.count }}座</text>
              <text class="health-stat-percent">{{ item.percentage }}%</text>
            </view>
          </view>
        </view>
      </view>

      <view class="section-card" v-if="dashboardData.alarmStations?.length > 0">
        <view class="section-title">告警电站 ({{ dashboardData.alarmStations.length }})</view>
        <view
          class="alarm-station-item"
          v-for="station in dashboardData.alarmStations"
          :key="station.stationId"
        >
          <view class="alarm-station-health" :class="station.healthColor" />
          <view class="alarm-station-info">
            <view class="alarm-station-name">{{ station.stationName }}</view>
            <view class="alarm-station-meta">
              <text class="alarm-level" :class="getAlarmLevelClass(station.maxAlarmLevel)">
                {{ getAlarmLevelText(station.maxAlarmLevel) }}
              </text>
            </view>
          </view>
          <view class="alarm-station-count">
            <text class="alarm-count-num">{{ station.alarmCount }}</text>
            <text class="alarm-count-label">条告警</text>
          </view>
        </view>
      </view>

      <view class="update-time">
        数据更新时间：{{ formatUpdateTime(dashboardData.updateTime) }}
      </view>
    </view>

    <view class="loading-container" v-if="loading">
      <view class="loading-spinner" />
      <text class="loading-text">加载中...</text>
    </view>

    <view class="error-container" v-if="!loading && !dashboardData">
      <text class="error-text">数据加载失败</text>
      <view class="retry-btn" @click="loadData">重新加载</view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, onPullDownRefresh } from 'vue'
import { getMobileDashboard } from '../../api/dashboard'

const loading = ref(false)
const dashboardData = ref(null)
const currentTime = ref('')

const formatNumber = (num) => {
  if (num == null) return '0'
  const n = Number(num)
  if (n >= 10000) {
    return (n / 10000).toFixed(2)
  }
  if (n >= 1000) {
    return (n / 1000).toFixed(1)
  }
  return n.toFixed(0)
}

const getPowerUnit = (num) => {
  if (num >= 10000) return '万kW'
  if (num >= 1000) return 'MW'
  return 'kW'
}

const getEnergyUnit = (num) => {
  if (num >= 10000) return '万kWh'
  if (num >= 1000) return 'MWh'
  return 'kWh'
}

const getWeightUnit = (num) => {
  if (num >= 10000) return '万t'
  return 'tCO₂'
}

const getAlarmLevelClass = (level) => {
  const map = { 1: 'level-low', 2: 'level-medium', 3: 'level-high', 4: 'level-urgent' }
  return map[level] || 'level-low'
}

const getAlarmLevelText = (level) => {
  const map = { 1: '低级', 2: '中级', 3: '高级', 4: '紧急' }
  return map[level] || '未知'
}

const formatUpdateTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const updateCurrentTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMobileDashboard()
    if (res.code === 200) {
      dashboardData.value = res.data
    }
  } catch (err) {
    console.error('加载驾驶舱数据失败:', err)
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

let timer = null

onMounted(() => {
  updateCurrentTime()
  timer = setInterval(updateCurrentTime, 1000)
  loadData()
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})

onPullDownRefresh(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.dashboard-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f5ff 0%, #f6ffed 50%, #e6fffb 100%);
  padding-bottom: 40rpx;
}

.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  background: linear-gradient(90deg, #1890ff 0%, #52c41a 100%);
  color: #fff;
}

.dashboard-title {
  font-size: 36rpx;
  font-weight: bold;
}

.dashboard-time {
  font-size: 24rpx;
  opacity: 0.9;
}

.dashboard-refresh {
  padding: 8rpx 16rpx;
}

.refresh-icon {
  font-size: 32rpx;
  color: #fff;

  &.spinning {
    animation: spin 1s linear infinite;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.dashboard-content {
  padding: 24rpx;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
  margin-bottom: 24rpx;
}

.kpi-card {
  display: flex;
  align-items: center;
  padding: 28rpx;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 6rpx;
  }

  &.blue::before { background: #1890ff; }
  &.green::before { background: #52c41a; }
  &.cyan::before { background: #13c2c2; }
  &.purple::before { background: #722ed1; }
  &.orange::before { background: #faad14; }
  &.red::before { background: #ff4d4f; }
}

.kpi-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  margin-right: 20rpx;
  flex-shrink: 0;

  .blue & { background: rgba(24, 144, 255, 0.15); }
  .green & { background: rgba(82, 196, 26, 0.15); }
  .cyan & { background: rgba(19, 194, 194, 0.15); }
  .purple & { background: rgba(114, 46, 209, 0.15); }
  .orange & { background: rgba(250, 173, 20, 0.15); }
  .red & { background: rgba(255, 77, 79, 0.15); }
}

.kpi-info {
  flex: 1;
  min-width: 0;
}

.kpi-value {
  display: flex;
  align-items: baseline;
  margin-bottom: 6rpx;
}

.kpi-num {
  font-size: 38rpx;
  font-weight: bold;
  color: #333;
  line-height: 1.2;
}

.kpi-unit {
  font-size: 22rpx;
  color: #999;
  margin-left: 6rpx;
}

.kpi-label {
  font-size: 24rpx;
  color: #666;
  margin-bottom: 4rpx;
}

.kpi-trend {
  font-size: 20rpx;

  &.up { color: #52c41a; }
  &.down { color: #ff4d4f; }
}

.section-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 24rpx;
  padding-left: 16rpx;
  border-left: 6rpx solid #1890ff;
}

.health-stats {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.health-stat-item {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.health-stat-bar {
  height: 16rpx;
  background: #f5f5f5;
  border-radius: 8rpx;
  overflow: hidden;
}

.health-stat-fill {
  height: 100%;
  border-radius: 8rpx;
  transition: width 0.5s ease;

  &.green { background: linear-gradient(90deg, #52c41a, #95de64); }
  &.yellow { background: linear-gradient(90deg, #faad14, #ffd666); }
  &.red { background: linear-gradient(90deg, #ff4d4f, #ff7875); }
}

.health-stat-info {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.health-stat-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;

  &.green { background: #52c41a; }
  &.yellow { background: #faad14; }
  &.red { background: #ff4d4f; }
}

.health-stat-label {
  font-size: 26rpx;
  color: #333;
}

.health-stat-count {
  font-size: 24rpx;
  color: #666;
  margin-left: auto;
}

.health-stat-percent {
  font-size: 24rpx;
  color: #1890ff;
  font-weight: 500;
}

.alarm-station-item {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.alarm-station-health {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  margin-right: 20rpx;
  flex-shrink: 0;

  &.green { background: #52c41a; }
  &.yellow { background: #faad14; }
  &.red { background: #ff4d4f; }
}

.alarm-station-info {
  flex: 1;
  min-width: 0;
}

.alarm-station-name {
  font-size: 28rpx;
  color: #333;
  margin-bottom: 6rpx;
}

.alarm-station-meta {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.alarm-level {
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  font-size: 20rpx;

  &.level-low { background: rgba(82, 196, 26, 0.15); color: #52c41a; }
  &.level-medium { background: rgba(250, 173, 20, 0.15); color: #faad14; }
  &.level-high { background: rgba(255, 77, 79, 0.15); color: #ff4d4f; }
  &.level-urgent { background: rgba(255, 77, 79, 0.3); color: #ff4d4f; }
}

.alarm-station-count {
  text-align: right;
  flex-shrink: 0;
}

.alarm-count-num {
  font-size: 32rpx;
  font-weight: bold;
  color: #ff4d4f;
}

.alarm-count-label {
  font-size: 22rpx;
  color: #999;
  margin-left: 4rpx;
}

.update-time {
  text-align: center;
  font-size: 22rpx;
  color: #999;
  margin-top: 32rpx;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;
}

.loading-spinner {
  width: 60rpx;
  height: 60rpx;
  border: 4rpx solid #f0f0f0;
  border-top-color: #1890ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 20rpx;
}

.loading-text {
  font-size: 26rpx;
  color: #999;
}

.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;
}

.error-text {
  font-size: 28rpx;
  color: #999;
  margin-bottom: 32rpx;
}

.retry-btn {
  padding: 16rpx 48rpx;
  background: #1890ff;
  color: #fff;
  border-radius: 40rpx;
  font-size: 26rpx;
}
</style>
