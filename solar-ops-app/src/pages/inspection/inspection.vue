<template>
  <view class="inspection-page">
    <view class="header-section">
      <view class="sync-status" @click="handleSync">
        <view class="sync-icon" :class="{ syncing: isSyncing }">
          <text>🔄</text>
        </view>
        <view class="sync-info">
          <text class="sync-text">{{ isOnline ? '在线' : '离线' }}</text>
          <text class="sync-sub">{{ pendingCount }}项待同步</text>
        </view>
        <text class="sync-btn">{{ isSyncing ? '同步中...' : '点击同步' }}</text>
      </view>
    </view>

    <view class="download-section" v-if="isOnline">
      <view class="download-card" @click="handleDownloadTasks">
        <view class="download-icon">📥</view>
        <view class="download-text">
          <text class="download-title">下载巡检任务</text>
          <text class="download-desc">提前下载任务，离线也能巡检</text>
        </view>
        <text class="download-arrow">›</text>
      </view>

      <view class="download-card map-card" @click="handleOfflineMap">
        <view class="download-icon">🗺️</view>
        <view class="download-text">
          <text class="download-title">离线地图管理</text>
          <text class="download-desc">下载地图瓦片，无网也能看地图</text>
        </view>
        <text class="download-arrow">›</text>
      </view>
    </view>

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

    <view class="task-list" v-if="taskList.length > 0">
      <view 
        v-for="item in taskList" 
        :key="item.id" 
        class="task-card"
        @click="handleTaskDetail(item)"
      >
        <view class="task-header">
          <view class="task-name">{{ item.task_name }}</view>
          <view class="task-status" :class="getStatusClass(item.status)">
            {{ getStatusText(item.status) }}
          </view>
        </view>
        
        <view class="task-info">
          <view class="info-item">
            <text class="info-label">任务编号</text>
            <text class="info-value">{{ item.task_no }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">电站名称</text>
            <text class="info-value">{{ item.station_name }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">计划时间</text>
            <text class="info-value">{{ formatDate(item.plan_start_time) }}</text>
          </view>
        </view>

        <view class="task-footer">
          <view class="priority-tag" :class="getPriorityClass(item.priority)">
            {{ getPriorityText(item.priority) }}
          </view>
          <view class="task-type">{{ getTypeText(item.task_type) }}</view>
        </view>

        <view class="task-actions" v-if="item.status === 2">
          <view class="action-btn primary" @click.stop="handleContinue(item)">
            继续巡检
          </view>
        </view>
        <view class="task-actions" v-else-if="item.status === 1 || item.status === 0">
          <view class="action-btn primary" @click.stop="handleStart(item)">
            开始巡检
          </view>
        </view>
        <view class="task-actions" v-else-if="item.status === 3">
          <view class="action-btn" @click.stop="handleViewResult(item)">
            查看结果
          </view>
        </view>
      </view>
    </view>

    <view class="empty-state" v-else>
      <text class="empty-icon">📋</text>
      <text class="empty-text">{{ getEmptyText() }}</text>
      <view class="empty-btn" v-if="activeTab === 0 && isOnline" @click="handleDownloadTasks">
        下载任务
      </view>
    </view>

    <view class="bottom-space"></view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted, onShow, computed } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import inspectionDB from '@/utils/inspection-db.js'
import syncService from '@/utils/sync-service.js'
import { getToken } from '@/utils/auth.js'

const activeTab = ref(0)
const taskList = ref([])
const isSyncing = ref(false)
const isOnline = ref(true)
const pendingCount = ref(0)

const tabs = ref([
  { label: '待执行', statuses: [0, 1], count: 0 },
  { label: '进行中', statuses: [2], count: 0 },
  { label: '已完成', statuses: [3], count: 0 }
])

function getStatusText(status) {
  const map = {
    0: '待下载',
    1: '已下载',
    2: '进行中',
    3: '已完成',
    4: '已取消'
  }
  return map[status] || '未知'
}

function getStatusClass(status) {
  const map = {
    0: 'status-pending',
    1: 'status-downloaded',
    2: 'status-progress',
    3: 'status-completed',
    4: 'status-cancelled'
  }
  return map[status] || ''
}

function getPriorityText(priority) {
  const map = {
    1: '低优先级',
    2: '中优先级',
    3: '高优先级'
  }
  return map[priority] || '中优先级'
}

function getPriorityClass(priority) {
  const map = {
    1: 'priority-low',
    2: 'priority-medium',
    3: 'priority-high'
  }
  return map[priority] || 'priority-medium'
}

function getTypeText(type) {
  const map = {
    1: '日常巡检',
    2: '专项巡检',
    3: '定期检修'
  }
  return map[type] || '日常巡检'
}

function formatDate(dateStr) {
  if (!dateStr) return '--'
  const date = new Date(dateStr)
  const pad = n => n.toString().padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function switchTab(index) {
  activeTab.value = index
  fetchTaskList()
}

async function fetchTaskList() {
  try {
    const tab = tabs.value[activeTab.value]
    let allTasks = await inspectionDB.getTaskList()
    
    const filteredTasks = allTasks.filter(task => 
      tab.statuses.includes(task.status)
    )
    
    taskList.value = filteredTasks
    updateTabCounts(allTasks)
  } catch (err) {
    console.error('获取任务列表失败:', err)
    taskList.value = []
  }
}

function updateTabCounts(tasks) {
  tabs.value[0].count = tasks.filter(t => t.status === 0 || t.status === 1).length
  tabs.value[1].count = tasks.filter(t => t.status === 2).length
  tabs.value[2].count = tasks.filter(t => t.status === 3).length
}

async function handleDownloadTasks() {
  if (!isOnline.value) {
    uni.showToast({ title: '网络不可用', icon: 'none' })
    return
  }

  uni.showLoading({ title: '下载中...' })
  try {
    const userInfo = uni.getStorageSync('userInfo')
    const userId = userInfo?.id || userInfo?.userId || null
    
    const tasks = await syncService.downloadTasks(userId)
    uni.hideLoading()
    
    if (tasks && tasks.length > 0) {
      uni.showToast({ title: `下载成功，共${tasks.length}个任务`, icon: 'success' })
    } else {
      uni.showToast({ title: '暂无新任务', icon: 'none' })
    }
    
    fetchTaskList()
  } catch (err) {
    uni.hideLoading()
    console.error('下载任务失败:', err)
    uni.showToast({ title: '下载失败，请重试', icon: 'none' })
  }
}

function handleOfflineMap() {
  uni.navigateTo({
    url: '/pages/inspection/offline-map'
  })
}

async function handleStart(item) {
  uni.showModal({
    title: '开始巡检',
    content: `确定要开始「${item.task_name}」吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await inspectionDB.updateTaskStatus(item.id, 2)
          uni.showToast({ title: '已开始', icon: 'success' })
          
          setTimeout(() => {
            uni.navigateTo({
              url: `/pages/inspection/execute?taskId=${item.id}`
            })
          }, 500)
          
          fetchTaskList()
        } catch (err) {
          console.error('开始任务失败:', err)
          uni.showToast({ title: '操作失败', icon: 'none' })
        }
      }
    }
  })
}

function handleContinue(item) {
  uni.navigateTo({
    url: `/pages/inspection/execute?taskId=${item.id}`
  })
}

function handleTaskDetail(item) {
  uni.navigateTo({
    url: `/pages/inspection/task-detail?id=${item.id}`
  })
}

function handleViewResult(item) {
  uni.navigateTo({
    url: `/pages/inspection/result-detail?taskId=${item.id}`
  })
}

async function handleSync() {
  if (isSyncing.value) return
  if (!isOnline.value) {
    uni.showToast({ title: '网络不可用', icon: 'none' })
    return
  }

  isSyncing.value = true
  try {
    await syncService.trySync()
    uni.showToast({ title: '同步完成', icon: 'success' })
    updatePendingCount()
  } catch (err) {
    console.error('同步失败:', err)
    uni.showToast({ title: '同步失败', icon: 'none' })
  } finally {
    isSyncing.value = false
  }
}

async function updatePendingCount() {
  try {
    const results = await inspectionDB.getPendingSyncResults()
    const photos = await inspectionDB.getPendingSyncPhotos()
    const audios = await inspectionDB.getPendingSyncAudios()
    pendingCount.value = results.length + photos.length + audios.length
  } catch (err) {
    pendingCount.value = 0
  }
}

function getEmptyText() {
  if (activeTab.value === 0) return '暂无待执行任务'
  if (activeTab.value === 1) return '暂无进行中任务'
  return '暂无已完成任务'
}

function initSyncListener() {
  const status = syncService.getSyncStatus()
  isOnline.value = status.isOnline
  isSyncing.value = status.isSyncing

  syncService.addListener('networkChange', (data) => {
    isOnline.value = data.isConnected
  })

  syncService.addListener('syncStart', () => {
    isSyncing.value = true
  })

  syncService.addListener('syncComplete', () => {
    isSyncing.value = false
    updatePendingCount()
  })
}

onPullDownRefresh(async () => {
  try {
    if (isOnline.value) {
      await handleDownloadTasks()
      await syncService.trySync()
    }
    await fetchTaskList()
  } finally {
    uni.stopPullDownRefresh()
  }
})

onMounted(() => {
  syncService.init()
  initSyncListener()
  fetchTaskList()
  updatePendingCount()
})

onShow(() => {
  fetchTaskList()
  updatePendingCount()
})
</script>

<style lang="scss" scoped>
.inspection-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.header-section {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  padding: 30rpx;
}

.sync-status {
  display: flex;
  align-items: center;
  background-color: rgba(255, 255, 255, 0.2);
  border-radius: 16rpx;
  padding: 24rpx 28rpx;
}

.sync-icon {
  font-size: 40rpx;
  margin-right: 20rpx;
  
  &.syncing {
    animation: spin 1s linear infinite;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.sync-info {
  flex: 1;
}

.sync-text {
  display: block;
  font-size: 30rpx;
  color: #fff;
  font-weight: 600;
}

.sync-sub {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 6rpx;
}

.sync-btn {
  font-size: 26rpx;
  color: #fff;
  background-color: rgba(255, 255, 255, 0.3);
  padding: 12rpx 24rpx;
  border-radius: 30rpx;
}

.download-section {
  padding: 20rpx 30rpx;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.download-card {
  display: flex;
  align-items: center;
  background-color: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);

  &.map-card {
    background: linear-gradient(135deg, #e6f7ff, #f6ffed);
  }
}

.download-icon {
  font-size: 48rpx;
  margin-right: 20rpx;
}

.download-text {
  flex: 1;
}

.download-title {
  display: block;
  font-size: 30rpx;
  color: #333;
  font-weight: 600;
}

.download-desc {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
}

.download-arrow {
  font-size: 40rpx;
  color: #ccc;
}

.tabs {
  display: flex;
  background-color: #ffffff;
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
  color: #ffffff;
  font-size: 20rpx;
  border-radius: 16rpx;
  margin-left: 8rpx;
  vertical-align: middle;
}

.task-list {
  padding: 20rpx 30rpx;
}

.task-card {
  background-color: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20rpx;
}

.task-name {
  flex: 1;
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
}

.task-status {
  flex-shrink: 0;
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  margin-left: 16rpx;
  
  &.status-pending {
    background-color: #fff7e6;
    color: #fa8c16;
  }
  
  &.status-downloaded {
    background-color: #e6f7ff;
    color: #1890ff;
  }
  
  &.status-progress {
    background-color: #f6ffed;
    color: #52c41a;
  }
  
  &.status-completed {
    background-color: #f0f0f0;
    color: #999;
  }
  
  &.status-cancelled {
    background-color: #fff1f0;
    color: #ff4d4f;
  }
}

.task-info {
  margin-bottom: 20rpx;
}

.info-item {
  display: flex;
  margin-bottom: 12rpx;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.info-label {
  flex-shrink: 0;
  font-size: 26rpx;
  color: #999;
  width: 140rpx;
}

.info-value {
  flex: 1;
  font-size: 26rpx;
  color: #333;
}

.task-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.priority-tag {
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 6rpx;
  
  &.priority-low {
    background-color: #f0f0f0;
    color: #666;
  }
  
  &.priority-medium {
    background-color: #e6f7ff;
    color: #1890ff;
  }
  
  &.priority-high {
    background-color: #fff1f0;
    color: #ff4d4f;
  }
}

.task-type {
  font-size: 24rpx;
  color: #666;
}

.task-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.action-btn {
  font-size: 26rpx;
  padding: 16rpx 40rpx;
  border-radius: 40rpx;
  background-color: #f5f5f5;
  color: #666;
  
  &.primary {
    background-color: #1890ff;
    color: #fff;
  }
}

.empty-state {
  text-align: center;
  padding: 120rpx 0;
  
  .empty-icon {
    font-size: 100rpx;
    display: block;
    margin-bottom: 24rpx;
  }
  
  .empty-text {
    font-size: 28rpx;
    color: #999;
    display: block;
    margin-bottom: 30rpx;
  }
  
  .empty-btn {
    display: inline-block;
    font-size: 28rpx;
    color: #fff;
    background-color: #1890ff;
    padding: 20rpx 60rpx;
    border-radius: 40rpx;
  }
}

.bottom-space {
  height: 40rpx;
}
</style>
