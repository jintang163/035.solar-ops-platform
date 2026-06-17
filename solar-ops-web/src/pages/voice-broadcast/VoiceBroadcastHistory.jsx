import React, { useState, useEffect, useRef, useCallback } from 'react'
import {
  Table,
  Card,
  Form,
  Select,
  Input,
  Button,
  Modal,
  Switch,
  Slider,
  Space,
  Tag,
  message,
  notification,
  DatePicker,
  Row,
  Col,
  Statistic
} from 'antd'
import {
  SoundOutlined,
  SettingOutlined,
  ReloadOutlined,
  AudioOutlined,
  PlayCircleOutlined,
  WarningOutlined,
  BellOutlined,
  CloseOutlined
} from '@ant-design/icons'
import {
  getBroadcastHistory,
  getBroadcastConfig,
  updateBroadcastConfig,
  testBroadcast,
  retryBroadcast
} from '../../api/voiceBroadcast'
import {
  getVoiceBroadcastWebSocket,
  closeVoiceBroadcastWebSocket
} from '../../utils/voiceBroadcastWebsocket'

const { RangePicker } = DatePicker
const { Option } = Select

const BROADCAST_TYPE_MAP = {
  1: { color: 'blue', text: '通讯中断', icon: '📡' },
  2: { color: 'red', text: '火灾预警', icon: '🔥' },
  3: { color: 'orange', text: '设备故障', icon: '⚠️' },
  4: { color: 'volcano', text: '紧急告警', icon: '🚨' },
  5: { color: 'purple', text: '数据异常', icon: '📊' }
}

const ALARM_LEVEL_MAP = {
  1: { color: 'blue', text: '提示' },
  2: { color: 'cyan', text: '中级' },
  3: { color: 'orange', text: '高级' },
  4: { color: 'red', text: '紧急' }
}

const STATUS_MAP = {
  0: { color: 'default', text: '待播报' },
  1: { color: 'green', text: '已播报' },
  2: { color: 'red', text: '播报失败' }
}

const PAGE_SIZE = 10

const useVoicePlay = (config = {}) => {
  const synthRef = useRef(null)
  const [speaking, setSpeaking] = useState(false)

  useEffect(() => {
    if (typeof window !== 'undefined' && window.speechSynthesis) {
      synthRef.current = window.speechSynthesis
    }
    return () => {
      if (synthRef.current) {
        synthRef.current.cancel()
      }
    }
  }, [])

  const speak = useCallback((text, options = {}) => {
    if (!synthRef.current || !text) {
      console.warn('[语音播报] 浏览器不支持语音合成或文本为空')
      return
    }
    try {
      synthRef.current.cancel()
      const utterance = new SpeechSynthesisUtterance(text)
      utterance.lang = options.lang || 'zh-CN'
      utterance.volume = options.volume != null ? options.volume : (config.volume != null ? config.volume : 1)
      utterance.rate = options.rate != null ? options.rate : (config.rate != null ? config.rate : 1)
      utterance.pitch = options.pitch || 1

      utterance.onstart = () => setSpeaking(true)
      utterance.onend = () => setSpeaking(false)
      utterance.onerror = () => setSpeaking(false)

      synthRef.current.speak(utterance)
    } catch (e) {
      console.error('[语音播报] 播放语音失败:', e)
      setSpeaking(false)
    }
  }, [config])

  const stop = useCallback(() => {
    if (synthRef.current) {
      synthRef.current.cancel()
      setSpeaking(false)
    }
  }, [])

  return { speak, stop, speaking }
}

const VoiceBroadcastHistory = () => {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [queryForm] = Form.useForm()
  const [configModalVisible, setConfigModalVisible] = useState(false)
  const [configForm] = Form.useForm()
  const [configLoading, setConfigLoading] = useState(false)
  const [broadcastConfig, setBroadcastConfig] = useState({
    enabled: true,
    minAlarmLevel: 3,
    volume: 0.8,
    rate: 1,
    startTime: '08:00',
    endTime: '20:00'
  })

  const [toastVisible, setToastVisible] = useState(false)
  const [currentToast, setCurrentToast] = useState(null)
  const toastTimerRef = useRef(null)

  const { speak, stop, speaking } = useVoicePlay({
    volume: broadcastConfig.volume,
    rate: broadcastConfig.rate
  })

  const fetchData = useCallback(async (page = 1, extraParams = {}) => {
    setLoading(true)
    try {
      const params = { pageNum: page, pageSize: PAGE_SIZE, ...extraParams }
      const res = await getBroadcastHistory(params)
      const pageResult = res.data || {}
      setData(pageResult.list || [])
      setTotal(pageResult.total || 0)
      setPageNum(pageResult.pageNum || page)
    } catch {
      setData([])
      setTotal(0)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData(1)
    loadConfig()

    const ws = getVoiceBroadcastWebSocket()
    ws.connect()

    const handleBroadcast = (broadcastData) => {
      handleRealtimeBroadcast(broadcastData)
    }

    ws.on('broadcast', handleBroadcast)

    return () => {
      ws.off('broadcast', handleBroadcast)
      closeVoiceBroadcastWebSocket()
      if (toastTimerRef.current) {
        clearTimeout(toastTimerRef.current)
      }
    }
  }, [fetchData])

  const loadConfig = async () => {
    try {
      const res = await getBroadcastConfig()
      if (res.data) {
        setBroadcastConfig(prev => ({ ...prev, ...res.data }))
      }
    } catch {
    }
  }

  const isWithinBroadcastTime = () => {
    const now = new Date()
    const currentMinutes = now.getHours() * 60 + now.getMinutes()
    const [startH, startM] = broadcastConfig.startTime.split(':').map(Number)
    const [endH, endM] = broadcastConfig.endTime.split(':').map(Number)
    const startMinutes = startH * 60 + startM
    const endMinutes = endH * 60 + endM
    return currentMinutes >= startMinutes && currentMinutes <= endMinutes
  }

  const handleRealtimeBroadcast = (broadcastData) => {
    if (!broadcastConfig.enabled) {
      return
    }
    if (broadcastData.alarmLevel < broadcastConfig.minAlarmLevel) {
      return
    }

    setData(prev => [broadcastData, ...prev].slice(0, 100))
    setTotal(prev => prev + 1)

    const levelInfo = ALARM_LEVEL_MAP[broadcastData.alarmLevel] || {}
    const typeInfo = BROADCAST_TYPE_MAP[broadcastData.broadcastType] || {}

    notification.open({
      message: `${levelInfo.icon || ''} ${levelInfo.text || '告警'}语音播报`,
      description: broadcastData.content || broadcastData.description,
      icon: <BellOutlined style={{ color: levelInfo.color || '#faad14' }} />,
      duration: 8,
      placement: 'topRight'
    })

    showBroadcastToast(broadcastData)

    if (isWithinBroadcastTime()) {
      if (broadcastData.audioUrl) {
        const audio = new Audio(broadcastData.audioUrl)
        audio.volume = broadcastConfig.volume
        audio.play().catch(err => {
          console.error('[语音播报] 音频播放失败，使用TTS兜底:', err)
          speak(broadcastData.content || broadcastData.description)
        })
      } else {
        speak(broadcastData.content || broadcastData.description)
      }
    }
  }

  const showBroadcastToast = (broadcastData) => {
    setCurrentToast(broadcastData)
    setToastVisible(true)

    if (toastTimerRef.current) {
      clearTimeout(toastTimerRef.current)
    }
    toastTimerRef.current = setTimeout(() => {
      setToastVisible(false)
      setCurrentToast(null)
    }, 10000)
  }

  const handlePlay = (record) => {
    if (record.audioUrl) {
      const audio = new Audio(record.audioUrl)
      audio.volume = broadcastConfig.volume
      audio.play().catch(err => {
        console.error('[语音播报] 音频播放失败:', err)
        speak(record.content || record.description)
      })
    } else {
      speak(record.content || record.description)
    }
  }

  const handleRetry = async (record) => {
    try {
      await retryBroadcast(record.id)
      message.success('重试播报成功')
      handlePlay(record)
    } catch (error) {
      message.error(error.message || '重试失败')
    }
  }

  const handleQuery = async () => {
    try {
      const values = await queryForm.validateFields()
      const queryParams = { ...values }
      if (values.dateRange) {
        queryParams.startTime = values.dateRange[0].format('YYYY-MM-DD HH:mm:ss')
        queryParams.endTime = values.dateRange[1].format('YYYY-MM-DD HH:mm:ss')
        delete queryParams.dateRange
      }
      fetchData(1, queryParams)
    } catch {
    }
  }

  const handleReset = () => {
    queryForm.resetFields()
    fetchData(1)
  }

  const handleTestBroadcast = async () => {
    try {
      await testBroadcast('这是一条语音播报测试消息，系统运行正常。')
      message.success('测试播报已触发')
      speak('这是一条语音播报测试消息，系统运行正常。')
    } catch (error) {
      message.error(error.message || '测试失败')
    }
  }

  const handleOpenConfig = () => {
    configForm.setFieldsValue(broadcastConfig)
    setConfigModalVisible(true)
  }

  const handleSaveConfig = async () => {
    try {
      const values = await configForm.validateFields()
      setConfigLoading(true)
      await updateBroadcastConfig(values)
      setBroadcastConfig(prev => ({ ...prev, ...values }))
      message.success('配置保存成功')
      setConfigModalVisible(false)
    } catch (error) {
      message.error(error.message || '保存失败')
    } finally {
      setConfigLoading(false)
    }
  }

  const handlePageChange = (page) => {
    fetchData(page)
  }

  const columns = [
    {
      title: '播报时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      sorter: (a, b) => new Date(b.createTime) - new Date(a.createTime),
      defaultSortOrder: 'descend'
    },
    {
      title: '类型',
      dataIndex: 'broadcastType',
      key: 'broadcastType',
      width: 120,
      render: (type) => {
        const info = BROADCAST_TYPE_MAP[type] || { color: 'default', text: '未知' }
        return (
          <Tag color={info.color}>
            {info.icon} {info.text}
          </Tag>
        )
      }
    },
    {
      title: '级别',
      dataIndex: 'alarmLevel',
      key: 'alarmLevel',
      width: 100,
      render: (level) => {
        const info = ALARM_LEVEL_MAP[level] || { color: 'default', text: level }
        return <Tag color={info.color}>{info.text}</Tag>
      }
    },
    {
      title: '电站名称',
      dataIndex: 'stationName',
      key: 'stationName',
      width: 160,
      render: (text, record) => text || (record.stationId ? `电站#${record.stationId}` : '-')
    },
    {
      title: '逆变器',
      dataIndex: 'inverterName',
      key: 'inverterName',
      width: 140,
      render: (text, record) => text || (record.inverterId ? `逆变器#${record.inverterId}` : '-')
    },
    {
      title: '播报内容',
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
      render: (text, record) => text || record.description || '-'
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => {
        const info = STATUS_MAP[status] || { color: 'default', text: status }
        return <Tag color={info.color}>{info.text}</Tag>
      }
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<PlayCircleOutlined />}
            onClick={() => handlePlay(record)}
          >
            播放
          </Button>
          <Button
            type="link"
            size="small"
            icon={<ReloadOutlined />}
            onClick={() => handleRetry(record)}
          >
            重试
          </Button>
        </Space>
      )
    }
  ]

  const statsData = [
    { title: '今日播报', value: data.filter(d => {
        const today = new Date().toDateString()
        return new Date(d.createTime).toDateString() === today
      }).length, icon: <SoundOutlined />, color: '#1890ff' },
    { title: '高级告警', value: data.filter(d => d.alarmLevel >= 3).length, icon: <WarningOutlined />, color: '#fa8c16' },
    { title: '紧急告警', value: data.filter(d => d.alarmLevel === 4).length, icon: <BellOutlined />, color: '#ff4d4f' },
    { title: '播报失败', value: data.filter(d => d.status === 2).length, icon: <CloseOutlined />, color: '#8c8c8c' }
  ]

  return (
    <div className="voice-broadcast-page" style={{ position: 'relative' }}>
      {toastVisible && currentToast && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            zIndex: 1000,
            padding: '16px 24px',
            background: `linear-gradient(90deg, ${ALARM_LEVEL_MAP[currentToast.alarmLevel]?.color || '#faad14'} 0%, ${ALARM_LEVEL_MAP[currentToast.alarmLevel]?.color || '#faad14'}dd 100%)`,
            color: '#fff',
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
            transform: toastVisible ? 'translateY(0)' : 'translateY(-100%)',
            transition: 'transform 0.3s ease',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, flex: 1 }}>
            <AudioOutlined style={{ fontSize: 24 }} spin={speaking} />
            <div>
              <div style={{ fontWeight: 600, fontSize: 16 }}>
                {BROADCAST_TYPE_MAP[currentToast.broadcastType]?.text || '告警'} - {ALARM_LEVEL_MAP[currentToast.alarmLevel]?.text || ''}
              </div>
              <div style={{ fontSize: 14, opacity: 0.9 }}>
                {currentToast.content || currentToast.description}
              </div>
            </div>
          </div>
          <Space>
            <Button
              type="primary"
              size="small"
              icon={<PlayCircleOutlined />}
              onClick={() => handlePlay(currentToast)}
            >
              播放
            </Button>
            <Button
              size="small"
              onClick={() => {
                stop()
                setToastVisible(false)
                setCurrentToast(null)
                if (toastTimerRef.current) clearTimeout(toastTimerRef.current)
              }}
            >
              关闭
            </Button>
          </Space>
        </div>
      )}

      <Card
        title="语音播报记录"
        extra={
          <Space>
            <Button icon={<AudioOutlined />} onClick={handleTestBroadcast}>
              测试播报
            </Button>
            <Button icon={<SettingOutlined />} onClick={handleOpenConfig}>
              配置
            </Button>
            <Button icon={<ReloadOutlined />} onClick={() => fetchData(1)}>
              刷新
            </Button>
          </Space>
        }
      >
        <Row gutter={16} style={{ marginBottom: 16 }}>
          {statsData.map((stat, index) => (
            <Col xs={12} sm={6} key={index}>
              <Card size="small">
                <Statistic
                  title={stat.title}
                  value={stat.value}
                  valueStyle={{ color: stat.color, fontSize: 20 }}
                  prefix={stat.icon}
                />
              </Card>
            </Col>
          ))}
        </Row>

        <Form form={queryForm} layout="inline" style={{ marginBottom: 16 }}>
          <Form.Item name="broadcastType" label="播报类型">
            <Select placeholder="全部" allowClear style={{ width: 140 }}>
              {Object.entries(BROADCAST_TYPE_MAP).map(([key, val]) => (
                <Option key={key} value={Number(key)}>{val.text}</Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="alarmLevel" label="告警级别">
            <Select placeholder="全部" allowClear style={{ width: 120 }}>
              {Object.entries(ALARM_LEVEL_MAP).map(([key, val]) => (
                <Option key={key} value={Number(key)}>{val.text}</Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="keyword" label="关键词">
            <Input placeholder="搜索内容" style={{ width: 180 }} allowClear />
          </Form.Item>
          <Form.Item name="dateRange" label="时间范围">
            <RangePicker showTime />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" onClick={handleQuery}>查询</Button>
              <Button onClick={handleReset}>重置</Button>
            </Space>
          </Form.Item>
        </Form>

        <Table
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1300 }}
          pagination={{
            current: pageNum,
            pageSize: PAGE_SIZE,
            total,
            showTotal: (t) => `共 ${t} 条`,
            showSizeChanger: false,
            onChange: handlePageChange
          }}
        />
      </Card>

      <Modal
        title="语音播报配置"
        open={configModalVisible}
        onCancel={() => setConfigModalVisible(false)}
        onOk={handleSaveConfig}
        confirmLoading={configLoading}
        width={520}
      >
        <Form
          form={configForm}
          layout="vertical"
          initialValues={broadcastConfig}
        >
          <Form.Item
            name="enabled"
            label="启用语音播报"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>
          <Form.Item
            name="minAlarmLevel"
            label="最低播报级别"
            rules={[{ required: true, message: '请选择最低播报级别' }]}
          >
            <Select>
              {Object.entries(ALARM_LEVEL_MAP).map(([key, val]) => (
                <Option key={key} value={Number(key)}>{val.text}</Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="volume" label="音量">
            <Slider min={0} max={1} step={0.1} />
          </Form.Item>
          <Form.Item name="rate" label="语速">
            <Slider min={0.5} max={2} step={0.1} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="startTime"
                label="播报开始时间"
                rules={[{ required: true, message: '请选择开始时间' }]}
              >
                <Input placeholder="如: 08:00" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="endTime"
                label="播报结束时间"
                rules={[{ required: true, message: '请选择结束时间' }]}
              >
                <Input placeholder="如: 20:00" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  )
}

export default VoiceBroadcastHistory
