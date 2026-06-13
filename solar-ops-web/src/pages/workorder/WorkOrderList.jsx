import React, { useState } from 'react'
import {
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  message,
  Card,
  Tag,
  Tabs,
  Descriptions,
  Timeline,
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
  WarningOutlined
} from '@ant-design/icons'
import StatusTag from '../../components/StatusTag'

const { TextArea } = Input
const { Option } = Select

const WorkOrderList = () => {
  const [data, setData] = useState([
    { id: 'WO20240101001', title: '一号电站逆变器故障', type: 'fault', level: 'high', station: '一号光伏电站', device: 'INV-003', status: 'pending', creator: '张三', createTime: '2024-01-15 09:30:00', description: '逆变器突然停机，无输出功率' },
    { id: 'WO20240102002', title: '二号电站日常巡检', type: 'inspection', level: 'low', station: '二号光伏电站', device: '-', status: 'processing', creator: '李四', createTime: '2024-01-16 08:00:00', description: '按计划进行日常巡检维护' },
    { id: 'WO20240103003', title: '三号电站组件清洗', type: 'maintenance', level: 'medium', station: '三号光伏电站', device: '-', status: 'processing', creator: '王五', createTime: '2024-01-16 10:00:00', description: '光伏组件表面积灰严重，需要清洗' },
    { id: 'WO20240104004', title: '四号电站通讯故障', type: 'fault', level: 'medium', station: '四号光伏电站', device: 'INV-007', status: 'completed', creator: '赵六', createTime: '2024-01-14 14:20:00', description: '设备通讯中断，数据无法上传' },
    { id: 'WO20240105005', title: '五号电站温度告警', type: 'warning', level: 'low', station: '五号光伏电站', device: 'INV-006', status: 'closed', creator: '系统', createTime: '2024-01-13 16:45:00', description: '逆变器温度过高，触发告警' }
  ])
  const [detailVisible, setDetailVisible] = useState(false)
  const [addVisible, setAddVisible] = useState(false)
  const [currentOrder, setCurrentOrder] = useState(null)
  const [activeTab, setActiveTab] = useState('all')
  const [form] = Form.useForm()

  const workOrderLogs = [
    { time: '2024-01-15 09:30:00', user: '系统', action: '创建工单', description: '逆变器故障自动触发工单' },
    { time: '2024-01-15 09:35:00', user: '张三', action: '受理工单', description: '已收到工单，准备前往现场' },
    { time: '2024-01-15 10:00:00', user: '张三', action: '现场检查', description: '到达现场，开始检查设备' },
    { time: '2024-01-15 11:30:00', user: '张三', action: '故障定位', description: '确认IGBT模块损坏，需更换' }
  ]

  const columns = [
    {
      title: '工单编号',
      dataIndex: 'id',
      key: 'id',
      width: 140,
      render: (text) => <a>{text}</a>
    },
    {
      title: '标题',
      dataIndex: 'title',
      key: 'title',
      width: 200,
      ellipsis: true
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 100,
      render: (type) => {
        const typeMap = {
          fault: { color: 'red', text: '故障' },
          inspection: { color: 'blue', text: '巡检' },
          maintenance: { color: 'green', text: '维护' },
          warning: { color: 'orange', text: '告警' }
        }
        const info = typeMap[type] || { color: 'default', text: type }
        return <Tag color={info.color}>{info.text}</Tag>
      }
    },
    {
      title: '优先级',
      dataIndex: 'level',
      key: 'level',
      width: 100,
      render: (level) => {
        const levelMap = {
          high: { color: 'red', text: '高', icon: <WarningOutlined /> },
          medium: { color: 'orange', text: '中' },
          low: { color: 'green', text: '低' }
        }
        const info = levelMap[level] || { color: 'default', text: level }
        return <Tag color={info.color}>{info.icon}{info.text}</Tag>
      }
    },
    {
      title: '所属电站',
      dataIndex: 'station',
      key: 'station',
      width: 130
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => <StatusTag status={status} />
    },
    {
      title: '创建人',
      dataIndex: 'creator',
      key: 'creator',
      width: 80
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 160
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record)}
          >
            详情
          </Button>
          {record.status === 'pending' && (
            <Button
              type="link"
              size="small"
              onClick={() => handleStatusChange(record, 'processing')}
            >
              受理
            </Button>
          )}
          {record.status === 'processing' && (
            <Button
              type="link"
              size="small"
              onClick={() => handleStatusChange(record, 'completed')}
            >
              完成
            </Button>
          )}
          {record.status === 'completed' && (
            <Button
              type="link"
              size="small"
              onClick={() => handleStatusChange(record, 'closed')}
            >
              关闭
            </Button>
          )}
        </Space>
      )
    }
  ]

  const handleViewDetail = (record) => {
    setCurrentOrder(record)
    setDetailVisible(true)
  }

  const handleStatusChange = (record, newStatus) => {
    Modal.confirm({
      title: '确认操作',
      content: `确定将工单 ${record.id} 状态变更为：${newStatus === 'processing' ? '处理中' : newStatus === 'completed' ? '已完成' : '已关闭'}？`,
      onOk: () => {
        setData(prev => prev.map(item =>
          item.id === record.id ? { ...item, status: newStatus } : item
        ))
        message.success('操作成功')
      }
    })
  }

  const handleAdd = () => {
    form.resetFields()
    setAddVisible(true)
  }

  const handleAddOk = async () => {
    try {
      const values = await form.validateFields()
      const newOrder = {
        ...values,
        id: `WO${Date.now()}`,
        status: 'pending',
        creator: '当前用户',
        createTime: new Date().toLocaleString()
      }
      setData(prev => [newOrder, ...prev])
      setAddVisible(false)
      message.success('工单创建成功')
    } catch (error) {
      console.error('表单验证失败:', error)
    }
  }

  const filteredData = activeTab === 'all'
    ? data
    : data.filter(item => item.status === activeTab)

  const statistics = {
    total: data.length,
    pending: data.filter(i => i.status === 'pending').length,
    processing: data.filter(i => i.status === 'processing').length,
    completed: data.filter(i => i.status === 'completed').length
  }

  const tabItems = [
    { key: 'all', label: `全部 (${statistics.total})` },
    { key: 'pending', label: `待处理 (${statistics.pending})` },
    { key: 'processing', label: `处理中 (${statistics.processing})` },
    { key: 'completed', label: `已完成 (${statistics.completed})` },
    { key: 'closed', label: `已关闭 (${data.filter(i => i.status === 'closed').length})` }
  ]

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
          <Col xs={12} sm={6}>
            <Card size="small">
              <Statistic
                title="全部工单"
                value={statistics.total}
                prefix={<ClockCircleOutlined />}
                valueStyle={{ fontSize: 20 }}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6}>
            <Card size="small">
              <Statistic
                title="待处理"
                value={statistics.pending}
                valueStyle={{ color: '#faad14', fontSize: 20 }}
                prefix={<WarningOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6}>
            <Card size="small">
              <Statistic
                title="处理中"
                value={statistics.processing}
                valueStyle={{ color: '#1890ff', fontSize: 20 }}
                prefix={<ClockCircleOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6}>
            <Card size="small">
              <Statistic
                title="已完成"
                value={statistics.completed}
                valueStyle={{ color: '#52c41a', fontSize: 20 }}
                prefix={<CheckCircleOutlined />}
              />
            </Card>
          </Col>
        </Row>

        <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} />

        <Table
          columns={columns}
          dataSource={filteredData}
          rowKey="id"
          pagination={{ pageSize: 10 }}
          scroll={{ x: 1100 }}
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
          <div className="workorder-detail">
            <Descriptions title="基本信息" bordered column={2} size="small">
              <Descriptions.Item label="工单编号">{currentOrder.id}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <StatusTag status={currentOrder.status} />
              </Descriptions.Item>
              <Descriptions.Item label="标题" span={2}>{currentOrder.title}</Descriptions.Item>
              <Descriptions.Item label="类型">
                <Tag>{currentOrder.type}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="优先级">
                <Tag color={currentOrder.level === 'high' ? 'red' : currentOrder.level === 'medium' ? 'orange' : 'green'}>
                  {currentOrder.level}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="所属电站">{currentOrder.station}</Descriptions.Item>
              <Descriptions.Item label="关联设备">{currentOrder.device}</Descriptions.Item>
              <Descriptions.Item label="创建人">{currentOrder.creator}</Descriptions.Item>
              <Descriptions.Item label="创建时间">{currentOrder.createTime}</Descriptions.Item>
              <Descriptions.Item label="问题描述" span={2}>
                {currentOrder.description}
              </Descriptions.Item>
            </Descriptions>

            <div style={{ marginTop: 24 }}>
              <h4>处理记录</h4>
              <Timeline
                items={workOrderLogs.map(log => ({
                  color: 'blue',
                  children: (
                    <div>
                      <p style={{ margin: 0, fontWeight: 500 }}>{log.action}</p>
                      <p style={{ margin: '4px 0', color: '#666', fontSize: 12 }}>{log.user} · {log.time}</p>
                      <p style={{ margin: 0 }}>{log.description}</p>
                    </div>
                  )
                }))}
              />
            </div>
          </div>
        )}
      </Modal>

      <Modal
        title="新建工单"
        open={addVisible}
        onOk={handleAddOk}
        onCancel={() => setAddVisible(false)}
        okText="提交"
        cancelText="取消"
        width={500}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="title"
            label="工单标题"
            rules={[{ required: true, message: '请输入工单标题' }]}
          >
            <Input placeholder="请输入工单标题" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="type"
                label="工单类型"
                rules={[{ required: true, message: '请选择工单类型' }]}
              >
                <Select placeholder="请选择">
                  <Option value="fault">故障</Option>
                  <Option value="inspection">巡检</Option>
                  <Option value="maintenance">维护</Option>
                  <Option value="warning">告警</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="level"
                label="优先级"
                rules={[{ required: true, message: '请选择优先级' }]}
              >
                <Select placeholder="请选择">
                  <Option value="high">高</Option>
                  <Option value="medium">中</Option>
                  <Option value="low">低</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Form.Item
            name="station"
            label="所属电站"
            rules={[{ required: true, message: '请选择电站' }]}
          >
            <Select placeholder="请选择电站">
              <Option value="一号光伏电站">一号光伏电站</Option>
              <Option value="二号光伏电站">二号光伏电站</Option>
              <Option value="三号光伏电站">三号光伏电站</Option>
              <Option value="四号光伏电站">四号光伏电站</Option>
              <Option value="五号光伏电站">五号光伏电站</Option>
            </Select>
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
