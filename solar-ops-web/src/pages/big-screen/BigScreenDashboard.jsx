import React, { useState, useEffect, useRef, useCallback } from 'react'
import { Modal, message } from 'antd'
import {
  ThunderboltOutlined,
  RiseOutlined,
  LeafOutlined,
  HeartOutlined,
  BellOutlined,
  FileTextOutlined,
  FullscreenOutlined,
  FullscreenExitOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  ReloadOutlined,
  ArrowLeftOutlined
} from '@ant-design/icons'
import * as echarts from 'echarts'
import ReactECharts from 'echarts-for-react'
import { getRealTimeDashboard, getInverterMonitorByStation } from '../../api/dashboard'
import { getDashboardWebSocket, closeDashboardWebSocket } from '../../utils/dashboardWebsocket'
import '../../styles/bigScreen.css'

const BigScreenDashboard = () => {
  const [loading, setLoading] = useState(true)
  const [dashboardData, setDashboardData] = useState(null)
  const [selectedStation, setSelectedStation] = useState(null)
  const [inverterData, setInverterData] = useState([])
  const [inverterModalVisible, setInverterModalVisible] = useState(false)
  const [inverterLoading, setInverterLoading] = useState(false)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [isCarousel, setIsCarousel] = useState(false)
  const [carouselIndex, setCarouselIndex] = useState(0)
  const [wsConnected, setWsConnected] = useState(false)
  const [currentTime, setCurrentTime] = useState(new Date())
  const [drillDownLevel, setDrillDownLevel] = useState(0)
  const [mapRegistered, setMapRegistered] = useState(false)

  const mapRegisteredRef = useRef(false)

  const wsRef = useRef(null)
  const carouselTimerRef = useRef(null)
  const containerRef = useRef(null)
  const chartRef = useRef(null)

  const formatNumber = (num) => {
    if (num == null) return '0'
    const n = Number(num)
    if (n >= 10000) {
      return (n / 10000).toFixed(2)
    }
    return n.toFixed(2)
  }

  const formatTime = (date) => {
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    })
  }

  const loadDashboardData = useCallback(async () => {
    try {
      const res = await getRealTimeDashboard()
      if (res.code === 200) {
        setDashboardData(res.data)
      }
    } catch (err) {
      console.error('加载大屏数据失败:', err)
      message.error('加载大屏数据失败')
    } finally {
      setLoading(false)
    }
  }, [])

  const loadInverterData = useCallback(async (stationId) => {
    setInverterLoading(true)
    try {
      const res = await getInverterMonitorByStation(stationId)
      if (res.code === 200) {
        setInverterData(res.data || [])
        setInverterModalVisible(true)
      }
    } catch (err) {
      console.error('加载逆变器数据失败:', err)
      message.error('加载逆变器数据失败')
    } finally {
      setInverterLoading(false)
    }
  }, [])

  const handleStationClick = useCallback((station) => {
    setSelectedStation(station)
    setDrillDownLevel(1)
    loadInverterData(station.stationId)
  }, [loadInverterData])

  const handleBackToOverview = useCallback(() => {
    setSelectedStation(null)
    setDrillDownLevel(0)
    setInverterModalVisible(false)
    setInverterData([])
  }, [])

  const handleCloseInverterModal = useCallback(() => {
    setInverterModalVisible(false)
  }, [])

  const toggleFullscreen = useCallback(() => {
    if (!document.fullscreenElement) {
      containerRef.current?.requestFullscreen?.()
      setIsFullscreen(true)
    } else {
      document.exitFullscreen?.()
      setIsFullscreen(false)
    }
  }, [])

  const toggleCarousel = useCallback(() => {
    setIsCarousel(prev => !prev)
  }, [])

  const handleCarouselClick = useCallback((index) => {
    setCarouselIndex(index)
  }, [])

  const refreshData = useCallback(() => {
    setLoading(true)
    loadDashboardData()
    message.success('数据已刷新')
  }, [loadDashboardData])

  const getInverterStatus = (item) => {
    if (item.faultCode) return 'fault'
    return item.onlineStatus === 1 ? 'online' : 'offline'
  }

  const getInverterStatusText = (status) => {
    const map = { online: '在线', offline: '离线', fault: '故障' }
    return map[status] || '未知'
  }

  const getTemperatureColor = (temp) => {
    if (temp > 55) return 'danger'
    if (temp > 45) return 'warn'
    return ''
  }

  useEffect(() => {
    if (mapRegisteredRef.current) return
    fetch('https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json')
      .then(res => res.json())
      .then(geoJson => {
        echarts.registerMap('china', geoJson)
        mapRegisteredRef.current = true
        setMapRegistered(true)
      })
      .catch(err => {
        console.error('加载中国地图数据失败:', err)
      })
  }, [])

  useEffect(() => {
    loadDashboardData()
  }, [loadDashboardData])

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date())
    }, 1000)
    return () => clearInterval(timer)
  }, [])

  useEffect(() => {
    const ws = getDashboardWebSocket()
    wsRef.current = ws

    const handleOpen = () => {
      setWsConnected(true)
      console.log('[大屏] WebSocket已连接')
    }

    const handleMessage = (data) => {
      if (data && data.totalPower !== undefined) {
        setDashboardData(data)
      }
    }

    const handleClose = () => {
      setWsConnected(false)
    }

    const handleError = () => {
      setWsConnected(false)
    }

    ws.on('open', handleOpen)
    ws.on('message', handleMessage)
    ws.on('close', handleClose)
    ws.on('error', handleError)
    ws.connect()

    return () => {
      ws.off('open', handleOpen)
      ws.off('message', handleMessage)
      ws.off('close', handleClose)
      ws.off('error', handleError)
      closeDashboardWebSocket()
      wsRef.current = null
    }
  }, [])

  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement)
    }
    document.addEventListener('fullscreenchange', handleFullscreenChange)
    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange)
    }
  }, [])

  useEffect(() => {
    if (isCarousel && dashboardData?.stationMapList?.length > 0) {
      carouselTimerRef.current = setInterval(() => {
        setCarouselIndex(prev => {
          const next = (prev + 1) % dashboardData.stationMapList.length
          return next
        })
      }, 5000)
    } else {
      if (carouselTimerRef.current) {
        clearInterval(carouselTimerRef.current)
        carouselTimerRef.current = null
      }
    }
    return () => {
      if (carouselTimerRef.current) {
        clearInterval(carouselTimerRef.current)
      }
    }
  }, [isCarousel, dashboardData?.stationMapList?.length])

  const powerTrendOption = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(16, 32, 64, 0.9)',
      borderColor: 'rgba(24, 144, 255, 0.5)',
      textStyle: { color: '#fff' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dashboardData?.powerTrend?.map(d => d.time) || [],
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.2)' } },
      axisLabel: { color: 'rgba(255, 255, 255, 0.6)', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      name: 'kW',
      nameTextStyle: { color: 'rgba(255, 255, 255, 0.6)' },
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.2)' } },
      axisLabel: { color: 'rgba(255, 255, 255, 0.6)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.05)' } }
    },
    series: [
      {
        name: '总功率',
        type: 'line',
        smooth: true,
        symbol: 'none',
        data: dashboardData?.powerTrend?.map(d => d.power) || [],
        lineStyle: { color: '#1890ff', width: 2 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(24, 144, 255, 0.4)' },
              { offset: 1, color: 'rgba(24, 144, 255, 0.05)' }
            ]
          }
        }
      }
    ]
  }

  const generationTrendOption = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(16, 32, 64, 0.9)',
      borderColor: 'rgba(82, 196, 26, 0.5)',
      textStyle: { color: '#fff' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dashboardData?.generationTrend?.map(d => d.date) || [],
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.2)' } },
      axisLabel: { color: 'rgba(255, 255, 255, 0.6)', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      name: 'kWh',
      nameTextStyle: { color: 'rgba(255, 255, 255, 0.6)' },
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.2)' } },
      axisLabel: { color: 'rgba(255, 255, 255, 0.6)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.05)' } }
    },
    series: [
      {
        name: '发电量',
        type: 'bar',
        data: dashboardData?.generationTrend?.map(d => d.generation) || [],
        itemStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: '#52c41a' },
              { offset: 1, color: 'rgba(82, 196, 26, 0.3)' }
            ]
          },
          borderRadius: [4, 4, 0, 0]
        }
      }
    ]
  }

  const stationStatusPieOption = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(16, 32, 64, 0.9)',
      borderColor: 'rgba(24, 144, 255, 0.5)',
      textStyle: { color: '#fff' }
    },
    legend: {
      orient: 'horizontal',
      bottom: 0,
      textStyle: { color: 'rgba(255, 255, 255, 0.7)', fontSize: 11 }
    },
    series: [
      {
        name: '设备状态',
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '40%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: 'rgba(16, 32, 64, 0.9)',
          borderWidth: 2
        },
        label: { show: false },
        data: [
          { value: dashboardData?.onlineCount || 0, name: '在线', itemStyle: { color: '#52c41a' } },
          { value: dashboardData?.offlineCount || 0, name: '离线', itemStyle: { color: '#8c8c8c' } },
          { value: dashboardData?.alarmCount || 0, name: '告警', itemStyle: { color: '#faad14' } }
        ]
      }
    ]
  }

  const mapOption = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(16, 32, 64, 0.95)',
      borderColor: 'rgba(24, 144, 255, 0.5)',
      textStyle: { color: '#fff' },
      formatter: (params) => {
        if (params.dataType === 'point') {
          const data = params.data
          return `
            <div style="min-width: 180px;">
              <div style="font-weight: bold; margin-bottom: 8px; color: #1890ff;">${data.name}</div>
              <div>健康度：<span style="color: ${data.healthColor}">${data.healthScore}%</span></div>
              <div>当前功率：<span style="color: #52c41a;">${data.currentPower} kW</span></div>
              <div>今日发电：<span style="color: #1890ff;">${data.todayGeneration} kWh</span></div>
              <div>逆变器：${data.onlineInverterCount}/${data.inverterCount} 在线</div>
              <div>告警：<span style="color: #faad14;">${data.alarmCount} 条</span></div>
              <div style="margin-top: 6px; color: rgba(255,255,255,0.5); font-size: 11px;">点击查看逆变器详情 →</div>
            </div>
          `
        }
        return ''
      }
    },
    geo: {
      map: 'china',
      roam: true,
      zoom: 1.2,
      center: [105, 36],
      itemStyle: {
        areaColor: 'rgba(24, 144, 255, 0.1)',
        borderColor: 'rgba(24, 144, 255, 0.4)',
        borderWidth: 1
      },
      emphasis: {
        itemStyle: {
          areaColor: 'rgba(24, 144, 255, 0.2)',
          borderColor: '#1890ff'
        },
        label: { show: false }
      },
      label: { show: false }
    },
    series: [
      {
        name: '电站',
        type: 'scatter',
        coordinateSystem: 'geo',
        symbolSize: (val, params) => {
          const baseSize = 18
          const capacity = params.data.capacity || 0
          return baseSize + Math.min(capacity / 1000, 15)
        },
        data: (dashboardData?.stationMapList || [])
          .filter(station => station.longitude != null && station.latitude != null)
          .map((station, index) => ({
          name: station.stationName,
          value: [station.longitude, station.latitude],
          stationId: station.stationId,
          stationCode: station.stationCode,
          capacity: station.capacity,
          currentPower: station.currentPower,
          todayGeneration: station.todayGeneration,
          healthLevel: station.healthLevel,
          healthColor: station.healthColor,
          healthScore: station.healthScore,
          inverterCount: station.inverterCount,
          onlineInverterCount: station.onlineInverterCount,
          alarmCount: station.alarmCount,
          address: station.address,
          itemStyle: {
            color: station.healthColor === 'green' ? '#52c41a' : station.healthColor === 'yellow' ? '#faad14' : '#ff4d4f',
            shadowBlur: 15,
            shadowColor: station.healthColor === 'green' ? '#52c41a' : station.healthColor === 'yellow' ? '#faad14' : '#ff4d4f'
          },
          rippleEffect: {
            scale: isCarousel && carouselIndex === index ? 3 : 0,
            brushType: 'stroke'
          }
        })),
        label: {
          show: true,
          formatter: '{b}',
          position: 'right',
          color: '#fff',
          fontSize: 11,
          textBorderColor: 'rgba(0,0,0,0.8)',
          textBorderWidth: 2
        },
        emphasis: {
          label: { show: true, fontSize: 13, fontWeight: 'bold' }
        }
      },
      {
        name: '选中脉冲',
        type: 'effectScatter',
        coordinateSystem: 'geo',
        symbolSize: 25,
        showEffectOn: 'render',
        rippleEffect: {
          period: 4,
          scale: 4,
          brushType: 'stroke'
        },
        data: isCarousel && dashboardData?.stationMapList?.[carouselIndex]
          && dashboardData.stationMapList[carouselIndex].longitude != null
          && dashboardData.stationMapList[carouselIndex].latitude != null
          ? [{
              name: dashboardData.stationMapList[carouselIndex].stationName,
              value: [
                dashboardData.stationMapList[carouselIndex].longitude,
                dashboardData.stationMapList[carouselIndex].latitude
              ],
              itemStyle: {
                color: 'transparent',
                borderColor: '#1890ff',
                borderWidth: 2
              }
            }]
          : []
      }
    ]
  }

  const kpiCards = [
    {
      title: '实时总功率',
      value: formatNumber(dashboardData?.totalPower),
      unit: dashboardData?.totalPower >= 10000 ? '万kW' : 'kW',
      icon: <ThunderboltOutlined />,
      color: '#1890ff',
      type: 'primary',
      trend: 'up',
      trendValue: '5.2%'
    },
    {
      title: '今日发电量',
      value: formatNumber(dashboardData?.todayGeneration),
      unit: dashboardData?.todayGeneration >= 10000 ? '万kWh' : 'kWh',
      icon: <RiseOutlined />,
      color: '#52c41a',
      type: 'success',
      trend: 'up',
      trendValue: '8.3%'
    },
    {
      title: '累计减排',
      value: formatNumber(dashboardData?.totalEmissionReduction),
      unit: dashboardData?.totalEmissionReduction >= 10000 ? '万tCO₂' : 'tCO₂',
      icon: <LeafOutlined />,
      color: '#13c2c2',
      type: 'success',
      trend: 'up',
      trendValue: '3.1%'
    },
    {
      title: '设备在线率',
      value: dashboardData?.onlineRate?.toFixed(1) || '0',
      unit: '%',
      icon: <HeartOutlined />,
      color: '#722ed1',
      type: 'warning',
      trend: 'up',
      trendValue: '2.1%'
    },
    {
      title: '告警数量',
      value: dashboardData?.alarmCount || '0',
      unit: '条',
      icon: <BellOutlined />,
      color: '#faad14',
      type: 'warning'
    },
    {
      title: '未处理工单',
      value: dashboardData?.unhandledWorkOrderCount || '0',
      unit: '个',
      icon: <FileTextOutlined />,
      color: '#ff4d4f',
      type: 'danger'
    }
  ]

  const handleMapClick = (params) => {
    if (params.dataType === 'point' && params.data?.stationId) {
      const station = dashboardData?.stationMapList?.find(s => s.stationId === params.data.stationId)
      if (station) {
        handleStationClick(station)
      }
    }
  }

  const onEvents = {
    click: handleMapClick
  }

  return (
    <div className={`big-screen-wrapper ${isFullscreen ? 'big-screen-fullscreen' : ''}`} ref={containerRef}>
      <div className="big-screen-container">
        <div className="big-screen-header">
          <div className="big-screen-actions">
            {drillDownLevel > 0 && (
              <button className="big-screen-btn" onClick={handleBackToOverview}>
                <ArrowLeftOutlined /> 返回总览
              </button>
            )}
            <button
              className={`big-screen-btn ${isCarousel ? 'active' : ''}`}
              onClick={toggleCarousel}
              title={isCarousel ? '暂停轮播' : '开始轮播'}
            >
              {isCarousel ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
              {isCarousel ? ' 暂停轮播' : ' 自动轮播'}
            </button>
            <button
              className="big-screen-btn"
              onClick={refreshData}
              title="刷新数据"
            >
              <ReloadOutlined /> 刷新
            </button>
            <button
              className="big-screen-btn"
              onClick={toggleFullscreen}
              title={isFullscreen ? '退出全屏' : '全屏投屏'}
            >
              {isFullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
              {isFullscreen ? ' 退出全屏' : ' 全屏投屏'}
            </button>
          </div>

          <h1 className="big-screen-title">
            {drillDownLevel === 0 ? '光伏电站运维驾驶舱' : `${selectedStation?.stationName} - 逆变器监控`}
          </h1>
          <div className="big-screen-subtitle">
            实时总览 · 智能监控 · 数据每10秒自动刷新
          </div>

          <div className="big-screen-time">
            <div>{formatTime(currentTime)}</div>
            <div className="big-screen-connection-status">
              <span className={`big-screen-connection-dot ${wsConnected ? 'connected' : 'disconnected'}`} />
              {wsConnected ? 'WebSocket 已连接' : 'WebSocket 连接断开'}
            </div>
          </div>
        </div>

        <div className="big-screen-content">
          <div className="big-screen-left">
            <div className="big-screen-kpi-grid">
              {kpiCards.map((card, index) => (
                <div
                  key={index}
                  className={`big-screen-kpi-card ${card.type !== 'primary' ? card.type : ''}`}
                >
                  <div className="big-screen-kpi-icon" style={{ color: card.color }}>
                    {card.icon}
                  </div>
                  <div className="big-screen-kpi-label">{card.title}</div>
                  <div className="big-screen-kpi-value">
                    {card.value}
                    <span className="big-screen-kpi-unit">{card.unit}</span>
                  </div>
                  {card.trend && (
                    <div className={`big-screen-kpi-trend ${card.trend}`}>
                      {card.trend === 'up' ? '↑' : '↓'} {card.trendValue}
                    </div>
                  )}
                </div>
              ))}
            </div>

            <div className="big-screen-panel" style={{ height: 'calc(50% - 100px)' }}>
              <div className="big-screen-panel-title">功率趋势（近24小时）</div>
              <div className="big-screen-chart-container">
                <ReactECharts
                  option={powerTrendOption}
                  style={{ height: '100%' }}
                  loading={loading}
                />
              </div>
            </div>

            <div className="big-screen-panel" style={{ height: 'calc(50% - 100px)', marginTop: '12px' }}>
              <div className="big-screen-panel-title">发电量趋势（近7天）</div>
              <div className="big-screen-chart-container">
                <ReactECharts
                  option={generationTrendOption}
                  style={{ height: '100%' }}
                  loading={loading}
                />
              </div>
            </div>
          </div>

          <div className="big-screen-center">
            <div className="big-screen-panel" style={{ height: '100%' }}>
              <div className="big-screen-panel-title">
                {drillDownLevel === 0 ? '电站分布地图' : `${selectedStation?.stationName} 逆变器分布`}
                <span style={{ marginLeft: 'auto', fontSize: '12px', color: 'rgba(255,255,255,0.5)' }}>
                  共 {dashboardData?.stationCount || 0} 个电站 · {dashboardData?.inverterCount || 0} 台逆变器
                </span>
              </div>
              <div className="big-screen-map-container">
                <ReactECharts
                  key={mapRegistered ? 'map-ready' : 'map-loading'}
                  ref={chartRef}
                  option={mapOption}
                  style={{ height: '100%' }}
                  loading={loading || !mapRegistered}
                  onEvents={onEvents}
                  opts={{ renderer: 'canvas' }}
                />
                <div className="big-screen-map-legend">
                  <div className="big-screen-map-legend-item">
                    <span className="big-screen-map-legend-dot green" />
                    <span>健康（优秀）</span>
                  </div>
                  <div className="big-screen-map-legend-item">
                    <span className="big-screen-map-legend-dot yellow" />
                    <span>注意（良好）</span>
                  </div>
                  <div className="big-screen-map-legend-item">
                    <span className="big-screen-map-legend-dot red" />
                    <span>异常（差）</span>
                  </div>
                </div>
                {isCarousel && (
                  <div className="big-screen-carousel-indicator">
                    {(dashboardData?.stationMapList || []).map((_, index) => (
                      <span
                        key={index}
                        className={`big-screen-carousel-dot ${carouselIndex === index ? 'active' : ''}`}
                        onClick={() => handleCarouselClick(index)}
                      />
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>

          <div className="big-screen-right">
            <div className="big-screen-panel" style={{ height: '45%' }}>
              <div className="big-screen-panel-title">设备状态分布</div>
              <div className="big-screen-chart-container">
                <ReactECharts
                  option={stationStatusPieOption}
                  style={{ height: '100%' }}
                  loading={loading}
                />
              </div>
            </div>

            <div className="big-screen-panel" style={{ height: 'calc(55% - 12px)', marginTop: '12px' }}>
              <div className="big-screen-panel-title">
                {drillDownLevel === 0 ? '电站列表' : '逆变器列表'}
              </div>
              <div className="big-screen-station-list">
                {drillDownLevel === 0 ? (
                  (dashboardData?.stationMapList || []).map((station) => (
                    <div
                      key={station.stationId}
                      className={`big-screen-station-item ${selectedStation?.stationId === station.stationId ? 'active' : ''}`}
                      onClick={() => handleStationClick(station)}
                    >
                      <span className={`big-screen-station-health ${station.healthColor}`} />
                      <div className="big-screen-station-info">
                        <div className="big-screen-station-name">{station.stationName}</div>
                        <div className="big-screen-station-meta">
                          <span>{station.inverterCount}台逆变器</span>
                          <span>{station.onlineInverterCount}台在线</span>
                          {station.alarmCount > 0 && (
                            <span style={{ color: '#faad14' }}>{station.alarmCount}条告警</span>
                          )}
                        </div>
                      </div>
                      <div className="big-screen-station-power">
                        {station.currentPower?.toFixed(1)} kW
                      </div>
                    </div>
                  ))
                ) : (
                  inverterData.map((inv) => {
                    const status = getInverterStatus(inv)
                    return (
                      <div
                        key={inv.id}
                        className={`big-screen-station-item ${inv.onlineStatus === 1 ? '' : 'offline'}`}
                      >
                        <span className={`big-screen-station-health ${inv.healthColor}`} />
                        <div className="big-screen-station-info">
                          <div className="big-screen-station-name">
                            {inv.deviceName || inv.deviceSn}
                          </div>
                          <div className="big-screen-station-meta">
                            <span>{inv.deviceModel}</span>
                            <span>{inv.installLocation}</span>
                          </div>
                        </div>
                        <div className="big-screen-station-power">
                          {inv.currentPower?.toFixed(1)} kW
                        </div>
                      </div>
                    )
                  })
                )}
              </div>
            </div>
          </div>
        </div>

        <Modal
          title={`${selectedStation?.stationName} - 逆变器实时监控`}
          open={inverterModalVisible}
          onCancel={handleCloseInverterModal}
          width={1200}
          footer={null}
          className="big-screen-inverter-modal"
          destroyOnClose
        >
          {inverterLoading ? (
            <div style={{ textAlign: 'center', padding: '40px', color: 'rgba(255,255,255,0.6)' }}>
              加载中...
            </div>
          ) : (
            <div className="big-screen-inverter-grid">
              {inverterData.map((inv) => {
                const status = getInverterStatus(inv)
                return (
                  <div
                    key={inv.id}
                    className={`big-screen-inverter-card ${status === 'offline' ? 'offline' : ''}`}
                  >
                    <div className="big-screen-inverter-header">
                      <span className="big-screen-inverter-name">
                        {inv.deviceName || inv.deviceSn}
                      </span>
                      <span className={`big-screen-inverter-status ${status}`}>
                        {getInverterStatusText(status)}
                      </span>
                    </div>
                    <div className="big-screen-inverter-data">
                      <div className="big-screen-inverter-data-item">
                        <div className="big-screen-inverter-data-label">当前功率</div>
                        <div className="big-screen-inverter-data-value">
                          {inv.currentPower?.toFixed(1)} kW
                        </div>
                      </div>
                      <div className="big-screen-inverter-data-item">
                        <div className="big-screen-inverter-data-label">今日发电</div>
                        <div className="big-screen-inverter-data-value">
                          {inv.dayGeneration?.toFixed(1)} kWh
                        </div>
                      </div>
                      <div className="big-screen-inverter-data-item">
                        <div className="big-screen-inverter-data-label">电压</div>
                        <div className="big-screen-inverter-data-value">
                          {inv.voltage?.toFixed(1)} V
                        </div>
                      </div>
                      <div className="big-screen-inverter-data-item">
                        <div className="big-screen-inverter-data-label">电流</div>
                        <div className="big-screen-inverter-data-value">
                          {inv.current?.toFixed(1)} A
                        </div>
                      </div>
                      <div className="big-screen-inverter-data-item">
                        <div className="big-screen-inverter-data-label">温度</div>
                        <div className={`big-screen-inverter-data-value ${getTemperatureColor(inv.temperature)}`}>
                          {inv.temperature?.toFixed(1)} ℃
                        </div>
                      </div>
                      <div className="big-screen-inverter-data-item">
                        <div className="big-screen-inverter-data-label">效率</div>
                        <div className="big-screen-inverter-data-value">
                          {inv.efficiency?.toFixed(1)} %
                        </div>
                      </div>
                    </div>
                    {inv.alarmMessage && (
                      <div style={{ marginTop: 8, padding: 6, background: 'rgba(255,77,79,0.15)', borderRadius: 4, fontSize: 11, color: '#ff4d4f' }}>
                        ⚠ {inv.alarmMessage}
                      </div>
                    )}
                  </div>
                )
              })}
              {inverterData.length === 0 && (
                <div style={{ gridColumn: '1/-1', textAlign: 'center', padding: '40px', color: 'rgba(255,255,255,0.5)' }}>
                  暂无逆变器数据
                </div>
              )}
            </div>
          )}
        </Modal>
      </div>
    </div>
  )
}

export default BigScreenDashboard
