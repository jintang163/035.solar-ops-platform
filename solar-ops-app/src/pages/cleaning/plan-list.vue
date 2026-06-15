<template>
  <view class="plan-list-page">
    <view class="tabs">
      <view
        v-for="(tab, index) in tabs"
        :key="index"
        class="tab-item"
        :class="{ active: activeTab === index }"
        @click="switchTab(index)"
      >
        <text class="tab-text">{{ tab.label }}</text>
        <view v-if="tab.count > 0" class="tab-badge">{{ tab.count }}</view>
      </view>
    </view>

    <view class="filter-bar">
      <view class="filter-item" @click="showStationFilter = true">
        <text class="filter-text">{{ currentStation || '全部电站' }}</text>
        <text class="filter-arrow">▼</text>
      </view>
      <view class="filter-item" @click="showDateFilter = true">
        <text class="filter-text">{{ dateRangeText || '全部日期' }}</text>
        <text class="filter-arrow">▼</text>
      </view>
    </view>

    <view class="plan-list" v-if="planList.length > 0">
      <view
        class="plan-card"
        v-for="plan in planList"
        :key="plan.id"
        @click="goToDetail(plan.id)"
      >
        <view class="card-status-bar" :class="'bar-' + plan.status"></view>
        <view class="card-content">
          <view class="card-head">
            <view class="plan-title">{{ plan.title }}</view>
            <view class="plan-status" :class="'tag-' + plan.status">
              {{ getStatusText(plan.status) }}
            </view>
          </view>
          <view class="card-meta">
            <view class="meta-row">
              <text class="meta-icon">🏢</text>
              <text class="meta-text">{{ plan.stationName || '未知电站' }}</text>
            </view>
            <view class="meta-row" v-if="plan.arrayNumber || plan.inverterName">
              <text class="meta-icon">📍</text>
              <text class="meta-text">{{ plan.arrayNumber || plan.inverterName }}</text>
            </view>
            <view class="meta-row">
              <text class="meta-icon">📅</text>
              <text class="meta-text">{{ plan.planDate }}</text>
              <text class="meta-divider">·</text>
              <text class="meta-icon">👤</text>
              <text class="meta-text">{{ plan.ownerName || '未指派' }}</text>
            </view>
          </view>
          <view class="card-effect" v-if="plan.status === 2 && (plan.improvedEnergy || plan.improvementRatePercent)">
            <view class="effect-item">
              <text class="effect-label">发电量提升</text>
              <text class="effect-value">+{{ plan.improvedEnergy || 0 }} kWh</text>
            </view>
            <view class="effect-rate" v-if="plan.improvementRatePercent">
              +{{ plan.improvementRatePercent }}%
            </view>
          </view>
          <view class="card-actions" v-if="plan.status === 0 || plan.status === 1">
            <view
              v-if="plan.status === 0"
              class="action-btn start"
              @click.stop="handleStart(plan)"
            >
              开始执行
            </view>
            <view
              v-if="plan.status === 1"
              class="action-btn complete"
              @click.stop="handleComplete(plan)"
            >
              完成清洗
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="empty-state" v-else>
      <text class="empty-icon">📋</text>
      <text class="empty-text">{{ getEmptyText() }}</text>
      <view class="empty-btn" @click="goToCreate" v-if="activeTab !== 3">
        创建清洗计划
      </view>
    </view>

    <view class="load-more" v-if="planList.length > 0">
      <text v-if="loading">加载中...</text>
      <text v-else-if="noMore">没有更多了</text>
      <text v-else @click="loadMore">加载更多</text>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import {
  getCleaningPlanPage,
  startCleaningPlan,
  completeCleaningPlan
} from '@/api/cleaning'

const activeTab = ref(0)
const loading = ref(false)
const noMore = ref(false)
const planList = ref([])
const showStationFilter = ref(false)
const showDateFilter = ref(false)
const currentStation = ref('')
const dateRangeText = ref('')

const tabs = ref([
  { label: '待执行', status: 0, count: 0 },
  { label: '执行中', status: 1, count: 0 },
  { label: '已完成', status: 2, count: 0 },
  { label: '已取消', status: 3, count: 0 }
])

const pageInfo = reactive({
  pageNum: 1,
  pageSize: 10
})

const queryParams = reactive({
  startDate: '',
  endDate: ''
})

function getStatusText(status) {
  const map = { 0: '待执行', 1: '执行中', 2: '已完成', 3: '已取消' }
  return map[status] || '未知'
}

function getEmptyText() {
  if (activeTab.value === 0) return '暂无待执行的清洗计划'
  if (activeTab.value === 1) return '暂无正在执行的清洗计划'
  if (activeTab.value === 2) return '暂无已完成的清洗计划'
  return '暂无已取消的清洗计划'
}

function getCurrentUser() {
  try {
    const userInfo = uni.getStorageSync('userInfo')
    if (userInfo) {
      const parsed = typeof userInfo === 'string' ? JSON.parse(userInfo) : userInfo
      return {
        operatorId: parsed.id || parsed.userId || '',
        operatorName: parsed.name || parsed.userName || '移动端用户'
      }
    }
  } catch (e) {}
  return { operatorId: '', operatorName: '移动端用户' }
}

async function fetchPlanList() {
  if (loading.value) return
  loading.value = true
  try {
    const tab = tabs.value[activeTab.value]
    const params = {
      status: tab.status,
      pageNum: pageInfo.pageNum,
      pageSize: pageInfo.pageSize,
      ...queryParams
    }
    const res = await getCleaningPlanPage(params)
    const list = res?.list || []

    if (pageInfo.pageNum === 1) {
      planList.value = list
    } else {
      planList.value = [...planList.value, ...list]
    }

    if (list.length < pageInfo.pageSize) {
      noMore.value = true
    }
  } catch (err) {
    console.error('获取计划列表失败:', err)
    if (pageInfo.pageNum === 1) {
      planList.value = []
    }
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  try {
    const promises = tabs.value.map(tab =>
      getCleaningPlanPage({ status: tab.status, pageNum: 1, pageSize: 1 })
    )
    const results = await Promise.all(promises)
    results.forEach((res, idx) => {
      tabs.value[idx].count = res?.total || 0
    })
  } catch (err) {
    console.error('获取统计失败:', err)
  }
}

function switchTab(index) {
  activeTab.value = index
  pageInfo.pageNum = 1
  planList.value = []
  noMore.value = false
  fetchPlanList()
}

function loadMore() {
  if (noMore.value || loading.value) return
  pageInfo.pageNum++
  fetchPlanList()
}

function goToDetail(id) {
  uni.navigateTo({ url: `/pages/cleaning/plan-detail?id=${id}` })
}

function goToCreate() {
  uni.navigateTo({ url: '/pages/cleaning/plan-edit' })
}

async function handleStart(plan) {
  uni.showModal({
    title: '开始执行',
    content: `确定开始执行「${plan.title}」吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          const { operatorId, operatorName } = getCurrentUser()
          await startCleaningPlan({
            planId: plan.id,
            operatorId,
            operatorName
          })
          uni.showToast({ title: '已开始', icon: 'success' })
          refreshAll()
        } catch (err) {
          uni.showToast({ title: '操作失败', icon: 'none' })
        }
      }
    }
  })
}

function handleComplete(plan) {
  uni.navigateTo({
    url: `/pages/cleaning/plan-detail?id=${plan.id}&action=complete`
  })
}

function refreshAll() {
  fetchStats()
  pageInfo.pageNum = 1
  planList.value = []
  noMore.value = false
  fetchPlanList()
}

onReachBottom(() => {
  loadMore()
})

watch(activeTab, () => {
  pageInfo.pageNum = 1
  planList.value = []
  noMore.value = false
  fetchPlanList()
})

onShow(() => {
  refreshAll()
})

onMounted(() => {
  refreshAll()
})
</script>

<style lang="scss" scoped>
.plan-list-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.tabs {
  display: flex;
  background-color: #fff;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 28rpx 0;
  position: relative;

  &.active {
    .tab-text {
      color: #1890ff;
      font-weight: 600;
    }

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 60rpx;
      height: 6rpx;
      background-color: #1890ff;
      border-radius: 3rpx;
    }
  }
}

.tab-text {
  font-size: 28rpx;
  color: #666;
}

.tab-badge {
  display: inline-block;
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  padding: 0 10rpx;
  background-color: #ff4d4f;
  color: #fff;
  font-size: 20rpx;
  border-radius: 16rpx;
  margin-left: 8rpx;
  vertical-align: middle;
}

.filter-bar {
  display: flex;
  padding: 20rpx 30rpx;
  background-color: #fff;
  border-bottom: 1rpx solid #f0f0f0;
  gap: 20rpx;

  .filter-item {
    display: flex;
    align-items: center;
    padding: 12rpx 24rpx;
    background-color: #f5f6fa;
    border-radius: 30rpx;
  }

  .filter-text {
    font-size: 24rpx;
    color: #595959;
    margin-right: 10rpx;
  }

  .filter-arrow {
    font-size: 18rpx;
    color: #bfbfbf;
  }
}

.plan-list {
  padding: 20rpx 30rpx;
}

.plan-card {
  background-color: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  display: flex;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.card-status-bar {
  width: 8rpx;
  flex-shrink: 0;

  &.bar-0 { background-color: #fa8c16; }
  &.bar-1 { background-color: #1890ff; }
  &.bar-2 { background-color: #52c41a; }
  &.bar-3 { background-color: #bfbfbf; }
}

.card-content {
  flex: 1;
  padding: 24rpx;
  min-width: 0;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16rpx;
}

.plan-title {
  flex: 1;
  font-size: 30rpx;
  font-weight: 600;
  color: #262626;
  line-height: 1.4;
  padding-right: 16rpx;
}

.plan-status {
  padding: 6rpx 16rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
  flex-shrink: 0;

  &.tag-0 { background-color: #fff7e6; color: #fa8c16; }
  &.tag-1 { background-color: #e6f7ff; color: #1890ff; }
  &.tag-2 { background-color: #f6ffed; color: #52c41a; }
  &.tag-3 { background-color: #f5f5f5; color: #8c8c8c; }
}

.card-meta {
  .meta-row {
    display: flex;
    align-items: center;
    margin-bottom: 10rpx;
    flex-wrap: wrap;

    &:last-child { margin-bottom: 0; }
  }

  .meta-icon {
    font-size: 24rpx;
    margin-right: 10rpx;
  }

  .meta-text {
    font-size: 24rpx;
    color: #8c8c8c;
  }

  .meta-divider {
    margin: 0 12rpx;
    color: #d9d9d9;
  }
}

.card-effect {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx dashed #f0f0f0;

  .effect-item {
    display: flex;
    align-items: center;
  }

  .effect-label {
    font-size: 24rpx;
    color: #8c8c8c;
    margin-right: 12rpx;
  }

  .effect-value {
    font-size: 28rpx;
    font-weight: 600;
    color: #52c41a;
  }

  .effect-rate {
    padding: 6rpx 16rpx;
    background-color: #f6ffed;
    color: #389e0d;
    border-radius: 8rpx;
    font-size: 22rpx;
    font-weight: 500;
  }
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 20rpx;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f5f5f5;

  .action-btn {
    padding: 12rpx 32rpx;
    border-radius: 8rpx;
    font-size: 26rpx;
    font-weight: 500;

    &.start {
      background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
      color: #fff;
    }

    &.complete {
      background: linear-gradient(135deg, #52c41a 0%, #95de64 100%);
      color: #fff;
    }
  }
}

.empty-state {
  text-align: center;
  padding: 160rpx 0;

  .empty-icon {
    display: block;
    font-size: 100rpx;
    margin-bottom: 24rpx;
  }

  .empty-text {
    display: block;
    font-size: 28rpx;
    color: #999;
    margin-bottom: 40rpx;
  }

  .empty-btn {
    display: inline-block;
    padding: 20rpx 60rpx;
    background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
    color: #fff;
    border-radius: 40rpx;
    font-size: 28rpx;
    font-weight: 500;
  }
}

.load-more {
  text-align: center;
  padding: 30rpx;
  font-size: 26rpx;
  color: #999;
  padding-bottom: 60rpx;
}
</style>
