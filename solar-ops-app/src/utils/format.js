export function formatNumber(num, digits = 2) {
  if (num === null || num === undefined || isNaN(num)) return '0'
  const n = Number(num)
  if (n >= 10000) {
    return (n / 10000).toFixed(digits) + '万'
  }
  return n.toFixed(digits)
}

export function formatPower(kwh) {
  if (kwh === null || kwh === undefined || isNaN(kwh)) return '0 kWh'
  const n = Number(kwh)
  if (n >= 1000000) {
    return (n / 1000000).toFixed(2) + ' MWh'
  } else if (n >= 1000) {
    return (n / 1000).toFixed(2) + ' MWh'
  }
  return n.toFixed(2) + ' kWh'
}

export function formatPercent(value, digits = 1) {
  if (value === null || value === undefined || isNaN(value)) return '0%'
  return (Number(value) * 100).toFixed(digits) + '%'
}

export function formatDate(date, format = 'YYYY-MM-DD') {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hour = String(d.getHours()).padStart(2, '0')
  const minute = String(d.getMinutes()).padStart(2, '0')
  const second = String(d.getSeconds()).padStart(2, '0')
  
  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hour)
    .replace('mm', minute)
    .replace('ss', second)
}

export function formatDateTime(date) {
  return formatDate(date, 'YYYY-MM-DD HH:mm:ss')
}

export function formatTime(date) {
  return formatDate(date, 'HH:mm:ss')
}

export function getStatusText(status, type = 'order') {
  const statusMap = {
    order: {
      0: '待接单',
      1: '处理中',
      2: '待验收',
      3: '已完成',
      4: '已取消'
    },
    device: {
      0: '离线',
      1: '在线',
      2: '故障'
    }
  }
  return statusMap[type]?.[status] || '未知'
}
