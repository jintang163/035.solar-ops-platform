<template>
  <view class="efficiency-page">
    <view class="section">
      <view class="section-header">
        <view class="section-title">系统健康度</view>
      </view>
      
      <view class="health-card">
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
            <text class="health-num">22</text>
          </view>
          <view class="health-item">
            <view class="health-dot health-dot--warning"></view>
            <text class="health-text">注意设备</text>
            <text class="health-num">2</text>
          </view>
          <view class="health-item">
            <view class="health-dot health-dot--error"></view>
            <text class="health-text">故障设备</text>
            <text class="health-num">0</text>
          </view>
        </view>
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
            @click="activeDate = item.value"
          >
            {{ item.label }}
          </view>
        </view>
      </view>
      
      <view class="trend-card">
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
                    left: `${(index / (trendData.length - 1)) * 100}%`, 
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
      </view>
    </view>
    
    <view class="section">
      <view class="section-header">
        <view class="section-title">PR排名</view>
        <view class="rank-desc">系统效率比</view>
      </view>
      
      <view class="rank-list">
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
    </view>
    
    <view class="bottom-space"></view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getHealthScore, getTrendData, getEfficiencyRank } from '@/api/efficiency'

const healthScore = ref(88)
const activeDate = ref('week')
const trendData = ref([])
const xAxisLabels = ref([])
const yAxisLabels = ref(['1000', '800', '600', '400', '200', '0'])
const prRankList = ref([])

const dateOptions = [
  { label: '日', value: 'day' },
  { label: '周', value: 'week' },
  { label: '月', value: 'month' },
  { label: '年', value: 'year' }
]

const maxValue = computed(() => {
  return Math.max(...trendData.value, 1)
})

const linePoints = computed(() => {
  const points = trendData.value.map((point, index) => {
    const x = (index / (trendData.value.length - 1)) * 100
    const y = 100 - (point / maxValue.value) * 100
    return `${x},${y}`
  })
  return points.join(' ')
})

const trendSummary = computed(() => {
  const total = trendData.value.reduce((sum, val) => sum + val, 0)
  const avg = total / trendData.value.length
  const max = Math.max(...trendData.value)
  return {
    total: total.toFixed(0),
    avg: avg.toFixed(0),
    max: max.toFixed(0)
  }
})

function initTrendData() {
  const data = []
  const labels = []
  for (let i = 0; i < 7; i++) {
    data.push(Math.floor(Math.random() * 500) + 300)
    labels.push(`周${'日一二三四五六'[i]}`)
  }
  trendData.value = data
  xAxisLabels.value = labels
}

function initRankData() {
  prRankList.value = [
    { id: 1, name: 'INV-001', pr: 0.92 },
    { id: 2, name: 'INV-002', pr: 0.89 },
    { id: 3, name: 'INV-005', pr: 0.87 },
    { id: 4, name: 'INV-003', pr: 0.85 },
    { id: 5, name: 'INV-004', pr: 0.82 },
    { id: 6, name: 'INV-006', pr: 0.78 }
  ]
}

async function fetchData() {
  try {
    const [health, trend, rank] = await Promise.all([
      getHealthScore(1),
      getTrendData({ type: activeDate.value }),
      getEfficiencyRank()
    ])
    
    if (health) healthScore.value = health.score || 88
    if (trend?.data) trendData.value = trend.data
    if (rank?.list) prRankList.value = rank.list
  } catch (err) {
    console.error('获取效率数据失败:', err)
  }
}

onMounted(() => {
  initTrendData()
  initRankData()
  fetchData()
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

.bottom-space {
  height: 40rpx;
}
</style>
