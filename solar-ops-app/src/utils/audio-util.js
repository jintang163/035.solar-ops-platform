class AudioRecorder {
  constructor() {
    this.recorder = null
    this.isRecording = false
    this.startTime = null
    this.duration = 0
    this.timer = null
    this.audioPath = null
    this.listeners = []
  }

  init() {
    // #ifdef APP-PLUS
    this.recorder = plus.audio.getRecorder()
    // #endif
    return this
  }

  start(options = {}) {
    return new Promise((resolve, reject) => {
      if (this.isRecording) {
        reject(new Error('正在录音中'))
        return
      }

      const {
        format = 'amr',
        samplerate = 8000,
        bitrate = 16
      } = options

      // #ifdef APP-PLUS
      if (!this.recorder) {
        this.recorder = plus.audio.getRecorder()
      }

      this.recorder.record({
        filename: `_doc/inspection/audio_${Date.now()}.${format}`,
        format: format,
        samplerate: samplerate,
        bitrate: bitrate
      }, (path) => {
        this.audioPath = path
        this.isRecording = false
        this.stopTimer()
        this.notifyListeners('stop', {
          path,
          duration: this.duration
        })
        resolve({
          path,
          duration: this.duration
        })
      }, (e) => {
        this.isRecording = false
        this.stopTimer()
        reject(e)
      })

      this.isRecording = true
      this.startTime = Date.now()
      this.duration = 0
      this.startTimer()
      this.notifyListeners('start', {})
      // #endif

      // #ifdef MP-WEIXIN
      const recorderManager = uni.getRecorderManager()
      
      recorderManager.onStart(() => {
        this.isRecording = true
        this.startTime = Date.now()
        this.duration = 0
        this.startTimer()
        this.notifyListeners('start', {})
      })

      recorderManager.onStop((res) => {
        this.isRecording = false
        this.audioPath = res.tempFilePath
        this.stopTimer()
        this.notifyListeners('stop', {
          path: res.tempFilePath,
          duration: res.duration
        })
        resolve({
          path: res.tempFilePath,
          duration: Math.floor(res.duration / 1000)
        })
      })

      recorderManager.onError((err) => {
        this.isRecording = false
        this.stopTimer()
        reject(err)
      })

      recorderManager.start({
        duration: 600000,
        format: format,
        sampleRate: samplerate,
        bitRate: bitrate,
        numberOfChannels: 1
      })
      this.recorder = recorderManager
      // #endif

      // #ifdef H5
      reject(new Error('H5暂不支持录音功能'))
      // #endif
    })
  }

  stop() {
    return new Promise((resolve, reject) => {
      if (!this.isRecording) {
        reject(new Error('没有正在进行的录音'))
        return
      }

      // #ifdef APP-PLUS
      this.recorder.stop()
      // #endif

      // #ifdef MP-WEIXIN
      if (this.recorder && this.recorder.stop) {
        this.recorder.stop()
      }
      // #endif
    })
  }

  pause() {
    return new Promise((resolve, reject) => {
      if (!this.isRecording) {
        reject(new Error('没有正在进行的录音'))
        return
      }

      // #ifdef MP-WEIXIN
      if (this.recorder && this.recorder.pause) {
        this.recorder.pause()
        this.stopTimer()
        this.notifyListeners('pause', {})
        resolve()
      } else {
        reject(new Error('暂停录音失败'))
      }
      // #endif

      // #ifndef MP-WEIXIN
      reject(new Error('当前平台不支持暂停录音'))
      // #endif
    })
  }

  resume() {
    return new Promise((resolve, reject) => {
      // #ifdef MP-WEIXIN
      if (this.recorder && this.recorder.resume) {
        this.recorder.resume()
        this.startTimer()
        this.notifyListeners('resume', {})
        resolve()
      } else {
        reject(new Error('恢复录音失败'))
      }
      // #endif

      // #ifndef MP-WEIXIN
      reject(new Error('当前平台不支持恢复录音'))
      // #endif
    })
  }

  startTimer() {
    this.timer = setInterval(() => {
      this.duration++
      this.notifyListeners('timeupdate', { duration: this.duration })
    }, 1000)
  }

  stopTimer() {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  }

  getDuration() {
    return this.duration
  }

  isRecordingStatus() {
    return this.isRecording
  }

  addListener(event, callback) {
    this.listeners.push({ event, callback })
  }

  removeListener(event, callback) {
    this.listeners = this.listeners.filter(
      l => l.event !== event || l.callback !== callback
    )
  }

  notifyListeners(event, data) {
    this.listeners
      .filter(l => l.event === event)
      .forEach(l => {
        try {
          l.callback(data)
        } catch (e) {
          console.error('录音监听器执行失败', e)
        }
      })
  }

  play(audioPath) {
    return new Promise((resolve, reject) => {
      // #ifdef APP-PLUS
      const player = plus.audio.createPlayer(audioPath)
      player.play()
      resolve(player)
      // #endif

      // #ifdef MP-WEIXIN
      const innerAudioContext = uni.createInnerAudioContext()
      innerAudioContext.src = audioPath
      innerAudioContext.play()
      resolve(innerAudioContext)
      // #endif

      // #ifdef H5
      const audio = new Audio(audioPath)
      audio.play()
      resolve(audio)
      // #endif
    })
  }

  getAudioFileSize(audioPath) {
    return new Promise((resolve) => {
      // #ifdef APP-PLUS
      plus.io.resolveLocalFileSystemURL(audioPath, (entry) => {
        entry.getMetadata((meta) => {
          resolve(meta.size)
        }, () => {
          resolve(0)
        })
      }, () => {
        resolve(0)
      })
      // #endif
      
      // #ifndef APP-PLUS
      resolve(0)
      // #endif
    })
  }

  formatDuration(seconds) {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }
}

const audioRecorder = new AudioRecorder()

export const audioUtil = {
  recorder: audioRecorder,

  async recordInspectionAudio(taskId, assetId) {
    try {
      const result = await audioRecorder.start({
        format: 'amr',
        samplerate: 8000,
        bitrate: 16
      })

      const fileSize = await audioRecorder.getAudioFileSize(result.path)
      
      const location = await this.getLocation()

      return {
        audioPath: result.path,
        duration: result.duration,
        fileSize,
        taskId,
        assetId,
        recordTime: new Date(),
        longitude: location ? location.longitude : null,
        latitude: location ? location.latitude : null
      }
    } catch (e) {
      console.error('录音失败', e)
      throw e
    }
  },

  getLocation() {
    return new Promise((resolve) => {
      uni.getLocation({
        type: 'gcj02',
        success: (res) => {
          resolve({
            longitude: res.longitude,
            latitude: res.latitude
          })
        },
        fail: () => {
          resolve(null)
        }
      })
    })
  },

  stopRecording() {
    return audioRecorder.stop()
  },

  isRecording() {
    return audioRecorder.isRecordingStatus()
  },

  getDuration() {
    return audioRecorder.getDuration()
  },

  formatDuration(seconds) {
    return audioRecorder.formatDuration(seconds)
  }
}

export default audioUtil
