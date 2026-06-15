<template>
  <view class="plan-detail-page">
    <view class="detail-card" v-if="plan">
      <view class="status-header" :class="'header-' + plan.status">
        <view class="status-icon">{{ getStatusIcon(plan.status) }}</view>
        <view class="status-info">
          <view class="status-title">{{ getStatusText(plan.status) }}</view>
          <view class="status-sub">{{ plan.planNo }}</view>
        </view>
      </view>

      <view class="detail-section">
        <view class="section-title">基本信息</view>
        <view class="info-row">
          <view class="info-label">计划标题</view>
          <view class="info-value strong">{{ plan.title }}</view>
        </view>
        <view class="info-row">
          <view class="info-label">计划日期</view>
          <view class="info-value">{{ plan.planDate }}</view>
        </view>
        <view class="info-row">
          <view class="info-label">所属电站</view>
          <view class="info-value">{{ plan.stationName || '--' }}</view>
        </view>
        <view class="info-row">
          <view class="info-label">方阵/逆变器</view>
          <view class="info-value">{{ plan.arrayNumber || plan.inverterName || '--' }}</view>
        </view>
        <view class="info-row">
          <view class="info-label">负责人</view>
          <view class="info-value">{{ plan.ownerName || '--' }}</view>
        </view>
        <view class="info-row">
          <view class="info-label">参与团队</view>
          <view class="info-value">{{ plan.teamMembers || '--' }}</view>
        </view>
        <view class="info-row">
          <view class="info-label">清洗方式</view>
          <view class="info-value">{{ plan.cleaningMethod || '--' }}</view>
        </view>
        <view class="info-row" v-if="plan.description">
          <view class="info-label">计划描述</view>
          <view class="info-value desc">{{ plan.description }}</view>
        </view>
      </view>

      <view class="detail-section" v-if="plan.status >= 1">
        <view class="section-title">
          <text>📷 清洗前照片</text>
          <view
            v-if="plan.status === 1"
            class="upload-mini-btn"
            @click="chooseAndUpload('before')"
          >
            <text>上传</text>
          </view>
        </view>
        <view class="photo-grid" v-if="beforePhotos.length > 0">
          <view class="photo-item" v-for="(url, idx) in beforePhotos" :key="idx">
            <image
              :src="normalizeUrl(url)"
              mode="aspectFill"
              class="photo-img"
              @click="previewImage(beforePhotos, idx)"
            />
          </view>
        </view>
        <view class="upload-area" v-else @click="chooseAndUpload('before')">
          <view class="upload-icon">📸</view>
          <view class="upload-text">点击上传清洗前照片</view>
        </view>
      </view>

      <view class="detail-section" v-if="plan.status === 2">
        <view class="section-title">
          <text>✨ 清洗后照片</text>
        </view>
        <view class="photo-grid" v-if="afterPhotos.length > 0">
          <view class="photo-item" v-for="(url, idx) in afterPhotos" :key="idx">
            <image
              :src="normalizeUrl(url)"
              mode="aspectFill"
              class="photo-img"
              @click="previewImage(afterPhotos, idx)"
            />
          </view>
        </view>
        <view class="no-photo" v-else>
          <text>暂无照片</text>
        </view>
      </view>

      <view class="detail-section" v-if="plan.status === 2">
        <view class="section-title">📊 清洗效果统计</view>
        <view class="effect-card">
          <view class="effect-row">
            <view class="effect-item">
              <view class="effect-label">清洗前日均发电</view>
              <view class="effect-value">{{ plan.beforeCleanEnergy || 0 }} kWh</view>
            </view>
            <view class="effect-arrow">→</view>
            <view class="effect-item">
              <view class="effect-label">清洗后日均发电</view>
              <view class="effect-value green">{{ plan.afterCleanEnergy || 0 }} kWh</view>
            </view>
          </view>
          <view class="effect-result">
            <view class="result-main">
              <text class="result-value">+{{ plan.improvedEnergy || 0 }}</text>
              <text class="result-unit">kWh/日</text>
            </view>
            <view class="result-tag" v-if="plan.improvementRatePercent">
              提升 {{ plan.improvementRatePercent }}%
            </view>
          </view>
          <view class="effect-details" v-if="plan.cleaningCost || plan.waterUsage">
            <view class="detail-item" v-if="plan.cleaningCost">
              <text class="detail-k">清洗费用</text>
              <text class="detail-v">¥ {{ plan.cleaningCost }}</text>
            </view>
            <view class="detail-item" v-if="plan.waterUsage">
              <text class="detail-k">用水量</text>
              <text class="detail-v">{{ plan.waterUsage }} L</text>
            </view>
          </view>
        </view>
        <view class="info-row" v-if="plan.workRemark">
          <view class="info-label">工作备注</view>
          <view class="info-value desc">{{ plan.workRemark }}</view>
        </view>
        <view class="info-row" v-if="plan.inspectionRemark">
          <view class="info-label">验收意见</view>
          <view class="info-value desc">{{ plan.inspectionRemark }}</view>
        </view>
        <view class="info-row" v-if="plan.inspectorName">
          <view class="info-label">验收人</view>
          <view class="info-value">{{ plan.inspectorName }} · {{ formatTime(plan.inspectionTime) }}</view>
        </view>
      </view>

      <view class="detail-section" v-if="plan.actualStartTime || plan.actualEndTime">
        <view class="section-title">⏱️ 执行时间线</view>
        <view class="timeline">
          <view class="timeline-item">
            <view class="timeline-dot blue"></view>
            <view class="timeline-content">
              <view class="timeline-label">开始执行</view>
              <view class="timeline-time">{{ formatTime(plan.actualStartTime) || '--' }}</view>
            </view>
          </view>
          <view class="timeline-item">
            <view class="timeline-dot green"></view>
            <view class="timeline-content">
              <view class="timeline-label">完成时间</view>
              <view class="timeline-time">{{ formatTime(plan.actualEndTime) || '--' }}</view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="bottom-action-bar">
      <view
        v-if="plan?.status === 0"
        class="action-btn primary"
        @click="handleStart"
      >
        <text>开始执行</text>
      </view>
      <view
        v-if="plan?.status === 0"
        class="action-btn danger"
        @click="handleCancel"
      >
        <text>取消计划</text>
      </view>
      <view
        v-if="plan?.status === 1"
        class="action-btn upload"
        @click="chooseAndUpload('before')"
      >
        <text>📷 上传照片</text>
      </view>
      <view
        v-if="plan?.status === 1"
        class="action-btn success"
        @click="showCompleteForm = true"
      >
        <text>完成清洗</text>
      </view>
      <view
        v-if="plan?.status === 0"
        class="action-btn edit"
        @click="goToEdit"
      >
        <text>编辑</text>
      </view>
    </view>

    <view class="complete-modal" v-if="showCompleteForm" @click="showCompleteForm = false">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">完成清洗计划</text>
          <text class="modal-close" @click="showCompleteForm = false">×</text>
        </view>
        <scroll-view scroll-y class="modal-body">
          <view class="form-item">
            <view class="form-label">清洗后照片</view>
            <view class="photo-grid form-photos">
              <view class="photo-item" v-for="(url, idx) in completePhotos.after" :key="idx">
                <image
                  :src="normalizeUrl(url)"
                  mode="aspectFill"
                  class="photo-img"
                  @click="previewImage(completePhotos.after, idx)"
                />
                <view class="photo-delete" @click="removeCompletePhoto('after', idx)">×</view>
              </view>
              <view
                v-if="completePhotos.after.length < 9"
                class="photo-add"
                @click="chooseAndUploadComplete('after')"
              >
                <text class="add-icon">+</text>
                <text class="add-text">上传</text>
              </view>
            </view>
          </view>
          <view class="form-item" v-if="completePhotos.before.length === 0">
            <view class="form-label">补充清洗前照片（可选）</view>
            <view class="photo-grid form-photos">
              <view class="photo-item" v-for="(url, idx) in completePhotos.before" :key="idx">
                <image
                  :src="normalizeUrl(url)"
                  mode="aspectFill"
                  class="photo-img"
                />
                <view class="photo-delete" @click="removeCompletePhoto('before', idx)">×</view>
              </view>
              <view
                v-if="completePhotos.before.length < 9"
                class="photo-add"
                @click="chooseAndUploadComplete('before')"
              >
                <text class="add-icon">+</text>
                <text class="add-text">上传</text>
              </view>
            </view>
          </view>
          <view class="form-row">
            <view class="form-item half">
              <view class="form-label">清洗方式</view>
              <picker
                :range="cleaningMethods"
                @change="onMethodChange"
              >
                <view class="picker-value">
                  {{ completeForm.cleaningMethod || '请选择' }}
                </view>
              </picker>
            </view>
            <view class="form-item half">
              <view class="form-label">用水量 (L)</view>
              <input
                type="digit"
                class="form-input"
                placeholder="请输入"
                v-model="completeForm.waterUsage"
              />
            </view>
          </view>
          <view class="form-item">
            <view class="form-label">清洗费用 (元)</view>
            <input
              type="digit"
              class="form-input"
              placeholder="请输入清洗费用"
              v-model="completeForm.cleaningCost"
            />
          </view>
          <view class="form-item">
            <view class="form-label">工作备注</view>
            <textarea
              class="form-textarea"
              placeholder="清洗过程中的问题、发现或说明..."
              v-model="completeForm.workRemark"
              :maxlength="500"
            />
          </view>
          <view class="form-item">
            <view class="form-label">验收意见</view>
            <textarea
              class="form-textarea"
              placeholder="验收人的评价或意见..."
              v-model="completeForm.inspectionRemark"
              :maxlength="300"
            />
          </view>
        </scroll-view>
        <view class="modal-footer">
          <view class="footer-btn cancel" @click="showCompleteForm = false">取消</view>
          <view class="footer-btn confirm" @click="handleComplete">确认完成</view>
        </view>
      </view>
    </view>

    <view class="loading-tip" v-if="loading">
      <text>加载中...</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import {
  getCleaningPlanDetail,
  startCleaningPlan,
  completeCleaningPlan,
  cancelCleaningPlan,
  uploadFile,
  uploadCleaningPhotos
} from '@/api/cleaning'

const loading = ref(false)
const plan = ref(null)
const showCompleteForm = ref(false)
const planId = ref(null)
const cleaningMethods = ['人工清洗', '机械清洗', '机器人清洗', '无水清洗']

const completeForm = ref({
  cleaningMethod: '',
  waterUsage: '',
  cleaningCost: '',
  workRemark: '',
  inspectionRemark: ''
})

const completePhotos = ref({
  before: [],
  after: []
})

const beforePhotos = computed(() => {
  if (plan.value?.beforeCleanPhotos) {
    return plan.value.beforeCleanPhotos.split(',').filter(Boolean)
  }
  return []
})

const afterPhotos = computed(() => {
  if (plan.value?.afterCleanPhotos) {
    return plan.value.afterCleanPhotos.split(',').filter(Boolean)
  }
  return []
})

function normalizeUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return url
}

function getStatusText(status) {
  const map = { 0: '待执行', 1: '执行中', 2: '已完成', 3: '已取消' }
  return map[status] || '未知'
}

function getStatusIcon(status) {
  const map = { 0: '⏳', 1: '🔧', 2: '✅', 3: '❌' }
  return map[status] || '📋'
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
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

onLoad((options) => {
  planId.value = options?.id
  if (options?.action === 'complete') {
    showCompleteForm.value = true
  }
})

async function fetchDetail() {
  if (!planId.value) return
  loading.value = true
  try {
    const data = await getCleaningPlanDetail(planId.value)
    plan.value = data
  } catch (err) {
    console.error('获取计划详情失败:', err)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function previewImage(urls, current) {
  const normalizedUrls = urls.map(normalizeUrl)
  uni.previewImage({
    current: normalizedUrls[current] || normalizedUrls[0],
    urls: normalizedUrls
  })
}

async function chooseAndUpload(type) {
  try {
    const res = await uni.chooseImage({
      count: 9,
      sourceType: ['album', 'camera'],
      sizeType: ['compressed']
    })

    uni.showLoading({ title: '上传中...' })
    const uploadedUrls = []
    const paths = res.tempFilePaths || res.tempFiles?.map(f => f.path) || []

    for (let i = 0; i < paths.length; i++) {
      try {
        const result = await uploadFile(paths[i])
        if (result?.url) {
          uploadedUrls.push(result.url)
        }
      } catch (err) {
        console.error('单图上传失败:', err)
      }
    }

    uni.hideLoading()

    if (uploadedUrls.length > 0) {
      const urlStr = uploadedUrls.join(',')
      const { operatorId, operatorName } = getCurrentUser()
      await uploadCleaningPhotos({
        planId: planId.value,
        [type === 'before' ? 'beforePhotos' : 'afterPhotos']: urlStr
      })
      uni.showToast({ title: `上传成功 ${uploadedUrls.length} 张`, icon: 'success' })
      fetchDetail()
    } else {
      uni.showToast({ title: '上传失败', icon: 'none' })
    }
  } catch (err) {
    uni.hideLoading()
    if (err?.errMsg?.includes('cancel')) return
    console.error('选择图片失败:', err)
  }
}

function onMethodChange(e) {
  completeForm.value.cleaningMethod = cleaningMethods[e.detail.value]
}

async function chooseAndUploadComplete(type) {
  try {
    const res = await uni.chooseImage({
      count: 9 - completePhotos.value[type].length,
      sourceType: ['album', 'camera'],
      sizeType: ['compressed']
    })
    const paths = res.tempFilePaths || res.tempFiles?.map(f => f.path) || []
    uni.showLoading({ title: '上传中...' })
    for (let i = 0; i < paths.length; i++) {
      try {
        const result = await uploadFile(paths[i])
        if (result?.url) {
          completePhotos.value[type].push(result.url)
        }
      } catch (err) {
        console.error('上传失败:', err)
      }
    }
    uni.hideLoading()
  } catch (err) {
    uni.hideLoading()
  }
}

function removeCompletePhoto(type, idx) {
  completePhotos.value[type].splice(idx, 1)
}

async function handleStart() {
  uni.showModal({
    title: '开始执行',
    content: '确定开始执行该清洗计划吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          const { operatorId, operatorName } = getCurrentUser()
          await startCleaningPlan({
            planId: planId.value,
            operatorId,
            operatorName
          })
          uni.showToast({ title: '已开始执行', icon: 'success' })
          fetchDetail()
        } catch (err) {
          uni.showToast({ title: err.message || '操作失败', icon: 'none' })
        }
      }
    }
  })
}

async function handleCancel() {
  uni.showModal({
    title: '取消计划',
    content: '确定取消该清洗计划吗？此操作不可恢复。',
    confirmColor: '#ff4d4f',
    success: async (res) => {
      if (res.confirm) {
        try {
          const { operatorId, operatorName } = getCurrentUser()
          await cancelCleaningPlan(planId.value, {
            operatorId,
            operatorName,
            reason: '移动端取消'
          })
          uni.showToast({ title: '已取消', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 800)
        } catch (err) {
          uni.showToast({ title: err.message || '操作失败', icon: 'none' })
        }
      }
    }
  })
}

async function handleComplete() {
  if (completePhotos.value.after.length === 0) {
    uni.showToast({ title: '请上传清洗后照片', icon: 'none' })
    return
  }
  try {
    const { operatorId, operatorName } = getCurrentUser()
    const params = {
      planId: planId.value,
      operatorId,
      operatorName,
      afterCleanPhotos: completePhotos.value.after.join(','),
      beforeCleanPhotos: completePhotos.value.before.join(','),
      workRemark: completeForm.value.workRemark,
      inspectionRemark: completeForm.value.inspectionRemark
    }
    if (completeForm.value.cleaningMethod) {
      params.cleaningMethod = completeForm.value.cleaningMethod
    }
    if (completeForm.value.waterUsage) {
      params.waterUsage = Number(completeForm.value.waterUsage)
    }
    if (completeForm.value.cleaningCost) {
      params.cleaningCost = Number(completeForm.value.cleaningCost)
    }
    await completeCleaningPlan(params)
    uni.showToast({ title: '完成成功', icon: 'success' })
    showCompleteForm.value = false
    fetchDetail()
  } catch (err) {
    uni.showToast({ title: err.message || '提交失败', icon: 'none' })
  }
}

function goToEdit() {
  uni.navigateTo({ url: `/pages/cleaning/plan-edit?id=${planId.value}` })
}

onShow(() => {
  fetchDetail()
})

onMounted(() => {
  fetchDetail()
})
</script>

<style lang="scss" scoped>
.plan-detail-page {
  min-height: 100vh;
  background-color: #f5f6fa;
  padding-bottom: 180rpx;
}

.detail-card {
  margin: 20rpx;
}

.status-header {
  display: flex;
  align-items: center;
  padding: 40rpx 30rpx;
  border-radius: 20rpx 20rpx 0 0;
  color: #fff;

  &.header-0 { background: linear-gradient(135deg, #fa8c16 0%, #ffa940 100%); }
  &.header-1 { background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%); }
  &.header-2 { background: linear-gradient(135deg, #52c41a 0%, #95de64 100%); }
  &.header-3 { background: linear-gradient(135deg, #8c8c8c 0%, #bfbfbf 100%); }

  .status-icon {
    font-size: 70rpx;
    margin-right: 24rpx;
  }

  .status-title {
    font-size: 40rpx;
    font-weight: 700;
    margin-bottom: 8rpx;
  }

  .status-sub {
    font-size: 24rpx;
    opacity: 0.85;
  }
}

.detail-section {
  background-color: #fff;
  padding: 28rpx 30rpx;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child {
    border-bottom: none;
    border-radius: 0 0 20rpx 20rpx;
  }

  .section-title {
    font-size: 28rpx;
    font-weight: 600;
    color: #262626;
    margin-bottom: 24rpx;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .upload-mini-btn {
      padding: 8rpx 20rpx;
      background-color: #e6f7ff;
      color: #1890ff;
      border-radius: 20rpx;
      font-size: 22rpx;
      font-weight: normal;
    }
  }
}

.info-row {
  display: flex;
  margin-bottom: 20rpx;

  &:last-child { margin-bottom: 0; }

  .info-label {
    width: 180rpx;
    font-size: 26rpx;
    color: #8c8c8c;
    flex-shrink: 0;
  }

  .info-value {
    flex: 1;
    font-size: 26rpx;
    color: #262626;
    word-break: break-all;

    &.strong {
      font-weight: 600;
      color: #262626;
      font-size: 28rpx;
    }

    &.desc {
      line-height: 1.6;
      color: #595959;
    }
  }
}

.photo-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;

  .photo-item {
    width: calc((100% - 32rpx) / 3);
    aspect-ratio: 1;
    position: relative;
    border-radius: 12rpx;
    overflow: hidden;
  }

  .photo-img {
    width: 100%;
    height: 100%;
  }

  .photo-delete {
    position: absolute;
    top: 0;
    right: 0;
    width: 40rpx;
    height: 40rpx;
    line-height: 36rpx;
    text-align: center;
    background-color: rgba(0, 0, 0, 0.6);
    color: #fff;
    font-size: 30rpx;
    border-bottom-left-radius: 12rpx;
  }

  .photo-add {
    width: calc((100% - 32rpx) / 3);
    aspect-ratio: 1;
    border: 2rpx dashed #d9d9d9;
    border-radius: 12rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #bfbfbf;

    .add-icon {
      font-size: 60rpx;
      line-height: 1;
    }

    .add-text {
      font-size: 22rpx;
      margin-top: 8rpx;
    }
  }

  &.form-photos {
    margin-top: 16rpx;
  }
}

.upload-area {
  border: 2rpx dashed #d9d9d9;
  border-radius: 12rpx;
  padding: 60rpx 0;
  text-align: center;

  .upload-icon {
    font-size: 60rpx;
    margin-bottom: 16rpx;
  }

  .upload-text {
    font-size: 26rpx;
    color: #bfbfbf;
  }
}

.no-photo {
  padding: 40rpx 0;
  text-align: center;
  color: #bfbfbf;
  font-size: 26rpx;
}

.effect-card {
  background: linear-gradient(135deg, #f6ffed 0%, #fcffe6 100%);
  border-radius: 16rpx;
  padding: 28rpx;

  .effect-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 28rpx;
  }

  .effect-item {
    flex: 1;
    text-align: center;
  }

  .effect-label {
    font-size: 22rpx;
    color: #8c8c8c;
    margin-bottom: 10rpx;
  }

  .effect-value {
    font-size: 32rpx;
    font-weight: 600;
    color: #262626;

    &.green { color: #389e0d; }
  }

  .effect-arrow {
    width: 60rpx;
    text-align: center;
    font-size: 36rpx;
    color: #52c41a;
  }

  .effect-result {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20rpx;
    background-color: rgba(255, 255, 255, 0.7);
    border-radius: 12rpx;
    margin-bottom: 20rpx;
  }

  .result-main {
    display: flex;
    align-items: baseline;
  }

  .result-value {
    font-size: 48rpx;
    font-weight: 700;
    color: #389e0d;
  }

  .result-unit {
    font-size: 24rpx;
    color: #52c41a;
    margin-left: 8rpx;
  }

  .result-tag {
    padding: 10rpx 24rpx;
    background-color: #52c41a;
    color: #fff;
    border-radius: 30rpx;
    font-size: 26rpx;
    font-weight: 500;
  }

  .effect-details {
    display: flex;
    gap: 20rpx;

    .detail-item {
      flex: 1;
      display: flex;
      justify-content: space-between;
      background-color: rgba(255, 255, 255, 0.6);
      padding: 16rpx 20rpx;
      border-radius: 10rpx;
    }

    .detail-k {
      font-size: 24rpx;
      color: #8c8c8c;
    }

    .detail-v {
      font-size: 24rpx;
      color: #262626;
      font-weight: 500;
    }
  }
}

.timeline {
  .timeline-item {
    display: flex;
    align-items: flex-start;
    margin-bottom: 24rpx;

    &:last-child { margin-bottom: 0; }
  }

  .timeline-dot {
    width: 20rpx;
    height: 20rpx;
    border-radius: 50%;
    margin-right: 20rpx;
    margin-top: 8rpx;
    flex-shrink: 0;

    &.blue { background-color: #1890ff; box-shadow: 0 0 0 6rpx #e6f7ff; }
    &.green { background-color: #52c41a; box-shadow: 0 0 0 6rpx #f6ffed; }
  }

  .timeline-label {
    font-size: 26rpx;
    color: #262626;
    margin-bottom: 6rpx;
  }

  .timeline-time {
    font-size: 24rpx;
    color: #8c8c8c;
  }
}

.bottom-action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #fff;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  display: flex;
  gap: 20rpx;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.05);

  .action-btn {
    flex: 1;
    height: 88rpx;
    line-height: 88rpx;
    text-align: center;
    border-radius: 44rpx;
    font-size: 30rpx;
    font-weight: 500;

    &.primary {
      background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
      color: #fff;
      flex: 2;
    }

    &.success {
      background: linear-gradient(135deg, #52c41a 0%, #95de64 100%);
      color: #fff;
      flex: 2;
    }

    &.upload {
      background: linear-gradient(135deg, #722ed1 0%, #9254de 100%);
      color: #fff;
    }

    &.danger {
      background-color: #fff1f0;
      color: #ff4d4f;
      border: 2rpx solid #ffa39e;
    }

    &.edit {
      background-color: #f5f5f5;
      color: #595959;
    }
  }
}

.complete-modal {
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
    max-height: 85vh;
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
    padding: 30rpx;
    max-height: 60vh;
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
        background: linear-gradient(135deg, #52c41a 0%, #95de64 100%);
        color: #fff;
      }
    }
  }
}

.form-item {
  margin-bottom: 28rpx;

  &.half {
    width: calc(50% - 10rpx);
  }

  .form-label {
    font-size: 26rpx;
    color: #595959;
    margin-bottom: 12rpx;
  }

  .form-input, .picker-value {
    width: 100%;
    height: 80rpx;
    line-height: 80rpx;
    padding: 0 24rpx;
    background-color: #f5f6fa;
    border-radius: 12rpx;
    font-size: 28rpx;
    color: #262626;
    box-sizing: border-box;
  }

  .picker-value {
    color: #262626;
  }

  .form-textarea {
    width: 100%;
    min-height: 160rpx;
    padding: 20rpx 24rpx;
    background-color: #f5f6fa;
    border-radius: 12rpx;
    font-size: 28rpx;
    color: #262626;
    box-sizing: border-box;
  }
}

.form-row {
  display: flex;
  justify-content: space-between;
}

.loading-tip {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #999;
  font-size: 28rpx;
}
</style>
