import React, { useState, useEffect, useMemo, useCallback } from 'react'
import {
  Card, Row, Col, Statistic, Table, Tag, Form, Select, Input, InputNumber,
  Button, Modal, Radio, Switch, Space, Divider, List, Badge, Descriptions,
  message, notification, Empty, Spin, Tabs, Tooltip
} from 'antd'
import {
  ThunderboltOutlined, CheckCircleOutlined, CloseCircleOutlined,
  ClockCircleOutlined, SyncOutlined, PlayCircleOutlined, StopOutlined,
  ReloadOutlined, SendOutlined, DatabaseOutlined, ExperimentOutlined,
  WarningOutlined, LineChartOutlined, CopyOutlined, InfoCircleOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import * as echarts from 'echarts'
import {
  getDispatchSummary, getDispatchCommands, getDispatchCommandDetail,
  getDispatchCommandCurve, getDispatchUploadRecords, createManualCommand,
  cancelDispatchCommand, getProtocolConfigs, testProtocolConnection
} from '../../api/gridDispatch'
import {
  getGridDispatchWebSocket,
  closeGridDispatchWebSocket
} from '../../utils/gridDispatchWebsocket'
import { getStationListAll } from '../../api/station'
import { getInverterListByStation } from '../../api/inverter'

const { Option } = Select
const { TextArea } = Input

const SOURCE_MAP = {
  1: { color: 'blue', text: '调度下发' },
  2: { color: 'orange', text: '人工干预' },
  3: { color: 'green', text: '自动策略' }
}

const TYPE_MAP = {
  1: { color: 'cyan', text: '有功功率' },
  2: { color: 'purple', text: '无功功率' },
  3: { color: 'geekblue', text: '电压调节' },
  4: { color: 'magenta', text: '频率调节' },
  5: { color: 'volcano', text: '启停控制' }
}

const PRIORITY_MAP = {
  1: { color: 'red', text: '紧急' },
  2: { color: 'orange', text: '高' },
  3: { color: 'blue', text: '普通' },
  4: { color: 'default', text: '低' }
}

const STATUS_MAP = {
  0: { color: 'default', text: '待执行' },
  1: { color: 'processing', text: '执行中' },
  2: { color: 'success', text: '成功' },
  3: { color: 'error', text: '失败' },
  4: { color: 'default', text: '已取消' },
  5: { color: 'warning', text: '超时' }
}

const UPLOAD_STATUS_MAP = {
  0: { color: 'default', text: '待上传' },
  1: { color: 'success', text: '上传成功' },
  2: { color: 'error', text: '上传失败' }
}

const PROTOCOL_STATUS_MAP = {
  0: { color: '#8c8c8c', text: '离线' },
  1: { color: '#52c41a', text: '在线' },
  2: { color: '#ff4d4f', text: '异常' }
}

const PAGE_SIZE = 10

const formatNumber = (num) => {
  if (num == null || isNaN(num)) return '-'
  return Number(num).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

const getDeviationColor = (deviation) => {
  if (deviation == null || isNaN(deviation)) return '#000'
  const abs = Math.abs(deviation)
  if (abs < 5) return '#52c41a'
  return deviation > 0 ? '#ff4d4f' : '#52c41a'
}

const getSuccessRateColor = (rate) => {
  if (rate == null) return '#000'
  if (rate >= 90) return '#52c41a'
  if (rate >= 80) return '#fa8c16'
  return '#ff4d4f'
}

const GridDispatchDashboard = () => {
  const [summaryLoading, setSummaryLoading] = useState(false)
  const [summary, setSummary] = useState({
    totalCount: 0,
    successCount: 0,
    failCount: 0,
    executingCount: 0,
    pendingCount: 0,
    uploadSuccessCount: 0,
    uploadFailCount: 0,
    successRate: 0
  })

  const [tableLoading, setTableLoading] = useState(false)
  const [tableData, setTableData] = useState([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [selectedRowId, setSelectedRowId] = useState(null)
  const [queryForm] = Form.useForm()
  const [queryParams, setQueryParams] = useState({})

  const [stationList, setStationList] = useState([])
  const [stationLoading, setStationLoading] = useState(false)
  const [inverterList, setInverterList] = useState([])
  const [inverterLoading, setInverterLoading] = useState(false)

  const [curveType, setCurveType] = useState(1)
  const [curveLoading, setCurveLoading] = useState(false)
  const [curveData, setCurveData] = useState(null)

  const [protocolLoading, setProtocolLoading] = useState(false)
  const [protocolList, setProtocolList] = useState([])
  const [testingProtocolId, setTestingProtocolId] = useState(null)

  const [uploadList, setUploadList] = useState([])
  const [uploadLoading, setUploadLoading] = useState(false)

  const [commandForm] = Form.useForm()
  const [commandSubmitting, setCommandSubmitting] = useState(false)
  const [formInverterList, setFormInverterList] = useState([])
  const [formInverterLoading, setFormInverterLoading] = useState(false)
  const [selectedCommandType, setSelectedCommandType] = useState(1)

  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [currentDetail, setCurrentDetail] = useState(null)
  const [detailCurveData, setDetailCurveData] = useState(null)
  const [detailCurveType, setDetailCurveType] = useState(1)

  const fetchSummary = useCallback(async () => {
    setSummaryLoading(true)
    try {
      const res = await getDispatchSummary()
      setSummary(res.data || {})
    } catch (e) {
      console.error('[调度统计] 获取失败:', e)
    } finally {
      setSummaryLoading(false)
    }
  }, [])

  const fetchCommands = useCallback(async (page = 1, extraParams = {}) => {
    setTableLoading(true)
    try {
      const params = { pageNum: page, pageSize: PAGE_SIZE, ...extraParams }
      const res = await getDispatchCommands(params)
      const pageResult = res.data || {}
      setTableData(pageResult.list || [])
      setTotal(pageResult.total || 0)
      setPageNum(pageResult.pageNum || page)
    } catch (e) {
      console.error('[指令列表] 获取失败:', e)
      setTableData([])
      setTotal(0)
    } finally {
      setTableLoading(false)
    }
  }, [])

  const fetchStations = useCallback(async () => {
    setStationLoading(true)
    try {
      const res = await getStationListAll()
      setStationList(res.data || [])
    } catch (e) {
      console.error('[电站列表] 获取失败:', e)
    } finally {
      setStationLoading(false)
    }
  }, [])

  const fetchInverters = useCallback(async (stationId, forForm = false) => {
    if (!stationId) {
      if (forForm) {
        setFormInverterList([])
      } else {
        setInverterList([])
      }
      return
    }
    const setLoading = forForm ? setFormInverterLoading : setInverterLoading
    const setList = forForm ? setFormInverterList : setInverterList
    setLoading(true)
    try {
      const res = await getInverterListByStation(stationId)
      setList(res.data || [])
    } catch (e) {
      console.error('[逆变器列表] 获取失败:', e)
      setList([])
    } finally {
      setLoading(false)
    }
  }, [])

  const fetchCurve = useCallback(async (id, type = 1) => {
    if (!id) return
    setCurveLoading(true)
    try {
      const res = await getDispatchCommandCurve(id)
      setCurveData(res.data || null)
    } catch (e) {
      console.error('[曲线数据] 获取失败:', e)
      setCurveData(null)
    } finally {
      setCurveLoading(false)
    }
  }, [])

  const fetchProtocolConfigs = useCallback(async () => {
    setProtocolLoading(true)
    try {
      const res = await getProtocolConfigs()
      setProtocolList(res.data || [])
    } catch (e) {
      console.error('[协议配置] 获取失败:', e)
      setProtocolList([])
    } finally {
      setProtocolLoading(false)
    }
  }, [])

  const fetchUploadRecords = useCallback(async () => {
    setUploadLoading(true)
    try {
      const res = await getDispatchUploadRecords({ pageNum: 1, pageSize: 10 })
      setUploadList((res.data && res.data.list) || [])
    } catch (e) {
      console.error('[上传记录] 获取失败:', e)
      setUploadList([])
    } finally {
      setUploadLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchSummary()
    fetchCommands(1)
    fetchStations()
    fetchProtocolConfigs()
    fetchUploadRecords()

    const ws = getGridDispatchWebSocket()
    ws.connect()

    const handleCommandUpdate = (data) => {
      fetchSummary()
      fetchCommands(pageNum, queryParams)
      if (data && (data.priority === 1 || data.priority === 2)) {
        const priorityInfo = PRIORITY_MAP[data.priority] || {}
        notification.open({
          message: `调度指令更新 - ${priorityInfo.text || ''}`,
          description: (
            <div>
              <div>指令号: {data.commandNo || '-'}</div>
              <div>电站: {data.stationName || '-'}</div>
              <div>状态: {(STATUS_MAP[data.status] && STATUS_MAP[data.status].text) || '-'}</div>
            </div>
          ),
          icon: <WarningOutlined style={{ color: data.priority === 1 ? '#ff4d4f' : '#fa8c16' }} />,
          duration: 6,
          placement: 'topRight'
        })
      }
    }

    const handleUploadStatus = (data) => {
      setUploadList(prev => {
        const newList = data ? [data, ...prev] : [...prev]
        return newList.slice(0, 10)
      })
    }

    ws.on('command-update', handleCommandUpdate)
    ws.on('upload-status', handleUploadStatus)

    return () => {
      ws.off('command-update', handleCommandUpdate)
      ws.off('upload-status', handleUploadStatus)
      closeGridDispatchWebSocket()
    }
  }, [fetchSummary, fetchCommands, fetchStations, fetchProtocolConfigs, fetchUploadRecords])

  useEffect(() => {
    if (queryParams.stationId) {
      fetchInverters(queryParams.stationId, false)
    }
  }, [queryParams.stationId, fetchInverters])

  const handleRowClick = (record) => {
    setSelectedRowId(record.id)
    fetchCurve(record.id, curveType)
  }

  const handleQuery = async () => {
    try {
      const values = await queryForm.validateFields()
      const params = { ...values }
      if (!params.stationId) delete params.stationId
      if (!params.commandType && params.commandType !== 0) delete params.commandType
      if (!params.status && params.status !== 0) delete params.status
      if (!params.keyword) delete params.keyword
      setQueryParams(params)
      fetchCommands(1, params)
    } catch (e) {
    }
  }

  const handleReset = () => {
    queryForm.resetFields()
    setQueryParams({})
    fetchCommands(1)
  }

  const handlePageChange = (page) => {
    fetchCommands(page, queryParams)
  }

  const handleCancelCommand = async (record) => {
    Modal.confirm({
      title: '确认取消指令',
      content: `确定要取消指令 ${record.commandNo} 吗？`,
      okText: '确认取消',
      cancelText: '返回',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await cancelDispatchCommand(record.id)
          message.success('指令取消成功')
          fetchCommands(pageNum, queryParams)
          fetchSummary()
        } catch (e) {
          message.error(e.message || '取消失败')
        }
      }
    })
  }

  const handleViewDetail = async (record) => {
    setDetailModalVisible(true)
    setDetailLoading(true)
    setCurrentDetail(null)
    setDetailCurveData(null)
    try {
      const [detailRes, curveRes] = await Promise.all([
        getDispatchCommandDetail(record.id),
        getDispatchCommandCurve(record.id)
      ])
      setCurrentDetail(detailRes.data || record)
      setDetailCurveData(curveRes.data || null)
    } catch (e) {
      console.error('[指令详情] 获取失败:', e)
      setCurrentDetail(record)
    } finally {
      setDetailLoading(false)
    }
  }

  const handleCopy = (text) => {
    navigator.clipboard.writeText(text).then(() => {
      message.success('已复制到剪贴板')
    }).catch(() => {
      message.error('复制失败')
    })
  }

  const handleTestProtocol = async (config) => {
    setTestingProtocolId(config.id)
    try {
      const res = await testProtocolConnection(config.id)
      message.success(res.message || '测试连接成功')
      fetchProtocolConfigs()
    } catch (e) {
      message.error(e.message || '测试连接失败')
    } finally {
      setTestingProtocolId(null)
    }
  }

  const handleStationChangeForForm = (stationId) => {
    commandForm.setFieldsValue({ inverterId: undefined })
    fetchInverters(stationId, true)
  }

  const handleCommandTypeChange = (e) => {
    setSelectedCommandType(e.target.value)
  }

  const handleFormSubmit = async () => {
    try {
      const values = await commandForm.validateFields()
      setCommandSubmitting(true)
      const submitData = { ...values }
      if (submitData.inverterId === 'all') {
        submitData.inverterId = null
        submitData.stationWide = true
      }
      await createManualCommand(submitData)
      message.success('指令下发成功')
      commandForm.resetFields()
      setSelectedCommandType(1)
      setFormInverterList([])
      fetchCommands(pageNum, queryParams)
      fetchSummary()
    } catch (e) {
      message.error(e.message || '下发失败')
    } finally {
      setCommandSubmitting(false)
    }
  }

  const buildCurveOption = useCallback((data, type, compact = false) => {
    if (!data || !data.curveDataList) {
      return null
    }
    const curveList = data.curveDataList || []
    const times = curveList.map(item => item.time || item.timestamp || '')

    let targetKey = 'targetActivePower'
    let actualKey = 'actualActivePower'
    let yName = '有功功率 (kW)'
    let title = '有功功率曲线对比'

    switch (type) {
      case 1:
        targetKey = 'targetActivePower'
        actualKey = 'actualActivePower'
        yName = '有功功率 (kW)'
        title = '有功功率曲线对比'
        break
      case 2:
        targetKey = 'targetVoltage'
        actualKey = 'actualVoltage'
        yName = '电压 (V)'
        title = '电压曲线对比'
        break
      case 3:
        targetKey = 'targetFrequency'
        actualKey = 'actualFrequency'
        yName = '频率 (Hz)'
        title = '频率曲线对比'
        break
      default:
        break
    }

    const targetData = curveList.map(item => item[targetKey] ?? null)
    const actualData = curveList.map(item => item[actualKey] ?? null)

    return {
      title: compact ? undefined : {
        text: title,
        left: 'center',
        textStyle: { fontSize: 14, fontWeight: 500 }
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' }
      },
      legend: {
        data: ['目标值', '实际值'],
        top: compact ? 0 : 30
      },
      grid: compact
        ? { left: '10%', right: '5%', top: '15%', bottom: '15%' }
        : { left: '3%', right: '4%', top: '18%', bottom: '12%', containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: times,
        axisLabel: {
          rotate: compact ? 0 : 30,
          fontSize: compact ? 10 : 11
        }
      },
      yAxis: {
        type: 'value',
        name: yName,
        nameTextStyle: { fontSize: 11 }
      },
      dataZoom: compact ? undefined : [
        { type: 'inside', start: 0, end: 100 },
        { type: 'slider', start: 0, end: 100, height: 20, bottom: 5 }
      ],
      series: [
        {
          name: '目标值',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: compact ? 4 : 6,
          lineStyle: { type: 'dashed', width: 2, color: '#1890ff' },
          itemStyle: { color: '#1890ff' },
          data: targetData
        },
        {
          name: '实际值',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: compact ? 4 : 6,
          lineStyle: { width: 2, color: '#52c41a' },
          itemStyle: { color: '#52c41a' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(82, 196, 26, 0.25)' },
              { offset: 1, color: 'rgba(82, 196, 26, 0.02)' }
            ])
          },
          data: actualData
        }
      ]
    }
  }, [])

  const columns = [
    {
      title: '指令号',
      dataIndex: 'commandNo',
      key: 'commandNo',
      width: 160,
      fixed: 'left',
      render: (text) => text ? (
        <Space>
          <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{text}</span>
          <Tooltip title="复制">
            <CopyOutlined
              style={{ color: '#1890ff', cursor: 'pointer' }}
              onClick={() => handleCopy(text)}
            />
          </Tooltip>
        </Space>
      ) : '-'
    },
    {
      title: '来源',
      dataIndex: 'source',
      key: 'source',
      width: 100,
      render: (val) => {
        const info = SOURCE_MAP[val] || { color: 'default', text: val }
        return <Tag color={info.color}>{info.text}</Tag>
      }
    },
    {
      title: '类型',
      dataIndex: 'commandType',
      key: 'commandType',
      width: 100,
      render: (val) => {
        const info = TYPE_MAP[val] || { color: 'default', text: val }
        return <Tag color={info.color}>{info.text}</Tag>
      }
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 90,
      render: (val) => {
        const info = PRIORITY_MAP[val] || { color: 'default', text: val }
        return <Tag color={info.color}>{info.text}</Tag>
      }
    },
    {
      title: '电站 / 逆变器',
      key: 'station',
      width: 160,
      render: (_, record) => (
        <Space direction="vertical" size={2}>
          <span>{record.stationName || '-'}</span>
          <span style={{ fontSize: 12, color: '#8c8c8c' }}>
            {record.inverterName || (record.inverterId ? `#${record.inverterId}` : '全站')}
          </span>
        </Space>
      )
    },
    {
      title: '目标功率(kW)',
      dataIndex: 'targetActivePower',
      key: 'targetActivePower',
      width: 120,
      align: 'right',
      render: (val) => formatNumber(val)
    },
    {
      title: '实际功率(kW)',
      dataIndex: 'actualActivePower',
      key: 'actualActivePower',
      width: 120,
      align: 'right',
      render: (val) => formatNumber(val)
    },
    {
      title: '偏差%',
      dataIndex: 'deviationPercent',
      key: 'deviationPercent',
      width: 90,
      align: 'right',
      render: (val) => {
        if (val == null || isNaN(val)) return '-'
        const color = getDeviationColor(val)
        return (
          <span style={{ color, fontWeight: 500 }}>
            {val > 0 ? '+' : ''}{Number(val).toFixed(2)}%
          </span>
        )
      }
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (val) => {
        const info = STATUS_MAP[val] || { color: 'default', text: val }
        return <Tag color={info.color}>{info.text}</Tag>
      }
    },
    {
      title: '下发时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 170,
      render: (text) => text || '-'
    },
    {
      title: '操作',
      key: 'action',
      width: 130,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<InfoCircleOutlined />}
            onClick={() => handleViewDetail(record)}
          >
            详情
          </Button>
          {(record.status === 0 || record.status === 1) && (
            <Button
              type="link"
              size="small"
              danger
              icon={<StopOutlined />}
              onClick={() => handleCancelCommand(record)}
            >
              取消
            </Button>
          )}
        </Space>
      )
    }
  ]

  const statsCards = useMemo(() => [
    {
      title: '总指令数',
      value: summary.totalCount || 0,
      icon: <ThunderboltOutlined />,
      color: '#1890ff'
    },
    {
      title: '成功数',
      value: summary.successCount || 0,
      icon: <CheckCircleOutlined />,
      color: '#52c41a'
    },
    {
      title: '失败数',
      value: summary.failCount || 0,
      icon: <CloseCircleOutlined />,
      color: '#ff4d4f'
    },
    {
      title: '执行中',
      value: summary.executingCount || 0,
      icon: <SyncOutlined spin />,
      color: '#1890ff'
    },
    {
      title: '待执行',
      value: summary.pendingCount || 0,
      icon: <ClockCircleOutlined />,
      color: '#fa8c16'
    },
    {
      title: '上传成功',
      value: summary.uploadSuccessCount || 0,
      icon: <DatabaseOutlined />,
      color: '#52c41a'
    },
    {
      title: '上传失败',
      value: summary.uploadFailCount || 0,
      icon: <WarningOutlined />,
      color: '#ff4d4f'
    },
    {
      title: '成功率',
      value: summary.successRate != null ? `${Number(summary.successRate).toFixed(1)}%` : '0%',
      icon: <PlayCircleOutlined />,
      color: getSuccessRateColor(summary.successRate)
    }
  ], [summary])

  return (
    <div className="grid-dispatch-page">
      <Row gutter={16} style={{ marginBottom: 16 }}>
        {statsCards.map((stat, index) => (
          <Col xs={12} sm={6} lg={3} key={index} style={{ marginBottom: 16 }}>
            <Card size="small" bordered hover>
              <Statistic
                title={stat.title}
                value={stat.value}
                valueStyle={{ color: stat.color, fontSize: 22, fontWeight: 600 }}
                prefix={React.cloneElement(stat.icon, { style: { fontSize: 18 } })}
                loading={summaryLoading}
              />
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={16}>
        <Col xs={24} lg={8} style={{ marginBottom: 16 }}>
          <Card
            title="指令执行记录"
            size="small"
            extra={
              <Button
                size="small"
                icon={<ReloadOutlined />}
                onClick={() => fetchCommands(pageNum, queryParams)}
              >
                刷新
              </Button>
            }
            styles={{ body: { padding: 12 } }}
          >
            <Form form={queryForm} layout="inline" style={{ marginBottom: 12 }}>
              <Form.Item name="commandType" label="类型" style={{ marginBottom: 8 }}>
                <Select placeholder="全部" allowClear style={{ width: 110 }}>
                  {Object.entries(TYPE_MAP).map(([k, v]) => (
                    <Option key={k} value={Number(k)}>{v.text}</Option>
                  ))}
                </Select>
              </Form.Item>
              <Form.Item name="status" label="状态" style={{ marginBottom: 8 }}>
                <Select placeholder="全部" allowClear style={{ width: 100 }}>
                  {Object.entries(STATUS_MAP).map(([k, v]) => (
                    <Option key={k} value={Number(k)}>{v.text}</Option>
                  ))}
                </Select>
              </Form.Item>
              <Form.Item name="stationId" label="电站" style={{ marginBottom: 8 }}>
                <Select
                  placeholder="全部"
                  allowClear
                  loading={stationLoading}
                  style={{ width: 140 }}
                  showSearch
                  optionFilterProp="label"
                >
                  {stationList.map(s => (
                    <Option key={s.id} value={s.id} label={s.stationName}>
                      {s.stationName}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
              <Form.Item name="keyword" label="关键词" style={{ marginBottom: 8 }}>
                <Input placeholder="指令号/电站" style={{ width: 140 }} allowClear />
              </Form.Item>
              <Form.Item style={{ marginBottom: 8 }}>
                <Space>
                  <Button type="primary" size="small" onClick={handleQuery}>搜索</Button>
                  <Button size="small" onClick={handleReset}>重置</Button>
                </Space>
              </Form.Item>
            </Form>

            <Table
              columns={columns}
              dataSource={tableData}
              rowKey="id"
              loading={tableLoading}
              size="small"
              scroll={{ x: 1300, y: 420 }}
              pagination={{
                current: pageNum,
                pageSize: PAGE_SIZE,
                total,
                showTotal: (t) => `共 ${t} 条`,
                showSizeChanger: false,
                onChange: handlePageChange,
                size: 'small'
              }}
              onRow={(record) => ({
                onClick: () => handleRowClick(record),
                style: {
                  cursor: 'pointer',
                  background: selectedRowId === record.id ? '#e6f7ff' : undefined
                }
              })}
            />
          </Card>
        </Col>

        <Col xs={24} lg={10} style={{ marginBottom: 16 }}>
          <Card
            title={
              <Space>
                <LineChartOutlined />
                <span>指令执行曲线对比</span>
                {selectedRowId && (
                  <Tag color="blue" style={{ marginLeft: 8 }}>
                    选中ID: {selectedRowId}
                  </Tag>
                )}
              </Space>
            }
            size="small"
            styles={{ body: { padding: 12 } }}
            style={{ marginBottom: 16 }}
          >
            <Tabs
              size="small"
              activeKey={String(curveType)}
              onChange={(key) => {
                setCurveType(Number(key))
                if (selectedRowId) fetchCurve(selectedRowId, Number(key))
              }}
              style={{ marginBottom: 8 }}
              items={[
                { key: '1', label: '有功功率' },
                { key: '2', label: '电压' },
                { key: '3', label: '频率' }
              ]}
            />
            <div style={{ height: 320, position: 'relative' }}>
              <Spin spinning={curveLoading} style={{ position: 'absolute', zIndex: 10, top: '50%', left: '50%', transform: 'translate(-50%, -50%)' }} />
              {curveData ? (
                <ReactECharts
                  option={buildCurveOption(curveData, curveType, false) || {}}
                  style={{ height: 320, width: '100%' }}
                  notMerge
                  lazyUpdate
                />
              ) : (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description={<span style={{ color: '#8c8c8c' }}>请点击左侧表格中的指令行查看曲线</span>}
                  style={{ height: 320, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                />
              )}
            </div>
          </Card>

          <Card
            title={
              <Space>
                <DatabaseOutlined />
                <span>最近上传状态</span>
              </Space>
            }
            size="small"
            extra={
              <Button size="small" icon={<ReloadOutlined />} onClick={fetchUploadRecords}>
                刷新
              </Button>
            }
            styles={{ body: { padding: 0 } }}
          >
            <Spin spinning={uploadLoading}>
              <List
                size="small"
                dataSource={uploadList}
                locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无上传记录" style={{ padding: 20 }} /> }}
                renderItem={(item) => {
                  const typeInfo = TYPE_MAP[item.commandType] || { color: 'default', text: item.commandType }
                  const statusInfo = UPLOAD_STATUS_MAP[item.status] || { color: 'default', text: item.status }
                  return (
                    <List.Item style={{ padding: '8px 12px', borderBottom: '1px solid #f0f0f0' }}>
                      <Space size="middle" style={{ width: '100%' }} wrap>
                        <span style={{ color: '#8c8c8c', fontSize: 12, fontFamily: 'monospace', minWidth: 66 }}>
                          {item.createTime ? item.createTime.slice(11, 19) : '-'}
                        </span>
                        <span style={{ fontSize: 12, maxWidth: 120, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {item.stationName || '-'}
                        </span>
                        <Tag color={typeInfo.color} style={{ margin: 0, fontSize: 11 }}>
                          {typeInfo.text}
                        </Tag>
                        <Tag color={statusInfo.color} style={{ margin: 0, fontSize: 11 }}>
                          {statusInfo.text}
                        </Tag>
                      </Space>
                    </List.Item>
                  )
                }}
                style={{ maxHeight: 220, overflowY: 'auto' }}
              />
            </Spin>
          </Card>
        </Col>

        <Col xs={24} lg={6} style={{ marginBottom: 16 }}>
          <Card
            title={
              <Space>
                <ExperimentOutlined />
                <span>协议配置状态</span>
              </Space>
            }
            size="small"
            extra={
              <Button size="small" icon={<ReloadOutlined />} onClick={fetchProtocolConfigs}>
                刷新
              </Button>
            }
            styles={{ body: { padding: 12 } }}
            style={{ marginBottom: 16 }}
          >
            <Spin spinning={protocolLoading}>
              {protocolList.length === 0 ? (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无协议配置" style={{ padding: 10 }} />
              ) : (
                <Space direction="vertical" size={10} style={{ width: '100%' }}>
                  {protocolList.map(config => {
                    const statusInfo = PROTOCOL_STATUS_MAP[config.status] || PROTOCOL_STATUS_MAP[0]
                    return (
                      <div
                        key={config.id}
                        style={{
                          padding: 12,
                          border: '1px solid #f0f0f0',
                          borderRadius: 6,
                          background: '#fafafa'
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
                          <Space>
                            <Badge color={statusInfo.color} />
                            <span style={{ fontWeight: 500 }}>
                              {config.protocolName || config.protocolType || '未知协议'}
                            </span>
                          </Space>
                          <Tag color={statusInfo.color} style={{ margin: 0 }}>
                            {statusInfo.text}
                          </Tag>
                        </div>
                        <div style={{ fontSize: 12, color: '#666', marginBottom: 8 }}>
                          <div>主站: {config.masterIp || '-'}:{config.masterPort || '-'}</div>
                          {config.slaveIp && <div>从站: {config.slaveIp}:{config.slavePort || ''}</div>}
                        </div>
                        <Button
                          type="primary"
                          size="small"
                          block
                          icon={<ExperimentOutlined />}
                          loading={testingProtocolId === config.id}
                          onClick={() => handleTestProtocol(config)}
                        >
                          测试连接
                        </Button>
                      </div>
                    )
                  })}
                </Space>
              )}
            </Spin>
          </Card>

          <Card
            title={
              <Space>
                <SendOutlined />
                <span>人工调度指令下发</span>
              </Space>
            }
            size="small"
            styles={{ body: { padding: 12 } }}
          >
            <Form
              form={commandForm}
              layout="vertical"
              initialValues={{ commandType: 1, priority: 3, startStop: true }}
              onFinish={handleFormSubmit}
            >
              <Form.Item
                name="stationId"
                label="电站选择"
                rules={[{ required: true, message: '请选择电站' }]}
                style={{ marginBottom: 12 }}
              >
                <Select
                  placeholder="请选择电站"
                  loading={stationLoading}
                  showSearch
                  optionFilterProp="label"
                  onChange={handleStationChangeForForm}
                >
                  {stationList.map(s => (
                    <Option key={s.id} value={s.id} label={s.stationName}>
                      {s.stationName}
                    </Option>
                  ))}
                </Select>
              </Form.Item>

              <Form.Item
                name="inverterId"
                label="逆变器"
                rules={[{ required: true, message: '请选择逆变器' }]}
                style={{ marginBottom: 12 }}
              >
                <Select
                  placeholder="请选择逆变器"
                  loading={formInverterLoading}
                  disabled={!commandForm.getFieldValue('stationId')}
                  showSearch
                  optionFilterProp="label"
                >
                  <Option value="all" label="全站调节">
                    <Tag color="blue">全站调节</Tag>
                  </Option>
                  {formInverterList.map(inv => (
                    <Option key={inv.id} value={inv.id} label={inv.inverterName || inv.deviceCode}>
                      {inv.inverterName || inv.deviceCode || `逆变器#${inv.id}`}
                    </Option>
                  ))}
                </Select>
              </Form.Item>

              <Form.Item
                name="commandType"
                label="指令类型"
                rules={[{ required: true, message: '请选择指令类型' }]}
                style={{ marginBottom: 12 }}
              >
                <Radio.Group onChange={handleCommandTypeChange}>
                  <Space direction="vertical" size={4}>
                    {Object.entries(TYPE_MAP).map(([k, v]) => (
                      <Radio key={k} value={Number(k)}>
                        <Tag color={v.color} style={{ margin: 0 }}>{v.text}</Tag>
                      </Radio>
                    ))}
                  </Space>
                </Radio.Group>
              </Form.Item>

              {selectedCommandType === 1 && (
                <Form.Item
                  name="targetActivePower"
                  label="目标有功功率 (kW)"
                  rules={[{ required: true, message: '请输入目标有功功率' }]}
                  style={{ marginBottom: 12 }}
                >
                  <InputNumber style={{ width: '100%' }} min={0} step={10} placeholder="请输入目标有功功率" />
                </Form.Item>
              )}

              {selectedCommandType === 2 && (
                <Form.Item
                  name="targetReactivePower"
                  label="目标无功功率 (kVar)"
                  rules={[{ required: true, message: '请输入目标无功功率' }]}
                  style={{ marginBottom: 12 }}
                >
                  <InputNumber style={{ width: '100%' }} step={10} placeholder="请输入目标无功功率" />
                </Form.Item>
              )}

              {selectedCommandType === 3 && (
                <Form.Item
                  name="targetVoltage"
                  label="目标电压 (V)"
                  rules={[{ required: true, message: '请输入目标电压' }]}
                  style={{ marginBottom: 12 }}
                >
                  <InputNumber style={{ width: '100%' }} min={0} step={1} placeholder="请输入目标电压" />
                </Form.Item>
              )}

              {selectedCommandType === 4 && (
                <Form.Item
                  name="targetFrequency"
                  label="目标频率 (Hz)"
                  rules={[{ required: true, message: '请输入目标频率' }]}
                  style={{ marginBottom: 12 }}
                >
                  <InputNumber style={{ width: '100%' }} min={0} step={0.1} placeholder="请输入目标频率" />
                </Form.Item>
              )}

              {selectedCommandType === 5 && (
                <Form.Item
                  name="startStop"
                  label="启停控制"
                  valuePropName="checked"
                  style={{ marginBottom: 12 }}
                >
                  <Switch
                    checkedChildren={<PlayCircleOutlined />}
                    unCheckedChildren={<StopOutlined />}
                  />
                  <span style={{ marginLeft: 12, color: '#666' }}>
                    {commandForm.getFieldValue('startStop') !== false ? '启动' : '停止'}
                  </span>
                </Form.Item>
              )}

              <Form.Item
                name="priority"
                label="优先级"
                rules={[{ required: true, message: '请选择优先级' }]}
                style={{ marginBottom: 12 }}
              >
                <Radio.Group>
                  <Space wrap>
                    {Object.entries(PRIORITY_MAP).map(([k, v]) => (
                      <Radio key={k} value={Number(k)}>
                        <Tag color={v.color} style={{ margin: 0 }}>{v.text}</Tag>
                      </Radio>
                    ))}
                  </Space>
                </Radio.Group>
              </Form.Item>

              <Form.Item
                name="remark"
                label="备注"
                style={{ marginBottom: 16 }}
              >
                <TextArea rows={2} placeholder="请输入备注信息（可选）" maxLength={200} showCount />
              </Form.Item>

              <Form.Item style={{ marginBottom: 0 }}>
                <Button
                  type="primary"
                  size="large"
                  block
                  icon={<SendOutlined />}
                  htmlType="submit"
                  loading={commandSubmitting}
                >
                  立即下发
                </Button>
              </Form.Item>
            </Form>
          </Card>
        </Col>
      </Row>

      <Modal
        title="调度指令详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>关闭</Button>
        ]}
        width={820}
        destroyOnClose
      >
        <Spin spinning={detailLoading}>
          {currentDetail && (
            <div>
              <Descriptions
                bordered
                size="small"
                column={2}
                style={{ marginBottom: 16 }}
              >
                <Descriptions.Item label="指令号">
                  <Space>
                    <span style={{ fontFamily: 'monospace' }}>{currentDetail.commandNo || '-'}</span>
                    <CopyOutlined
                      style={{ color: '#1890ff', cursor: 'pointer' }}
                      onClick={() => handleCopy(currentDetail.commandNo)}
                    />
                  </Space>
                </Descriptions.Item>
                <Descriptions.Item label="下发时间">{currentDetail.createTime || '-'}</Descriptions.Item>

                <Descriptions.Item label="来源">
                  {(() => { const info = SOURCE_MAP[currentDetail.source] || { color: 'default', text: currentDetail.source }; return <Tag color={info.color}>{info.text}</Tag>; })()}
                </Descriptions.Item>
                <Descriptions.Item label="类型">
                  {(() => { const info = TYPE_MAP[currentDetail.commandType] || { color: 'default', text: currentDetail.commandType }; return <Tag color={info.color}>{info.text}</Tag>; })()}
                </Descriptions.Item>

                <Descriptions.Item label="优先级">
                  {(() => { const info = PRIORITY_MAP[currentDetail.priority] || { color: 'default', text: currentDetail.priority }; return <Tag color={info.color}>{info.text}</Tag>; })()}
                </Descriptions.Item>
                <Descriptions.Item label="状态">
                  {(() => { const info = STATUS_MAP[currentDetail.status] || { color: 'default', text: currentDetail.status }; return <Tag color={info.color}>{info.text}</Tag>; })()}
                </Descriptions.Item>

                <Descriptions.Item label="电站">{currentDetail.stationName || '-'}</Descriptions.Item>
                <Descriptions.Item label="逆变器">{currentDetail.inverterName || (currentDetail.inverterId ? `#${currentDetail.inverterId}` : '全站')}</Descriptions.Item>

                <Descriptions.Item label="目标有功功率" span={1}>
                  {formatNumber(currentDetail.targetActivePower)} kW
                </Descriptions.Item>
                <Descriptions.Item label="实际有功功率" span={1}>
                  {formatNumber(currentDetail.actualActivePower)} kW
                </Descriptions.Item>

                {currentDetail.targetReactivePower != null && (
                  <Descriptions.Item label="目标无功功率" span={1}>
                    {formatNumber(currentDetail.targetReactivePower)} kVar
                  </Descriptions.Item>
                )}
                {currentDetail.actualReactivePower != null && (
                  <Descriptions.Item label="实际无功功率" span={1}>
                    {formatNumber(currentDetail.actualReactivePower)} kVar
                  </Descriptions.Item>
                )}

                {currentDetail.targetVoltage != null && (
                  <Descriptions.Item label="目标电压" span={1}>
                    {formatNumber(currentDetail.targetVoltage)} V
                  </Descriptions.Item>
                )}
                {currentDetail.actualVoltage != null && (
                  <Descriptions.Item label="实际电压" span={1}>
                    {formatNumber(currentDetail.actualVoltage)} V
                  </Descriptions.Item>
                )}

                {currentDetail.targetFrequency != null && (
                  <Descriptions.Item label="目标频率" span={1}>
                    {formatNumber(currentDetail.targetFrequency)} Hz
                  </Descriptions.Item>
                )}
                {currentDetail.actualFrequency != null && (
                  <Descriptions.Item label="实际频率" span={1}>
                    {formatNumber(currentDetail.actualFrequency)} Hz
                  </Descriptions.Item>
                )}

                <Descriptions.Item label="偏差" span={1}>
                  <span style={{ color: getDeviationColor(currentDetail.deviationPercent), fontWeight: 500 }}>
                    {currentDetail.deviationPercent != null ? `${currentDetail.deviationPercent > 0 ? '+' : ''}${Number(currentDetail.deviationPercent).toFixed(2)}%` : '-'}
                  </span>
                </Descriptions.Item>
                <Descriptions.Item label="执行耗时">
                  {currentDetail.executeDuration != null ? `${currentDetail.executeDuration}ms` : '-'}
                </Descriptions.Item>

                <Descriptions.Item label="备注" span={2}>
                  {currentDetail.remark || '-'}
                </Descriptions.Item>

                {currentDetail.failReason && (
                  <Descriptions.Item label="失败原因" span={2}>
                    <span style={{ color: '#ff4d4f' }}>{currentDetail.failReason}</span>
                  </Descriptions.Item>
                )}
              </Descriptions>

              <Divider style={{ margin: '8px 0 12px' }} />

              <div style={{ marginBottom: 8 }}>
                <Tabs
                  size="small"
                  activeKey={String(detailCurveType)}
                  onChange={(key) => setDetailCurveType(Number(key))}
                  items={[
                    { key: '1', label: '有功功率' },
                    { key: '2', label: '电压' },
                    { key: '3', label: '频率' }
                  ]}
                />
              </div>

              <div style={{ height: 240 }}>
                {detailCurveData ? (
                  <ReactECharts
                    option={buildCurveOption(detailCurveData, detailCurveType, true) || {}}
                    style={{ height: 240, width: '100%' }}
                    notMerge
                    lazyUpdate
                  />
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无曲线数据" style={{ height: 240, display: 'flex', alignItems: 'center', justifyContent: 'center' }} />
                )}
              </div>
            </div>
          )}
        </Spin>
      </Modal>
    </div>
  )
}

export default GridDispatchDashboard
