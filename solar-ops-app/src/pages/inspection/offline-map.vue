<template>
  <view class="tile-page">
    <view class="page-header">
      <text class="header-title">离线地图管理</text>
      <text class="header-tip">下载瓦片后可无网查看电站地图</text>
    </view>

    <view class="station-list">
      <view class="station-card" v-for="station in stations" :key="station.id">
        <view class="station-info">
          <view class="station-icon">
            <text class="icon-text">⚡</text>
          </view>
          <view class="station-main">
            <text class="station-name">{{ station.stationName }}</text>
            <view class="station-meta">
              <text class="meta-text">{{ station.address }}</text>
            </view>
            <view class="tile-info" v-if="getTileInfo(station.id)">
              <text class="tile-text">
                已下载 {{ getTileInfo(station.id).totalTiles }} 张瓦片
              </text>
              <text class="tile-time">
                更新于 {{ formatTime(getTileInfo(station.id).updateTime) }}
              </text>
            </view>
          </view>
        </view>

        <view class="station-actions">
          <view 
            class="action-btn download" 
            v-if="!getTileInfo(station.id) && !downloading[station.id]"
            @click="startDownload(station)"
          >
            <text class="btn-text">下载</text>
          </view>

          <view class="download-progress" v-else-if="downloading[station.id]">
            <view class="progress-bar">
              <view 
                class="progress-fill" 
                :style="{ width: downloadProgress[station.id] + '%' }"
              ></view>
            </view>
            <text class="progress-text">{{ downloadProgress[station.id] }}%</text>
          </view>

          <view 
            class="action-btn redownload" 
            v-else-if="getTileInfo(station.id)"
            @click="startDownload(station)"
          >
            <text class="btn-text">更新</text>
          </view>

          <view 
            class="action-btn view" 
            v-if="getTileInfo(station.id)"
            @click="viewMap(station)"
          >
            <text class="btn-text">查看</text>
          </view>

          <view 
            class="action-btn delete" 
            v-if="getTileInfo(station.id)"
            @click="deleteTiles(station)"
          >
            <text class="btn-icon">🗑</text>
          </view>
        </view>
      </view>
    </view>

    <view class="empty-tip" v-if="stations.length === 0">
      <text class="empty-icon">📋</text>
      <text class="empty-text">暂无电站数据</text>
    </view>

    <view class="map-view" v-if="showMap">
      <view class="map-mask" @click="closeMap"></view>
      <view class="map-panel">
        <view class="map-panel-header">
          <text class="panel-title">{{ currentStation?.stationName }}</text>
          <text class="close-btn" @click="closeMap">×</text>
        </view>
        <view class="map-panel-body">
          <offline-map
            v-if="currentStation"
            :station-id="currentStation.id"
            :station-name="currentStation.stationName"
            :center-lng="currentStation.longitude"
            :center-lat="currentStation.latitude"
          />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import mapTileUtil from '@/utils/map-tile-util.js'
import OfflineMap from '@/components/offline-map/offline-map.vue'

const stations = ref([])
const tileInfoMap = ref({})
const downloading = ref({})
const downloadProgress = ref({})
const showMap = ref(false)
const currentStation = ref(null)

function loadTileInfo() {
  const all = mapTileUtil.getAllOfflineStations()
  const map = {}
  all.forEach((info) => {
    map[info.stationId] = info
  })
  tileInfoMap.value = map
}

function getTileInfo(stationId) {
  return tileInfoMap.value[stationId] || null
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const pad = (n) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function loadStations() {
  const mockStations = [
    {
      id: 1,
      stationName: '北京朝阳光伏电站',
      address: '北京市朝阳区xx路xx号',
      longitude: 116.455144,
      latitude: 39.938977,
      bounds: { west: 116.40, south: 39.90, east: 116.50, north: 39.98 }
    },
    {
      id: 2,
      stationName: '上海浦东光伏电站',
      address: '上海市浦东新区xx路xx号',
      longitude: 121.544711,
      latitude: 31.229817,
      bounds: { west: 121.48, south: 31.18, east: 121.60, north: 31.28 }
    },
    {
      id: 3,
      stationName: '深圳南山光伏电站',
      address: '深圳市南山区xx路xx号',
      longitude: 113.930172,
      latitude: 22.533266,
      bounds: { west: 113.88, south: 22.48, east: 113.98, north: 22.58 }
    }
  ]

  stations.value = mockStations
}

async function startDownload(station) {
  if (downloading.value[station.id]) return

  uni.showModal({
    title: '确认下载',
    content: `将下载 ${station.stationName} 的地图瓦片，是否继续？`,
    success: async (res) => {
      if (res.confirm) {
        downloading.value[station.id] = true
        downloadProgress.value[station.id] = 0

        try {
          await mapTileUtil.downloadStationTiles(
            station.id,
            station.stationName,
            station.bounds,
            {
              minZoom: 12,
              maxZoom: 17,
              onProgress: (p) => {
                downloadProgress.value[station.id] = p.percent
              }
            }
          )

          uni.showToast({ title: '下载完成', icon: 'success' })
          loadTileInfo()
        } catch (e) {
          console.error('下载失败:', e)
          uni.showToast({ title: '下载失败', icon: 'none' })
        } finally {
          downloading.value[station.id] = false
        }
      }
    }
  })
}

function viewMap(station) {
  currentStation.value = station
  showMap.value = true
}

function closeMap() {
  showMap.value = false
  currentStation.value = null
}

async function deleteTiles(station) {
  uni.showModal({
    title: '确认删除',
    content: `确定删除 ${station.stationName} 的离线地图数据吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await mapTileUtil.deleteStationTiles(station.id)
          loadTileInfo()
          uni.showToast({ title: '已删除', icon: 'success' })
        } catch (e) {
          uni.showToast({ title: '删除失败', icon: 'none' })
        }
      }
    }
  })
}

onMounted(() => {
  loadStations()
  loadTileInfo()
})
</script>

<style lang="scss" scoped>
.tile-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.page-header {
  padding: 40rpx 30rpx 30rpx;
  background: linear-gradient(135deg, #1890ff, #096dd9);
}

.header-title {
  display: block;
  font-size: 36rpx;
  font-weight: 600;
  color: #fff;
  margin-bottom: 10rpx;
}

.header-tip {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
}

.station-list {
  padding: 20rpx 30rpx;
}

.station-card {
  background-color: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.station-info {
  display: flex;
  margin-bottom: 24rpx;
}

.station-icon {
  width: 80rpx;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  background: linear-gradient(135deg, #1890ff, #52c41a);
  border-radius: 16rpx;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.icon-text {
  font-size: 36rpx;
}

.station-main {
  flex: 1;
  min-width: 0;
}

.station-name {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 8rpx;
}

.station-meta {
  margin-bottom: 8rpx;
}

.meta-text {
  font-size: 24rpx;
  color: #999;
}

.tile-info {
  background-color: #f6ffed;
  padding: 12rpx 16rpx;
  border-radius: 8rpx;
}

.tile-text {
  display: block;
  font-size: 22rpx;
  color: #52c41a;
  margin-bottom: 4rpx;
}

.tile-time {
  font-size: 20rpx;
  color: #999;
}

.station-actions {
  display: flex;
  align-items: center;
  gap: 16rpx;
  border-top: 1rpx solid #f0f0f0;
  padding-top: 20rpx;
}

.action-btn {
  padding: 14rpx 36rpx;
  border-radius: 30rpx;
  text-align: center;

  &.download {
    background-color: #1890ff;
  }

  &.redownload {
    background-color: #fa8c16;
  }

  &.view {
    background-color: #52c41a;
  }

  &.delete {
    background-color: #fff1f0;
    padding: 14rpx 24rpx;
  }
}

.btn-text {
  font-size: 26rpx;
  color: #fff;
}

.delete .btn-icon {
  font-size: 26rpx;
  color: #ff4d4f;
}

.download-progress {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.progress-bar {
  flex: 1;
  height: 16rpx;
  background-color: #f0f0f0;
  border-radius: 8rpx;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #1890ff, #52c41a);
  border-radius: 8rpx;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 24rpx;
  color: #1890ff;
  font-weight: 500;
  min-width: 80rpx;
  text-align: right;
}

.empty-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 24rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}

.map-view {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 100;
}

.map-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
}

.map-panel {
  position: absolute;
  top: 50%;
  left: 30rpx;
  right: 30rpx;
  transform: translateY(-50%);
  height: 80vh;
  background-color: #fff;
  border-radius: 20rpx;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.map-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.panel-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.close-btn {
  font-size: 48rpx;
  color: #999;
  line-height: 1;
  padding: 0 10rpx;
}

.map-panel-body {
  flex: 1;
  overflow: hidden;
}
</style>
