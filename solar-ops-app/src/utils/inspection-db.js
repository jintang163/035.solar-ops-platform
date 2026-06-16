import db from '@/utils/sqlite.js'

export const inspectionDB = {
  saveTasks(tasks) {
    return new Promise(async (resolve, reject) => {
      try {
        for (const task of tasks) {
          await this.saveTask(task)
        }
        resolve()
      } catch (e) {
        reject(e)
      }
    })
  },

  saveTask(task) {
    return new Promise(async (resolve, reject) => {
      try {
        const existingTask = await db.queryOne('inspection_task', 'id = ?', [task.id])
        
        const taskData = {
          id: task.id,
          task_no: task.taskNo || task.task_no,
          task_name: task.taskName || task.task_name,
          station_id: task.stationId || task.station_id,
          station_name: task.stationName || task.station_name,
          task_type: task.taskType || task.task_type,
          priority: task.priority,
          plan_start_time: task.planStartTime || task.plan_start_time,
          plan_end_time: task.planEndTime || task.plan_end_time,
          actual_start_time: task.actualStartTime || task.actual_start_time,
          actual_end_time: task.actualEndTime || task.actual_end_time,
          status: task.status,
          assignee_id: task.assigneeId || task.assignee_id,
          assignee_name: task.assigneeName || task.assignee_name,
          description: task.description,
          remark: task.remark,
          sync_status: 1
        }

        if (existingTask) {
          await db.update('inspection_task', taskData, 'id = ?', [task.id])
        } else {
          await db.insert('inspection_task', taskData)
        }

        if (task.items && task.items.length > 0) {
          await db.remove('inspection_task_item', 'task_id = ?', [task.id])
          for (const item of task.items) {
            await this.saveTaskItem(task.id, item)
          }
        }

        resolve()
      } catch (e) {
        reject(e)
      }
    })
  },

  saveTaskItem(taskId, item) {
    return db.insert('inspection_task_item', {
      task_item_id: item.taskItemId || item.task_item_id,
      task_id: taskId,
      item_id: item.itemId || item.item_id,
      item_code: item.itemCode || item.item_code,
      item_name: item.itemName || item.item_name,
      item_type: item.itemType || item.item_type,
      asset_id: item.assetId || item.asset_id,
      asset_name: item.assetName || item.asset_name,
      asset_code: item.assetCode || item.asset_code,
      standard_value: item.standardValue || item.standard_value,
      min_value: item.minValue || item.min_value,
      max_value: item.maxValue || item.max_value,
      unit: item.unit,
      is_required: item.isRequired || item.is_required,
      sort_order: item.sortOrder || item.sort_order,
      description: item.description
    })
  },

  getTaskList(status) {
    let where = 'is_deleted = 0'
    let args = []
    if (status !== undefined && status !== null) {
      where += ' AND status = ?'
      args.push(status)
    }
    return db.query('inspection_task', where, args, 'priority DESC, plan_start_time ASC')
  },

  getTaskDetail(taskId) {
    return new Promise(async (resolve, reject) => {
      try {
        const task = await db.queryOne('inspection_task', 'id = ?', [taskId])
        if (!task) {
          resolve(null)
          return
        }
        
        const items = await db.query('inspection_task_item', 'task_id = ?', [taskId], 'sort_order ASC')
        
        resolve({
          ...task,
          items
        })
      } catch (e) {
        reject(e)
      }
    })
  },

  updateTaskStatus(taskId, status) {
    const data = { status }
    if (status === 2) {
      data.actual_start_time = new Date().toISOString()
    } else if (status === 3) {
      data.actual_end_time = new Date().toISOString()
    }
    return db.update('inspection_task', data, 'id = ?', [taskId])
  },

  saveResult(result) {
    return new Promise(async (resolve, reject) => {
      try {
        const resultData = {
          result_no: result.resultNo || result.result_no || this.generateNo('IR'),
          task_id: result.taskId || result.task_id,
          task_no: result.taskNo || result.task_no,
          station_id: result.stationId || result.station_id,
          station_name: result.stationName || result.station_name,
          inspector_id: result.inspectorId || result.inspector_id,
          inspector_name: result.inspectorName || result.inspector_name,
          start_time: result.startTime || result.start_time,
          end_time: result.endTime || result.end_time,
          total_items: result.totalItems || result.total_items || 0,
          normal_items: result.normalItems || result.normal_items || 0,
          abnormal_items: result.abnormalItems || result.abnormal_items || 0,
          result_status: result.resultStatus || result.result_status || 1,
          overall_remark: result.overallRemark || result.overall_remark,
          longitude: result.longitude,
          latitude: result.latitude,
          is_offline: 1,
          sync_status: 0
        }

        const resultId = await db.insert('inspection_result', resultData)

        if (result.items && result.items.length > 0) {
          for (const item of result.items) {
            await this.saveResultItem(resultId, item)
          }
        }

        if (result.photos && result.photos.length > 0) {
          for (const photo of result.photos) {
            await this.savePhoto(resultId, photo)
          }
        }

        if (result.audios && result.audios.length > 0) {
          for (const audio of result.audios) {
            await this.saveAudio(resultId, audio)
          }
        }

        resolve(resultId)
      } catch (e) {
        reject(e)
      }
    })
  },

  saveResultItem(resultId, item) {
    return db.insert('inspection_result_item', {
      result_id: resultId,
      task_item_id: item.taskItemId || item.task_item_id,
      item_id: item.itemId || item.item_id,
      item_name: item.itemName || item.item_name,
      item_type: item.itemType || item.item_type,
      asset_id: item.assetId || item.asset_id,
      asset_name: item.assetName || item.asset_name,
      asset_code: item.assetCode || item.asset_code,
      check_value: item.checkValue || item.check_value,
      standard_value: item.standardValue || item.standard_value,
      is_normal: item.isNormal !== undefined ? item.isNormal : (item.is_normal !== undefined ? item.is_normal : 1),
      abnormal_desc: item.abnormalDesc || item.abnormal_desc,
      remark: item.remark,
      check_time: item.checkTime || item.check_time || new Date().toISOString(),
      longitude: item.longitude,
      latitude: item.latitude
    })
  },

  savePhoto(resultId, photo) {
    return db.insert('inspection_photo', {
      photo_no: photo.photoNo || photo.photo_no || this.generateNo('PH'),
      result_id: resultId,
      result_item_id: photo.resultItemId || photo.result_item_id,
      task_id: photo.taskId || photo.task_id,
      asset_id: photo.assetId || photo.asset_id,
      photo_type: photo.photoType || photo.photo_type || 1,
      photo_url: photo.photoUrl || photo.photo_url,
      thumbnail_url: photo.thumbnailUrl || photo.thumbnail_url,
      file_size: photo.fileSize || photo.file_size,
      watermark_time: photo.watermarkTime || photo.watermark_time,
      longitude: photo.longitude,
      latitude: photo.latitude,
      has_watermark: photo.hasWatermark !== undefined ? photo.hasWatermark : (photo.has_watermark !== undefined ? photo.has_watermark : 0),
      remark: photo.remark,
      is_offline: 1,
      sync_status: 0
    })
  },

  saveAudio(resultId, audio) {
    return db.insert('inspection_audio', {
      audio_no: audio.audioNo || audio.audio_no || this.generateNo('AU'),
      result_id: resultId,
      result_item_id: audio.resultItemId || audio.result_item_id,
      task_id: audio.taskId || audio.task_id,
      asset_id: audio.assetId || audio.asset_id,
      audio_url: audio.audioUrl || audio.audio_url,
      file_size: audio.fileSize || audio.file_size,
      duration: audio.duration,
      record_time: audio.recordTime || audio.record_time,
      longitude: audio.longitude,
      latitude: audio.latitude,
      remark: audio.remark,
      is_offline: 1,
      sync_status: 0
    })
  },

  getPendingSyncResults() {
    return db.query('inspection_result', 'sync_status = 0', [], 'create_time ASC')
  },

  getResultPhotos(resultId) {
    return db.query('inspection_photo', 'result_id = ?', [resultId])
  },

  getResultAudios(resultId) {
    return db.query('inspection_audio', 'result_id = ?', [resultId])
  },

  getResultItems(resultId) {
    return db.query('inspection_result_item', 'result_id = ?', [resultId])
  },

  getResultDetail(resultId) {
    return new Promise(async (resolve, reject) => {
      try {
        const result = await db.queryOne('inspection_result', 'id = ?', [resultId])
        if (!result) {
          resolve(null)
          return
        }
        
        const items = await this.getResultItems(resultId)
        const photos = await this.getResultPhotos(resultId)
        const audios = await this.getResultAudios(resultId)
        
        resolve({
          ...result,
          items,
          photos,
          audios
        })
      } catch (e) {
        reject(e)
      }
    })
  },

  markResultSynced(resultId) {
    return db.update('inspection_result', { sync_status: 1 }, 'id = ?', [resultId])
  },

  markPhotoSynced(photoId) {
    return db.update('inspection_photo', { sync_status: 1 }, 'id = ?', [photoId])
  },

  markAudioSynced(audioId) {
    return db.update('inspection_audio', { sync_status: 1 }, 'id = ?', [audioId])
  },

  getPendingSyncPhotos() {
    return db.query('inspection_photo', 'sync_status = 0', [])
  },

  getPendingSyncAudios() {
    return db.query('inspection_audio', 'sync_status = 0', [])
  },

  generateNo(prefix) {
    const now = new Date()
    const dateStr = now.getFullYear().toString() +
      (now.getMonth() + 1).toString().padStart(2, '0') +
      now.getDate().toString().padStart(2, '0') +
      now.getHours().toString().padStart(2, '0') +
      now.getMinutes().toString().padStart(2, '0') +
      now.getSeconds().toString().padStart(2, '0')
    const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0')
    return prefix + dateStr + random
  }
}

export default inspectionDB
