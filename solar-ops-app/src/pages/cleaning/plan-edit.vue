<template>
  <view class="plan-edit-page">
    <view class="form-card">
      <view class="form-header">
        <text class="form-title">{{ isEdit ? '编辑清洗计划' : '新建清洗计划' }}</text>
      </view>

      <view class="form-item">
        <view class="form-label required">计划标题</view>
        <input
          class="form-input"
          placeholder="如：3号方阵清洗作业"
          v-model="formData.title"
          :maxlength="50"
        />
      </view>

      <view class="form-item">
        <view class="form-label required">计划清洗日期</view>
        <picker
          mode="date"
          :value="formData.planDate"
          @change="onDateChange"
        >
          <view class="picker-value">
            {{ formData.planDate || '请选择日期' }}
          </view>
        </picker>
      </view>

      <view class="form-row">
        <view class="form-item half">
          <view class="form-label required">电站ID</view>
          <input
            type="number"
            class="form-input"
            placeholder="请输入"
            v-model="formData.stationId"
          />
        </view>
        <view class="form-item half">
          <view class="form-label">电站名称</view>
          <input
            class="form-input"
            placeholder="请输入"
            v-model="formData.stationName"
          />
        </view>
      </view>

      <view class="form-row">
        <view class="form-item half">
          <view class="form-label">逆变器ID</view>
          <input
            type="number"
            class="form-input"
            placeholder="请输入"
            v-model="formData.inverterId"
          />
        </view>
        <view class="form-item half">
          <view class="form-label">方阵编号</view>
          <input
            class="form-input"
            placeholder="如：3号方阵"
            v-model="formData.arrayNumber"
          />
        </view>
      </view>

      <view class="form-row">
        <view class="form-item half">
          <view class="form-label">负责人ID</view>
          <input
            type="number"
            class="form-input"
            placeholder="请输入"
            v-model="formData.ownerId"
          />
        </view>
        <view class="form-item half">
          <view class="form-label">负责人姓名</view>
          <input
            class="form-input"
            placeholder="请输入"
            v-model="formData.ownerName"
          />
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
              {{ formData.cleaningMethod || '请选择' }}
            </view>
          </picker>
        </view>
        <view class="form-item half">
          <view class="form-label">清洗费用(元)</view>
          <input
            type="digit"
            class="form-input"
            placeholder="请输入"
            v-model="formData.cleaningCost"
          />
        </view>
      </view>

      <view class="form-item">
        <view class="form-label">参与人员/团队</view>
        <input
          class="form-input"
          placeholder="多人请用逗号分隔"
          v-model="formData.teamMembers"
          :maxlength="200"
        />
      </view>

      <view class="form-item">
        <view class="form-label">计划描述</view>
        <textarea
          class="form-textarea"
          placeholder="请详细描述清洗作业范围、要求等..."
          v-model="formData.description"
          :maxlength="500"
        />
      </view>

      <view class="form-tip" v-if="reminderInfo">
        <view class="tip-icon">💡</view>
        <view class="tip-content">
          <view class="tip-title">基于清洗建议创建</view>
          <view class="tip-desc">{{ reminderInfo.title }}</view>
        </view>
      </view>
    </view>

    <view class="bottom-bar">
      <view class="submit-btn" @click="handleSubmit">
        <text>{{ isEdit ? '保存修改' : '创建计划' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  createCleaningPlan,
  updateCleaningPlan,
  getCleaningPlanDetail,
  getReminderDetail
} from '@/api/cleaning'

const loading = ref(false)
const isEdit = ref(false)
const planId = ref(null)
const reminderId = ref(null)
const reminderInfo = ref(null)
const cleaningMethods = ['人工清洗', '机械清洗', '机器人清洗', '无水清洗']

const formData = reactive({
  title: '',
  planDate: '',
  stationId: '',
  stationName: '',
  inverterId: '',
  arrayNumber: '',
  ownerId: '',
  ownerName: '',
  cleaningMethod: '',
  cleaningCost: '',
  teamMembers: '',
  description: ''
})

function getCurrentUser() {
  try {
    const userInfo = uni.getStorageSync('userInfo')
    if (userInfo) {
      const parsed = typeof userInfo === 'string' ? JSON.parse(userInfo) : userInfo
      return {
        id: parsed.id || parsed.userId || '',
        name: parsed.name || parsed.userName || '移动端用户'
      }
    }
  } catch (e) {}
  return { id: '', name: '移动端用户' }
}

onLoad(async (options) => {
  if (options?.id) {
    isEdit.value = true
    planId.value = options.id
    await loadPlanDetail(options.id)
  }
  if (options?.date) {
    formData.planDate = options.date
  }
  if (options?.reminderId) {
    reminderId.value = options.reminderId
    await loadReminderInfo(options.reminderId)
  }
})

async function loadPlanDetail(id) {
  try {
    const data = await getCleaningPlanDetail(id)
    if (data) {
      formData.title = data.title || ''
      formData.planDate = data.planDate || ''
      formData.stationId = data.stationId || ''
      formData.stationName = data.stationName || ''
      formData.inverterId = data.inverterId || ''
      formData.arrayNumber = data.arrayNumber || ''
      formData.ownerId = data.ownerId || ''
      formData.ownerName = data.ownerName || ''
      formData.cleaningMethod = data.cleaningMethod || ''
      formData.cleaningCost = data.cleaningCost || ''
      formData.teamMembers = data.teamMembers || ''
      formData.description = data.description || ''
    }
  } catch (err) {
    console.error('加载计划详情失败:', err)
  }
}

async function loadReminderInfo(id) {
  try {
    const data = await getReminderDetail(id)
    if (data) {
      reminderInfo.value = data
      if (!formData.title) {
        formData.title = data.title?.replace('推荐', '清洗作业') || ''
      }
      if (!formData.stationId) formData.stationId = data.stationId || ''
      if (!formData.stationName) formData.stationName = data.stationName || ''
      if (!formData.inverterId) formData.inverterId = data.inverterId || ''
      if (!formData.arrayNumber) formData.arrayNumber = data.arrayNumber || ''
      if (!formData.planDate && data.suggestCleanDate) {
        formData.planDate = data.suggestCleanDate
      }
    }
  } catch (err) {
    console.error('加载提醒信息失败:', err)
  }
}

function onDateChange(e) {
  formData.planDate = e.detail.value
}

function onMethodChange(e) {
  formData.cleaningMethod = cleaningMethods[e.detail.value]
}

function validate() {
  if (!formData.title.trim()) {
    uni.showToast({ title: '请输入计划标题', icon: 'none' })
    return false
  }
  if (!formData.planDate) {
    uni.showToast({ title: '请选择清洗日期', icon: 'none' })
    return false
  }
  if (!formData.stationId) {
    uni.showToast({ title: '请输入电站ID', icon: 'none' })
    return false
  }
  return true
}

async function handleSubmit() {
  if (!validate()) return

  loading.value = true
  try {
    const user = getCurrentUser()
    const submitData = {
      ...formData,
      reminderId: reminderId.value || undefined,
      stationId: formData.stationId ? Number(formData.stationId) : undefined,
      inverterId: formData.inverterId ? Number(formData.inverterId) : undefined,
      ownerId: formData.ownerId ? Number(formData.ownerId) : undefined,
      cleaningCost: formData.cleaningCost ? Number(formData.cleaningCost) : undefined
    }

    if (isEdit.value) {
      submitData.id = Number(planId.value)
      await updateCleaningPlan(submitData, {
        operatorId: user.id,
        operatorName: user.name
      })
      uni.showToast({ title: '修改成功', icon: 'success' })
    } else {
      await createCleaningPlan(submitData, {
        creatorId: user.id,
        creatorName: user.name
      })
      uni.showToast({ title: '创建成功', icon: 'success' })
    }

    setTimeout(() => uni.navigateBack(), 800)
  } catch (err) {
    uni.showToast({ title: err.message || (isEdit.value ? '修改失败' : '创建失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.plan-edit-page {
  min-height: 100vh;
  background-color: #f5f6fa;
  padding-bottom: 180rpx;
}

.form-card {
  margin: 20rpx;
  background-color: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.form-header {
  margin-bottom: 30rpx;
  padding-bottom: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;

  .form-title {
    font-size: 34rpx;
    font-weight: 600;
    color: #262626;
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

    &.required::before {
      content: '*';
      color: #ff4d4f;
      margin-right: 6rpx;
    }
  }

  .form-input, .picker-value {
    width: 100%;
    height: 84rpx;
    line-height: 84rpx;
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
    min-height: 180rpx;
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

.form-tip {
  display: flex;
  align-items: flex-start;
  padding: 24rpx;
  background: linear-gradient(135deg, #fff7e6 0%, #fffbe6 100%);
  border-radius: 12rpx;
  margin-top: 10rpx;

  .tip-icon {
    font-size: 36rpx;
    margin-right: 16rpx;
    flex-shrink: 0;
  }

  .tip-title {
    font-size: 26rpx;
    font-weight: 600;
    color: #ad6800;
    margin-bottom: 6rpx;
  }

  .tip-desc {
    font-size: 24rpx;
    color: #874d00;
    line-height: 1.5;
  }
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #fff;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.05);

  .submit-btn {
    height: 96rpx;
    line-height: 96rpx;
    text-align: center;
    background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
    color: #fff;
    border-radius: 48rpx;
    font-size: 32rpx;
    font-weight: 600;
    box-shadow: 0 8rpx 24rpx rgba(24, 144, 255, 0.3);
  }
}
</style>
