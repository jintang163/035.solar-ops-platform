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
  Popconfirm,
  Switch
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, LockOutlined } from '@ant-design/icons'

const { Option } = Select

const UserManagement = () => {
  const [data, setData] = useState([
    { id: 1, username: 'admin', name: '管理员', role: 'admin', phone: '13800138000', email: 'admin@solar.com', status: true, createTime: '2023-01-01' },
    { id: 2, username: 'zhangsan', name: '张三', role: 'operator', phone: '13800138001', email: 'zhangsan@solar.com', status: true, createTime: '2023-03-15' },
    { id: 3, username: 'lisi', name: '李四', role: 'operator', phone: '13800138002', email: 'lisi@solar.com', status: true, createTime: '2023-05-20' },
    { id: 4, username: 'wangwu', name: '王五', role: 'viewer', phone: '13800138003', email: 'wangwu@solar.com', status: false, createTime: '2023-08-10' },
    { id: 5, username: 'zhaoliu', name: '赵六', role: 'operator', phone: '13800138004', email: 'zhaoliu@solar.com', status: true, createTime: '2023-10-01' }
  ])
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState(null)
  const [form] = Form.useForm()
  const [passwordModalVisible, setPasswordModalVisible] = useState(false)
  const [passwordForm] = Form.useForm()

  const roleMap = {
    admin: { color: 'red', text: '超级管理员' },
    operator: { color: 'blue', text: '运维人员' },
    viewer: { color: 'green', text: '查看人员' }
  }

  const columns = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      width: 120
    },
    {
      title: '姓名',
      dataIndex: 'name',
      key: 'name',
      width: 100
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      width: 120,
      render: (role) => {
        const info = roleMap[role] || { color: 'default', text: role }
        return <Tag color={info.color}>{info.text}</Tag>
      }
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
      width: 130
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
      width: 180
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status, record) => (
        <Switch
          checked={status}
          onChange={(checked) => handleStatusChange(record.id, checked)}
        />
      )
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 120
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
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
          <Button
            type="link"
            size="small"
            icon={<LockOutlined />}
            onClick={() => handleResetPassword(record)}
          >
            重置密码
          </Button>
          <Popconfirm
            title="确定删除该用户？"
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

  const handleStatusChange = (id, status) => {
    setData(prev => prev.map(item =>
      item.id === id ? { ...item, status } : item
    ))
    message.success(status ? '已启用' : '已禁用')
  }

  const handleResetPassword = (record) => {
    setEditingItem(record)
    passwordForm.resetFields()
    setPasswordModalVisible(true)
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
          status: true,
          createTime: new Date().toISOString().split('T')[0]
        }
        setData(prev => [...prev, newItem])
        message.success('新增成功')
      }
      setModalVisible(false)
    } catch (error) {
      console.error('表单验证失败:', error)
    }
  }

  const handlePasswordOk = async () => {
    try {
      await passwordForm.validateFields()
      message.success('密码重置成功')
      setPasswordModalVisible(false)
    } catch (error) {
      console.error('表单验证失败:', error)
    }
  }

  return (
    <div className="user-management-page">
      <Card
        title="用户管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新增用户
          </Button>
        }
      >
        <Table
          columns={columns}
          dataSource={data}
          rowKey="id"
          pagination={{ pageSize: 10 }}
          scroll={{ x: 900 }}
        />
      </Card>

      <Modal
        title={editingItem ? '编辑用户' : '新增用户'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        okText="确定"
        cancelText="取消"
        width={500}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="username"
            label="用户名"
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 3, max: 20, message: '用户名长度在3到20个字符' }
            ]}
          >
            <Input placeholder="请输入用户名" disabled={!!editingItem} />
          </Form.Item>
          <Form.Item
            name="name"
            label="姓名"
            rules={[{ required: true, message: '请输入姓名' }]}
          >
            <Input placeholder="请输入姓名" />
          </Form.Item>
          <Form.Item
            name="role"
            label="角色"
            rules={[{ required: true, message: '请选择角色' }]}
          >
            <Select placeholder="请选择角色">
              <Option value="admin">超级管理员</Option>
              <Option value="operator">运维人员</Option>
              <Option value="viewer">查看人员</Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="phone"
            label="手机号"
            rules={[
              { required: true, message: '请输入手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' }
            ]}
          >
            <Input placeholder="请输入手机号" />
          </Form.Item>
          <Form.Item
            name="email"
            label="邮箱"
            rules={[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '请输入正确的邮箱格式' }
            ]}
          >
            <Input placeholder="请输入邮箱" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="重置密码"
        open={passwordModalVisible}
        onOk={handlePasswordOk}
        onCancel={() => setPasswordModalVisible(false)}
        okText="确定"
        cancelText="取消"
        width={400}
      >
        <Form form={passwordForm} layout="vertical">
          <Form.Item
            name="password"
            label="新密码"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 6, max: 20, message: '密码长度在6到20个字符' }
            ]}
          >
            <Input.Password placeholder="请输入新密码" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="确认密码"
            dependencies={['password']}
            rules={[
              { required: true, message: '请确认密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('两次输入的密码不一致'))
                }
              })
            ]}
          >
            <Input.Password placeholder="请再次输入密码" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default UserManagement
