import React, { useState, useEffect, useRef, useCallback } from 'react'
import {
  Row,
  Col,
  Table,
  Card,
  Tabs,
  Select,
  Button,
  Space,
  Tag,
  Input,
  message,
  Tooltip
} from 'antd'
import {
  ReloadOutlined,
  SearchOutlined,
  EnvironmentOutlined,
  ThunderboltOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import StatusTag from '../../components/StatusTag'
import { getInverterList, getInverterListByStation } from '../../api/inverter'
import { getAllDeviceRealtimeData, getDeviceRealtimeData, getDeviceHistoryData } from '../../api/device'
import { getDeviceDataWebSocket, closeDeviceDataWebSocket } from '../../utils/websocket'

const { Search } = Input
const { Option } = Select

const mapOnlineStatus = (onlineStatus, faultCode) => {
  if (faultCode) return 'fault'
  return onlineStatus === 1 ? 'online' : 'offline'
}

const InverterMonitor = () => {
  const [viewMode, setViewMode] = useState('list')
  const [selectedInverter, setSelectedInverter] = useState(null)
  const [realtimeDetail, setRealtimeDetail] = useState(null)
  const [historyData, setHistoryData] = useState([])
  const [tableData, setTableData] = useState([])
  const [loading, setLoading] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [stationFilter, setStationFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [searchText, setSearchText] = useState('')
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })
  const [stationOptions, setStationOptions] = useState([])
  const chartRef = useRef(null)
  const wsRef = useRef(null)
  const realtimeMapRef = useRef({})

  const fetchInverterList = useCallback(async (page = 1, pageSize = 10, stationId = '', status = '', search = '') => {
    setLoading(true)
    try {
      const params = {
        pageNum: page,
        pageSize
      }
      if (stationId) params.stationId = stationId
      if (status) params.onlineStatus = status === 'online' ? 1 : status === 'offline' ? 0 : undefined
      if (search) params.deviceSn = search

      const res = await getInverterList(params)
      if (res.code === 200) {
        const { total, list, pageNum } = res.data
        const mergedList = (list || []).map(item => ({
          ...item,
          name: item.deviceName || item.deviceSn,
          station: item.stationId,
          capacity: item.ratedPower,
          status: mapOnlineStatus(item.onlineStatus, item.faultCode),
          power: realtimeMapRef.current[item.id]?.power ?? 0,
          voltage: realtimeMapRef.current[item.id]?.voltage ?? 0,
          current: realtimeMapRef.current[item.id]?.current ?? 0,
          temperature: realtimeMapRef.current[item.id]?.temperature ?? 0,
          efficiency: realtimeMapRef.current[item.id]?.efficiency ?? 0,
          longitude: realtimeMapRef.current[item.id]?.longitude ?? item.longitude ?? 116.4,
          latitude: realtimeMapRef.current[item.id]?.latitude ?? item.latitude ?? 39.9
        }))

        setTableData(mergedList)
        setPagination(prev => ({ ...prev, current: pageNum || page, total: total || 0 }))

        const stations = [...new Set((list || []).map(item => item.stationId).filter(Boolean))]
        if (stations.length > 0) {
          setStationOptions(prev => {
            const merged = [...new Set([...prev, ...stations])]
            return merged
          })
        }
      } else {
        message.error(res.message || '获取逆变器列表失败')
      }
    } catch (err) {
      message.error('获取逆变器列表失败')
    } finally {
      setLoading(false)
    }
  }, [])

  const fetchAllRealtimeData = useCallback(async () => {
    try {
      const res = await getAllDeviceRealtimeData()
      if (res.code === 200) {
        const data = res.data
        const map = {}
        if (Array.isArray(data)) {
          data.forEach(item => {
            map[item.deviceId] = item
          })
        } else if (data && typeof data === 'object') {
          Object.keys(data).forEach(key => {
            map[key] = data[key]
          })
        }
        realtimeMapRef.current = map

        setTableData(prev => prev.map(item => {
          const rt = map[item.id]
          if (rt) {
            return {
              ...item,
              power: rt.power ?? item.power,
              voltage: rt.voltage ?? item.voltage,
              current: rt.current ?? item.current,
              temperature: rt.temperature ?? item.temperature,
              efficiency: rt.efficiency ?? item.efficiency,
              longitude: rt.longitude ?? item.longitude,
              latitude: rt.latitude ?? item.latitude
            }
          }
          return item
        }))
      }
    } catch (err) {
      console.error('获取实时数据失败', err)
    }
  }, [])

  const fetchInverterDetail = useCallback(async (inverter) => {
    setDetailLoading(true)
    try {
      const [realtimeRes, historyRes] = await Promise.all([
        getDeviceRealtimeData(inverter.id),
        getDeviceHistoryData(inverter.id, { timeRange: '1h' })
      ])

      if (realtimeRes.code === 200) {
        const rt = realtimeRes.data
        setRealtimeDetail({
          power: rt.power ?? 0,
          voltage: rt.voltage ?? 0,
          current: rt.current ?? 0,
          temperature: rt.temperature ?? 0,
          efficiency: rt.efficiency ?? 0
        })
      } else {
        message.error(realtimeRes.message || '获取实时详情失败')
      }

      if (historyRes.code === 200) {
        const hList = historyRes.data?.list || historyRes.data || []
        setHistoryData(Array.isArray(hList) ? hList : [])
      } else {
        setHistoryData([])
      }
    } catch (err) {
      message.error('获取逆变器详情失败')
    } finally {
      setDetailLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchInverterList()
    fetchAllRealtimeData()
  }, [fetchInverterList, fetchAllRealtimeData])

  useEffect(() => {
    const ws = getDeviceDataWebSocket()
    wsRef.current = ws

    const handleMessage = (data) => {
      if (!data || !data.deviceId) return

      realtimeMapRef.current[data.deviceId] = {
        ...realtimeMapRef.current[data.deviceId],
        ...data
      }

      setTableData(prev => prev.map(item => {
        if (item.id === data.deviceId || item.id === Number(data.deviceId)) {
          return {
            ...item,
            power: data.power ?? item.power,
            voltage: data.voltage ?? item.voltage,
            current: data.current ?? item.current,
            temperature: data.temperature ?? item.temperature,
            efficiency: data.efficiency ?? item.efficiency
          }
        }
        return item
      }))

      if (selectedInverter && (selectedInverter.id === data.deviceId || selectedInverter.id === Number(data.deviceId))) {
        setRealtimeDetail(prev => prev ? {
          ...prev,
          power: data.power ?? prev.power,
          voltage: data.voltage ?? prev.voltage,
          current: data.current ?? prev.current,
          temperature: data.temperature ?? prev.temperature,
          efficiency: data.efficiency ?? prev.efficiency
        } : prev)

        setHistoryData(prev => {
          if (!prev || prev.length === 0) return prev
          const now = new Date()
          const timeStr = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
          const newPoint = {
            time: timeStr,
            power: data.power ?? 0,
            voltage: data.voltage ?? 0,
            current: data.current ?? 0
          }
          return [...prev.slice(1), newPoint]
        })
      }
    }

    ws.on('message', handleMessage)
    ws.connect()

    return () => {
      ws.off('message', handleMessage)
      closeDeviceDataWebSocket()
      wsRef.current = null
    }
  }, [])

  useEffect(() => {
    if (selectedInverter) {
      fetchInverterDetail(selectedInverter)
    }
  }, [selectedInverter?.id])

  const handleRefresh = async () => {
    await Promise.all([
      fetchInverterList(pagination.current, pagination.pageSize, stationFilter, statusFilter, searchText),
      fetchAllRealtimeData()
    ])
    if (selectedInverter) {
      fetchInverterDetail(selectedInverter)
    }
    message.success('数据已刷新')
  }

  const handleTableChange = (pag) => {
    setPagination(prev => ({ ...prev, current: pag.current, pageSize: pag.pageSize }))
    fetchInverterList(pag.current, pag.pageSize, stationFilter, statusFilter, searchText)
  }

  const handleFilterChange = (filterType, value) => {
    if (filterType === 'station') {
      setStationFilter(value || '')
      fetchInverterList(1, pagination.pageSize, value || '', statusFilter, searchText)
    } else if (filterType === 'status') {
      setStatusFilter(value || '')
      fetchInverterList(1, pagination.pageSize, stationFilter, value || '', searchText)
    }
    setPagination(prev => ({ ...prev, current: 1 }))
  }

  const handleSearch = (value) => {
    setSearchText(value)
    fetchInverterList(1, pagination.pageSize, stationFilter, statusFilter, value)
    setPagination(prev => ({ ...prev, current: 1 }))
  }

  const handleSelectInverter = (record) => {
    setSelectedInverter(record)
    setRealtimeDetail({
      power: record.power,
      voltage: record.voltage,
      current: record.current,
      temperature: record.temperature,
      efficiency: record.efficiency
    })
  }

  const chartOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['功率(kW)', '电压(V)', '电流(A)'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: historyData.map(item => item.time || item.timestamp || '')
    },
    yAxis: [
      { type: 'value', name: '功率/电压', position: 'left' },
      { type: 'value', name: '电流', position: 'right' }
    ],
    series: [
      {
        name: '功率(kW)',
        type: 'line',
        smooth: true,
        data: historyData.map(item => item.power != null ? Number(item.power).toFixed(1) : '-'),
        itemStyle: { color: '#1890ff' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
              { offset: 1, color: 'rgba(24, 144, 255, 0)' }
            ]
          }
        }
      },
      {
        name: '电压(V)',
        type: 'line',
        smooth: true,
        data: historyData.map(item => item.voltage != null ? Number(item.voltage).toFixed(1) : '-'),
        itemStyle: { color: '#52c41a' }
      },
      {
        name: '电流(A)',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: historyData.map(item => item.current != null ? Number(item.current).toFixed(1) : '-'),
        itemStyle: { color: '#faad14' }
      }
    ]
  }

  const mapOption = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        if (params.dataType === 'point') {
          const data = params.data
          return `
            <div>
              <div><strong>${data.name}</strong></div>
              <div>状态：${data.status === 'online' ? '在线' : data.status === 'fault' ? '故障' : '离线'}</div>
              <div>功率：${data.power} kW</div>
              <div>温度：${data.temperature} ℃</div>
            </div>
          `
        }
      }
    },
    geo: {
      map: 'china',
      roam: true,
      zoom: 5,
      center: [116.4, 39.9],
      itemStyle: {
        areaColor: '#f0f2f5',
        borderColor: '#d9d9d9'
      },
      emphasis: {
        itemStyle: { areaColor: '#e6f7ff' }
      }
    },
    series: [
      {
        name: '逆变器点位',
        type: 'scatter',
        coordinateSystem: 'geo',
        symbolSize: (val, params) => {
          const baseSize = 15
          return params.data.status === 'online' ? baseSize : params.data.status === 'fault' ? baseSize + 5 : baseSize
        },
        data: tableData.map(item => ({
          name: item.name,
          value: [item.longitude, item.latitude, item.power],
          status: item.status,
          power: Number(item.power).toFixed(1),
          temperature: Number(item.temperature).toFixed(1),
          itemStyle: {
            color: item.status === 'online' ? '#52c41a' : item.status === 'fault' ? '#ff4d4f' : '#8c8c8c'
          }
        })),
        label: {
          show: true,
          formatter: '{b}',
          position: 'right',
          fontSize: 10
        },
        emphasis: {
          label: { show: true, fontSize: 12, fontWeight: 'bold' }
        }
      }
    ]
  }

  const columns = [
    {
      title: '设备编号',
      dataIndex: 'deviceSn',
      key: 'deviceSn',
      width: 120,
      render: (text) => <Tag color="blue">{text}</Tag>
    },
    {
      title: '设备名称',
      dataIndex: 'deviceName',
      key: 'deviceName',
      width: 120
    },
    {
      title: '所属电站',
      dataIndex: 'stationId',
      key: 'stationId',
      width: 120
    },
    {
      title: '设备型号',
      dataIndex: 'deviceModel',
      key: 'deviceModel',
      width: 120
    },
    {
      title: '额定功率(kW)',
      dataIndex: 'ratedPower',
      key: 'ratedPower',
      width: 120,
      sorter: (a, b) => (a.ratedPower || 0) - (b.ratedPower || 0)
    },
    {
      title: '实时功率(kW)',
      dataIndex: 'power',
      key: 'power',
      width: 120,
      sorter: (a, b) => (a.power || 0) - (b.power || 0),
      render: (val) => <span style={{ color: '#1890ff', fontWeight: 500 }}>{val != null ? Number(val).toFixed(1) : '-'}</span>
    },
    {
      title: '温度(℃)',
      dataIndex: 'temperature',
      key: 'temperature',
      width: 100,
      sorter: (a, b) => (a.temperature || 0) - (b.temperature || 0),
      render: (val) => {
        if (val == null) return '-'
        const color = val > 50 ? '#ff4d4f' : val > 45 ? '#faad14' : '#52c41a'
        return <span style={{ color }}>{Number(val).toFixed(1)}</span>
      }
    },
    {
      title: '效率(%)',
      dataIndex: 'efficiency',
      key: 'efficiency',
      width: 100,
      sorter: (a, b) => (a.efficiency || 0) - (b.efficiency || 0),
      render: (val) => val != null ? Number(val).toFixed(1) : '-'
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => <StatusTag status={status} />
    },
    {
      title: '最后在线时间',
      dataIndex: 'lastOnlineTime',
      key: 'lastOnlineTime',
      width: 160
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleSelectInverter(record)}>
            查看详情
          </Button>
        </Space>
      )
    }
  ]

  return (
    <div className="inverter-monitor-page">
      <Card
        title="逆变器监控"
        extra={
          <Space>
            <Tabs
              activeKey={viewMode}
              onChange={setViewMode}
              size="small"
              items={[
                { key: 'list', label: '列表模式' },
                { key: 'map', label: '地图模式' }
              ]}
            />
            <Button
              icon={<ReloadOutlined />}
              onClick={handleRefresh}
              loading={loading}
            >
              刷新
            </Button>
          </Space>
        }
      >
        {viewMode === 'list' && (
          <>
            <Space style={{ marginBottom: 16 }} wrap>
              <Select
                placeholder="选择电站"
                style={{ width: 180 }}
                allowClear
                value={stationFilter || undefined}
                onChange={(val) => handleFilterChange('station', val)}
              >
                {stationOptions.map(station => (
                  <Option key={station} value={station}>{station}</Option>
                ))}
              </Select>
              <Select
                placeholder="设备状态"
                style={{ width: 150 }}
                allowClear
                value={statusFilter || undefined}
                onChange={(val) => handleFilterChange('status', val)}
              >
                <Option value="online">在线</Option>
                <Option value="offline">离线</Option>
                <Option value="fault">故障</Option>
              </Select>
              <Search
                placeholder="搜索设备编号"
                style={{ width: 200 }}
                allowClear
                enterButton={<SearchOutlined />}
                onSearch={handleSearch}
              />
            </Space>
            <Table
              columns={columns}
              dataSource={tableData}
              rowKey="id"
              loading={loading}
              pagination={{
                current: pagination.current,
                pageSize: pagination.pageSize,
                total: pagination.total,
                showSizeChanger: true,
                showTotal: (total) => `共 ${total} 条`
              }}
              onChange={handleTableChange}
              scroll={{ x: 1200 }}
            />
          </>
        )}

        {viewMode === 'map' && (
          <div style={{ height: 500 }}>
            <ReactECharts option={mapOption} style={{ height: '100%' }} />
          </div>
        )}
      </Card>

      {selectedInverter && viewMode === 'list' && (
        <Card
          title={`${selectedInverter.deviceName || selectedInverter.deviceSn} 实时数据`}
          style={{ marginTop: 16 }}
          extra={<StatusTag status={selectedInverter.status} />}
          loading={detailLoading}
        >
          <Row gutter={16}>
            <Col xs={24} md={6}>
              <div className="inverter-detail-item">
                <div className="detail-label">当前功率</div>
                <div className="detail-value power">
                  {realtimeDetail?.power != null ? Number(realtimeDetail.power).toFixed(1) : '-'} kW
                </div>
              </div>
            </Col>
            <Col xs={24} md={6}>
              <div className="inverter-detail-item">
                <div className="detail-label">设备温度</div>
                <div className="detail-value temp">
                  {realtimeDetail?.temperature != null ? Number(realtimeDetail.temperature).toFixed(1) : '-'} ℃
                </div>
              </div>
            </Col>
            <Col xs={24} md={6}>
              <div className="inverter-detail-item">
                <div className="detail-label">转换效率</div>
                <div className="detail-value efficiency">
                  {realtimeDetail?.efficiency != null ? Number(realtimeDetail.efficiency).toFixed(1) : '-'} %
                </div>
              </div>
            </Col>
            <Col xs={24} md={6}>
              <div className="inverter-detail-item">
                <div className="detail-label">额定功率</div>
                <div className="detail-value">
                  {selectedInverter.ratedPower != null ? selectedInverter.ratedPower : '-'} kW
                </div>
              </div>
            </Col>
          </Row>
          <Row gutter={16} style={{ marginTop: 8 }}>
            <Col xs={24} md={6}>
              <div className="inverter-detail-item">
                <div className="detail-label">电压</div>
                <div className="detail-value">
                  {realtimeDetail?.voltage != null ? Number(realtimeDetail.voltage).toFixed(1) : '-'} V
                </div>
              </div>
            </Col>
            <Col xs={24} md={6}>
              <div className="inverter-detail-item">
                <div className="detail-label">电流</div>
                <div className="detail-value">
                  {realtimeDetail?.current != null ? Number(realtimeDetail.current).toFixed(1) : '-'} A
                </div>
              </div>
            </Col>
            <Col xs={24} md={6}>
              <div className="inverter-detail-item">
                <div className="detail-label">设备型号</div>
                <div className="detail-value">{selectedInverter.deviceModel || '-'}</div>
              </div>
            </Col>
            <Col xs={24} md={6}>
              <div className="inverter-detail-item">
                <div className="detail-label">最后在线</div>
                <div className="detail-value">{selectedInverter.lastOnlineTime || '-'}</div>
              </div>
            </Col>
          </Row>
          <div style={{ marginTop: 16 }}>
            <ReactECharts option={chartOption} style={{ height: 300 }} />
          </div>
        </Card>
      )}
    </div>
  )
}

export default InverterMonitor
