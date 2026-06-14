import React, { useState, useEffect, useCallback } from 'react'
import {
  Row,
  Col,
  Card,
  Select,
  Space,
  Spin,
  Button,
  Statistic,
  Alert,
  Table,
  Tag,
  Modal,
  Form,
  Input,
  Radio,
  message,
  Tooltip,
  Divider
} from 'antd'
import {
  ThunderboltOutlined,
  WarningOutlined,
  ReloadOutlined,
  BulbOutlined,
  CloudOutlined,
  EnvironmentOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import dayjs from 'dayjs'
import {
  getPredictionCurve,
  getPredictionSummary,
  getWeatherOverview,
  queryAlerts,
  handleAlert,
  executePrediction,
  triggerModelTraining,
  countPendingAlerts
} from '../../api/prediction'
import { getStationListAll } from '../../api/station'

const { Option } = Select
const { TextArea } = Input

const HOUR_OPTIONS = [
  { label: '1小时', value: 1 },
  { label: '3小时', value: 3 },
  { label: '6小时', value: 6 },
  { label: '12小时', value: 12 }
]

const ROOT_CAUSE_OPTIONS = [
  { label: '天气原因', value: 'weather' },
  { label: '设备故障', value: 'equipment' },
  { label: '其他', value: 'other' }
]

const PowerPrediction = () => {
  const [selectedStation, setSelectedStation] = useState(null)
  const [stationList, setStationList] = useState([])
  const [hours, setHours] = useState(6)
  const [loading, setLoading] = useState(false)
  const [curveData, setCurveData] = useState(null)
  const [summaryData, setSummaryData] = useState(null)
  const [weatherData, setWeatherData] = useState(null)
  const [alertList, setAlertList] = useState([])
  const [alertLoading, setAlertLoading] = useState(false)
  const [pendingAlertCount, setPendingAlertCount] = useState(0)
  const [alertModalVisible, setAlertModalVisible] = useState(false)
  const [currentAlert, setCurrentAlert] = useState(null)
  const [form] = Form.useForm()

  const fetchStationList = useCallback(async () => {
    try {
      const res = await getStationListAll()
      const list = res.data || []
      setStationList(list)
      if (list.length > 0 && !selectedStation) {
        setSelectedStation(list[0].id)
      }
    } catch (e) {
      console.error('获取电站列表失败', e)
    }
  }, [selectedStation])

  const fetchAllData = useCallback(async () => {
    if (!selectedStation) return
    setLoading(true)
    try {
      const [curveRes, summaryRes, weatherRes] = await Promise.all([
        getPredictionCurve({ stationId: selectedStation, hours }),
        getPredictionSummary(selectedStation),
        getWeatherOverview(selectedStation)
      ])
      setCurveData(curveRes.data)
      setSummaryData(summaryRes.data)
      setWeatherData(weatherRes.data)
    } catch (e) {
      console.error('获取预测数据失败', e)
    } finally {
      setLoading(false)
    }
  }, [selectedStation, hours])

  const fetchAlerts = useCallback(async () => {
    setAlertLoading(true)
    try {
      const params = {}
      if (selectedStation) params.stationId = selectedStation
      params.pageNum = 1
      params.pageSize = 10
      const res = await queryAlerts(params)
      setAlertList(res.data || [])
    } catch (e) {
      console.error('获取告警列表失败', e)
    } finally {
      setAlertLoading(false)
    }
  }, [selectedStation])

  const fetchPendingCount = useCallback(async () => {
    try {
      const res = await countPendingAlerts(selectedStation)
      setPendingAlertCount(res.data || 0)
    } catch (e) {
      console.error('获取未处理告警数失败', e)
    }
  }, [selectedStation])

  useEffect(() => {
    fetchStationList()
  }, [fetchStationList])

  useEffect(() => {
    if (selectedStation) {
      fetchAllData()
      fetchAlerts()
      fetchPendingCount()
    }
  }, [selectedStation, hours, fetchAllData, fetchAlerts, fetchPendingCount])

  const getCurveOption = () => {
    if (!curveData) return {}
    const { timeAxis, predictedPower, actualPower, deviationRate } = curveData

    return {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' }
      },
      legend: {
        data: ['预测功率', '实际功率', '偏差率'],
        top: 0
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
        data: timeAxis,
        axisLabel: { rotate: 30 }
      },
      yAxis: [
        {
          type: 'value',
          name: '功率(kW)',
          position: 'left',
          axisLine: { lineStyle: { color: '#1890ff' } },
          splitLine: { show: true }
        },
        {
          type: 'value',
          name: '偏差率(%)',
          position: 'right',
          min: -50,
          max: 50,
          axisLine: { lineStyle: { color: '#ff4d4f' } },
          splitLine: { show: false },
          axisLabel: { formatter: '{value}%' }
        }
      ],
      series: [
        {
          name: '预测功率',
          type: 'line',
          smooth: true,
          data: predictedPower,
          itemStyle: { color: '#1890ff' },
          lineStyle: { width: 3 },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
                { offset: 1, color: 'rgba(24, 144, 255, 0.05)' }
              ]
            }
          }
        },
        {
          name: '实际功率',
          type: 'line',
          smooth: true,
          data: actualPower,
          itemStyle: { color: '#52c41a' },
          lineStyle: { width: 3, type: 'solid' },
          symbol: 'circle',
          symbolSize: 6
        },
        {
          name: '偏差率',
          type: 'bar',
          yAxisIndex: 1,
          data: deviationRate,
          itemStyle: {
            color: (params) => {
              const val = Math.abs(params.data)
              if (val >= 20) return '#ff4d4f'
              if (val >= 10) return '#faad14'
              return 'rgba(0,0,0,0.1)'
            }
          },
          barWidth: '40%'
        }
      ],
      markLine: {
        silent: true,
        yAxis: 1,
        data: [
          { yAxis: 20, lineStyle: { color: '#ff4d4f', type: 'dashed' }, label: { formatter: '阈值+20%' } },
          { yAxis: -20, lineStyle: { color: '#ff4d4f', type: 'dashed' }, label: { formatter: '阈值-20%' } }
        ]
      }
    }
  }

  const getWeatherChartOption = () => {
    if (!weatherData?.hourlyForecast?.length) return {}
    const forecast = weatherData.hourlyForecast
    const xData = forecast.map(f => dayjs(f.forecastTime).format('HH:mm'))
    const tempData = forecast.map(f => f.temperature)
    const irrData = forecast.map(f => f.irradiance)
    const cloudData = forecast.map(f => f.cloudCover)

    return {
      tooltip: {
        trigger: 'axis'
      },
      legend: {
        data: ['温度', '辐照度', '云量'],
        top: 0
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: xData
      },
      yAxis: [
        {
          type: 'value',
          name: '温度(℃)/云量(%)',
          position: 'left'
        },
        {
          type: 'value',
          name: '辐照度(W/m²)',
          position: 'right'
        }
      ],
      series: [
        {
          name: '温度',
          type: 'line',
          smooth: true,
          data: tempData,
          itemStyle: { color: '#fa541c' }
        },
        {
          name: '云量',
          type: 'line',
          smooth: true,
          data: cloudData,
          itemStyle: { color: '#8c8c8c' },
          lineStyle: { type: 'dashed' }
        },
        {
          name: '辐照度',
          type: 'bar',
          yAxisIndex: 1,
          data: irrData,
          itemStyle: { color: '#faad14' },
          barWidth: '30%'
        }
      ]
    }
  }

  const getAlertLevelTag = (level) => {
    const map = {
      1: { color: 'blue', text: '低' },
      2: { color: 'orange', text: '中' },
      3: { color: 'red', text: '高' },
      4: { color: 'magenta', text: '紧急' }
    }
    const cfg = map[level] || { color: 'default', text: '未知' }
    return <Tag color={cfg.color}>{cfg.text}</Tag>
  }

  const getAlertStatusTag = (status) => {
    const map = {
      0: { color: 'red', text: '未处理' },
      1: { color: 'green', text: '已处理' },
      2: { color: 'default', text: '已忽略' }
    }
    const cfg = map[status] || { color: 'default', text: '未知' }
    return <Tag color={cfg.color}>{cfg.text}</Tag>
  }

  const getRootCauseTag = (cause) => {
    const map = {
      weather: { color: 'blue', text: '天气原因', icon: <CloudOutlined /> },
      equipment: { color: 'red', text: '设备故障', icon: <ThunderboltOutlined /> },
      other: { color: 'default', text: '待排查', icon: <BulbOutlined /> }
    }
    const cfg = map[cause] || { color: 'default', text: '未知', icon: null }
    return <Tag color={cfg.color} icon={cfg.icon}>{cfg.text}</Tag>
  }

  const handleStationChange = (value) => {
    setSelectedStation(value)
  }

  const handleHoursChange = (value) => {
    setHours(value)
  }

  const handleExecutePrediction = async () => {
    try {
      setLoading(true)
      message.loading({ content: '正在执行预测...', key: 'predict' })
      await executePrediction({ stationId: selectedStation, horizon: hours })
      message.success({ content: '预测执行成功', key: 'predict' })
      await fetchAllData()
    } catch (e) {
      message.error({ content: '预测执行失败', key: 'predict' })
    } finally {
      setLoading(false)
    }
  }

  const handleTrainModel = async () => {
    try {
      message.loading({ content: '正在训练模型...', key: 'train', duration: 0 })
      await triggerModelTraining({ stationId: selectedStation })
      message.success({ content: '模型训练成功', key: 'train' })
    } catch (e) {
      message.error({ content: '模型训练失败', key: 'train' })
    }
  }

  const openHandleAlertModal = (record) => {
    setCurrentAlert(record)
    form.setFieldsValue({
      status: 1,
      remark: '',
      rootCause: record.rootCause
    })
    setAlertModalVisible(true)
  }

  const handleAlertSubmit = async () => {
    try {
      const values = await form.validateFields()
      await handleAlert(currentAlert.id, values)
      message.success('告警处理成功')
      setAlertModalVisible(false)
      fetchAlerts()
      fetchPendingCount()
    } catch (e) {
      console.error(e)
    }
  }

  const alertColumns = [
    {
      title: '告警时间',
      dataIndex: 'alertTime',
      key: 'alertTime',
      width: 160,
      render: (t) => t ? dayjs(t).format('MM-DD HH:mm:ss') : '-'
    },
    {
      title: '目标时间',
      dataIndex: 'targetTime',
      key: 'targetTime',
      width: 140,
      render: (t) => t ? dayjs(t).format('MM-DD HH:mm') : '-'
    },
    {
      title: '电站',
      dataIndex: 'stationName',
      key: 'stationName',
      width: 120
    },
    {
      title: '告警级别',
      dataIndex: 'alertLevel',
      key: 'alertLevel',
      width: 80,
      render: (v) => getAlertLevelTag(v)
    },
    {
      title: '预测值(kW)',
      dataIndex: 'predictedValue',
      key: 'predictedValue',
      width: 100,
      render: (v) => v?.toFixed?.(2) || v
    },
    {
      title: '实际值(kW)',
      dataIndex: 'actualValue',
      key: 'actualValue',
      width: 100,
      render: (v) => v?.toFixed?.(2) || v
    },
    {
      title: '偏差率',
      dataIndex: 'deviationRate',
      key: 'deviationRate',
      width: 90,
      render: (v) => {
        const val = Number(v) * 100
        const color = val >= 35 ? '#ff4d4f' : val >= 20 ? '#faad14' : '#52c41a'
        return <span style={{ color, fontWeight: 'bold' }}>{val.toFixed(2)}%</span>
      }
    },
    {
      title: '根因分析',
      dataIndex: 'rootCause',
      key: 'rootCause',
      width: 100,
      render: (v) => getRootCauseTag(v)
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (v) => getAlertStatusTag(v)
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_, record) => (
        record.status === 0 ? (
          <Button type="link" size="small" onClick={() => openHandleAlertModal(record)}>
            处理
          </Button>
        ) : <span style={{ color: '#999' }}>-</span>
      )
    }
  ]

  return (
    <div className="power-prediction-page">
      <Card
        title={
          <Space>
            <ThunderboltOutlined style={{ color: '#1890ff' }} />
            功率预测与偏差分析
            {pendingAlertCount > 0 && (
              <Tooltip title={`${pendingAlertCount}条未处理告警`}>
                <Tag color="red" icon={<WarningOutlined />}>
                  {pendingAlertCount}
                </Tag>
              </Tooltip>
            )}
          </Space>
        }
        extra={
          <Space>
            <Select
              value={selectedStation}
              onChange={handleStationChange}
              style={{ width: 180 }}
              placeholder="选择电站"
            >
              {stationList.map(s => (
                <Option key={s.id} value={s.id}>
                  <EnvironmentOutlined /> {s.stationName}
                </Option>
              ))}
            </Select>
            <Select
              value={hours}
              onChange={handleHoursChange}
              style={{ width: 100 }}
            >
              {HOUR_OPTIONS.map(opt => (
                <Option key={opt.value} value={opt.value}>{opt.label}</Option>
              ))}
            </Select>
            <Button
              icon={<ReloadOutlined />}
              onClick={fetchAllData}
            >
              刷新
            </Button>
            <Button
              type="primary"
              onClick={handleExecutePrediction}
              loading={loading}
            >
              执行预测
            </Button>
            <Button
              onClick={handleTrainModel}
            >
              训练模型
            </Button>
          </Space>
        }
      >
        <Spin spinning={loading}>
          {summaryData && (
            <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
              <Col xs={12} sm={6}>
                <Card size="small">
                  <Statistic
                    title="今日预测发电量"
                    value={summaryData.todayPredictedEnergy}
                    suffix="kWh"
                    precision={2}
                    valueStyle={{ color: '#1890ff' }}
                  />
                </Card>
              </Col>
              <Col xs={12} sm={6}>
                <Card size="small">
                  <Statistic
                    title="今日实际发电量"
                    value={summaryData.todayActualEnergy}
                    suffix="kWh"
                    precision={2}
                    valueStyle={{ color: '#52c41a' }}
                  />
                </Card>
              </Col>
              <Col xs={12} sm={6}>
                <Card size="small">
                  <Statistic
                    title="平均预测准确率"
                    value={summaryData.avgAccuracy}
                    suffix="%"
                    precision={2}
                    valueStyle={{ color: summaryData.avgAccuracy >= 85 ? '#52c41a' : '#faad14' }}
                  />
                </Card>
              </Col>
              <Col xs={12} sm={6}>
                <Card size="small">
                  <Statistic
                    title="告警数量 / 未处理"
                    value={summaryData.alertCount}
                    suffix={` / ${summaryData.pendingAlertCount}`}
                    valueStyle={{ color: summaryData.pendingAlertCount > 0 ? '#ff4d4f' : '#52c41a' }}
                  />
                </Card>
              </Col>
            </Row>
          )}

          <Row gutter={[16, 16]}>
            <Col xs={24} lg={16}>
              <Card
                title="功率预测曲线（预测 vs 实际）"
                size="small"
                extra={
                  <Space size="small">
                    <Tag color="blue">预测</Tag>
                    <Tag color="green">实际</Tag>
                    <Tag color="red">偏差超20%</Tag>
                  </Space>
                }
              >
                <ReactECharts
                  option={getCurveOption()}
                  style={{ height: 380 }}
                  notMerge
                />
              </Card>
            </Col>
            <Col xs={24} lg={8}>
              <Card
                title={
                  <Space>
                    <CloudOutlined />
                    气象预报
                  </Space>
                }
                size="small"
              >
                {weatherData && (
                  <>
                    <Row gutter={[8, 8]} style={{ marginBottom: 12 }}>
                      <Col span={12}>
                        <div style={{ fontSize: 12, color: '#999' }}>天气</div>
                        <div style={{ fontSize: 18, fontWeight: 'bold' }}>
                          {weatherData.weather || '-'}
                        </div>
                      </Col>
                      <Col span={12}>
                        <div style={{ fontSize: 12, color: '#999' }}>温度</div>
                        <div style={{ fontSize: 18, fontWeight: 'bold', color: '#fa541c' }}>
                          {weatherData.temperature ?? '-'}℃
                        </div>
                      </Col>
                      <Col span={12}>
                        <div style={{ fontSize: 12, color: '#999' }}>湿度</div>
                        <div style={{ fontSize: 16 }}>
                          {weatherData.humidity ?? '-'}%
                        </div>
                      </Col>
                      <Col span={12}>
                        <div style={{ fontSize: 12, color: '#999' }}>辐照度</div>
                        <div style={{ fontSize: 16, color: '#faad14' }}>
                          {weatherData.irradiance ?? '-'} W/m²
                        </div>
                      </Col>
                    </Row>
                    <Divider style={{ margin: '8px 0' }} />
                    <ReactECharts
                      option={getWeatherChartOption()}
                      style={{ height: 200 }}
                      notMerge
                    />
                  </>
                )}
              </Card>
            </Col>
          </Row>

          {curveData?.alertCount > 0 && (
            <Row style={{ marginTop: 16 }}>
              <Col span={24}>
                <Alert
                  type="warning"
                  showIcon
                  icon={<WarningOutlined />}
                  message={`当前时段存在 ${curveData.alertCount} 个预测偏差超过20%的数据点，最大偏差 ${curveData.maxDeviation?.toFixed?.(2) || 0}%`}
                  description="请检查下方告警列表，分析是天气原因还是设备故障导致。"
                />
              </Col>
            </Row>
          )}

          <Row style={{ marginTop: 16 }}>
            <Col span={24}>
              <Card
                title={
                  <Space>
                    <WarningOutlined style={{ color: '#ff4d4f' }} />
                    预测偏差告警列表
                  </Space>
                }
                size="small"
                extra={
                  <Button size="small" onClick={fetchAlerts}>刷新</Button>
                }
              >
                <Table
                  columns={alertColumns}
                  dataSource={alertList}
                  rowKey="id"
                  size="small"
                  loading={alertLoading}
                  pagination={{
                    pageSize: 5,
                    showSizeChanger: false,
                    showTotal: (total) => `共 ${total} 条`
                  }}
                  scroll={{ x: 1100 }}
                />
              </Card>
            </Col>
          </Row>
        </Spin>
      </Card>

      <Modal
        title="处理告警"
        open={alertModalVisible}
        onOk={handleAlertSubmit}
        onCancel={() => setAlertModalVisible(false)}
        destroyOnClose
      >
        {currentAlert && (
          <div>
            <Alert
              style={{ marginBottom: 16 }}
              type={currentAlert.alertLevel >= 3 ? 'error' : 'warning'}
              showIcon
              message={currentAlert.alertContent}
              description={
                <div>
                  <div>预测功率: {currentAlert.predictedValue?.toFixed?.(2) || 0} kW</div>
                  <div>实际功率: {currentAlert.actualValue?.toFixed?.(2) || 0} kW</div>
                  <div>偏差率: {(Number(currentAlert.deviationRate) * 100).toFixed(2)}%</div>
                </div>
              }
            />
            <Form form={form} layout="vertical">
              <Form.Item
                name="status"
                label="处理状态"
                rules={[{ required: true, message: '请选择处理状态' }]}
              >
                <Radio.Group>
                  <Radio value={1}>已处理</Radio>
                  <Radio value={2}>已忽略</Radio>
                </Radio.Group>
              </Form.Item>
              <Form.Item
                name="rootCause"
                label="根因确认"
                rules={[{ required: true, message: '请选择根因' }]}
              >
                <Radio.Group>
                  {ROOT_CAUSE_OPTIONS.map(opt => (
                    <Radio key={opt.value} value={opt.value}>{opt.label}</Radio>
                  ))}
                </Radio.Group>
              </Form.Item>
              <Form.Item
                name="remark"
                label="处理备注"
              >
                <TextArea rows={3} placeholder="请填写处理说明..." />
              </Form.Item>
            </Form>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default PowerPrediction
