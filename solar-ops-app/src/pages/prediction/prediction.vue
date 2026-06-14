<template>
  <view class="prediction-page">
    <view class="header">
      <view class="header-bg"></view>
      <view class="header-content">
        <view class="header-title">⚡ 功率预测趋势</view>
        <view class="header-sub" v-if="weatherData">
          <text class="weather-text">{{ weatherData.weather }}</text>
          <text class="temp-text">{{ weatherData.temperature }}℃</text>
          <text class="irr-text">{{ weatherData.irradiance }} W/m²</text>
        </view>
      </view>
    </view>

    <view class="summary-section">
      <view class="summary-card">
        <view class="summary-item">
          <view class="summary-label">预测准确率</view>
          <view class="summary-value" :class="{ 'good': summary?.avgAccuracy >= 85, 'warn': summary?.avgAccuracy < 85 && summary?.avgAccuracy >= 70, 'bad': summary?.avgAccuracy < 70 }">
            {{ summary?.avgAccuracy || '--' }}%
          </view>
        </view>
        <view class="summary-divider"></view>
        <view class="summary-item">
          <view class="summary-label">今日预测发电</view>
          <view class="summary-value summary-value--blue">
            {{ summary?.todayPredictedEnergy || '0.00' }}
            <text class="summary-unit">kWh</text>
          </view>
        </view>
        <view class="summary-divider"></view>
        <view class="summary-item">
          <view class="summary-label">偏差告警</view>
          <view class="summary-value" :class="{ 'bad': summary?.pendingAlertCount > 0 }">
            {{ summary?.pendingAlertCount || 0 }}
            <text class="summary-unit">条</text>
          </view>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <view class="section-title">功率预测趋势</view>
        <view class="legend">
          <view class="legend-item">
            <view class="legend-dot legend-dot--blue"></view>
            <text>预测</text>
          </view>
          <view class="legend-item">
            <view class="legend-dot legend-dot--green"></view>
            <text>实际</text>
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
            <view class="chart-bars">
              <view 
                v-for="(point, idx) in chartPoints" 
                :key="idx"
                class="chart-point-wrapper"
              >
                <view class="deviation-bar" v-if="point.showDeviation" :style="{ height: point.deviationHeight + 'rpx', background: point.deviationColor }"></view>
                <view class="lines-area">
                  <view 
                    class="line-point line-point--predicted"
                    :style="{ bottom: point.predictedPos + 'rpx' }"
                  ></view>
                  <view 
                    class="line-point line-point--actual"
                    v-if="point.hasActual"
                    :style="{ bottom: point.actualPos + 'rpx' }"
                  ></view>
                </view>
                <text class="point-label">{{ point.label }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <view class="section-title">偏差告警</view>
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
            <text class="alert-time">{{ formatTime(item.alertTime) }}</text>
          </view>
          <view class="alert-content">{{ item.alertContent }}</view>
          <view class="alert-footer">
            <view class="alert-cause" :class="'cause--' + item.rootCause">
              {{ getCauseText(item.rootCause) }}
            </view>
            <view class="alert-status" :class="'status--' + getStatusClass(item.status)">
              {{ getStatusText(item.status) }}
            </view>
          </view>
        </view>
      </view>

      <view class="empty-state" v-else>
        <text class="empty-icon">✅</text>
        <text class="empty-text">暂无偏差告警</text>
      </view>
    </view>

    <view class="bottom-space"></view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { getPredictionCurve, getPredictionSummary, getWeatherOverview, queryAlerts } from '@/api/prediction'
import { getStationListAll } from '@/api/auth'
import { formatDate } from '@/utils/format'

const DEFAULT_STATION_ID = 1

const loading = ref(false)
const summary = ref(null)
const weatherData = ref(null)
const curveData = ref(null)
const alertList = ref([])

const yAxisLabels = computed(() => {
  if (!curveData.value?.predictedPower?.length) return ['0', '25', '50', '75', '100']
  const values = curveData.value.predictedPower.filter(v => v != null)
  const maxVal = Math.max(...values, 1)
  const step = Math.ceil(maxVal / 4)
  return [step * 4, step * 3, step * 2, step, 0].map(v => v >= 1000 ? (v / 1000).toFixed(1) + 'k' : v)
})

const chartPoints = computed(() => {
  if (!curveData.value) return []
  const { timeAxis, predictedPower, actualPower, deviationRate } = curveData.value
  const powers = predictedPower.filter(v => v != null)
  const maxPower = Math.max(...powers, 1)
  const maxHeight = 260

  return (timeAxis || []).map((label, idx) => {
    const predicted = predictedPower[idx] || 0
    const actual = actualPower[idx]
    const deviation = deviationRate[idx]

    const predictedPos = Math.round((predicted / maxPower) * maxHeight)
    const actualPos = actual != null ? Math.round((actual / maxPower) * maxHeight) : 0
    const absDeviation = deviation != null ? Math.abs(Number(deviation)) : 0
    const showDeviation = absDeviation >= 10
    const deviationHeight = Math.round((absDeviation / 50) * 60)
    const deviationColor = absDeviation >= 20 ? 'rgba(255, 77, 79, 0.6)' : 'rgba(250, 173, 20, 0.6)'

    return {
      label: label.split(' ')[1] || label,
      predictedPos,
      actualPos,
      hasActual: actual != null,
      showDeviation,
      deviationHeight,
      deviationColor
    }
  })
})

function formatTime(t) {
  if (!t) return '-'
  return formatDate(new Date(t), 'MM-DD HH:mm')
}

function getLevelClass(level) {
  return { 1: 'low', 2: 'medium', 3: 'high', 4: 'urgent' }[level] || 'low'
}

function getLevelText(level) {
  return { 1: '低', 2: '中', 3: '高', 4: '紧急' }[level] || '低'
}

function getCauseText(cause) {
  return { weather: '☁️ 天气原因', equipment: '⚡ 设备故障', other: '🔍 待排查' }[cause] || '🔍 待排查'
}

function getStatusClass(status) {
  return { 0: 'pending', 1: 'handled', 2: 'ignored' }[status] || 'pending'
}

function getStatusText(status) {
  return { 0: '未处理', 1: '已处理', 2: '已忽略' }[status] || '未知'
}

async function fetchData() {
  loading.value = true
  try {
    const results = await Promise.allSettled([
      getPredictionSummary(DEFAULT_STATION_ID),
      getPredictionCurve({ stationId: DEFAULT_STATION_ID, hours: 6 }),
      getWeatherOverview(DEFAULT_STATION_ID),
      queryAlerts({ stationId: DEFAULT_STATION_ID, status: 0, pageNum: 1, pageSize: 5 })
    ])

    if (results[0].status === 'fulfilled') {
      summary.value = results[0].value
    }
    if (results[1].status === 'fulfilled') {
      curveData.value = results[1].value
    }
    if (results[2].status === 'fulfilled') {
      weatherData.value = results[2].value
    }
    if (results[3].status === 'fulfilled') {
      alertList.value = results[3].value || []
    }
  } catch (err) {
    console.error('获取预测数据失败:', err)
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
.prediction-page {
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
  background: linear-gradient(135deg, #722ed1 0%, #9254de 100%);
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
  align-items: center;
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 30rpx 0;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
}

.summary-item {
  flex: 1;
  text-align: center;
}

.summary-label {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 12rpx;
}

.summary-value {
  font-size: 40rpx;
  font-weight: 600;
  color: #333;

  &.good { color: #52c41a; }
  &.warn { color: #faad14; }
  &.bad { color: #ff4d4f; }

  &--blue { color: #1890ff; }
}

.summary-unit {
  font-size: 22rpx;
  font-weight: 400;
  color: #999;
  margin-left: 6rpx;
}

.summary-divider {
  width: 1rpx;
  height: 80rpx;
  background-color: #f0f0f0;
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
    background-color: #722ed1;
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

  &--blue { background-color: #1890ff; }
  &--green { background-color: #52c41a; }
}

.chart-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.chart-canvas {
  display: flex;
  height: 320rpx;
}

.chart-y-axis {
  width: 60rpx;
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
  height: 280rpx;
}

.deviation-bar {
  position: absolute;
  top: 0;
  width: 6rpx;
  border-radius: 3rpx;
}

.lines-area {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 40rpx;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.line-point {
  position: absolute;
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;

  &--predicted {
    background-color: #1890ff;
    box-shadow: 0 0 0 4rpx rgba(24, 144, 255, 0.2);
  }

  &--actual {
    background-color: #52c41a;
    box-shadow: 0 0 0 4rpx rgba(82, 196, 26, 0.2);
  }
}

.point-label {
  position: absolute;
  bottom: 0;
  font-size: 20rpx;
  color: #999;
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
  justify-content: space-between;
  margin-bottom: 16rpx;
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

.alert-time {
  font-size: 24rpx;
  color: #999;
}

.alert-content {
  font-size: 26rpx;
  color: #333;
  line-height: 1.6;
  margin-bottom: 16rpx;
}

.alert-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.alert-cause {
  font-size: 24rpx;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;

  &.cause--weather { background-color: #e6f7ff; color: #1890ff; }
  &.cause--equipment { background-color: #fff1f0; color: #ff4d4f; }
  &.cause--other { background-color: #f5f5f5; color: #666; }
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
