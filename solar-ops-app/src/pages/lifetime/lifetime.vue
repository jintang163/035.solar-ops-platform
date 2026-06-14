<template>
  <view class="lifetime-page">
    <view class="header">
      <view class="header-bg"></view>
      <view class="header-content">
        <view class="header-title">❤️ 设备生命周期预测</view>
        <view class="header-sub" v-if="predictionData">
          <text class="health-text">健康度 {{ (predictionData.currentHealthScore * 100).toFixed(1) }}%</text>
        </view>
      </view>
    </view>

    <view class="summary-section">
      <view class="summary-card">
        <view class="summary-item">
          <view class="summary-icon health-icon">❤️</view>
          <view class="summary-label">当前健康度</view>
          <view class="summary-value" :class="getHealthClass(predictionData?.currentHealthScore)">
            {{ predictionData ? (predictionData.currentHealthScore * 100).toFixed(0) : '--' }}
            <text class="summary-unit">%</text>
          </view>
          <view class="summary-desc">{{ predictionData?.healthLevelDesc || '--' }}</view>
        </view>
        <view class="summary-divider"></view>
        <view class="summary-item">
          <view class="summary-icon life-icon">⏳</view>
          <view class="summary-label">剩余寿命</view>
          <view class="summary-value" :class="{ 'warn': predictionData?.remainingLifeDays <= 180, 'bad': predictionData?.remainingLifeDays <= 90 }">
            {{ predictionData?.remainingLifeDays || '--' }}
            <text class="summary-unit">天</text>
          </view>
          <view class="summary-desc">{{ predictionData?.remainingLifeDesc || '--' }}</view>
        </view>
        <view class="summary-divider"></view>
        <view class="summary-item">
          <view class="summary-icon alert-icon">⚠️</view>
          <view class="summary-label">预警级别</view>
          <view class="summary-value" :class="getAlertClass(predictionData?.alertLevel)">
            {{ predictionData?.alertLevelDesc || '--' }}
          </view>
          <view class="summary-desc">{{ predictionData?.replacementAdvice ? '建议更换' : '正常运行' }}</view>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <view class="section-title">健康度趋势</view>
        <view class="legend">
          <view class="legend-item">
            <view class="legend-dot legend-dot--green"></view>
            <text>健康度</text>
          </view>
          <view class="legend-item">
            <view class="legend-dot legend-dot--blue"></view>
            <text>置信度</text>
          </view>
        </view>
      </view>

      <view class="chart-card">
        <view class="chart-canvas">
          <view class="chart-y-axis">
            <text v-for="(label, idx) in yAxisLabels" :key="idx">{{ label }}</text>
          </view>
          <view class="chart-area">
            <view class="chart-grid">
              <view class="grid-line" v-for="i in 5" :key="i"></view>
            </view>
            <view class="threshold-line threshold-warning">
              <text class="threshold-label">注意</text>
            </view>
            <view class="threshold-line threshold-danger">
              <text class="threshold-label">危险</text>
            </view>
            <view class="chart-bars">
              <view 
                v-for="(point, idx) in chartPoints" 
                :key="idx"
                class="chart-point-wrapper"
              >
                <view 
                  class="health-point"
                  :style="{ bottom: point.healthPos + 'rpx' }"
                ></view>
                <view 
                  class="confidence-point"
                  :style="{ bottom: point.confidencePos + 'rpx' }"
                  v-if="idx % 3 === 0"
                ></view>
                <text class="point-label">{{ point.label }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <view class="section-title">备件更换建议</view>
      </view>

      <view class="advice-card" v-if="sparePartData?.warnings?.length > 0">
        <view class="advice-warning" :class="'warning--' + sparePartData.warnings[0].level">
          <view class="warning-icon">⚠️</view>
          <view class="warning-content">
            <view class="warning-title">{{ sparePartData.warnings[0].message }}</view>
            <view class="warning-spare">建议备件: {{ sparePartData.warnings[0].sparePart }}</view>
          </view>
        </view>
      </view>

      <view class="suggestion-list" v-if="sparePartData?.suggestions?.length > 0">
        <view class="suggestion-item" v-for="(item, idx) in sparePartData.suggestions" :key="idx">
          <view class="suggestion-icon">🔧</view>
          <view class="suggestion-content">
            <view class="suggestion-title">{{ item.component }}</view>
            <view class="suggestion-reason">{{ item.reason }}</view>
            <view class="suggestion-action">{{ item.recommendation }}</view>
            <view class="suggestion-cost">预估费用: {{ item.estimatedCost }}</view>
          </view>
        </view>
      </view>

      <view class="empty-state" v-else-if="!sparePartData?.suggestions?.length">
        <text class="empty-icon">✅</text>
        <text class="empty-text">设备状态良好，暂无备件更换建议</text>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <view class="section-title">寿命预警</view>
        <view class="alert-count" v-if="alertList.length > 0">
          <text>{{ alertList.length }}条</text>
        </view>
      </view>

      <view class="alert-list" v-if="alertList.length > 0">
        <view class="alert-card" v-for="item in alertList" :key="item.id">
          <view class="alert-header">
            <view class="alert-level" :class="'alert-level--' + getLevelClass(item.alertLevel)">
              {{ getLevelText(item.alertLevel) }}
            </view>
            <view class="alert-type" :class="'type--' + getTypeClass(item.alertType)">
              {{ getTypeText(item.alertType) }}
            </view>
            <text class="alert-time">{{ formatTime(item.alertTime) }}</text>
          </view>
          <view class="alert-title">{{ item.alertTitle }}</view>
          <view class="alert-content">{{ item.alertContent }}</view>
          <view class="alert-footer">
            <view class="alert-life">剩余寿命: {{ item.remainingLifeDays || '--' }}天</view>
            <view class="alert-status" :class="'status--' + getStatusClass(item.status)">
              {{ getStatusText(item.status) }}
            </view>
          </view>
        </view>
      </view>

      <view class="empty-state" v-else>
        <text class="empty-icon">✅</text>
        <text class="empty-text">暂无寿命预警</text>
      </view>
    </view>

    <view class="bottom-space"></view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { getLifetimePrediction, getSparePartAdvice, queryLifetimeAlerts } from '@/api/prediction'
import { formatDate } from '@/utils/format'

const DEFAULT_STATION_ID = 1
const DEFAULT_INVERTER_ID = 1

const loading = ref(false)
const predictionData = ref(null)
const sparePartData = ref(null)
const alertList = ref([])

const yAxisLabels = computed(() => ['100%', '75%', '50%', '25%', '0%'])

const chartPoints = computed(() => {
  if (!predictionData.value) return []
  const { timeAxis, healthTrend, confidenceTrend } = predictionData.value
  const maxHeight = 260

  return (timeAxis || []).map((label, idx) => {
    const health = healthTrend?.[idx] || 0
    const confidence = confidenceTrend?.[idx] || 0

    return {
      label: label.split('-')[1] + '日',
      healthPos: Math.round(health * maxHeight),
      confidencePos: Math.round(confidence * maxHeight)
    }
  })
})

function getHealthClass(score) {
  if (score >= 0.7) return 'good'
  if (score >= 0.5) return 'warn'
  return 'bad'
}

function getAlertClass(level) {
  if (level >= 3) return 'bad'
  if (level >= 2) return 'warn'
  return 'good'
}

function getLevelClass(level) {
  return { 1: 'low', 2: 'medium', 3: 'high', 4: 'urgent' }[level] || 'low'
}

function getLevelText(level) {
  return { 1: '低', 2: '中', 3: '高', 4: '紧急' }[level] || '低'
}

function getTypeClass(type) {
  return { 1: 'life', 2: 'spare' }[type] || 'life'
}

function getTypeText(type) {
  return { 1: '寿命预警', 2: '备件建议' }[type] || '未知'
}

function getStatusClass(status) {
  return { 0: 'pending', 1: 'handled', 2: 'ignored' }[status] || 'pending'
}

function getStatusText(status) {
  return { 0: '未处理', 1: '已处理', 2: '已忽略' }[status] || '未知'
}

function formatTime(t) {
  if (!t) return '-'
  return formatDate(new Date(t), 'MM-DD HH:mm')
}

async function fetchData() {
  loading.value = true
  try {
    const results = await Promise.allSettled([
      getLifetimePrediction({ stationId: DEFAULT_STATION_ID, inverterId: DEFAULT_INVERTER_ID, forecastDays: 90 }),
      getSparePartAdvice({ stationId: DEFAULT_STATION_ID, inverterId: DEFAULT_INVERTER_ID }),
      queryLifetimeAlerts({ stationId: DEFAULT_STATION_ID, pageNum: 1, pageSize: 5 })
    ])

    if (results[0].status === 'fulfilled') {
      predictionData.value = results[0].value?.data || results[0].value
    }
    if (results[1].status === 'fulfilled') {
      sparePartData.value = results[1].value?.data || results[1].value
    }
    if (results[2].status === 'fulfilled') {
      alertList.value = results[2].value?.list || results[2].value || []
    }
  } catch (err) {
    console.error('获取寿命预测数据失败:', err)
  } finally {
    loading.value = false
  }
}

function onRefresh() {
  fetchData().finally(() => {
    uni.stopPullDownRefresh()
    uni.showToast({ title: '刷新成功', icon: 'success' })
  })
}

onMounted(() => {
  fetchData()
})

onPullDownRefresh(() => {
  onRefresh()
})
</script>

<style lang="scss" scoped>
.lifetime-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.header {
  position: relative;
  padding-top: 80rpx;
  padding-bottom: 80rpx;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 300rpx;
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
  border-radius: 0 0 40rpx 40rpx;
}

.header-content {
  position: relative;
  z-index: 1;
  padding: 0 30rpx;
  color: #ffffff;
}

.header-title {
  font-size: 36rpx;
  font-weight: 600;
  margin-bottom: 16rpx;
}

.header-sub {
  display: flex;
  align-items: center;
  gap: 24rpx;
  font-size: 26rpx;
  opacity: 0.9;
}

.summary-section {
  padding: 0 30rpx;
  margin-top: -50rpx;
  position: relative;
  z-index: 2;
}

.summary-card {
  display: flex;
  align-items: stretch;
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 30rpx 0;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
}

.summary-item {
  flex: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.summary-icon {
  font-size: 40rpx;
  margin-bottom: 12rpx;
}

.summary-label {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 8rpx;
}

.summary-value {
  font-size: 36rpx;
  font-weight: 600;
  color: #333;

  &.good { color: #52c41a; }
  &.warn { color: #faad14; }
  &.bad { color: #ff4d4f; }
}

.summary-unit {
  font-size: 22rpx;
  font-weight: 400;
  color: #999;
  margin-left: 4rpx;
}

.summary-desc {
  font-size: 22rpx;
  color: #999;
  margin-top: 8rpx;
}

.summary-divider {
  width: 1rpx;
  background-color: #f0f0f0;
  margin: 10rpx 0;
}

.section {
  padding: 30rpx;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
  position: relative;
  padding-left: 20rpx;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 8rpx;
    height: 32rpx;
    background-color: #52c41a;
    border-radius: 4rpx;
  }
}

.legend {
  display: flex;
  gap: 24rpx;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 22rpx;
  color: #666;
}

.legend-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;

  &--green { background-color: #52c41a; }
  &--blue { background-color: #1890ff; }
}

.chart-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.chart-canvas {
  display: flex;
  height: 360rpx;
}

.chart-y-axis {
  width: 70rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding-right: 12rpx;

  text {
    font-size: 20rpx;
    color: #999;
    text-align: right;
  }
}

.chart-area {
  flex: 1;
  position: relative;
}

.chart-grid {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 40rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.grid-line {
  height: 1rpx;
  background-color: #f5f5f5;
}

.threshold-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 1rpx;
  border-top: 2rpx dashed;

  .threshold-label {
    position: absolute;
    right: -10rpx;
    top: -20rpx;
    font-size: 18rpx;
    padding: 2rpx 8rpx;
    border-radius: 4rpx;
    color: #fff;
  }

  &.threshold-warning {
    bottom: calc(40rpx + 260rpx * 0.7);
    border-color: #faad14;

    .threshold-label {
      background-color: #faad14;
    }
  }

  &.threshold-danger {
    bottom: calc(40rpx + 260rpx * 0.3);
    border-color: #ff4d4f;

    .threshold-label {
      background-color: #ff4d4f;
    }
  }
}

.chart-bars {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding-bottom: 40rpx;
}

.chart-point-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  height: 320rpx;
}

.health-point {
  position: absolute;
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background-color: #52c41a;
  box-shadow: 0 0 0 4rpx rgba(82, 196, 26, 0.2);
}

.confidence-point {
  position: absolute;
  width: 6rpx;
  height: 6rpx;
  border-radius: 50%;
  background-color: #1890ff;
}

.point-label {
  position: absolute;
  bottom: 0;
  font-size: 18rpx;
  color: #999;
}

.advice-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
  margin-bottom: 20rpx;
}

.advice-warning {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  padding: 20rpx;
  border-radius: 12rpx;

  &.warning--critical {
    background-color: #fff1f0;
    border-left: 8rpx solid #ff4d4f;
  }

  &.warning--warning {
    background-color: #fffbe6;
    border-left: 8rpx solid #faad14;
  }
}

.warning-icon {
  font-size: 36rpx;
  flex-shrink: 0;
}

.warning-content {
  flex: 1;
}

.warning-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 8rpx;
}

.warning-spare {
  font-size: 24rpx;
  color: #666;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.suggestion-item {
  display: flex;
  gap: 20rpx;
  background-color: #ffffff;
  border-radius: 12rpx;
  padding: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.suggestion-icon {
  font-size: 32rpx;
  flex-shrink: 0;
  margin-top: 4rpx;
}

.suggestion-content {
  flex: 1;
}

.suggestion-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 8rpx;
}

.suggestion-reason {
  font-size: 24rpx;
  color: #666;
  margin-bottom: 6rpx;
}

.suggestion-action {
  font-size: 24rpx;
  color: #1890ff;
  margin-bottom: 6rpx;
}

.suggestion-cost {
  font-size: 24rpx;
  color: #fa8c16;
  font-weight: 500;
}

.alert-count {
  font-size: 24rpx;
  color: #ff4d4f;
  background-color: #fff1f0;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.alert-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
  border-left: 8rpx solid #ff4d4f;
}

.alert-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 12rpx;
}

.alert-level {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  color: #ffffff;

  &--low { background-color: #1890ff; }
  &--medium { background-color: #faad14; }
  &--high { background-color: #ff7a45; }
  &--urgent { background-color: #ff4d4f; }
}

.alert-type {
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;

  &.type--life {
    background-color: #fff1f0;
    color: #ff4d4f;
  }
  &.type--spare {
    background-color: #fff7e6;
    color: #fa8c16;
  }
}

.alert-time {
  margin-left: auto;
  font-size: 22rpx;
  color: #999;
}

.alert-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 8rpx;
}

.alert-content {
  font-size: 24rpx;
  color: #666;
  line-height: 1.6;
  margin-bottom: 12rpx;
}

.alert-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.alert-life {
  font-size: 24rpx;
  color: #999;
}

.alert-status {
  font-size: 24rpx;

  &.status--pending { color: #ff4d4f; }
  &.status--handled { color: #52c41a; }
  &.status--ignored { color: #999; }
}

.empty-state {
  text-align: center;
  padding: 60rpx 0;
  background-color: #ffffff;
  border-radius: 16rpx;

  .empty-icon {
    font-size: 64rpx;
    display: block;
    margin-bottom: 16rpx;
  }

  .empty-text {
    font-size: 26rpx;
    color: #999;
  }
}

.bottom-space {
  height: 40rpx;
}
</style>
