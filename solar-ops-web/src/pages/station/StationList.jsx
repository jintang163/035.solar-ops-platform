import React, { useState } from 'react'
import {
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  InputNumber,
  Select,
  message,
  Popconfirm,
  Card,
  Tag
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons'

const { Search } = Input
const { Option } = Select

const StationList = () => {
  const [data, setData] = useState([
    { id: 1, name: '一号光伏电站', capacity: 5000, location: '北京市朝阳区', status: 'running', inverterCount: 12, createdAt: '2023-01-15' },
    { id: 2, name: '二号光伏电站', capacity: 8000, location: '北京市海淀区', status: 'running', inverterCount: 18, createdAt: '2023-03-20' },
    { id: 3, name: '三号光伏电站', capacity: 3000, location: '北京市丰台区', status: 'maintenance', inverterCount: 8, createdAt: '2023-05-10' },
    { id: 4, name: '四号光伏电站', capacity: 10000, location: '北京市通州区', status: 'running', inverterCount: 25, createdAt: '2023-06-01' },
    { id: 5, name: '五号光伏电站', capacity: 6000, location: '北京市顺义区', status: 'fault', inverterCount: 15, createdAt: '2023-08-15' }
  ])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState(null)
  const [form] = Form.useForm()
  const [searchText, setSearchText] = useState('')

  const columns = [
    {
      title: '电站名称',
      dataIndex: 'name',
      key: 'name',
      width: 150
    },
    {
      title: '容量(kW)',
      dataIndex: 'capacity',
      key: 'capacity',
      width: 120,
      sorter: (a, b) => a.capacity - b.capacity
    },
    {
      title: '位置',
      dataIndex: 'location',
      key: 'location',
      width: 180
    },
    {
      title: '逆变器数量',
      dataIndex: 'inverterCount',
      key: 'inverterCount',
      width: 120
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => {
        const statusMap = {
          running: { color: 'green', text: '运行中' },
          maintenance: { color: 'orange', text: '维护中' },
          fault: { color: 'red', text: '故障' },
          stopped: { color: 'gray', text: '停运' }
        }
        const info = statusMap[status] || { color: 'default', text: status }
        return <Tag color={info.color}>{info.text}</Tag>
      }
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 120
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除该电站？"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  const handleAdd = () => {
    setEditingItem(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record) => {
    setEditingItem(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleDelete = (id) => {
    setData(prev => prev.filter(item => item.id !== id))
    message.success('删除成功')
  }

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields()
      if (editingItem) {
        setData(prev => prev.map(item =>
          item.id === editingItem.id ? { ...item, ...values } : item
        ))
        message.success('编辑成功')
      } else {
        const newItem = {
          ...values,
          id: Date.now(),
          inverterCount: 0,
          createdAt: new Date().toISOString().split('T')[0]
        }
        setData(prev => [...prev, newItem])
        message.success('新增成功')
      }
      setModalVisible(false)
    } catch (error) {
      console.error('表单验证失败:', error)
    }
  }

  const filteredData = data.filter(item =>
    item.name.includes(searchText) || item.location.includes(searchText)
  )

  return (
    <div className="station-list-page">
      <Card
        title="电站管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新增电站
          </Button>
        }
      >
        <Space style={{ marginBottom: 16 }}>
          <Search
            placeholder="搜索电站名称或位置"
            allowClear
            style={{ width: 280 }}
            onSearch={setSearchText}
            enterButton={<SearchOutlined />}
          />
        </Space>
        <Table
          columns={columns}
          dataSource={filteredData}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
          scroll={{ x: 900 }}
        />
      </Card>

      <Modal
        title={editingItem ? '编辑电站' : '新增电站'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        okText="确定"
        cancelText="取消"
        width={500}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="电站名称"
            rules={[{ required: true, message: '请输入电站名称' }]}
          >
            <Input placeholder="请输入电站名称" />
          </Form.Item>
          <Form.Item
            name="capacity"
            label="容量(kW)"
            rules={[{ required: true, message: '请输入容量' }]}
          >
            <InputNumber style={{ width: '100%' }} min={0} placeholder="请输入容量" />
          </Form.Item>
          <Form.Item
            name="location"
            label="位置"
            rules={[{ required: true, message: '请输入位置' }]}
          >
            <Input placeholder="请输入位置" />
          </Form.Item>
          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select placeholder="请选择状态">
              <Option value="running">运行中</Option>
              <Option value="maintenance">维护中</Option>
              <Option value="fault">故障</Option>
              <Option value="stopped">停运</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default StationList
