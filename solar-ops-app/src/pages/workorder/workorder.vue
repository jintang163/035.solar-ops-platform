<template>
  <view class="workorder-page">
    <view class="tabs">
      <view 
        v-for="(tab, index) in tabs" 
        :key="index"
        class="tab-item"
        :class="{ active: activeTab === index }"
        @click="switchTab(index)"
      >
        <text class="tab-text">{{ tab.label }}</text>
        <view v-if="tab.count > 0" class="tab-badge">{{ tab.count }}</view>
      </view>
    </view>
    
    <view class="order-list" v-if="orderList.length > 0">
      <work-order-card 
        v-for="item in orderList" 
        :key="item.id" 
        :data="item"
        @grab="handleGrab"
        @click="handleDetail"
      />
    </view>
    
    <view class="empty-state" v-else>
      <text class="empty-icon">📋</text>
      <text class="empty-text">暂无工单</text>
    </view>
    
    <view class="load-more" v-if="orderList.length > 0">
      <text v-if="loading">加载中...</text>
      <text v-else-if="noMore">没有更多了</text>
      <text v-else @click="loadMore">加载更多</text>
    </view>
    
    <view class="bottom-space"></view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { onReachBottom } from '@dcloudio/uni-app'
import WorkOrderCard from '@/components/work-order-card.vue'
import { getWorkOrderPage, getWorkOrderStatistics, acceptWorkOrder, getWorkOrderDetail } from '@/api/workorder'

const activeTab = ref(0)
const loading = ref(false)
const noMore = ref(false)
const orderList = ref([])
const pageInfo = reactive({
  pageNum: 1,
  pageSize: 10
})

const tabs = ref([
  { label: '待接单', statuses: [0], count: 0 },
  { label: '处理中', statuses: [1, 2], count: 0 },
  { label: '待验收', statuses: [3], count: 0 },
  { label: '已完成', statuses: [4], count: 0 }
])

function mapOrderItem(item) {
  return {
    ...item,
    deviceName: item.inverterName || item.deviceName || '--',
    level: item.faultLevel ?? item.level ?? 1
  }
}

function switchTab(index) {
  activeTab.value = index
  pageInfo.pageNum = 1
  orderList.value = []
  noMore.value = false
  fetchOrderList()
}

async function fetchOrderList() {
  if (loading.value) return

  loading.value = true
  try {
    const tab = tabs.value[activeTab.value]
    const statuses = tab.statuses

    if (statuses.length === 1) {
      const res = await getWorkOrderPage({
        status: statuses[0],
        pageNum: pageInfo.pageNum,
        pageSize: pageInfo.pageSize
      })
      handleListResponse(res)
    } else {
      const requests = statuses.map(status =>
        getWorkOrderPage({
          status,
          pageNum: pageInfo.pageNum,
          pageSize: pageInfo.pageSize
        })
      )
      const results = await Promise.all(requests)
      const mergedList = results
        .filter(Boolean)
        .flatMap(res => res.list || [])
        .map(mapOrderItem)
        .sort((a, b) => b.createTime - a.createTime)
        .slice(0, pageInfo.pageSize)

      if (pageInfo.pageNum === 1) {
        orderList.value = mergedList
      } else {
        orderList.value = [...orderList.value, ...mergedList]
      }

      const totalFetched = results.reduce((sum, res) => sum + (res?.list?.length || 0), 0)
      if (totalFetched < pageInfo.pageSize) {
        noMore.value = true
      }
    }
  } catch (err) {
    console.error('获取工单列表失败:', err)
    if (pageInfo.pageNum === 1) {
      orderList.value = []
    }
  } finally {
    loading.value = false
  }
}

function handleListResponse(res) {
  if (res?.list) {
    const mapped = res.list.map(mapOrderItem)
    if (pageInfo.pageNum === 1) {
      orderList.value = mapped
    } else {
      orderList.value = [...orderList.value, ...mapped]
    }
    if (res.list.length < pageInfo.pageSize) {
      noMore.value = true
    }
  } else {
    if (pageInfo.pageNum === 1) {
      orderList.value = []
    }
    noMore.value = true
  }
}

async function fetchStats() {
  try {
    const stats = await getWorkOrderStatistics()
    if (stats) {
      tabs.value[0].count = stats.pending || 0
      tabs.value[1].count = (stats.accepted || 0) + (stats.processing || 0)
      tabs.value[2].count = stats.checking || 0
      tabs.value[3].count = stats.completed || 0
    }
  } catch (err) {
    console.error('获取工单统计失败:', err)
  }
}

function getCurrentUser() {
  try {
    const userInfo = uni.getStorageSync('userInfo')
    if (userInfo) {
      const parsed = typeof userInfo === 'string' ? JSON.parse(userInfo) : userInfo
      return {
        operatorId: parsed.id || parsed.userId || '',
        operatorName: parsed.name || parsed.userName || ''
      }
    }
  } catch (e) {
    console.error('获取用户信息失败:', e)
  }
  return { operatorId: '', operatorName: '' }
}

async function handleGrab(item) {
  uni.showModal({
    title: '确认接单',
    content: `确定要接单 ${item.orderNo} 吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          const { operatorId, operatorName } = getCurrentUser()
          await acceptWorkOrder({
            orderId: item.id,
            operatorId,
            operatorName
          })
          uni.showToast({ title: '接单成功', icon: 'success' })
          fetchOrderList()
          fetchStats()
        } catch (err) {
          console.error('接单失败:', err)
          uni.showToast({ title: '接单失败，请重试', icon: 'none' })
        }
      }
    }
  })
}

async function handleDetail(item) {
  try {
    await getWorkOrderDetail(item.id)
    uni.navigateTo({
      url: `/pages/workorder/detail?id=${item.id}`
    })
  } catch (err) {
    console.error('获取工单详情失败:', err)
    uni.navigateTo({
      url: `/pages/workorder/detail?id=${item.id}`
    })
  }
}

function loadMore() {
  if (noMore.value || loading.value) return
  pageInfo.pageNum++
  fetchOrderList()
}

onReachBottom(() => {
  loadMore()
})

watch(activeTab, () => {
  pageInfo.pageNum = 1
  orderList.value = []
  noMore.value = false
  fetchOrderList()
})

onMounted(() => {
  fetchStats()
  fetchOrderList()
})
</script>

<style lang="scss" scoped>
.workorder-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.tabs {
  display: flex;
  background-color: #ffffff;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 28rpx 0;
  position: relative;
  
  &.active {
    .tab-text {
      color: #1890ff;
      font-weight: 600;
    }
    
    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 60rpx;
      height: 6rpx;
      background-color: #1890ff;
      border-radius: 3rpx;
    }
  }
}

.tab-text {
  font-size: 28rpx;
  color: #666;
}

.tab-badge {
  display: inline-block;
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  padding: 0 10rpx;
  background-color: #ff4d4f;
  color: #ffffff;
  font-size: 20rpx;
  border-radius: 16rpx;
  margin-left: 8rpx;
  vertical-align: middle;
}

.order-list {
  padding: 20rpx 30rpx;
}

.empty-state {
  text-align: center;
  padding: 120rpx 0;
  
  .empty-icon {
    font-size: 100rpx;
    display: block;
    margin-bottom: 24rpx;
  }
  
  .empty-text {
    font-size: 28rpx;
    color: #999;
  }
}

.load-more {
  text-align: center;
  padding: 30rpx;
  font-size: 26rpx;
  color: #999;
}

.bottom-space {
  height: 40rpx;
}
</style>
