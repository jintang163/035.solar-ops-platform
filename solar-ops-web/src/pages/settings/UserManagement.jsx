import React, { useState, useEffect, useRef } from 'react'
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
  Switch,
  Drawer,
  Checkbox,
  Row,
  Col,
  Typography,
  Divider,
  Alert
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  LockOutlined,
  SafetyOutlined,
  EnvironmentOutlined,
  TeamOutlined
} from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import dayjs from 'dayjs'
import {
  getUserList,
  addUser,
  updateUser,
  deleteUser
} from '../../api/users'
import {
  getUserStations,
  assignUserStations
} from '../../api/workspace'
import { getStationList } from '../../api/station'

const { Option } = Select
const { Title, Text } = Typography

const ROLE_OPTIONS = [
  { value: 'admin', label: '超级管理员', color: 'red' },
  { value: 'ops', label: '运维人员', color: 'blue' },
  { value: 'user', label: '普通用户', color: 'green' }
]

const ORG_TYPE_OPTIONS = [
  { value: 1, label: '集团总部', color: 'purple' },
  { value: 2, label: '区域公司', color: 'blue' },
  { value: 3, label: '电站', color: 'green' }
]

const DATA_SCOPE_OPTIONS = [
  { value: 1, label: '全部数据' },
  { value: 2, label: '本组织及以下' },
  { value: 3, label: '仅本人数据' }
]

const PERMISSION_TYPE_OPTIONS = [
  { value: 1, label: '只读' },
  { value: 2, label: '读写' },
  { value: 3, label: '管理' }
]

const UserManagement = () => {
  const actionRef = useRef()
  const [form] = Form.useForm()
  const [passwordForm] = Form.useForm()
  const [assignForm] = Form.useForm()

  const [modalVisible, setModalVisible] = useState(false)
  const [passwordModalVisible, setPasswordModalVisible] = useState(false)
  const [assignVisible, setAssignVisible] = useState(false)
  const [editingItem, setEditingItem] = useState(null)
  const [loading, setLoading] = useState(false)

  const [allStations, setAllStations] = useState([])
  const [userStations, setUserStations] = useState([])
  const [checkedStations, setCheckedStations] = useState([])

  useEffect(() => {
    loadAllStations()
  }, [])

  const loadAllStations = async () => {
    try {
      const res = await getStationList({ pageNum: 1, pageSize: 1000 })
      if (res.data?.list) {
        setAllStations(res.data.list)
      }
    } catch (e) {
      console.error('加载电站列表失败', e)
    }
  }

  const roleMap = {
    admin: { color: 'red', text: '超级管理员' },
    ops: { color: 'blue', text: '运维人员' },
    user: { color: 'green', text: '普通用户' }
  }

  const dataScopeMap = {
    1: { color: 'purple', text: '全部数据' },
    2: { color: 'blue', text: '本组织及以下' },
    3: { color: 'orange', text: '仅本人数据' }
  }

  const columns = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      width: 120
    },
    {
      title: '昵称',
      dataIndex: 'nickname',
      key: 'nickname',
      width: 120
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
      title: '管理员',
      dataIndex: 'isAdmin',
      key: 'isAdmin',
      width: 100,
      render: (val) => val === 1
        ? <Tag color="red" icon={<SafetyOutlined />}>超级管理员</Tag>
        : <Tag color="default">普通用户</Tag>
    },
    {
      title: '数据权限',
      dataIndex: 'dataScope',
      key: 'dataScope',
      width: 120,
      render: (scope) => {
        const info = dataScopeMap[scope] || { color: 'default', text: scope }
        return <Tag color={info.color}>{info.text}</Tag>
      }
    },
    {
      title: '电站权限',
      key: 'stationCount',
      width: 100,
      render: (_, record) => (
        <Tag color="blue" icon={<EnvironmentOutlined />}>
          {record.stationCount || 0} 个电站
        </Tag>
      )
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
          checked={status === 1}
          onChange={(checked) => handleStatusChange(record.id, checked)}
        />
      )
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 170,
      render: (val) => val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      fixed: 'right',
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
            icon={<EnvironmentOutlined />}
            onClick={() => handleAssignStation(record)}
          >
            电站权限
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
    form.setFieldsValue({
      ...record,
      name: record.nickname
    })
    setModalVisible(true)
  }

  const handleDelete = async (id) => {
    try {
      await deleteUser(id)
      message.success('删除成功')
      actionRef.current?.reload()
    } catch (e) {
      console.error('删除失败', e)
    }
  }

  const handleStatusChange = async (id, checked) => {
    try {
      await updateUser({ id, status: checked ? 1 : 0 })
      message.success(checked ? '已启用' : '已禁用')
    } catch (e) {
      message.error('操作失败')
      actionRef.current?.reload()
    }
  }

  const handleResetPassword = (record) => {
    setEditingItem(record)
    passwordForm.resetFields()
    setPasswordModalVisible(true)
  }

  const handleAssignStation = async (record) => {
    setEditingItem(record)
    setAssignVisible(true)
    assignForm.resetFields()

    try {
      const res = await getUserStations(record.id)
      if (res.data) {
        setUserStations(res.data)
        setCheckedStations(res.data.map(s => s.stationId))
        assignForm.setFieldsValue({
          permissionType: res.data[0]?.permissionType || 2
        })
      }
    } catch (e) {
      console.error('加载用户电站权限失败', e)
    }
  }

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      const userData = {
        ...editingItem,
        ...values,
        nickname: values.name
      }

      if (editingItem) {
        await updateUser(userData)
        message.success('编辑成功')
      } else {
        await addUser(userData)
        message.success('新增成功')
      }
      setModalVisible(false)
      actionRef.current?.reload()
    } catch (error) {
      console.error('表单验证失败:', error)
    } finally {
      setLoading(false)
    }
  }

  const handlePasswordOk = async () => {
    try {
      const values = await passwordForm.validateFields()
      await updateUser({
        id: editingItem.id,
        password: values.password
      })
      message.success('密码重置成功')
      setPasswordModalVisible(false)
    } catch (error) {
      console.error('表单验证失败:', error)
    }
  }

  const handleAssignOk = async () => {
    if (checkedStations.length === 0) {
      Modal.confirm({
        title: '确认取消所有电站权限？',
        content: '未选择任何电站，该用户将无法访问任何电站数据，是否继续？',
        okText: '确认',
        cancelText: '取消',
        onOk: async () => {
          await doAssign()
        }
      })
      return
    }
    await doAssign()
  }

  const doAssign = async () => {
    try {
      const values = await assignForm.validateFields()
      setLoading(true)

      await assignUserStations({
        userId: editingItem.id,
        stationIds: checkedStations,
        permissionType: values.permissionType
      })

      message.success('电站权限分配成功')
      setAssignVisible(false)
      actionRef.current?.reload()
    } catch (error) {
      console.error('分配失败:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleCheckAll = (e) => {
    if (e.target.checked) {
      setCheckedStations(allStations.map(s => s.id))
    } else {
      setCheckedStations([])
    }
  }

  const handleCheckStation = (stationId, checked) => {
    if (checked) {
      setCheckedStations([...checkedStations, stationId])
    } else {
      setCheckedStations(checkedStations.filter(id => id !== stationId))
    }
  }

  const handleCheckOrg = (orgId, checked) => {
    const orgStations = allStations.filter(s => s.orgId === orgId).map(s => s.id)
    if (checked) {
      setCheckedStations([...new Set([...checkedStations, ...orgStations])])
    } else {
      setCheckedStations(checkedStations.filter(id => !orgStations.includes(id)))
    }
  }

  const getOrgStations = (orgId) => {
    return allStations.filter(s => s.orgId === orgId)
  }

  const getOrgGroups = () => {
    const groups = {}
    allStations.forEach(station => {
      const orgId = station.orgId || 0
      if (!groups[orgId]) {
        groups[orgId] = {
          orgId,
          orgName: station.orgName || '未分组',
          stations: []
        }
      }
      groups[orgId].stations.push(station)
    })
    return Object.values(groups)
  }

  const orgGroups = getOrgGroups()
  const indeterminate = checkedStations.length > 0 && checkedStations.length < allStations.length
  const checkAll = checkedStations.length === allStations.length && allStations.length > 0

  return (
    <div className="user-management-page" style={{ padding: 16 }}>
      <Card>
        <ProTable
          rowKey="id"
          actionRef={actionRef}
          columns={columns}
          request={async (params) => {
            const queryParams = {
              pageNum: params.current,
              pageSize: params.pageSize,
              keyword: params.keyword,
              role: params.role,
              status: params.status
            }
            const res = await getUserList(queryParams)
            return {
              data: res.data?.list || [],
              success: true,
              total: res.data?.total || 0
            }
          }}
          headerTitle="用户管理"
          toolBarRender={() => [
            <Button
              key="add"
              type="primary"
              icon={<PlusOutlined />}
              onClick={handleAdd}
            >
              新增用户
            </Button>
          ]}
          search={{
            labelWidth: 'auto',
            defaultCollapsed: false
          }}
          form={{
            syncToUrl: true
          }}
          pagination={{
            defaultPageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`
          }}
        />
      </Card>

      <Modal
        title={editingItem ? '编辑用户' : '新增用户'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        confirmLoading={loading}
        okText="确定"
        cancelText="取消"
        width={600}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
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
            </Col>
            <Col span={12}>
              <Form.Item
                name="name"
                label="昵称"
                rules={[{ required: true, message: '请输入昵称' }]}
              >
                <Input placeholder="请输入昵称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="role"
                label="角色"
                rules={[{ required: true, message: '请选择角色' }]}
                initialValue="user"
              >
                <Select placeholder="请选择角色">
                  {ROLE_OPTIONS.map(item => (
                    <Option key={item.value} value={item.value}>{item.label}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="dataScope"
                label="数据权限范围"
                rules={[{ required: true, message: '请选择数据权限' }]}
                initialValue={2}
              >
                <Select placeholder="请选择数据权限范围">
                  {DATA_SCOPE_OPTIONS.map(item => (
                    <Option key={item.value} value={item.value}>{item.label}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
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
            </Col>
            <Col span={12}>
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
            </Col>
            {!editingItem && (
              <Col span={24}>
                <Form.Item
                  name="password"
                  label="初始密码"
                  rules={[
                    { required: true, message: '请输入初始密码' },
                    { min: 6, max: 20, message: '密码长度在6到20个字符' }
                  ]}
                >
                  <Input.Password placeholder="请输入初始密码" />
                </Form.Item>
              </Col>
            )}
          </Row>
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

      <Drawer
        title={
          <Space>
            <TeamOutlined />
            <span>电站权限分配 - {editingItem?.nickname || editingItem?.username}</span>
          </Space>
        }
        width={720}
        open={assignVisible}
        onClose={() => setAssignVisible(false)}
        extra={
          <Space>
            <Button onClick={() => setAssignVisible(false)}>取消</Button>
            <Button type="primary" onClick={handleAssignOk} loading={loading}>
              保存
            </Button>
          </Space>
        }
      >
        {editingItem?.isAdmin === 1 ? (
          <Alert
            message="超级管理员拥有所有电站的访问权限"
            description="超级管理员角色默认可以访问所有电站数据，无需单独分配权限。"
            type="info"
            showIcon
          />
        ) : (
          <div>
            <Card size="small" style={{ marginBottom: 16 }}>
              <Row align="middle">
                <Col flex="none">
                  <Checkbox
                    indeterminate={indeterminate}
                    checked={checkAll}
                    onChange={handleCheckAll}
                  >
                    全选
                  </Checkbox>
                </Col>
                <Col flex="auto" style={{ textAlign: 'right' }}>
                  <Text type="secondary">
                    已选择 <Text strong style={{ color: '#1890ff' }}>{checkedStations.length}</Text> / {allStations.length} 个电站
                  </Text>
                </Col>
              </Row>
            </Card>

            <Form form={assignForm} layout="vertical">
              <Form.Item
                name="permissionType"
                label="默认权限类型"
                initialValue={2}
                help="为所选电站设置统一的权限类型"
              >
                <Select placeholder="请选择权限类型">
                  {PERMISSION_TYPE_OPTIONS.map(item => (
                    <Option key={item.value} value={item.value}>{item.label}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Form>

            <Divider />

            <div className="station-list">
              {orgGroups.map(group => {
                const groupChecked = getOrgStations(group.orgId).every(s => checkedStations.includes(s.id))
                const groupIndeterminate = getOrgStations(group.orgId).some(s => checkedStations.includes(s.id)) && !groupChecked

                return (
                  <Card
                    key={group.orgId}
                    size="small"
                    title={
                      <Space>
                        <Checkbox
                          indeterminate={groupIndeterminate}
                          checked={groupChecked}
                          onChange={(e) => handleCheckOrg(group.orgId, e.target.checked)}
                        />
                        <TeamOutlined style={{ color: '#1890ff' }} />
                        <span>{group.orgName}</span>
                        <Tag color="blue">{group.stations.length}个电站</Tag>
                      </Space>
                    }
                    style={{ marginBottom: 12 }}
                  >
                    <Row gutter={[8, 8]}>
                      {group.stations.map(station => (
                        <Col key={station.id} xs={24} sm={12} md={8}>
                          <Card
                            size="small"
                            className={checkedStations.includes(station.id) ? 'station-card checked' : 'station-card'}
                            style={{
                              borderColor: checkedStations.includes(station.id) ? '#1890ff' : '#f0f0f0',
                              cursor: 'pointer',
                              transition: 'all 0.2s'
                            }}
                            onClick={() => handleCheckStation(station.id, !checkedStations.includes(station.id))}
                          >
                            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                              <Checkbox checked={checkedStations.includes(station.id)} />
                              <div style={{ flex: 1, minWidth: 0 }}>
                                <div style={{
                                  fontWeight: 500,
                                  overflow: 'hidden',
                                  textOverflow: 'ellipsis',
                                  whiteSpace: 'nowrap'
                                }}>
                                  {station.stationName}
                                </div>
                                <div style={{ fontSize: 12, color: '#999', fontFamily: 'monospace' }}>
                                  {station.stationCode}
                                </div>
                                <div style={{ fontSize: 12, color: '#999' }}>
                                  装机: {station.capacity}kW
                                </div>
                              </div>
                            </div>
                          </Card>
                        </Col>
                      ))}
                    </Row>
                  </Card>
                )
              })}
            </div>
          </div>
        )}
      </Drawer>

      <style>{`
        .station-card:hover {
          border-color: #1890ff !important;
          box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
        }
        .station-card.checked {
          background: #f0f8ff;
        }
      `}</style>
    </div>
  )
}

export default UserManagement
