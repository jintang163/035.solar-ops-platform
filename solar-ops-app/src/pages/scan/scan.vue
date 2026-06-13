<template>
  <view class="scan-page">
    <view class="scan-container">
      <view class="scan-area">
        <view class="scan-box">
          <view class="scan-frame">
            <view class="corner corner-tl"></view>
            <view class="corner corner-tr"></view>
            <view class="corner corner-bl"></view>
            <view class="corner corner-br"></view>
            <view class="scan-line" :class="{ 'scan-move': scanning }"></view>
          </view>
        </view>
        
        <view class="scan-tip">
          <text class="tip-text">将二维码放入框内，自动识别</text>
        </view>
      </view>
      
      <view class="action-section">
        <view class="action-row">
          <view class="action-btn" @click="startScan">
            <view class="action-icon">📷</view>
            <text class="action-text">{{ scanning ? '识别中...' : '点击扫码' }}</text>
          </view>
          
          <view class="action-btn" @click="inputManual">
            <view class="action-icon">⌨️</view>
            <text class="action-text">手动输入</text>
          </view>
        </view>
      </view>
      
      <view class="history-section" v-if="scanHistory.length > 0">
        <view class="section-header">
          <text class="section-title">扫码历史</text>
          <text class="clear-btn" @click="clearHistory">清空</text>
        </view>
        <view class="history-list">
          <view 
            class="history-item" 
            v-for="(item, index) in scanHistory" 
            :key="index"
            @click="goToAssetDetail(item)"
          >
            <view class="history-icon">📱</view>
            <view class="history-info">
              <text class="history-code">{{ item.assetName || item.assetCode }}</text>
              <text class="history-time">{{ item.scanTime }}</text>
            </view>
            <text class="history-arrow">›</text>
          </view>
        </view>
      </view>
    </view>
    
    <view class="modal" v-if="inputVisible" @click="closeInputModal">
      <view class="modal-content" @click.stop>
        <view class="modal-title">输入资产编号</view>
        <input 
          v-model="inputCode" 
          class="modal-input" 
          placeholder="请输入资产编号" 
          focus
        />
        <view class="modal-btns">
          <view class="modal-btn cancel-btn" @click="closeInputModal">取消</view>
          <view class="modal-btn confirm-btn" @click="confirmInput">确定</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAssetByScan } from '@/api/asset'
import { formatDate } from '@/utils/format'

const scanning = ref(false)
const scanHistory = ref([])
const inputVisible = ref(false)
const inputCode = ref('')

onMounted(() => {
  loadHistory()
  startScan()
})

function loadHistory() {
  try {
    const history = uni.getStorageSync('scanHistory')
    if (history) {
      scanHistory.value = JSON.parse(history)
    }
  } catch (e) {
    console.error('加载历史记录失败:', e)
  }
}

function saveHistory(asset) {
  const history = {
    id: asset.id,
    assetCode: asset.assetCode,
    assetName: asset.assetName,
    assetType: asset.assetType,
    scanTime: formatDate(new Date(), 'MM-DD HH:mm')
  }
  
  const newHistory = [history, ...scanHistory.value.filter(h => h.id !== asset.id)].slice(0, 10)
  scanHistory.value = newHistory
  uni.setStorageSync('scanHistory', JSON.stringify(newHistory))
}

function clearHistory() {
  uni.showModal({
    title: '提示',
    content: '确定清空扫码历史？',
    success: (res) => {
      if (res.confirm) {
        scanHistory.value = []
        uni.removeStorageSync('scanHistory')
        uni.showToast({ title: '已清空', icon: 'success' })
      }
    }
  })
}

async function startScan() {
  scanning.value = true
  
  try {
    const res = await uni.scanCode({
      onlyFromCamera: false,
      scanType: ['qrCode', 'barCode']
    })
    
    const code = res[0].result
    console.log('扫码结果:', code)
    
    await queryAssetByCode(code)
    
  } catch (err) {
    console.error('扫码失败:', err)
    if (err.errMsg && err.errMsg.indexOf('cancel') === -1) {
      uni.showToast({ title: '扫码失败，请重试', icon: 'none' })
    }
  } finally {
    scanning.value = false
  }
}

function inputManual() {
  inputCode.value = ''
  inputVisible.value = true
}

function closeInputModal() {
  inputVisible.value = false
}

async function confirmInput() {
  if (!inputCode.value.trim()) {
    uni.showToast({ title: '请输入资产编号', icon: 'none' })
    return
  }
  
  inputVisible.value = false
  await queryAssetByCode(inputCode.value.trim())
}

async function queryAssetByCode(code) {
  uni.showLoading({ title: '查询中...' })
  
  try {
    const asset = await getAssetByScan(code)
    
    if (asset) {
      saveHistory(asset)
      uni.navigateTo({
        url: `/pages/asset/asset-detail?id=${asset.id}&code=${code}`
      })
    } else {
      uni.showToast({ title: '未找到该资产', icon: 'none' })
    }
    
  } catch (err) {
    console.error('查询资产失败:', err)
    uni.showToast({ title: err.message || '查询失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

function goToAssetDetail(item) {
  uni.navigateTo({
    url: `/pages/asset/asset-detail?id=${item.id}`
  })
}
</script>

<style lang="scss" scoped>
.scan-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #1890ff 0%, #e6f7ff 100%);
}

.scan-container {
  padding: 40rpx 30rpx;
}

.scan-area {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 0;
}

.scan-box {
  width: 500rpx;
  height: 500rpx;
  position: relative;
}

.scan-frame {
  width: 100%;
  height: 100%;
  position: relative;
  border: 2rpx solid rgba(255, 255, 255, 0.3);
  border-radius: 16rpx;
  overflow: hidden;
}

.corner {
  position: absolute;
  width: 40rpx;
  height: 40rpx;
  border-color: #ffffff;
  z-index: 2;

  &.corner-tl {
    top: 0;
    left: 0;
    border-top: 6rpx solid #ffffff;
    border-left: 6rpx solid #ffffff;
    border-top-left-radius: 16rpx;
  }
  
  &.corner-tr {
    top: 0;
    right: 0;
    border-top: 6rpx solid #ffffff;
    border-right: 6rpx solid #ffffff;
    border-top-right-radius: 16rpx;
  }
  
  &.corner-bl {
    bottom: 0;
    left: 0;
    border-bottom: 6rpx solid #ffffff;
    border-left: 6rpx solid #ffffff;
    border-bottom-left-radius: 16rpx;
  }
  
  &.corner-br {
    bottom: 0;
    right: 0;
    border-bottom: 6rpx solid #ffffff;
    border-right: 6rpx solid #ffffff;
    border-bottom-right-radius: 16rpx;
  }
}

.scan-line {
  position: absolute;
  left: 5%;
  width: 90%;
  height: 4rpx;
  background: linear-gradient(90deg, 
    transparent 0%, 
    #52c41a 50%, 
    transparent 100%);
  box-shadow: 0 0 20rpx #52c41a;
  top: 0;
  z-index: 1;

  &.scan-move {
    animation: scanMove 2s ease-in-out infinite;
  }
}

@keyframes scanMove {
  0% { top: 0; }
  50% { top: calc(100% - 4rpx); }
  100% { top: 0; }
}

.scan-tip {
  margin-top: 40rpx;
  text-align: center;
}

.tip-text {
  font-size: 28rpx;
  color: rgba(255, 255, 255);
  opacity: 0.9;
}

.action-section {
  margin-top: 60rpx;
}

.action-row {
  display: flex;
  justify-content: center;
  gap: 80rpx;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30rpx;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16rpx;
  width: 200rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.1);
}

.action-icon {
  font-size: 48rpx;
  margin-bottom: 12rpx;
}

.action-text {
  font-size: 26rpx;
  color: #333;
}

.history-section {
  margin-top: 60rpx;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16rpx;
  padding: 30rpx;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.clear-btn {
  font-size: 24rpx;
  color: #999;
}

.history-list {
  max-height: 400rpx;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.history-icon {
  font-size: 40rpx;
  margin-right: 20rpx;
}

.history-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.history-code {
  font-size: 28rpx;
  color: #333;
  margin-bottom: 4rpx;
}

.history-time {
  font-size: 22rpx;
  color: #999;
}

.history-arrow {
  font-size: 32rpx;
  color: #ccc;
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-content {
  width: 600rpx;
  background: #ffffff;
  border-radius: 16rpx;
  padding: 40rpx;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
  text-align: center;
  margin-bottom: 30rpx;
}

.modal-input {
  width: 100%;
  height: 80rpx;
  border: 1rpx solid #e8e8e8;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
  margin-bottom: 30rpx;
}

.modal-btns {
  display: flex;
  gap: 20rpx;
}

.modal-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: 8rpx;
  font-size: 28rpx;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
}

.confirm-btn {
  background: #1890ff;
  color: #ffffff;
}
</style>
