import { getToken } from './auth'

class DashboardWebSocketClient {
  constructor() {
    this.ws = null
    this.reconnectTimer = null
    this.heartbeatTimer = null
    this.listeners = new Map()
    this.reconnectCount = 0
    this.maxReconnectCount = 10
    this.url = this.buildUrl()
  }

  buildUrl() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const basePath = '/admin'
    return `${protocol}//${host}${basePath}/websocket/dashboard`
  }

  connect() {
    const token = getToken()
    const wsUrl = token ? `${this.url}?token=${encodeURIComponent(token)}` : this.url

    this.ws = new WebSocket(wsUrl)

    this.ws.onopen = () => {
      console.log('[大屏] WebSocket连接成功')
      this.reconnectCount = 0
      this.startHeartbeat()
      this.emit('open')
    }

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        this.emit('message', data)
      } catch (e) {
        this.emit('message', event.data)
      }
    }

    this.ws.onerror = (error) => {
      console.error('[大屏] WebSocket错误:', error)
      this.emit('error', error)
    }

    this.ws.onclose = () => {
      console.log('[大屏] WebSocket连接关闭')
      this.stopHeartbeat()
      this.emit('close')
      this.reconnect()
    }
  }

  reconnect() {
    if (this.reconnectCount >= this.maxReconnectCount) {
      console.log('[大屏] WebSocket重连次数已达上限')
      return
    }
    this.reconnectCount++
    this.reconnectTimer = setTimeout(() => {
      console.log(`[大屏] WebSocket正在重连... 第${this.reconnectCount}次`)
      this.connect()
    }, 3000 * this.reconnectCount)
  }

  startHeartbeat() {
    this.heartbeatTimer = setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
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

  send(data) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(typeof data === 'string' ? data : JSON.stringify(data))
    }
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
      const index = callbacks.indexOf(callback)
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    }
  }

  emit(event, data) {
    const callbacks = this.listeners.get(event)
    if (callbacks) {
      callbacks.forEach((cb) => cb(data))
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

let dashboardWs = null

export function getDashboardWebSocket() {
  if (!dashboardWs) {
    dashboardWs = new DashboardWebSocketClient()
  }
  return dashboardWs
}

export function closeDashboardWebSocket() {
  if (dashboardWs) {
    dashboardWs.close()
    dashboardWs = null
  }
}

export default DashboardWebSocketClient
