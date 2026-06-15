import React, { useState, useEffect, useRef } from 'react'
import {
  Card,
  Row,
  Col,
  DatePicker,
  Select,
  Button,
  Space,
  Tag,
  Table,
  Typography,
  Empty,
  Tooltip,
  message
} from 'antd'
import {
  ThunderboltOutlined,
  RiseOutlined,
  DollarOutlined,
  HeartOutlined,
  ApartmentOutlined,
  BarChartOutlined,
  LineChartOutlined,
  PieChartOutlined,
  DownloadOutlined,
  ReloadOutlined,
  WarningOutlined,
  InfoCircleOutlined
} from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import dayjs from 'dayjs'
import StatCard from '../../components/StatCard'
import ChartCard from '../../components/ChartCard'
import { getStationList } from '../../api/station'
import { getWorkspaceInfo } from '../../api/workspace'
import { getUser, isSuperAdmin } from '../../utils/auth'

const { RangePicker } = DatePicker
const { Option } = Select
const { Title, Text } = Typography

const COLORS = ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16']

const KPI_METRICS = [
  { key: 'totalGeneration', label: '总发电量', unit: '万kWh', color: '#1890ff', icon: <ThunderboltOutlined /> },
  { key: 'avgGeneration', label: '平均发电量', unit: '万kWh', color: '#52c41a', icon: <RiseOutlined /> },
  { key: 'totalRevenue', label: '总收益', unit: '万元', color: '#faad14', icon: <DollarOutlined /> },
  { key: 'avgEfficiency', label: '平均效率', unit: '%', color: '#722ed1', icon: <HeartOutlined /> },
  { key: 'totalInverters', label: '逆变器数量', unit: '台', color: '#13c2c2', icon: <ApartmentOutlined /> },
  { key: 'avgAvailability', label: '平均可用率', unit: '%', color: '#eb2f96', icon: <InfoCircleOutlined /> }
]

const GroupReport = () => {
  const actionRef = useRef()
  const [loading, setLoading] = useState(false)
  const [allStations, setAllStations] = useState([])
  const [selectedStations, setSelectedStations] = useState([])
  const [dateRange, setDateRange] = useState([dayjs().subtract(30, 'day'), dayjs()])
  const [reportData, setReportData] = useState([])
  const [kpiSummary, setKpiSummary] = useState({})
  const [workspace, setWorkspace] = useState(null)
  const [chartType, setChartType] = useState('bar')

  useEffect(() => {
    loadWorkspace()
    loadStations()
  }, [])

  useEffect(() => {
    if (selectedStations.length > 0) {
      loadReportData()
    }
  }, [selectedStations, dateRange])

  const loadWorkspace = async () => {
    try {
      const res = await getWorkspaceInfo()
      if (res.data) {
        setWorkspace(res.data)
      }
    } catch (e) {
      console.warn('获取工作空间失败', e)
    }
  }

  const loadStations = async () => {
    try {
      const res = await getStationList({ pageNum: 1, pageSize: 1000 })
      if (res.data?.list) {
        setAllStations(res.data.list)
        if (!isSuperAdmin() && res.data.list.length > 0) {
          setSelectedStations(res.data.list.slice(0, 5).map(s => s.id))
        }
      }
    } catch (e) {
      console.error('加载电站列表失败', e)
    }
  }

  const loadReportData = async () => {
    setLoading(true)
    try {
      await new Promise(resolve => setTimeout(resolve, 800))

      const mockData = selectedStations.map((stationId, index) => {
        const station = allStations.find(s => s.id === stationId)
        const baseGeneration = (station?.capacity || 1000) * 0.8 * (0.8 + Math.random() * 0.4)
        return {
          id: stationId,
          stationName: station?.stationName || `电站${stationId}`,
          stationCode: station?.stationCode || `ST${stationId}`,
          capacity: station?.capacity || 1000,
          orgName: station?.orgName || '-',
          totalGeneration: Number((baseGeneration * 30 / 10000).toFixed(2)),
          dayGeneration: Number((baseGeneration / 10000).toFixed(2)),
          monthGeneration: Number((baseGeneration * 30 / 10000).toFixed(2)),
          yearGeneration: Number((baseGeneration * 365 / 10000).toFixed(2)),
          totalRevenue: Number((baseGeneration * 30 * 0.38 / 10000).toFixed(2)),
          avgEfficiency: Number((85 + Math.random() * 10).toFixed(1)),
          avgAvailability: Number((92 + Math.random() * 7).toFixed(1)),
          totalInverters: Math.floor(50 + Math.random() * 100),
          onlineInverters: Math.floor(45 + Math.random() * 95),
          faultCount: Math.floor(Math.random() * 5),
          alarmCount: Math.floor(Math.random() * 10),
          irradiation: Number((3.5 + Math.random() * 2).toFixed(2)),
          performanceRatio: Number((75 + Math.random() * 15).toFixed(1)),
          equivalentHours: Number((80 + Math.random() * 40).toFixed(1)),
          coalSaving: Number((baseGeneration * 30 * 0.35 / 1000).toFixed(2)),
          co2Reduction: Number((baseGeneration * 30 * 0.785 / 1000).toFixed(2)),
          lastUpdate: dayjs().format('YYYY-MM-DD HH:mm')
        }
      })

      setReportData(mockData)

      const summary = {
        totalGeneration: mockData.reduce((s, d) => s + d.totalGeneration, 0).toFixed(2),
        avgGeneration: (mockData.reduce((s, d) => s + d.totalGeneration, 0) / mockData.length).toFixed(2),
        totalRevenue: mockData.reduce((s, d) => s + d.totalRevenue, 0).toFixed(2),
        avgEfficiency: (mockData.reduce((s, d) => s + d.avgEfficiency, 0) / mockData.length).toFixed(1),
        totalInverters: mockData.reduce((s, d) => s + d.totalInverters, 0),
        avgAvailability: (mockData.reduce((s, d) => s + d.avgAvailability, 0) / mockData.length).toFixed(1),
        totalFaults: mockData.reduce((s, d) => s + d.faultCount, 0),
        totalAlarms: mockData.reduce((s, d) => s + d.alarmCount, 0)
      }
      setKpiSummary(summary)
    } catch (e) {
      console.error('加载报表数据失败', e)
    } finally {
      setLoading(false)
    }
  }

  const handleSelectAllStations = () => {
    setSelectedStations(allStations.map(s => s.id))
  }

  const handleClearSelection = () => {
    setSelectedStations([])
    setReportData([])
    setKpiSummary({})
  }

  const handleExport = () => {
    message.success('报表导出中，请稍候...')
  }

  const getGenerationTrendOption = () => {
    const dates = []
    for (let i = 29; i >= 0; i--) {
      dates.push(dayjs().subtract(i, 'day').format('MM-DD'))
    }

    const series = reportData.slice(0, 6).map((station, index) => ({
      name: station.stationName,
      type: chartType === 'bar' ? 'bar' : 'line',
      smooth: true,
      itemStyle: {
        color: COLORS[index % COLORS.length]
      },
      lineStyle: {
        width: chartType === 'line' ? 2 : 0
      },
      areaStyle: chartType === 'line' ? {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: `${COLORS[index % COLORS.length]}40` },
            { offset: 1, color: `${COLORS[index % COLORS.length]}05` }
          ]
        }
      } : undefined,
      data: dates.map(() => Number((station.capacity * 0.0008 * (0.7 + Math.random() * 0.5)).toFixed(2)))
    }))

    return {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' }
      },
      legend: {
        data: reportData.slice(0, 6).map(s => s.stationName),
        top: 0,
        type: 'scroll'
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        top: 50,
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: dates,
        axisLabel: { rotate: 45 }
      },
      yAxis: {
        type: 'value',
        name: '万kWh'
      },
      series
    }
  }

  const getPieOption = () => {
    return {
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c}万kWh ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        top: 'center'
      },
      series: [
        {
          name: '发电量占比',
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['60%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 8,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false,
            position: 'center'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 16,
              fontWeight: 'bold'
            }
          },
          labelLine: {
            show: false
          },
          data: reportData.map((d, i) => ({
            value: d.totalGeneration,
            name: d.stationName,
            itemStyle: { color: COLORS[i % COLORS.length] }
          }))
        }
      ]
    }
  }

  const getEfficiencyCompareOption = () => {
    return {
      tooltip: {
        trigger: 'axis'
      },
      legend: {
        data: ['发电效率', '可用率', 'PR值'],
        top: 0
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        top: 40,
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: reportData.map(d => d.stationName),
        axisLabel: { rotate: 30 }
      },
      yAxis: {
        type: 'value',
        name: '%',
        max: 100
      },
      series: [
        {
          name: '发电效率',
          type: 'bar',
          itemStyle: { color: '#1890ff' },
          data: reportData.map(d => d.avgEfficiency)
        },
        {
          name: '可用率',
          type: 'bar',
          itemStyle: { color: '#52c41a' },
          data: reportData.map(d => d.avgAvailability)
        },
        {
          name: 'PR值',
          type: 'bar',
          itemStyle: { color: '#faad14' },
          data: reportData.map(d => d.performanceRatio)
        }
      ]
    }
  }

  const columns = [
    {
      title: '电站名称',
      dataIndex: 'stationName',
      key: 'stationName',
      fixed: 'left',
      width: 160,
      render: (text, record) => (
        <Space>
          <Tag color="blue">{record.stationCode}</Tag>
          <span>{text}</span>
        </Space>
      )
    },
    {
      title: '所属组织',
      dataIndex: 'orgName',
      key: 'orgName',
      width: 120
    },
    {
      title: '装机容量',
      dataIndex: 'capacity',
      key: 'capacity',
      width: 100,
      render: (val) => `${val} kW`,
      sorter: (a, b) => a.capacity - b.capacity
    },
    {
      title: '今日发电量',
      dataIndex: 'dayGeneration',
      key: 'dayGeneration',
      width: 120,
      render: (val) => <span style={{ color: '#1890ff', fontWeight: 500 }}>{val} 万kWh</span>,
      sorter: (a, b) => a.dayGeneration - b.dayGeneration
    },
    {
      title: '本月发电量',
      dataIndex: 'monthGeneration',
      key: 'monthGeneration',
      width: 120,
      render: (val) => `${val} 万kWh`,
      sorter: (a, b) => a.monthGeneration - b.monthGeneration
    },
    {
      title: '本年发电量',
      dataIndex: 'yearGeneration',
      key: 'yearGeneration',
      width: 120,
      render: (val) => `${val} 万kWh`,
      sorter: (a, b) => a.yearGeneration - b.yearGeneration
    },
    {
      title: '发电收益',
      dataIndex: 'totalRevenue',
      key: 'totalRevenue',
      width: 100,
      render: (val) => <span style={{ color: '#faad14' }}>¥{val}万</span>,
      sorter: (a, b) => a.totalRevenue - b.totalRevenue
    },
    {
      title: '发电效率',
      dataIndex: 'avgEfficiency',
      key: 'avgEfficiency',
      width: 100,
      render: (val) => {
        const color = val >= 90 ? '#52c41a' : val >= 80 ? '#faad14' : '#f5222d'
        return <span style={{ color, fontWeight: 500 }}>{val}%</span>
      },
      sorter: (a, b) => a.avgEfficiency - b.avgEfficiency
    },
    {
      title: '设备可用率',
      dataIndex: 'avgAvailability',
      key: 'avgAvailability',
      width: 110,
      render: (val) => {
        const color = val >= 95 ? '#52c41a' : val >= 90 ? '#faad14' : '#f5222d'
        return <span style={{ color }}>{val}%</span>
      },
      sorter: (a, b) => a.avgAvailability - b.avgAvailability
    },
    {
      title: '逆变器',
      key: 'inverter',
      width: 100,
      render: (_, record) => (
        <Space>
          <span style={{ color: '#52c41a' }}>{record.onlineInverters}</span>
          <span style={{ color: '#999' }}>/</span>
          <span>{record.totalInverters}</span>
        </Space>
      )
    },
    {
      title: '故障数',
      dataIndex: 'faultCount',
      key: 'faultCount',
      width: 80,
      render: (val) => val > 0
        ? <Tag color="red" icon={<WarningOutlined />}>{val}</Tag>
        : <Tag color="green">0</Tag>,
      sorter: (a, b) => a.faultCount - b.faultCount
    },
    {
      title: '告警数',
      dataIndex: 'alarmCount',
      key: 'alarmCount',
      width: 80,
      render: (val) => val > 0
        ? <Tag color="orange">{val}</Tag>
        : <Tag color="green">0</Tag>,
      sorter: (a, b) => a.alarmCount - b.alarmCount
    },
    {
      title: 'PR值',
      dataIndex: 'performanceRatio',
      key: 'performanceRatio',
      width: 90,
      render: (val) => `${val}%`
    },
    {
      title: '利用小时',
      dataIndex: 'equivalentHours',
      key: 'equivalentHours',
      width: 90,
      render: (val) => `${val}h`
    },
    {
      title: '节煤量',
      dataIndex: 'coalSaving',
      key: 'coalSaving',
      width: 100,
      render: (val) => `${val}吨`
    },
    {
      title: 'CO₂减排',
      dataIndex: 'co2Reduction',
      key: 'co2Reduction',
      width: 100,
      render: (val) => `${val}吨`
    },
    {
      title: '更新时间',
      dataIndex: 'lastUpdate',
      key: 'lastUpdate',
      fixed: 'right',
      width: 160
    }
  ]

  return (
    <div className="group-report-page" style={{ padding: 16 }}>
      <Card
        style={{ marginBottom: 16 }}
        bodyStyle={{ padding: '16px 24px' }}
      >
        <Row align="middle" gutter={[16, 16]}>
          <Col xs={24} md={6}>
            <Space direction="vertical" size={0}>
              <Title level={4} style={{ margin: 0 }}>
                <BarChartOutlined style={{ color: '#1890ff', marginRight: 8 }} />
                集团版数据报表
              </Title>
              <Text type="secondary">
                多电站数据对比分析
                {workspace && <Tag color="blue" style={{ marginLeft: 8 }}>{workspace.currentStation?.stationName || '全部电站'}</Tag>}
              </Text>
            </Space>
          </Col>
          <Col xs={24} md={18}>
            <Row gutter={[12, 12]} align="middle" justify="end">
              <Col flex="none">
                <Text type="secondary">时间范围：</Text>
              </Col>
              <Col flex="200px">
                <RangePicker
                  value={dateRange}
                  onChange={setDateRange}
                  style={{ width: '100%' }}
                />
              </Col>
              <Col flex="none">
                <Text type="secondary">选择电站：</Text>
              </Col>
              <Col flex="auto" style={{ minWidth: 200 }}>
                <Select
                  mode="multiple"
                  placeholder="请选择要对比的电站"
                  value={selectedStations}
                  onChange={setSelectedStations}
                  optionFilterProp="children"
                  showSearch
                  style={{ width: '100%' }}
                  maxTagCount={3}
                  maxTagPlaceholder={(omittedValues) => `+${omittedValues.length} 个电站`}
                >
                  {allStations.map(station => (
                    <Option key={station.id} value={station.id}>
                      <Space>
                        <Tag color={station.orgId ? 'blue' : 'default'}>
                          {station.orgName || '未分组'}
                        </Tag>
                        <span>{station.stationName}</span>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          {station.capacity}kW
                        </Text>
                      </Space>
                    </Option>
                  ))}
                </Select>
              </Col>
              <Col flex="none">
                <Space>
                  <Button size="small" onClick={handleSelectAllStations}>
                    全选
                  </Button>
                  <Button size="small" onClick={handleClearSelection}>
                    清空
                  </Button>
                </Space>
              </Col>
              <Col flex="none">
                <Space>
                  <Button
                    icon={<ReloadOutlined />}
                    onClick={loadReportData}
                    disabled={selectedStations.length === 0}
                  >
                    刷新
                  </Button>
                  <Button
                    type="primary"
                    icon={<DownloadOutlined />}
                    onClick={handleExport}
                    disabled={selectedStations.length === 0}
                  >
                    导出报表
                  </Button>
                </Space>
              </Col>
            </Row>
          </Col>
        </Row>
      </Card>

      {selectedStations.length === 0 ? (
        <Card>
          <Empty
            description={<span>请选择要分析的电站，支持多电站数据对比</span>}
          />
        </Card>
      ) : (
        <>
          <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
            {KPI_METRICS.map((metric, index) => (
              <Col xs={24} sm={12} md={8} lg={4} key={metric.key}>
                <StatCard
                  title={metric.label}
                  value={kpiSummary[metric.key] || 0}
                  suffix={metric.unit}
                  icon={metric.icon}
                  color={metric.color}
                  loading={loading}
                />
              </Col>
            ))}
          </Row>

          <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
            <Col xs={24} lg={16}>
              <Card
                title="📈 发电量趋势对比"
                loading={loading}
                extra={
                  <Space>
                    <Button.Group size="small">
                      <Button
                        type={chartType === 'bar' ? 'primary' : 'default'}
                        icon={<BarChartOutlined />}
                        onClick={() => setChartType('bar')}
                      >
                        柱状图
                      </Button>
                      <Button
                        type={chartType === 'line' ? 'primary' : 'default'}
                        icon={<LineChartOutlined />}
                        onClick={() => setChartType('line')}
                      >
                        折线图
                      </Button>
                    </Button.Group>
                    <Tooltip title="最多展示6个电站的趋势数据">
                      <InfoCircleOutlined style={{ color: '#999' }} />
                    </Tooltip>
                  </Space>
                }
              >
                <ChartCard
                  option={getGenerationTrendOption()}
                  height={350}
                  loading={loading}
                />
              </Card>
            </Col>
            <Col xs={24} lg={8}>
              <Card
                title="🥧 发电量占比分布"
                loading={loading}
                extra={<PieChartOutlined style={{ color: '#999' }} />}
              >
                <ChartCard
                  option={getPieOption()}
                  height={350}
                  loading={loading}
                />
              </Card>
            </Col>
          </Row>

          <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
            <Col xs={24} lg={24}>
              <Card
                title="📊 效率指标对比"
                loading={loading}
              >
                <ChartCard
                  option={getEfficiencyCompareOption()}
                  height={300}
                  loading={loading}
                />
              </Card>
            </Col>
          </Row>

          <Card
            title={
              <Space>
                <BarChartOutlined style={{ color: '#1890ff' }} />
                <span>电站详细数据对比</span>
                <Tag color="blue">{reportData.length} 个电站</Tag>
              </Space>
            }
            extra={
              <Text type="secondary">
                统计周期：{dateRange[0]?.format('YYYY-MM-DD')} 至 {dateRange[1]?.format('YYYY-MM-DD')}
              </Text>
            }
          >
            <ProTable
              rowKey="id"
              actionRef={actionRef}
              columns={columns}
              dataSource={reportData}
              loading={loading}
              search={false}
              options={{
                density: true,
                fullScreen: true,
                setting: true
              }}
              pagination={{
                pageSize: 10,
                showSizeChanger: true,
                showQuickJumper: true,
                showTotal: (total) => `共 ${total} 条记录`
              }}
              scroll={{ x: 1800 }}
            />
          </Card>
        </>
      )}
    </div>
  )
}

export default GroupReport
