<template>
  <view class="drone-page">
    <view class="stats-row">
      <view class="stat-card">
        <text class="stat-value">{{ stats.totalTasks || 0 }}</text>
        <text class="stat-label">巡检任务</text>
      </view>
      <view class="stat-card warning">
        <text class="stat-value">{{ stats.pendingDefects || 0 }}</text>
        <text class="stat-label">待确认缺陷</text>
      </view>
      <view class="stat-card danger">
        <text class="stat-value">{{ stats.seriousDefects || 0 }}</text>
        <text class="stat-label">严重缺陷</text>
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

    <view class="content-area">
      <view v-if="activeTab === 0">
        <view class="task-list" v-if="taskList.length > 0">
          <view 
            v-for="item in taskList" 
            :key="item.id" 
            class="task-card"
            @click="handleTaskDetail(item)"
          >
            <view class="task-header">
              <text class="task-name">{{ item.taskName }}</text>
              <view class="task-status" :class="'status-' + item.status">
                {{ getTaskStatusText(item.status) }}
              </view>
            </view>
            <view class="task-info">
              <view class="info-row">
                <text class="info-label">电站：</text>
                <text class="info-value">{{ item.stationName || '--' }}</text>
              </view>
              <view class="info-row">
                <text class="info-label">图片数量：</text>
                <text class="info-value">{{ item.imageCount || 0 }} 张</text>
              </view>
              <view class="info-row">
                <text class="info-label">发现缺陷：</text>
                <text class="info-value defect-count">{{ item.defectCount || 0 }} 处</text>
              </view>
              <view class="info-row">
                <text class="info-label">巡检时间：</text>
                <text class="info-value">{{ formatDate(item.inspectionTime, 'MM-DD HH:mm') }}</text>
              </view>
            </view>
            <view class="task-footer">
              <text class="create-time">{{ formatDate(item.createTime, 'YYYY-MM-DD HH:mm') }}</text>
            </view>
          </view>
        </view>
        
        <view class="empty-state" v-else>
          <text class="empty-icon">🚁</text>
          <text class="empty-text">暂无巡检任务</text>
        </view>
      </view>

      <view v-if="activeTab === 1">
        <view class="defect-list" v-if="defectList.length > 0">
          <view 
            v-for="item in defectList" 
            :key="item.id" 
            class="defect-card"
            @click="handleDefectDetail(item)"
          >
            <view class="defect-header">
              <view class="defect-type" :class="'type-' + item.defectType">
                {{ getDefectTypeText(item.defectType) }}
              </view>
              <view class="defect-level" :class="'level-' + item.defectLevel">
                {{ getDefectLevelText(item.defectLevel) }}
              </view>
            </view>
            <view class="defect-body">
              <image 
                :src="item.annotatedImageUrl || item.imageUrl" 
                mode="aspectFill" 
                class="defect-image"
              />
              <view class="defect-info">
                <view class="info-row">
                  <text class="info-label">置信度：</text>
                  <text class="info-value">{{ formatPercent(item.confidence) }}</text>
                </view>
                <view class="info-row" v-if="item.temperature">
                  <text class="info-label">温度：</text>
                  <text class="info-value temp">{{ item.temperature }}°C</text>
                </view>
                <view class="info-row">
                  <text class="info-label">位置：</text>
                  <text class="info-value">({{ item.centerX }}, {{ item.centerY }})</text>
                </view>
                <view class="info-row">
                  <text class="info-label">状态：</text>
                  <view class="confirm-status" :class="'confirm-' + item.confirmed">
                    {{ item.confirmed === 1 ? '已确认' : '待确认' }}
                  </view>
                </view>
              </view>
            </view>
            <view class="defect-footer">
              <text class="station-name">{{ item.stationName || '--' }}</text>
              <text class="detect-time">{{ formatDate(item.detectTime, 'MM-DD HH:mm') }}</text>
            </view>
            <view class="defect-actions" v-if="item.confirmed !== 1">
              <button class="btn-confirm" @click.stop="handleConfirm(item)">确认缺陷</button>
              <button class="btn-order" @click.stop="handleGenerateOrder(item)">生成工单</button>
            </view>
          </view>
        </view>
        
        <view class="empty-state" v-else>
          <text class="empty-icon">🔍</text>
          <text class="empty-text">暂无缺陷记录</text>
        </view>
      </view>
    </view>

    <view class="load-more" v-if="(activeTab === 0 && taskList.length > 0) || (activeTab === 1 && defectList.length > 0)">
      <text v-if="loading">加载中...</text>
      <text v-else-if="noMore">没有更多了</text>
      <text v-else @click="loadMore">加载更多</text>
    </view>

    <view class="bottom-space"></view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted, onPullDownRefresh } from 'vue'
import { onReachBottom } from '@dcloudio/uni-app'
import { 
  getInspectionTaskPage, 
  getInspectionStatistics,
  getDefectPage,
  confirmDefect,
  generateWorkOrder
} from '@/api/drone'
import { formatDate, formatPercent } from '@/utils/format'

const activeTab = ref(0)
const loading = ref(false)
const noMore = ref(false)
const taskList = ref([])
const defectList = ref([])
const stats = ref({})

const pageInfo = reactive({
  pageNum: 1,
  pageSize: 10
})

const tabs = ref([
  { label: '巡检任务', count: 0 },
  { label: '缺陷告警', count: 0 }
])

const taskStatusMap = {
  0: '待执行',
  1: '执行中',
  2: '已完成',
  3: '已取消'
}

const defectTypeMap = {
  'HOT_SPOT': '热斑',
  'MICROCRACK': '隐裂',
  'SHADOW': '遮挡',
  'DELAMINATION': '脱层',
  'BROKEN': '破损',
  'DIRTY': '脏污'
}

const defectLevelMap = {
  1: '轻微',
  2: '一般',
  3: '严重'
}

function getTaskStatusText(status) {
  return taskStatusMap[status] || '未知'
}

function getDefectTypeText(type) {
  return defectTypeMap[type] || type || '未知'
}

function getDefectLevelText(level) {
  return defectLevelMap[level] || '未知'
}

function switchTab(index) {
  activeTab.value = index
  pageInfo.pageNum = 1
  taskList.value = []
  defectList.value = []
  noMore.value = false
  fetchList()
}

async function fetchList() {
  if (loading.value) return
  
  loading.value = true
  try {
    if (activeTab.value === 0) {
      const res = await getInspectionTaskPage({
        pageNum: pageInfo.pageNum,
        pageSize: pageInfo.pageSize
      })
      handleTaskResponse(res)
    } else {
      const res = await getDefectPage({
        pageNum: pageInfo.pageNum,
        pageSize: pageInfo.pageSize
      })
      handleDefectResponse(res)
    }
  } catch (err) {
    console.error('获取列表失败:', err)
    if (pageInfo.pageNum === 1) {
      if (activeTab.value === 0) {
        taskList.value = []
      } else {
        defectList.value = []
      }
    }
  } finally {
    loading.value = false
  }
}

function handleTaskResponse(res) {
  if (res?.list) {
    if (pageInfo.pageNum === 1) {
      taskList.value = res.list
    } else {
      taskList.value = [...taskList.value, ...res.list]
    }
    if (res.list.length < pageInfo.pageSize) {
      noMore.value = true
    }
  } else {
    if (pageInfo.pageNum === 1) {
      taskList.value = []
    }
    noMore.value = true
  }
}

function handleDefectResponse(res) {
  if (res?.list) {
    if (pageInfo.pageNum === 1) {
      defectList.value = res.list
    } else {
      defectList.value = [...defectList.value, ...res.list]
    }
    if (res.list.length < pageInfo.pageSize) {
      noMore.value = true
    }
  } else {
    if (pageInfo.pageNum === 1) {
      defectList.value = []
    }
    noMore.value = true
  }
}

async function fetchStats() {
  try {
    const data = await getInspectionStatistics()
    if (data) {
      stats.value = data
      tabs.value[0].count = data.totalTasks || 0
      tabs.value[1].count = data.pendingDefects || 0
    }
  } catch (err) {
    console.error('获取统计失败:', err)
  }
}

function handleTaskDetail(item) {
  uni.navigateTo({
    url: `/pages/drone/detail?id=${item.id}&type=task`
  })
}

function handleDefectDetail(item) {
  uni.navigateTo({
    url: `/pages/drone/detail?id=${item.id}&type=defect`
  })
}

async function handleConfirm(item) {
  uni.showModal({
    title: '确认缺陷',
    content: `确认该${getDefectTypeText(item.defectType)}缺陷吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await confirmDefect({
            id: item.id,
            confirmed: 1,
            confirmRemark: '移动端确认'
          })
          uni.showToast({ title: '确认成功', icon: 'success' })
          item.confirmed = 1
          fetchStats()
        } catch (err) {
          console.error('确认失败:', err)
          uni.showToast({ title: '确认失败', icon: 'none' })
        }
      }
    }
  })
}

async function handleGenerateOrder(item) {
  uni.showModal({
    title: '生成工单',
    content: `确定要为该缺陷生成工单吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await generateWorkOrder({
            defectId: item.id,
            defectType: item.defectType,
            defectLevel: item.defectLevel,
            stationId: item.stationId,
            description: `${getDefectTypeText(item.defectType)}缺陷，位置(${item.centerX}, ${item.centerY})`
          })
          uni.showToast({ title: '工单已生成', icon: 'success' })
          fetchList()
        } catch (err) {
          console.error('生成工单失败:', err)
          uni.showToast({ title: '生成失败', icon: 'none' })
        }
      }
    }
  })
}

function loadMore() {
  if (noMore.value || loading.value) return
  pageInfo.pageNum++
  fetchList()
}

onReachBottom(() => {
  loadMore()
})

onPullDownRefresh(() => {
  pageInfo.pageNum = 1
  noMore.value = false
  if (activeTab.value === 0) {
    taskList.value = []
  } else {
    defectList.value = []
  }
  Promise.all([fetchStats(), fetchList()]).finally(() => {
    uni.stopPullDownRefresh()
  })
})

onMounted(() => {
  fetchStats()
  fetchList()
})
</script>

<style lang="scss" scoped>
.drone-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.stats-row {
  display: flex;
  padding: 24rpx;
  gap: 20rpx;
  background-color: #fff;
  margin-bottom: 20rpx;
}

.stat-card {
  flex: 1;
  text-align: center;
  padding: 30rpx 20rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16rpx;

  &.warning {
    background: linear-gradient(135deg, #f6d365 0%, #fda085 100%);
  }

  &.danger {
    background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%);
  }
}

.stat-value {
  display: block;
  font-size: 48rpx;
  font-weight: bold;
  color: #fff;
  margin-bottom: 8rpx;
}

.stat-label {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.9);
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

.content-area {
  padding: 20rpx 30rpx;
}

.task-card,
.defect-card {
  background-color: #fff;
  border-radius: 16rpx;
  margin-bottom: 24rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);
}

.task-header,
.defect-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.task-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.task-status {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;

  &.status-0 {
    background-color: #e6f7ff;
    color: #1890ff;
  }

  &.status-1 {
    background-color: #fff7e6;
    color: #fa8c16;
  }

  &.status-2 {
    background-color: #f6ffed;
    color: #52c41a;
  }

  &.status-3 {
    background-color: #f5f5f5;
    color: #999;
  }
}

.defect-type {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
  font-weight: 500;

  &.type-HOT_SPOT {
    background-color: #fff1f0;
    color: #f5222d;
  }

  &.type-MICROCRACK {
    background-color: #fff7e6;
    color: #fa8c16;
  }

  &.type-SHADOW {
    background-color: #e6f7ff;
    color: #1890ff;
  }

  &.type-DELAMINATION {
    background-color: #fffbe6;
    color: #faad14;
  }

  &.type-BROKEN {
    background-color: #fff1f0;
    color: #f5222d;
  }

  &.type-DIRTY {
    background-color: #f6ffed;
    color: #52c41a;
  }
}

.defect-level {
  padding: 6rpx 16rpx;
  border-radius: 16rpx;
  font-size: 22rpx;

  &.level-1 {
    background-color: #f6ffed;
    color: #52c41a;
  }

  &.level-2 {
    background-color: #fffbe6;
    color: #faad14;
  }

  &.level-3 {
    background-color: #fff1f0;
    color: #f5222d;
  }
}

.task-info {
  padding: 20rpx 24rpx;
}

.info-row {
  display: flex;
  margin-bottom: 12rpx;
  align-items: center;

  &:last-child {
    margin-bottom: 0;
  }
}

.info-label {
  font-size: 26rpx;
  color: #999;
  min-width: 140rpx;
}

.info-value {
  font-size: 26rpx;
  color: #333;
  flex: 1;

  &.defect-count {
    color: #f5222d;
    font-weight: 500;
  }

  &.temp {
    color: #ff4d4f;
    font-weight: 500;
  }
}

.confirm-status {
  padding: 4rpx 12rpx;
  border-radius: 12rpx;
  font-size: 22rpx;

  &.confirm-1 {
    background-color: #f6ffed;
    color: #52c41a;
  }

  &.confirm-0 {
    background-color: #fff7e6;
    color: #fa8c16;
  }
}

.task-footer {
  padding: 16rpx 24rpx;
  border-top: 1rpx solid #f0f0f0;
}

.create-time {
  font-size: 24rpx;
  color: #999;
}

.defect-body {
  display: flex;
  padding: 20rpx;
  gap: 20rpx;
}

.defect-image {
  width: 180rpx;
  height: 180rpx;
  border-radius: 12rpx;
  background-color: #f5f5f5;
  flex-shrink: 0;
}

.defect-info {
  flex: 1;
}

.defect-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 24rpx;
  border-top: 1rpx solid #f0f0f0;
}

.station-name {
  font-size: 24rpx;
  color: #666;
}

.detect-time {
  font-size: 24rpx;
  color: #999;
}

.defect-actions {
  display: flex;
  gap: 20rpx;
  padding: 20rpx 24rpx;
  border-top: 1rpx solid #f0f0f0;
}

.btn-confirm,
.btn-order {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 26rpx;
  border-radius: 36rpx;
  border: none;
  margin: 0;
}

.btn-confirm {
  background-color: #e6f7ff;
  color: #1890ff;
}

.btn-order {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
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
  }
}

.load-more {
  text-align: center;
  padding: 30rpx;
  font-size: 26rpx;
  color: #999;
}

.bottom-space {
  height: 40rpx;
}
</style>
