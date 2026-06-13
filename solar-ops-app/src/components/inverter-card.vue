<template>
  <view class="inverter-card" @click="handleClick">
    <view class="inverter-card__header">
      <view class="inverter-card__title">
        <text class="inverter-card__name">{{ data.name || '逆变器' }}</text>
        <status-tag :type="statusType" :status="data.status" />
      </view>
      <view class="inverter-card__station">{{ data.stationName || '光伏电站' }}</view>
    </view>
    
    <view class="inverter-card__body">
      <view class="inverter-card__item">
        <view class="item-label">当前功率</view>
        <view class="item-value">{{ data.power !== undefined ? data.power.toFixed(2) : '0.00' }} kW</view>
      </view>
      <view class="inverter-card__item">
        <view class="item-label">今日发电</view>
        <view class="item-value">{{ data.dailyEnergy !== undefined ? data.dailyEnergy.toFixed(2) : '0.00' }} kWh</view>
      </view>
      <view class="inverter-card__item">
        <view class="item-label">累计发电</view>
        <view class="item-value">{{ data.totalEnergy !== undefined ? data.totalEnergy.toFixed(2) : '0.00' }} kWh</view>
      </view>
      <view class="inverter-card__item">
        <view class="item-label">直流电压</view>
        <view class="item-value">{{ data.dcVoltage !== undefined ? data.dcVoltage.toFixed(1) : '0.0' }} V</view>
      </view>
    </view>
    
    <view class="inverter-card__footer">
      <text class="update-time">更新时间：{{ formatUpdateTime }}</text>
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
  }
})

const emit = defineEmits(['click'])

const statusType = computed(() => {
  if (props.data.status === 1) return 'success'
  if (props.data.status === 2) return 'error'
  return 'default'
})

const formatUpdateTime = computed(() => {
  if (props.data.updateTime) {
    return formatDateTime(props.data.updateTime)
  }
  return '--'
})

function handleClick() {
  emit('click', props.data)
}
</script>

<style lang="scss" scoped>
.inverter-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
  
  &__header {
    margin-bottom: 24rpx;
    padding-bottom: 20rpx;
    border-bottom: 1rpx solid #f0f0f0;
  }
  
  &__title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12rpx;
  }
  
  &__name {
    font-size: 32rpx;
    font-weight: 600;
    color: #333;
  }
  
  &__station {
    font-size: 24rpx;
    color: #999;
  }
  
  &__body {
    display: flex;
    flex-wrap: wrap;
  }
  
  &__item {
    width: 50%;
    margin-bottom: 20rpx;
    
    &:nth-child(odd) {
      padding-right: 20rpx;
    }
    
    &:nth-child(even) {
      padding-left: 20rpx;
    }
  }
  
  .item-label {
    font-size: 24rpx;
    color: #999;
    margin-bottom: 8rpx;
  }
  
  .item-value {
    font-size: 28rpx;
    color: #333;
    font-weight: 500;
  }
  
  &__footer {
    margin-top: 12rpx;
    padding-top: 16rpx;
    border-top: 1rpx solid #f0f0f0;
  }
  
  .update-time {
    font-size: 22rpx;
    color: #bbb;
  }
}
</style>
