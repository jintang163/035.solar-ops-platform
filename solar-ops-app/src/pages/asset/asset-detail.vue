<template>
  <view class="asset-detail-page">
    <view class="asset-header" v-if="asset">
      <view class="asset-basic">
        <view class="asset-type-tag" :class="asset.assetType">
          {{ getAssetTypeText(asset.assetType) }}
        </view>
        <view class="asset-name">{{ asset.assetName }}</view>
        <view class="asset-code">编号：{{ asset.assetCode }}</view>
      </view>
      
      <view class="warranty-section">
        <view class="warranty-item" v-if="asset.warrantyEndDate">
          <text class="warranty-label">质保到期</text>
          <view class="warranty-status" :class="warrantyStatusClass">
            <text class="warranty-days">{{ warrantyDaysText }}</text>
          </view>
        </view>
        <view class="warranty-item">
          <text class="warranty-label">状态</text>
          <view class="status-tag" :class="getStatusClass(asset.assetStatus)">
            {{ getStatusText(asset.assetStatus) }}
          </view>
        </view>
      </view>
    </view>
    
    <view class="tab-bar">
      <view 
        class="tab-item" 
        :class="{ active: activeTab === 'info' }"
        @click="activeTab = 'info'"
      >
        基本信息
      </view>
      <view 
        class="tab-item" 
        :class="{ active: activeTab === 'maintenance' }"
        @click="activeTab = 'maintenance'"
      >
        维修记录 ({{ maintenanceList.length }})
      </view>
    </view>
    
    <view class="tab-content">
      <view class="info-panel" v-if="activeTab === 'info' && asset">
        <view class="info-card">
          <view class="info-title">设备信息</view>
          <view class="info-row">
            <text class="info-label">设备型号</text>
            <text class="info-value">{{ asset.deviceModel || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">设备品牌</text>
            <text class="info-value">{{ asset.brand || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">设备容量</text>
            <text class="info-value">{{ asset.capacity || '-' }} kW</text>
          </view>
          <view class="info-row">
            <text class="info-label">所属电站</text>
            <text class="info-value">{{ asset.stationName || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">安装位置</text>
            <text class="info-value">{{ asset.installLocation || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">安装日期</text>
            <text class="info-value">{{ formatDate(asset.installDate) }}</text>
          </view>
        </view>
        
        <view class="info-card">
          <view class="info-title">质保信息</view>
          <view class="info-row">
            <text class="info-label">质保期限</text>
            <text class="info-value">{{ asset.warrantyMonths ? asset.warrantyMonths + ' 个月' : '-' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">质保开始</text>
            <text class="info-value">{{ formatDate(asset.warrantyStartDate) }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">质保到期</text>
            <text class="info-value">{{ formatDate(asset.warrantyEndDate) }}</text>
          </view>
        </view>
        
        <view class="info-card">
          <view class="info-title">供应商信息</view>
          <view class="info-row">
            <text class="info-label">供应商</text>
            <text class="info-value">{{ asset.supplier || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">生产厂家</text>
            <text class="info-value">{{ asset.manufacturer || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">责任人</text>
            <text class="info-value">{{ asset.responsiblePerson || '-' }}</text>
          </view>
        </view>
        
        <view class="info-card" v-if="asset.remark">
          <view class="info-title">备注</view>
          <view class="remark-text">{{ asset.remark }}</view>
        </view>
      </view>
      
      <view class="maintenance-panel" v-if="activeTab === 'maintenance'">
        <view class="add-btn" @click="showAddModal = true">
          <text class="add-icon">+</text>
          <text class="add-text">添加维修记录</text>
        </view>
        
        <view class="maintenance-list" v-if="maintenanceList.length > 0">
          <view 
            class="maintenance-item" 
            v-for="item in maintenanceList" 
            :key="item.id"
            @click="viewMaintenanceDetail(item)"
          >
            <view class="maintenance-header">
              <view class="maintenance-type" :class="getMaintenanceTypeClass(item.maintenanceType)">
                {{ getMaintenanceTypeText(item.maintenanceType) }}
              </view>
              <text class="maintenance-time">{{ formatDate(item.maintenanceTime) }}</text>
            </view>
            <view class="maintenance-content">
              <text class="fault-desc" v-if="item.faultDescription">
                故障：{{ item.faultDescription }}
              </text>
              <text class="work-content" v-if="item.maintenanceContent">
                内容：{{ item.maintenanceContent }}
              </text>
            </view>
            <view class="maintenance-footer">
              <text class="operator">维修人：{{ item.maintenancePerson || '-' }}</text>
              <text class="cost" v-if="item.cost">
                ¥{{ item.cost }}
              </text>
            </view>
          </view>
        </view>
        
        <view class="empty-state" v-else>
          <text class="empty-icon">📝</text>
          <text class="empty-text">暂无维修记录</text>
        </view>
      </view>
    </view>
    
    <view class="action-bar" v-if="asset && asset.assetStatus === 1">
      <view class="action-btn retire-btn" @click="handleRetire">
        <text>资产退役</text>
      </view>
      <view class="action-btn scrap-btn" @click="handleScrap">
        <text>资产报废</text>
      </view>
    </view>
    
    <view class="modal" v-if="showAddModal" @click="closeAddModal">
      <view class="modal-content large-modal" @click.stop>
        <view class="modal-header">
          <text class="modal-title">添加维修记录</text>
          <text class="modal-close" @click="closeAddModal">×</text>
        </view>
        
        <scroll-view class="modal-body" scroll-y>
          <view class="form-group">
            <text class="form-label">维修类型 <text class="required">*</text></text>
            <picker 
              :value="formData.maintenanceTypeIndex" 
              :range="maintenanceTypeOptions"
              @change="onTypeChange"
            >
              <view class="picker-input">
                {{ maintenanceTypeOptions[formData.maintenanceTypeIndex] || '请选择' }}
              </view>
            </picker>
          </view>
          
          <view class="form-group">
            <text class="form-label">维修人员</text>
            <input 
              v-model="formData.maintenancePerson" 
              class="form-input" 
              placeholder="请输入维修人员"
            />
          </view>
          
          <view class="form-group">
            <text class="form-label">维修时间</text>
            <picker 
              mode="date" 
              :value="formData.maintenanceDate"
              @change="onDateChange"
            >
              <view class="picker-input">
                {{ formData.maintenanceDate || '请选择日期' }}
              </view>
            </picker>
          </view>
          
          <view class="form-group">
            <text class="form-label">维修费用</text>
            <input 
              v-model="formData.cost" 
              class="form-input" 
              type="digit"
              placeholder="请输入维修费用"
            />
          </view>
          
          <view class="form-group">
            <text class="form-label">故障描述</text>
            <textarea 
              v-model="formData.faultDescription" 
              class="form-textarea" 
              placeholder="请描述故障情况"
            />
          </view>
          
          <view class="form-group">
            <text class="form-label">维修内容</text>
            <textarea 
              v-model="formData.maintenanceContent" 
              class="form-textarea" 
              placeholder="请描述维修内容"
            />
          </view>
          
          <view class="form-group">
            <text class="form-label">解决方案</text>
            <textarea 
              v-model="formData.solution" 
              class="form-textarea" 
              placeholder="请描述解决方案"
            />
          </view>
          
          <view class="form-group">
            <text class="form-label">现场照片</text>
            <view class="upload-section">
              <view 
                class="upload-item" 
                v-for="(url, index) in photoUrls" 
                :key="index"
              >
                <image :src="url" class="upload-img" mode="aspectFill" @click="previewPhoto(index)" />
                <view class="delete-btn" @click="removePhoto(index)">×</view>
              </view>
              <view class="upload-btn" @click="chooseImage" v-if="photoUrls.length < 9">
                <text class="upload-icon">+</text>
                <text class="upload-text">上传照片</text>
              </view>
            </view>
          </view>
          
          <view class="form-group">
            <view class="form-label-row">
              <text class="form-label">更换备件</text>
              <text class="add-spare-btn" @click="addSparePart">+ 添加</text>
            </view>
            <view class="spare-part-list" v-if="spareParts.length > 0">
              <view class="spare-part-item" v-for="(part, index) in spareParts" :key="index">
                <view class="spare-part-header">
                  <text class="spare-part-title">备件 {{ index + 1 }}</text>
                  <text class="spare-part-delete" @click="removeSparePart(index)">删除</text>
                </view>
                <view class="spare-part-row">
                  <input 
                    v-model="part.partCode" 
                    class="form-input small" 
                    placeholder="备件编号"
                  />
                  <input 
                    v-model="part.partName" 
                    class="form-input small" 
                    placeholder="备件名称"
                  />
                </view>
                <view class="spare-part-row">
                  <input 
                    v-model="part.partModel" 
                    class="form-input small" 
                    placeholder="型号"
                  />
                  <input 
                    v-model="part.brand" 
                    class="form-input small" 
                    placeholder="品牌"
                  />
                </view>
                <view class="spare-part-row">
                  <input 
                    v-model="part.quantity" 
                    class="form-input small" 
                    type="number"
                    placeholder="数量"
                  />
                  <input 
                    v-model="part.unitPrice" 
                    class="form-input small" 
                    type="digit"
                    placeholder="单价"
                  />
                </view>
              </view>
            </view>
          </view>
        </scroll-view>
        
        <view class="modal-footer">
          <view class="modal-btn cancel-btn" @click="closeAddModal">取消</view>
          <view class="modal-btn confirm-btn" @click="submitMaintenance">提交</view>
        </view>
      </view>
    </view>
    
    <view class="modal" v-if="showDetailModal" @click="showDetailModal = false">
      <view class="modal-content large-modal" @click.stop>
        <view class="modal-header">
          <text class="modal-title">维修记录详情</text>
          <text class="modal-close" @click="showDetailModal = false">×</text>
        </view>
        
        <scroll-view class="modal-body" scroll-y v-if="currentDetail">
          <view class="detail-section">
            <view class="detail-row">
              <text class="detail-label">维修类型</text>
              <view class="detail-type" :class="getMaintenanceTypeClass(currentDetail.maintenanceType)">
                {{ getMaintenanceTypeText(currentDetail.maintenanceType) }}
              </view>
            </view>
            <view class="detail-row">
              <text class="detail-label">维修人员</text>
              <text class="detail-value">{{ currentDetail.maintenancePerson || '-' }}</text>
            </view>
            <view class="detail-row">
              <text class="detail-label">维修时间</text>
              <text class="detail-value">{{ formatDate(currentDetail.maintenanceTime) }}</text>
            </view>
            <view class="detail-row">
              <text class="detail-label">维修费用</text>
              <text class="detail-value price">
                {{ currentDetail.cost ? '¥' + currentDetail.cost : '-' }}
              </text>
            </view>
          </view>
          
          <view class="detail-section" v-if="currentDetail.faultDescription">
            <view class="section-subtitle">故障描述</view>
            <text class="section-content">{{ currentDetail.faultDescription }}</text>
          </view>
          
          <view class="detail-section" v-if="currentDetail.maintenanceContent">
            <view class="section-subtitle">维修内容</view>
            <text class="section-content">{{ currentDetail.maintenanceContent }}</text>
          </view>
          
          <view class="detail-section" v-if="currentDetail.solution">
            <view class="section-subtitle">解决方案</view>
            <text class="section-content">{{ currentDetail.solution }}</text>
          </view>
          
          <view class="detail-section" v-if="currentDetail.photos && currentDetail.photos.length > 0">
            <view class="section-subtitle">现场照片</view>
            <view class="photo-grid">
              <image 
                v-for="(url, index) in currentDetail.photos" 
                :key="index"
                :src="url" 
                class="photo-item" 
                mode="aspectFill"
                @click="previewPhoto(index, currentDetail.photos)"
              />
            </view>
          </view>
          
          <view class="detail-section" v-if="currentDetail.spareParts && currentDetail.spareParts.length > 0">
            <view class="section-subtitle">更换备件</view>
            <view class="spare-table">
              <view class="spare-table-header">
                <text class="spare-th">名称</text>
                <text class="spare-th">型号</text>
                <text class="spare-th">数量</text>
                <text class="spare-th">单价</text>
                <text class="spare-th">小计</text>
              </view>
              <view 
                class="spare-table-row" 
                v-for="(part, index) in currentDetail.spareParts" 
                :key="index"
              >
                <text class="spare-td">{{ part.partName }}</text>
                <text class="spare-td">{{ part.partModel || '-' }}</text>
                <text class="spare-td">{{ part.quantity }}</text>
                <text class="spare-td">¥{{ part.unitPrice || '0' }}</text>
                <text class="spare-td price">¥{{ part.totalPrice || '0' }}</text>
              </view>
            </view>
          </view>
        </scroll-view>
        
        <view class="modal-footer">
          <view class="modal-btn confirm-btn" @click="showDetailModal = false">关闭</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { 
  getAssetDetail, 
  getMaintenanceRecords, 
  addMaintenanceWithSpareParts,
  uploadFile,
  retireAsset,
  scrapAsset
} from '@/api/asset'
import { formatDate } from '@/utils/format'

const asset = ref(null)
const activeTab = ref('info')
const maintenanceList = ref([])
const showAddModal = ref(false)
const showDetailModal = ref(false)
const currentDetail = ref(null)
const photoUrls = ref([])
const spareParts = ref([])

const maintenanceTypeOptions = ['日常维护', '故障维修', '定期巡检', '备件更换']

const formData = reactive({
  maintenanceTypeIndex: 0,
  maintenanceType: 1,
  maintenancePerson: '',
  maintenanceDate: '',
  cost: '',
  faultDescription: '',
  maintenanceContent: '',
  solution: ''
})

const ASSET_TYPE_MAP = {
  station: '电站',
  inverter: '逆变器',
  combiner: '汇流箱',
  panel: '光伏组件',
  transformer: '变压器',
  other: '其他'
}

const ASSET_STATUS_MAP = {
  1: { text: '正常', class: 'status-normal' },
  2: { text: '运维中', class: 'status-maintenance' },
  3: { text: '已退役', class: 'status-retired' },
  4: { text: '已报废', class: 'status-scrapped' }
}

const MAINTENANCE_TYPE_MAP = {
  1: { text: '日常维护', class: 'type-1' },
  2: { text: '故障维修', class: 'type-2' },
  3: { text: '定期巡检', class: 'type-3' },
  4: { text: '备件更换', class: 'type-4' }
}

const warrantyStatusClass = computed(() => {
  if (!asset.value?.warrantyEndDate) return 'warranty-normal'
  const now = new Date()
  const end = new Date(asset.value.warrantyEndDate)
  const diff = Math.ceil((end - now) / (1000 * 60 * 60 * 24))
  
  if (diff < 0) return 'warranty-expired'
  if (diff <= 30) return 'warranty-warning'
  return 'warranty-normal'
})

const warrantyDaysText = computed(() => {
  if (!asset.value?.warrantyEndDate) return '无质保'
  const now = new Date()
  const end = new Date(asset.value.warrantyEndDate)
  const diff = Math.ceil((end - now) / (1000 * 60 * 60 * 24))
  
  if (diff < 0) return `已过期 ${-diff} 天`
  return `剩余 ${diff} 天`
})

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = currentPage.options
  
  if (options.id) {
    fetchAssetDetail(options.id)
    fetchMaintenanceList(options.id)
  }
})

async function fetchAssetDetail(id) {
  try {
    const data = await getAssetDetail(id)
    asset.value = data || {}
  } catch (e) {
    console.error('获取资产详情失败:', e)
    uni.showToast({ title: '获取资产详情失败', icon: 'none' })
  }
}

async function fetchMaintenanceList(assetId) {
  try {
    const data = await getMaintenanceRecords(assetId)
    const list = data.list || data || []
    maintenanceList.value = list.map(item => {
      const result = { ...item }
      if (result.photos && typeof result.photos === 'string') {
        try {
          result.photos = JSON.parse(result.photos)
        } catch (e) {
          result.photos = result.photos.split(',').filter(Boolean)
        }
      }
      if (result.cost !== undefined && result.cost !== null) {
        result.cost = Number(result.cost).toFixed(2)
      }
      return result
    })
  } catch (e) {
    console.error('获取维修记录失败:', e)
  }
}

function getAssetTypeText(type) {
  return ASSET_TYPE_MAP[type] || type
}

function getStatusText(status) {
  return ASSET_STATUS_MAP[status]?.text || status
}

function getStatusClass(status) {
  return ASSET_STATUS_MAP[status]?.class || 'status-normal'
}

function getMaintenanceTypeText(type) {
  if (typeof type === 'number') {
    return MAINTENANCE_TYPE_MAP[type]?.text || maintenanceTypeOptions[type - 1] || type
  }
  return MAINTENANCE_TYPE_MAP[type]?.text || type
}

function getMaintenanceTypeClass(type) {
  if (typeof type === 'number') {
    return MAINTENANCE_TYPE_MAP[type]?.class || 'type-1'
  }
  return MAINTENANCE_TYPE_MAP[type]?.class || 'type-1'
}

function onTypeChange(e) {
  const index = e.detail.value
  formData.maintenanceTypeIndex = index
  formData.maintenanceType = index + 1
}

function onDateChange(e) {
  formData.maintenanceDate = e.detail.value
}

function chooseImage() {
  uni.chooseImage({
    count: 9 - photoUrls.value.length,
    success: (res) => {
      uploadImages(res.tempFilePaths)
    }
  })
}

async function uploadImages(filePaths) {
  uni.showLoading({ title: '上传中...' })
  try {
    const results = await Promise.all(
      filePaths.map(path => uploadFile(path))
    )
    results.forEach(result => {
      if (result && result.url) {
        photoUrls.value.push(result.url)
      }
    })
    uni.showToast({ title: '上传成功', icon: 'success' })
  } catch (e) {
    console.error('上传失败:', e)
    uni.showToast({ title: '上传失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

function removePhoto(index) {
  photoUrls.value.splice(index, 1)
}

function previewPhoto(index, urls = photoUrls.value) {
  uni.previewImage({
    current: index,
    urls: urls
  })
}

function addSparePart() {
  spareParts.value.push({
    partCode: '',
    partName: '',
    partModel: '',
    brand: '',
    quantity: '',
    unitPrice: ''
  })
}

function removeSparePart(index) {
  spareParts.value.splice(index, 1)
}

function closeAddModal() {
  showAddModal.value = false
  resetForm()
}

function resetForm() {
  formData.maintenanceTypeIndex = 0
  formData.maintenanceType = 1
  formData.maintenancePerson = ''
  formData.maintenanceDate = ''
  formData.cost = ''
  formData.faultDescription = ''
  formData.maintenanceContent = ''
  formData.solution = ''
  photoUrls.value = []
  spareParts.value = []
}

function viewMaintenanceDetail(item) {
  currentDetail.value = item
  showDetailModal.value = true
}

async function submitMaintenance() {
  if (!formData.maintenancePerson) {
    uni.showToast({ title: '请输入维修人员', icon: 'none' })
    return
  }

  const validSpareParts = spareParts.value
    .filter(part => part.partName && part.partCode)
    .map(part => ({
      partCode: part.partCode,
      partName: part.partName,
      partModel: part.partModel,
      brand: part.brand,
      quantity: part.quantity ? parseInt(part.quantity) : null,
      unitPrice: part.unitPrice ? parseFloat(part.unitPrice) : null,
      totalPrice: part.unitPrice && part.quantity ? 
        (parseFloat(part.unitPrice) * parseInt(part.quantity)).toFixed(2) : null
    }))

  const data = {
    assetId: asset.value.id,
    maintenanceType: formData.maintenanceType,
    maintenancePerson: formData.maintenancePerson,
    maintenanceTime: formData.maintenanceDate ? 
      formData.maintenanceDate + 'T00:00:00' : null,
    cost: formData.cost ? parseFloat(formData.cost) : null,
    faultDescription: formData.faultDescription,
    maintenanceContent: formData.maintenanceContent,
    solution: formData.solution,
    photos: JSON.stringify(photoUrls.value),
    spareParts: validSpareParts
  }

  uni.showLoading({ title: '提交中...' })
  try {
    await addMaintenanceWithSpareParts(data)
    uni.showToast({ title: '提交成功', icon: 'success' })
    closeAddModal()
    fetchMaintenanceList(asset.value.id)
  } catch (e) {
    console.error('提交失败:', e)
    uni.showToast({ title: e.message || '提交失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

function handleRetire() {
  uni.showModal({
    title: '资产退役',
    content: '确定将该资产退役吗？退役后将无法编辑。',
    success: async (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '处理中...' })
        try {
          await retireAsset(asset.value.id)
          asset.value.assetStatus = 3
          uni.showToast({ title: '退役成功', icon: 'success' })
        } catch (e) {
          uni.showToast({ title: '操作失败', icon: 'none' })
        } finally {
          uni.hideLoading()
        }
      }
    }
  })
}

function handleScrap() {
  uni.showModal({
    title: '资产报废',
    content: '确定将该资产报废吗？此操作不可恢复。',
    success: async (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '处理中...' })
        try {
          await scrapAsset(asset.value.id)
          asset.value.assetStatus = 4
          uni.showToast({ title: '报废成功', icon: 'success' })
        } catch (e) {
          uni.showToast({ title: '操作失败', icon: 'none' })
        } finally {
          uni.hideLoading()
        }
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.asset-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 140rpx;
}

.asset-header {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  padding: 40rpx 30rpx;
  color: #ffffff;
}

.asset-basic {
  margin-bottom: 30rpx;
}

.asset-type-tag {
  display: inline-block;
  padding: 8rpx 20rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20rpx;
  font-size: 24rpx;
  margin-bottom: 16rpx;
}

.asset-name {
  font-size: 36rpx;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.asset-code {
  font-size: 26rpx;
  opacity: 0.8;
}

.warranty-section {
  display: flex;
  gap: 30rpx;
}

.warranty-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12rpx;
}

.warranty-label {
  font-size: 24rpx;
  opacity: 0.8;
  margin-bottom: 8rpx;
}

.warranty-status {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 26rpx;
  font-weight: 600;

  &.warranty-normal {
    background: #52c41a;
  }
  
  &.warranty-warning {
    background: #faad14;
  }
  
  &.warranty-expired {
    background: #ff4d4f;
  }
}

.warranty-days {
  color: #ffffff;
}

.status-tag {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 26rpx;
  font-weight: 600;

  &.status-normal {
    background: #52c41a;
  }
  
  &.status-maintenance {
    background: #faad14;
  }
  
  &.status-retired {
    background: #8c8c8c;
  }
  
  &.status-scrapped {
    background: #ff4d4f;
  }
}

.tab-bar {
  display: flex;
  background: #ffffff;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 30rpx 0;
  font-size: 28rpx;
  color: #666;
  position: relative;

  &.active {
    color: #1890ff;
    font-weight: 600;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 60rpx;
      height: 6rpx;
      background: #1890ff;
      border-radius: 3rpx;
    }
  }
}

.tab-content {
  padding: 20rpx;
}

.info-panel {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.info-card {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 30rpx;
}

.info-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.info-row {
  display: flex;
  padding: 16rpx 0;

  &:not(:last-child) {
    border-bottom: 1rpx solid #f8f8f8;
  }
}

.info-label {
  width: 180rpx;
  font-size: 26rpx;
  color: #999;
  flex-shrink: 0;
}

.info-value {
  flex: 1;
  font-size: 26rpx;
  color: #333;
}

.remark-text {
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
}

.maintenance-panel {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.add-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24rpx;
  background: #ffffff;
  border-radius: 16rpx;
  border: 2rpx dashed #1890ff;
}

.add-icon {
  font-size: 32rpx;
  color: #1890ff;
  margin-right: 8rpx;
}

.add-text {
  font-size: 28rpx;
  color: #1890ff;
}

.maintenance-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.maintenance-item {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 30rpx;
}

.maintenance-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.maintenance-type {
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;

  &.type-1 {
    background: #e6f7ff;
    color: #1890ff;
  }
  
  &.type-2 {
    background: #fff1f0;
    color: #ff4d4f;
  }
  
  &.type-3 {
    background: #f6ffed;
    color: #52c41a;
  }
  
  &.type-4 {
    background: #fff7e6;
    color: #fa8c16;
  }
}

.maintenance-time {
  font-size: 24rpx;
  color: #999;
}

.maintenance-content {
  margin-bottom: 16rpx;
}

.fault-desc,
.work-content {
  display: block;
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
  margin-bottom: 8rpx;
}

.maintenance-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16rpx;
  border-top: 1rpx solid #f0f0f0;
}

.operator {
  font-size: 24rpx;
  color: #999;
}

.cost {
  font-size: 26rpx;
  color: #ff4d4f;
  font-weight: 600;
}

.empty-state {
  text-align: center;
  padding: 80rpx 0;
  background: #ffffff;
  border-radius: 16rpx;
}

.empty-icon {
  font-size: 80rpx;
  display: block;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}

.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #ffffff;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
  gap: 20rpx;
}

.action-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  border-radius: 12rpx;
  font-size: 28rpx;
  font-weight: 600;

  &.retire-btn {
    background: #fff7e6;
    color: #fa8c16;
  }
  
  &.scrap-btn {
    background: #fff1f0;
    color: #ff4d4f;
  }
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
  width: 90%;
  max-height: 85vh;
  background: #ffffff;
  border-radius: 16rpx;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &.large-modal {
    width: 95%;
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.modal-close {
  font-size: 40rpx;
  color: #999;
  line-height: 1;
}

.modal-body {
  flex: 1;
  padding: 30rpx;
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  gap: 20rpx;
  padding: 20rpx 30rpx;
  border-top: 1rpx solid #f0f0f0;
}

.modal-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: 8rpx;
  font-size: 28rpx;
  font-weight: 600;

  &.cancel-btn {
    background: #f5f5f5;
    color: #666;
  }
  
  &.confirm-btn {
    background: #1890ff;
    color: #ffffff;
  }
}

.form-group {
  margin-bottom: 30rpx;
}

.form-label {
  display: block;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 16rpx;
  font-weight: 500;
}

.form-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.add-spare-btn {
  font-size: 26rpx;
  color: #1890ff;
}

.required {
  color: #ff4d4f;
}

.form-input {
  width: 100%;
  height: 80rpx;
  border: 1rpx solid #e8e8e8;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;

  &.small {
    width: calc(50% - 10rpx);
  }
}

.form-textarea {
  width: 100%;
  min-height: 120rpx;
  border: 1rpx solid #e8e8e8;
  border-radius: 8rpx;
  padding: 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
  line-height: 1.6;
}

.picker-input {
  height: 80rpx;
  line-height: 80rpx;
  border: 1rpx solid #e8e8e8;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  color: #333;
}

.upload-section {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.upload-item {
  position: relative;
  width: 160rpx;
  height: 160rpx;
}

.upload-img {
  width: 100%;
  height: 100%;
  border-radius: 8rpx;
}

.delete-btn {
  position: absolute;
  top: -12rpx;
  right: -12rpx;
  width: 40rpx;
  height: 40rpx;
  line-height: 36rpx;
  text-align: center;
  background: #ff4d4f;
  color: #ffffff;
  border-radius: 50%;
  font-size: 28rpx;
}

.upload-btn {
  width: 160rpx;
  height: 160rpx;
  border: 2rpx dashed #d9d9d9;
  border-radius: 8rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.upload-icon {
  font-size: 48rpx;
  color: #d9d9d9;
}

.upload-text {
  font-size: 22rpx;
  color: #999;
  margin-top: 8rpx;
}

.spare-part-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.spare-part-item {
  padding: 20rpx;
  background: #fafafa;
  border-radius: 8rpx;
}

.spare-part-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.spare-part-title {
  font-size: 26rpx;
  color: #666;
  font-weight: 500;
}

.spare-part-delete {
  font-size: 24rpx;
  color: #ff4d4f;
}

.spare-part-row {
  display: flex;
  gap: 20rpx;
  margin-bottom: 12rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

.detail-section {
  margin-bottom: 30rpx;
}

.detail-row {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.detail-label {
  width: 160rpx;
  font-size: 26rpx;
  color: #999;
  flex-shrink: 0;
}

.detail-value {
  flex: 1;
  font-size: 26rpx;
  color: #333;

  &.price {
    color: #ff4d4f;
    font-weight: 600;
  }
}

.detail-type {
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  font-size: 24rpx;

  &.type-1 {
    background: #e6f7ff;
    color: #1890ff;
  }
  
  &.type-2 {
    background: #fff1f0;
    color: #ff4d4f;
  }
  
  &.type-3 {
    background: #f6ffed;
    color: #52c41a;
  }
  
  &.type-4 {
    background: #fff7e6;
    color: #fa8c16;
  }
}

.section-subtitle {
  font-size: 28rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 16rpx;
}

.section-content {
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
}

.photo-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.photo-item {
  width: 180rpx;
  height: 180rpx;
  border-radius: 8rpx;
}

.spare-table {
  border: 1rpx solid #f0f0f0;
  border-radius: 8rpx;
  overflow: hidden;
}

.spare-table-header {
  display: flex;
  background: #fafafa;
}

.spare-th {
  flex: 1;
  padding: 16rpx;
  font-size: 24rpx;
  color: #999;
  text-align: center;
  font-weight: 500;
}

.spare-table-row {
  display: flex;
  border-top: 1rpx solid #f0f0f0;
}

.spare-td {
  flex: 1;
  padding: 16rpx;
  font-size: 24rpx;
  color: #666;
  text-align: center;

  &.price {
    color: #ff4d4f;
    font-weight: 600;
  }
}
</style>
