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
  InputNumber
} from 'antd'
import {
  PlusOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  WarningOutlined,
  ThunderboltOutlined,
  SafetyCertificateOutlined
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
import { getUser } from '../../utils/auth'

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
  const [currentOrder, setCurrentOrder] = useState(null)
  const [actionType, setActionType] = useState('')
  const [actionLoading, setActionLoading] = useState(false)
  const [addLoading, setAddLoading] = useState(false)
  const [addForm] = Form.useForm()
  const [actionForm] = Form.useForm()

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
    setAddVisible(true)
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
        title="新建工单"
        open={addVisible}
        onOk={handleAddOk}
        onCancel={() => setAddVisible(false)}
        confirmLoading={addLoading}
        okText="提交"
        cancelText="取消"
        width={500}
      >
        <Form form={addForm} layout="vertical">
          <Form.Item
            name="stationId"
            label="电站ID"
            rules={[{ required: true, message: '请输入电站ID' }]}
          >
            <InputNumber placeholder="请输入电站ID" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="inverterId"
            label="逆变器ID"
          >
            <InputNumber placeholder="请输入逆变器ID" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="faultCode"
            label="故障代码"
            rules={[{ required: true, message: '请输入故障代码' }]}
          >
            <Input placeholder="请输入故障代码" />
          </Form.Item>
          <Form.Item
            name="description"
            label="问题描述"
            rules={[{ required: true, message: '请输入问题描述' }]}
          >
            <TextArea rows={4} placeholder="请详细描述问题" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default WorkOrderList
