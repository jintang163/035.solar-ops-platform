<template>
  <view class="efficiency-page">
    <view class="section">
      <view class="section-header">
        <view class="section-title">系统健康度</view>
      </view>

      <view class="health-card">
        <view v-if="healthLoading" class="empty-state">加载中...</view>
        <view v-else-if="healthScore === 0 && !healthHasData" class="empty-state">暂无数据</view>
        <template v-else>
          <view class="gauge-container">
            <view class="gauge">
              <view class="gauge-bg"></view>
              <view class="gauge-fill" :style="{ transform: `rotate(${healthScore * 1.8 - 90}deg)` }"></view>
              <view class="gauge-center">
                <view class="gauge-value">{{ healthScore }}</view>
                <view class="gauge-label">健康度</view>
              </view>
            </view>
          </view>

          <view class="health-stats">
            <view class="health-item">
              <view class="health-dot health-dot--good"></view>
              <text class="health-text">良好设备</text>
              <text class="health-num">{{ healthGood }}</text>
            </view>
            <view class="health-item">
              <view class="health-dot health-dot--warning"></view>
              <text class="health-text">注意设备</text>
              <text class="health-num">{{ healthWarning }}</text>
            </view>
            <view class="health-item">
              <view class="health-dot health-dot--error"></view>
              <text class="health-text">故障设备</text>
              <text class="health-num">{{ healthError }}</text>
            </view>
          </view>
        </template>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <view class="section-title">发电趋势</view>
        <view class="date-selector">
          <view
            v-for="item in dateOptions"
            :key="item.value"
            class="date-item"
            :class="{ active: activeDate === item.value }"
            @click="handleDateChange(item.value)"
          >
            {{ item.label }}
          </view>
        </view>
      </view>

      <view class="trend-card">
        <view v-if="trendLoading" class="empty-state">加载中...</view>
        <view v-else-if="trendData.length === 0" class="empty-state">暂无数据</view>
        <template v-else>
          <view class="trend-chart">
            <view class="chart-area">
              <view class="y-axis">
                <text v-for="(label, index) in yAxisLabels" :key="index">{{ label }}</text>
              </view>
              <view class="chart-body">
                <view class="chart-grid">
                  <view v-for="i in 5" :key="i" class="grid-line"></view>
                </view>
                <view class="chart-line">
                  <view
                    v-for="(point, index) in trendData"
                    :key="index"
                    class="chart-point"
                    :style="{
                      left: `${trendData.length > 1 ? (index / (trendData.length - 1)) * 100 : 50}%`,
                      bottom: `${point / maxValue * 100}%`
                    }"
                  ></view>
                  <svg class="line-svg" viewBox="0 0 100 100" preserveAspectRatio="none">
                    <polyline :points="linePoints" fill="none" stroke="#1890ff" stroke-width="2" />
                  </svg>
                </view>
              </view>
            </view>
            <view class="x-axis">
              <text v-for="(label, index) in xAxisLabels" :key="index">{{ label }}</text>
            </view>
          </view>

          <view class="trend-summary">
            <view class="summary-item">
              <view class="summary-label">总发电量</view>
              <view class="summary-value">{{ trendSummary.total }} kWh</view>
            </view>
            <view class="summary-item">
              <view class="summary-label">日均发电</view>
              <view class="summary-value">{{ trendSummary.avg }} kWh</view>
            </view>
            <view class="summary-item">
              <view class="summary-label">峰值功率</view>
              <view class="summary-value">{{ trendSummary.max }} kW</view>
            </view>
          </view>
        </template>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <view class="section-title">PR排名</view>
        <view class="rank-desc">系统效率比</view>
      </view>

      <view class="rank-list" v-if="rankLoading">
        <view class="empty-state">加载中...</view>
      </view>
      <view class="rank-list" v-else-if="prRankList.length > 0">
        <view
          v-for="(item, index) in prRankList"
          :key="item.id"
          class="rank-item"
        >
          <view class="rank-no" :class="`rank-no--${index + 1}`">
            {{ index + 1 }}
          </view>
          <view class="rank-info">
            <view class="rank-name">{{ item.name }}</view>
            <view class="rank-bar">
              <view class="rank-bar-fill" :style="{ width: `${item.pr * 100}%` }"></view>
            </view>
          </view>
          <view class="rank-value">{{ (item.pr * 100).toFixed(1) }}%</view>
        </view>
      </view>
      <view class="rank-list" v-else>
        <view class="empty-state">暂无数据</view>
      </view>
    </view>

    <view class="bottom-space"></view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { getEfficiencyRank, getStationEfficiency, getHealthAssessment, getStationHealthList } from '@/api/efficiency'
import { getStationListAll } from '@/api/auth'

const healthScore = ref(0)
const healthGood = ref(0)
const healthWarning = ref(0)
const healthError = ref(0)
const healthHasData = ref(false)
const healthLoading = ref(false)

const activeDate = ref('week')
const trendData = ref([])
const xAxisLabels = ref([])
const yAxisLabels = ref([])
const trendLoading = ref(false)

const prRankList = ref([])
const rankLoading = ref(false)

const stationIds = ref([])

const dateOptions = [
  { label: '日', value: 'day' },
  { label: '周', value: 'week' },
  { label: '月', value: 'month' },
  { label: '年', value: 'year' }
]

const statisticsTypeMap = { day: 1, week: 2, month: 3, year: 4 }

function formatDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function getDateRange() {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth()
  const d = now.getDate()

  switch (activeDate.value) {
    case 'day':
      return { startDate: formatDate(now), endDate: formatDate(now) }
    case 'week': {
      const dayOfWeek = now.getDay() || 7
      const start = new Date(y, m, d - dayOfWeek + 1)
      const end = new Date(y, m, d - dayOfWeek + 7)
      return { startDate: formatDate(start), endDate: formatDate(end) }
    }
    case 'month': {
      const start = new Date(y, m, 1)
      const end = new Date(y, m + 1, 0)
      return { startDate: formatDate(start), endDate: formatDate(end) }
    }
    case 'year':
      return { startDate: `${y}-01-01`, endDate: `${y}-12-31` }
  }
}

function formatXLabel(dateStr) {
  if (!dateStr) return ''
  const parts = dateStr.split('-')
  switch (activeDate.value) {
    case 'day':
      return parts.length >= 3 ? `${parseInt(parts[1])}/${parseInt(parts[2])}` : dateStr
    case 'week':
      return parts.length >= 3 ? `${parseInt(parts[1])}/${parseInt(parts[2])}` : dateStr
    case 'month':
      return parts.length >= 3 ? `${parseInt(parts[2])}日` : dateStr
    case 'year':
      return parts.length >= 2 ? `${parseInt(parts[1])}月` : dateStr
    default:
      return dateStr
  }
}

function computeYAxisLabels(max) {
  if (max <= 0) return ['0', '0', '0', '0', '0', '0']
  const step = Math.ceil(max / 5)
  const top = step * 5
  const labels = []
  for (let i = 5; i >= 0; i--) {
    labels.push(String(step * i))
  }
  return labels
}

const maxValue = computed(() => {
  return Math.max(...trendData.value, 1)
})

const linePoints = computed(() => {
  if (trendData.value.length === 0) return ''
  const points = trendData.value.map((point, index) => {
    const x = trendData.value.length > 1
      ? (index / (trendData.value.length - 1)) * 100
      : 50
    const y = 100 - (point / maxValue.value) * 100
    return `${x},${y}`
  })
  return points.join(' ')
})

const trendSummary = computed(() => {
  if (trendData.value.length === 0) {
    return { total: '0', avg: '0', max: '0' }
  }
  const total = trendData.value.reduce((sum, val) => sum + val, 0)
  const avg = total / trendData.value.length
  const max = Math.max(...trendData.value)
  return {
    total: total.toFixed(0),
    avg: avg.toFixed(0),
    max: max.toFixed(0)
  }
})

async function fetchStationList() {
  try {
    const res = await getStationListAll()
    const list = Array.isArray(res) ? res : (res?.data || [])
    stationIds.value = list.map(s => s.stationId || s.id).filter(Boolean)
  } catch (err) {
    console.error('获取电站列表失败:', err)
    stationIds.value = []
  }
}

async function fetchHealthData() {
  if (stationIds.value.length === 0) return
  healthLoading.value = true
  try {
    const res = await getStationHealthList(stationIds.value)
    const list = Array.isArray(res) ? res : (res?.data || [])
    if (list.length > 0) {
      healthHasData.value = true
      const totalScore = list.reduce((sum, item) => sum + (item.efficiencyScore || 0), 0)
      healthScore.value = Math.round(totalScore / list.length)
      healthGood.value = list.filter(item => item.healthLevel === 1).length
      healthWarning.value = list.filter(item => item.healthLevel === 2).length
      healthError.value = list.filter(item => item.healthLevel === 3).length
    } else {
      healthHasData.value = false
      healthScore.value = 0
      healthGood.value = 0
      healthWarning.value = 0
      healthError.value = 0
    }
  } catch (err) {
    console.error('获取健康度数据失败:', err)
    healthHasData.value = false
    healthScore.value = 0
    healthGood.value = 0
    healthWarning.value = 0
    healthError.value = 0
  } finally {
    healthLoading.value = false
  }
}

async function fetchTrendData() {
  if (stationIds.value.length === 0) return
  trendLoading.value = true
  try {
    const stationId = stationIds.value[0]
    const { startDate, endDate } = getDateRange()
    const statisticsType = statisticsTypeMap[activeDate.value]
    const res = await getStationEfficiency(stationId, { statisticsType, startDate, endDate })
    const list = Array.isArray(res) ? res : (res?.data || [])
    if (list.length > 0) {
      trendData.value = list.map(item => item.totalEnergy || 0)
      xAxisLabels.value = list.map(item => formatXLabel(item.statisticsDate))
      yAxisLabels.value = computeYAxisLabels(Math.max(...trendData.value))
    } else {
      trendData.value = []
      xAxisLabels.value = []
      yAxisLabels.value = []
    }
  } catch (err) {
    console.error('获取发电趋势失败:', err)
    trendData.value = []
    xAxisLabels.value = []
    yAxisLabels.value = []
  } finally {
    trendLoading.value = false
  }
}

async function fetchRankData() {
  rankLoading.value = true
  try {
    const statisticsType = statisticsTypeMap[activeDate.value]
    const { startDate } = getDateRange()
    const res = await getEfficiencyRank({ statisticsType, date: startDate, topN: 10 })
    const list = Array.isArray(res) ? res : (res?.data || [])
    prRankList.value = list.map((item, index) => ({
      id: index + 1,
      name: item.stationName || '',
      pr: (item.prValue || 0) / 100
    }))
  } catch (err) {
    console.error('获取PR排名失败:', err)
    prRankList.value = []
  } finally {
    rankLoading.value = false
  }
}

function handleDateChange(value) {
  activeDate.value = value
}

watch(activeDate, () => {
  fetchTrendData()
  fetchRankData()
})

onMounted(async () => {
  await fetchStationList()
  fetchHealthData()
  fetchTrendData()
  fetchRankData()
})
</script>

<style lang="scss" scoped>
.efficiency-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20rpx 0;
}

.section {
  padding: 20rpx 30rpx;
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
    background-color: #1890ff;
    border-radius: 4rpx;
  }
}

.date-selector {
  display: flex;
  background-color: #f0f0f0;
  border-radius: 8rpx;
  padding: 4rpx;
}

.date-item {
  padding: 10rpx 24rpx;
  font-size: 24rpx;
  color: #666;
  border-radius: 6rpx;

  &.active {
    background-color: #1890ff;
    color: #ffffff;
  }
}

.health-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 40rpx 30rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.gauge-container {
  display: flex;
  justify-content: center;
  margin-bottom: 40rpx;
}

.gauge {
  position: relative;
  width: 300rpx;
  height: 150rpx;
  overflow: hidden;
}

.gauge-bg {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 300rpx;
  height: 150rpx;
  border-radius: 150rpx 150rpx 0 0;
  background-color: #f0f0f0;
}

.gauge-fill {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 300rpx;
  height: 150rpx;
  border-radius: 150rpx 150rpx 0 0;
  background: conic-gradient(from -90deg, #52c41a 0deg, #1890ff 180deg, transparent 180deg);
  transform-origin: bottom center;
  transform: rotate(0deg);
}

.gauge-center {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 200rpx;
  height: 100rpx;
  background-color: #ffffff;
  border-radius: 100rpx 100rpx 0 0;
  text-align: center;
  padding-top: 30rpx;
  box-sizing: border-box;
}

.gauge-value {
  font-size: 48rpx;
  font-weight: 600;
  color: #1890ff;
  line-height: 1;
}

.gauge-label {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
}

.health-stats {
  display: flex;
  justify-content: space-around;
}

.health-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.health-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  margin-bottom: 12rpx;

  &--good {
    background-color: #52c41a;
  }

  &--warning {
    background-color: #faad14;
  }

  &--error {
    background-color: #ff4d4f;
  }
}

.health-text {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 8rpx;
}

.health-num {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.trend-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 30rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.trend-chart {
  height: 320rpx;
  margin-bottom: 30rpx;
}

.chart-area {
  display: flex;
  height: 280rpx;
}

.y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 80rpx;
  padding-right: 16rpx;

  text {
    font-size: 20rpx;
    color: #999;
    text-align: right;
  }
}

.chart-body {
  flex: 1;
  position: relative;
}

.chart-grid {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.grid-line {
  height: 1rpx;
  background-color: #f0f0f0;
}

.chart-line {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.line-svg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.chart-point {
  position: absolute;
  width: 12rpx;
  height: 12rpx;
  background-color: #1890ff;
  border: 4rpx solid #ffffff;
  border-radius: 50%;
  transform: translate(-50%, 50%);
  z-index: 1;
}

.x-axis {
  display: flex;
  justify-content: space-between;
  margin-top: 12rpx;
  padding-left: 80rpx;

  text {
    font-size: 20rpx;
    color: #999;
  }
}

.trend-summary {
  display: flex;
  border-top: 1rpx solid #f0f0f0;
  padding-top: 24rpx;
}

.summary-item {
  flex: 1;
  text-align: center;
}

.summary-label {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 8rpx;
}

.summary-value {
  font-size: 28rpx;
  color: #333;
  font-weight: 600;
}

.rank-desc {
  font-size: 24rpx;
  color: #999;
}

.rank-list {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 10rpx 30rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.rank-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.rank-no {
  width: 48rpx;
  height: 48rpx;
  line-height: 48rpx;
  text-align: center;
  font-size: 24rpx;
  font-weight: 600;
  color: #ffffff;
  background-color: #999;
  border-radius: 8rpx;
  margin-right: 20rpx;
  flex-shrink: 0;

  &--1 {
    background-color: #ffd700;
  }

  &--2 {
    background-color: #c0c0c0;
  }

  &--3 {
    background-color: #cd7f32;
  }
}

.rank-info {
  flex: 1;
  margin-right: 20rpx;
}

.rank-name {
  font-size: 28rpx;
  color: #333;
  margin-bottom: 12rpx;
}

.rank-bar {
  height: 12rpx;
  background-color: #f0f0f0;
  border-radius: 6rpx;
  overflow: hidden;
}

.rank-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #1890ff 0%, #52c41a 100%);
  border-radius: 6rpx;
}

.rank-value {
  font-size: 28rpx;
  font-weight: 600;
  color: #1890ff;
  flex-shrink: 0;
}

.empty-state {
  text-align: center;
  padding: 60rpx 0;
  font-size: 28rpx;
  color: #999;
}

.bottom-space {
  height: 40rpx;
}
</style>
