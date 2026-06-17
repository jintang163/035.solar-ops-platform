<template>
  <view class="grid-dispatch-page">
    <scroll-view scroll-x class="stat-scroll">
      <view class="stat-row">
        <view class="stat-card" v-for="item in statCards" :key="item.key">
          <view class="stat-icon" :style="{background: item.bgColor}">
            <text class="iconfont" :class="item.iconClass"></text>
          </view>
          <view class="stat-content">
            <view class="stat-value" :style="{color: item.color}">{{ item.value }}</view>
            <view class="stat-label">{{ item.label }}</view>
          </view>
        </view>
      </view>
    </scroll-view>

    <view class="filter-bar">
      <picker mode="selector" :range="typeOptions" range-key="label" @change="onTypeChange">
        <view class="filter-item">
          <text>{{ selectedTypeLabel }}</text>
          <text class="arrow">▼</text>
        </view>
      </picker>
      <picker mode="selector" :range="statusOptions" range-key="label" @change="onStatusChange">
        <view class="filter-item">
          <text>{{ selectedStatusLabel }}</text>
          <text class="arrow">▼</text>
        </view>
      </picker>
      <view class="refresh-btn" @click="loadSummary">
        <text class="refresh-icon" :class="{spinning: loading}">⟳</text>
      </view>
    </view>

    <view class="command-list" v-if="list.length > 0">
      <view
        class="command-card"
        v-for="item in list"
        :key="item.id"
        @click="showDetail(item)"
      >
        <view class="card-header">
          <view class="command-no">#{{ item.commandNo }}</view>
          <view class="card-tags">
            <text class="tag" :class="'priority-' + item.priority">{{ priorityDesc(item.priority) }}</text>
            <text class="tag" :class="'status-' + item.status">{{ statusDesc(item.status) }}</text>
          </view>
        </view>

        <view class="card-body">
          <view class="info-row">
            <text class="info-label">类型</text>
            <text class="tag type-tag" :class="'type-' + item.commandType">{{ typeDesc(item.commandType) }}</text>
            <text class="source-tag" :class="'source-' + item.commandSource">{{ sourceDesc(item.commandSource) }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">📍</text>
            <text class="info-value">{{ item.stationName }} {{ item.inverterName ? '- ' + item.inverterName : '' }}</text>
          </view>
          <view class="power-row">
            <view class="power-item target">
              <view class="power-label">目标功率</view>
              <view class="power-value">{{ formatKW(item.targetActivePower) }} kW</view>
            </view>
            <view class="arrow-divider">→</view>
            <view class="power-item actual">
              <view class="power-label">实际功率</view>
              <view class="power-value">{{ formatKW(item.actualActivePower) }} kW</view>
            </view>
          </view>
          <view v-if="item.deviationPercent != null" class="deviation-row">
            <text class="deviation-label">偏差:</text>
            <text class="deviation-value" :class="Math.abs(item.deviationPercent) <= 5 ? 'deviation-good' : 'deviation-bad'">
              {{ item.deviationPercent > 0 ? '+' : '' }}{{ item.deviationPercent.toFixed(2) }}%
            </text>
          </view>
        </view>

        <view class="card-footer">
          <text class="issue-time">🕒 {{ formatTime(item.issueTime) }}</text>
          <text v-if="item.operatorName" class="operator">👤 {{ item.operatorName }}</text>
        </view>
      </view>
      <view v-if="!finished && list.length > 0" class="load-more" @click="loadMore">
        {{ loadingMore ? '加载中...' : '点击加载更多' }}
      </view>
      <view v-if="finished && list.length > 0" class="load-finished">
        没有更多了 ~
      </view>
    </view>

    <view v-else-if="!loading" class="empty-state">
      <text class="empty-icon">📋</text>
      <text class="empty-text">暂无调度指令</text>
    </view>

    <view v-if="loading && list.length === 0" class="loading-state">
      <view class="loading-spinner"></view>
      <text>加载中...</text>
    </view>

    <view v-if="detailVisible" class="detail-modal-mask" @click="closeDetail">
      <view class="detail-modal" @click.stop>
        <view class="modal-header">
          <text class="modal-title">指令详情</text>
          <text class="modal-close" @click="closeDetail">✕</text>
        </view>
        <scroll-view scroll-y class="modal-body">
          <view class="detail-section">
            <view class="detail-title">基本信息</view>
            <view class="detail-grid">
              <view class="detail-item"><text class="label">指令编号</text><text class="value">{{ currentDetail.commandNo }}</text></view>
              <view class="detail-item"><text class="label">指令类型</text><text class="value">{{ typeDesc(currentDetail.commandType) }}</text></view>
              <view class="detail-item"><text class="label">指令来源</text><text class="value">{{ sourceDesc(currentDetail.commandSource) }}</text></view>
              <view class="detail-item"><text class="label">优先级</text><text class="value">{{ priorityDesc(currentDetail.priority) }}</text></view>
              <view class="detail-item"><text class="label">执行状态</text><text class="value">{{ statusDesc(currentDetail.status) }}</text></view>
              <view class="detail-item"><text class="label">偏差</text><text class="value">{{ currentDetail.deviationPercent != null ? currentDetail.deviationPercent.toFixed(2) + '%' : '-' }}</text></view>
            </view>
          </view>
          <view class="detail-section">
            <view class="detail-title">设备信息</view>
            <view class="detail-grid">
              <view class="detail-item"><text class="label">电站</text><text class="value">{{ currentDetail.stationName }}</text></view>
              <view class="detail-item"><text class="label">逆变器</text><text class="value">{{ currentDetail.inverterName || '全站调节' }}</text></view>
            </view>
          </view>
          <view class="detail-section">
            <view class="detail-title">目标与实际</view>
            <view class="detail-grid">
              <view class="detail-item"><text class="label">目标有功</text><text class="value">{{ formatKW(currentDetail.targetActivePower) }} kW</text></view>
              <view class="detail-item"><text class="label">实际有功</text><text class="value">{{ formatKW(currentDetail.actualActivePower) }} kW</text></view>
              <view class="detail-item"><text class="label">目标无功</text><text class="value">{{ formatKW(currentDetail.targetReactivePower) }} kVar</text></view>
              <view class="detail-item"><text class="label">实际无功</text><text class="value">{{ formatKW(currentDetail.actualReactivePower) }} kVar</text></view>
              <view class="detail-item"><text class="label">目标电压</text><text class="value">{{ currentDetail.targetVoltage ? currentDetail.targetVoltage.toFixed(1) : '-' }} V</text></view>
              <view class="detail-item"><text class="label">实际电压</text><text class="value">{{ currentDetail.actualVoltage ? currentDetail.actualVoltage.toFixed(1) : '-' }} V</text></view>
            </view>
          </view>
          <view class="detail-section">
            <view class="detail-title">时间线</view>
            <view class="timeline">
              <view class="timeline-item"><text class="dot"></text><text>下发时间: {{ formatTime(currentDetail.issueTime) }}</text></view>
              <view class="timeline-item" v-if="currentDetail.executeStartTime"><text class="dot blue"></text><text>开始执行: {{ formatTime(currentDetail.executeStartTime) }}</text></view>
              <view class="timeline-item" v-if="currentDetail.executeEndTime"><text class="dot green"></text><text>完成时间: {{ formatTime(currentDetail.executeEndTime) }}</text></view>
              <view class="timeline-item" v-if="currentDetail.failReason"><text class="dot red"></text><text>失败原因: {{ currentDetail.failReason }}</text></view>
            </view>
          </view>
          <view class="detail-section" v-if="currentDetail.remark">
            <view class="detail-title">备注</view>
            <view class="remark-text">{{ currentDetail.remark }}</view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { getDispatchSummary, getDispatchCommands } from '@/api/gridDispatch'

const loading = ref(false)
const loadingMore = ref(false)
const detailVisible = ref(false)
const currentDetail = ref({})
const list = ref([])
const pageNum = ref(1)
const pageSize = ref(20)
const finished = ref(false)
const total = ref(0)

const selectedType = ref(null)
const selectedStatus = ref(null)
const summary = ref({
  totalCommandCount: 0, successCommandCount: 0, failCommandCount: 0,
  pendingCommandCount: 0, executingCommandCount: 0, successRate: 0
})

const typeOptions = [
  { value: null, label: '全部类型' },
  { value: 1, label: '有功调节' }, { value: 2, label: '无功调节' },
  { value: 3, label: '电压调节' }, { value: 4, label: '频率调节' },
  { value: 5, label: '启停逆变器' }
]
const statusOptions = [
  { value: null, label: '全部状态' },
  { value: 0, label: '待执行' }, { value: 1, label: '执行中' },
  { value: 2, label: '成功' }, { value: 3, label: '失败' },
  { value: 4, label: '已取消' }
]

const selectedTypeLabel = computed(() => typeOptions.find(o => o.value === selectedType.value)?.label || '类型')
const selectedStatusLabel = computed(() => statusOptions.find(o => o.value === selectedStatus.value)?.label || '状态')
const statCards = computed(() => [
  { key:'total', label:'总指令', value: summary.value.totalCommandCount||0, color:'#1890ff', bgColor:'#e6f7ff', iconClass:'📋' },
  { key:'success', label:'成功', value: summary.value.successCommandCount||0, color:'#52c41a', bgColor:'#f6ffed', iconClass:'✓' },
  { key:'fail', label:'失败', value: summary.value.failCommandCount||0, color:'#ff4d4f', bgColor:'#fff2f0', iconClass:'✕' },
  { key:'executing', label:'执行中', value: summary.value.executingCommandCount||0, color:'#1890ff', bgColor:'#e6f7ff', iconClass:'⚡' },
  { key:'pending', label:'待执行', value: summary.value.pendingCommandCount||0, color:'#fa8c16', bgColor:'#fff7e6', iconClass:'⏱' },
  { key:'rate', label:'成功率', value:(summary.value.successRate||0).toFixed(1)+'%',
    color:(summary.value.successRate||0)>=90?'#52c41a':(summary.value.successRate||0)>=80?'#fa8c16':'#ff4d4f',
    bgColor:'#f0f5ff', iconClass:'📈' }
])

function typeDesc(type) { const m={1:'有功调节',2:'无功调节',3:'电压调节',4:'频率调节',5:'启停'}; return m[type]||'-' }
function statusDesc(status){ const m={0:'待执行',1:'执行中',2:'成功',3:'失败',4:'已取消',5:'超时'}; return m[status]||'-' }
function priorityDesc(p){ const m={1:'紧急',2:'高',3:'普通',4:'低'}; return m[p]||'普通' }
function sourceDesc(s){ const m={1:'调度下发',2:'人工干预',3:'自动调节'}; return m[s]||'-' }
function formatKW(v){ if(v==null)return '-'; return Number(v).toLocaleString('zh-CN',{maximumFractionDigits:1}) }
function formatTime(t){ if(!t)return '-'; const d=new Date(t); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}` }

async function loadSummary() {
  try {
    const res = await getDispatchSummary()
    if (res && res.code === 0) summary.value = res.data || {}
  } catch (e) { console.error(e) }
}

async function loadList(reset = false) {
  if (reset) { pageNum.value = 1; finished.value = false; list.value = [] }
  if (finished.value) return
  loadingMore.value = !reset
  loading.value = reset
  try {
    const params = {
      pageNum: pageNum.value, pageSize: pageSize.value,
      commandType: selectedType.value, status: selectedStatus.value
    }
    const res = await getDispatchCommands(params)
    if (res && res.code === 0) {
      const data = res.data || {}
      list.value = reset ? (data.list || []) : list.value.concat(data.list || [])
      total.value = data.total || 0
      finished.value = list.value.length >= total.value || (data.list || []).length < pageSize.value
      pageNum.value++
    }
  } catch (e) { console.error(e) } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function onTypeChange(e){ selectedType.value = typeOptions[e.detail.value].value; loadList(true); }
function onStatusChange(e){ selectedStatus.value = statusOptions[e.detail.value].value; loadList(true); }
function loadMore(){ loadList(false) }
function showDetail(item){ currentDetail.value = item; detailVisible.value = true }
function closeDetail(){ detailVisible.value = false }

onMounted(() => { loadSummary(); loadList(true) })
onPullDownRefresh(() => { loadSummary(); loadList(true); uni.stopPullDownRefresh() })
onReachBottom(() => { if(!finished.value) loadList(false) })
</script>

<style lang="scss" scoped>
.grid-dispatch-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 40rpx;
}
.stat-scroll { white-space: nowrap; padding: 24rpx 24rpx 0; }
.stat-row { display: inline-flex; gap: 20rpx; }
.stat-card {
  display: inline-flex; align-items: center;
  background: #fff; border-radius: 16rpx; padding: 24rpx 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04); min-width: 200rpx;
}
.stat-icon {
  width: 72rpx; height: 72rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 32rpx; margin-right: 20rpx;
}
.stat-value { font-size: 40rpx; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 22rpx; color: #8c8c8c; margin-top: 6rpx; }

.filter-bar {
  display: flex; padding: 24rpx; gap: 16rpx; align-items: center;
}
.filter-item {
  flex: 1; background: #fff; border-radius: 12rpx; padding: 16rpx 24rpx;
  font-size: 26rpx; color: #333; display: flex; justify-content: space-between;
}
.arrow { font-size: 20rpx; color: #8c8c8c; margin-left: 10rpx; }
.refresh-btn {
  width: 72rpx; height: 72rpx; background: #fff; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 36rpx; color: #1890ff;
}
.refresh-icon.spinning { animation: spin 1s linear infinite; display: inline-block; }
@keyframes spin { to { transform: rotate(360deg) } }

.command-list { padding: 0 24rpx; }
.command-card {
  background: #fff; border-radius: 20rpx; margin-bottom: 24rpx; overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.card-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20rpx 24rpx; background: linear-gradient(135deg, #f0f5ff, #e6f7ff);
}
.command-no { font-size: 26rpx; font-weight: 600; color: #1890ff; font-family: monospace; }
.card-tags { display: flex; gap: 12rpx; }
.tag {
  padding: 6rpx 16rpx; border-radius: 100rpx; font-size: 22rpx; line-height: 1.4;
}
.status-0, .status-4 { background: #fafafa; color: #8c8c8c; }
.status-1 { background: #e6f7ff; color: #1890ff; }
.status-2 { background: #f6ffed; color: #52c41a; }
.status-3 { background: #fff2f0; color: #ff4d4f; }
.status-5 { background: #fff7e6; color: #fa8c16; }
.priority-1 { background: #fff2f0; color: #ff4d4f; font-weight: 600; }
.priority-2 { background: #fff7e6; color: #fa8c16; font-weight: 600; }
.priority-3 { background: #e6f7ff; color: #1890ff; }
.priority-4 { background: #fafafa; color: #8c8c8c; }
.type-tag { font-weight: 500; }
.type-1 { background: #e6f7ff; color: #1890ff; }
.type-2 { background: #f9f0ff; color: #722ed1; }
.type-3 { background: #fff7e6; color: #fa8c16; }
.type-4 { background: #f6ffed; color: #52c41a; }
.type-5 { background: #fff0f6; color: #eb2f96; }
.source-tag { padding: 6rpx 16rpx; border-radius: 100rpx; font-size: 22rpx; }
.source-1 { background: #f0f5ff; color: #2f54eb; }
.source-2 { background: #fff7e6; color: #fa8c16; }
.source-3 { background: #f6ffed; color: #52c41a; }

.card-body { padding: 24rpx; }
.info-row {
  display: flex; align-items: center; margin-bottom: 16rpx;
  font-size: 26rpx; color: #555; gap: 12rpx;
}
.info-label { color: #8c8c8c; width: 80rpx; }
.info-value { flex: 1; color: #333; }
.power-row {
  display: flex; align-items: center; justify-content: space-between;
  background: #fafafa; border-radius: 12rpx; padding: 20rpx; margin: 16rpx 0;
}
.power-item { text-align: center; flex: 1; }
.power-label { font-size: 22rpx; color: #8c8c8c; margin-bottom: 8rpx; }
.power-value { font-size: 32rpx; font-weight: 700; }
.power-item.target .power-value { color: #1890ff; }
.power-item.actual .power-value { color: #52c41a; }
.arrow-divider { font-size: 32rpx; color: #d9d9d9; }

.deviation-row { display: flex; align-items: center; gap: 10rpx; font-size: 24rpx; }
.deviation-label { color: #8c8c8c; }
.deviation-good { color: #52c41a; font-weight: 600; }
.deviation-bad { color: #ff4d4f; font-weight: 600; }

.card-footer {
  display: flex; justify-content: space-between; padding: 16rpx 24rpx;
  background: #fafafa; font-size: 22rpx; color: #8c8c8c;
}

.load-more, .load-finished {
  text-align: center; padding: 32rpx; font-size: 24rpx; color: #8c8c8c;
}

.empty-state, .loading-state {
  padding: 200rpx 0; text-align: center;
}
.empty-icon { font-size: 120rpx; display: block; margin-bottom: 20rpx; opacity: 0.6; }
.empty-text { color: #8c8c8c; font-size: 28rpx; }
.loading-spinner {
  width: 60rpx; height: 60rpx; margin: 0 auto 20rpx;
  border: 6rpx solid #e6f7ff; border-top-color: #1890ff;
  border-radius: 50%; animation: spin 1s linear infinite;
}

.detail-modal-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: flex-end; z-index: 999;
}
.detail-modal {
  width: 100%; max-height: 85vh; background: #fff;
  border-radius: 32rpx 32rpx 0 0; overflow: hidden;
}
.modal-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 32rpx; border-bottom: 1rpx solid #f0f0f0;
}
.modal-title { font-size: 34rpx; font-weight: 600; color: #262626; }
.modal-close { font-size: 36rpx; color: #8c8c8c; padding: 0 16rpx; }
.modal-body { max-height: calc(85vh - 100rpx); padding: 0 32rpx 32rpx; }
.detail-section { margin-top: 32rpx; }
.detail-title {
  font-size: 28rpx; font-weight: 600; color: #262626; margin-bottom: 20rpx;
  padding-left: 16rpx; border-left: 6rpx solid #1890ff;
}
.detail-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 16rpx;
}
.detail-item {
  background: #fafafa; border-radius: 12rpx; padding: 16rpx;
  display: flex; flex-direction: column; gap: 8rpx;
}
.detail-item .label { font-size: 22rpx; color: #8c8c8c; }
.detail-item .value { font-size: 26rpx; color: #262626; font-weight: 500; }

.timeline { padding-left: 20rpx; border-left: 4rpx solid #e8e8e8; }
.timeline-item {
  position: relative; padding: 16rpx 0 16rpx 24rpx; font-size: 24rpx; color: #555;
}
.timeline-item .dot {
  position: absolute; left: -30rpx; top: 22rpx;
  width: 16rpx; height: 16rpx; border-radius: 50%; background: #bfbfbf;
}
.timeline-item .dot.blue { background: #1890ff; }
.timeline-item .dot.green { background: #52c41a; }
.timeline-item .dot.red { background: #ff4d4f; }

.remark-text {
  background: #fafafa; border-radius: 12rpx; padding: 20rpx;
  font-size: 26rpx; color: #595959; line-height: 1.6;
}
</style>
