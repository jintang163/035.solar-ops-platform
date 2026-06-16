<template>
  <view class="knowledge-page">
    <view class="search-header">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          v-model="searchKeyword"
          placeholder="搜索故障码、名称、描述、标签"
          confirm-type="search"
          @confirm="handleSearch"
          @input="handleInputChange"
        />
        <text v-if="searchKeyword" class="search-clear" @click="clearSearch">✕</text>
      </view>
    </view>

    <view class="filter-bar" v-if="showFilter">
      <scroll-view scroll-x class="filter-scroll">
        <view class="filter-tags">
          <view
            class="filter-tag"
            :class="{ active: !filterLevel }"
            @click="setLevel(null)"
          >
            全部级别
          </view>
          <view
            v-for="item in levelOptions"
            :key="item.value"
            class="filter-tag"
            :class="{ active: filterLevel === item.value }"
            :style="{ borderColor: filterLevel === item.value ? item.color : '#ddd', color: filterLevel === item.value ? item.color : '#666' }"
            @click="setLevel(item.value)"
          >
            {{ item.label }}
          </view>
        </view>
      </scroll-view>
    </view>

    <view class="stats-bar" v-if="searchKeyword || filterLevel">
      <text class="stats-text">找到 {{ total }} 条相关知识</text>
      <text class="stats-hint" v-if="!searchKeyword">基于ES + TF-IDF智能搜索</text>
    </view>

    <view class="knowledge-list" v-if="list.length > 0">
      <view
        v-for="item in list"
        :key="item.id"
        class="knowledge-card"
        @click="goDetail(item)"
      >
        <view class="card-header">
          <view class="card-title-row">
            <text class="fault-code" :style="{ fontFamily: 'monospace' }">{{ item.faultCode }}</text>
            <view class="level-tag" :style="{ background: getLevelColor(item.faultLevel) + '20', color: getLevelColor(item.faultLevel) }">
              {{ getLevelText(item.faultLevel) }}
            </view>
            <view v-if="item.faultType" class="type-tag">{{ item.faultType }}</view>
          </view>
          <text class="fault-name">{{ item.faultName }}</text>
        </view>

        <view class="card-tags" v-if="item.tags">
          <text
            v-for="tag in parseTags(item.tags).slice(0, 4)"
            :key="tag"
            class="tag-item"
          >
            #{{ tag }}
          </text>
        </view>

        <view class="card-desc" v-if="item.faultDesc">
          <text class="desc-label">故障描述：</text>
          <text class="desc-text">{{ truncate(item.faultDesc, 60) }}</text>
        </view>

        <view class="card-footer">
          <view class="footer-stats">
            <view class="stat-item">
              <text class="stat-icon" style="color: #52c41a">👍</text>
              <text class="stat-num">{{ item.likeCount || 0 }}</text>
            </view>
            <view class="stat-item">
              <text class="stat-icon" style="color: #ff7a45">👎</text>
              <text class="stat-num">{{ item.dislikeCount || 0 }}</text>
            </view>
            <view class="stat-item">
              <text class="stat-icon" style="color: #1890ff">📋</text>
              <text class="stat-num">{{ item.useCount || 0 }}使用</text>
            </view>
            <view class="stat-item">
              <text class="stat-icon" style="color: #722ed1">👁</text>
              <text class="stat-num">{{ item.viewCount || 0 }}</text>
            </view>
          </view>
          <text class="footer-arrow">›</text>
        </view>
      </view>
    </view>

    <view class="empty-state" v-else-if="!loading">
      <text class="empty-icon">📚</text>
      <text class="empty-text">{{ searchKeyword ? '未找到相关知识' : '暂无知识库数据' }}</text>
      <text class="empty-hint" v-if="searchKeyword">试试其他关键词</text>
    </view>

    <view class="loading-state" v-if="loading">
      <text class="loading-text">加载中...</text>
    </view>

    <view class="load-more" v-if="list.length > 0 && !noMore && !loading">
      <text @click="loadMore">加载更多</text>
    </view>
    <view class="no-more" v-if="noMore && list.length > 0">
      <text>没有更多了</text>
    </view>

    <view class="bottom-space"></view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { onReachBottom } from '@dcloudio/uni-app'
import { getKnowledgePage } from '@/api/knowledge'

const loading = ref(false)
const noMore = ref(false)
const list = ref([])
const total = ref(0)
const searchKeyword = ref('')
const filterLevel = ref(null)
const showFilter = ref(true)
let searchTimer = null

const pageInfo = reactive({
  pageNum: 1,
  pageSize: 10
})

const levelOptions = [
  { value: 1, label: '低级', color: '#52c41a' },
  { value: 2, label: '中级', color: '#faad14' },
  { value: 3, label: '高级', color: '#ff4d4f' },
  { value: 4, label: '紧急', color: '#cf1322' }
]

function getLevelColor(level) {
  const map = { 1: '#52c41a', 2: '#faad14', 3: '#ff4d4f', 4: '#cf1322' }
  return map[level] || '#999'
}

function getLevelText(level) {
  const map = { 1: '低级', 2: '中级', 3: '高级', 4: '紧急' }
  return map[level] || '未知'
}

function parseTags(tags) {
  if (!tags) return []
  return tags.split(',').map(t => t.trim()).filter(Boolean)
}

function truncate(text, max) {
  if (!text) return ''
  return text.length > max ? text.slice(0, max) + '...' : text
}

function handleInputChange() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    pageInfo.pageNum = 1
    list.value = []
    noMore.value = false
    fetchList()
  }, 500)
}

function handleSearch() {
  pageInfo.pageNum = 1
  list.value = []
  noMore.value = false
  fetchList()
}

function clearSearch() {
  searchKeyword.value = ''
  pageInfo.pageNum = 1
  list.value = []
  noMore.value = false
  fetchList()
}

function setLevel(level) {
  filterLevel.value = level
  pageInfo.pageNum = 1
  list.value = []
  noMore.value = false
  fetchList()
}

async function fetchList() {
  if (loading.value) return
  loading.value = true
  try {
    const params = {
      pageNum: pageInfo.pageNum,
      pageSize: pageInfo.pageSize,
      status: 1
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    if (filterLevel.value) {
      params.faultLevel = filterLevel.value
    }
    const res = await getKnowledgePage(params)
    const newList = res.list || []
    total.value = res.total || 0

    if (pageInfo.pageNum === 1) {
      list.value = newList
    } else {
      list.value = [...list.value, ...newList]
    }

    if (newList.length < pageInfo.pageSize) {
      noMore.value = true
    }
  } catch (e) {
    console.error('获取知识库列表失败', e)
  } finally {
    loading.value = false
  }
}

function loadMore() {
  if (noMore.value || loading.value) return
  pageInfo.pageNum++
  fetchList()
}

function goDetail(item) {
  uni.navigateTo({
    url: `/pages/knowledge/knowledge-detail?id=${item.id}`
  })
}

onReachBottom(() => {
  loadMore()
})

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.knowledge-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.search-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid #eee;
}

.search-box {
  display: flex;
  align-items: center;
  background: #f5f5f5;
  border-radius: 40rpx;
  padding: 16rpx 24rpx;
}

.search-icon {
  font-size: 28rpx;
  margin-right: 12rpx;
  color: #999;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.search-clear {
  font-size: 28rpx;
  color: #999;
  padding: 0 8rpx;
}

.filter-bar {
  background: #fff;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #eee;
}

.filter-scroll {
  white-space: nowrap;
}

.filter-tags {
  display: inline-flex;
  padding: 0 24rpx;
  gap: 16rpx;
}

.filter-tag {
  display: inline-block;
  padding: 10rpx 24rpx;
  border-radius: 30rpx;
  font-size: 24rpx;
  border: 1rpx solid #ddd;
  color: #666;
  background: #fff;

  &.active {
    background: #e6f4ff;
    border-color: #1890ff;
    color: #1890ff;
  }
}

.stats-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 24rpx;
  font-size: 24rpx;
  color: #666;
}

.stats-hint {
  color: #1890ff;
}

.knowledge-list {
  padding: 16rpx 24rpx;
}

.knowledge-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.card-header {
  margin-bottom: 16rpx;
}

.card-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 8rpx;
}

.fault-code {
  font-size: 24rpx;
  color: #1890ff;
  background: #e6f4ff;
  padding: 4rpx 14rpx;
  border-radius: 6rpx;
}

.level-tag {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 6rpx;
}

.type-tag {
  font-size: 22rpx;
  background: #f5f5f5;
  color: #666;
  padding: 4rpx 14rpx;
  border-radius: 6rpx;
}

.fault-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #262626;
  line-height: 1.4;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-bottom: 12rpx;
}

.tag-item {
  font-size: 22rpx;
  color: #5959b3;
  background: #f0f0ff;
  padding: 4rpx 14rpx;
  border-radius: 6rpx;
}

.card-desc {
  margin-bottom: 16rpx;
  font-size: 26rpx;
  line-height: 1.6;
}

.desc-label {
  color: #8c8c8c;
}

.desc-text {
  color: #595959;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16rpx;
  border-top: 1rpx solid #f0f0f0;
}

.footer-stats {
  display: flex;
  gap: 24rpx;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4rpx;
  font-size: 24rpx;
  color: #8c8c8c;
}

.stat-icon {
  font-size: 24rpx;
}

.footer-arrow {
  font-size: 32rpx;
  color: #bfbfbf;
}

.empty-state,
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 40rpx;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #8c8c8c;
  margin-bottom: 12rpx;
}

.empty-hint {
  font-size: 24rpx;
  color: #bfbfbf;
}

.loading-text {
  font-size: 26rpx;
  color: #999;
}

.load-more,
.no-more {
  text-align: center;
  padding: 30rpx;
  font-size: 26rpx;
  color: #999;
}

.bottom-space {
  height: 60rpx;
}
</style>
