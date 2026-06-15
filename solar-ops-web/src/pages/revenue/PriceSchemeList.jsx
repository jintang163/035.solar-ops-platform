import React, { useState, useEffect, useCallback } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  DatePicker,
  InputNumber,
  message,
  Tag,
  Switch,
  Row,
  Col,
  Tooltip,
  Descriptions,
  Drawer
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  FileTextOutlined
} from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  getPriceSchemeList,
  getPriceSchemeDetail,
  createPriceScheme,
  updatePriceScheme,
  deletePriceScheme
} from '../../api/revenue'

const { RangePicker } = DatePicker
const { TextArea } = Input
const { Option } = Select

const STATUS_MAP = {
  0: { color: 'error', text: '已停用' },
  1: { color: 'processing', text: '生效中' }
}

const PAGE_SIZE = 10

const PriceSchemeList = () => {
  const [loading, setLoading] = useState(false)
  const [schemeList, setSchemeList] = useState([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [queryForm] = Form.useForm()

  const [modalVisible, setModalVisible] = useState(false)
  const [modalLoading, setModalLoading] = useState(false)
  const [modalForm] = Form.useForm()
  const [editingScheme, setEditingScheme] = useState(null)

  const [detailVisible, setDetailVisible] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [currentScheme, setCurrentScheme] = useState(null)

  const fetchSchemeList = useCallback(async (page = 1, values = {}) => {
    setLoading(true)
    try {
      const params = { pageNum: page, pageSize: PAGE_SIZE, ...values }
      const res = await getPriceSchemeList(params)
      const pageResult = res.data || {}
      setSchemeList(pageResult.list || [])
      setTotal(pageResult.total || 0)
      setPageNum(pageResult.pageNum || page)
    } catch {
      setSchemeList([])
      setTotal(0)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchSchemeList(1)
  }, [fetchSchemeList])

  const handleQuery = async () => {
    try {
      const values = await queryForm.validateFields()
      const queryParams = { ...values }
      if (values.dateRange) {
        queryParams.startDate = values.dateRange[0].format('YYYY-MM-DD')
        queryParams.endDate = values.dateRange[1].format('YYYY-MM-DD')
        delete queryParams.dateRange
      }
      fetchSchemeList(1, queryParams)
    } catch {
      // ignore
    }
  }

  const handleReset = () => {
    queryForm.resetFields()
    fetchSchemeList(1)
  }

  const handlePageChange = (page) => {
    fetchSchemeList(page)
  }

  const handleAdd = (scheme = null) => {
    setEditingScheme(scheme)
    modalForm.resetFields()
    if (scheme) {
      modalForm.setFieldsValue({
        ...scheme,
        subsidyStartDate: scheme.subsidyStartDate ? dayjs(scheme.subsidyStartDate) : null,
        subsidyEndDate: scheme.subsidyEndDate ? dayjs(scheme.subsidyEndDate) : null,
        isDefault: scheme.isDefault === 1,
        isParity: scheme.isParity === 1
      })
    }
    setModalVisible(true)
  }

  const handleModalOk = async () => {
    try {
      const values = await modalForm.validateFields()
      const submitData = {
        ...values,
        subsidyStartDate: values.subsidyStartDate ? values.subsidyStartDate.format('YYYY-MM-DD') : undefined,
        subsidyEndDate: values.subsidyEndDate ? values.subsidyEndDate.format('YYYY-MM-DD') : undefined,
        isDefault: values.isDefault ? 1 : 0,
        isParity: values.isParity ? 1 : 0
      }
      setModalLoading(true)

      if (editingScheme && editingScheme.id) {
        submitData.id = editingScheme.id
        await updatePriceScheme(submitData)
        message.success('方案更新成功')
      } else {
        await createPriceScheme(submitData)
        message.success('方案创建成功')
      }

      setModalVisible(false)
      fetchSchemeList(pageNum)
    } catch (error) {
      if (error.errorFields) return
      message.error(error.message || (editingScheme ? '更新失败' : '创建失败'))
    } finally {
      setModalLoading(false)
    }
  }

  const handleViewDetail = async (scheme) => {
    setDetailLoading(true)
    setDetailVisible(true)
    try {
      const res = await getPriceSchemeDetail(scheme.id)
      setCurrentScheme(res.data || scheme)
    } catch {
      setCurrentScheme(scheme)
    } finally {
      setDetailLoading(false)
    }
  }

  const handleDelete = (scheme) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除电价方案「${scheme.schemeName}」吗？`,
      okText: '确认删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await deletePriceScheme(scheme.id)
          message.success('删除成功')
          fetchSchemeList(pageNum)
        } catch (error) {
          message.error(error.message || '删除失败')
        }
      }
    })
  }

  const columns = [
    {
      title: '方案名称',
      dataIndex: 'schemeName',
      key: 'schemeName',
      width: 180,
      render: (text, record) => (
        <a onClick={() => handleViewDetail(record)} style={{ color: '#1890ff' }}>{text}</a>
      )
    },
    {
      title: '所属电站',
      dataIndex: 'stationName',
      key: 'stationName',
      width: 140
    },
    {
      title: '上网电价',
      dataIndex: 'gridPrice',
      key: 'gridPrice',
      width: 120,
      render: (val) => <span style={{ color: '#1890ff', fontWeight: 500 }}>¥{Number(val || 0).toFixed(4)}</span>
    },
    {
      title: '国家补贴',
      dataIndex: 'nationalSubsidy',
      key: 'nationalSubsidy',
      width: 120,
      render: (val) => <span style={{ color: '#52c41a' }}>¥{Number(val || 0).toFixed(4)}</span>
    },
    {
      title: '省级补贴',
      dataIndex: 'provincialSubsidy',
      key: 'provincialSubsidy',
      width: 120,
      render: (val) => <span style={{ color: '#52c41a' }}>¥{Number(val || 0).toFixed(4)}</span>
    },
    {
      title: '市级补贴',
      dataIndex: 'municipalSubsidy',
      key: 'municipalSubsidy',
      width: 120,
      render: (val) => <span style={{ color: '#52c41a' }}>¥{Number(val || 0).toFixed(4)}</span>
    },
    {
      title: '综合电价',
      dataIndex: 'totalPrice',
      key: 'totalPrice',
      width: 120,
      render: (val, record) => {
        const total = Number(record.gridPrice || 0) + Number(record.nationalSubsidy || 0) +
          Number(record.provincialSubsidy || 0) + Number(record.municipalSubsidy || 0)
        return <span style={{ color: '#fa8c16', fontWeight: 600 }}>¥{total.toFixed(4)}</span>
      }
    },
    {
      title: '平价上网',
      dataIndex: 'isParity',
      key: 'isParity',
      width: 100,
      render: (val) => (
        val === 1
          ? <Tag color="green">是</Tag>
          : <Tag color="default">否</Tag>
      )
    },
    {
      title: '是否默认',
      dataIndex: 'isDefault',
      key: 'isDefault',
      width: 100,
      render: (val) => (
        val === 1
          ? <Tag color="gold"><CheckCircleOutlined /> 默认方案</Tag>
          : <Tag color="default"><CloseCircleOutlined /> 否</Tag>
      )
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
      title: '补贴起始日期',
      dataIndex: 'subsidyStartDate',
      key: 'subsidyStartDate',
      width: 130
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>
            详情
          </Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleAdd(record)}>
            编辑
          </Button>
          <Button type="link" size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record)}>
            删除
          </Button>
        </Space>
      )
    }
  ]

  return (
    <div className="price-scheme-page">
      <Card
        title={
          <span>
            <FileTextOutlined style={{ color: '#1890ff', marginRight: 6 }} />
            电价方案管理
          </span>
        }
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => handleAdd()}>
            新增方案
          </Button>
        }
      >
        <Form form={queryForm} layout="inline" style={{ marginBottom: 16 }}>
          <Form.Item name="stationId" label="电站">
            <Select placeholder="全部" allowClear style={{ width: 160 }}>
              <Option value={1}>一号光伏电站</Option>
              <Option value={2}>二号光伏电站</Option>
              <Option value={3}>三号光伏电站</Option>
              <Option value={4}>四号光伏电站</Option>
              <Option value={5}>五号光伏电站</Option>
            </Select>
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select placeholder="全部" allowClear style={{ width: 140 }}>
              <Option value={0}>已停用</Option>
              <Option value={1}>生效中</Option>
            </Select>
          </Form.Item>
          <Form.Item name="keyword" label="关键词">
            <Input placeholder="方案名称" allowClear style={{ width: 200 }} />
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
          dataSource={schemeList}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1600 }}
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
        title={editingScheme && editingScheme.id ? '编辑电价方案' : '新增电价方案'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        confirmLoading={modalLoading}
        okText="提交"
        cancelText="取消"
        width={720}
        destroyOnClose
      >
        <Form form={modalForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="schemeName"
                label="方案名称"
                rules={[{ required: true, message: '请输入方案名称' }]}
              >
                <Input placeholder="如：2024年标杆上网电价方案" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="stationId"
                label="所属电站"
                rules={[{ required: true, message: '请选择电站' }]}
              >
                <Select placeholder="请选择电站">
                  <Option value={1}>一号光伏电站</Option>
                  <Option value={2}>二号光伏电站</Option>
                  <Option value={3}>三号光伏电站</Option>
                  <Option value={4}>四号光伏电站</Option>
                  <Option value={5}>五号光伏电站</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="subsidyStartDate"
                label="补贴起始日期"
                rules={[{ required: true, message: '请选择补贴起始日期' }]}
              >
                <DatePicker style={{ width: '100%' }} placeholder="选择日期" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="subsidyEndDate"
                label="补贴截止日期"
              >
                <DatePicker style={{ width: '100%' }} placeholder="选择日期（可选）" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="gridPrice"
                label="上网电价 (元/kWh)"
                rules={[{ required: true, message: '请输入上网电价' }]}
              >
                <InputNumber
                  placeholder="请输入"
                  min={0}
                  step={0.0001}
                  precision={4}
                  style={{ width: '100%' }}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="nationalSubsidy"
                label="国家补贴 (元/kWh)"
              >
                <InputNumber
                  placeholder="请输入"
                  min={0}
                  step={0.0001}
                  precision={4}
                  style={{ width: '100%' }}
                />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="provincialSubsidy"
                label="省级补贴 (元/kWh)"
              >
                <InputNumber
                  placeholder="请输入"
                  min={0}
                  step={0.0001}
                  precision={4}
                  style={{ width: '100%' }}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="municipalSubsidy"
                label="市级补贴 (元/kWh)"
              >
                <InputNumber
                  placeholder="请输入"
                  min={0}
                  step={0.0001}
                  precision={4}
                  style={{ width: '100%' }}
                />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="benchmarkPrice"
                label="标杆电价 (元/kWh)"
              >
                <InputNumber
                  placeholder="请输入"
                  min={0}
                  step={0.0001}
                  precision={4}
                  style={{ width: '100%' }}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="status"
                label="状态"
                rules={[{ required: true, message: '请选择状态' }]}
              >
                <Select placeholder="请选择状态">
                  <Option value={0}>已停用</Option>
                  <Option value={1}>生效中</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="isParity"
                label="平价上网"
                valuePropName="checked"
              >
                <Switch />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="isDefault"
                label="设为默认方案"
                valuePropName="checked"
              >
                <Switch />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item
            name="remark"
            label="方案说明"
          >
            <TextArea rows={3} placeholder="请输入方案说明" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title="电价方案详情"
        width={720}
        onClose={() => setDetailVisible(false)}
        open={detailVisible}
        loading={detailLoading}
      >
        {currentScheme && (
          <>
            <Descriptions title="基本信息" bordered column={2} size="small" style={{ marginBottom: 24 }}>
              <Descriptions.Item label="方案名称" span={2}>{currentScheme.schemeName}</Descriptions.Item>
              <Descriptions.Item label="所属电站">{currentScheme.stationName || '-'}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={(STATUS_MAP[currentScheme.status] || {}).color}>
                  {(STATUS_MAP[currentScheme.status] || {}).text || currentScheme.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="补贴起始日期">{currentScheme.subsidyStartDate || '-'}</Descriptions.Item>
              <Descriptions.Item label="补贴截止日期">{currentScheme.subsidyEndDate || '-'}</Descriptions.Item>
              <Descriptions.Item label="是否默认">
                {currentScheme.isDefault === 1
                  ? <Tag color="gold"><CheckCircleOutlined /> 默认方案</Tag>
                  : <Tag color="default"><CloseCircleOutlined /> 否</Tag>}
              </Descriptions.Item>
              <Descriptions.Item label="平价上网">
                {currentScheme.isParity === 1
                  ? <Tag color="green">是</Tag>
                  : <Tag color="default">否</Tag>}
              </Descriptions.Item>
            </Descriptions>

            <Card title="电价构成" size="small" style={{ marginBottom: 24 }} type="inner">
              <Row gutter={16}>
                <Col span={6}>
                  <div style={{ textAlign: 'center', padding: '12px 0', background: '#e6f7ff', borderRadius: 4 }}>
                    <div style={{ fontSize: 12, color: '#666', marginBottom: 4 }}>上网电价</div>
                    <div style={{ fontSize: 20, fontWeight: 600, color: '#1890ff' }}>
                      ¥{Number(currentScheme.gridPrice || 0).toFixed(4)}
                    </div>
                    <div style={{ fontSize: 11, color: '#999' }}>元/kWh</div>
                  </div>
                </Col>
                <Col span={6}>
                  <div style={{ textAlign: 'center', padding: '12px 0', background: '#f6ffed', borderRadius: 4 }}>
                    <div style={{ fontSize: 12, color: '#666', marginBottom: 4 }}>国家补贴</div>
                    <div style={{ fontSize: 20, fontWeight: 600, color: '#52c41a' }}>
                      ¥{Number(currentScheme.nationalSubsidy || 0).toFixed(4)}
                    </div>
                    <div style={{ fontSize: 11, color: '#999' }}>元/kWh</div>
                  </div>
                </Col>
                <Col span={6}>
                  <div style={{ textAlign: 'center', padding: '12px 0', background: '#f6ffed', borderRadius: 4 }}>
                    <div style={{ fontSize: 12, color: '#666', marginBottom: 4 }}>省级补贴</div>
                    <div style={{ fontSize: 20, fontWeight: 600, color: '#52c41a' }}>
                      ¥{Number(currentScheme.provincialSubsidy || 0).toFixed(4)}
                    </div>
                    <div style={{ fontSize: 11, color: '#999' }}>元/kWh</div>
                  </div>
                </Col>
                <Col span={6}>
                  <div style={{ textAlign: 'center', padding: '12px 0', background: '#f6ffed', borderRadius: 4 }}>
                    <div style={{ fontSize: 12, color: '#666', marginBottom: 4 }}>市级补贴</div>
                    <div style={{ fontSize: 20, fontWeight: 600, color: '#52c41a' }}>
                      ¥{Number(currentScheme.municipalSubsidy || 0).toFixed(4)}
                    </div>
                    <div style={{ fontSize: 11, color: '#999' }}>元/kWh</div>
                  </div>
                </Col>
              </Row>
              <div style={{ marginTop: 16, padding: 16, background: '#fff7e6', borderRadius: 4, textAlign: 'center' }}>
                <Tooltip title="综合电价 = 上网电价 + 国家补贴 + 省级补贴 + 市级补贴">
                  <div style={{ fontSize: 14, color: '#666', marginBottom: 4 }}>综合电价</div>
                  <div style={{ fontSize: 28, fontWeight: 600, color: '#fa8c16' }}>
                    ¥{(
                      Number(currentScheme.gridPrice || 0) +
                      Number(currentScheme.nationalSubsidy || 0) +
                      Number(currentScheme.provincialSubsidy || 0) +
                      Number(currentScheme.municipalSubsidy || 0)
                    ).toFixed(4)}
                  </div>
                  <div style={{ fontSize: 12, color: '#999' }}>元/kWh</div>
                </Tooltip>
              </div>
            </Card>

            {currentScheme.remark && (
              <Card title="方案说明" size="small" type="inner">
                <div style={{ color: '#666', lineHeight: 1.6 }}>{currentScheme.remark}</div>
              </Card>
            )}
          </>
        )}
      </Drawer>
    </div>
  )
}

export default PriceSchemeList
