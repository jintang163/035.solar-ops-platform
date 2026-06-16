<template>
  <view class="execute-page">
    <view class="progress-bar">
      <view class="progress-info">
        <text class="progress-text">巡检进度</text>
        <text class="progress-count">{{ completedCount }}/{{ items.length }}项</text>
      </view>
      <view class="progress-track">
        <view class="progress-fill" :style="{ width: progressPercent + '%' }"></view>
      </view>
    </view>

    <view class="item-list">
      <view 
        v-for="(item, index) in items" 
        :key="item.task_item_id || item.id"
        class="item-card"
        :class="{ active: activeIndex === index }"
        @click="setActive(index)"
      >
        <view class="item-header">
          <view class="item-index">{{ index + 1 }}</view>
          <view class="item-title">
            <text class="item-name">{{ item.item_name }}</text>
            <text class="item-asset" v-if="item.asset_name">{{ item.asset_name }}</text>
          </view>
          <view class="item-status" v-if="getItemResult(item)">
            <text class="status-icon">
              {{ getItemResult(item).isNormal === 1 ? '✅' : '⚠️' }}
            </text>
          </view>
        </view>

        <view class="item-detail" v-if="activeIndex === index">
          <view class="item-desc" v-if="item.description">
            <text class="desc-label">检查说明：</text>
            <text class="desc-text">{{ item.description }}</text>
          </view>

          <view class="item-standard" v-if="item.standard_value">
            <text class="standard-label">标准值：</text>
            <text class="standard-value">{{ item.standard_value }}{{ item.unit || '' }}</text>
          </view>

          <view class="input-section">
            <view class="input-title">检查结果</view>
            
            <view class="check-options" v-if="item.item_type === 1 || item.item_type === 5">
              <view 
                class="check-option" 
                :class="{ active: getItemResult(item).isNormal === 1 }"
                @click="setItemNormal(item, 1)"
              >
                <text class="option-icon">✅</text>
                <text class="option-text">正常</text>
              </view>
              <view 
                class="check-option abnormal" 
                :class="{ active: getItemResult(item).isNormal === 0 }"
                @click="setItemNormal(item, 0)"
              >
                <text class="option-icon">⚠️</text>
                <text class="option-text">异常</text>
              </view>
            </view>

            <view class="value-input" v-if="item.item_type === 2 || item.item_type === 4">
              <input 
                class="value-field" 
                type="digit"
                placeholder="请输入数值"
                :value="getItemValue(item)"
                @input="onValueInput(item, $event)"
                @blur="checkItemValue(item)"
              />
              <text class="value-unit">{{ item.unit || '' }}</text>
            </view>

            <view class="abnormal-input" v-if="item.item_type === 3">
              <view class="voice-section">
                <view class="voice-btn" :class="{ recording: isRecording }" @click="toggleRecording(item)">
                  <text class="voice-icon">{{ isRecording ? '⏹' : '🎙️' }}</text>
                  <text class="voice-text">
                    {{ isRecording ? '停止录音 (' + formatDuration(recordingDuration) + ')' : '点击录音' }}
                  </text>
                </view>
                <view class="voice-player" v-if="getItemAudio(item)">
                  <text class="audio-duration">
                    录音时长: {{ formatDuration(getItemAudio(item).duration || 0) }}s
                  </text>
                  <text class="play-btn" @click="playAudio(item)">▶播放</text>
                </view>
              </view>
              
              <view class="sound-options">
                <text class="sound-label">声音状况：</text>
                <view class="sound-btns">
                  <text 
                    class="sound-btn" 
                    :class="{ active: getItemResult(item).isNormal === 1 }"
                    @click="setItemNormal(item, 1)"
                  >正常</text>
                  <text 
                    class="sound-btn abnormal" 
                    :class="{ active: getItemResult(item).isNormal === 0 }"
                    @click="setItemNormal(item, 0)"
                  >异常</text>
                </view>
              </view>
            </view>

            <view class="abnormal-desc" v-if="getItemResult(item).isNormal === 0">
              <textarea 
                class="desc-textarea"
                placeholder="请描述异常情况"
                v-model="getItemResult(item).abnormalDesc"
                maxlength="500"
              />
            </view>
          </view>

          <view class="photo-section">
            <view class="section-title-row">
              <text class="section-title">照片</text>
              <text class="photo-count">{{ getItemPhotos(item).length }}张</text>
            </view>
            
            <view class="photo-grid">
              <view 
                v-for="(photo, pIndex) in getItemPhotos(item)" 
                :key="pIndex"
                class="photo-item"
              >
                <image :src="photo.photoPath" mode="aspectFill" class="photo-img" @click="previewPhoto(item, pIndex)"/>
                <view class="photo-delete" @click="deletePhoto(item, pIndex)">
                  <text>×</text>
                </view>
                <view class="photo-type" v-if="photo.photoType === 2">红外</view>
              </view>
              
              <view class="photo-add" @click="takePhoto(item)">
                <text class="add-icon">📷</text>
                <text class="add-text">拍照</text>
              </view>
              
              <view class="photo-add infrared" @click="takeInfraredPhoto(item)">
                <text class="add-icon">🌡️</text>
                <text class="add-text">红外</text>
              </view>
            </view>
          </view>

          <view class="remark-section">
            <text class="remark-label">备注</text>
            <textarea 
              class="remark-textarea"
              placeholder="添加备注信息（选填）"
              v-model="getItemResult(item).remark"
              maxlength="200"
            />
          </view>

          <view class="item-actions">
            <view class="action-btn prev" v-if="index > 0" @click="prevItem">上一项</view>
            <view class="action-btn next" v-if="index < items.length - 1" @click="nextItem">下一项</view>
            <view class="action-btn submit" v-if="index === items.length - 1" @click="handleSubmit">
              提交巡检
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="bottom-summary" v-if="isSubmitting">
      <view class="summary-card">
        <view class="summary-title">巡检完成</view>
        <view class="summary-stats">
          <view class="stat-item">
            <text class="stat-value">{{ items.length }}</text>
            <text class="stat-label">总检查项</text>
          </view>
          <view class="stat-item normal">
            <text class="stat-value">{{ normalCount }}</text>
            <text class="stat-label">正常项</text>
          </view>
          <view class="stat-item abnormal">
            <text class="stat-value">{{ abnormalCount }}</text>
            <text class="stat-label">异常项</text>
          </view>
        </view>
        <view class="summary-actions">
          <view class="summary-btn cancel" @click="cancelSubmit">取消</view>
          <view class="summary-btn confirm" @click="confirmSubmit">确认提交</view>
        </view>
      </view>
    </view>

    <view class="floating-submit" v-if="showFloatingSubmit" @click="handleSubmit">
      <text class="submit-icon">📋</text>
      <text class="submit-text">提交巡检</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { onLoad, onBackPress } from '@dcloudio/uni-app'
import inspectionDB from '@/utils/inspection-db.js'
import syncService from '@/utils/sync-service.js'
import photoUtil from '@/utils/photo-util.js'
import audioUtil from '@/utils/audio-util.js'

const taskId = ref(null)
const task = ref(null)
const items = ref([])
const results = ref({})
const photos = ref({})
const audios = ref({})
const activeIndex = ref(0)
const isRecording = ref(false)
const recordingDuration = ref(0)
const recordingItem = ref(null)
const isSubmitting = ref(false)
const startTime = ref(null)
let recordTimer = null

const completedCount = computed(() => {
  return items.value.filter(item => {
    const result = results.value[item.task_item_id || item.id]
    return result && result.isNormal !== undefined && result.isNormal !== null
  }).length
})

const progressPercent = computed(() => {
  if (items.value.length === 0) return 0
  return (completedCount.value / items.value.length) * 100
})

const normalCount = computed(() => {
  return Object.values(results.value).filter(r => r.isNormal === 1).length
})

const abnormalCount = computed(() => {
  return Object.values(results.value).filter(r => r.isNormal === 0).length
})

const showFloatingSubmit = computed(() => {
  return completedCount.value > 0 && !isSubmitting.value
})

function getItemResult(item) {
  const key = item.task_item_id || item.id
  if (!results.value[key]) {
    results.value[key] = {
      isNormal: null,
      checkValue: '',
      abnormalDesc: '',
      remark: ''
    }
  }
  return results.value[key]
}

function getItemPhotos(item) {
  const key = item.task_item_id || item.id
  if (!photos.value[key]) {
    photos.value[key] = []
  }
  return photos.value[key]
}

function getItemAudio(item) {
  const key = item.task_item_id || item.id
  return audios.value[key] || null
}

function setActive(index) {
  activeIndex.value = index
}

function setItemNormal(item, isNormal) {
  const result = getItemResult(item)
  result.isNormal = isNormal
}

function getItemValue(item) {
  const result = getItemResult(item)
  return result.checkValue || ''
}

function onValueInput(item, e) {
  const result = getItemResult(item)
  result.checkValue = e.detail.value
}

function checkItemValue(item) {
  const result = getItemResult(item)
  const value = parseFloat(result.checkValue)
  
  if (isNaN(value)) {
    result.isNormal = null
    return
  }

  const minVal = item.min_value !== null && item.min_value !== undefined ? parseFloat(item.min_value) : null
  const maxVal = item.max_value !== null && item.max_value !== undefined ? parseFloat(item.max_value) : null

  if (minVal !== null && !isNaN(minVal) && value < minVal) {
    result.isNormal = 0
  } else if (maxVal !== null && !isNaN(maxVal) && value > maxVal) {
    result.isNormal = 0
  } else {
    result.isNormal = 1
  }
}

async function takePhoto(item) {
  try {
    const photo = await photoUtil.takeInspectionPhoto({
      photoType: 1,
      addWatermark: true,
      compress: true
    })
    
    if (photo) {
      const itemPhotos = getItemPhotos(item)
      itemPhotos.push(photo)
    }
  } catch (err) {
    console.error('拍照失败:', err)
    uni.showToast({ title: '拍照失败', icon: 'none' })
  }
}

async function takeInfraredPhoto(item) {
  try {
    const photo = await photoUtil.takeInspectionPhoto({
      photoType: 2,
      addWatermark: true,
      compress: true
    })
    
    if (photo) {
      const itemPhotos = getItemPhotos(item)
      itemPhotos.push(photo)
    }
  } catch (err) {
    console.error('红外拍照失败:', err)
    uni.showToast({ title: '拍照失败', icon: 'none' })
  }
}

function previewPhoto(item, index) {
  const itemPhotos = getItemPhotos(item)
  const urls = itemPhotos.map(p => p.photoPath)
  uni.previewImage({
    urls,
    current: index
  })
}

function deletePhoto(item, index) {
  uni.showModal({
    title: '删除照片',
    content: '确定要删除这张照片吗？',
    success: (res) => {
      if (res.confirm) {
        const itemPhotos = getItemPhotos(item)
        itemPhotos.splice(index, 1)
      }
    }
  })
}

async function toggleRecording(item) {
  if (isRecording.value) {
    await stopRecording()
  } else {
    await startRecording(item)
  }
}

async function startRecording(item) {
  try {
    recordingItem.value = item
    recordingDuration.value = 0
    isRecording.value = true
    
    recordTimer = setInterval(() => {
      recordingDuration.value++
    }, 1000)

    audioUtil.recorder.start()
    
    uni.showToast({ title: '开始录音', icon: 'none' })
  } catch (err) {
    console.error('开始录音失败:', err)
    isRecording.value = false
    uni.showToast({ title: '录音失败', icon: 'none' })
  }
}

async function stopRecording() {
  try {
    if (recordTimer) {
      clearInterval(recordTimer)
      recordTimer = null
    }

    const result = await audioUtil.recorder.stop()
    
    if (result && recordingItem.value) {
      const key = recordingItem.value.task_item_id || recordingItem.value.id
      const location = await photoUtil.getLocation()
      
      audios.value[key] = {
        audioPath: result.path,
        duration: result.duration || recordingDuration.value,
        recordTime: new Date(),
        longitude: location ? location.longitude : null,
        latitude: location ? location.latitude : null
      }
    }
    
    isRecording.value = false
    recordingItem.value = null
    recordingDuration.value = 0
    
    uni.showToast({ title: '录音完成', icon: 'success' })
  } catch (err) {
    console.error('停止录音失败:', err)
    isRecording.value = false
    uni.showToast({ title: '录音失败', icon: 'none' })
  }
}

function playAudio(item) {
  const audio = getItemAudio(item)
  if (audio && audio.audioPath) {
    audioUtil.recorder.play(audio.audioPath)
  }
}

function formatDuration(seconds) {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

function prevItem() {
  if (activeIndex.value > 0) {
    activeIndex.value--
  }
}

function nextItem() {
  if (activeIndex.value < items.value.length - 1) {
    activeIndex.value++
  }
}

function handleSubmit() {
  if (completedCount.value < items.value.length) {
    uni.showModal({
      title: '提示',
      content: `还有 ${items.value.length - completedCount.value} 项未检查，确定提交吗？`,
      success: (res) => {
        if (res.confirm) {
          isSubmitting.value = true
        }
      }
    })
  } else {
    isSubmitting.value = true
  }
}

function cancelSubmit() {
  isSubmitting.value = false
}

async function confirmSubmit() {
  try {
    uni.showLoading({ title: '保存中...' })
    
    const userInfo = uni.getStorageSync('userInfo') || {}
    
    const resultData = {
      taskId: taskId.value,
      taskNo: task.value ? task.value.task_no : '',
      stationId: task.value ? task.value.station_id : null,
      stationName: task.value ? task.value.station_name : '',
      inspectorId: userInfo.id || userInfo.userId,
      inspectorName: userInfo.nickname || userInfo.userName,
      startTime: startTime.value,
      endTime: new Date().toISOString(),
      totalItems: items.value.length,
      normalItems: normalCount.value,
      abnormalItems: abnormalCount.value,
      resultStatus: abnormalCount.value > 0 ? 2 : 1,
      overallRemark: '',
      items: [],
      photos: [],
      audios: []
    }

    const location = await photoUtil.getLocation()
    if (location) {
      resultData.longitude = location.longitude
      resultData.latitude = location.latitude
    }

    items.value.forEach(item => {
      const key = item.task_item_id || item.id
      const result = results.value[key]
      const itemPhotos = photos.value[key] || []
      const itemAudio = audios.value[key]

      if (result && result.isNormal !== null && result.isNormal !== undefined) {
        resultData.items.push({
          taskItemId: item.task_item_id,
          itemId: item.item_id,
          itemName: item.item_name,
          itemType: item.item_type,
          assetId: item.asset_id,
          assetName: item.asset_name,
          assetCode: item.asset_code,
          checkValue: result.checkValue || '',
          standardValue: item.standard_value,
          isNormal: result.isNormal,
          abnormalDesc: result.abnormalDesc || '',
          remark: result.remark || '',
          checkTime: new Date().toISOString(),
          longitude: location ? location.longitude : null,
          latitude: location ? location.latitude : null
        })

        itemPhotos.forEach(photo => {
          resultData.photos.push({
            resultItemId: null,
            taskId: taskId.value,
            assetId: item.asset_id,
            photoType: photo.photoType || 1,
            photoUrl: photo.photoPath,
            thumbnailUrl: photo.compressedPath,
            fileSize: photo.fileSize,
            watermarkTime: photo.watermarkTime,
            longitude: photo.longitude,
            latitude: photo.latitude,
            hasWatermark: photo.hasWatermark,
            remark: '',
            isOffline: 1
          })
        })

        if (itemAudio) {
          resultData.audios.push({
            resultItemId: null,
            taskId: taskId.value,
            assetId: item.asset_id,
            audioUrl: itemAudio.audioPath,
            fileSize: itemAudio.fileSize,
            duration: itemAudio.duration,
            recordTime: itemAudio.recordTime,
            longitude: itemAudio.longitude,
            latitude: itemAudio.latitude,
            remark: '',
            isOffline: 1
          })
        }
      }
    })

    const resultId = await inspectionDB.saveResult(resultData)
    await inspectionDB.updateTaskStatus(taskId.value, 3)

    uni.hideLoading()
    
    if (syncService.isOnline()) {
      uni.showModal({
        title: '提交成功',
        content: '是否立即上传到服务器？',
        confirmText: '立即上传',
        cancelText: '稍后上传',
        success: async (res) => {
          if (res.confirm) {
            try {
              await syncService.trySync()
              uni.showToast({ title: '上传成功', icon: 'success' })
            } catch (err) {
              uni.showToast({ title: '上传失败，将在网络恢复后自动上传', icon: 'none' })
            }
          } else {
            uni.showToast({ title: '已保存，网络恢复后自动上传', icon: 'success' })
          }
          
          setTimeout(() => {
            uni.navigateBack()
          }, 1500)
        }
      })
    } else {
      uni.showToast({ title: '已保存，网络恢复后自动上传', icon: 'success' })
      setTimeout(() => {
        uni.navigateBack()
      }, 1500)
    }
  } catch (err) {
    uni.hideLoading()
    console.error('提交失败:', err)
    uni.showToast({ title: '提交失败，请重试', icon: 'none' })
  }
}

async function fetchTaskDetail() {
  if (!taskId.value) return
  
  try {
    const detail = await inspectionDB.getTaskDetail(taskId.value)
    if (detail) {
      task.value = detail
      items.value = detail.items || []
      
      if (detail.status !== 2) {
        await inspectionDB.updateTaskStatus(taskId.value, 2)
      }
      
      startTime.value = detail.actual_start_time || new Date().toISOString()
    }
  } catch (err) {
    console.error('获取任务详情失败:', err)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

onLoad((options) => {
  taskId.value = options.taskId ? parseInt(options.taskId) : null
  fetchTaskDetail()
})

onUnmounted(() => {
  if (isRecording.value) {
    stopRecording()
  }
  if (recordTimer) {
    clearInterval(recordTimer)
  }
})

onBackPress(() => {
  if (isSubmitting.value) {
    isSubmitting.value = false
    return true
  }
  
  if (completedCount.value > 0) {
    uni.showModal({
      title: '提示',
      content: '巡检未完成，确定退出吗？',
      success: (res) => {
        if (res.confirm) {
          uni.navigateBack()
        }
      }
    })
    return true
  }
  
  return false
})
</script>

<style lang="scss" scoped>
.execute-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 120rpx;
}

.progress-bar {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #fff;
  padding: 24rpx 30rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.progress-text {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

.progress-count {
  font-size: 26rpx;
  color: #1890ff;
}

.progress-track {
  height: 12rpx;
  background-color: #f0f0f0;
  border-radius: 6rpx;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #1890ff, #52c41a);
  border-radius: 6rpx;
  transition: width 0.3s ease;
}

.item-list {
  padding: 20rpx 30rpx;
}

.item-card {
  background-color: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
  
  &.active {
    box-shadow: 0 4rpx 20rpx rgba(24, 144, 255, 0.2);
  }
}

.item-header {
  display: flex;
  align-items: center;
  padding: 28rpx;
  background-color: #fff;
}

.item-index {
  width: 48rpx;
  height: 48rpx;
  line-height: 48rpx;
  text-align: center;
  background-color: #1890ff;
  color: #fff;
  font-size: 24rpx;
  border-radius: 50%;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.item-title {
  flex: 1;
}

.item-name {
  display: block;
  font-size: 30rpx;
  font-weight: 500;
  color: #333;
}

.item-asset {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-top: 6rpx;
}

.item-status {
  flex-shrink: 0;
}

.status-icon {
  font-size: 36rpx;
}

.item-detail {
  padding: 0 28rpx 28rpx;
  border-top: 1rpx solid #f0f0f0;
}

.item-desc {
  padding: 20rpx 0;
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
}

.desc-label {
  color: #999;
}

.desc-text {
  color: #666;
}

.item-standard {
  padding: 16rpx 20rpx;
  background-color: #f6ffed;
  border-radius: 8rpx;
  margin-bottom: 20rpx;
}

.standard-label {
  font-size: 26rpx;
  color: #999;
}

.standard-value {
  font-size: 26rpx;
  color: #52c41a;
  font-weight: 500;
}

.input-section {
  margin-bottom: 24rpx;
}

.input-title {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
  margin-bottom: 16rpx;
}

.check-options {
  display: flex;
  gap: 20rpx;
}

.check-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30rpx 0;
  background-color: #f9f9f9;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
  
  &.active {
    background-color: #f6ffed;
    border-color: #52c41a;
  }
  
  &.abnormal.active {
    background-color: #fff1f0;
    border-color: #ff4d4f;
  }
}

.option-icon {
  font-size: 48rpx;
  margin-bottom: 12rpx;
}

.option-text {
  font-size: 28rpx;
  color: #666;
}

.value-input {
  display: flex;
  align-items: center;
  background-color: #f9f9f9;
  border-radius: 12rpx;
  padding: 20rpx;
}

.value-field {
  flex: 1;
  font-size: 32rpx;
  color: #333;
}

.value-unit {
  font-size: 28rpx;
  color: #999;
  margin-left: 16rpx;
}

.voice-section {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.voice-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30rpx 0;
  background-color: #f9f9f9;
  border-radius: 12rpx;
  
  &.recording {
    background-color: #fff1f0;
    
    .voice-icon {
      animation: pulse 1s ease-in-out infinite;
    }
  }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

.voice-icon {
  font-size: 48rpx;
  margin-bottom: 12rpx;
}

.voice-text {
  font-size: 26rpx;
  color: #666;
}

.voice-player {
  flex: 1;
  text-align: center;
  padding: 20rpx;
  background-color: #e6f7ff;
  border-radius: 12rpx;
}

.audio-duration {
  display: block;
  font-size: 24rpx;
  color: #1890ff;
  margin-bottom: 8rpx;
}

.play-btn {
  font-size: 26rpx;
  color: #1890ff;
}

.sound-options {
  display: flex;
  align-items: center;
  margin-top: 20rpx;
}

.sound-label {
  font-size: 26rpx;
  color: #666;
  margin-right: 16rpx;
}

.sound-btns {
  display: flex;
  gap: 16rpx;
}

.sound-btn {
  padding: 10rpx 24rpx;
  font-size: 24rpx;
  background-color: #f0f0f0;
  color: #666;
  border-radius: 20rpx;
  
  &.active {
    background-color: #52c41a;
    color: #fff;
  }
  
  &.abnormal.active {
    background-color: #ff4d4f;
  }
}

.abnormal-desc {
  margin-top: 20rpx;
}

.desc-textarea {
  width: 100%;
  height: 160rpx;
  padding: 20rpx;
  font-size: 26rpx;
  background-color: #fff7e6;
  border-radius: 12rpx;
  color: #333;
  box-sizing: border-box;
}

.photo-section {
  margin-bottom: 24rpx;
}

.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
}

.photo-count {
  font-size: 24rpx;
  color: #999;
}

.photo-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.photo-item {
  position: relative;
  width: 200rpx;
  height: 200rpx;
  border-radius: 12rpx;
  overflow: hidden;
}

.photo-img {
  width: 100%;
  height: 100%;
}

.photo-delete {
  position: absolute;
  top: 8rpx;
  right: 8rpx;
  width: 40rpx;
  height: 40rpx;
  line-height: 36rpx;
  text-align: center;
  background-color: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 28rpx;
  border-radius: 50%;
}

.photo-type {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 20rpx;
  color: #fff;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.6));
  padding: 8rpx 0;
}

.photo-add {
  width: 200rpx;
  height: 200rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #f9f9f9;
  border: 2rpx dashed #ddd;
  border-radius: 12rpx;
  
  &.infrared {
    background-color: #fff7e6;
    border-color: #fa8c16;
  }
}

.add-icon {
  font-size: 40rpx;
  margin-bottom: 8rpx;
}

.add-text {
  font-size: 24rpx;
  color: #999;
}

.remark-section {
  margin-bottom: 24rpx;
}

.remark-label {
  display: block;
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
  margin-bottom: 12rpx;
}

.remark-textarea {
  width: 100%;
  height: 120rpx;
  padding: 20rpx;
  font-size: 26rpx;
  background-color: #f9f9f9;
  border-radius: 12rpx;
  color: #333;
  box-sizing: border-box;
}

.item-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 24rpx;
}

.action-btn {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  font-size: 28rpx;
  border-radius: 40rpx;
  background-color: #f0f0f0;
  color: #666;
  
  &.next {
    background-color: #1890ff;
    color: #fff;
  }
  
  &.submit {
    background-color: #52c41a;
    color: #fff;
  }
}

.bottom-summary {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 100;
  display: flex;
  align-items: flex-end;
}

.summary-card {
  width: 100%;
  background-color: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding: 40rpx 30rpx;
  padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
}

.summary-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #333;
  text-align: center;
  margin-bottom: 30rpx;
}

.summary-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 40rpx;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 48rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 8rpx;
  
  &.normal {
    color: #52c41a;
  }
  
  &.abnormal {
    color: #ff4d4f;
  }
}

.stat-label {
  font-size: 26rpx;
  color: #999;
}

.summary-actions {
  display: flex;
  gap: 20rpx;
}

.summary-btn {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  font-size: 30rpx;
  border-radius: 50rpx;
  
  &.cancel {
    background-color: #f0f0f0;
    color: #666;
  }
  
  &.confirm {
    background-color: #1890ff;
    color: #fff;
  }
}

.floating-submit {
  position: fixed;
  right: 30rpx;
  bottom: 120rpx;
  display: flex;
  align-items: center;
  padding: 20rpx 30rpx;
  background-color: #1890ff;
  color: #fff;
  border-radius: 50rpx;
  box-shadow: 0 4rpx 20rpx rgba(24, 144, 255, 0.4);
  z-index: 50;
}

.submit-icon {
  font-size: 32rpx;
  margin-right: 12rpx;
}

.submit-text {
  font-size: 28rpx;
  font-weight: 500;
}
</style>
