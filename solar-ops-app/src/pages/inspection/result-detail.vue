<template>
  <view class="result-detail-page">
    <view class="result-header" v-if="result">
      <view class="result-title">巡检结果</view>
      <view class="result-status" :class="getStatusClass(result.result_status)">
        {{ getStatusText(result.result_status) }}
      </view>
    </view>

    <view class="section" v-if="result">
      <view class="section-title">基本信息</view>
      <view class="info-list">
        <view class="info-item">
          <text class="info-label">任务名称</text>
          <text class="info-value">{{ task?.task_name || '--' }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">任务编号</text>
          <text class="info-value">{{ result.task_no }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">电站名称</text>
          <text class="info-value">{{ result.station_name }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">巡检人员</text>
          <text class="info-value">{{ result.inspector_name }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">开始时间</text>
          <text class="info-value">{{ formatDate(result.start_time) }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">结束时间</text>
          <text class="info-value">{{ formatDate(result.end_time) }}</text>
        </view>
      </view>
    </view>

    <view class="section stats-section" v-if="result">
      <view class="section-title">检查统计</view>
      <view class="stats-row">
        <view class="stat-item">
          <text class="stat-value total">{{ result.total_items || 0 }}</text>
          <text class="stat-label">总检查项</text>
        </view>
        <view class="stat-item">
          <text class="stat-value normal">{{ result.normal_items || 0 }}</text>
          <text class="stat-label">正常</text>
        </view>
        <view class="stat-item">
          <text class="stat-value abnormal">{{ result.abnormal_items || 0 }}</text>
          <text class="stat-label">异常</text>
        </view>
      </view>
      <view class="pass-rate">
        <text class="rate-label">通过率</text>
        <text class="rate-value">{{ passRate }}%</text>
      </view>
    </view>

    <view class="section">
      <view class="section-title">
        检查项明细
        <text class="item-count">({{ resultItems.length }}项)</text>
      </view>

      <view class="result-item" v-for="item in resultItems" :key="item.id" class="item-card">
        <view class="item-header">
          <view class="item-status-icon">{{ item.is_normal === 1 ? '✅' : '⚠️' }}</view>
          <view class="item-info">
            <text class="item-name">{{ item.item_name }}</text>
            <text class="item-asset" v-if="item.asset_name">{{ item.asset_name }}</text>
          </view>
          <text class="item-type">{{ getItemTypeText(item.item_type) }}</text>
        </view>
        
        <view class="item-detail">
          <view class="detail-row" v-if="item.check_value">
            <text class="detail-label">检查值：</text>
            <text class="detail-value" :class="{ abnormal: item.is_normal === 0 }">
              {{ item.check_value }}
            </text>
          </view>
          <view class="detail-row" v-if="item.standard_value">
            <text class="detail-label">标准值：</text>
            <text class="detail-value">{{ item.standard_value }}</text>
          </view>
          <view class="detail-row abnormal-desc" v-if="item.abnormal_desc">
            <text class="desc-label">异常描述：</text>
            <text class="desc-text">{{ item.abnormal_desc }}</text>
          </view>
          <view class="detail-row" v-if="item.remark">
            <text class="detail-label">备注：</text>
            <text class="detail-value">{{ item.remark }}</text>
          </view>
          
          <view class="item-photos" v-if="getItemPhotos(item).length > 0">
            <text class="photos-label">照片：</text>
            <view class="photo-list">
              <image 
                v-for="(photo, pIndex) in getItemPhotos(item)" 
                :key="pIndex"
                :src="photo.photo_url || photo.photoPath"
                class="photo-thumb"
                mode="aspectFill"
                @click="previewPhoto(item, pIndex)"
              />
            </view>
          </view>
          
          <view class="item-audio" v-if="getItemAudio(item)">
            <view class="audio-player" @click="playAudio(item)">
              <text class="audio-icon">🎵</text>
              <text class="audio-text">
                录音 ({{ formatDuration(getItemAudio(item).duration || 0) }})
              </text>
              <text class="play-btn">▶ 播放</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="section" v-if="result && result.overall_remark">
      <view class="section-title">总体评价</view>
      <view class="remark-box">
        <text>{{ result.overall_remark }}</text>
      </view>
    </view>

    <view class="sync-section" v-if="result && result.sync_status === 0">
      <view class="sync-card" @click="handleSync">
        <text class="sync-icon">📤</text>
        <view class="sync-info">
          <text class="sync-title">待上传</text>
          <text class="sync-desc">该结果尚未上传到服务器</text>
        </view>
        <text class="sync-btn">点击上传</text>
      </view>
    </view>

    <view class="bottom-space"></view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import inspectionDB from '@/utils/inspection-db.js'
import syncService from '@/utils/sync-service.js'
import audioUtil from '@/utils/audio-util.js'

const result = ref(null)
const resultItems = ref([])
const photos = ref([])
const audios = ref([])
const taskId = ref(null)

const passRate = computed(() => {
  if (!result.value || !result.value.total_items === 0) return 0
  const total = result.value.total_items || 0
  const normal = result.value.normal_items || 0
  return total > 0 ? ((normal / total * 100).toFixed(1) : 0
})

function getStatusText(status) {
  const map = {
    1: '正常',
    2: '异常',
    3: '待复核'
  }
  return map[status] || '未知'
}

function getStatusClass(status) {
  const map = {
    1: 'status-normal',
    2: 'status-abnormal',
    3: 'status-pending'
  }
  return map[status] || ''
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

function formatDuration(seconds) {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}分${secs}秒'
}

function getItemPhotos(item) {
  return photos.value.filter(p => p.result_item_id === item.id || p.resultItemId === item.id
}

function getItemAudio(item) {
  return audios.value.find(a => a.result_item_id === item.id || a.resultItemId === item.id
}

function previewPhoto(item, index) {
  const itemPhotos = getItemPhotos(item)
  const urls = itemPhotos.map(p => p.photo_url || p.photoPath)
  uni.previewImage({
    urls,
    current: index
  })
}

function playAudio(item) {
  const audio = getItemAudio(item)
  if (audio && (audio.audio_url || audio.audioPath)) {
    audioUtil.recorder.play(audio.audio_url || audio.audioPath)
  }
}

async function fetchResultDetail() {
  if (!taskId.value) return
  
  try {
    const results = await inspectionDB.query('inspection_result', 'task_id = ?', [taskId.value]
    if (results && results.length > 0) {
      result.value = results[0]
      
      const items = await inspectionDB.getResultItems(result.value.id)
      resultItems.value = items || []
      
      const photoList = await inspectionDB.getResultPhotos(result.value.id)
      photos.value = photoList || []
      
      const audioList = await inspectionDB.getResultAudios(result.value.id)
      audios.value = audioList || []
    }
  } catch (err) {
    console.error('获取结果详情失败:', err)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

async function handleSync() {
  if (!syncService.isOnline()) {
    uni.showToast({ title: '网络不可用', icon: 'none' })
    return
  }

  uni.showLoading({ title: '上传中...' })
  try {
    await syncService.trySync()
    uni.hideLoading()
    uni.showToast({ title: '上传成功', icon: 'success' })
    fetchResultDetail()
  } catch (err) {
    uni.hideLoading()
    console.error('上传失败:', err)
    uni.showToast({ title: '上传失败', icon: 'none' })
  }
}

onLoad((options) => {
  taskId.value = options.taskId ? parseInt(options.taskId) : null
  fetchResultDetail()
})
</script>

<style lang="scss" scoped>
.result-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.result-header {
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
  padding: 40rpx 30rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
}

.result-title {
  font-size: 36rpx;
  font-weight: 600;
}

.result-status {
  font-size: 24rpx;
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  background-color: rgba(255, 255, 255, 0.3);
  
  &.status-normal {
    background-color: #f6ffed;
    color: #52c41a;
  }
  
  &.status-abnormal {
    background-color: #fff1f0;
    color: #ff4d4f;
  }
  
  &.status-pending {
    background-color: #fff7e6;
    color: #fa8c16;
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

.item-count {
  font-size: 24rpx;
  color: #999;
  font-weight: normal;
  margin-left: 8rpx;
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

.stats-section {
  .stats-row {
    display: flex;
    justify-content: space-around;
    margin-bottom: 24rpx;
  }
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 44rpx;
  font-weight: 600;
  margin-bottom: 8rpx;
  
  &.total {
    color: #333;
  }
  
  &.normal {
    color: #52c41a;
  }
  
  &.abnormal {
    color: #ff4d4f;
  }
}

.stat-label {
  font-size: 24rpx;
  color: #999;
}

.pass-rate {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx;
  background-color: #f9f9f9;
  border-radius: 12rpx;
}

.rate-label {
  font-size: 26rpx;
  color: #666;
}

.rate-value {
  font-size: 32rpx;
  font-weight: 600;
  color: #1890ff;
}

.item-card {
  background-color: #f9f9f9;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.item-header {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.item-status-icon {
  font-size: 32rpx;
  margin-right: 16rpx;
}

.item-info {
  flex: 1;
}

.item-name {
  display: block;
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
}

.item-asset {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-top: 4rpx;
}

.item-type {
  font-size: 22rpx;
  color: #1890ff;
  background-color: #e6f7ff;
  padding: 6rpx 12rpx;
  border-radius: 6rpx;
}

.item-detail {
  padding-top: 16rpx;
  border-top: 1rpx solid #eee;
}

.detail-row {
  display: flex;
  margin-bottom: 12rpx;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.detail-label {
  flex-shrink: 0;
  font-size: 24rpx;
  color: #999;
  width: 120rpx;
}

.detail-value {
  flex: 1;
  font-size: 26rpx;
  color: #333;
  
  &.abnormal {
    color: #ff4d4f;
  }
}

.abnormal-desc {
  background-color: #fff1f0;
  padding: 16rpx;
  border-radius: 8rpx;
  margin-top: 12rpx;
}

.desc-label {
  display: block;
  font-size: 24rpx;
  color: #ff4d4f;
  margin-bottom: 8rpx;
}

.desc-text {
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
}

.item-photos {
  margin-top: 16rpx;
}

.photos-label {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-bottom: 12rpx;
}

.photo-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.photo-thumb {
  width: 160rpx;
  height: 160rpx;
  border-radius: 8rpx;
}

.item-audio {
  margin-top: 16rpx;
}

.audio-player {
  display: flex;
  align-items: center;
  padding: 20rpx;
  background-color: #e6f7ff;
  border-radius: 12rpx;
}

.audio-icon {
  font-size: 32rpx;
  margin-right: 16rpx;
}

.audio-text {
  flex: 1;
  font-size: 26rpx;
  color: #333;
}

.play-btn {
  font-size: 26rpx;
  color: #1890ff;
}

.remark-box {
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
  padding: 20rpx;
  background-color: #f9f9f9;
  border-radius: 12rpx;
}

.sync-section {
  padding: 20rpx 30rpx;
}

.sync-card {
  display: flex;
  align-items: center;
  background-color: #fff7e6;
  border-radius: 16rpx;
  padding: 28rpx;
}

.sync-icon {
  font-size: 40rpx;
  margin-right: 20rpx;
}

.sync-info {
  flex: 1;
}

.sync-title {
  display: block;
  font-size: 28rpx;
  font-weight: 500;
  color: #fa8c16;
}

.sync-desc {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-top: 6rpx;
}

.sync-btn {
  font-size: 26rpx;
  color: #fa8c16;
  background-color: #fff;
  padding: 12rpx 24rpx;
  border-radius: 30rpx;
}

.bottom-space {
  height: 40rpx;
}
</style>
