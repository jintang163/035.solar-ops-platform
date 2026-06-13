import React, { useState, useEffect, useRef } from 'react'
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

const { Search } = Input
const { Option } = Select

const InverterMonitor = () => {
  const [viewMode, setViewMode] = useState('list')
  const [selectedInverter, setSelectedInverter] = useState(null)
  const [realTimeData, setRealTimeData] = useState([])
  const [tableData, setTableData] = useState([])
  const [loading, setLoading] = useState(false)
  const [stationFilter, setStationFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const chartRef = useRef(null)
  const wsRef = useRef(null)

  const mockInverters = [
    { id: 1, name: 'INV-001', station: '一号光伏电站', capacity: 100, status: 'online', power: 85.6, temperature: 42.3, efficiency: 95.2, longitude: 116.404, latitude: 39.915 },
    { id: 2, name: 'INV-002', station: '一号光伏电站', capacity: 100, status: 'online', power: 92.1, temperature: 44.5, efficiency: 96.1, longitude: 116.408, latitude: 39.917 },
    { id: 3, name: 'INV-003', station: '一号光伏电站', capacity: 100, status: 'fault', power: 0, temperature: 25.0, efficiency: 0, longitude: 116.412, latitude: 39.913 },
    { id: 4, name: 'INV-004', station: '二号光伏电站', capacity: 150, status: 'online', power: 128.5, temperature: 45.2, efficiency: 94.8, longitude: 116.42, latitude: 39.9 },
    { id: 5, name: 'INV-005', station: '二号光伏电站', capacity: 150, status: 'online', power: 135.2, temperature: 46.8, efficiency: 95.5, longitude: 116.425, latitude: 39.905 },
    { id: 6, name: 'INV-006', station: '二号光伏电站', capacity: 150, status: 'warning', power: 110.3, temperature: 52.1, efficiency: 92.1, longitude: 116.43, latitude: 39.91 },
    { id: 7, name: 'INV-007', station: '三号光伏电站', capacity: 200, status: 'offline', power: 0, temperature: 0, efficiency: 0, longitude: 116.38, latitude: 39.92 },
    { id: 8, name: 'INV-008', station: '三号光伏电站', capacity: 200, status: 'online', power: 175.8, temperature: 43.6, efficiency: 95.9, longitude: 116.385, latitude: 39.925 },
    { id: 9, name: 'INV-009', station: '四号光伏电站', capacity: 120, status: 'online', power: 98.7, temperature: 41.2, efficiency: 94.5, longitude: 116.45, latitude: 39.89 },
    { id: 10, name: 'INV-010', station: '五号光伏电站', capacity: 180, status: 'online', power: 156.3, temperature: 44.8, efficiency: 95.7, longitude: 116.35, latitude: 39.93 }
  ]

  useEffect(() => {
    setTableData(mockInverters)
    setSelectedInverter(mockInverters[0])
    generateInitialChartData()
  }, [])

  const generateInitialChartData = () => {
    const now = new Date()
    const data = []
    for (let i = 59; i >= 0; i--) {
      const time = new Date(now.getTime() - i * 60000)
      const timeStr = `${time.getHours().toString().padStart(2, '0')}:${time.getMinutes().toString().padStart(2, '0')}`
      data.push({
        time: timeStr,
        power: 80 + Math.random() * 30,
        voltage: 380 + Math.random() * 20,
        current: 150 + Math.random() * 50
      })
    }
    setRealTimeData(data)
  }

  useEffect(() => {
    const interval = setInterval(() => {
      const now = new Date()
      const timeStr = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
      
      setRealTimeData(prev => {
        const newData = [...prev.slice(1)]
        newData.push({
          time: timeStr,
          power: 80 + Math.random() * 30,
          voltage: 380 + Math.random() * 20,
          current: 150 + Math.random() * 50
        })
        return newData
      })

      setTableData(prev => prev.map(item => {
        if (item.status === 'online') {
          return {
            ...item,
            power: Math.max(0, item.power + (Math.random() - 0.5) * 5),
            temperature: Math.max(20, item.temperature + (Math.random() - 0.5) * 2)
          }
        }
        return item
      }))
    }, 3000)

    return () => clearInterval(interval)
  }, [])

  const chartOption = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['功率(kW)', '电压(V)', '电流(A)']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: realTimeData.map(item => item.time)
    },
    yAxis: [
      {
        type: 'value',
        name: '功率/电压',
        position: 'left'
      },
      {
        type: 'value',
        name: '电流',
        position: 'right'
      }
    ],
    series: [
      {
        name: '功率(kW)',
        type: 'line',
        smooth: true,
        data: realTimeData.map(item => item.power.toFixed(1)),
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
        data: realTimeData.map(item => item.voltage.toFixed(1)),
        itemStyle: { color: '#52c41a' }
      },
      {
        name: '电流(A)',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: realTimeData.map(item => item.current.toFixed(1)),
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
              <div>状态：${data.status === 'online' ? '在线' : data.status === 'fault' ? '故障' : data.status === 'warning' ? '告警' : '离线'}</div>
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
        itemStyle: {
          areaColor: '#e6f7ff'
        }
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
          power: item.power.toFixed(1),
          temperature: item.temperature.toFixed(1),
          itemStyle: {
            color: item.status === 'online' ? '#52c41a' : item.status === 'fault' ? '#ff4d4f' : item.status === 'warning' ? '#faad14' : '#8c8c8c'
          }
        })),
        label: {
          show: true,
          formatter: '{b}',
          position: 'right',
          fontSize: 10
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 12,
            fontWeight: 'bold'
          }
        }
      }
    ]
  }

  const columns = [
    {
      title: '设备编号',
      dataIndex: 'name',
      key: 'name',
      width: 100,
      render: (text) => <Tag color="blue">{text}</Tag>
    },
    {
      title: '所属电站',
      dataIndex: 'station',
      key: 'station',
      width: 120
    },
    {
      title: '容量(kW)',
      dataIndex: 'capacity',
      key: 'capacity',
      width: 100,
      sorter: (a, b) => a.capacity - b.capacity
    },
    {
      title: '实时功率(kW)',
      dataIndex: 'power',
      key: 'power',
      width: 120,
      sorter: (a, b) => a.power - b.power,
      render: (val) => <span style={{ color: '#1890ff', fontWeight: 500 }}>{val.toFixed(1)}</span>
    },
    {
      title: '温度(℃)',
      dataIndex: 'temperature',
      key: 'temperature',
      width: 100,
      sorter: (a, b) => a.temperature - b.temperature,
      render: (val) => {
        const color = val > 50 ? '#ff4d4f' : val > 45 ? '#faad14' : '#52c41a'
        return <span style={{ color }}>{val.toFixed(1)}</span>
      }
    },
    {
      title: '效率(%)',
      dataIndex: 'efficiency',
      key: 'efficiency',
      width: 100,
      sorter: (a, b) => a.efficiency - b.efficiency,
      render: (val) => val.toFixed(1)
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => <StatusTag status={status} />
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" onClick={() => setSelectedInverter(record)}>
            查看详情
          </Button>
        </Space>
      )
    }
  ]

  const handleRefresh = () => {
    setLoading(true)
    setTimeout(() => {
      setLoading(false)
      message.success('数据已刷新')
    }, 500)
  }

  const handleViewModeChange = (key) => {
    setViewMode(key)
  }

  const filteredData = tableData.filter(item => {
    const matchStation = !stationFilter || item.station === stationFilter
    const matchStatus = !statusFilter || item.status === statusFilter
    return matchStation && matchStatus
  })

  const stationOptions = [...new Set(mockInverters.map(item => item.station))]

  return (
    <div className="inverter-monitor-page">
      <Card
        title="逆变器监控"
        extra={
          <Space>
            <Tabs
              activeKey={viewMode}
              onChange={handleViewModeChange}
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
                onChange={setStationFilter}
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
                onChange={setStatusFilter}
              >
                <Option value="online">在线</Option>
                <Option value="offline">离线</Option>
                <Option value="fault">故障</Option>
                <Option value="warning">告警</Option>
              </Select>
              <Search
                placeholder="搜索设备编号"
                style={{ width: 200 }}
                allowClear
                enterButton={<SearchOutlined />}
              />
            </Space>
            <Table
              columns={columns}
              dataSource={filteredData}
              rowKey="id"
              loading={loading}
              pagination={{ pageSize: 10 }}
              scroll={{ x: 800 }}
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
          title={`${selectedInverter.name} 实时数据`}
          style={{ marginTop: 16 }}
          extra={<StatusTag status={selectedInverter.status} />}
        >
          <Row gutter={16}>
            <Col xs={24} md={6}>
              <div className="inverter-detail-item">
                <div className="detail-label">当前功率</div>
                <div className="detail-value power">{selectedInverter.power.toFixed(1)} kW</div>
              </div>
            </Col>
            <Col xs={24} md={6}>
              <div className="inverter-detail-item">
                <div className="detail-label">设备温度</div>
                <div className="detail-value temp">{selectedInverter.temperature.toFixed(1)} ℃</div>
              </div>
            </Col>
            <Col xs={24} md={6}>
              <div className="inverter-detail-item">
                <div className="detail-label">转换效率</div>
                <div className="detail-value efficiency">{selectedInverter.efficiency.toFixed(1)} %</div>
              </div>
            </Col>
            <Col xs={24} md={6}>
              <div className="inverter-detail-item">
                <div className="detail-label">额定容量</div>
                <div className="detail-value">{selectedInverter.capacity} kW</div>
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
