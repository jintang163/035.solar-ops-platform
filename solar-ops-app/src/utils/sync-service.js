import inspectionDB from '@/utils/inspection-db.js'
import inspectionApi from '@/api/inspection.js'

class SyncService {
  constructor() {
    this.isSyncing = false
    this.networkStatus = 'unknown'
    this.syncTimer = null
    this.listeners = []
  }

  init() {
    this.initNetworkListener()
    this.startAutoSync()
    return this
  }

  initNetworkListener() {
    uni.getNetworkType({
      success: (res) => {
        this.networkStatus = res.networkType
        if (res.networkType !== 'none') {
          this.trySync()
        }
      }
    })

    uni.onNetworkStatusChange((res) => {
      this.networkStatus = res.networkType
      const isConnected = res.networkType !== 'none'
      this.notifyListeners('networkChange', {
        isConnected,
        networkType: res.networkType
      })

      if (isConnected) {
        this.trySync()
      }
    })
  }

  startAutoSync() {
    if (this.syncTimer) {
      clearInterval(this.syncTimer)
    }
    this.syncTimer = setInterval(() => {
      this.trySync()
    }, 5 * 60 * 1000)
  }

  stopAutoSync() {
    if (this.syncTimer) {
      clearInterval(this.syncTimer)
      this.syncTimer = null
    }
  }

  isOnline() {
    return this.networkStatus !== 'none' && this.networkStatus !== 'unknown'
  }

  async trySync() {
    if (this.isSyncing || !this.isOnline()) {
      return false
    }

    this.isSyncing = true
    this.notifyListeners('syncStart', {})

    try {
      const results = await inspectionDB.getPendingSyncResults()
      const photos = await inspectionDB.getPendingSyncPhotos()
      const audios = await inspectionDB.getPendingSyncAudios()

      const totalCount = results.length + photos.length + audios.length
      
      if (totalCount === 0) {
        this.notifyListeners('syncComplete', { success: true, total: 0 })
        this.isSyncing = false
        return true
      }

      let successCount = 0
      let failCount = 0

      for (const result of results) {
        try {
          await this.syncResult(result)
          successCount++
        } catch (e) {
          console.error('同步结果失败', e)
          failCount++
        }
      }

      for (const photo of photos) {
        try {
          await this.syncPhoto(photo)
          successCount++
        } catch (e) {
          console.error('同步照片失败', e)
          failCount++
        }
      }

      for (const audio of audios) {
        try {
          await this.syncAudio(audio)
          successCount++
        } catch (e) {
          console.error('同步录音失败', e)
          failCount++
        }
      }

      this.notifyListeners('syncComplete', {
        success: failCount === 0,
        total: totalCount,
        successCount,
        failCount
      })

      return failCount === 0
    } catch (e) {
      console.error('同步失败', e)
      this.notifyListeners('syncError', { error: e })
      return false
    } finally {
      this.isSyncing = false
    }
  }

  async syncResult(result) {
    const items = await inspectionDB.getResultItems(result.id)
    const photos = await inspectionDB.getResultPhotos(result.id)
    const audios = await inspectionDB.getResultAudios(result.id)

    const submitData = {
      taskId: result.task_id,
      taskNo: result.task_no,
      stationId: result.station_id,
      stationName: result.station_name,
      startTime: result.start_time,
      endTime: result.end_time,
      overallRemark: result.overall_remark,
      longitude: result.longitude,
      latitude: result.latitude,
      isOffline: result.is_offline,
      items: items.map(item => ({
        taskItemId: item.task_item_id,
        itemId: item.item_id,
        itemName: item.item_name,
        itemType: item.item_type,
        assetId: item.asset_id,
        assetName: item.asset_name,
        assetCode: item.asset_code,
        checkValue: item.check_value,
        standardValue: item.standard_value,
        isNormal: item.is_normal,
        abnormalDesc: item.abnormal_desc,
        remark: item.remark,
        checkTime: item.check_time,
        longitude: item.longitude,
        latitude: item.latitude
      })),
      photos: photos.map(photo => ({
        resultItemId: photo.result_item_id,
        taskId: photo.task_id,
        assetId: photo.asset_id,
        photoType: photo.photo_type,
        photoUrl: photo.photo_url,
        thumbnailUrl: photo.thumbnail_url,
        fileSize: photo.file_size,
        watermarkTime: photo.watermark_time,
        longitude: photo.longitude,
        latitude: photo.latitude,
        hasWatermark: photo.has_watermark,
        remark: photo.remark,
        isOffline: photo.is_offline
      })),
      audios: audios.map(audio => ({
        resultItemId: audio.result_item_id,
        taskId: audio.task_id,
        assetId: audio.asset_id,
        audioUrl: audio.audio_url,
        fileSize: audio.file_size,
        duration: audio.duration,
        recordTime: audio.record_time,
        longitude: audio.longitude,
        latitude: audio.latitude,
        remark: audio.remark,
        isOffline: audio.is_offline
      }))
    }

    await inspectionApi.submitResult(submitData)
    await inspectionDB.markResultSynced(result.id)

    return true
  }

  async syncPhoto(photo) {
    if (!photo.photo_url) {
      await inspectionDB.markPhotoSynced(photo.id)
      return true
    }

    if (photo.photo_url.startsWith('http')) {
      await inspectionDB.markPhotoSynced(photo.id)
      return true
    }

    try {
      const result = await inspectionApi.uploadPhoto(
        photo.photo_url,
        photo.task_id,
        photo.result_item_id,
        photo.photo_type
      )
      if (result && result.photoUrl) {
        await inspectionDB.markPhotoSynced(photo.id)
      }
      return true
    } catch (e) {
      console.error('照片上传失败', e)
      throw e
    }
  }

  async syncAudio(audio) {
    if (!audio.audio_url) {
      await inspectionDB.markAudioSynced(audio.id)
      return true
    }

    if (audio.audio_url.startsWith('http')) {
      await inspectionDB.markAudioSynced(audio.id)
      return true
    }

    try {
      const result = await inspectionApi.uploadAudio(
        audio.audio_url,
        audio.task_id,
        audio.result_item_id
      )
      if (result && result.audioUrl) {
        await inspectionDB.markAudioSynced(audio.id)
      }
      return true
    } catch (e) {
      console.error('录音上传失败', e)
      throw e
    }
  }

  async downloadTasks(userId) {
    if (!this.isOnline()) {
      throw new Error('网络不可用，无法下载任务')
    }

    try {
      const tasks = await inspectionApi.getDownloadTasks(userId)
      
      if (tasks && tasks.length > 0) {
        await inspectionDB.saveTasks(tasks)
        
        for (const task of tasks) {
          try {
            await inspectionApi.markTaskAsDownloaded(task.id)
          } catch (e) {
            console.warn('标记任务已下载失败', e)
          }
        }
      }

      return tasks
    } catch (e) {
      console.error('下载任务失败', e)
      throw e
    }
  }

  addListener(event, callback) {
    this.listeners.push({ event, callback })
  }

  removeListener(event, callback) {
    this.listeners = this.listeners.filter(l => l.event !== event || l.callback !== callback)
  }

  notifyListeners(event, data) {
    this.listeners
      .filter(l => l.event === event)
      .forEach(l => {
        try {
          l.callback(data)
        } catch (e) {
          console.error('监听器执行失败', e)
        }
      })
  }

  getSyncStatus() {
    return {
      isSyncing: this.isSyncing,
      isOnline: this.isOnline(),
      networkStatus: this.networkStatus
    }
  }
}

const syncService = new SyncService()

export default syncService
