<template>
  <view class="compare-page">
    <view class="filter-bar">
      <view class="filter-item" @click="showStationPicker = true">
        <text class="filter-label">电站</text>
        <text class="filter-value">
          {{ selectedStations.length > 0 ? `已选${selectedStations.length}个` : '请选择' }}
        </text>
        <text class="filter-arrow">›</text>
      </view>
      <view class="filter-item time-filter">
        <view
          v-for="item in timeOptions"
          :key="item.value"
          class="time-chip"
          :class="{ active: activeTime === item.value }"
          @click="handleTimeChange(item.value)"
        >
          {{ item.label }}
        </view>
      </view>
      <view class="compare-btn" :class="{ disabled: selectedStations.length < 2 }" @click="handleCompare">
        对比
      </view>
    </view>

    <view class="tips-section" v-if="stationMetrics.length > 0">
      <view class="tip-card tip-card--best">
        <text class="tip-emoji">🏆</text>
        <view class="tip-content">
          <text class="tip-label">最佳表现</text>
          <text class="tip-text">{{ bestStation.stationName }} PR {{ bestStation.pr }}%</text>
        </view>
      </view>
      <view class="tip-card tip-card--worst">
        <text class="tip-emoji">⚠️</text>
        <view class="tip-content">
          <text class="tip-label">需优化</text>
          <text class="tip-text">{{ worstStation.stationName }} 故障率 {{ worstStation.failureRate }}%</text>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <view class="section-title">指标对比</view>
      </view>

      <view v-if="loading" class="empty-state">加载中...</view>
      <view v-else-if="stationMetrics.length === 0" class="empty-state">请选择至少2个电站进行对比</view>
      <template v-else>
        <view
          v-for="(station, index) in stationMetrics"
          :key="station.stationId"
          class="station-card"
        >
          <view class="station-header">
            <view class="station-name">{{ station.stationName }}</view>
            <view
              class="pr-value"
              :class="parseFloat(station.pr) >= 75 ? 'pr-good' : 'pr-bad'"
            >
              PR {{ station.pr }}%
            </view>
          </view>

          <view class="metric-row">
            <view class="metric-item">
              <view class="metric-label">等效小时</view>
              <view class="metric-value">{{ station.equivalentHours }} h</view>
            </view>
            <view class="metric-item">
              <view class="metric-label">故障率</view>
              <view class="metric-value" :class="parseFloat(station.failureRate) > 2 ? 'text-bad' : ''">
                {{ station.failureRate }}%
              </view>
            </view>
            <view class="metric-item">
              <view class="metric-label">健康度</view>
              <view class="metric-value" :class="getHealthClass(station.healthScore)">
                {{ station.healthScore }}
              </view>
            </view>
          </view>
        </view>

        <view class="chart-section">
          <view class="section-subtitle">PR 值对比柱状图</view>
          <view class="bar-chart">
            <view
              v-for="(station, index) in stationMetrics"
              :key="station.stationId"
              class="bar-item"
            >
              <view class="bar-value">{{ station.pr }}%</view>
              <view class="bar-wrapper">
                <view
                  class="bar-fill"
                  :style="{
                    height: getBarHeight(station.pr) + '%',
                    backgroundColor: barColors[index % barColors.length]
                  }"
                ></view>
              </view>
              <view class="bar-label">{{ getShortName(station.stationName) }}</view>
            </view>
          </view>
        </view>
      </template>
    </view>

    <view class="section" v-if="recommendations.length > 0">
      <view class="section-header">
        <view class="section-title">优化建议</view>
      </view>
      <view class="recommend-list">
        <view
          v-for="(item, index) in recommendations"
          :key="index"
          class="recommend-item"
        >
          <view class="recommend-header" @click="toggleRecommend(index)">
            <view class="recommend-title">
              <text class="recommend-icon">{{ item.type === 'warning' ? '⚠️' : '💡' }}</text>
              {{ item.title }}
            </view>
            <view class="recommend-arrow" :class="{ open: expandedRecommends.includes(index) }">›</view>
          </view>
          <view v-show="expandedRecommends.includes(index)" class="recommend-content">
            {{ item.content }}
          </view>
        </view>
      </view>
    </view>

    <view class="bottom-space"></view>

    <view class="station-modal" v-if="showStationPicker" @click.self="showStationPicker = false">
      <view class="modal-content">
        <view class="modal-header">
          <text class="modal-cancel" @click="showStationPicker = false">取消</text>
          <text class="modal-title">选择电站</text>
          <text class="modal-confirm" @click="confirmStationSelect">确定</text>
        </view>
        <scroll-view scroll-y class="station-list">
          <view
            v-for="station in allStations"
            :key="station.stationId || station.id"
            class="station-option"
            @click="toggleStation(station)"
          >
            <view class="station-checkbox">
              <view
                class="checkbox-inner"
                :class="{ checked: isStationSelected(station) }"
              >
                <text v-if="isStationSelected(station)">✓</text>
              </view>
            </view>
            <text class="station-option-name">{{ station.stationName || station.name }}</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { compareStations } from '@/api/efficiency'
import { getStationListAll } from '@/api/auth'

const loading = ref(false)
const showStationPicker = ref(false)
const activeTime = ref('month')
const tempSelectedStations = ref([])
const selectedStations = ref([])
const allStations = ref([])
const stationMetrics = ref([])
const recommendations = ref([])
const expandedRecommends = ref([])

const timeOptions = [
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' },
  { label: '本季', value: 'quarter' },
  { label: '本年', value: 'year' }
]

const statisticsTypeMap = { week: 2, month: 3, quarter: 5, year: 4 }

const barColors = ['#1890ff', '#52c41a', '#faad14', '#722ed1', '#eb2f96', '#13c2c2']

const bestStation = computed(() => {
  if (stationMetrics.value.length === 0) return { stationName: '-', pr: '0' }
  return stationMetrics.value.reduce((best, curr) =>
    parseFloat(curr.pr) > parseFloat(best.pr) ? curr : best
  )
})

const worstStation = computed(() => {
  if (stationMetrics.value.length === 0) return { stationName: '-', failureRate: '0' }
  return stationMetrics.value.reduce((worst, curr) =>
    parseFloat(curr.failureRate) > parseFloat(worst.failureRate) ? curr : worst
  )
})

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

  switch (activeTime.value) {
    case 'week': {
      const dayOfWeek = now.getDay() || 7
      const start = new Date(y, m, d - dayOfWeek + 1)
      const end = new Date(y, m, d - dayOfWeek + 7)
      return { startTime: formatDate(start), endTime: formatDate(end) }
    }
    case 'month': {
      const start = new Date(y, m, 1)
      const end = new Date(y, m + 1, 0)
      return { startTime: formatDate(start), endTime: formatDate(end) }
    }
    case 'quarter': {
      const q = Math.floor(m / 3)
      const start = new Date(y, q * 3, 1)
      const end = new Date(y, q * 3 + 3, 0)
      return { startTime: formatDate(start), endTime: formatDate(end) }
    }
    case 'year':
      return { startTime: `${y}-01-01`, endTime: `${y}-12-31` }
  }
}

function handleTimeChange(value) {
  activeTime.value = value
}

function isStationSelected(station) {
  const id = station.stationId || station.id
  return tempSelectedStations.value.some(s => (s.stationId || s.id) === id)
}

function toggleStation(station) {
  const id = station.stationId || station.id
  const idx = tempSelectedStations.value.findIndex(s => (s.stationId || s.id) === id)
  if (idx > -1) {
    tempSelectedStations.value.splice(idx, 1)
  } else {
    tempSelectedStations.value.push(station)
  }
}

function confirmStationSelect() {
  selectedStations.value = [...tempSelectedStations.value]
  showStationPicker.value = false
}

function getHealthClass(score) {
  const s = parseFloat(score)
  if (s >= 80) return 'text-good'
  if (s >= 60) return 'text-warning'
  return 'text-bad'
}

function getBarHeight(pr) {
  const val = parseFloat(pr)
  return Math.min(Math.max(val, 5), 100)
}

function getShortName(name) {
  if (!name) return ''
  return name.length > 4 ? name.slice(0, 4) : name
}

function toggleRecommend(index) {
  const idx = expandedRecommends.value.indexOf(index)
  if (idx > -1) {
    expandedRecommends.value.splice(idx, 1)
  } else {
    expandedRecommends.value.push(index)
  }
}

async function fetchAllStations() {
  try {
    const res = await getStationListAll()
    allStations.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (err) {
    console.error('获取电站列表失败:', err)
    allStations.value = []
  }
}

async function handleCompare() {
  if (selectedStations.value.length < 2) {
    uni.showToast({ title: '请至少选择2个电站', icon: 'none' })
    return
  }

  loading.value = true
  try {
    const stationIds = selectedStations.value.map(s => s.stationId || s.id)
    const { startTime, endTime } = getDateRange()
    const statisticsType = statisticsTypeMap[activeTime.value]

    const res = await compareStations({
      stationIds,
      startTime,
      endTime,
      statisticsType
    })

    const data = Array.isArray(res) ? res : (res?.stationMetrics || [])
    if (Array.isArray(data) && data.length > 0) {
      stationMetrics.value = data.map((item, index) => ({
        stationId: item.stationId || selectedStations.value[index]?.stationId || index,
        stationName: item.stationName || selectedStations.value[index]?.stationName || selectedStations.value[index]?.name || `电站${index + 1}`,
        pr: item.pr != null ? (typeof item.pr === 'number' && item.pr < 1 ? (item.pr * 100).toFixed(1) : String(item.pr)) : '0.0',
        equivalentHours: item.equivalentHours != null ? String(item.equivalentHours) : '0',
        failureRate: item.failureRate != null ? (typeof item.failureRate === 'number' && item.failureRate < 1 ? (item.failureRate * 100).toFixed(2) : String(item.failureRate)) : '0.00',
        healthScore: item.healthScore != null ? String(item.healthScore) : '0'
      }))
      recommendations.value = res?.recommendations || []
    } else {
      stationMetrics.value = []
      recommendations.value = []
      uni.showToast({ title: '暂无对比数据', icon: 'none' })
    }
  } catch (err) {
    console.error('电站对比失败:', err)
    stationMetrics.value = []
    recommendations.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchAllStations()
})
</script>

<style lang="scss" scoped>
.compare-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 20rpx;
}

.filter-bar {
  background-color: #ffffff;
  padding: 24rpx 30rpx;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.filter-item {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  background-color: #f8f9fa;
  border-radius: 12rpx;
}

.filter-label {
  font-size: 28rpx;
  color: #666;
  margin-right: 16rpx;
}

.filter-value {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.filter-arrow {
  font-size: 32rpx;
  color: #999;
}

.time-filter {
  flex-wrap: wrap;
  gap: 16rpx;
  padding: 12rpx;
}

.time-chip {
  padding: 12rpx 32rpx;
  font-size: 26rpx;
  color: #666;
  background-color: #f0f0f0;
  border-radius: 32rpx;

  &.active {
    background-color: #1890ff;
    color: #ffffff;
  }
}

.compare-btn {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  color: #ffffff;
  text-align: center;
  padding: 24rpx 0;
  border-radius: 12rpx;
  font-size: 30rpx;
  font-weight: 600;

  &.disabled {
    opacity: 0.5;
  }
}

.tips-section {
  padding: 20rpx 30rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.tip-card {
  display: flex;
  align-items: center;
  padding: 24rpx 28rpx;
  border-radius: 12rpx;

  &--best {
    background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
  }

  &--worst {
    background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%);
  }
}

.tip-emoji {
  font-size: 40rpx;
  margin-right: 20rpx;
}

.tip-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.tip-label {
  font-size: 22rpx;
  color: #999;
  margin-bottom: 6rpx;
}

.tip-text {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
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

.section-subtitle {
  font-size: 26rpx;
  color: #666;
  margin-bottom: 20rpx;
}

.station-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.station-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.station-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.pr-value {
  font-size: 28rpx;
  font-weight: 600;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;

  &.pr-good {
    color: #52c41a;
    background-color: #f6ffed;
  }

  &.pr-bad {
    color: #ff4d4f;
    background-color: #fff1f0;
  }
}

.metric-row {
  display: flex;
  border-top: 1rpx solid #f0f0f0;
  padding-top: 20rpx;
}

.metric-item {
  flex: 1;
  text-align: center;

  &:not(:last-child) {
    border-right: 1rpx solid #f0f0f0;
  }
}

.metric-label {
  font-size: 22rpx;
  color: #999;
  margin-bottom: 8rpx;
}

.metric-value {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.text-good {
  color: #52c41a !important;
}

.text-warning {
  color: #faad14 !important;
}

.text-bad {
  color: #ff4d4f !important;
}

.chart-section {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 28rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.bar-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 280rpx;
  padding: 20rpx 0;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.bar-value {
  font-size: 22rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 10rpx;
}

.bar-wrapper {
  width: 48rpx;
  height: 200rpx;
  background-color: #f0f0f0;
  border-radius: 8rpx 8rpx 0 0;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}

.bar-fill {
  width: 100%;
  border-radius: 8rpx 8rpx 0 0;
  transition: height 0.3s ease;
}

.bar-label {
  font-size: 20rpx;
  color: #666;
  margin-top: 10rpx;
}

.recommend-list {
  background-color: #ffffff;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.recommend-item {
  border-bottom: 1rpx solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.recommend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx;
}

.recommend-title {
  flex: 1;
  font-size: 28rpx;
  color: #333;
  display: flex;
  align-items: center;
}

.recommend-icon {
  margin-right: 12rpx;
  font-size: 28rpx;
}

.recommend-arrow {
  font-size: 32rpx;
  color: #999;
  transition: transform 0.3s;

  &.open {
    transform: rotate(90deg);
  }
}

.recommend-content {
  padding: 0 28rpx 28rpx;
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
}

.empty-state {
  text-align: center;
  padding: 60rpx 0;
  font-size: 28rpx;
  color: #999;
  background-color: #ffffff;
  border-radius: 16rpx;
}

.bottom-space {
  height: 40rpx;
}

.station-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
}

.modal-content {
  width: 100%;
  background-color: #ffffff;
  border-radius: 24rpx 24rpx 0 0;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.modal-cancel {
  font-size: 28rpx;
  color: #999;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.modal-confirm {
  font-size: 28rpx;
  color: #1890ff;
  font-weight: 600;
}

.station-list {
  flex: 1;
  padding: 10rpx 0;
  max-height: 60vh;
}

.station-option {
  display: flex;
  align-items: center;
  padding: 24rpx 30rpx;
}

.station-checkbox {
  margin-right: 20rpx;
}

.checkbox-inner {
  width: 40rpx;
  height: 40rpx;
  border: 2rpx solid #d9d9d9;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  color: #ffffff;

  &.checked {
    background-color: #1890ff;
    border-color: #1890ff;
  }
}

.station-option-name {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}
</style>
