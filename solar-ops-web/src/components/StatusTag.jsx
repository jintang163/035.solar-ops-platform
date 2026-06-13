import React from 'react'
import { Tag } from 'antd'

const statusMap = {
  online: { color: 'success', text: '在线' },
  offline: { color: 'default', text: '离线' },
  fault: { color: 'error', text: '故障' },
  warning: { color: 'warning', text: '告警' },
  running: { color: 'processing', text: '运行中' },
  standby: { color: 'default', text: '待机' },
  pending: { color: 'orange', text: '待处理' },
  processing: { color: 'blue', text: '处理中' },
  completed: { color: 'green', text: '已完成' },
  closed: { color: 'gray', text: '已关闭' },
  excellent: { color: 'green', text: '优秀' },
  good: { color: 'blue', text: '良好' },
  normal: { color: 'orange', text: '一般' },
  poor: { color: 'red', text: '较差' }
}

const StatusTag = ({ status, type = 'default' }) => {
  const statusInfo = statusMap[status] || { color: 'default', text: status }

  if (type === 'dot') {
    return <span className={`status-dot status-dot-${status}`}>{statusInfo.text}</span>
  }

  return <Tag color={statusInfo.color}>{statusInfo.text}</Tag>
}

export default StatusTag
