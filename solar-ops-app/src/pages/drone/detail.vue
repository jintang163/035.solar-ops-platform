<template>
  <view class="detail-page">
    <view v-if="type === 'task'" class="task-detail">
      <view class="detail-header">
        <text class="detail-title">{{ taskData.taskName }}</text>
        <view class="task-status" :class="'status-' + taskData.status">
          {{ getTaskStatusText(taskData.status) }}
        </view>
      </view>

      <view class="info-section">
        <text class="section-title">基本信息</text>
        <view class="info-card">
          <view class="info-item">
            <text class="info-label">电站名称</text>
            <text class="info-value">{{ taskData.stationName || '--' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">巡检人员</text>
            <text class="info-value">{{ taskData.operatorName || '--' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">巡检时间</text>
            <text class="info-value">{{ formatDateTime(taskData.inspectionTime) }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">创建时间</text>
            <text class="info-value">{{ formatDateTime(taskData.createTime) }}</text>
          </view>
        </view>
      </view>

      <view class="info-section">
        <text class="section-title">巡检统计</text>
        <view class="stats-grid">
          <view class="stat-item">
            <text class="stat-value">{{ taskData.imageCount || 0 }}</text>
            <text class="stat-label">图片总数</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ taskData.detectedImageCount || 0 }}</text>
            <text class="stat-label">已检测</text>
          </view>
          <view class="stat-item defect">
            <text class="stat-value">{{ taskData.defectCount || 0 }}</text>
            <text class="stat-label">缺陷总数</text>
          </view>
          <view class="stat-item confirmed">
            <text class="stat-value">{{ taskData.confirmedDefectCount || 0 }}</text>
            <text class="stat-label">已确认</text>
          </view>
        </view>
      </view>

      <view class="info-section" v-if="taskData.remark">
        <text class="section-title">备注</text>
        <view class="remark-card">
          <text class="remark-text">{{ taskData.remark }}</text>
        </view>
      </view>
    </view>

    <view v-if="type === 'defect'" class="defect-detail">
      <view class="detail-header">
        <view class="defect-tags">
          <view class="defect-type" :class="'type-' + defectData.defectType">
            {{ getDefectTypeText(defectData.defectType) }}
          </view>
          <view class="defect-level" :class="'level-' + defectData.defectLevel">
            {{ getDefectLevelText(defectData.defectLevel) }}
          </view>
        </view>
        <view class="confirm-status" :class="'confirm-' + defectData.confirmed">
          {{ defectData.confirmed === 1 ? '已确认' : '待确认' }}
        </view>
      </view>

      <view class="image-section">
        <image 
          :src="defectData.annotatedImageUrl || defectData.imageUrl" 
          mode="widthFix" 
          class="defect-image-large"
          @click="previewImage"
        />
        <view class="image-meta">
          <text class="meta-text">点击图片可预览</text>
        </view>
      </view>

      <view class="info-section">
        <text class="section-title">缺陷信息</text>
        <view class="info-card">
          <view class="info-item">
            <text class="info-label">置信度</text>
            <text class="info-value">{{ formatPercent(defectData.confidence) }}</text>
          </view>
          <view class="info-item" v-if="defectData.temperature">
            <text class="info-label">温度</text>
            <text class="info-value temp">{{ defectData.temperature }}°C</text>
          </view>
          <view class="info-item">
            <text class="info-label">组件位置</text>
            <text class="info-value">组件 {{ defectData.componentRow || '-' }} 行 {{ defectData.componentCol || '-' }} 列</text>
          </view>
          <view class="info-item">
            <text class="info-label">像素坐标</text>
            <text class="info-value">中心 ({{ defectData.centerX }}, {{ defectData.centerY }})</text>
          </view>
          <view class="info-item">
            <text class="info-label">边界框</text>
            <text class="info-value">[{{ defectData.bboxX1 }}, {{ defectData.bboxY1 }}, {{ defectData.bboxX2 }}, {{ defectData.bboxY2 }}]</text>
          </view>
          <view class="info-item">
            <text class="info-label">所属电站</text>
            <text class="info-value">{{ defectData.stationName || '--' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">检测时间</text>
            <text class="info-value">{{ formatDateTime(defectData.detectTime) }}</text>
          </view>
        </view>
      </view>

      <view class="info-section" v-if="defectData.description">
        <text class="section-title">AI描述</text>
        <view class="remark-card">
          <text class="remark-text">{{ defectData.description }}</text>
        </view>
      </view>

      <view class="info-section" v-if="defectData.confirmRemark">
        <text class="section-title">确认备注</text>
        <view class="remark-card">
          <text class="remark-text">{{ defectData.confirmRemark }}</text>
        </view>
      </view>

      <view class="info-section" v-if="defectData.workOrderId">
        <text class="section-title">关联工单</text>
        <view class="order-card" @click="goToOrder">
          <view class="order-info">
            <text class="order-no">工单编号：{{ defectData.workOrderNo || '--' }}</text>
            <text class="order-status">状态：{{ getOrderStatus(defectData.workOrderStatus) }}</text>
          </view>
          <text class="order-arrow">›</text>
        </view>
      </view>
    </view>

    <view class="bottom-actions" v-if="type === 'defect' && defectData.confirmed !== 1">
      <button class="btn-secondary" @click="handleConfirm">确认缺陷</button>
      <button class="btn-primary" @click="handleGenerateOrder">生成工单</button>
    </view>

    <view class="bottom-space"></view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { 
  getInspectionTaskDetail, 
  getDefectDetail,
  confirmDefect,
  generateWorkOrder
} from '@/api/drone'
import { formatDate, formatPercent } from '@/utils/format'

const type = ref('task')
const id = ref('')
const taskData = ref({})
const defectData = ref({})

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

const orderStatusMap = {
  0: '待接单',
  1: '处理中',
  2: '待验收',
  3: '已完成',
  4: '已取消'
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

function getOrderStatus(status) {
  return orderStatusMap[status] || '未知'
}

function formatDateTime(date) {
  return formatDate(date, 'YYYY-MM-DD HH:mm:ss')
}

async function fetchTaskDetail() {
  try {
    const data = await getInspectionTaskDetail(id.value)
    if (data) {
      taskData.value = data
      uni.setNavigationBarTitle({ title: '巡检任务详情' })
    }
  } catch (err) {
    console.error('获取任务详情失败:', err)
    uni.showToast({ title: '获取详情失败', icon: 'none' })
  }
}

async function fetchDefectDetail() {
  try {
    const data = await getDefectDetail(id.value)
    if (data) {
      defectData.value = data
      uni.setNavigationBarTitle({ title: '缺陷详情' })
    }
  } catch (err) {
    console.error('获取缺陷详情失败:', err)
    uni.showToast({ title: '获取详情失败', icon: 'none' })
  }
}

function previewImage() {
  const url = defectData.value.annotatedImageUrl || defectData.value.imageUrl
  if (url) {
    uni.previewImage({
      urls: [url],
      current: url
    })
  }
}

function goToOrder() {
  if (defectData.value.workOrderId) {
    uni.navigateTo({
      url: `/pages/workorder/detail?id=${defectData.value.workOrderId}`
    })
  }
}

async function handleConfirm() {
  uni.showModal({
    title: '确认缺陷',
    content: `确认该${getDefectTypeText(defectData.value.defectType)}缺陷吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await confirmDefect({
            id: id.value,
            confirmed: 1,
            confirmRemark: '移动端确认'
          })
          uni.showToast({ title: '确认成功', icon: 'success' })
          defectData.value.confirmed = 1
          defectData.value.confirmRemark = '移动端确认'
        } catch (err) {
          console.error('确认失败:', err)
          uni.showToast({ title: '确认失败', icon: 'none' })
        }
      }
    }
  })
}

async function handleGenerateOrder() {
  uni.showModal({
    title: '生成工单',
    content: `确定要为该缺陷生成工单吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await generateWorkOrder({
            defectId: id.value,
            defectType: defectData.value.defectType,
            defectLevel: defectData.value.defectLevel,
            stationId: defectData.value.stationId,
            description: `${getDefectTypeText(defectData.value.defectType)}缺陷，位置(${defectData.value.centerX}, ${defectData.value.centerY})`
          })
          uni.showToast({ title: '工单已生成', icon: 'success' })
          fetchDefectDetail()
        } catch (err) {
          console.error('生成工单失败:', err)
          uni.showToast({ title: '生成失败', icon: 'none' })
        }
      }
    }
  })
}

onLoad((options) => {
  id.value = options.id
  type.value = options.type || 'task'
  
  if (type.value === 'task') {
    fetchTaskDetail()
  } else {
    fetchDefectDetail()
  }
})
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 140rpx;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  background-color: #fff;
  margin-bottom: 20rpx;
}

.detail-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #333;
}

.defect-tags {
  display: flex;
  gap: 16rpx;
}

.task-status {
  padding: 10rpx 24rpx;
  border-radius: 24rpx;
  font-size: 26rpx;

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
  padding: 10rpx 24rpx;
  border-radius: 24rpx;
  font-size: 26rpx;
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
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;

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

.confirm-status {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;

  &.confirm-1 {
    background-color: #f6ffed;
    color: #52c41a;
  }

  &.confirm-0 {
    background-color: #fff7e6;
    color: #fa8c16;
  }
}

.info-section {
  margin-bottom: 24rpx;
}

.section-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  padding: 0 30rpx 16rpx;
}

.info-card,
.remark-card,
.order-card {
  background-color: #fff;
  margin: 0 24rpx;
  border-radius: 16rpx;
  padding: 24rpx;
}

.info-item {
  display: flex;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }
}

.info-label {
  font-size: 26rpx;
  color: #999;
  min-width: 180rpx;
}

.info-value {
  font-size: 26rpx;
  color: #333;
  flex: 1;

  &.temp {
    color: #ff4d4f;
    font-weight: 500;
  }
}

.stats-grid {
  display: flex;
  background-color: #fff;
  margin: 0 24rpx;
  border-radius: 16rpx;
  padding: 30rpx 0;
}

.stat-item {
  flex: 1;
  text-align: center;
  border-right: 1rpx solid #f0f0f0;

  &:last-child {
    border-right: none;
  }

  &.defect .stat-value {
    color: #f5222d;
  }

  &.confirmed .stat-value {
    color: #52c41a;
  }
}

.stat-value {
  display: block;
  font-size: 44rpx;
  font-weight: bold;
  color: #1890ff;
  margin-bottom: 8rpx;
}

.stat-label {
  font-size: 24rpx;
  color: #999;
}

.remark-text {
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
}

.image-section {
  background-color: #fff;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.defect-image-large {
  width: 100%;
  border-radius: 12rpx;
  background-color: #f5f5f5;
}

.image-meta {
  text-align: center;
  margin-top: 16rpx;
}

.meta-text {
  font-size: 24rpx;
  color: #999;
}

.order-card {
  display: flex;
  align-items: center;
}

.order-info {
  flex: 1;
}

.order-no {
  display: block;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 8rpx;
}

.order-status {
  font-size: 24rpx;
  color: #fa8c16;
}

.order-arrow {
  font-size: 40rpx;
  color: #ccc;
}

.bottom-actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 24rpx;
  padding: 24rpx 30rpx;
  background-color: #fff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.06);
  z-index: 100;
}

.btn-secondary,
.btn-primary {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
  border-radius: 44rpx;
  border: none;
  margin: 0;
}

.btn-secondary {
  background-color: #e6f7ff;
  color: #1890ff;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.bottom-space {
  height: 40rpx;
}
</style>
