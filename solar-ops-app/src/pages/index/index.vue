<template>
  <view class="index-page">
    <view class="header">
      <view class="header-bg"></view>
      <view class="header-content">
        <view class="station-info">
          <view class="station-name">阳光光伏电站</view>
          <view class="station-status">
            <view class="status-dot"></view>
            <text>运行中</text>
          </view>
        </view>
        <view class="header-time">{{ currentTime }}</view>
      </view>
    </view>
    
    <view class="stats-section">
      <view class="stats-grid">
        <view class="stat-item">
          <view class="stat-icon stat-icon--blue">
            <text>⚡</text>
          </view>
          <view class="stat-value">{{ stats.totalEnergy }}</view>
          <view class="stat-label">总发电量 (万kWh)</view>
        </view>
        <view class="stat-item">
          <view class="stat-icon stat-icon--green">
            <text>☀️</text>
          </view>
          <view class="stat-value">{{ stats.todayEnergy }}</view>
          <view class="stat-label">今日发电量 (kWh)</view>
        </view>
        <view class="stat-item">
          <view class="stat-icon stat-icon--orange">
            <text>📟</text>
          </view>
          <view class="stat-value">{{ stats.deviceCount }}</view>
          <view class="stat-label">设备数 (台)</view>
        </view>
        <view class="stat-item">
          <view class="stat-icon stat-icon--purple">
            <text>📶</text>
          </view>
          <view class="stat-value">{{ stats.onlineRate }}%</view>
          <view class="stat-label">在线率</view>
        </view>
      </view>
    </view>
    
    <view class="section">
      <view class="section-header">
        <view class="section-title">逆变器列表</view>
        <view class="section-more" @click="viewMore">
          查看更多
          <text class="arrow">›</text>
        </view>
      </view>
      
      <view class="inverter-list" v-if="inverterList.length > 0">
        <inverter-card 
          v-for="item in inverterList" 
          :key="item.id" 
          :data="item"
          @click="goToDetail(item)"
        />
      </view>
      
      <view class="empty-state" v-else>
        <text class="empty-icon">📭</text>
        <text class="empty-text">暂无数据</text>
      </view>
    </view>
    
    <view class="section">
      <view class="section-header">
        <view class="section-title">实时发电功率</view>
        <view class="realtime-tag">
          <view class="blink-dot"></view>
          <text>实时</text>
        </view>
      </view>
      
      <view class="power-card">
        <view class="power-value">
          <text class="power-num">{{ currentPower }}</text>
          <text class="power-unit">kW</text>
        </view>
        <view class="power-chart">
          <view class="chart-bars">
            <view 
              v-for="(item, index) in powerChartData" 
              :key="index"
              class="chart-bar"
              :style="{ height: item + '%' }"
            ></view>
          </view>
          <view class="chart-labels">
            <text v-for="(label, index) in chartLabels" :key="index">{{ label }}</text>
          </view>
        </view>
      </view>
    </view>
    
    <view class="bottom-space"></view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import InverterCard from '@/components/inverter-card.vue'
import { getDashboardStats, getInverterList } from '@/api/device'
import { formatDate } from '@/utils/format'

const currentTime = ref(formatDate(new Date(), 'YYYY-MM-DD HH:mm'))
const currentPower = ref(0)
const loading = ref(false)

const stats = reactive({
  totalEnergy: '0.00',
  todayEnergy: '0.00',
  deviceCount: 0,
  onlineRate: '0.00'
})

const inverterList = ref([])
const powerChartData = ref([])
const chartLabels = ref([])

function initChartData() {
  const data = []
  const labels = []
  for (let i = 0; i < 12; i++) {
    data.push(Math.floor(Math.random() * 60) + 40)
    labels.push(`${i * 2}:00`)
  }
  powerChartData.value = data
  chartLabels.value = labels
  currentPower.value = (Math.random() * 500 + 200).toFixed(2)
}

async function fetchData() {
  loading.value = true
  try {
    const [statsData, inverterData] = await Promise.all([
      getDashboardStats(),
      getInverterList({ pageSize: 5 })
    ])
    
    if (statsData) {
      Object.assign(stats, statsData)
    }
    if (inverterData?.list) {
      inverterList.value = inverterData.list
    }
  } catch (err) {
    console.error('获取数据失败:', err)
    loadMockData()
  } finally {
    loading.value = false
  }
}

function loadMockData() {
  stats.totalEnergy = '1,256.80'
  stats.todayEnergy = '8,523.60'
  stats.deviceCount = 24
  stats.onlineRate = '95.83'
  
  inverterList.value = [
    {
      id: 1,
      name: 'INV-001',
      status: 1,
      stationName: '阳光光伏电站',
      power: 125.5,
      dailyEnergy: 1250.8,
      totalEnergy: 156800.5,
      dcVoltage: 580.2,
      updateTime: new Date().getTime()
    },
    {
      id: 2,
      name: 'INV-002',
      status: 1,
      stationName: '阳光光伏电站',
      power: 118.3,
      dailyEnergy: 1180.2,
      totalEnergy: 148520.3,
      dcVoltage: 575.8,
      updateTime: new Date().getTime()
    },
    {
      id: 3,
      name: 'INV-003',
      status: 0,
      stationName: '阳光光伏电站',
      power: 0,
      dailyEnergy: 0,
      totalEnergy: 132600.0,
      dcVoltage: 0,
      updateTime: new Date().getTime() - 3600000
    }
  ]
}

function updateTime() {
  currentTime.value = formatDate(new Date(), 'YYYY-MM-DD HH:mm')
}

function viewMore() {
  uni.showToast({ title: '查看更多', icon: 'none' })
}

function goToDetail(item) {
  uni.showToast({ title: `查看 ${item.name}`, icon: 'none' })
}

function onRefresh() {
  fetchData()
  initChartData()
  setTimeout(() => {
    uni.stopPullDownRefresh()
    uni.showToast({ title: '刷新成功', icon: 'success' })
  }, 1000)
}

onMounted(() => {
  loadMockData()
  initChartData()
  setInterval(updateTime, 60000)
})

onPullDownRefresh(() => {
  onRefresh()
})
</script>

<style lang="scss" scoped>
.index-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.header {
  position: relative;
  padding-top: 80rpx;
  padding-bottom: 120rpx;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 360rpx;
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  border-radius: 0 0 40rpx 40rpx;
}

.header-content {
  position: relative;
  z-index: 1;
  padding: 0 30rpx;
  color: #ffffff;
}

.station-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.station-name {
  font-size: 36rpx;
  font-weight: 600;
}

.station-status {
  display: flex;
  align-items: center;
  font-size: 24rpx;
  background: rgba(255, 255, 255, 0.2);
  padding: 8rpx 20rpx;
  border-radius: 24rpx;
}

.status-dot {
  width: 12rpx;
  height: 12rpx;
  background-color: #52c41a;
  border-radius: 50%;
  margin-right: 10rpx;
}

.header-time {
  font-size: 26rpx;
  opacity: 0.8;
}

.stats-section {
  padding: 0 30rpx;
  margin-top: -80rpx;
  position: relative;
  z-index: 2;
}

.stats-grid {
  display: flex;
  flex-wrap: wrap;
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 30rpx 0;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
}

.stat-item {
  width: 50%;
  text-align: center;
  padding: 20rpx 0;
  box-sizing: border-box;
  
  &:nth-child(1),
  &:nth-child(2) {
    padding-bottom: 30rpx;
    border-bottom: 1rpx solid #f0f0f0;
  }
  
  &:nth-child(odd) {
    border-right: 1rpx solid #f0f0f0;
  }
}

.stat-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16rpx;
  font-size: 32rpx;
  
  &--blue {
    background-color: #e6f7ff;
  }
  
  &--green {
    background-color: #f0f9eb;
  }
  
  &--orange {
    background-color: #fff7e6;
  }
  
  &--purple {
    background-color: #f9f0ff;
  }
}

.stat-value {
  font-size: 36rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 8rpx;
}

.stat-label {
  font-size: 24rpx;
  color: #999;
}

.section {
  padding: 30rpx;
  
  &:last-of-type {
    padding-bottom: 0;
  }
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

.section-more {
  display: flex;
  align-items: center;
  font-size: 26rpx;
  color: #999;
  
  .arrow {
    margin-left: 8rpx;
    font-size: 30rpx;
  }
}

.realtime-tag {
  display: flex;
  align-items: center;
  font-size: 24rpx;
  color: #ff4d4f;
}

.blink-dot {
  width: 12rpx;
  height: 12rpx;
  background-color: #ff4d4f;
  border-radius: 50%;
  margin-right: 10rpx;
  animation: blink 1.5s ease-in-out infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.power-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 30rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.power-value {
  text-align: center;
  margin-bottom: 30rpx;
}

.power-num {
  font-size: 72rpx;
  font-weight: 600;
  color: #1890ff;
}

.power-unit {
  font-size: 28rpx;
  color: #999;
  margin-left: 10rpx;
}

.power-chart {
  height: 200rpx;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 160rpx;
  padding: 0 10rpx;
}

.chart-bar {
  width: 8rpx;
  background: linear-gradient(180deg, #1890ff 0%, #91d5ff 100%);
  border-radius: 4rpx;
  min-height: 10rpx;
}

.chart-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 12rpx;
  padding: 0 4rpx;
  
  text {
    font-size: 20rpx;
    color: #999;
  }
}

.empty-state {
  text-align: center;
  padding: 80rpx 0;
  
  .empty-icon {
    font-size: 80rpx;
    display: block;
    margin-bottom: 20rpx;
  }
  
  .empty-text {
    font-size: 28rpx;
    color: #999;
  }
}

.bottom-space {
  height: 40rpx;
}
</style>
