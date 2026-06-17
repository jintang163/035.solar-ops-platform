import { getToken } from './auth'

class VoiceBroadcastWebSocket {
  constructor() {
    this.ws = null
    this.reconnectCount = 0
    this.maxReconnectCount = 10
    this.listeners = new Map()
    this.heartbeatTimer = null
    this.reconnectTimer = null
    this.url = this.buildUrl()
  }

  buildUrl() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const basePath = '/admin'
    return `${protocol}//${host}${basePath}/websocket/voice-broadcast`
  }

  connect() {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      return
    }
    const token = getToken()
    const wsUrl = token ? `${this.url}?token=${encodeURIComponent(token)}` : this.url

    try {
      this.ws = new WebSocket(wsUrl)

      this.ws.onopen = () => {
        console.log('[语音播报] WebSocket已连接')
        this.reconnectCount = 0
        this.startHeartbeat()
        this.emit('open')
      }

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          if (data.type === 'voice_broadcast') {
            this.emit('broadcast', data.data)
          }
        } catch (e) {
          console.error('[语音播报] 解析消息失败', e)
        }
      }

      this.ws.onclose = () => {
        console.log('[语音播报] WebSocket连接关闭')
        this.stopHeartbeat()
        this.emit('close')
        this.scheduleReconnect()
      }

      this.ws.onerror = (error) => {
        console.error('[语音播报] WebSocket错误', error)
        this.emit('error', error)
      }
    } catch (e) {
      console.error('[语音播报] 创建WebSocket失败', e)
      this.scheduleReconnect()
    }
  }

  startHeartbeat() {
    this.heartbeatTimer = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send('heartbeat')
      }
    }, 30000)
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  scheduleReconnect() {
    if (this.reconnectCount >= this.maxReconnectCount) {
      console.log('[语音播报] 达到最大重连次数，停止重连')
      return
    }
    const delay = Math.min(1000 * Math.pow(2, this.reconnectCount), 30000)
    this.reconnectCount++
    console.log(`[语音播报] ${delay / 1000}秒后进行第${this.reconnectCount}次重连`)
    this.reconnectTimer = setTimeout(() => this.connect(), delay)
  }

  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event).push(callback)
  }

  off(event, callback) {
    const callbacks = this.listeners.get(event)
    if (callbacks) {
      if (callback) {
        const index = callbacks.indexOf(callback)
        if (index > -1) {
          callbacks.splice(index, 1)
        }
      } else {
        this.listeners.delete(event)
      }
    }
  }

  emit(event, data) {
    const callbacks = this.listeners.get(event)
    if (callbacks) {
      callbacks.forEach(cb => {
        try {
          cb(data)
        } catch (e) {
          console.error(`[语音播报] 事件${event}回调执行失败`, e)
        }
      })
    }
  }

  close() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.stopHeartbeat()
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.listeners.clear()
  }
}

let instance = null

export function getVoiceBroadcastWebSocket() {
  if (!instance) {
    instance = new VoiceBroadcastWebSocket()
  }
  return instance
}

export function closeVoiceBroadcastWebSocket() {
  if (instance) {
    instance.close()
    instance = null
  }
}

export default VoiceBroadcastWebSocket
