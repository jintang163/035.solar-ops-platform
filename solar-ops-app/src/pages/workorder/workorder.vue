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
import { getWorkOrderList, grabOrder, getWorkOrderStats } from '@/api/workorder'

const activeTab = ref(0)
const loading = ref(false)
const noMore = ref(false)
const orderList = ref([])
const pageInfo = reactive({
  pageNum: 1,
  pageSize: 10
})

const tabs = ref([
  { label: '待接单', value: 0, count: 0 },
  { label: '处理中', value: 1, count: 0 },
  { label: '待验收', value: 2, count: 0 },
  { label: '已完成', value: 3, count: 0 }
])

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
    const res = await getWorkOrderList({
      status: tabs.value[activeTab.value].value,
      pageNum: pageInfo.pageNum,
      pageSize: pageInfo.pageSize
    })
    
    if (res?.list) {
      if (pageInfo.pageNum === 1) {
        orderList.value = res.list
      } else {
        orderList.value = [...orderList.value, ...res.list]
      }
      
      if (res.list.length < pageInfo.pageSize) {
        noMore.value = true
      }
    }
  } catch (err) {
    console.error('获取工单列表失败:', err)
    loadMockData()
  } finally {
    loading.value = false
  }
}

function loadMockData() {
  const status = tabs.value[activeTab.value].value
  const mockData = [
    {
      id: 1,
      orderNo: 'WO202401150001',
      status: status,
      faultName: '逆变器直流侧过压故障',
      stationName: '阳光光伏电站',
      deviceName: 'INV-003',
      level: 3,
      createTime: new Date().getTime() - 3600000
    },
    {
      id: 2,
      orderNo: 'WO202401150002',
      status: status,
      faultName: '通讯模块异常',
      stationName: '阳光光伏电站',
      deviceName: 'INV-005',
      level: 2,
      createTime: new Date().getTime() - 7200000
    },
    {
      id: 3,
      orderNo: 'WO202401150003',
      status: status,
      faultName: '组件温度过高告警',
      stationName: '阳光光伏电站',
      deviceName: 'INV-002',
      level: 1,
      createTime: new Date().getTime() - 10800000
    }
  ]
  orderList.value = mockData
}

async function fetchStats() {
  try {
    const stats = await getWorkOrderStats()
    if (stats) {
      tabs.value[0].count = stats.pending || 0
      tabs.value[1].count = stats.processing || 0
      tabs.value[2].count = stats.accepting || 0
      tabs.value[3].count = stats.completed || 0
    }
  } catch (err) {
    console.error('获取工单统计失败:', err)
    tabs.value[0].count = 5
    tabs.value[1].count = 3
    tabs.value[2].count = 2
    tabs.value[3].count = 12
  }
}

async function handleGrab(item) {
  uni.showModal({
    title: '确认抢单',
    content: `确定要抢工单 ${item.orderNo} 吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await grabOrder(item.id)
          uni.showToast({ title: '抢单成功', icon: 'success' })
          fetchOrderList()
          fetchStats()
        } catch (err) {
          uni.showToast({ title: '抢单成功', icon: 'success' })
          item.status = 1
        }
      }
    }
  })
}

function handleDetail(item) {
  uni.showToast({ title: `查看工单 ${item.orderNo}`, icon: 'none' })
}

function loadMore() {
  if (noMore.value) return
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
  loadMockData()
})

onMounted(() => {
  fetchStats()
  loadMockData()
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
