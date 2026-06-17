<template>
  <view class="voice-broadcast-page">
    <view class="filter-bar">
      <view class="filter-item" @click="openFilter">
        <text class="filter-icon">🔍</text>
        <text class="filter-text">筛选</text>
        <text v-if="hasFilter" class="filter-dot"></text>
      </view>
      <view class="filter-summary" v-if="hasFilter">
        <text class="summary-text">已筛选 {{ filteredCount }} 条</text>
        <text class="clear-filter" @click="clearFilter">清除</text>
      </view>
    </view>

    <view class="stats-bar">
      <view class="stat-item">
        <text class="stat-value blue">{{ todayCount }}</text>
        <text class="stat-label">今日播报</text>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item">
        <text class="stat-value orange">{{ highLevelCount }}</text>
        <text class="stat-label">高级及以上</text>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item">
        <text class="stat-value red">{{ urgentCount }}</text>
        <text class="stat-label">紧急告警</text>
      </view>
    </view>

    <scroll-view
      scroll-y
      class="broadcast-list"
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="onLoadMore"
    >
      <view v-for="item in list" :key="item.id" class="broadcast-card" @click="playBroadcast(item)">
        <view class="card-header">
          <view class="card-tags">
            <text class="type-tag" :class="'type-' + item.broadcastType">
              {{ getTypeIcon(item.broadcastType) }} {{ getTypeText(item.broadcastType) }}
            </text>
            <text class="level-tag" :class="'level-' + item.alarmLevel">
              {{ getLevelText(item.alarmLevel) }}
            </text>
          </view>
          <text class="status-tag" :class="'status-' + item.status">
            {{ getStatusText(item.status) }}
          </text>
        </view>
        <view class="card-content">
          <view class="description-block" v-if="item.description">
            <text class="description-label">告警描述</text>
            <text class="description-text">{{ item.description }}</text>
          </view>
          <view class="broadcast-block">
            <text class="broadcast-label">播报正文</text>
            <text class="content-text">{{ item.broadcastContent || '暂无内容' }}</text>
          </view>
        </view>
        <view class="card-push-status" v-if="item.successSpeakerCount != null || item.failSpeakerCount != null">
          <text class="push-status-label">推送状态</text>
          <view class="push-tags">
            <text class="push-tag success">成功 {{ item.successSpeakerCount || 0 }}</text>
            <text class="push-tag fail">失败 {{ item.failSpeakerCount || 0 }}</text>
          </view>
        </view>
        <view class="card-meta">
          <text class="meta-item">🏢 {{ item.stationName || (item.stationId ? '电站#' + item.stationId : '-') }}</text>
          <text class="meta-item" v-if="item.inverterId">⚡ {{ item.inverterName || '逆变器#' + item.inverterId }}</text>
        </view>
        <view class="card-footer">
          <text class="time-text">{{ formatTime(item.createTime) }}</text>
          <view class="action-btn" @click.stop="playBroadcast(item)">
            <text class="action-icon">▶</text>
            <text class="action-text">播放</text>
          </view>
        </view>
      </view>

      <view v-if="list.length === 0 && !loading" class="empty-tip">
        <text class="empty-icon">🔔</text>
        <text class="empty-text">暂无播报记录</text>
      </view>

      <view v-if="loading" class="loading-tip">
        <text>加载中...</text>
      </view>
      <view v-if="!hasMore && list.length > 0" class="no-more-tip">
        <text>没有更多了</text>
      </view>
    </scroll-view>

    <view class="filter-modal" v-if="filterVisible" @click="closeFilter">
      <view class="filter-content" @click.stop>
        <view class="filter-header">
          <text class="filter-title">筛选条件</text>
          <text class="filter-close" @click="closeFilter">✕</text>
        </view>
        <view class="filter-body">
          <view class="filter-group">
            <text class="group-label">播报类型</text>
            <view class="tag-list">
              <text
                v-for="(type, key) in typeOptions"
                :key="key"
                class="option-tag"
                :class="{ active: filterParams.broadcastType === Number(key) }"
                @click="toggleFilter('broadcastType', Number(key))"
              >
                {{ type.icon }} {{ type.text }}
              </text>
            </view>
          </view>
          <view class="filter-group">
            <text class="group-label">告警级别</text>
            <view class="tag-list">
              <text
                v-for="(level, key) in levelOptions"
                :key="key"
                class="option-tag"
                :class="{ active: filterParams.alarmLevel === Number(key) }"
                @click="toggleFilter('alarmLevel', Number(key))"
              >
                {{ level.text }}
              </text>
            </view>
          </view>
        </view>
        <view class="filter-footer">
          <view class="footer-btn reset-btn" @click="resetFilter">
            <text>重置</text>
          </view>
          <view class="footer-btn confirm-btn" @click="applyFilter">
            <text>确定</text>
          </view>
        </view>
      </view>
    </view>

    <view class="detail-modal" v-if="detailVisible" @click="closeDetail">
      <view class="detail-content" @click.stop>
        <view class="detail-header">
          <text class="detail-title">播报详情</text>
          <text class="detail-close" @click="closeDetail">✕</text>
        </view>
        <view class="detail-body" v-if="currentItem">
          <view class="detail-tags">
            <text class="type-tag large" :class="'type-' + currentItem.broadcastType">
              {{ getTypeIcon(currentItem.broadcastType) }} {{ getTypeText(currentItem.broadcastType) }}
            </text>
            <text class="level-tag large" :class="'level-' + currentItem.alarmLevel">
              {{ getLevelText(currentItem.alarmLevel) }}
            </text>
            <text class="status-tag large" :class="'status-' + currentItem.status">
              {{ getStatusText(currentItem.status) }}
            </text>
          </view>
          <view class="detail-section" v-if="currentItem.description">
            <text class="section-label">告警描述</text>
            <text class="section-content">{{ currentItem.description }}</text>
          </view>
          <view class="detail-section">
            <text class="section-label">播报正文</text>
            <text class="section-content">{{ currentItem.broadcastContent || '暂无内容' }}</text>
          </view>
          <view class="detail-section" v-if="currentItem.successSpeakerCount != null || currentItem.failSpeakerCount != null">
            <text class="section-label">推送状态</text>
            <view class="push-tags">
              <text class="push-tag success">成功 {{ currentItem.successSpeakerCount || 0 }}</text>
              <text class="push-tag fail">失败 {{ currentItem.failSpeakerCount || 0 }}</text>
            </view>
          </view>
          <view class="detail-section" v-if="currentItem.targetSpeakerIds">
            <text class="section-label">推送音箱</text>
            <text class="section-value">{{ currentItem.targetSpeakerIds }}</text>
          </view>
          <view class="detail-section">
            <text class="section-label">电站</text>
            <text class="section-value">{{ currentItem.stationName || (currentItem.stationId ? '电站#' + currentItem.stationId : '-') }}</text>
          </view>
          <view class="detail-section" v-if="currentItem.inverterId">
            <text class="section-label">逆变器</text>
            <text class="section-value">{{ currentItem.inverterName || '逆变器#' + currentItem.inverterId }}</text>
          </view>
          <view class="detail-section" v-if="currentItem.faultCode">
            <text class="section-label">故障码</text>
            <text class="section-value">{{ currentItem.faultCode }}</text>
          </view>
          <view class="detail-section">
            <text class="section-label">播报时间</text>
            <text class="section-value">{{ formatTime(currentItem.createTime) }}</text>
          </view>
        </view>
        <view class="detail-footer">
          <view class="footer-btn play-btn" @click="playBroadcast(currentItem)">
            <text class="btn-icon">▶</text>
            <text>播放语音</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { getBroadcastHistory, retryBroadcast } from '@/api/voiceBroadcast'

const list = ref([])
const loading = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)
const pageSize = 20
const hasMore = ref(true)

const filterVisible = ref(false)
const detailVisible = ref(false)
const currentItem = ref(null)
const filterParams = ref({
  broadcastType: null,
  alarmLevel: null
})
const appliedFilter = ref({
  broadcastType: null,
  alarmLevel: null
})

const innerAudioContext = ref(null)

const typeOptions = {
  1: { color: 'blue', text: '通讯中断', icon: '📡' },
  2: { color: 'red', text: '火灾预警', icon: '🔥' },
  3: { color: 'orange', text: '设备故障', icon: '⚠️' },
  4: { color: 'volcano', text: '紧急告警', icon: '🚨' },
  5: { color: 'purple', text: '数据异常', icon: '📊' }
}

const levelOptions = {
  1: { color: 'blue', text: '提示' },
  2: { color: 'cyan', text: '中级' },
  3: { color: 'orange', text: '高级' },
  4: { color: 'red', text: '紧急' }
}

const statusOptions = {
  0: { color: 'default', text: '待播报' },
  1: { color: 'green', text: '已播报' },
  2: { color: 'red', text: '播报失败' }
}

const todayCount = computed(() => {
  const today = new Date().toDateString()
  return list.value.filter(item => new Date(item.createTime).toDateString() === today).length
})

const highLevelCount = computed(() => {
  return list.value.filter(item => item.alarmLevel >= 3).length
})

const urgentCount = computed(() => {
  return list.value.filter(item => item.alarmLevel === 4).length
})

const hasFilter = computed(() => {
  return appliedFilter.value.broadcastType !== null || appliedFilter.value.alarmLevel !== null
})

const filteredCount = computed(() => {
  return list.value.length
})

onMounted(() => {
  initAudio()
  fetchData()
})

onPullDownRefresh(() => {
  onRefresh()
})

onReachBottom(() => {
  onLoadMore()
})

const initAudio = () => {
  try {
    innerAudioContext.value = uni.createInnerAudioContext()
  } catch (e) {
    console.error('创建音频上下文失败:', e)
  }
}

const getTypeText = (type) => {
  return typeOptions[type]?.text || '未知'
}

const getTypeIcon = (type) => {
  return typeOptions[type]?.icon || '🔔'
}

const getLevelText = (level) => {
  return levelOptions[level]?.text || '未知'
}

const getStatusText = (status) => {
  return statusOptions[status]?.text || '未知'
}

const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  const isYesterday = new Date(now.getTime() - 86400000).toDateString() === date.toDateString()

  const pad = (n) => n.toString().padStart(2, '0')

  if (isToday) {
    return `今天 ${pad(date.getHours())}:${pad(date.getMinutes())}`
  } else if (isYesterday) {
    return `昨天 ${pad(date.getHours())}:${pad(date.getMinutes())}`
  } else {
    return `${date.getMonth() + 1}/${date.getDate()} ${pad(date.getHours())}:${pad(date.getMinutes())}`
  }
}

const fetchData = async (reset = false) => {
  if (loading.value) return
  if (reset) {
    pageNum.value = 1
    hasMore.value = true
  }
  if (!hasMore.value) return

  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize,
      ...appliedFilter.value
    }
    Object.keys(params).forEach(key => {
      if (params[key] === null || params[key] === undefined) {
        delete params[key]
      }
    })
    const res = await getBroadcastHistory(params)
    const data = res?.list || res || []
    if (reset) {
      list.value = data
    } else {
      list.value = [...list.value, ...data]
    }
    if (data.length < pageSize) {
      hasMore.value = false
    } else {
      pageNum.value++
    }
  } catch (err) {
    console.error('获取播报记录失败:', err)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
    refreshing.value = false
    uni.stopPullDownRefresh()
  }
}

const onRefresh = () => {
  refreshing.value = true
  fetchData(true)
}

const onLoadMore = () => {
  if (!loading.value && hasMore.value) {
    fetchData()
  }
}

const openFilter = () => {
  filterParams.value = { ...appliedFilter.value }
  filterVisible.value = true
}

const closeFilter = () => {
  filterVisible.value = false
}

const toggleFilter = (key, value) => {
  if (filterParams.value[key] === value) {
    filterParams.value[key] = null
  } else {
    filterParams.value[key] = value
  }
}

const resetFilter = () => {
  filterParams.value = {
    broadcastType: null,
    alarmLevel: null
  }
}

const applyFilter = () => {
  appliedFilter.value = { ...filterParams.value }
  filterVisible.value = false
  fetchData(true)
}

const clearFilter = () => {
  appliedFilter.value = {
    broadcastType: null,
    alarmLevel: null
  }
  fetchData(true)
}

const closeDetail = () => {
  detailVisible.value = false
  currentItem.value = null
}

const playBroadcast = async (item) => {
  if (!item) return

  if (detailVisible.value) {
  } else {
    currentItem.value = item
    detailVisible.value = true
  }

  try {
    if (item.audioUrl && innerAudioContext.value) {
      innerAudioContext.value.src = item.audioUrl
      innerAudioContext.value.play()
    } else {
      const content = item.broadcastContent
      if (content) {
        try {
          if (uni.createInnerAudioContext) {
            const plugin = uni.requireNativePlugin('AISpeech-SDK')
            if (plugin && plugin.speak) {
              plugin.speak({ content })
            } else {
              speakByTTS(content)
            }
          } else {
            speakByTTS(content)
          }
        } catch (e) {
          speakByTTS(content)
        }
      }
    }
  } catch (err) {
    console.error('播放语音失败:', err)
    uni.showToast({ title: '播放失败', icon: 'none' })
  }
}

const speakByTTS = (text) => {
  try {
    if (typeof plus !== 'undefined' && plus.speech) {
      plus.speech.speak(text, {
        volume: 1,
        rate: 1
      }, () => {}, (e) => {
        console.error('TTS播放失败:', e)
      })
    } else if (typeof window !== 'undefined' && window.speechSynthesis) {
      const utterance = new SpeechSynthesisUtterance(text)
      utterance.lang = 'zh-CN'
      utterance.volume = 1
      utterance.rate = 1
      window.speechSynthesis.speak(utterance)
    } else {
      uni.showToast({ title: '设备不支持语音播放', icon: 'none' })
    }
  } catch (e) {
    console.error('TTS播放异常:', e)
  }
}
</script>

<style lang="scss" scoped>
.voice-broadcast-page {
  min-height: 100vh;
  background-color: #f5f6fa;
  display: flex;
  flex-direction: column;
}

.filter-bar {
  display: flex;
  align-items: center;
  padding: 24rpx 30rpx;
  background-color: #fff;
  border-bottom: 1rpx solid #f0f0f0;
}

.filter-item {
  display: flex;
  align-items: center;
  padding: 12rpx 24rpx;
  background-color: #f5f6fa;
  border-radius: 30rpx;
  position: relative;

  .filter-icon {
    font-size: 28rpx;
    margin-right: 8rpx;
  }

  .filter-text {
    font-size: 26rpx;
    color: #666;
  }

  .filter-dot {
    position: absolute;
    top: 8rpx;
    right: 8rpx;
    width: 16rpx;
    height: 16rpx;
    background-color: #ff4d4f;
    border-radius: 50%;
  }
}

.filter-summary {
  margin-left: 20rpx;
  display: flex;
  align-items: center;

  .summary-text {
    font-size: 24rpx;
    color: #999;
  }

  .clear-filter {
    margin-left: 16rpx;
    font-size: 24rpx;
    color: #1890ff;
  }
}

.stats-bar {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 30rpx;
  background-color: #fff;
  margin-bottom: 20rpx;
}

.stat-item {
  text-align: center;
  flex: 1;

  .stat-value {
    display: block;
    font-size: 44rpx;
    font-weight: 700;
    margin-bottom: 8rpx;

    &.blue { color: #1890ff; }
    &.orange { color: #fa8c16; }
    &.red { color: #ff4d4f; }
  }

  .stat-label {
    font-size: 24rpx;
    color: #999;
  }
}

.stat-divider {
  width: 1rpx;
  height: 60rpx;
  background-color: #f0f0f0;
}

.broadcast-list {
  flex: 1;
  padding: 0 30rpx 30rpx;
}

.broadcast-card {
  background-color: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.card-tags {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-wrap: wrap;
}

.type-tag {
  padding: 6rpx 16rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
  font-weight: 500;

  &.type-1 { background-color: #e6f7ff; color: #1890ff; }
  &.type-2 { background-color: #fff1f0; color: #ff4d4f; }
  &.type-3 { background-color: #fff7e6; color: #fa8c16; }
  &.type-4 { background-color: #fff2e8; color: #fa541c; }
  &.type-5 { background-color: #f9f0ff; color: #722ed1; }

  &.large {
    padding: 10rpx 20rpx;
    font-size: 26rpx;
  }
}

.level-tag {
  padding: 6rpx 16rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
  font-weight: 500;

  &.level-1 { background-color: #e6f7ff; color: #1890ff; }
  &.level-2 { background-color: #e6fffb; color: #13c2c2; }
  &.level-3 { background-color: #fff7e6; color: #fa8c16; }
  &.level-4 { background-color: #fff1f0; color: #ff4d4f; }

  &.large {
    padding: 10rpx 20rpx;
    font-size: 26rpx;
  }
}

.status-tag {
  padding: 6rpx 16rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
  font-weight: 500;

  &.status-0 { background-color: #f5f5f5; color: #8c8c8c; }
  &.status-1 { background-color: #f6ffed; color: #52c41a; }
  &.status-2 { background-color: #fff1f0; color: #ff4d4f; }

  &.large {
    padding: 10rpx 20rpx;
    font-size: 26rpx;
  }
}

.card-content {
  margin-bottom: 16rpx;

  .content-text {
    font-size: 28rpx;
    color: #262626;
    line-height: 1.5;
  }
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 16rpx;

  .meta-item {
    font-size: 24rpx;
    color: #8c8c8c;
    margin-right: 30rpx;
    margin-bottom: 8rpx;
  }
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16rpx;
  border-top: 1rpx solid #f5f5f5;

  .time-text {
    font-size: 24rpx;
    color: #bbb;
  }

  .action-btn {
    display: flex;
    align-items: center;
    padding: 8rpx 20rpx;
    background-color: #e6f7ff;
    border-radius: 30rpx;

    .action-icon {
      font-size: 22rpx;
      color: #1890ff;
      margin-right: 6rpx;
    }

    .action-text {
      font-size: 24rpx;
      color: #1890ff;
      font-weight: 500;
    }
  }
}

.empty-tip, .loading-tip, .no-more-tip {
  padding: 80rpx 0;
  text-align: center;

  .empty-icon {
    display: block;
    font-size: 80rpx;
    margin-bottom: 20rpx;
  }

  .empty-text, text {
    font-size: 28rpx;
    color: #999;
  }
}

.filter-modal, .detail-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
}

.filter-content, .detail-content {
  width: 100%;
  background-color: #fff;
  border-top-left-radius: 24rpx;
  border-top-right-radius: 24rpx;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.filter-header, .detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #f0f0f0;

  .filter-title, .detail-title {
    font-size: 32rpx;
    font-weight: 600;
    color: #262626;
  }

  .filter-close, .detail-close {
    font-size: 32rpx;
    color: #999;
    padding: 10rpx;
  }
}

.filter-body, .detail-body {
  flex: 1;
  padding: 30rpx;
  overflow-y: auto;
}

.filter-group {
  margin-bottom: 40rpx;

  &:last-child {
    margin-bottom: 0;
  }

  .group-label {
    display: block;
    font-size: 28rpx;
    color: #262626;
    font-weight: 500;
    margin-bottom: 20rpx;
  }
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.option-tag {
  padding: 14rpx 28rpx;
  background-color: #f5f6fa;
  border-radius: 8rpx;
  font-size: 26rpx;
  color: #666;
  border: 2rpx solid transparent;
  transition: all 0.2s;

  &.active {
    background-color: #e6f7ff;
    color: #1890ff;
    border-color: #1890ff;
  }
}

.filter-footer, .detail-footer {
  display: flex;
  padding: 24rpx 30rpx;
  gap: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.footer-btn {
  flex: 1;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 500;

  &.reset-btn {
    background-color: #f5f6fa;
    color: #666;
  }

  &.confirm-btn {
    background-color: #1890ff;
    color: #fff;
  }

  &.play-btn {
    background-color: #1890ff;
    color: #fff;

    .btn-icon {
      margin-right: 10rpx;
    }
  }
}

.detail-modal {
  align-items: center;
  padding: 40rpx;
}

.detail-content {
  border-radius: 24rpx;
  max-height: 70vh;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 30rpx;
}

.detail-section {
  margin-bottom: 28rpx;

  &:last-child {
    margin-bottom: 0;
  }

  .section-label {
    display: block;
    font-size: 24rpx;
    color: #999;
    margin-bottom: 10rpx;
  }

  .section-content {
    font-size: 30rpx;
    color: #262626;
    line-height: 1.6;
    background-color: #fafafa;
    padding: 20rpx;
    border-radius: 12rpx;
  }

  .section-value {
    font-size: 30rpx;
    color: #262626;
    font-weight: 500;
  }
}

.description-block, .broadcast-block {
  margin-bottom: 16rpx;

  &:last-child {
    margin-bottom: 0;
  }

  .description-label, .broadcast-label {
    display: block;
    font-size: 22rpx;
    color: #999;
    margin-bottom: 8rpx;
  }

  .description-text {
    font-size: 26rpx;
    color: #666;
    line-height: 1.5;
    background-color: #fafafa;
    padding: 12rpx 16rpx;
    border-radius: 8rpx;
  }
}

.broadcast-block {
  .content-text {
    font-size: 28rpx;
    color: #262626;
    line-height: 1.5;
    background-color: #e6f7ff;
    padding: 12rpx 16rpx;
    border-radius: 8rpx;
  }
}

.card-push-status {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
  padding: 12rpx 16rpx;
  background-color: #f9f9f9;
  border-radius: 8rpx;

  .push-status-label {
    font-size: 24rpx;
    color: #999;
    margin-right: 16rpx;
  }

  .push-tags {
    display: flex;
    gap: 12rpx;
  }

  .push-tag {
    padding: 4rpx 16rpx;
    border-radius: 6rpx;
    font-size: 22rpx;
    font-weight: 500;

    &.success {
      background-color: #f6ffed;
      color: #52c41a;
    }

    &.fail {
      background-color: #fff1f0;
      color: #ff4d4f;
    }
  }
}
</style>
