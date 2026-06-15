<template>
  <view class="calendar-page">
    <view class="calendar-container">
      <view class="calendar-header">
        <view class="month-nav" @click="prevMonth">
          <text>‹</text>
        </view>
        <view class="month-title">{{ currentYear }}年{{ currentMonth }}月</view>
        <view class="month-nav" @click="nextMonth">
          <text>›</text>
        </view>
      </view>

      <view class="weekday-row">
        <view class="weekday-item" v-for="day in weekDays" :key="day">{{ day }}</view>
      </view>

      <view class="days-grid">
        <view
          v-for="(day, index) in calendarDays"
          :key="index"
          class="day-cell"
          :class="{
            'other-month': !day.currentMonth,
            'today': day.isToday,
            'selected': day.date === selectedDate,
            'has-plan': day.planCount > 0
          }"
          @click="selectDate(day)"
        >
          <view class="day-number">{{ day.day }}</view>
          <view class="day-dots" v-if="day.planCount > 0">
            <view
              v-for="i in Math.min(day.planCount, 3)"
              :key="i"
              class="plan-dot"
              :class="'dot-' + (i - 1)"
            ></view>
          </view>
          <view class="plan-count-badge" v-else-if="day.planCount > 0">
            {{ day.planCount }}
          </view>
        </view>
      </view>
    </view>

    <view class="selected-date-info" v-if="selectedDate">
      <view class="info-header">
        <text class="info-date">{{ selectedDate }}</text>
        <text class="info-count" v-if="dayPlans.length > 0">共 {{ dayPlans.length }} 个计划</text>
        <view class="add-btn" @click="createPlanForDate">
          <text>+ 新建</text>
        </view>
      </view>

      <view class="day-plan-list" v-if="dayPlans.length > 0">
        <view
          class="day-plan-card"
          v-for="plan in dayPlans"
          :key="plan.id"
          @click="goToDetail(plan.id)"
        >
          <view class="plan-status-bar" :class="getStatusClass(plan.status)"></view>
          <view class="plan-main">
            <view class="plan-head">
              <text class="plan-title">{{ plan.title }}</text>
              <view class="plan-status" :class="'status-tag-' + plan.status">
                {{ getStatusText(plan.status) }}
              </view>
            </view>
            <view class="plan-info-row">
              <text class="info-label">🏢</text>
              <text class="info-value">{{ plan.stationName || '未知电站' }}</text>
            </view>
            <view class="plan-info-row" v-if="plan.arrayNumber || plan.inverterName">
              <text class="info-label">📍</text>
              <text class="info-value">{{ plan.arrayNumber || plan.inverterName }}</text>
            </view>
            <view class="plan-info-row">
              <text class="info-label">👤</text>
              <text class="info-value">{{ plan.ownerName || '未指派' }}</text>
            </view>
            <view class="plan-improvement" v-if="plan.status === 2 && plan.improvedEnergy > 0">
              <text class="improvement-text">📈 发电量提升: +{{ plan.improvedEnergy }} kWh</text>
              <text class="improvement-rate" v-if="plan.improvementRatePercent">
                (+{{ plan.improvementRatePercent }}%)
              </text>
            </view>
          </view>
          <view class="plan-actions" v-if="plan.status === 0 || plan.status === 1">
            <view
              v-if="plan.status === 0"
              class="action-btn primary"
              @click.stop="startPlan(plan)"
            >
              <text>开始</text>
            </view>
            <view
              v-if="plan.status === 1"
              class="action-btn success"
              @click.stop="completePlan(plan)"
            >
              <text>完成</text>
            </view>
          </view>
        </view>
      </view>

      <view class="empty-day" v-else>
        <text class="empty-icon">🗓️</text>
        <text class="empty-text">{{ selectedDate }} 暂无清洗计划</text>
        <view class="empty-tips">
          <text>💡 点击右上方 "+ 新建" 按钮创建计划</text>
        </view>
      </view>
    </view>

    <view class="stats-bar">
      <view class="stats-item">
        <view class="stats-num orange">{{ stats.pending }}</view>
        <view class="stats-label">待执行</view>
      </view>
      <view class="stats-item">
        <view class="stats-num blue">{{ stats.progress }}</view>
        <view class="stats-label">执行中</view>
      </view>
      <view class="stats-item">
        <view class="stats-num green">{{ stats.done }}</view>
        <view class="stats-label">本月完成</view>
      </view>
      <view class="stats-item">
        <view class="stats-num cyan">{{ stats.improved }}</view>
        <view class="stats-label">提升(kWh)</view>
      </view>
    </view>

    <view class="float-create-btn" @click="goToCreate">
      <text class="plus-icon">+</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  getCleaningPlanCalendar,
  getCleaningPlanList,
  startCleaningPlan,
  completeCleaningPlan
} from '@/api/cleaning'

const weekDays = ['日', '一', '二', '三', '四', '五', '六']
const now = new Date()

const currentYear = ref(now.getFullYear())
const currentMonth = ref(now.getMonth() + 1)
const selectedDate = ref(formatDate(now))
const monthPlans = ref([])
const dayPlans = ref([])
const stats = ref({ pending: 0, progress: 0, done: 0, improved: '0' })

function padZero(n) {
  return n < 10 ? '0' + n : '' + n
}

function formatDate(date) {
  return `${date.getFullYear()}-${padZero(date.getMonth() + 1)}-${padZero(date.getDate())}`
}

function getDaysInMonth(year, month) {
  return new Date(year, month, 0).getDate()
}

function getFirstDayOfMonth(year, month) {
  return new Date(year, month - 1, 1).getDay()
}

const calendarDays = computed(() => {
  const days = []
  const daysInMonth = getDaysInMonth(currentYear.value, currentMonth.value)
  const firstDay = getFirstDayOfMonth(currentYear.value, currentMonth.value)

  const prevMonthDays = getDaysInMonth(
    currentMonth.value === 1 ? currentYear.value - 1 : currentYear.value,
    currentMonth.value === 1 ? 12 : currentMonth.value - 1
  )

  for (let i = firstDay - 1; i >= 0; i--) {
    const day = prevMonthDays - i
    const year = currentMonth.value === 1 ? currentYear.value - 1 : currentYear.value
    const month = currentMonth.value === 1 ? 12 : currentMonth.value - 1
    const dateStr = `${year}-${padZero(month)}-${padZero(day)}`
    days.push({
      day,
      date: dateStr,
      currentMonth: false,
      isToday: false,
      planCount: getPlanCountForDate(dateStr)
    })
  }

  const todayStr = formatDate(new Date())
  for (let i = 1; i <= daysInMonth; i++) {
    const dateStr = `${currentYear.value}-${padZero(currentMonth.value)}-${padZero(i)}`
    days.push({
      day: i,
      date: dateStr,
      currentMonth: true,
      isToday: dateStr === todayStr,
      planCount: getPlanCountForDate(dateStr)
    })
  }

  const remaining = 42 - days.length
  const nextMonth = currentMonth.value === 12 ? 1 : currentMonth.value + 1
  const nextYear = currentMonth.value === 12 ? currentYear.value + 1 : currentYear.value
  for (let i = 1; i <= remaining; i++) {
    const dateStr = `${nextYear}-${padZero(nextMonth)}-${padZero(i)}`
    days.push({
      day: i,
      date: dateStr,
      currentMonth: false,
      isToday: false,
      planCount: getPlanCountForDate(dateStr)
    })
  }

  return days
})

function getPlanCountForDate(dateStr) {
  return monthPlans.value.filter(p => p.planDate === dateStr).length
}

function getStatusClass(status) {
  const map = {
    0: 'status-pending',
    1: 'status-progress',
    2: 'status-done',
    3: 'status-cancel'
  }
  return map[status] || 'status-pending'
}

function getStatusText(status) {
  const map = { 0: '待执行', 1: '执行中', 2: '已完成', 3: '已取消' }
  return map[status] || '未知'
}

function prevMonth() {
  if (currentMonth.value === 1) {
    currentMonth.value = 12
    currentYear.value--
  } else {
    currentMonth.value--
  }
  fetchMonthPlans()
}

function nextMonth() {
  if (currentMonth.value === 12) {
    currentMonth.value = 1
    currentYear.value++
  } else {
    currentMonth.value++
  }
  fetchMonthPlans()
}

async function selectDate(day) {
  selectedDate.value = day.date
  await fetchDayPlans()
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

async function fetchMonthPlans() {
  try {
    const startDate = `${currentYear.value}-${padZero(currentMonth.value)}-01`
    const endDate = `${currentYear.value}-${padZero(currentMonth.value)}-${getDaysInMonth(currentYear.value, currentMonth.value)}`
    const data = await getCleaningPlanCalendar({ startDate, endDate })
    monthPlans.value = data || []
    await fetchDayPlans()
    await fetchMonthStats()
  } catch (err) {
    console.error('获取月度计划失败:', err)
  }
}

async function fetchDayPlans() {
  try {
    const res = await getCleaningPlanList({
      pageNum: 1,
      pageSize: 50,
      startDate: selectedDate.value,
      endDate: selectedDate.value
    })
    dayPlans.value = res?.list || res || []
  } catch (err) {
    console.error('获取日计划失败:', err)
    dayPlans.value = []
  }
}

async function fetchMonthStats() {
  try {
    const monthStart = `${currentYear.value}-${padZero(currentMonth.value)}-01`
    const monthEnd = `${currentYear.value}-${padZero(currentMonth.value)}-${getDaysInMonth(currentYear.value, currentMonth.value)}`
    const res = await getCleaningPlanList({
      pageNum: 1,
      pageSize: 1000,
      startDate: monthStart,
      endDate: monthEnd
    })
    const list = res?.list || res || []
    let pending = 0, progress = 0, done = 0
    let improvedTotal = 0
    list.forEach(p => {
      if (p.status === 0) pending++
      else if (p.status === 1) progress++
      else if (p.status === 2) {
        done++
        improvedTotal += Number(p.improvedEnergy || 0)
      }
    })
    stats.value = {
      pending,
      progress,
      done,
      improved: improvedTotal >= 10000 ? (improvedTotal / 10000).toFixed(1) + '万' : improvedTotal.toFixed(0)
    }
  } catch (err) {
    console.error('获取月度统计失败:', err)
  }
}

function createPlanForDate() {
  uni.navigateTo({
    url: `/pages/cleaning/plan-edit?date=${selectedDate.value}`
  })
}

function goToCreate() {
  uni.navigateTo({ url: '/pages/cleaning/plan-edit' })
}

function goToDetail(id) {
  uni.navigateTo({ url: `/pages/cleaning/plan-detail?id=${id}` })
}

async function startPlan(plan) {
  uni.showModal({
    title: '开始执行',
    content: `确定开始执行「${plan.title}」吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          const { operatorId, operatorName } = getCurrentUser()
          await startCleaningPlan({
            planId: plan.id,
            operatorId,
            operatorName
          })
          uni.showToast({ title: '已开始', icon: 'success' })
          fetchMonthPlans()
        } catch (err) {
          uni.showToast({ title: '操作失败', icon: 'none' })
        }
      }
    }
  })
}

async function completePlan(plan) {
  uni.navigateTo({
    url: `/pages/cleaning/plan-detail?id=${plan.id}&action=complete`
  })
}

onShow(() => {
  fetchMonthPlans()
})

onMounted(() => {
  fetchMonthPlans()
})
</script>

<style lang="scss" scoped>
.calendar-page {
  min-height: 100vh;
  background-color: #f5f6fa;
  padding-bottom: 140rpx;
}

.calendar-container {
  background-color: #fff;
  margin: 20rpx;
  border-radius: 20rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0 28rpx;

  .month-nav {
    width: 64rpx;
    height: 64rpx;
    line-height: 60rpx;
    text-align: center;
    font-size: 48rpx;
    color: #1890ff;
    font-weight: 300;
  }

  .month-title {
    font-size: 34rpx;
    font-weight: 600;
    color: #262626;
  }
}

.weekday-row {
  display: flex;
  margin-bottom: 12rpx;

  .weekday-item {
    flex: 1;
    text-align: center;
    font-size: 24rpx;
    color: #8c8c8c;
    padding: 12rpx 0;
  }
}

.days-grid {
  display: flex;
  flex-wrap: wrap;
}

.day-cell {
  width: calc(100% / 7);
  aspect-ratio: 1;
  box-sizing: border-box;
  padding: 8rpx;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;

  &.other-month {
    .day-number {
      color: #d9d9d9;
    }
  }

  &.today {
    .day-number {
      background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
      color: #fff;
      font-weight: 600;
    }
  }

  &.selected {
    background-color: #e6f7ff;
    border-radius: 12rpx;

    .day-number {
      border: 2rpx solid #1890ff;
    }
  }

  &.has-plan {
    .day-number {
      font-weight: 600;
    }
  }
}

.day-number {
  width: 56rpx;
  height: 56rpx;
  line-height: 52rpx;
  text-align: center;
  font-size: 28rpx;
  color: #262626;
  border-radius: 50%;
  box-sizing: border-box;
}

.day-dots {
  display: flex;
  gap: 6rpx;
  margin-top: 6rpx;

  .plan-dot {
    width: 10rpx;
    height: 10rpx;
    border-radius: 50%;

    &.dot-0 { background-color: #1890ff; }
    &.dot-1 { background-color: #52c41a; }
    &.dot-2 { background-color: #fa8c16; }
  }
}

.plan-count-badge {
  position: absolute;
  top: 4rpx;
  right: 8rpx;
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  padding: 0 8rpx;
  background-color: #ff4d4f;
  color: #fff;
  font-size: 18rpx;
  border-radius: 16rpx;
  text-align: center;
}

.selected-date-info {
  margin: 20rpx;

  .info-header {
    display: flex;
    align-items: center;
    margin-bottom: 20rpx;
    padding: 0 10rpx;
  }

  .info-date {
    font-size: 32rpx;
    font-weight: 600;
    color: #262626;
  }

  .info-count {
    margin-left: 20rpx;
    font-size: 24rpx;
    color: #1890ff;
    background-color: #e6f7ff;
    padding: 6rpx 16rpx;
    border-radius: 20rpx;
  }

  .add-btn {
    margin-left: auto;
    padding: 14rpx 28rpx;
    background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
    color: #fff;
    font-size: 26rpx;
    border-radius: 30rpx;
  }
}

.day-plan-list {
  .day-plan-card {
    background-color: #fff;
    border-radius: 16rpx;
    margin-bottom: 16rpx;
    display: flex;
    overflow: hidden;
    box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
  }

  .plan-status-bar {
    width: 8rpx;
    flex-shrink: 0;

    &.status-pending { background-color: #fa8c16; }
    &.status-progress { background-color: #1890ff; }
    &.status-done { background-color: #52c41a; }
    &.status-cancel { background-color: #bfbfbf; }
  }

  .plan-main {
    flex: 1;
    padding: 24rpx;
    min-width: 0;
  }

  .plan-head {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 16rpx;
  }

  .plan-title {
    flex: 1;
    font-size: 30rpx;
    font-weight: 600;
    color: #262626;
    line-height: 1.4;
    padding-right: 16rpx;
  }

  .plan-status {
    padding: 6rpx 16rpx;
    border-radius: 6rpx;
    font-size: 22rpx;
    flex-shrink: 0;

    &.status-tag-0 { background-color: #fff7e6; color: #fa8c16; }
    &.status-tag-1 { background-color: #e6f7ff; color: #1890ff; }
    &.status-tag-2 { background-color: #f6ffed; color: #52c41a; }
    &.status-tag-3 { background-color: #f5f5f5; color: #8c8c8c; }
  }

  .plan-info-row {
    display: flex;
    align-items: center;
    margin-bottom: 10rpx;

    .info-label {
      font-size: 24rpx;
      margin-right: 12rpx;
    }

    .info-value {
      font-size: 24rpx;
      color: #666;
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .plan-improvement {
    margin-top: 16rpx;
    padding-top: 16rpx;
    border-top: 1rpx dashed #f0f0f0;
    display: flex;
    align-items: center;
    gap: 12rpx;

    .improvement-text {
      font-size: 26rpx;
      color: #52c41a;
      font-weight: 500;
    }

    .improvement-rate {
      padding: 4rpx 12rpx;
      background-color: #f6ffed;
      color: #389e0d;
      border-radius: 8rpx;
      font-size: 22rpx;
    }
  }

  .plan-actions {
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding: 16rpx;
    gap: 16rpx;

    .action-btn {
      padding: 12rpx 24rpx;
      border-radius: 10rpx;
      font-size: 24rpx;
      text-align: center;

      &.primary {
        background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
        color: #fff;
      }

      &.success {
        background: linear-gradient(135deg, #52c41a 0%, #95de64 100%);
        color: #fff;
      }
    }
  }
}

.empty-day {
  background-color: #fff;
  border-radius: 16rpx;
  padding: 80rpx 0;
  text-align: center;

  .empty-icon {
    display: block;
    font-size: 80rpx;
    margin-bottom: 20rpx;
  }

  .empty-text {
    display: block;
    font-size: 28rpx;
    color: #8c8c8c;
    margin-bottom: 20rpx;
  }

  .empty-tips {
    font-size: 24rpx;
    color: #bbb;
  }
}

.stats-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #fff;
  display: flex;
  padding: 20rpx 0;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.05);

  .stats-item {
    flex: 1;
    text-align: center;
    border-right: 1rpx solid #f0f0f0;

    &:last-child {
      border-right: none;
    }
  }

  .stats-num {
    font-size: 36rpx;
    font-weight: 700;
    margin-bottom: 6rpx;

    &.orange { color: #fa8c16; }
    &.blue { color: #1890ff; }
    &.green { color: #52c41a; }
    &.cyan { color: #13c2c2; }
  }

  .stats-label {
    font-size: 22rpx;
    color: #999;
  }
}

.float-create-btn {
  position: fixed;
  right: 40rpx;
  bottom: 180rpx;
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
  box-shadow: 0 8rpx 24rpx rgba(24, 144, 255, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;

  .plus-icon {
    color: #fff;
    font-size: 60rpx;
    font-weight: 300;
    line-height: 1;
    margin-top: -6rpx;
  }
}
</style>
