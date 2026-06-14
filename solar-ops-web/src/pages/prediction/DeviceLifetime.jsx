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
  message,
  Tooltip,
  Divider,
  Progress,
  List,
  Avatar
} from 'antd'
import {
  ThunderboltOutlined,
  WarningOutlined,
  ReloadOutlined,
  BulbOutlined,
  EnvironmentOutlined,
  HeartOutlined,
  ClockCircleOutlined,
  ToolOutlined,
  SafetyOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import dayjs from 'dayjs'
import {
  getLifetimePrediction,
  getSparePartAdvice,
  queryLifetimeAlerts,
  handleLifetimeAlert,
  triggerLifetimeTraining,
  countLifetimePendingAlerts,
  getHealthHistory
} from '../../api/prediction'
import { getStationListAll } from '../../api/station'
import { getInverterList } from '../../api/inverter'

const { Option } = Select
const { TextArea } = Input

const FORECAST_OPTIONS = [
  { label: '30天', value: 30 },
  { label: '90天', value: 90 },
  { label: '180天', value: 180 },
  { label: '365天', value: 365 }
]

const DeviceLifetime = () => {
  const [selectedStation, setSelectedStation] = useState(null)
  const [selectedInverter, setSelectedInverter] = useState(null)
  const [stationList, setStationList] = useState([])
  const [inverterList, setInverterList] = useState([])
  const [forecastDays, setForecastDays] = useState(90)
  const [loading, setLoading] = useState(false)
  const [predictionData, setPredictionData] = useState(null)
  const [sparePartData, setSparePartData] = useState(null)
  const [alertList, setAlertList] = useState([])
  const [alertLoading, setAlertLoading] = useState(false)
  const [pendingAlertCount, setPendingAlertCount] = useState(0)
  const [alertModalVisible, setAlertModalVisible] = useState(false)
  const [currentAlert, setCurrentAlert] = useState(null)
  const [form] = Form.useForm()
  const [errorMessage, setErrorMessage] = useState(null)
  const [sparePartError, setSparePartError] = useState(null)
  const [hasPredictionData, setHasPredictionData] = useState(false)

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

  const fetchInverterList = useCallback(async () => {
    if (!selectedStation) return
    try {
      const res = await getInverterList({ stationId: selectedStation, pageNum: 1, pageSize: 100 })
      const list = res.data?.list || []
      setInverterList(list)
      if (list.length > 0 && !selectedInverter) {
        setSelectedInverter(list[0].id)
      }
    } catch (e) {
      console.error('获取逆变器列表失败', e)
    }
  }, [selectedStation, selectedInverter])

  const fetchPredictionData = useCallback(async () => {
    if (!selectedStation || !selectedInverter) return
    setLoading(true)
    setErrorMessage(null)
    setHasPredictionData(false)
    try {
      const res = await getLifetimePrediction({
        stationId: selectedStation,
        inverterId: selectedInverter,
        forecastDays
      })
      if (res.data) {
        setPredictionData(res.data)
        setHasPredictionData(true)
      } else {
        setPredictionData(null)
        setErrorMessage('暂无寿命预测数据，请确保已有足够的健康度历史数据（至少30天）并完成模型训练')
      }
    } catch (e) {
      console.error('获取寿命预测数据失败', e)
      setPredictionData(null)
      setHasPredictionData(false)
      const msg = e.response?.data?.message || e.message || '获取寿命预测数据失败'
      setErrorMessage(msg)
    } finally {
      setLoading(false)
    }
  }, [selectedStation, selectedInverter, forecastDays])

  const fetchSparePartAdvice = useCallback(async () => {
    if (!selectedStation || !selectedInverter) return
    setSparePartError(null)
    try {
      const res = await getSparePartAdvice({
        stationId: selectedStation,
        inverterId: selectedInverter
      })
      if (res.data) {
        setSparePartData(res.data)
      } else {
        setSparePartData(null)
        setSparePartError('暂无有效的备件建议数据')
      }
    } catch (e) {
      console.error('获取备件建议失败', e)
      setSparePartData(null)
      const msg = e.response?.data?.message || e.message || '获取备件建议失败'
      setSparePartError(msg)
    }
  }, [selectedStation, selectedInverter])

  const fetchAlerts = useCallback(async () => {
    setAlertLoading(true)
    try {
      const params = {
        pageNum: 1,
        pageSize: 10
      }
      if (selectedStation) params.stationId = selectedStation
      if (selectedInverter) params.inverterId = selectedInverter
      const res = await queryLifetimeAlerts(params)
      setAlertList(res.data?.list || [])
    } catch (e) {
      console.error('获取预警列表失败', e)
    } finally {
      setAlertLoading(false)
    }
  }, [selectedStation, selectedInverter])

  const fetchPendingCount = useCallback(async () => {
    try {
      const res = await countLifetimePendingAlerts(selectedStation)
      setPendingAlertCount(res.data || 0)
    } catch (e) {
      console.error('获取未处理预警数失败', e)
    }
  }, [selectedStation])

  useEffect(() => {
    fetchStationList()
  }, [fetchStationList])

  useEffect(() => {
    if (selectedStation) {
      fetchInverterList()
    }
  }, [selectedStation, fetchInverterList])

  useEffect(() => {
    if (selectedInverter) {
      fetchPredictionData()
      fetchSparePartAdvice()
      fetchAlerts()
      fetchPendingCount()
    }
  }, [selectedInverter, forecastDays, fetchPredictionData, fetchSparePartAdvice, fetchAlerts, fetchPendingCount])

  const getHealthChartOption = () => {
    if (!predictionData || !predictionData.healthTrend || predictionData.healthTrend.length === 0) {
      return {
        title: {
          text: '暂无健康度趋势数据',
          subtext: '请确保已有足够的历史数据并完成模型训练',
          left: 'center',
          top: 'center',
          textStyle: { color: '#999', fontSize: 16 },
          subtextStyle: { color: '#bbb', fontSize: 12 }
        }
      }
    }
    const { timeAxis, healthTrend, confidenceTrend } = predictionData

    return {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' }
      },
      legend: {
        data: ['健康度趋势', '置信度'],
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
          name: '健康度(0-1)',
          position: 'left',
          min: 0,
          max: 1,
          axisLine: { lineStyle: { color: '#52c41a' } },
          splitLine: { show: true }
        },
        {
          type: 'value',
          name: '置信度(0-1)',
          position: 'right',
          min: 0,
          max: 1,
          axisLine: { lineStyle: { color: '#1890ff' } },
          splitLine: { show: false }
        }
      ],
      series: [
        {
          name: '健康度趋势',
          type: 'line',
          smooth: true,
          data: healthTrend,
          itemStyle: { color: '#52c41a' },
          lineStyle: { width: 3 },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(82, 196, 26, 0.3)' },
                { offset: 1, color: 'rgba(82, 196, 26, 0.05)' }
              ]
            }
          },
          markLine: {
            silent: true,
            data: [
              { yAxis: 0.7, lineStyle: { color: '#faad14', type: 'dashed' }, label: { formatter: '注意阈值' } },
              { yAxis: 0.3, lineStyle: { color: '#ff4d4f', type: 'dashed' }, label: { formatter: '危险阈值' } }
            ]
          }
        },
        {
          name: '置信度',
          type: 'line',
          yAxisIndex: 1,
          smooth: true,
          data: confidenceTrend,
          itemStyle: { color: '#1890ff' },
          lineStyle: { width: 2, type: 'dashed' }
        }
      ]
    }
  }

  const getAlertLevelTag = (level) => {
    const map = {
      1: { color: 'blue', text: '低' },
      2: { color: 'orange', text: '中' },
      3: { color: 'orange', text: '高' },
      4: { color: 'red', text: '紧急' }
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

  const getAlertTypeTag = (type) => {
    const map = {
      1: { color: 'red', text: '寿命预警', icon: <HeartOutlined /> },
      2: { color: 'orange', text: '备件更换', icon: <ToolOutlined /> }
    }
    const cfg = map[type] || { color: 'default', text: '未知', icon: null }
    return <Tag color={cfg.color} icon={cfg.icon}>{cfg.text}</Tag>
  }

  const handleStationChange = (value) => {
    setSelectedStation(value)
    setSelectedInverter(null)
  }

  const handleInverterChange = (value) => {
    setSelectedInverter(value)
  }

  const handleForecastChange = (value) => {
    setForecastDays(value)
  }

  const handleRefresh = () => {
    fetchPredictionData()
    fetchSparePartAdvice()
    fetchAlerts()
    fetchPendingCount()
  }

  const handleTrainModel = async () => {
    try {
      message.loading({ content: '正在训练模型...', key: 'train', duration: 0 })
      await triggerLifetimeTraining({ stationId: selectedStation, inverterId: selectedInverter })
      message.success({ content: '模型训练成功', key: 'train' })
      fetchPredictionData()
    } catch (e) {
      message.error({ content: '模型训练失败', key: 'train' })
    }
  }

  const openHandleAlertModal = (record) => {
    setCurrentAlert(record)
    form.setFieldsValue({
      status: 1,
      handleRemark: ''
    })
    setAlertModalVisible(true)
  }

  const handleAlertSubmit = async () => {
    try {
      const values = await form.validateFields()
      await handleLifetimeAlert(currentAlert.id, values)
      message.success('预警处理成功')
      setAlertModalVisible(false)
      fetchAlerts()
      fetchPendingCount()
    } catch (e) {
      console.error(e)
    }
  }

  const getHealthLevelColor = (score) => {
    if (score >= 0.7) return '#52c41a'
    if (score >= 0.5) return '#faad14'
    if (score >= 0.3) return '#fa8c16'
    return '#ff4d4f'
  }

  const alertColumns = [
    {
      title: '预警时间',
      dataIndex: 'alertTime',
      key: 'alertTime',
      width: 160,
      render: (t) => t ? dayjs(t).format('MM-DD HH:mm:ss') : '-'
    },
    {
      title: '逆变器',
      dataIndex: 'inverterName',
      key: 'inverterName',
      width: 120
    },
    {
      title: '预警类型',
      dataIndex: 'alertType',
      key: 'alertType',
      width: 100,
      render: (v) => getAlertTypeTag(v)
    },
    {
      title: '预警级别',
      dataIndex: 'alertLevel',
      key: 'alertLevel',
      width: 80,
      render: (v) => getAlertLevelTag(v)
    },
    {
      title: '预警标题',
      dataIndex: 'alertTitle',
      key: 'alertTitle',
      width: 150
    },
    {
      title: '剩余寿命(天)',
      dataIndex: 'remainingLifeDays',
      key: 'remainingLifeDays',
      width: 100,
      render: (v) => v || '-'
    },
    {
      title: '建议备件',
      dataIndex: 'sparePart',
      key: 'sparePart',
      width: 120,
      render: (v) => v || '-'
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
    <div className="device-lifetime-page">
      <Card
        title={
          <Space>
            <HeartOutlined style={{ color: '#ff4d4f' }} />
            设备生命周期预测
            {pendingAlertCount > 0 && (
              <Tooltip title={`${pendingAlertCount}条未处理预警`}>
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
              value={selectedInverter}
              onChange={handleInverterChange}
              style={{ width: 180 }}
              placeholder="选择逆变器"
            >
              {inverterList.map(i => (
                <Option key={i.id} value={i.id}>
                  <ThunderboltOutlined /> {i.deviceName}
                </Option>
              ))}
            </Select>
            <Select
              value={forecastDays}
              onChange={handleForecastChange}
              style={{ width: 100 }}
            >
              {FORECAST_OPTIONS.map(opt => (
                <Option key={opt.value} value={opt.value}>{opt.label}</Option>
              ))}
            </Select>
            <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
              刷新
            </Button>
            <Button type="primary" onClick={handleTrainModel}>
              训练模型
            </Button>
          </Space>
        }
      >
        <Spin spinning={loading}>
          {errorMessage && (
            <Alert
              type="warning"
              showIcon
              icon={<BulbOutlined />}
              message="暂无预测数据"
              description={errorMessage}
              style={{ marginBottom: 16 }}
              action={
                <Button size="small" type="primary" onClick={handleTrainModel}>
                  立即训练模型
                </Button>
              }
            />
          )}
          {hasPredictionData && predictionData && (
            <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
              <Col xs={24} sm={12} md={6}>
                <Card size="small">
                  <Statistic
                    title="当前健康度"
                    value={(predictionData.currentHealthScore * 100).toFixed(1)}
                    suffix="%"
                    valueStyle={{ color: getHealthLevelColor(predictionData.currentHealthScore) }}
                    prefix={<HeartOutlined />}
                  />
                  <div style={{ marginTop: 8 }}>
                    <Progress
                      percent={(predictionData.currentHealthScore * 100).toFixed(0)}
                      strokeColor={getHealthLevelColor(predictionData.currentHealthScore)}
                      showInfo={false}
                      size="small"
                    />
                  </div>
                  <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>
                    {predictionData.healthLevelDesc}
                  </div>
                </Card>
              </Col>
              <Col xs={24} sm={12} md={6}>
                <Card size="small">
                  <Statistic
                    title="剩余寿命"
                    value={predictionData.remainingLifeDays}
                    suffix="天"
                    valueStyle={{ color: predictionData.remainingLifeDays <= 90 ? '#ff4d4f' : '#52c41a' }}
                    prefix={<ClockCircleOutlined />}
                  />
                  <div style={{ fontSize: 12, color: '#999', marginTop: 8 }}>
                    约 {predictionData.remainingLifeDesc}
                  </div>
                </Card>
              </Col>
              <Col xs={24} sm={12} md={6}>
                <Card size="small">
                  <Statistic
                    title="预警级别"
                    value={predictionData.alertLevelDesc}
                    valueStyle={{ color: predictionData.alertLevel >= 3 ? '#ff4d4f' : '#52c41a' }}
                    prefix={<WarningOutlined />}
                  />
                  <div style={{ fontSize: 12, color: '#999', marginTop: 8 }}>
                    模型版本: {predictionData.modelVersion}
                  </div>
                </Card>
              </Col>
              <Col xs={24} sm={12} md={6}>
                <Card size="small">
                  <Statistic
                    title="备件更换建议"
                    value={predictionData.replacementAdvice ? '建议更换' : '正常运行'}
                    valueStyle={{ color: predictionData.replacementAdvice ? '#ff4d4f' : '#52c41a' }}
                    prefix={<ToolOutlined />}
                  />
                  <div style={{ fontSize: 12, color: '#999', marginTop: 8 }}>
                    {predictionData.replacementAdvice ? '请尽快安排更换' : '设备状态良好'}
                  </div>
                </Card>
              </Col>
            </Row>
          )}

          <Row gutter={[16, 16]}>
            <Col xs={24} lg={16}>
              <Card
                title={
                  <Space>
                    <SafetyOutlined style={{ color: '#52c41a' }} />
                    健康度趋势预测
                  </Space>
                }
                size="small"
                extra={
                  <Space size="small">
                    <Tag color="green">健康度</Tag>
                    <Tag color="blue">置信度</Tag>
                    <Tag color="orange">注意阈值</Tag>
                    <Tag color="red">危险阈值</Tag>
                  </Space>
                }
              >
                <ReactECharts
                  option={getHealthChartOption()}
                  style={{ height: 380 }}
                  notMerge
                />
              </Card>
            </Col>
            <Col xs={24} lg={8}>
              <Card
                title={
                  <Space>
                    <ToolOutlined style={{ color: '#faad14' }} />
                    备件更换建议
                  </Space>
                }
                size="small"
              >
                {sparePartError ? (
                  <div style={{ textAlign: 'center', padding: '40px 20px', color: '#999' }}>
                    <BulbOutlined style={{ fontSize: 48, color: '#ddd', marginBottom: 12 }} />
                    <div style={{ fontSize: 14, marginBottom: 8 }}>{sparePartError}</div>
                    <div style={{ fontSize: 12, color: '#bbb' }}>
                      请先完成寿命预测后再查看备件建议
                    </div>
                  </div>
                ) : sparePartData ? (
                  <>
                    {sparePartData.warnings?.length > 0 && (
                      <>
                        <Alert
                          type={sparePartData.warnings[0]?.level === 'critical' ? 'error' : 'warning'}
                          showIcon
                          icon={<WarningOutlined />}
                          message={sparePartData.warnings[0]?.message}
                          style={{ marginBottom: 12 }}
                        />
                        <Divider style={{ margin: '8px 0' }} />
                      </>
                    )}
                    <List
                      size="small"
                      dataSource={sparePartData.suggestions || []}
                      locale={{ emptyText: '暂无建议，设备状态良好' }}
                      renderItem={item => (
                        <List.Item>
                          <List.Item.Meta
                            avatar={<Avatar icon={<ToolOutlined />} style={{ backgroundColor: '#faad14' }} />}
                            title={item.component}
                            description={
                              <div>
                                <div style={{ fontSize: 12, color: '#666' }}>{item.reason}</div>
                                <div style={{ fontSize: 12, color: '#1890ff' }}>{item.recommendation}</div>
                                <div style={{ fontSize: 12, color: '#fa8c16', marginTop: 4 }}>
                                  预估费用: {item.estimatedCost}
                                </div>
                              </div>
                            }
                          />
                        </List.Item>
                      )}
                    />
                  </>
                ) : (
                  <div style={{ textAlign: 'center', padding: '40px 20px', color: '#999' }}>
                    <ToolOutlined style={{ fontSize: 48, color: '#ddd', marginBottom: 12 }} />
                    <div style={{ fontSize: 14 }}>暂无备件建议数据</div>
                  </div>
                )}
              </Card>
            </Col>
          </Row>

          <Row style={{ marginTop: 16 }}>
            <Col span={24}>
              <Card
                title={
                  <Space>
                    <WarningOutlined style={{ color: '#ff4d4f' }} />
                    寿命预警列表
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
        title="处理预警"
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
              message={currentAlert.alertTitle}
              description={
                <div>
                  <div>剩余寿命: {currentAlert.remainingLifeDays || '-'} 天</div>
                  <div>当前健康度: {currentAlert.currentHealth ? (currentAlert.currentHealth * 100).toFixed(1) + '%' : '-'}</div>
                  <div>建议备件: {currentAlert.sparePart || '-'}</div>
                </div>
              }
            />
            <Form form={form} layout="vertical">
              <Form.Item
                name="status"
                label="处理状态"
                rules={[{ required: true, message: '请选择处理状态' }]}
              >
                <Select>
                  <Select.Option value={1}>已处理</Select.Option>
                  <Select.Option value={2}>已忽略</Select.Option>
                </Select>
              </Form.Item>
              <Form.Item
                name="handleRemark"
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

export default DeviceLifetime
