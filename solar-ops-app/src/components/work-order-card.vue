<template>
  <view class="work-order-card" @click="handleClick">
    <view class="work-order-card__header">
      <view class="work-order-card__title">
        <text class="order-no">{{ data.orderNo || '工单编号' }}</text>
        <status-tag :text="statusText" :type="statusType" />
      </view>
      <view class="work-order-card__fault">{{ data.faultName || '故障描述' }}</view>
    </view>
    
    <view class="work-order-card__body">
      <view class="info-row">
        <text class="info-label">电站：</text>
        <text class="info-value">{{ data.stationName || '--' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">设备：</text>
        <text class="info-value">{{ data.deviceName || '--' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">等级：</text>
        <text class="info-value" :class="`level--${data.level}`">{{ levelText }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">创建时间：</text>
        <text class="info-value">{{ formatCreateTime }}</text>
      </view>
    </view>
    
    <view class="work-order-card__footer" v-if="showActions">
      <view class="action-btns">
        <view 
          v-if="data.status === 0" 
          class="btn btn--primary"
          @click.stop="handleGrab"
        >
          抢单
        </view>
        <view 
          v-if="data.status === 0 || data.status === 1" 
          class="btn btn--default"
          @click.stop="handleDetail"
        >
          查看详情
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { formatDateTime } from '@/utils/format'
import StatusTag from './status-tag.vue'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  },
  showActions: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['click', 'grab', 'accept', 'detail'])

const statusMap = {
  0: '待接单',
  1: '处理中',
  2: '待验收',
  3: '已完成',
  4: '已取消'
}

const statusTypeMap = {
  0: 'warning',
  1: 'primary',
  2: 'processing',
  3: 'success',
  4: 'default'
}

const levelMap = {
  1: '一般',
  2: '重要',
  3: '紧急'
}

const statusText = computed(() => statusMap[props.data.status] || '未知')
const statusType = computed(() => statusTypeMap[props.data.status] || 'default')
const levelText = computed(() => levelMap[props.data.level] || '一般')

const formatCreateTime = computed(() => {
  if (props.data.createTime) {
    return formatDateTime(props.data.createTime)
  }
  return '--'
})

function handleClick() {
  emit('click', props.data)
}

function handleGrab() {
  emit('grab', props.data)
}

function handleDetail() {
  emit('detail', props.data)
}
</script>

<style lang="scss" scoped>
.work-order-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
  
  &__header {
    margin-bottom: 20rpx;
    padding-bottom: 20rpx;
    border-bottom: 1rpx solid #f0f0f0;
  }
  
  &__title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12rpx;
  }
  
  .order-no {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
  }
  
  &__fault {
    font-size: 26rpx;
    color: #666;
    line-height: 1.4;
  }
  
  &__body {
    margin-bottom: 20rpx;
  }
  
  .info-row {
    display: flex;
    align-items: center;
    margin-bottom: 12rpx;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
  
  .info-label {
    font-size: 26rpx;
    color: #999;
    width: 140rpx;
    flex-shrink: 0;
  }
  
  .info-value {
    font-size: 26rpx;
    color: #333;
    flex: 1;
  }
  
  .level--1 {
    color: #52c41a;
  }
  
  .level--2 {
    color: #faad14;
  }
  
  .level--3 {
    color: #ff4d4f;
  }
  
  &__footer {
    padding-top: 20rpx;
    border-top: 1rpx solid #f0f0f0;
  }
  
  .action-btns {
    display: flex;
    justify-content: flex-end;
    gap: 20rpx;
  }
  
  .btn {
    padding: 12rpx 32rpx;
    font-size: 26rpx;
    border-radius: 8rpx;
    line-height: 1.4;
    
    &--primary {
      background-color: #1890ff;
      color: #ffffff;
    }
    
    &--default {
      background-color: #f5f5f5;
      color: #666;
    }
  }
}
</style>
