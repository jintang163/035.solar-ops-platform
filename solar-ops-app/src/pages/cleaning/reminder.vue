<template>
  <view class="reminder-page">
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

    <view class="filter-section">
      <view class="level-filters">
        <view
          v-for="(level, idx) in levels"
          :key="idx"
          class="level-tag"
          :class="{ active: selectedLevel === level.value, ['lv-' + level.value]: true }"
          @click="filterByLevel(level.value)"
        >
          {{ level.label }}
        </view>
      </view>
    </view>

    <view class="reminder-list" v-if="reminderList.length > 0">
      <view
        class="reminder-card"
        v-for="item in reminderList"
        :key="item.id"
        @click="openDetail(item)"
      >
        <view class="card-level" :class="'level-' + item.dustLevel">
          {{ getLevelText(item.dustLevel) }}
        </view>
        <view class="card-body">
          <view class="card-head">
            <view class="reminder-title">{{ item.title }}</view>
            <view class="reminder-status" :class="'status-' + item.status">
              {{ getStatusText(item.status) }}
            </view>
          </view>

          <view class="card-meta">
            <view class="meta-item">
              <text class="meta-icon">🏢</text>
              <text class="meta-text">{{ item.stationName || '--' }}</text>
            </view>
            <view class="meta-item" v-if="item.arrayNumber || item.inverterName">
              <text class="meta-icon">📍</text>
              <text class="meta-text">{{ item.arrayNumber || item.inverterName }}</text>
            </view>
            <view class="meta-item">
              <text class="meta-icon">📅</text>
              <text class="meta-text">建议日期 {{ item.suggestCleanDate || item.createTime?.substring(0, 10) }}</text>
            </view>
          </view>

          <view class="card-stats">
            <view class="stat-item">
              <view class="stat-label">衰减率</view>
              <view class="stat-value warn">{{ item.attenuationRatePercent || '0' }}%</view>
            </view>
            <view class="stat-item">
              <view class="stat-label">连续下降</view>
              <view class="stat-value danger">{{ item.continuousDeclineDays || 0 }}天</view>
            </view>
            <view class="stat-item">
              <view class="stat-label">日损失</view>
              <view class="stat-value danger">~{{ item.estimatedDailyLossKwh || 0 }} kWh</view>
            </view>
          </view>

          <view class="card-ai-tip" v-if="item.dustLevel >= 1">
            <view class="ai-icon">🤖</view>
            <view class="ai-text">
              {{ generateAISuggestion(item) }}
            </view>
          </view>

          <view class="card-actions" v-if="item.status === 0">
            <view class="action-btn ignore" @click.stop="handleIgnore(item)">
              <text>忽略</text>
            </view>
            <view class="action-btn create" @click.stop="handleCreatePlan(item)">
              <text>创建清洗计划</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="empty-state" v-else>
      <text class="empty-icon">🔔</text>
      <text class="empty-text">{{ getEmptyText() }}</text>
    </view>

    <view class="load-more" v-if="reminderList.length > 0">
      <text v-if="loading">加载中...</text>
      <text v-else-if="noMore">没有更多了</text>
      <text v-else @click="loadMore">加载更多</text>
    </view>

    <view class="detail-modal" v-if="selectedItem" @click="selectedItem = null">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">清洗建议详情</text>
          <text class="modal-close" @click="selectedItem = null">×</text>
        </view>
        <scroll-view scroll-y class="modal-body" v-if="selectedItem">
          <view class="detail-section">
            <view class="detail-title">基本信息</view>
            <view class="detail-row">
              <view class="detail-k">提醒编号</view>
              <view class="detail-v">{{ selectedItem.reminderNo }}</view>
            </view>
            <view class="detail-row">
              <view class="detail-k">标题</view>
              <view class="detail-v">{{ selectedItem.title }}</view>
            </view>
            <view class="detail-row">
              <view class="detail-k">电站</view>
              <view class="detail-v">{{ selectedItem.stationName || '--' }}</view>
            </view>
            <view class="detail-row">
              <view class="detail-k">逆变器</view>
              <view class="detail-v">{{ selectedItem.inverterName || '--' }}</view>
            </view>
            <view class="detail-row">
              <view class="detail-k">方阵</view>
              <view class="detail-v">{{ selectedItem.arrayNumber || '--' }}</view>
            </view>
          </view>

          <view class="detail-section">
            <view class="detail-title">积灰检测数据</view>
            <view class="detail-stats">
              <view class="ds-item">
                <view class="ds-label">积灰等级</view>
                <view class="ds-value" :class="'lv-' + selectedItem.dustLevel">
                  {{ getLevelText(selectedItem.dustLevel) }}
                </view>
              </view>
              <view class="ds-item">
                <view class="ds-label">衰减率</view>
                <view class="ds-value">{{ selectedItem.attenuationRatePercent || 0 }}%</view>
              </view>
              <view class="ds-item">
                <view class="ds-label">连续下降天数</view>
                <view class="ds-value">{{ selectedItem.continuousDeclineDays || 0 }}天</view>
              </view>
              <view class="ds-item">
                <view class="ds-label">参考期PR值</view>
                <view class="ds-value">{{ selectedItem.referencePr || 0 }}%</view>
              </view>
              <view class="ds-item">
                <view class="ds-label">检测期PR值</view>
                <view class="ds-value">{{ selectedItem.detectPr || 0 }}%</view>
              </view>
              <view class="ds-item">
                <view class="ds-label">日损失电量</view>
                <view class="ds-value warn">~{{ selectedItem.estimatedDailyLossKwh || 0 }} kWh</view>
              </view>
            </view>
          </view>

          <view class="detail-section">
            <view class="detail-title">🤖 AI诊断建议</view>
            <view class="ai-detail-box">
              <view class="urgency-tag" :class="'urg-' + selectedItem.dustLevel">
                紧迫性：{{ getUrgencyText(selectedItem.dustLevel) }}
              </view>
              <view class="ai-text-block">
                {{ generateAISuggestion(selectedItem) }}
              </view>
              <view class="cost-estimate">
                <view class="cost-label">预估费用范围</view>
                <view class="cost-value">
                  ¥ {{ estimateCost(selectedItem).min }} - ¥ {{ estimateCost(selectedItem).max }}
                </view>
                <view class="cost-note">基于面积和清洗方式估算</view>
              </view>
              <view class="roi-tip">
                <text class="roi-icon">💰</text>
                <text class="roi-text">
                  预计 {{ calcPaybackDays(selectedItem) }} 天可通过增发电量回收清洗成本
                </text>
              </view>
            </view>
          </view>
        </scroll-view>
        <view class="modal-footer" v-if="selectedItem?.status === 0">
          <view class="footer-btn cancel" @click="handleIgnore(selectedItem); selectedItem = null">忽略建议</view>
          <view class="footer-btn confirm" @click="handleCreatePlan(selectedItem)">创建清洗计划</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import {
  getReminderPage,
  processReminder,
  ignoreReminder
} from '@/api/cleaning'

const activeTab = ref(0)
const selectedLevel = ref(null)
const loading = ref(false)
const noMore = ref(false)
const reminderList = ref([])
const selectedItem = ref(null)

const tabs = ref([
  { label: '待处理', status: 0, count: 0 },
  { label: '已创建计划', status: 1, count: 0 },
  { label: '已忽略', status: 2, count: 0 }
])

const levels = ref([
  { label: '全部', value: null },
  { label: '轻度', value: 1 },
  { label: '中度', value: 2 },
  { label: '重度', value: 3 }
])

const pageInfo = reactive({
  pageNum: 1,
  pageSize: 10
})

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

function getLevelText(level) {
  const map = { 0: '无积灰', 1: '轻度积灰', 2: '中度积灰', 3: '重度积灰' }
  return map[level] || '未知'
}

function getStatusText(status) {
  const map = { 0: '待处理', 1: '已创建计划', 2: '已忽略' }
  return map[status] || '未知'
}

function getUrgencyText(level) {
  const map = { 0: '较低', 1: '一般', 2: '较高', 3: '紧急' }
  return map[level] || '未知'
}

function generateAISuggestion(item) {
  const loss = item.estimatedDailyLossKwh || 0
  const days = item.continuousDeclineDays || 0
  const name = item.arrayNumber || item.inverterName || '该区域'
  const level = item.dustLevel

  if (level >= 3) {
    return `检测到${name}存在严重积灰问题（衰减率${item.attenuationRatePercent || 0}%），已连续${days}天呈下降趋势，日均损失约${loss}kWh。建议立即安排清洗作业，优先采用高压水枪配合人工的方式进行深度清洁，避免长期积灰导致组件热斑和不可逆衰减。`
  }
  if (level === 2) {
    return `${name}积灰情况较明显（衰减率${item.attenuationRatePercent || 0}%），建议在未来${days > 7 ? '3天' : '7天'}内安排清洗。预计清洗后可恢复约${Math.round(item.attenuationRatePercent || 0)}%的发电效率，减少日均${loss}kWh的电量损失。可选择机械清洗或人工清洗方式。`
  }
  if (level === 1) {
    return `${name}有轻度积灰迹象，已连续下降${days}天。可根据天气和排班情况，在未来2周内安排预防性清洗，或结合下次日常巡检时进行简单清洁。目前日均损失约${loss}kWh，可暂缓至中度时处理。`
  }
  return '检测结果正常，建议保持日常巡检。'
}

function estimateCost(item) {
  const base = 500
  const multiplier = { 1: 1.0, 2: 1.3, 3: 1.6 }[item.dustLevel] || 1.0
  return {
    min: Math.round(base * multiplier),
    max: Math.round(base * 2.5 * multiplier)
  }
}

function calcPaybackDays(item) {
  const cost = estimateCost(item)
  const avgCost = (cost.min + cost.max) / 2
  const dailySave = (item.estimatedDailyLossKwh || 10) * 0.8
  const pricePerKwh = 0.6
  const dailyValue = dailySave * pricePerKwh
  const days = Math.ceil(avgCost / dailyValue)
  return Math.min(Math.max(days, 3), 60)
}

function getEmptyText() {
  if (activeTab.value === 0) return '暂无待处理的清洗建议'
  if (activeTab.value === 1) return '暂无已创建计划的提醒'
  return '暂无已忽略的提醒'
}

async function fetchReminderList() {
  if (loading.value) return
  loading.value = true
  try {
    const tab = tabs.value[activeTab.value]
    const params = {
      status: tab.status,
      dustLevel: selectedLevel.value,
      pageNum: pageInfo.pageNum,
      pageSize: pageInfo.pageSize
    }
    const res = await getReminderPage(params)
    const list = res?.list || []
    if (pageInfo.pageNum === 1) {
      reminderList.value = list
    } else {
      reminderList.value = [...reminderList.value, ...list]
    }
    if (list.length < pageInfo.pageSize) {
      noMore.value = true
    }
  } catch (err) {
    console.error('获取提醒列表失败:', err)
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  try {
    const promises = tabs.value.map(tab =>
      getReminderPage({ status: tab.status, pageNum: 1, pageSize: 1 })
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
  selectedLevel.value = null
  pageInfo.pageNum = 1
  reminderList.value = []
  noMore.value = false
  fetchReminderList()
}

function filterByLevel(value) {
  selectedLevel.value = selectedLevel.value === value ? null : value
  pageInfo.pageNum = 1
  reminderList.value = []
  noMore.value = false
  fetchReminderList()
}

function loadMore() {
  if (noMore.value || loading.value) return
  pageInfo.pageNum++
  fetchReminderList()
}

function openDetail(item) {
  selectedItem.value = item
}

async function handleIgnore(item) {
  uni.showModal({
    title: '忽略建议',
    content: '确定忽略此清洗建议吗？',
    confirmColor: '#8c8c8c',
    success: async (res) => {
      if (res.confirm) {
        try {
          const { operatorId, operatorName } = getCurrentUser()
          await ignoreReminder(item.id, { operatorId, operatorName })
          uni.showToast({ title: '已忽略', icon: 'success' })
          refreshAll()
        } catch (err) {
          uni.showToast({ title: '操作失败', icon: 'none' })
        }
      }
    }
  })
}

function handleCreatePlan(item) {
  uni.navigateTo({
    url: `/pages/cleaning/plan-edit?reminderId=${item.id}`
  })
}

function refreshAll() {
  fetchStats()
  pageInfo.pageNum = 1
  reminderList.value = []
  noMore.value = false
  fetchReminderList()
}

onReachBottom(() => {
  loadMore()
})

onShow(() => {
  refreshAll()
})

onMounted(() => {
  refreshAll()
})
</script>

<style lang="scss" scoped>
.reminder-page {
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

.filter-section {
  background-color: #fff;
  padding: 20rpx 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.level-filters {
  display: flex;
  gap: 16rpx;
  flex-wrap: wrap;
}

.level-tag {
  padding: 10rpx 28rpx;
  border-radius: 30rpx;
  font-size: 24rpx;
  background-color: #f5f5f5;
  color: #666;

  &.active {
    color: #fff;
    background-color: #1890ff;
  }

  &.lv-1.active { background-color: #faad14; }
  &.lv-2.active { background-color: #fa8c16; }
  &.lv-3.active { background-color: #f5222d; }
}

.reminder-list {
  padding: 20rpx 30rpx;
}

.reminder-card {
  background-color: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.card-level {
  padding: 14rpx 30rpx;
  color: #fff;
  font-size: 26rpx;
  font-weight: 600;

  &.level-0 { background-color: #52c41a; }
  &.level-1 { background-color: #faad14; }
  &.level-2 { background-color: #fa8c16; }
  &.level-3 { background: linear-gradient(135deg, #f5222d 0%, #ff4d4f 100%); }
}

.card-body {
  padding: 24rpx 30rpx;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16rpx;
}

.reminder-title {
  flex: 1;
  font-size: 30rpx;
  font-weight: 600;
  color: #262626;
  line-height: 1.4;
  padding-right: 16rpx;
}

.reminder-status {
  padding: 6rpx 16rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
  flex-shrink: 0;

  &.status-0 { background-color: #fff2e8; color: #fa8c16; }
  &.status-1 { background-color: #e6f7ff; color: #1890ff; }
  &.status-2 { background-color: #f5f5f5; color: #8c8c8c; }
}

.card-meta {
  margin-bottom: 16rpx;

  .meta-item {
    display: flex;
    align-items: center;
    margin-bottom: 8rpx;
  }

  .meta-icon {
    font-size: 22rpx;
    margin-right: 10rpx;
    width: 28rpx;
  }

  .meta-text {
    font-size: 24rpx;
    color: #8c8c8c;
  }
}

.card-stats {
  display: flex;
  background-color: #fafafa;
  border-radius: 12rpx;
  padding: 16rpx 0;

  .stat-item {
    flex: 1;
    text-align: center;
    border-right: 1rpx solid #f0f0f0;

    &:last-child { border-right: none; }
  }

  .stat-label {
    font-size: 22rpx;
    color: #8c8c8c;
    margin-bottom: 6rpx;
  }

  .stat-value {
    font-size: 28rpx;
    font-weight: 600;
    color: #262626;

    &.warn { color: #fa8c16; }
    &.danger { color: #f5222d; }
  }
}

.card-ai-tip {
  display: flex;
  align-items: flex-start;
  margin-top: 16rpx;
  padding: 16rpx;
  background: linear-gradient(135deg, #f9f0ff 0%, #fff0f6 100%);
  border-radius: 10rpx;

  .ai-icon {
    font-size: 28rpx;
    margin-right: 12rpx;
    flex-shrink: 0;
    margin-top: 2rpx;
  }

  .ai-text {
    flex: 1;
    font-size: 24rpx;
    color: #531dab;
    line-height: 1.6;
  }
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 20rpx;
  margin-top: 20rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f5f5f5;

  .action-btn {
    padding: 12rpx 32rpx;
    border-radius: 8rpx;
    font-size: 26rpx;
    font-weight: 500;

    &.ignore {
      background-color: #f5f5f5;
      color: #8c8c8c;
    }

    &.create {
      background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
      color: #fff;
    }
  }
}

.empty-state {
  text-align: center;
  padding: 200rpx 0;

  .empty-icon {
    display: block;
    font-size: 120rpx;
    margin-bottom: 32rpx;
  }

  .empty-text {
    display: block;
    font-size: 28rpx;
    color: #999;
  }
}

.load-more {
  text-align: center;
  padding: 30rpx;
  font-size: 26rpx;
  color: #999;
  padding-bottom: 60rpx;
}

.detail-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;

  .modal-content {
    width: 100%;
    background-color: #fff;
    border-radius: 32rpx 32rpx 0 0;
    max-height: 88vh;
    display: flex;
    flex-direction: column;
  }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 32rpx 30rpx;
    border-bottom: 1rpx solid #f5f5f5;
  }

  .modal-title {
    font-size: 34rpx;
    font-weight: 600;
    color: #262626;
  }

  .modal-close {
    font-size: 48rpx;
    color: #bfbfbf;
    line-height: 1;
  }

  .modal-body {
    flex: 1;
    max-height: 65vh;
    padding: 30rpx;
  }

  .modal-footer {
    display: flex;
    border-top: 1rpx solid #f5f5f5;
    padding: 20rpx 30rpx;
    padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
    gap: 20rpx;

    .footer-btn {
      flex: 1;
      height: 88rpx;
      line-height: 88rpx;
      text-align: center;
      border-radius: 44rpx;
      font-size: 30rpx;
      font-weight: 500;

      &.cancel {
        background-color: #f5f5f5;
        color: #595959;
      }

      &.confirm {
        background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
        color: #fff;
      }
    }
  }
}

.detail-section {
  margin-bottom: 32rpx;

  &:last-child { margin-bottom: 0; }

  .detail-title {
    font-size: 28rpx;
    font-weight: 600;
    color: #262626;
    margin-bottom: 20rpx;
    padding-left: 12rpx;
    border-left: 6rpx solid #1890ff;
  }
}

.detail-row {
  display: flex;
  margin-bottom: 16rpx;

  .detail-k {
    width: 160rpx;
    font-size: 26rpx;
    color: #8c8c8c;
    flex-shrink: 0;
  }

  .detail-v {
    flex: 1;
    font-size: 26rpx;
    color: #262626;
  }
}

.detail-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;

  .ds-item {
    width: calc(50% - 8rpx);
    background-color: #fafafa;
    border-radius: 12rpx;
    padding: 16rpx 20rpx;
  }

  .ds-label {
    font-size: 22rpx;
    color: #8c8c8c;
    margin-bottom: 8rpx;
  }

  .ds-value {
    font-size: 28rpx;
    font-weight: 600;
    color: #262626;

    &.lv-1 { color: #faad14; }
    &.lv-2 { color: #fa8c16; }
    &.lv-3 { color: #f5222d; }
    &.warn { color: #d46b08; }
  }
}

.ai-detail-box {
  background: linear-gradient(135deg, #f9f0ff 0%, #fff0f6 100%);
  border-radius: 16rpx;
  padding: 24rpx;

  .urgency-tag {
    display: inline-block;
    padding: 8rpx 20rpx;
    border-radius: 20rpx;
    font-size: 24rpx;
    font-weight: 500;
    margin-bottom: 16rpx;

    &.urg-1 { background-color: #fff7e6; color: #faad14; }
    &.urg-2 { background-color: #fff2e8; color: #fa8c16; }
    &.urg-3 { background-color: #fff1f0; color: #f5222d; }
  }

  .ai-text-block {
    font-size: 26rpx;
    color: #531dab;
    line-height: 1.7;
    margin-bottom: 20rpx;
  }

  .cost-estimate {
    background-color: rgba(255, 255, 255, 0.7);
    border-radius: 12rpx;
    padding: 16rpx 20rpx;
    margin-bottom: 16rpx;
  }

  .cost-label {
    font-size: 22rpx;
    color: #8c8c8c;
    margin-bottom: 6rpx;
  }

  .cost-value {
    font-size: 32rpx;
    font-weight: 700;
    color: #531dab;
  }

  .cost-note {
    font-size: 22rpx;
    color: #8c8c8c;
    margin-top: 4rpx;
  }

  .roi-tip {
    display: flex;
    align-items: center;
    padding: 14rpx 18rpx;
    background-color: rgba(255, 255, 255, 0.8);
    border-radius: 10rpx;
  }

  .roi-icon {
    font-size: 28rpx;
    margin-right: 10rpx;
  }

  .roi-text {
    flex: 1;
    font-size: 24rpx;
    color: #389e0d;
    font-weight: 500;
  }
}
</style>
