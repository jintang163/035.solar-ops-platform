<template>
  <view class="stat-card" @click="handleClick">
    <view class="stat-card__icon" :style="{ backgroundColor: bgColor }">
      <text class="stat-card__icon-text">{{ icon }}</text>
    </view>
    <view class="stat-card__content">
      <view class="stat-card__value">
        <text class="stat-card__num">{{ value }}</text>
        <text class="stat-card__unit">{{ unit }}</text>
      </view>
      <view class="stat-card__label">{{ label }}</view>
    </view>
    <view class="stat-card__arrow" v-if="showArrow">
      <text class="arrow-icon">›</text>
    </view>
  </view>
</template>

<script setup>
defineProps({
  icon: {
    type: String,
    default: '📊'
  },
  value: {
    type: [String, Number],
    default: '0'
  },
  unit: {
    type: String,
    default: ''
  },
  label: {
    type: String,
    default: ''
  },
  bgColor: {
    type: String,
    default: '#e6f7ff'
  },
  showArrow: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

function handleClick() {
  emit('click')
}
</script>

<style lang="scss" scoped>
.stat-card {
  display: flex;
  align-items: center;
  padding: 30rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
  
  &__icon {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 24rpx;
    flex-shrink: 0;
  }
  
  &__icon-text {
    font-size: 36rpx;
  }
  
  &__content {
    flex: 1;
  }
  
  &__value {
    display: flex;
    align-items: baseline;
    margin-bottom: 8rpx;
  }
  
  &__num {
    font-size: 36rpx;
    font-weight: 600;
    color: #333;
    line-height: 1.2;
  }
  
  &__unit {
    font-size: 24rpx;
    color: #999;
    margin-left: 8rpx;
  }
  
  &__label {
    font-size: 26rpx;
    color: #666;
  }
  
  &__arrow {
    margin-left: 16rpx;
  }
  
  .arrow-icon {
    font-size: 36rpx;
    color: #ccc;
  }
}
</style>
