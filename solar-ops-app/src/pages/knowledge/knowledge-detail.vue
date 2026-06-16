<template>
  <view class="knowledge-detail-page" v-if="detail">
    <view class="detail-header">
      <view class="header-tags">
        <text class="fault-code" style="font-family: monospace">{{ detail.faultCode }}</text>
        <view class="level-tag" :style="{ background: getLevelColor(detail.faultLevel) + '20', color: getLevelColor(detail.faultLevel) }">
          {{ getLevelText(detail.faultLevel) }}
        </view>
        <view v-if="detail.faultType" class="type-tag">{{ detail.faultType }}</view>
        <view class="status-tag status-published">已发布</view>
      </view>
      <text class="detail-title">{{ detail.faultName }}</text>

      <view class="detail-tags" v-if="detail.tags">
        <text v-for="tag in parseTags(detail.tags)" :key="tag" class="tag-item">
          #{{ tag }}
        </text>
      </view>

      <view class="detail-meta">
        <view class="meta-item">
          <text class="meta-icon">👤</text>
          <text class="meta-text">{{ detail.creatorName || '系统' }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-icon">🕒</text>
          <text class="meta-text">{{ formatTime(detail.updateTime) }}</text>
        </view>
      </view>
    </view>

    <view class="feedback-bar">
      <view
        class="fb-btn"
        :class="{ active: userFeedback === 1 }"
        @click="submitFeedback(1)"
      >
        <text class="fb-icon">{{ userFeedback === 1 ? '✅' : '👍' }}</text>
        <text class="fb-label">有用</text>
        <text class="fb-count">{{ likeCount }}</text>
      </view>
      <view
        class="fb-btn fb-dislike"
        :class="{ active: userFeedback === 2 }"
        @click="submitFeedback(2)"
      >
        <text class="fb-icon">{{ userFeedback === 2 ? '✅' : '👎' }}</text>
        <text class="fb-label">无用</text>
        <text class="fb-count">{{ dislikeCount }}</text>
      </view>
      <view class="fb-divider"></view>
      <view class="fb-stat">
        <text class="stat-icon">📋</text>
        <text class="stat-label">{{ detail.useCount || 0 }}人使用</text>
      </view>
      <view class="fb-stat">
        <text class="stat-icon">👁</text>
        <text class="stat-label">{{ detail.viewCount || 0 }}次浏览</text>
      </view>
    </view>

    <view class="rate-card" v-if="likeCount + dislikeCount > 0">
      <view class="rate-header">
        <text class="rate-title">方案满意度</text>
        <text class="rate-value">{{ likeRate }}%</text>
      </view>
      <view class="rate-bar">
        <view class="rate-bar-inner" :style="{ width: likeRate + '%' }"></view>
      </view>
      <view class="rate-labels">
        <text class="rate-label">有用 {{ likeCount }}</text>
        <text class="rate-label">无用 {{ dislikeCount }}</text>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-icon">⚠️</text>
        <text class="section-title">故障描述</text>
      </view>
      <view class="section-content">
        <text class="content-text">{{ detail.faultDesc || '暂无描述' }}</text>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-icon">💡</text>
        <text class="section-title">解决方案</text>
        <view class="section-tag">已被验证 {{ detail.useCount || 0 }} 次</view>
      </view>
      <view class="section-content solution-content">
        <rich-text v-if="detail.solutionRichText" :nodes="detail.solutionRichText"></rich-text>
        <text v-else class="content-text">{{ detail.solution || '暂无解决方案' }}</text>
      </view>
    </view>

    <view class="section" v-if="detail.videoUrl">
      <view class="section-header">
        <text class="section-icon">🎬</text>
        <text class="section-title">视频教程</text>
      </view>
      <view class="video-card" @click="playVideo">
        <view class="video-placeholder">
          <text class="video-icon">▶️</text>
          <text class="video-text">点击播放视频教程</text>
        </view>
      </view>
    </view>

    <view class="action-bar">
      <button class="action-btn secondary" @click="copySolution">
        <text class="btn-icon">📋</text>
        <text>复制方案</text>
      </button>
      <button class="action-btn primary" @click="useSolution">
        <text class="btn-icon">✅</text>
        <text>引用此方案</text>
      </button>
    </view>

    <view class="bottom-space"></view>
  </view>

  <view class="loading-page" v-else>
    <text class="loading-text">加载中...</text>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  getKnowledgeDetail,
  submitKnowledgeFeedback,
  getUserFeedback,
  recordKnowledgeUsage
} from '@/api/knowledge'
import { getToken } from '@/utils/auth'

const detail = ref(null)
const id = ref(null)
const loading = ref(false)
const userFeedback = ref(null)
const likeCount = ref(0)
const dislikeCount = ref(0)
const submitting = ref(false)

function getLevelColor(level) {
  const map = { 1: '#52c41a', 2: '#faad14', 3: '#ff4d4f', 4: '#cf1322' }
  return map[level] || '#999'
}

function getLevelText(level) {
  const map = { 1: '低级', 2: '中级', 3: '高级', 4: '紧急' }
  return map[level] || '未知'
}

function parseTags(tags) {
  if (!tags) return []
  return tags.split(',').map(t => t.trim()).filter(Boolean)
}

function formatTime(time) {
  if (!time) return '-'
  return String(time).slice(0, 16).replace('T', ' ')
}

const likeRate = computed(() => {
  const total = likeCount.value + dislikeCount.value
  if (total === 0) return 0
  return Math.round((likeCount.value / total) * 100)
})

function getCurrentUser() {
  try {
    const userInfo = uni.getStorageSync('userInfo')
    if (userInfo) {
      return typeof userInfo === 'string' ? JSON.parse(userInfo) : userInfo
    }
    return {}
  } catch (e) {
    return {}
  }
}

async function fetchDetail() {
  if (!id.value) return
  loading.value = true
  try {
    const data = await getKnowledgeDetail(id.value)
    detail.value = data
    likeCount.value = data.likeCount || 0
    dislikeCount.value = data.dislikeCount || 0

    const user = getCurrentUser()
    if (user?.id) {
      try {
        const fb = await getUserFeedback(id.value, user.id)
        if (fb) {
          userFeedback.value = fb.feedbackType
        }
      } catch (e) {}
    }
  } catch (e) {
    console.error('获取详情失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function submitFeedback(feedbackType) {
  const user = getCurrentUser()
  if (!user?.id) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    uni.navigateTo({ url: '/pages/login/login' })
    return
  }

  if (submitting.value) return
  submitting.value = true

  const oldFb = userFeedback.value
  try {
    await submitKnowledgeFeedback({
      knowledgeId: id.value,
      userId: user.id,
      userName: user.name || user.username || '',
      feedbackType
    })

    if (oldFb !== feedbackType) {
      if (feedbackType === 1) {
        likeCount.value++
        if (oldFb === 2) dislikeCount.value = Math.max(0, dislikeCount.value - 1)
      } else if (feedbackType === 2) {
        dislikeCount.value++
        if (oldFb === 1) likeCount.value = Math.max(0, likeCount.value - 1)
      }
    }

    userFeedback.value = feedbackType
    uni.showToast({
      title: feedbackType === 1 ? '感谢您的认可 👍' : '感谢反馈，我们会持续优化',
      icon: 'none'
    })
  } catch (e) {
    console.error('反馈失败', e)
    uni.showToast({ title: e.message || '提交失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

function copySolution() {
  if (!detail.value) return
  const text = detail.value.solutionRichText
    ? detail.value.solutionRichText.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
    : (detail.value.solution || '')
  if (!text) {
    uni.showToast({ title: '暂无解决方案', icon: 'none' })
    return
  }
  uni.setClipboardData({
    data: text,
    success: () => {
      uni.showToast({ title: '已复制到剪贴板', icon: 'success' })
    }
  })
}

async function useSolution() {
  const user = getCurrentUser()
  try {
    await recordKnowledgeUsage({
      knowledgeId: id.value,
      userId: user?.id,
      userName: user?.name || user?.username,
      sourceType: 2
    })
    uni.showToast({ title: '已引用此方案', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: '已记录使用', icon: 'success' })
  }
}

function playVideo() {
  if (!detail.value?.videoUrl) return
  uni.previewImage ? null : null
  uni.showModal({
    title: '视频教程',
    content: '即将打开视频链接：' + detail.value.videoUrl,
    success: (res) => {
      if (res.confirm) {
        // #ifdef H5
        window.open(detail.value.videoUrl)
        // #endif
        // #ifndef H5
        uni.setClipboardData({
          data: detail.value.videoUrl,
          success: () => uni.showToast({ title: '链接已复制', icon: 'success' })
        })
        // #endif
      }
    }
  })
}

onLoad((options) => {
  id.value = options?.id
  if (id.value) {
    fetchDetail()
  } else {
    uni.showToast({ title: '参数错误', icon: 'none' })
  }
})
</script>

<style lang="scss" scoped>
.knowledge-detail-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 140rpx;
}

.detail-header {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  padding: 32rpx 24rpx 40rpx;
  color: #fff;
}

.header-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.fault-code {
  font-size: 24rpx;
  background: rgba(255, 255, 255, 0.2);
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  color: #fff;
}

.level-tag,
.type-tag {
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
}

.type-tag {
  background: rgba(255, 255, 255, 0.15);
  color: #e6f7ff;
}

.status-tag {
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;

  &.status-published {
    background: #52c41a;
    color: #fff;
  }
}

.detail-title {
  font-size: 36rpx;
  font-weight: 700;
  line-height: 1.4;
  margin-bottom: 16rpx;
  display: block;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.tag-item {
  font-size: 24rpx;
  color: #e6f7ff;
  background: rgba(255, 255, 255, 0.12);
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
}

.detail-meta {
  display: flex;
  gap: 32rpx;
  font-size: 24rpx;
  color: #bae7ff;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6rpx;
}

.feedback-bar {
  background: #fff;
  margin: -20rpx 24rpx 0;
  border-radius: 16rpx;
  padding: 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-around;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
  position: relative;
  z-index: 10;
}

.fb-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
  padding: 12rpx 32rpx;
  border-radius: 12rpx;
  background: #fafafa;
  transition: all 0.3s;

  &.active {
    background: #e6ffed;
  }

  &.fb-dislike.active {
    background: #fff1f0;
  }
}

.fb-icon {
  font-size: 36rpx;
}

.fb-label {
  font-size: 22rpx;
  color: #666;
}

.fb-count {
  font-size: 24rpx;
  color: #333;
  font-weight: 600;
}

.fb-divider {
  width: 2rpx;
  height: 60rpx;
  background: #f0f0f0;
}

.fb-stat {
  display: flex;
  align-items: center;
  gap: 6rpx;
  font-size: 24rpx;
  color: #8c8c8c;
}

.rate-card {
  background: #fff;
  margin: 16rpx 24rpx;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
}

.rate-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.rate-title {
  font-size: 26rpx;
  color: #333;
  font-weight: 500;
}

.rate-value {
  font-size: 32rpx;
  font-weight: 700;
  color: #52c41a;
}

.rate-bar {
  height: 12rpx;
  background: #f5f5f5;
  border-radius: 6rpx;
  overflow: hidden;
  margin-bottom: 8rpx;
}

.rate-bar-inner {
  height: 100%;
  background: linear-gradient(90deg, #52c41a 0%, #73d13d 100%);
  border-radius: 6rpx;
  transition: width 0.5s;
}

.rate-labels {
  display: flex;
  justify-content: space-between;
  font-size: 22rpx;
  color: #999;
}

.section {
  background: #fff;
  margin: 16rpx 24rpx;
  border-radius: 12rpx;
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.section-icon {
  font-size: 32rpx;
  margin-right: 12rpx;
}

.section-title {
  flex: 1;
  font-size: 30rpx;
  font-weight: 600;
  color: #262626;
}

.section-tag {
  font-size: 22rpx;
  color: #fa8c16;
  background: #fff7e6;
  padding: 4rpx 14rpx;
  border-radius: 20rpx;
}

.section-content {
  padding: 24rpx;
}

.content-text {
  font-size: 28rpx;
  color: #434343;
  line-height: 1.8;
  white-space: pre-wrap;
}

.solution-content {
  background: #fafcff;
  margin: 0 16rpx 16rpx;
  border-radius: 12rpx;
  border-left: 6rpx solid #1890ff;
}

.video-card {
  margin: 0 24rpx 24rpx;
  border-radius: 12rpx;
  overflow: hidden;
  background: #000;
}

.video-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60rpx 0;
  background: linear-gradient(135deg, #434343 0%, #000 100%);
}

.video-icon {
  font-size: 80rpx;
  margin-bottom: 16rpx;
}

.video-text {
  font-size: 28rpx;
  color: #fff;
}

.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 20rpx;
  padding: 20rpx 24rpx;
  background: #fff;
  box-shadow: 0 -2rpx 16rpx rgba(0, 0, 0, 0.06);
}

.action-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 44rpx;
  font-size: 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;

  &.secondary {
    background: #f5f5f5;
    color: #666;
    border: none;
  }

  &.primary {
    background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
    color: #fff;
    border: none;
  }
}

.btn-icon {
  font-size: 28rpx;
}

.loading-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}

.loading-text {
  font-size: 28rpx;
  color: #999;
}

.bottom-space {
  height: 20rpx;
}
</style>
