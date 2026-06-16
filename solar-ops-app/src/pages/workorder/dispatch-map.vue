<template>
  <view class="dispatch-page">
    <view class="nav-bar">
      <view class="nav-back" @click="goBack">
        <text class="back-icon">‹</text>
      </view>
      <text class="nav-title">智能调度</text>
      <view class="nav-action" @click="refreshData">
        <text class="action-icon">🔄</text>
      </view>
    </view>

    <view class="map-container">
      <map
        id="dispatchMap"
        class="dispatch-map"
        :latitude="mapCenter.lat"
        :longitude="mapCenter.lng"
        :scale="mapScale"
        :markers="markers"
        :show-location="true"
        :enable-3D="false"
        :show-compass="true"
        :enable-overlooking="false"
        :enable-zoom="true"
        :enable-scroll="true"
        :enable-rotate="false"
        @markertap="onMarkerTap"
        @regionchange="onRegionChange"
      ></map>

      <view class="map-toolbar">
        <view class="tool-btn" @click="locateMe">
          <text class="tool-icon">📍</text>
        </view>
        <view class="tool-btn" @click="zoomIn">
          <text class="tool-icon">+</text>
        </view>
        <view class="tool-btn" @click="zoomOut">
          <text class="tool-icon">−</text>
        </view>
      </view>

      <view class="stats-bar">
        <view class="stat-item">
          <text class="stat-value">{{ operatorList.length }}</text>
          <text class="stat-label">在岗人员</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-value">{{ pendingCount }}</text>
          <text class="stat-label">待派工单</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-value">{{ selectedOrder ? 1 : 0 }}</text>
          <text class="stat-label">选中工单</text>
        </view>
      </view>
    </view>

    <view class="bottom-panel" :class="{ expanded: showRecommend }">
      <view class="panel-header" @click="togglePanel">
        <view class="panel-title">
          <text class="title-text">{{ selectedOrder ? '推荐接单人员' : '在岗人员列表' }}</text>
          <text class="panel-hint">{{ showRecommend ? '收起' : '展开' }}</text>
        </view>
        <view class="panel-arrow" :class="{ up: showRecommend }">
          <text>▾</text>
        </view>
      </view>

      <view class="order-selector" v-if="!selectedOrder">
        <text class="selector-label">选择待派工单：</text>
        <scroll-view class="order-scroll" scroll-x>
          <view class="order-tags">
            <view 
              v-for="order in pendingOrders" 
              :key="order.id"
              class="order-tag"
              :class="{ active: selectedOrder?.id === order.id }"
              @click="selectOrder(order)"
            >
              <text class="tag-title">{{ order.orderNo }}</text>
              <text class="tag-level" :class="'level-' + order.faultLevel">
                {{ getLevelText(order.faultLevel) }}
              </text>
            </view>
          </view>
        </scroll-view>
      </view>

      <view class="selected-order" v-if="selectedOrder">
        <view class="order-header">
          <view class="order-badge" :class="'level-' + selectedOrder.faultLevel">
            {{ getLevelText(selectedOrder.faultLevel) }}
          </view>
          <text class="order-no">{{ selectedOrder.orderNo }}</text>
          <view class="clear-btn" @click="clearOrder">
            <text>×</text>
          </view>
        </view>
        <text class="order-fault">{{ selectedOrder.faultName || selectedOrder.faultCode }}</text>
        <text class="order-desc">{{ selectedOrder.description || '暂无描述' }}</text>
      </view>

      <view class="recommend-list">
        <view 
          v-for="(op, index) in displayOperators" 
          :key="op.userId"
          class="operator-card"
          :class="{ selected: selectedOperator?.userId === op.userId, top3: index < 3 }"
          @click="selectOperator(op)"
        >
          <view class="op-rank" v-if="selectedOrder && index < 3">
            <text class="rank-text">{{ index + 1 }}</text>
          </view>

          <view class="op-avatar">
            <text class="avatar-icon">👤</text>
          </view>

          <view class="op-info">
            <view class="op-header">
              <text class="op-name">{{ op.userName }}</text>
              <view class="op-level" v-if="selectedOrder" :class="getLevelClass(op.totalScore)">
                {{ op.recommendLevel }}
              </view>
            </view>

            <view class="op-tags" v-if="op.skillTags && op.skillTags.length > 0">
              <text class="skill-tag" v-for="(tag, i) in op.skillTags.slice(0, 3)" :key="i">
                {{ tag }}
              </text>
            </view>

            <view class="op-meta">
              <text class="meta-item" v-if="op.distanceKm !== undefined">
                📍 {{ formatDistance(op.distanceKm) }}
              </text>
              <text class="meta-item" v-if="op.etaMinutes !== undefined">
                ⏱️ {{ op.etaMinutes }}分钟
              </text>
              <text class="meta-item">
                📋 {{ op.activeTaskCount || 0 }}个进行中
              </text>
            </view>
          </view>

          <view class="op-score" v-if="selectedOrder">
            <text class="score-value">{{ op.totalScore }}</text>
            <text class="score-label">推荐分</text>
          </view>
        </view>

        <view class="empty-list" v-if="displayOperators.length === 0">
          <text class="empty-icon">👥</text>
          <text class="empty-text">{{ selectedOrder ? '附近没有可用的运维人员' : '暂无在岗人员' }}</text>
        </view>
      </view>

      <view class="action-bar" v-if="selectedOrder && selectedOperator">
        <view class="action-btn secondary" @click="callOperator">
          <text class="btn-icon">📞</text>
          <text class="btn-text">联系</text>
        </view>
        <view class="action-btn primary" @click="handleAssign">
          <text class="btn-icon">✅</text>
          <text class="btn-text">确认派单</text>
        </view>
      </view>

      <view class="action-bar" v-if="selectedOrder && !selectedOperator">
        <view class="action-btn auto" @click="handleAutoAssign">
          <text class="btn-icon">🤖</text>
          <text class="btn-text">智能自动派单</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  getRecommendOperators,
  getAllOperatorLocations,
  assignDispatchOrder,
  autoAssignOrder,
  getWorkOrderPage
} from '@/api/workorder'

const mapCenter = reactive({ lat: 39.90923, lng: 116.397428 })
const mapScale = ref(14)
const operatorList = ref([])
const pendingOrders = ref([])
const pendingCount = ref(0)
const selectedOrder = ref(null)
const selectedOperator = ref(null)
const showRecommend = ref(true)
const loading = ref(false)

const displayOperators = computed(() => {
  if (selectedOrder.value && recommendedList.value.length > 0) {
    return recommendedList.value
  }
  return operatorList.value
})

const recommendedList = ref([])

const markers = computed(() => {
  const list = []

  if (selectedOrder.value && selectedOrder.value.latitude && selectedOrder.value.longitude) {
    list.push({
      id: -999,
      latitude: Number(selectedOrder.value.latitude),
      longitude: Number(selectedOrder.value.longitude),
      iconPath: '',
      width: 40,
      height: 40,
      callout: {
        content: selectedOrder.value.faultName || '故障工单',
        color: '#fff',
        fontSize: 12,
        borderRadius: 4,
        bgColor: '#ff4d4f',
        padding: 6,
        display: 'ALWAYS'
      }
    })
  }

  operatorList.value.forEach((op, index) => {
    if (op.latitude && op.longitude) {
      const isSelected = selectedOperator.value?.userId === op.userId
      list.push({
        id: Number(op.userId) || index + 1,
        latitude: Number(op.latitude),
        longitude: Number(op.longitude),
        iconPath: '',
        width: isSelected ? 44 : 36,
        height: isSelected ? 44 : 36,
        callout: {
          content: op.userName || '运维人员',
          color: '#333',
          fontSize: 11,
          borderRadius: 6,
          bgColor: isSelected ? '#1890ff' : '#fff',
          color: isSelected ? '#fff' : '#333',
          padding: 5,
          display: 'ALWAYS',
          textAlign: 'center'
        }
      })
    }
  })

  return list
})

function getLevelText(level) {
  const map = { 1: '低级', 2: '中级', 3: '高级', 4: '紧急' }
  return map[level] || '未知'
}

function getLevelClass(score) {
  if (score >= 85) return 'level-best'
  if (score >= 70) return 'level-good'
  if (score >= 55) return 'level-normal'
  return 'level-low'
}

function formatDistance(km) {
  if (km < 1) {
    return Math.round(km * 1000) + 'm'
  }
  return km.toFixed(1) + 'km'
}

function goBack() {
  uni.navigateBack()
}

function togglePanel() {
  showRecommend.value = !showRecommend.value
}

async function refreshData() {
  loading.value = true
  try {
    await Promise.all([
      loadOperators(),
      loadPendingOrders()
    ])
    uni.showToast({ title: '刷新成功', icon: 'success' })
  } catch (e) {
    console.error('刷新失败:', e)
    uni.showToast({ title: '刷新失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function loadOperators() {
  try {
    const res = await getAllOperatorLocations()
    if (res && res.list) {
      operatorList.value = res.list
    } else if (Array.isArray(res)) {
      operatorList.value = res
    }
    if (operatorList.value.length > 0) {
      mapCenter.lat = Number(operatorList.value[0].latitude) || mapCenter.lat
      mapCenter.lng = Number(operatorList.value[0].longitude) || mapCenter.lng
    }
  } catch (e) {
    console.error('加载人员位置失败:', e)
  }
}

async function loadPendingOrders() {
  try {
    const res = await getWorkOrderPage({
      status: 0,
      pageNum: 1,
      pageSize: 20
    })
    if (res?.list) {
      pendingOrders.value = res.list
      pendingCount.value = res.total || res.list.length
    }
  } catch (e) {
    console.error('加载待派工单失败:', e)
  }
}

function selectOrder(order) {
  selectedOrder.value = order
  selectedOperator.value = null

  if (order.latitude && order.longitude) {
    mapCenter.lat = Number(order.latitude)
    mapCenter.lng = Number(order.longitude)
    mapScale.value = 15
  }

  loadRecommendations()
}

function clearOrder() {
  selectedOrder.value = null
  selectedOperator.value = null
  recommendedList.value = []
}

async function loadRecommendations() {
  if (!selectedOrder.value) return

  try {
    const lng = selectedOrder.value.longitude
    const lat = selectedOrder.value.latitude

    if (!lng || !lat) {
      console.warn('工单没有经纬度信息')
      return
    }

    const res = await getRecommendOperators({
      stationId: selectedOrder.value.stationId,
      stationLng: lng,
      stationLat: lat,
      requiredSkill: selectedOrder.value.faultName,
      faultLevel: selectedOrder.value.faultLevel
    })

    if (Array.isArray(res)) {
      recommendedList.value = res
    } else if (res?.list) {
      recommendedList.value = res.list
    }
  } catch (e) {
    console.error('加载推荐人员失败:', e)
  }
}

function selectOperator(op) {
  selectedOperator.value = op
  if (op.latitude && op.longitude) {
    mapCenter.lat = Number(op.latitude)
    mapCenter.lng = Number(op.longitude)
  }
}

function onMarkerTap(e) {
  const markerId = e.markerId
  if (markerId === -999) return

  const op = operatorList.value.find(o => Number(o.userId) === markerId || Number(o.id) === markerId)
  if (op) {
    selectOperator(op)
  }
}

function onRegionChange(e) {
}

function locateMe() {
  uni.getLocation({
    type: 'gcj02',
    success: (res) => {
      mapCenter.lat = res.latitude
      mapCenter.lng = res.longitude
      mapScale.value = 16
    },
    fail: () => {
      uni.showToast({ title: '定位失败', icon: 'none' })
    }
  })
}

function zoomIn() {
  mapScale.value = Math.min(18, mapScale.value + 1)
}

function zoomOut() {
  mapScale.value = Math.max(3, mapScale.value - 1)
}

function callOperator() {
  if (!selectedOperator.value?.phone) {
    uni.showToast({ title: '暂无联系电话', icon: 'none' })
    return
  }
  uni.makePhoneCall({
    phoneNumber: selectedOperator.value.phone,
    fail: () => {
      uni.showToast({ title: '拨号失败', icon: 'none' })
    }
  })
}

async function handleAssign() {
  if (!selectedOrder.value || !selectedOperator.value) return

  uni.showModal({
    title: '确认派单',
    content: `确定将工单 ${selectedOrder.value.orderNo} 派给 ${selectedOperator.value.userName} 吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          const userInfo = uni.getStorageSync('userInfo')
          const user = userInfo ? (typeof userInfo === 'string' ? JSON.parse(userInfo) : userInfo) : {}

          await assignDispatchOrder({
            orderId: selectedOrder.value.id,
            handlerId: selectedOperator.value.userId,
            handlerName: selectedOperator.value.userName,
            operatorId: user.id || user.userId,
            operatorName: user.name || user.nickname || user.userName || '管理员'
          })

          uni.showToast({ title: '派单成功', icon: 'success' })

          selectedOrder.value = null
          selectedOperator.value = null
          recommendedList.value = []
          loadPendingOrders()
          loadOperators()
        } catch (e) {
          console.error('派单失败:', e)
          uni.showToast({ title: '派单失败，请重试', icon: 'none' })
        }
      }
    }
  })
}

async function handleAutoAssign() {
  if (!selectedOrder.value) return

  uni.showModal({
    title: '智能派单',
    content: '系统将自动选择最优运维人员派单，是否继续？',
    success: async (res) => {
      if (res.confirm) {
        try {
          const userInfo = uni.getStorageSync('userInfo')
          const user = userInfo ? (typeof userInfo === 'string' ? JSON.parse(userInfo) : userInfo) : {}

          const result = await autoAssignOrder({
            orderId: selectedOrder.value.id,
            stationId: selectedOrder.value.stationId,
            stationLng: selectedOrder.value.longitude,
            stationLat: selectedOrder.value.latitude,
            requiredSkill: selectedOrder.value.faultName,
            faultLevel: selectedOrder.value.faultLevel,
            operatorId: user.id || user.userId,
            operatorName: user.name || user.nickname || user.userName || '管理员'
          })

          uni.showToast({ title: '智能派单成功', icon: 'success' })

          selectedOrder.value = null
          selectedOperator.value = null
          recommendedList.value = []
          loadPendingOrders()
          loadOperators()
        } catch (e) {
          console.error('智能派单失败:', e)
          uni.showToast({ title: '派单失败，请重试', icon: 'none' })
        }
      }
    }
  })
}

onLoad((options) => {
  if (options.orderId) {
    loadOrderDetail(options.orderId)
  }
})

async function loadOrderDetail(orderId) {
  try {
    const res = await getWorkOrderPage({
      id: orderId,
      pageNum: 1,
      pageSize: 1
    })
    if (res?.list && res.list.length > 0) {
      selectOrder(res.list[0])
    }
  } catch (e) {
    console.error('加载工单详情失败:', e)
  }
}

let refreshTimer = null

onMounted(() => {
  loadOperators()
  loadPendingOrders()

  refreshTimer = setInterval(() => {
    loadOperators()
  }, 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>

<style lang="scss" scoped>
.dispatch-page {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  padding: 0 30rpx;
  padding-top: var(--status-bar-height, 44px);
  background: linear-gradient(135deg, #1890ff, #096dd9);
}

.nav-back, .nav-action {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-icon {
  font-size: 48rpx;
  color: #fff;
  font-weight: 300;
}

.action-icon {
  font-size: 36rpx;
}

.nav-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #fff;
}

.map-container {
  flex: 1;
  position: relative;
  background-color: #e8e8e8;
}

.dispatch-map {
  width: 100%;
  height: 100%;
}

.map-toolbar {
  position: absolute;
  right: 24rpx;
  bottom: 340rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  z-index: 10;
}

.tool-btn {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(255, 255, 255, 0.95);
  border-radius: 50%;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.15);
}

.tool-icon {
  font-size: 32rpx;
}

.stats-bar {
  position: absolute;
  top: calc(88rpx + var(--status-bar-height, 44px) + 20rpx);
  left: 30rpx;
  right: 30rpx;
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  background-color: rgba(255, 255, 255, 0.95);
  border-radius: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 36rpx;
  font-weight: 600;
  color: #1890ff;
  margin-bottom: 4rpx;
}

.stat-label {
  font-size: 22rpx;
  color: #666;
}

.stat-divider {
  width: 1rpx;
  height: 48rpx;
  background-color: #f0f0f0;
}

.bottom-panel {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: #fff;
  border-radius: 24rpx 24rpx 0 0;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.1);
  z-index: 20;
  transition: transform 0.3s ease;

  &.expanded {
    transform: translateY(0);
  }

  &:not(.expanded) {
    transform: translateY(calc(100% - 80rpx));
  }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.title-text {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.panel-hint {
  font-size: 24rpx;
  color: #999;
}

.panel-arrow {
  font-size: 24rpx;
  color: #999;
  transition: transform 0.3s ease;

  &.up {
    transform: rotate(180deg);
  }
}

.order-selector {
  padding: 20rpx 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.selector-label {
  font-size: 26rpx;
  color: #666;
  margin-bottom: 16rpx;
  display: block;
}

.order-scroll {
  white-space: nowrap;
}

.order-tags {
  display: inline-flex;
  gap: 16rpx;
}

.order-tag {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  padding: 16rpx 24rpx;
  background-color: #f5f5f5;
  border-radius: 12rpx;
  border: 2rpx solid transparent;

  &.active {
    background-color: #e6f7ff;
    border-color: #1890ff;
  }
}

.tag-title {
  font-size: 24rpx;
  color: #333;
  margin-bottom: 4rpx;
}

.tag-level {
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: 6rpx;

  &.level-1 { background-color: #f6ffed; color: #52c41a; }
  &.level-2 { background-color: #fffbe6; color: #faad14; }
  &.level-3 { background-color: #fff2e8; color: #fa8c16; }
  &.level-4 { background-color: #fff1f0; color: #ff4d4f; }
}

.selected-order {
  padding: 20rpx 30rpx;
  background-color: #fafafa;
  border-bottom: 1rpx solid #f0f0f0;
}

.order-header {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 12rpx;
}

.order-badge {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;

  &.level-1 { background-color: #f6ffed; color: #52c41a; }
  &.level-2 { background-color: #fffbe6; color: #faad14; }
  &.level-3 { background-color: #fff2e8; color: #fa8c16; }
  &.level-4 { background-color: #fff1f0; color: #ff4d4f; }
}

.order-no {
  flex: 1;
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.clear-btn {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  color: #ccc;
}

.order-fault {
  display: block;
  font-size: 26rpx;
  color: #333;
  margin-bottom: 8rpx;
}

.order-desc {
  font-size: 24rpx;
  color: #999;
}

.recommend-list {
  max-height: 500rpx;
  overflow-y: auto;
  padding: 16rpx 30rpx;
}

.operator-card {
  display: flex;
  align-items: center;
  padding: 24rpx;
  background-color: #fafafa;
  border-radius: 16rpx;
  margin-bottom: 16rpx;
  position: relative;
  border: 2rpx solid transparent;

  &.selected {
    background-color: #e6f7ff;
    border-color: #1890ff;
  }

  &.top3 {
    background-color: #fff;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
  }
}

.op-rank {
  position: absolute;
  top: -10rpx;
  left: -10rpx;
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff4d4f, #fa8c16);
  border-radius: 50%;
  color: #fff;
  font-weight: 600;
  font-size: 24rpx;
  z-index: 2;
}

.rank-text {
  color: #fff;
  font-size: 24rpx;
}

.op-avatar {
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1890ff, #52c41a);
  border-radius: 50%;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.avatar-icon {
  font-size: 40rpx;
}

.op-info {
  flex: 1;
  min-width: 0;
}

.op-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 8rpx;
}

.op-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.op-level {
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: 6rpx;

  &.level-best { background-color: #f6ffed; color: #52c41a; }
  &.level-good { background-color: #e6f7ff; color: #1890ff; }
  &.level-normal { background-color: #fffbe6; color: #faad14; }
  &.level-low { background-color: #f5f5f5; color: #999; }
}

.op-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-bottom: 8rpx;
}

.skill-tag {
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  background-color: #f0f5ff;
  color: #597ef7;
  border-radius: 6rpx;
}

.op-meta {
  display: flex;
  gap: 20rpx;
}

.meta-item {
  font-size: 22rpx;
  color: #999;
}

.op-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-left: 16rpx;
  padding-left: 16rpx;
  border-left: 1rpx solid #f0f0f0;
}

.score-value {
  font-size: 40rpx;
  font-weight: 600;
  color: #1890ff;
  line-height: 1.2;
}

.score-label {
  font-size: 20rpx;
  color: #999;
  margin-top: 4rpx;
}

.empty-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 0;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 16rpx;
}

.empty-text {
  font-size: 26rpx;
  color: #999;
}

.action-bar {
  display: flex;
  gap: 20rpx;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #f0f0f0;
  background-color: #fff;
}

.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 24rpx 0;
  border-radius: 50rpx;
  font-size: 28rpx;

  &.secondary {
    background-color: #f5f5f5;
    color: #333;
  }

  &.primary {
    background: linear-gradient(135deg, #1890ff, #096dd9);
    color: #fff;
  }

  &.auto {
    background: linear-gradient(135deg, #722ed1, #1890ff);
    color: #fff;
  }
}

.btn-icon {
  font-size: 32rpx;
}

.btn-text {
  font-size: 28rpx;
  font-weight: 500;
}
</style>
