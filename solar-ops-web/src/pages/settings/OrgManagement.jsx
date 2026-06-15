import React, { useState, useEffect, useRef } from 'react'
import {
  Card,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  InputNumber,
  Switch,
  Tree,
  message,
  Popconfirm,
  Row,
  Col,
  Typography,
  Tag,
  Divider
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ApartmentOutlined,
  TeamOutlined,
  EnvironmentOutlined,
  PlusCircleOutlined,
  ReloadOutlined,
  SaveOutlined,
  InfoCircleOutlined
} from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import dayjs from 'dayjs'
import {
  getOrgList,
  getOrgTree,
  getOrgDetail,
  createOrg,
  updateOrg,
  deleteOrg
} from '../../api/workspace'

const { Option } = Select
const { Title, Text } = Typography

const ORG_TYPE_OPTIONS = [
  { value: 1, label: '集团总部', color: 'purple', icon: <ApartmentOutlined /> },
  { value: 2, label: '区域公司', color: 'blue', icon: <TeamOutlined /> },
  { value: 3, label: '电站', color: 'green', icon: <EnvironmentOutlined /> }
]

const OrgManagement = () => {
  const actionRef = useRef()
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState(null)
  const [orgTree, setOrgTree] = useState([])
  const [selectedOrgId, setSelectedOrgId] = useState(null)
  const [orgDetail, setOrgDetail] = useState(null)
  const [expandedKeys, setExpandedKeys] = useState([])
  const [selectedKeys, setSelectedKeys] = useState([])

  useEffect(() => {
    loadOrgTree()
  }, [])

  const loadOrgTree = async () => {
    try {
      const res = await getOrgTree()
      if (res.data) {
        setOrgTree(res.data)
        const defaultExpanded = res.data.map(item => item.key || item.id)
        setExpandedKeys(defaultExpanded)
      }
    } catch (e) {
      console.error('加载组织树失败', e)
    }
  }

  const handleSelect = (selectedKeys, info) => {
    setSelectedKeys(selectedKeys)
    if (selectedKeys.length > 0) {
      setSelectedOrgId(selectedKeys[0])
      loadOrgDetail(selectedKeys[0])
    } else {
      setSelectedOrgId(null)
      setOrgDetail(null)
    }
  }

  const loadOrgDetail = async (orgId) => {
    try {
      const res = await getOrgDetail(orgId)
      if (res.data) {
        setOrgDetail(res.data)
      }
    } catch (e) {
      console.error('加载组织详情失败', e)
    }
  }

  const handleAdd = (parentId = null) => {
    setEditingItem(null)
    form.resetFields()
    form.setFieldsValue({
      parentId,
      orgType: parentId ? 2 : 1,
      sortOrder: 0,
      status: 1
    })
    setModalVisible(true)
  }

  const handleEdit = (record) => {
    setEditingItem(record)
    form.setFieldsValue({
      ...record,
      name: record.orgName,
      type: record.orgType
    })
    setModalVisible(true)
  }

  const handleDelete = async (id) => {
    try {
      await deleteOrg(id)
      message.success('删除成功')
      loadOrgTree()
      if (selectedOrgId === id) {
        setSelectedOrgId(null)
        setOrgDetail(null)
        setSelectedKeys([])
      }
    } catch (e) {
      console.error('删除失败', e)
    }
  }

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      const orgData = {
        ...editingItem,
        ...values,
        orgName: values.name,
        orgType: values.type
      }

      if (editingItem) {
        await updateOrg(orgData)
        message.success('更新成功')
      } else {
        await createOrg(orgData)
        message.success('创建成功')
      }
      setModalVisible(false)
      loadOrgTree()
    } catch (error) {
      console.error('表单验证失败:', error)
    } finally {
      setLoading(false)
    }
  }

  const renderTreeNodes = (data) => {
    return data?.map(item => {
      const typeInfo = ORG_TYPE_OPTIONS.find(t => t.value === item.orgType) || ORG_TYPE_OPTIONS[0]
      return {
        title: (
          <Space>
            {typeInfo.icon}
            <span>{item.orgName}</span>
            <Tag color={typeInfo.color} style={{ marginLeft: 8 }}>
              {typeInfo.label}
            </Tag>
            {item.stationCount !== undefined && (
              <Tag color="blue">
                <EnvironmentOutlined /> {item.stationCount}个电站
              </Tag>
            )}
          </Space>
        ),
        key: item.id,
        children: item.children ? renderTreeNodes(item.children) : undefined,
        icon: typeInfo.icon
      }
    })
  }

  const columns = [
    {
      title: '组织编码',
      dataIndex: 'orgCode',
      key: 'orgCode',
      width: 120,
      render: (val) => <Text code>{val}</Text>
    },
    {
      title: '组织名称',
      dataIndex: 'orgName',
      key: 'orgName',
      width: 180,
      render: (val, record) => {
        const typeInfo = ORG_TYPE_OPTIONS.find(t => t.value === record.orgType)
        return (
          <Space>
            {typeInfo?.icon}
            <span>{val}</span>
          </Space>
        )
      }
    },
    {
      title: '组织类型',
      dataIndex: 'orgType',
      key: 'orgType',
      width: 100,
      render: (val) => {
        const typeInfo = ORG_TYPE_OPTIONS.find(t => t.value === val)
        return <Tag color={typeInfo?.color}>{typeInfo?.label}</Tag>
      }
    },
    {
      title: '负责人',
      dataIndex: 'leaderName',
      key: 'leaderName',
      width: 100
    },
    {
      title: '电站数量',
      dataIndex: 'stationCount',
      key: 'stationCount',
      width: 100,
      render: (val) => (
        <Tag color="blue" icon={<EnvironmentOutlined />}>
          {val || 0} 个
        </Tag>
      )
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      width: 80
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (val) => (
        <Tag color={val === 1 ? 'green' : 'default'}>
          {val === 1 ? '启用' : '禁用'}
        </Tag>
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
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<PlusCircleOutlined />}
            onClick={() => handleAdd(record.id)}
            disabled={record.orgType === 3}
          >
            添加子组织
          </Button>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除该组织？"
            description={record.children?.length > 0 || record.stationCount > 0
              ? '删除将同时删除所有子组织和关联数据，请谨慎操作！'
              : '删除后无法恢复，请确认操作。'}
            onConfirm={() => handleDelete(record.id)}
            okText="确认删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  const treeData = renderTreeNodes(orgTree)

  return (
    <div className="org-management-page" style={{ padding: 16 }}>
      <Row gutter={[16, 16]}>
        <Col xs={24} md={6} lg={5}>
          <Card
            title={
              <Space>
                <ApartmentOutlined style={{ color: '#1890ff' }} />
                <span>组织架构树</span>
              </Space>
            }
            extra={
              <Button
                type="text"
                icon={<ReloadOutlined />}
                onClick={loadOrgTree}
              />
            }
          >
            <div style={{ marginBottom: 12 }}>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => handleAdd(null)}
                block
              >
                新增顶级组织
              </Button>
            </div>
            <Tree
              showLine={{ showLeafIcon: false }}
              showIcon
              treeData={treeData}
              expandedKeys={expandedKeys}
              selectedKeys={selectedKeys}
              onExpand={setExpandedKeys}
              onSelect={handleSelect}
              defaultExpandAll
            />
          </Card>

          {orgDetail && (
            <Card
              title={
                <Space>
                  <InfoCircleOutlined style={{ color: '#1890ff' }} />
                  <span>组织详情</span>
                </Space>
              }
              style={{ marginTop: 16 }}
              size="small"
            >
              <div style={{ marginBottom: 12 }}>
                <Text type="secondary">组织名称</Text>
                <div style={{ fontSize: 16, fontWeight: 500 }}>
                  {orgDetail.orgName}
                </div>
              </div>
              <Divider style={{ margin: '8px 0' }} />
              <Row gutter={[8, 8]}>
                <Col span={12}>
                  <Text type="secondary" style={{ fontSize: 12 }}>组织编码</Text>
                  <div><Text code>{orgDetail.orgCode}</Text></div>
                </Col>
                <Col span={12}>
                  <Text type="secondary" style={{ fontSize: 12 }}>组织类型</Text>
                  <div>
                    <Tag color={ORG_TYPE_OPTIONS.find(t => t.value === orgDetail.orgType)?.color}>
                      {ORG_TYPE_OPTIONS.find(t => t.value === orgDetail.orgType)?.label}
                    </Tag>
                  </div>
                </Col>
                <Col span={12}>
                  <Text type="secondary" style={{ fontSize: 12 }}>负责人</Text>
                  <div>{orgDetail.leaderName || '-'}</div>
                </Col>
                <Col span={12}>
                  <Text type="secondary" style={{ fontSize: 12 }}>电站数量</Text>
                  <div><Tag color="blue">{orgDetail.stationCount || 0} 个</Tag></div>
                </Col>
              </Row>
              {orgDetail.remark && (
                <>
                  <Divider style={{ margin: '8px 0' }} />
                  <div>
                    <Text type="secondary" style={{ fontSize: 12 }}>备注</Text>
                    <div style={{ fontSize: 13 }}>{orgDetail.remark}</div>
                  </div>
                </>
              )}
              <Divider style={{ margin: '12px 0' }} />
              <Space>
                <Button
                  size="small"
                  icon={<EditOutlined />}
                  onClick={() => handleEdit(orgDetail)}
                >
                  编辑
                </Button>
                <Button
                  size="small"
                  icon={<PlusCircleOutlined />}
                  onClick={() => handleAdd(orgDetail.id)}
                  disabled={orgDetail.orgType === 3}
                >
                  添加下级
                </Button>
              </Space>
            </Card>
          )}
        </Col>

        <Col xs={24} md={18} lg={19}>
          <Card>
            <ProTable
              rowKey="id"
              actionRef={actionRef}
              columns={columns}
              request={async (params) => {
                const queryParams = {
                  pageNum: params.current,
                  pageSize: params.pageSize,
                  parentId: selectedOrgId,
                  keyword: params.keyword,
                  orgType: params.orgType,
                  status: params.status
                }
                const res = await getOrgList(queryParams)
                return {
                  data: res.data?.list || [],
                  success: true,
                  total: res.data?.total || 0
                }
              }}
              headerTitle={
                <Space>
                  <TeamOutlined style={{ color: '#1890ff' }} />
                  <span>组织列表</span>
                  {selectedOrgId && orgDetail && (
                    <Tag color="blue">
                      当前: {orgDetail.orgName}
                    </Tag>
                  )}
                </Space>
              }
              toolBarRender={() => [
                <Button
                  key="add"
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={() => handleAdd(selectedOrgId)}
                >
                  新增组织
                </Button>,
                selectedOrgId && (
                  <Button
                    key="back"
                    onClick={() => {
                      setSelectedOrgId(null)
                      setOrgDetail(null)
                      setSelectedKeys([])
                      actionRef.current?.reload()
                    }}
                  >
                    显示全部
                  </Button>
                )
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
        </Col>
      </Row>

      <Modal
        title={editingItem ? '编辑组织' : '新增组织'}
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
                name="orgCode"
                label="组织编码"
                rules={[
                  { required: true, message: '请输入组织编码' },
                  { max: 50, message: '组织编码最多50个字符' }
                ]}
              >
                <Input placeholder="请输入组织编码" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="name"
                label="组织名称"
                rules={[
                  { required: true, message: '请输入组织名称' },
                  { max: 100, message: '组织名称最多100个字符' }
                ]}
              >
                <Input placeholder="请输入组织名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="type"
                label="组织类型"
                rules={[{ required: true, message: '请选择组织类型' }]}
                initialValue={2}
              >
                <Select placeholder="请选择组织类型">
                  {ORG_TYPE_OPTIONS.map(item => (
                    <Option key={item.value} value={item.value}>
                      <Space>
                        {item.icon}
                        <span>{item.label}</span>
                      </Space>
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="parentId"
                label="上级组织"
                help="不选则为顶级组织"
              >
                <Select
                  placeholder="请选择上级组织"
                  allowClear
                  showSearch
                  optionFilterProp="children"
                >
                  {orgTree.map(org => (
                    <Option key={org.id} value={org.id}>
                      {org.orgName}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="leaderId"
                label="负责人ID"
              >
                <InputNumber
                  placeholder="请输入负责人ID"
                  style={{ width: '100%' }}
                  min={1}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="leaderName"
                label="负责人姓名"
              >
                <Input placeholder="请输入负责人姓名" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="sortOrder"
                label="排序号"
                initialValue={0}
                help="数字越小越靠前"
              >
                <InputNumber
                  placeholder="排序号"
                  style={{ width: '100%' }}
                  min={0}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="status"
                label="状态"
                initialValue={1}
                valuePropName="checked"
              >
                <Switch
                  checkedChildren="启用"
                  unCheckedChildren="禁用"
                />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item
                name="remark"
                label="备注"
              >
                <Input.TextArea
                  placeholder="请输入备注信息"
                  rows={3}
                  maxLength={500}
                  showCount
                />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  )
}

export default OrgManagement
