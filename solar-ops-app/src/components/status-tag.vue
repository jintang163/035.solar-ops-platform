<template>
  <view class="status-tag" :class="`status-tag--${type}`">
    {{ text }}
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { getStatusText } from '@/utils/format'

const props = defineProps({
  status: {
    type: [Number, String],
    default: 0
  },
  type: {
    type: String,
    default: 'order'
  },
  text: {
    type: String,
    default: ''
  }
})

const displayText = computed(() => {
  return props.text || getStatusText(props.status, props.type)
})
</script>

<style lang="scss" scoped>
.status-tag {
  display: inline-block;
  padding: 6rpx 16rpx;
  font-size: 24rpx;
  border-radius: 8rpx;
  line-height: 1.2;
  
  &--default {
    background-color: #f0f0f0;
    color: #999;
  }
  
  &--success {
    background-color: #f0f9eb;
    color: #52c41a;
  }
  
  &--warning {
    background-color: #fffbe6;
    color: #faad14;
  }
  
  &--error {
    background-color: #fff1f0;
    color: #ff4d4f;
  }
  
  &--primary {
    background-color: #e6f7ff;
    color: #1890ff;
  }
  
  &--processing {
    background-color: #e6f7ff;
    color: #1890ff;
  }
}
</style>
