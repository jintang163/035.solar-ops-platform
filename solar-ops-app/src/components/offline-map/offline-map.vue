<template>
  <view class="offline-map-wrap">
    <view class="map-header">
      <text class="map-title">{{ stationName || '离线地图' }}</text>
      <view class="map-status" :class="{ online: isOnline }">
        <text class="status-dot"></text>
        <text class="status-text">{{ isOnline ? '在线' : '离线' }}</text>
      </view>
    </view>

    <view class="map-container" @touchstart="onTouchStart" @touchmove="onTouchMove" @touchend="onTouchEnd">
      <view class="tiles-layer" :style="tilesStyle">
        <image
          v-for="tile in visibleTiles"
          :key="tile.key"
          :src="tile.url"
          class="map-tile"
          :style="{
            left: tile.left + 'px',
            top: tile.top + 'px',
            width: tileSize + 'px',
            height: tileSize + 'px'
          }"
          mode="aspectFill"
        />
      </view>

      <view class="markers-layer">
        <view
          v-for="marker in markerList"
          :key="marker.id"
          class="map-marker"
          :class="marker.type || 'normal'"
          :style="{
            left: marker.left + 'px',
            top: marker.top + 'px'
          }"
          @click.stop="onMarkerClick(marker)"
        >
          <text class="marker-icon">{{ marker.icon || '📍' }}</text>
          <view class="marker-label" v-if="marker.label">{{ marker.label }}</view>
        </view>
      </view>

      <view class="map-controls">
        <view class="control-btn" @click="zoomIn">
          <text class="control-icon">+</text>
        </view>
        <view class="control-btn" @click="zoomOut">
          <text class="control-icon">−</text>
        </view>
        <view class="control-btn" @click="resetView">
          <text class="control-icon">⌖</text>
        </view>
      </view>

      <view class="zoom-info">
        <text>缩放: {{ currentZoom }}</text>
      </view>
    </view>

    <view class="map-legend" v-if="showLegend">
      <view class="legend-item">
        <text class="legend-dot normal"></text>
        <text class="legend-text">设备正常</text>
      </view>
      <view class="legend-item">
        <text class="legend-dot warning"></text>
        <text class="legend-text">设备异常</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import mapTileUtil from '@/utils/map-tile-util.js'

const props = defineProps({
  stationId: {
    type: [Number, String],
    required: true
  },
  stationName: {
    type: String,
    default: ''
  },
  centerLng: {
    type: Number,
    default: 116.397428
  },
  centerLat: {
    type: Number,
    default: 39.90923
  },
  showLegend: {
    type: Boolean,
    default: true
  },
  markers: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['markerClick', 'ready'])

const tileSize = 256
const minZoom = 10
const maxZoom = 18
const currentZoom = ref(14)
const centerX = ref(0)
const centerY = ref(0)
const isOnline = ref(true)
const containerWidth = ref(0)
const containerHeight = ref(0)

let touchStartX = 0
let touchStartY = 0
let startCenterX = 0
let startCenterY = 0
let lastTouchDistance = 0

const tilesStyle = computed(() => {
  return {
    transform: `translate(${-centerX.value}px, ${-centerY.value}px)`,
    transition: 'transform 0.1s ease-out'
  }
})

const visibleTiles = computed(() => {
  const tiles = []
  const z = currentZoom.value
  const n = Math.pow(2, z)
  const pixelX = lngToPixel(props.centerLng, z) + centerX.value - containerWidth.value / 2
  const pixelY = latToPixel(props.centerLat, z) + centerY.value - containerHeight.value / 2

  const startTileX = Math.max(0, Math.floor(pixelX / tileSize) - 1)
  const endTileX = Math.min(n - 1, Math.ceil((pixelX + containerWidth.value) / tileSize) + 1)
  const startTileY = Math.max(0, Math.floor(pixelY / tileSize) - 1)
  const endTileY = Math.min(n - 1, Math.ceil((pixelY + containerHeight.value) / tileSize) + 1)

  const hasOffline = mapTileUtil.hasOfflineTiles(props.stationId)

  for (let x = startTileX; x <= endTileX; x++) {
    for (let y = startTileY; y <= endTileY; y++) {
      const left = x * tileSize - pixelX + containerWidth.value / 2 - centerX.value
      const top = y * tileSize - pixelY + containerHeight.value / 2 - centerY.value

      let url = ''
      let isOffline = false

      if (hasOffline && mapTileUtil.isTileInRange(props.stationId, x, y, z)) {
        const localUrl = mapTileUtil.getTileUrl(props.stationId, x, y, z)
        if (localUrl) {
          url = localUrl
          isOffline = true
        }
      }

      if (!url && isOnline.value) {
        url = `https://webrd0${((x + y) % 4) + 1}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x=${x}&y=${y}&z=${z}`
      }

      if (url) {
        tiles.push({
          key: `${z}_${x}_${y}`,
          x,
          y,
          z,
          left,
          top,
          url,
          isOffline
        })
      }
    }
  }

  return tiles
})

const markerList = computed(() => {
  if (!props.markers || props.markers.length === 0) return []
  
  const z = currentZoom.value
  const pixelX = lngToPixel(props.centerLng, z) + centerX.value - containerWidth.value / 2
  const pixelY = latToPixel(props.centerLat, z) + centerY.value - containerHeight.value / 2

  return props.markers.map((marker) => {
    const x = lngToPixel(marker.lng, z) - pixelX + containerWidth.value / 2 - centerX.value
    const y = latToPixel(marker.lat, z) - pixelY + containerHeight.value / 2 - centerY.value
    return {
      ...marker,
      left: x,
      top: y
    }
  })
})

function lngToPixel(lng, zoom) {
  const n = Math.pow(2, zoom)
  return ((lng + 180) / 360) * n * tileSize
}

function latToPixel(lat, zoom) {
  const n = Math.pow(2, zoom)
  const latRad = (lat * Math.PI) / 180
  return (1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * n * tileSize
}

function pixelToLng(x, zoom) {
  const n = Math.pow(2, zoom)
  return (x / (n * tileSize)) * 360 - 180
}

function pixelToLat(y, zoom) {
  const n = Math.pow(2, zoom)
  const tileY = y / tileSize
  const n1 = Math.PI - 2 * Math.PI * tileY / n
  return (180 / Math.PI) * Math.atan(0.5 * (Math.exp(n1) - Math.exp(-n1)))
}

function onTouchStart(e) {
  if (e.touches.length === 1) {
    touchStartX = e.touches[0].clientX
    touchStartY = e.touches[0].clientY
    startCenterX = centerX.value
    startCenterY = centerY.value
  } else if (e.touches.length === 2) {
    const dx = e.touches[0].clientX - e.touches[1].clientX
    const dy = e.touches[0].clientY - e.touches[1].clientY
    lastTouchDistance = Math.sqrt(dx * dx + dy * dy)
  }
}

function onTouchMove(e) {
  if (e.touches.length === 1) {
    const dx = e.touches[0].clientX - touchStartX
    const dy = e.touches[0].clientY - touchStartY
    centerX.value = startCenterX - dx
    centerY.value = startCenterY - dy
  } else if (e.touches.length === 2) {
    const dx = e.touches[0].clientX - e.touches[1].clientX
    const dy = e.touches[0].clientY - e.touches[1].clientY
    const distance = Math.sqrt(dx * dx + dy * dy)

    if (lastTouchDistance > 0) {
      const scale = distance / lastTouchDistance
      const delta = scale > 1 ? 1 : -1
      const newZoom = Math.max(minZoom, Math.min(maxZoom, currentZoom.value + delta * 0.1))
      currentZoom.value = Math.round(newZoom)
    }
    lastTouchDistance = distance
  }
}

function onTouchEnd() {
  lastTouchDistance = 0
}

function zoomIn() {
  if (currentZoom.value < maxZoom) {
    currentZoom.value++
  }
}

function zoomOut() {
  if (currentZoom.value > minZoom) {
    currentZoom.value--
  }
}

function resetView() {
  centerX.value = 0
  centerY.value = 0
  currentZoom.value = 14
}

function onMarkerClick(marker) {
  emit('markerClick', marker)
}

function checkNetwork() {
  uni.getNetworkType({
    success: (res) => {
      isOnline.value = res.networkType !== 'none'
    }
  })
}

onMounted(() => {
  const query = uni.createSelectorQuery()
  query.select('.map-container').boundingClientRect((rect) => {
    if (rect) {
      containerWidth.value = rect.width
      containerHeight.value = rect.height
    }
  }).exec()

  checkNetwork()
  uni.onNetworkStatusChange((res) => {
    isOnline.value = res.isConnected
  })

  emit('ready', {
    zoomIn,
    zoomOut,
    resetView,
    getCenter: () => ({
      lng: pixelToLng(lngToPixel(props.centerLng, currentZoom.value) + centerX.value, currentZoom.value),
      lat: pixelToLat(latToPixel(props.centerLat, currentZoom.value) + centerY.value, currentZoom.value)
    })
  })
})
</script>

<style lang="scss" scoped>
.offline-map-wrap {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #e8e8e8;
}

.map-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 30rpx;
  background-color: #fff;
  border-bottom: 1rpx solid #f0f0f0;
  z-index: 10;
}

.map-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.map-status {
  display: flex;
  align-items: center;
  padding: 8rpx 20rpx;
  background-color: #fff1f0;
  border-radius: 20rpx;

  &.online {
    background-color: #f6ffed;
  }
}

.status-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background-color: #ff4d4f;
  margin-right: 10rpx;

  .online & {
    background-color: #52c41a;
  }
}

.status-text {
  font-size: 24rpx;
  color: #ff4d4f;

  .online & {
    color: #52c41a;
  }
}

.map-container {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: repeating-linear-gradient(
    45deg,
    #f0f0f0,
    #f0f0f0 10px,
    #e8e8e8 10px,
    #e8e8e8 20px
  );
}

.tiles-layer {
  position: absolute;
  top: 0;
  left: 0;
  will-change: transform;
}

.map-tile {
  position: absolute;
  pointer-events: none;
}

.map-controls {
  position: absolute;
  right: 24rpx;
  bottom: 160rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  z-index: 5;
}

.control-btn {
  width: 72rpx;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  background-color: rgba(255, 255, 255, 0.95);
  border-radius: 50%;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.15);
}

.control-icon {
  font-size: 36rpx;
  color: #333;
  font-weight: 300;
}

.zoom-info {
  position: absolute;
  left: 24rpx;
  bottom: 160rpx;
  padding: 10rpx 20rpx;
  background-color: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 22rpx;
  border-radius: 8rpx;
  z-index: 5;
}

.map-legend {
  position: absolute;
  left: 24rpx;
  bottom: 40rpx;
  display: flex;
  gap: 30rpx;
  padding: 16rpx 24rpx;
  background-color: rgba(255, 255, 255, 0.95);
  border-radius: 12rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
  z-index: 5;
}

.legend-item {
  display: flex;
  align-items: center;
}

.legend-dot {
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  margin-right: 10rpx;

  &.normal {
    background-color: #52c41a;
  }

  &.warning {
    background-color: #ff4d4f;
  }
}

.legend-text {
  font-size: 22rpx;
  color: #666;
}

.markers-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 10;
}

.map-marker {
  position: absolute;
  transform: translate(-50%, -100%);
  pointer-events: auto;
  display: flex;
  flex-direction: column;
  align-items: center;

  &.normal .marker-icon {
    color: #1890ff;
  }

  &.warning .marker-icon {
    color: #fa8c16;
  }

  &.danger .marker-icon {
    color: #ff4d4f;
  }
}

.marker-icon {
  font-size: 40rpx;
  line-height: 1;
  filter: drop-shadow(0 2rpx 4rpx rgba(0, 0, 0, 0.2));
}

.marker-label {
  margin-top: 4rpx;
  padding: 4rpx 12rpx;
  background-color: rgba(0, 0, 0, 0.7);
  color: #fff;
  font-size: 20rpx;
  border-radius: 6rpx;
  white-space: nowrap;
}
</style>
