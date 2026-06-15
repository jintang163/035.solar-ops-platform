<template>
  <view class="outbound-scan-page">
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
          <text class="tip-text">扫描备件二维码，执行出库操作</text>
        </view>
      </view>
      
      <view class="action-section">
        <view class="action-row">
          <view class="action-btn primary" @click="startScan">
            <view class="action-icon">📷</view>
            <text class="action-text">{{ scanning ? '识别中...' : '扫码出库' }}</text>
          </view>
          
          <view class="action-btn" @click="inputManual">
            <view class="action-icon">⌨️</view>
            <text class="action-text">手动输入</text>
          </view>
          
          <view class="action-btn" @click="goToList">
            <view class="action-icon">📋</view>
            <text class="action-text">出库记录</text>
          </view>
        </view>
      </view>

      <view v-if="currentPart" class="part-info-section">
        <view class="part-header">
          <view class="part-name">{{ currentPart.partName }}</view>
          <view class="part-tag" :class="getWarnClass(currentPart.warnStatus)">
            {{ getWarnText(currentPart.warnStatus) }}
          </view>
        </view>
        
        <view class="part-detail">
          <view class="detail-row">
            <text class="detail-label">备件编号</text>
            <text class="detail-value" style="font-family: monospace;">{{ currentPart.partCode }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">备件型号</text>
            <text class="detail-value">{{ currentPart.partModel }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">备件类型</text>
            <text class="detail-value">{{ currentPart.partTypeDesc }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">当前库存</text>
            <text class="detail-value quantity" :class="getQuantityClass(currentPart.quantity, currentPart.safeQuantity)">
              {{ currentPart.quantity }} {{ currentPart.unit }}
            </text>
          </view>
          <view class="detail-row">
            <text class="detail-label">安全库存</text>
            <text class="detail-value">{{ currentPart.safeQuantity }} {{ currentPart.unit }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">存放位置</text>
            <text class="detail-value">{{ currentPart.storageLocation }}</text>
          </view>
        </view>
      </view>

      <view v-if="currentPart" class="outbound-form-section">
        <view class="form-title">出库信息</view>
        
        <view class="form-item">
          <text class="form-label">出库类型</text>
          <picker :range="inOutTypeOptions" :range-key="'label'" :value="inOutTypeIndex" @change="onInOutTypeChange">
            <view class="picker-view">
              {{ inOutTypeOptions[inOutTypeIndex].label }}
              <text class="picker-arrow">›</text>
            </view>
          </picker>
        </view>

        <view class="form-item">
          <text class="form-label">出库数量</text>
          <view class="quantity-input-wrap">
            <view class="quantity-btn" @click="decreaseQuantity">−</view>
            <input 
              v-model="outboundQuantity" 
              type="number" 
              class="quantity-input" 
              placeholder="0"
            />
            <view class="quantity-btn" @click="increaseQuantity">+</view>
          </view>
          <text class="form-hint">可出库数量: {{ currentPart.quantity }} {{ currentPart.unit }}</text>
        </view>

        <view v-if="inOutTypeOptions[inOutTypeIndex].value === 21" class="form-item">
          <text class="form-label">关联工单号</text>
          <input 
            v-model="workOrderNo" 
            class="form-input" 
            placeholder="请输入工单号（可选）"
          />
        </view>

        <view class="form-item">
          <text class="form-label">备注</text>
          <textarea 
            v-model="remark" 
            class="form-textarea" 
            placeholder="请输入备注信息（可选）"
            maxlength="200"
          />
        </view>

        <view class="submit-btn-wrap">
          <button 
            class="submit-btn" 
            :disabled="!canSubmit"
            :class="{ 'btn-disabled': !canSubmit }"
            @click="handleOutbound"
          >
            确认出库
          </button>
        </view>
      </view>
      
      <view class="history-section" v-if="recentRecords.length > 0">
        <view class="section-header">
          <text class="section-title">最近出库记录</text>
        </view>
        <view class="history-list">
          <view 
            class="history-item" 
            v-for="(item, index) in recentRecords" 
            :key="index"
          >
            <view class="history-icon-box" :class="getTypeClass(item.inOutType)">
              <text class="history-icon">{{ getTypeIcon(item.inOutType) }}</text>
            </view>
            <view class="history-info">
              <view class="history-row">
                <text class="history-name">{{ item.partName }}</text>
                <text class="history-quantity" :class="getTypeClass(item.inOutType)">
                  -{{ item.quantity }}
                </text>
              </view>
              <view class="history-row">
                <text class="history-desc">{{ item.partCode }} · {{ getTypeText(item.inOutType) }}</text>
                <text class="history-time">{{ formatTime(item.createTime) }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>
    
    <view class="modal" v-if="inputVisible" @click="closeInputModal">
      <view class="modal-content" @click.stop>
        <view class="modal-title">输入备件编号</view>
        <input 
          v-model="inputCode" 
          class="modal-input" 
          placeholder="请输入备件编号或二维码内容" 
          focus
        />
        <view class="modal-btns">
          <view class="modal-btn cancel-btn" @click="closeInputModal">取消</view>
          <view class="modal-btn confirm-btn" @click="confirmInput">确定</view>
        </view>
      </view>
    </view>

    <view class="modal" v-if="successVisible" @click="closeSuccessModal">
      <view class="modal-content success-modal" @click.stop>
        <view class="success-icon">✓</view>
        <view class="success-title">出库成功</view>
        <view class="success-info">
          <text>{{ currentPart?.partName }} -{{ outboundQuantity }} {{ currentPart?.unit }}</text>
        </view>
        <view class="modal-btns">
          <view class="modal-btn cancel-btn" @click="closeSuccessModal">继续扫码</view>
          <view class="modal-btn confirm-btn" @click="resetAndContinue">完成</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { scanSparePart, sparePartOutbound, getInOutRecordList } from '@/api/spareParts'

const scanning = ref(false)
const inputVisible = ref(false)
const inputCode = ref('')
const currentPart = ref(null)
const outboundQuantity = ref('1')
const inOutTypeIndex = ref(0)
const workOrderNo = ref('')
const remark = ref('')
const successVisible = ref(false)
const recentRecords = ref([])

const inOutTypeOptions = ref([
  { value: 21, label: '工单出库' },
  { value: 23, label: '报废出库' }
])

const canSubmit = computed(() => {
  const qty = parseInt(outboundQuantity.value)
  return currentPart.value && 
         qty > 0 && 
         qty <= currentPart.value.quantity
})

onMounted(() => {
  loadRecentRecords()
  const parts = getCurrentPages()
  const currentPage = parts[parts.length - 1]
  const options = currentPage.options
  if (options.code) {
    queryPartByCode(options.code)
  }
})

async function loadRecentRecords() {
  try {
    const res = await getInOutRecordList({
      pageNum: 1,
      pageSize: 5,
      inOutType: [21, 22, 23]
    })
    if (res && res.list) {
      recentRecords.value = res.list
    }
  } catch (e) {
    console.error('加载出库记录失败', e)
  }
}

function getWarnClass(status) {
  if (status === 2) return 'tag-danger'
  if (status === 1) return 'tag-warning'
  return 'tag-normal'
}

function getWarnText(status) {
  if (status === 2) return '库存不足'
  if (status === 1) return '低库存预警'
  return '库存正常'
}

function getQuantityClass(qty, safeQty) {
  if (qty <= 0) return 'qty-danger'
  if (qty < safeQty) return 'qty-warning'
  return 'qty-normal'
}

function getTypeClass(type) {
  if (type === 21) return 'type-workorder'
  if (type === 22) return 'type-stocktake'
  if (type === 23) return 'type-scrap'
  return ''
}

function getTypeIcon(type) {
  if (type === 21) return '🔧'
  if (type === 22) return '📊'
  if (type === 23) return '🗑️'
  return '📤'
}

function getTypeText(type) {
  const option = inOutTypeOptions.value.find(o => o.value === type)
  return option?.label || '出库'
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

function onInOutTypeChange(e) {
  inOutTypeIndex.value = e.detail.value
}

function decreaseQuantity() {
  let qty = parseInt(outboundQuantity.value) || 1
  if (qty > 1) {
    outboundQuantity.value = String(qty - 1)
  }
}

function increaseQuantity() {
  let qty = parseInt(outboundQuantity.value) || 0
  if (currentPart.value && qty < currentPart.value.quantity) {
    outboundQuantity.value = String(qty + 1)
  }
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
    
    await queryPartByCode(code)
    
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
    uni.showToast({ title: '请输入备件编号', icon: 'none' })
    return
  }
  
  inputVisible.value = false
  await queryPartByCode(inputCode.value.trim())
}

async function queryPartByCode(code) {
  uni.showLoading({ title: '查询中...' })
  
  try {
    const part = await scanSparePart(code)
    
    if (part) {
      currentPart.value = part
      outboundQuantity.value = '1'
      if (part.quantity <= 0) {
        uni.showToast({ title: '库存不足，无法出库', icon: 'none' })
      }
    } else {
      uni.showToast({ title: '未找到该备件', icon: 'none' })
    }
    
  } catch (err) {
    console.error('查询备件失败:', err)
    uni.showToast({ title: err.message || '查询失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

async function handleOutbound() {
  if (!canSubmit.value) {
    uni.showToast({ title: '请检查出库数量', icon: 'none' })
    return
  }

  uni.showModal({
    title: '确认出库',
    content: `确定要出库 ${currentPart.value.partName} ${outboundQuantity.value} ${currentPart.value.unit} 吗？`,
    success: async (res) => {
      if (res.confirm) {
        await doOutbound()
      }
    }
  })
}

async function doOutbound() {
  uni.showLoading({ title: '出库中...' })
  
  try {
    const selectedType = inOutTypeOptions.value[inOutTypeIndex.value]
    const params = {
      partId: currentPart.value.id,
      partCode: currentPart.value.partCode,
      partName: currentPart.value.partName,
      inOutType: selectedType.value,
      quantity: parseInt(outboundQuantity.value),
      workOrderNo: workOrderNo.value || undefined,
      operatorName: '管理员',
      remark: remark.value || undefined
    }

    await sparePartOutbound(params)
    
    successVisible.value = true
    loadRecentRecords()
    
  } catch (err) {
    console.error('出库失败:', err)
    uni.showToast({ title: err.message || '出库失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

function closeSuccessModal() {
  successVisible.value = false
  currentPart.value = null
  outboundQuantity.value = '1'
  workOrderNo.value = ''
  remark.value = ''
}

function resetAndContinue() {
  successVisible.value = false
  currentPart.value = null
  outboundQuantity.value = '1'
  workOrderNo.value = ''
  remark.value = ''
}

function goToList() {
  uni.showToast({ title: '开发中', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.outbound-scan-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #1890ff 0%, #e6f7ff 100%);
  padding-bottom: 40rpx;
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
  margin-top: 40rpx;
}

.action-row {
  display: flex;
  justify-content: center;
  gap: 30rpx;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24rpx;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16rpx;
  flex: 1;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.1);
  
  &.primary {
    background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
    
    .action-text {
      color: #fff;
    }
  }
}

.action-icon {
  font-size: 40rpx;
  margin-bottom: 8rpx;
}

.action-text {
  font-size: 24rpx;
  color: #333;
}

.part-info-section {
  margin-top: 40rpx;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.1);
}

.part-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.part-name {
  font-size: 36rpx;
  font-weight: 600;
  color: #333;
}

.part-tag {
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  
  &.tag-normal {
    background: #f6ffed;
    color: #52c41a;
  }
  
  &.tag-warning {
    background: #fffbe6;
    color: #faad14;
  }
  
  &.tag-danger {
    background: #fff1f0;
    color: #ff4d4f;
  }
}

.part-detail {
  .detail-row {
    display: flex;
    justify-content: space-between;
    padding: 12rpx 0;
    
    &:not(:last-child) {
      border-bottom: 1rpx solid #fafafa;
    }
  }
  
  .detail-label {
    font-size: 26rpx;
    color: #999;
  }
  
  .detail-value {
    font-size: 26rpx;
    color: #333;
    
    &.quantity {
      font-weight: 600;
    }
  }
  
  .qty-normal {
    color: #52c41a;
  }
  
  .qty-warning {
    color: #faad14;
  }
  
  .qty-danger {
    color: #ff4d4f;
  }
}

.outbound-form-section {
  margin-top: 30rpx;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.1);
}

.form-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 24rpx;
}

.form-item {
  margin-bottom: 24rpx;
}

.form-label {
  display: block;
  font-size: 26rpx;
  color: #666;
  margin-bottom: 12rpx;
}

.picker-view {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 80rpx;
  padding: 0 20rpx;
  background: #f8f9fa;
  border-radius: 8rpx;
  font-size: 28rpx;
  color: #333;
}

.picker-arrow {
  color: #ccc;
  font-size: 32rpx;
}

.quantity-input-wrap {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.quantity-btn {
  width: 70rpx;
  height: 70rpx;
  line-height: 66rpx;
  text-align: center;
  background: #f0f2f5;
  border-radius: 8rpx;
  font-size: 36rpx;
  color: #1890ff;
  font-weight: 600;
  
  &:active {
    background: #e6f7ff;
  }
}

.quantity-input {
  flex: 1;
  height: 70rpx;
  text-align: center;
  background: #f8f9fa;
  border-radius: 8rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.form-hint {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #999;
}

.form-input {
  width: 100%;
  height: 80rpx;
  padding: 0 20rpx;
  background: #f8f9fa;
  border-radius: 8rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
}

.form-textarea {
  width: 100%;
  height: 160rpx;
  padding: 16rpx 20rpx;
  background: #f8f9fa;
  border-radius: 8rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
}

.submit-btn-wrap {
  margin-top: 30rpx;
}

.submit-btn {
  width: 100%;
  height: 90rpx;
  line-height: 90rpx;
  background: linear-gradient(135deg, #ff4d4f 0%, #cf1322 100%);
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 12rpx;
  border: none;
  
  &.btn-disabled {
    background: #d9d9d9;
    color: #fff;
  }
}

.history-section {
  margin-top: 40rpx;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.1);
}

.section-header {
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.history-list {
  max-height: 500rpx;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  
  &:not(:last-child) {
    border-bottom: 1rpx solid #f0f0f0;
  }
}

.history-icon-box {
  width: 72rpx;
  height: 72rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  
  &.type-workorder {
    background: #e6f7ff;
  }
  
  &.type-stocktake {
    background: #fff7e6;
  }
  
  &.type-scrap {
    background: #fff1f0;
  }
}

.history-icon {
  font-size: 36rpx;
}

.history-info {
  flex: 1;
}

.history-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  &:first-child {
    margin-bottom: 4rpx;
  }
}

.history-name {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

.history-quantity {
  font-size: 28rpx;
  font-weight: 600;
  
  &.type-workorder {
    color: #1890ff;
  }
  
  &.type-stocktake {
    color: #faad14;
  }
  
  &.type-scrap {
    color: #ff4d4f;
  }
}

.history-desc {
  font-size: 22rpx;
  color: #999;
}

.history-time {
  font-size: 22rpx;
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
  
  &.success-modal {
    text-align: center;
  }
}

.success-icon {
  width: 120rpx;
  height: 120rpx;
  line-height: 120rpx;
  margin: 0 auto 30rpx;
  background: #f6ffed;
  border-radius: 50%;
  font-size: 60rpx;
  color: #52c41a;
}

.success-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 16rpx;
}

.success-info {
  font-size: 28rpx;
  color: #666;
  margin-bottom: 40rpx;
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
