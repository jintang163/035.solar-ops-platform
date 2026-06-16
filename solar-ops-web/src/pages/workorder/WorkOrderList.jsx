import React, { useState, useEffect, useCallback } from 'react'
import {
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  message,
  Card,
  Tag,
  Tabs,
  Descriptions,
  Row,
  Col,
  Statistic,
  InputNumber,
  Select,
  Tooltip,
  Progress,
  Spin,
  Empty,
  List,
  Rate
} from 'antd'
import {
  PlusOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  WarningOutlined,
  ThunderboltOutlined,
  SafetyCertificateOutlined,
  BulbOutlined
} from '@ant-design/icons'
import {
  getWorkOrderPage,
  getWorkOrderDetail,
  createWorkOrder,
  acceptWorkOrder,
  startProcessWorkOrder,
  submitCheckWorkOrder,
  completeWorkOrder,
  closeWorkOrder,
  getWorkOrderStatistics
} from '../../api/workorder'
import { recommendKnowledge, submitKnowledgeFeedback, getUserFeedback, recordKnowledgeUsage } from '../../api/knowledge'
import { getUser } from '../../utils/auth'
import KnowledgeRecommendModal from '../../components/KnowledgeRecommendModal'

const { TextArea } = Input

const STATUS_MAP = {
  0: { color: 'orange', text: '待接单' },
  1: { color: 'blue', text: '已接单' },
  2: { color: 'processing', text: '处理中' },
  3: { color: 'purple', text: '待验收' },
  4: { color: 'green', text: '已完成' },
  5: { color: 'default', text: '已关闭' }
}

const FAULT_LEVEL_MAP = {
  1: { color: 'green', text: '低级' },
  2: { color: 'orange', text: '中级' },
  3: { color: 'red', text: '高级' },
  4: { color: '#cf1322', text: '紧急' }
}

const PAGE_SIZE = 10

const WorkOrderList = () => {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [activeTab, setActiveTab] = useState('all')
  const [statistics, setStatistics] = useState({ 0: 0, 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 })
  const [detailVisible, setDetailVisible] = useState(false)
  const [addVisible, setAddVisible] = useState(false)
  const [actionVisible, setActionVisible] = useState(false)
  const [recommendVisible, setRecommendVisible] = useState(false)
  const [currentOrder, setCurrentOrder] = useState(null)
  const [actionType, setActionType] = useState('')
  const [actionLoading, setActionLoading] = useState(false)
  const [addLoading, setAddLoading] = useState(false)
  const [addForm] = Form.useForm()
  const [actionForm] = Form.useForm()
  const [recommendParams, setRecommendParams] = useState({
    faultCode: '',
    faultName: '',
    description: '',
    faultLevel: null,
    stationId: null,
    inverterId: null
  })
  const [autoRecommendResults, setAutoRecommendResults] = useState([])
  const [autoRecommendLoading, setAutoRecommendLoading] = useState(false)
  const [recommendDebounceTimer, setRecommendDebounceTimer] = useState(null)
  const [userAutoFeedbacks, setUserAutoFeedbacks] = useState({})

  const fetchStatistics = useCallback(async () => {
    try {
      const res = await getWorkOrderStatistics()
      setStatistics(res.data || {})
    } catch {
      // ignore
    }
  }, [])

  const fetchData = useCallback(async (status, page = 1) => {
    setLoading(true)
    try {
      const params = { pageNum: page, pageSize: PAGE_SIZE }
      if (status !== 'all') {
        params.status = Number(status)
      }
      const res = await getWorkOrderPage(params)
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
    fetchStatistics()
  }, [fetchStatistics])

  useEffect(() => {
    fetchData(activeTab, 1)
  }, [activeTab, fetchData])

  const handleTabChange = (key) => {
    setActiveTab(key)
  }

  const handlePageChange = (page) => {
    fetchData(activeTab, page)
  }

  const handleViewDetail = async (record) => {
    try {
      const res = await getWorkOrderDetail(record.id)
      setCurrentOrder(res.data || record)
      setDetailVisible(true)
    } catch {
      setCurrentOrder(record)
      setDetailVisible(true)
    }
  }

  const getCurrentUser = () => {
    const user = getUser() || {}
    return { operatorId: user.id, operatorName: user.name || user.username || '' }
  }

  const handleAction = (record, type) => {
    setCurrentOrder(record)
    setActionType(type)
    actionForm.resetFields()
    setActionVisible(true)
  }

  const handleActionOk = async () => {
    try {
      const values = await actionForm.validateFields()
      const user = getCurrentUser()
      const baseParams = { orderId: currentOrder.id, ...user }
      setActionLoading(true)

      if (actionType === 'accept') {
        await acceptWorkOrder(baseParams)
      } else if (actionType === 'startProcess') {
        await startProcessWorkOrder({ ...baseParams, solution: values.solution })
      } else if (actionType === 'submitCheck') {
        await submitCheckWorkOrder({
          ...baseParams,
          solution: values.solution,
          repairPhotos: values.repairPhotos || ''
        })
      } else if (actionType === 'complete') {
        await completeWorkOrder(baseParams)
      } else if (actionType === 'close') {
        await closeWorkOrder(baseParams)
      }

      message.success('操作成功')
      setActionVisible(false)
      fetchData(activeTab, pageNum)
      fetchStatistics()
    } catch (error) {
      if (error.errorFields) return
      message.error(error.message || '操作失败')
    } finally {
      setActionLoading(false)
    }
  }

  const handleAdd = () => {
    addForm.resetFields()
    setRecommendParams({
      faultCode: '',
      faultName: '',
      description: '',
      faultLevel: null,
      stationId: null,
      inverterId: null
    })
    setAutoRecommendResults([])
    setAutoRecommendLoading(false)
    setUserAutoFeedbacks({})
    if (recommendDebounceTimer) {
      clearTimeout(recommendDebounceTimer)
    }
    setAddVisible(true)
  }

  const triggerAutoRecommend = useCallback(async (values) => {
    const { faultCode, faultName, description, faultLevel, stationId, inverterId } = values || {}
    if (!faultCode && !faultName && !description) {
      setAutoRecommendResults([])
      return
    }
    setAutoRecommendLoading(true)
    try {
      const res = await recommendKnowledge({
        faultCode: faultCode || '',
        faultName: faultName || '',
        description: description || '',
        faultLevel: faultLevel || null,
        stationId: stationId || null,
        inverterId: inverterId || null,
        topN: 3,
        minConfidence: 0.25
      })
      const list = res.data || []
      setAutoRecommendResults(list)

      const currentUser = getUser() || {}
      if (currentUser.id && list.length > 0) {
        const fbMap = {}
        for (const item of list) {
          try {
            const fbRes = await getUserFeedback(item.id, currentUser.id)
            if (fbRes.data) {
              fbMap[item.id] = fbRes.data
            }
          } catch (e) {}
        }
        setUserAutoFeedbacks(fbMap)
      }
    } catch (e) {
      console.warn('自动推荐失败', e)
      setAutoRecommendResults([])
    } finally {
      setAutoRecommendLoading(false)
    }
  }, [recommendDebounceTimer])

  const handleFormValuesChange = useCallback((changedValues, allValues) => {
    const keys = Object.keys(changedValues)
    if (keys.some(k => ['faultCode', 'faultName', 'description', 'faultLevel', 'stationId', 'inverterId'].includes(k))) {
      if (recommendDebounceTimer) {
        clearTimeout(recommendDebounceTimer)
      }
      const timer = setTimeout(() => {
        triggerAutoRecommend(allValues)
      }, 600)
      setRecommendDebounceTimer(timer)
    }
  }, [recommendDebounceTimer, triggerAutoRecommend])

  const handleApplyQuickRecommend = (item) => {
    handleSelectSolution(item)
    const user = getUser() || {}
    if (user?.id) {
      recordKnowledgeUsage({
        knowledgeId: item.id,
        userId: user.id,
        userName: user.name || user.username,
        sourceType: 1
      }).catch(() => {})
    }
  }

  const handleQuickFeedback = async (item, feedbackType) => {
    const currentUser = getUser() || {}
    if (!currentUser.id) {
      message.warning('请先登录')
      return
    }
    try {
      await submitKnowledgeFeedback({
        knowledgeId: item.id,
        userId: currentUser.id,
        userName: currentUser.name || currentUser.username,
        feedbackType
      })
      message.success('反馈已提交')
      setAutoRecommendResults(prev => prev.map(r => {
        if (r.id === item.id) {
          const newR = { ...r }
          const oldFb = userAutoFeedbacks[item.id]
          if (oldFb && oldFb.feedbackType !== feedbackType) {
            if (oldFb.feedbackType === 1) newR.likeCount = Math.max(0, (newR.likeCount || 0) - 1)
            else if (oldFb.feedbackType === 2) newR.dislikeCount = Math.max(0, (newR.dislikeCount || 0) - 1)
          }
          if (feedbackType === 1 && (!oldFb || oldFb.feedbackType !== 1)) {
            newR.likeCount = (newR.likeCount || 0) + 1
          }
          if (feedbackType === 2 && (!oldFb || oldFb.feedbackType !== 2)) {
            newR.dislikeCount = (newR.dislikeCount || 0) + 1
          }
          return newR
        }
        return r
      }))
      setUserAutoFeedbacks(prev => ({ ...prev, [item.id]: { ...prev[item.id], feedbackType } }))
    } catch (e) {
      message.error(e.message || '反馈失败')
    }
  }

  const renderConfidenceBadge = (item) => {
    const info = {
      high: { color: '#52c41a', bg: '#f6ffed', text: '高置信度' },
      medium: { color: '#faad14', bg: '#fffbe6', text: '中置信度' },
      low: { color: '#ff7a45', bg: '#fff2e8', text: '低置信度' }
    }[item.confidenceLevel] || { color: '#999', bg: '#f5f5f5', text: '匹配中' }
    const percent = Math.round(Number(item.confidence || 0) * 100)
    return (
      <span style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 6,
        background: info.bg,
        color: info.color,
        border: `1px solid ${info.color}40`,
        borderRadius: 10,
        padding: '2px 8px',
        fontSize: 12,
        fontWeight: 500
      }}>
        {info.text}
        <Progress
          type="circle"
          size={20}
          percent={percent}
          strokeColor={info.color}
          showInfo={false}
        />
        <span>{percent}%</span>
      </span>
    )
  }

  const handleTriggerRecommend = async () => {
    try {
      const values = await addForm.validateFields(['stationId', 'faultCode', 'description'])
      setRecommendParams({
        faultCode: values.faultCode || '',
        faultName: values.faultName || '',
        description: values.description || '',
        faultLevel: values.faultLevel || null,
        stationId: values.stationId || null,
        inverterId: values.inverterId || null
      })
      setRecommendVisible(true)
    } catch (e) {
      if (e.errorFields) {
        message.warning('请先填写电站ID和故障代码/故障描述')
      }
    }
  }

  const handleSelectSolution = (item) => {
    const currentDesc = addForm.getFieldValue('description') || ''
    const solutionText = item.solutionRichText
      ? item.solutionRichText.replace(/<[^>]*>/g, '').trim()
      : (item.solution || '')

    if (!addForm.getFieldValue('faultName') && item.faultName) {
      addForm.setFieldsValue({ faultName: item.faultName })
    }
    if (!addForm.getFieldValue('faultLevel') && item.faultLevel) {
      addForm.setFieldsValue({ faultLevel: item.faultLevel })
    }

    addForm.setFieldsValue({
      description: currentDesc
        ? `${currentDesc}\n\n【推荐方案】\n${solutionText}`
        : `【推荐方案】\n${solutionText}`
    })

    message.success('已应用推荐方案到工单描述')
  }

  const handleAddOk = async () => {
    try {
      const values = await addForm.validateFields()
      setAddLoading(true)
      await createWorkOrder(values)
      message.success('工单创建成功')
      setAddVisible(false)
      fetchData(activeTab, 1)
      fetchStatistics()
    } catch (error) {
      if (error.errorFields) return
      message.error(error.message || '创建失败')
    } finally {
      setAddLoading(false)
    }
  }

  const handleConfirmAction = (record, type, actionLabel) => {
    Modal.confirm({
      title: '确认操作',
      content: `确定对工单 ${record.orderNo} 执行「${actionLabel}」操作？`,
      onOk: async () => {
        const user = getCurrentUser()
        const baseParams = { orderId: record.id, ...user }
        try {
          if (type === 'accept') {
            await acceptWorkOrder(baseParams)
          } else if (type === 'complete') {
            await completeWorkOrder(baseParams)
          } else if (type === 'close') {
            await closeWorkOrder(baseParams)
          }
          message.success('操作成功')
          fetchData(activeTab, pageNum)
          fetchStatistics()
        } catch (error) {
          message.error(error.message || '操作失败')
        }
      }
    })
  }

  const getActionLabel = (type) => {
    const map = { accept: '接单', startProcess: '开始处理', submitCheck: '提交验收', complete: '完成', close: '关闭' }
    return map[type] || type
  }

  const columns = [
    {
      title: '工单编号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 150,
      render: (text, record) => <a onClick={() => handleViewDetail(record)}>{text}</a>
    },
    {
      title: '故障名称',
      dataIndex: 'faultName',
      key: 'faultName',
      width: 180,
      ellipsis: true
    },
    {
      title: '故障等级',
      dataIndex: 'faultLevel',
      key: 'faultLevel',
      width: 100,
      render: (level) => {
        const info = FAULT_LEVEL_MAP[level] || { color: 'default', text: level }
        return <Tag color={info.color}>{info.text}</Tag>
      }
    },
    {
      title: '所属电站',
      dataIndex: 'stationName',
      key: 'stationName',
      width: 140
    },
    {
      title: '逆变器',
      dataIndex: 'inverterName',
      key: 'inverterName',
      width: 120
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
      title: '处理人',
      dataIndex: 'handlerName',
      key: 'handlerName',
      width: 100,
      render: (text) => text || '-'
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 170
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>
            详情
          </Button>
          {record.status === 0 && (
            <Button type="link" size="small" onClick={() => handleConfirmAction(record, 'accept', '接单')}>
              接单
            </Button>
          )}
          {record.status === 1 && (
            <Button type="link" size="small" onClick={() => handleAction(record, 'startProcess')}>
              处理
            </Button>
          )}
          {record.status === 2 && (
            <Button type="link" size="small" onClick={() => handleAction(record, 'submitCheck')}>
              提交验收
            </Button>
          )}
          {record.status === 3 && (
            <Button type="link" size="small" onClick={() => handleConfirmAction(record, 'complete', '完成')}>
              完成
            </Button>
          )}
          {[0, 1, 2, 3].includes(record.status) && (
            <Button type="link" size="small" danger onClick={() => handleConfirmAction(record, 'close', '关闭')}>
              关闭
            </Button>
          )}
        </Space>
      )
    }
  ]

  const statTotal = Object.values(statistics).reduce((a, b) => a + (Number(b) || 0), 0)

  const tabItems = [
    { key: 'all', label: `全部 (${statTotal})` },
    { key: '0', label: `待接单 (${statistics[0] || 0})` },
    { key: '1', label: `已接单 (${statistics[1] || 0})` },
    { key: '2', label: `处理中 (${statistics[2] || 0})` },
    { key: '3', label: `待验收 (${statistics[3] || 0})` },
    { key: '4', label: `已完成 (${statistics[4] || 0})` },
    { key: '5', label: `已关闭 (${statistics[5] || 0})` }
  ]

  const renderActionModal = () => {
    const showSolution = actionType === 'startProcess' || actionType === 'submitCheck'
    const showPhotos = actionType === 'submitCheck'

    return (
      <Modal
        title={getActionLabel(actionType)}
        open={actionVisible}
        onOk={handleActionOk}
        onCancel={() => setActionVisible(false)}
        confirmLoading={actionLoading}
        okText="确认"
        cancelText="取消"
        width={500}
      >
        <Form form={actionForm} layout="vertical">
          {showSolution && (
            <Form.Item
              name="solution"
              label="处理方案"
              rules={[{ required: true, message: '请输入处理方案' }]}
            >
              <TextArea rows={4} placeholder="请详细描述处理方案" />
            </Form.Item>
          )}
          {showPhotos && (
            <Form.Item name="repairPhotos" label="维修照片">
              <Input placeholder="请输入照片URL，多个用逗号分隔" />
            </Form.Item>
          )}
        </Form>
      </Modal>
    )
  }

  return (
    <div className="workorder-list-page">
      <Card
        title="工单管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新建工单
          </Button>
        }
      >
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col xs={12} sm={6} md={4}>
            <Card size="small">
              <Statistic
                title="全部工单"
                value={statTotal}
                prefix={<ClockCircleOutlined />}
                valueStyle={{ fontSize: 20 }}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6} md={4}>
            <Card size="small">
              <Statistic
                title="待接单"
                value={statistics[0] || 0}
                valueStyle={{ color: '#faad14', fontSize: 20 }}
                prefix={<WarningOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6} md={4}>
            <Card size="small">
              <Statistic
                title="处理中"
                value={statistics[2] || 0}
                valueStyle={{ color: '#1890ff', fontSize: 20 }}
                prefix={<ThunderboltOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6} md={4}>
            <Card size="small">
              <Statistic
                title="待验收"
                value={statistics[3] || 0}
                valueStyle={{ color: '#722ed1', fontSize: 20 }}
                prefix={<SafetyCertificateOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6} md={4}>
            <Card size="small">
              <Statistic
                title="已完成"
                value={statistics[4] || 0}
                valueStyle={{ color: '#52c41a', fontSize: 20 }}
                prefix={<CheckCircleOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6} md={4}>
            <Card size="small">
              <Statistic
                title="已关闭"
                value={statistics[5] || 0}
                valueStyle={{ color: '#999', fontSize: 20 }}
                prefix={<CloseCircleOutlined />}
              />
            </Card>
          </Col>
        </Row>

        <Tabs activeKey={activeTab} onChange={handleTabChange} items={tabItems} />

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
        title="工单详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={700}
      >
        {currentOrder && (
          <Descriptions title="基本信息" bordered column={2} size="small">
            <Descriptions.Item label="工单编号">{currentOrder.orderNo}</Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={(STATUS_MAP[currentOrder.status] || {}).color}>
                {(STATUS_MAP[currentOrder.status] || {}).text || currentOrder.status}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="故障名称" span={2}>{currentOrder.faultName}</Descriptions.Item>
            <Descriptions.Item label="故障代码">{currentOrder.faultCode || '-'}</Descriptions.Item>
            <Descriptions.Item label="故障等级">
              <Tag color={(FAULT_LEVEL_MAP[currentOrder.faultLevel] || {}).color}>
                {(FAULT_LEVEL_MAP[currentOrder.faultLevel] || {}).text || currentOrder.faultLevel}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="所属电站">{currentOrder.stationName || '-'}</Descriptions.Item>
            <Descriptions.Item label="逆变器">{currentOrder.inverterName || '-'}</Descriptions.Item>
            <Descriptions.Item label="处理人">{currentOrder.handlerName || '-'}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{currentOrder.createTime || '-'}</Descriptions.Item>
            <Descriptions.Item label="问题描述" span={2}>{currentOrder.description || '-'}</Descriptions.Item>
            {currentOrder.solution && (
              <Descriptions.Item label="处理方案" span={2}>{currentOrder.solution}</Descriptions.Item>
            )}
          </Descriptions>
        )}
      </Modal>

      {renderActionModal()}

      <Modal
        title={
          <Space>
            <span>新建工单</span>
            <Tag color="purple">支持智能推荐</Tag>
          </Space>
        }
        open={addVisible}
        onOk={handleAddOk}
        onCancel={() => setAddVisible(false)}
        confirmLoading={addLoading}
        okText="提交"
        cancelText="取消"
        width={640}
      >
        <Form
          form={addForm}
          layout="vertical"
          onValuesChange={handleFormValuesChange}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="stationId"
                label="电站ID"
                rules={[{ required: true, message: '请输入电站ID' }]}
              >
                <InputNumber placeholder="请输入电站ID" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="inverterId" label="逆变器ID">
                <InputNumber placeholder="请输入逆变器ID" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="faultCode"
                label="故障代码"
                rules={[{ required: true, message: '请输入故障代码' }]}
                tooltip="如: INV_NO_COMM"
              >
                <Input placeholder="请输入故障代码" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="faultLevel" label="故障级别">
                <Select placeholder="请选择故障级别" allowClear>
                  <Select.Option value={1}>低级</Select.Option>
                  <Select.Option value={2}>中级</Select.Option>
                  <Select.Option value={3}>高级</Select.Option>
                  <Select.Option value={4}>紧急</Select.Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Form.Item
            name="faultName"
            label="故障名称"
            tooltip="如不填写，可通过智能推荐自动填充"
          >
            <Input placeholder="如: 逆变器通讯中断（可留空，点击智能推荐）" />
          </Form.Item>
          <Form.Item
            label="智能推荐解决方案"
            tooltip="基于Elasticsearch + TF-IDF算法，输入故障码和描述后自动匹配相似历史案例"
          >
            <div>
              <Tooltip title="根据故障码和描述智能匹配相似案例">
                <Button
                  type="primary"
                  ghost
                  icon={<BulbOutlined />}
                  onClick={handleTriggerRecommend}
                  block
                >
                  🔍 查看更多推荐方案
                </Button>
              </Tooltip>
            </div>

            <div style={{ marginTop: 12 }}>
              {autoRecommendLoading && (
                <div style={{ textAlign: 'center', padding: 16, color: '#999' }}>
                  <Spin size="small" />
                  <span style={{ marginLeft: 8 }}>AI正在智能匹配相似故障案例...</span>
                </div>
              )}

              {!autoRecommendLoading && autoRecommendResults.length > 0 && (
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', marginBottom: 8 }}>
                    <Tag color="purple" style={{ margin: 0 }}>
                      <BulbOutlined /> 智能推荐 {autoRecommendResults.length} 个相似方案
                    </Tag>
                    <span style={{ fontSize: 12, color: '#999', marginLeft: 8 }}>
                      基于ES + TF-IDF算法匹配
                    </span>
                  </div>
                  <List
                    size="small"
                    dataSource={autoRecommendResults}
                    locale={{ emptyText: '' }}
                    renderItem={(item, idx) => {
                      const feedback = userAutoFeedbacks[item.id]
                      const isLiked = feedback?.feedbackType === 1
                      const isDisliked = feedback?.feedbackType === 2
                      return (
                        <List.Item
                          key={item.id}
                          style={{
                            padding: '10px 12px',
                            marginBottom: 8,
                            border: '1px solid #e6f4ff',
                            background: '#fafcff',
                            borderRadius: 6
                          }}
                        >
                          <div style={{ width: '100%' }}>
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
                              <div>
                                <Space size={6} wrap>
                                  <Tag color="blue" style={{ fontFamily: 'monospace', fontSize: 12, margin: 0 }}>
                                    {item.faultCode}
                                  </Tag>
                                  <Tag color={(FAULT_LEVEL_MAP[item.faultLevel] || {}).color} style={{ margin: 0 }}>
                                    {(FAULT_LEVEL_MAP[item.faultLevel] || {}).text}
                                  </Tag>
                                  <span style={{ fontWeight: 500 }}>{item.faultName}</span>
                                </Space>
                              </div>
                              {renderConfidenceBadge(item)}
                            </div>
                            <div style={{ fontSize: 12, color: '#666', marginBottom: 6 }}>
                              <span style={{ color: '#8c8c8c' }}>匹配原因：</span>{item.matchReason}
                            </div>
                            <div style={{ fontSize: 12, color: '#595959', lineHeight: 1.6, marginBottom: 8 }}>
                              {item.solution ? item.solution.slice(0, 80) + '...' : '查看详情了解解决方案'}
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                              <Space size={8}>
                                <Button
                                  size="small"
                                  type={isLiked ? 'primary' : 'text'}
                                  icon={isLiked ? <CheckCircleOutlined /> : <CheckCircleOutlined />}
                                  onClick={() => handleQuickFeedback(item, 1)}
                                  style={{ padding: '0 6px', color: isLiked ? '#52c41a' : '#8c8c8c' }}
                                >
                                  有用 {item.likeCount || 0}
                                </Button>
                                <Button
                                  size="small"
                                  type={isDisliked ? 'primary' : 'text'}
                                  danger={isDisliked}
                                  onClick={() => handleQuickFeedback(item, 2)}
                                  style={{ padding: '0 6px', color: isDisliked ? '#ff4d4f' : '#8c8c8c' }}
                                >
                                  无用 {item.dislikeCount || 0}
                                </Button>
                                <span style={{ fontSize: 12, color: '#bfbfbf' }}>
                                  📋 {item.useCount || 0}人使用
                                </span>
                              </Space>
                              <Space>
                                <Button size="small" onClick={() => {
                                  setRecommendParams({
                                    faultCode: item.faultCode || '',
                                    faultName: item.faultName || '',
                                    description: '',
                                    faultLevel: item.faultLevel || null,
                                    stationId: null,
                                    inverterId: null
                                  })
                                  setRecommendVisible(true)
                                }}>
                                  详情
                                </Button>
                                <Button
                                  size="small"
                                  type="primary"
                                  onClick={() => handleApplyQuickRecommend(item)}
                                >
                                  应用方案
                                </Button>
                              </Space>
                            </div>
                          </div>
                        </List.Item>
                      )
                    }}
                  />
                </div>
              )}

              {!autoRecommendLoading && autoRecommendResults.length === 0 && (
                addForm.getFieldValue('faultCode') ? (
                  <div style={{ textAlign: 'center', padding: '16px 8px', color: '#999', fontSize: 12 }}>
                    <Empty
                      image={Empty.PRESENTED_IMAGE_SIMPLE}
                      description="暂未匹配到相似案例"
                      style={{ margin: 0 }}
                    />
                  </div>
                ) : (
                  <div style={{ textAlign: 'center', padding: '16px 8px', color: '#999', fontSize: 12 }}>
                    💡 输入故障代码或问题描述后，将自动推荐相似故障解决方案
                  </div>
                )
              )}
            </div>
          </Form.Item>
          <Form.Item
            name="description"
            label="问题描述"
            rules={[{ required: true, message: '请输入问题描述' }]}
            tooltip="推荐方案将自动填充到此处"
          >
            <TextArea
              rows={5}
              placeholder="请详细描述故障现象、发生时间、影响范围等...\n\n💡 提示：点击上方「智能推荐」按钮，系统将自动匹配相似案例的解决方案"
            />
          </Form.Item>
          <Form.Item name="expectHours" label="预计完成时间(小时)">
            <InputNumber placeholder="如: 24" style={{ width: '100%' }} min={1} />
          </Form.Item>
        </Form>
      </Modal>

      <KnowledgeRecommendModal
        visible={recommendVisible}
        onCancel={() => setRecommendVisible(false)}
        faultCode={recommendParams.faultCode}
        faultName={recommendParams.faultName}
        description={recommendParams.description}
        faultLevel={recommendParams.faultLevel}
        stationId={recommendParams.stationId}
        inverterId={recommendParams.inverterId}
        onSelectSolution={handleSelectSolution}
      />
    </div>
  )
}

export default WorkOrderList
