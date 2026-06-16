<template>
  <view class="task-detail-page">
    <view class="task-header" v-if="task">
      <view class="task-title">{{ task.task_name }}</view>
      <view class="task-meta">
        <view class="meta-item">
          <text class="meta-label">任务编号</text>
          <text class="meta-value">{{ task.task_no }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-label">状态</text>
          <text class="meta-status" :class="getStatusClass(task.status)">
            {{ getStatusText(task.status) }}
          </text>
        </view>
      </view>
    </view>

    <view class="section" v-if="task">
      <view class="section-title">基本信息</view>
      <view class="info-list">
        <view class="info-item">
          <text class="info-label">电站名称</text>
          <text class="info-value">{{ task.station_name }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">任务类型</text>
          <text class="info-value">{{ getTypeText(task.task_type) }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">优先级</text>
          <text class="info-value">{{ getPriorityText(task.priority) }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">计划时间</text>
          <text class="info-value">{{ formatDate(task.plan_start_time) }} ~ {{ formatDate(task.plan_end_time) }}</text>
        </view>
        <view class="info-item" v-if="task.assignee_name">
          <text class="info-label">指派人员</text>
          <text class="info-value">{{ task.assignee_name }}</text>
        </view>
      </view>
    </view>

    <view class="section" v-if="task && task.description">
      <view class="section-title">任务描述</view>
      <view class="description-box">
        <text>{{ task.description }}</text>
      </view>
    </view>

    <view class="section">
      <view class="section-title">
        检查项 ({{ items.length }}项)
      </view>
      
      <view class="item-list" v-if="items.length > 0">
        <view 
          v-for="(item, index) in groupedItems" 
          :key="index"
          class="item-group"
        >
          <view class="group-title">{{ item.assetName || '其他' }}</view>
          <view 
            v-for="subItem in item.items" 
            :key="subItem.task_item_id || subItem.id"
            class="item-card"
          >
            <view class="item-header">
              <text class="item-name">{{ subItem.item_name }}</text>
              <text class="item-type">{{ getItemTypeText(subItem.item_type) }}</text>
            </view>
            <view class="item-desc" v-if="subItem.description">
              {{ subItem.description }}
            </view>
            <view class="item-standard" v-if="subItem.standard_value">
              <text class="standard-label">标准值：</text>
              <text class="standard-value">{{ subItem.standard_value }}{{ subItem.unit || '' }}</text>
            </view>
            <view class="item-tags">
              <text class="required-tag" v-if="subItem.is_required === 1">必填</text>
              <text class="asset-tag" v-if="subItem.asset_code">{{ subItem.asset_code }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="empty-items" v-else>
        <text>暂无检查项</text>
      </view>
    </view>

    <view class="bottom-bar" v-if="task && (task.status === 0 || task.status === 1)">
      <view class="bar-btn primary" @click="handleStartTask">
        开始巡检
      </view>
    </view>

    <view class="bottom-bar" v-else-if="task && task.status === 2">
      <view class="bar-btn primary" @click="handleContinue">
        继续巡检
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import inspectionDB from '@/utils/inspection-db.js'

const task = ref(null)
const items = ref([])
const taskId = ref(null)

const groupedItems = computed(() => {
  const groups = {}
  items.value.forEach(item => {
    const assetName = item.asset_name || '其他'
    if (!groups[assetName]) {
      groups[assetName] = []
    }
    groups[assetName].push(item)
  })
  
  return Object.keys(groups).map(assetName => ({
    assetName,
    items: groups[assetName]
  }))
})

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

function getTypeText(type) {
  const map = {
    1: '日常巡检',
    2: '专项巡检',
    3: '定期检修'
  }
  return map[type] || '日常巡检'
}

function getPriorityText(priority) {
  const map = {
    1: '低优先级',
    2: '中优先级',
    3: '高优先级'
  }
  return map[priority] || '中优先级'
}

function getItemTypeText(type) {
  const map = {
    1: '外观检查',
    2: '仪表读数',
    3: '声音检查',
    4: '红外测温',
    5: '功能测试'
  }
  return map[type] || '检查项'
}

function formatDate(dateStr) {
  if (!dateStr) return '--'
  const date = new Date(dateStr)
  const pad = n => n.toString().padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function fetchTaskDetail() {
  if (!taskId.value) return
  
  try {
    const detail = await inspectionDB.getTaskDetail(taskId.value)
    if (detail) {
      task.value = detail
      items.value = detail.items || []
    }
  } catch (err) {
    console.error('获取任务详情失败:', err)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

async function handleStartTask() {
  if (!taskId.value) return
  
  uni.showModal({
    title: '开始巡检',
    content: '确定要开始本次巡检吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await inspectionDB.updateTaskStatus(taskId.value, 2)
          uni.showToast({ title: '已开始', icon: 'success' })
          
          setTimeout(() => {
            uni.redirectTo({
              url: `/pages/inspection/execute?taskId=${taskId.value}`
            })
          }, 500)
        } catch (err) {
          console.error('开始任务失败:', err)
          uni.showToast({ title: '操作失败', icon: 'none' })
        }
      }
    }
  })
}

function handleContinue() {
  uni.redirectTo({
    url: `/pages/inspection/execute?taskId=${taskId.value}`
  })
}

onLoad((options) => {
  taskId.value = options.id ? parseInt(options.id) : null
  fetchTaskDetail()
})
</script>

<style lang="scss" scoped>
.task-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 120rpx;
}

.task-header {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  padding: 40rpx 30rpx;
  color: #fff;
}

.task-title {
  font-size: 36rpx;
  font-weight: 600;
  line-height: 1.4;
  margin-bottom: 24rpx;
}

.task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.meta-item {
  flex: 1;
  min-width: 45%;
}

.meta-label {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 8rpx;
}

.meta-value {
  font-size: 28rpx;
  color: #fff;
}

.meta-status {
  display: inline-block;
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  background-color: rgba(255, 255, 255, 0.3);
  
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

.section {
  background-color: #fff;
  margin: 20rpx 30rpx;
  border-radius: 16rpx;
  padding: 28rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.info-list {
  .info-item {
    display: flex;
    margin-bottom: 16rpx;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
}

.info-label {
  flex-shrink: 0;
  width: 160rpx;
  font-size: 26rpx;
  color: #999;
}

.info-value {
  flex: 1;
  font-size: 26rpx;
  color: #333;
}

.description-box {
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
  padding: 20rpx;
  background-color: #f9f9f9;
  border-radius: 12rpx;
}

.item-group {
  margin-bottom: 24rpx;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.group-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 16rpx;
  padding-left: 12rpx;
  border-left: 6rpx solid #1890ff;
}

.item-card {
  background-color: #f9f9f9;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 12rpx;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.item-name {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
}

.item-type {
  font-size: 22rpx;
  color: #1890ff;
  background-color: #e6f7ff;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
}

.item-desc {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 10rpx;
  line-height: 1.5;
}

.item-standard {
  font-size: 24rpx;
  margin-bottom: 10rpx;
}

.standard-label {
  color: #999;
}

.standard-value {
  color: #52c41a;
  font-weight: 500;
}

.item-tags {
  display: flex;
  gap: 10rpx;
  flex-wrap: wrap;
}

.required-tag {
  font-size: 20rpx;
  color: #ff4d4f;
  background-color: #fff1f0;
  padding: 4rpx 10rpx;
  border-radius: 6rpx;
}

.asset-tag {
  font-size: 20rpx;
  color: #666;
  background-color: #f0f0f0;
  padding: 4rpx 10rpx;
  border-radius: 6rpx;
}

.empty-items {
  text-align: center;
  padding: 60rpx 0;
  font-size: 26rpx;
  color: #999;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: #fff;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.bar-btn {
  font-size: 32rpx;
  text-align: center;
  padding: 24rpx 0;
  border-radius: 50rpx;
  
  &.primary {
    background-color: #1890ff;
    color: #fff;
  }
}
</style>
