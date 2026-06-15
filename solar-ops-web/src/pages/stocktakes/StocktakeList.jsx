import React, { useState, useRef } from 'react'
import {
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
  Tag,
  Row,
  Col,
  Statistic,
  Divider,
  Table,
  Drawer,
  Descriptions,
  Progress
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  PlayCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  DownloadOutlined,
  EyeOutlined,
  ExclamationCircleOutlined,
  WarningOutlined,
  CheckOutlined,
  CloseOutlined
} from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import dayjs from 'dayjs'
import {
  getStocktakeList,
  getStocktakeDetail,
  getStocktakeItems,
  createStocktake,
  startStocktake,
  updateStocktakeItem,
  completeStocktake,
  cancelStocktake,
  exportStocktakeDiff
} from '../../api/spareParts'

const { Option } = Select
const { TextArea } = Input

const STOCKTAKE_TYPE_MAP = {
  1: { color: 'blue', text: '全盘' },
  2: { color: 'geekblue', text: '抽盘' },
  3: { color: 'purple', text: '专项盘点' }
}

const STATUS_MAP = {
  0: { color: 'default', text: '待盘点', icon: null, action: '开始' },
  1: { color: 'processing', text: '盘点中', icon: null, action: '完成' },
  2: { color: 'success', text: '已完成', icon: <CheckCircleOutlined />, action: null },
  3: { color: 'error', text: '已取消', icon: <CloseCircleOutlined />, action: null }
}

const DIFF_TYPE_MAP = {
  0: { color: 'success', text: '无差异' },
  1: { color: 'blue', text: '盘盈' },
  2: { color: 'red', text: '盘亏' }
}

const STOCKTAKE_TYPE_OPTIONS = [
  { value: 1, label: '全盘' },
  { value: 2, label: '抽盘' },
  { value: 3, label: '专项盘点' }
]

const PART_TYPE_OPTIONS = [
  { value: 'fan', label: '风扇' },
  { value: 'capacitor', label: '电容' },
  { value: 'board', label: '板卡' },
  { value: 'other', label: '其他' }
]

const WAREHOUSE_OPTIONS = [
  { value: '中心仓库', label: '中心仓库' },
  { value: 'A分库', label: 'A分库' },
  { value: 'B分库', label: 'B分库' }
]

const DIFF_TYPE_OPTIONS = [
  { value: null, label: '全部' },
  { value: 0, label: '无差异' },
  { value: 1, label: '盘盈' },
  { value: 2, label: '盘亏' }
]

const StocktakeList = () => {
  const actionRef = useRef()
  const [form] = Form.useForm()
  const [itemForm] = Form.useForm()

  const [createVisible, setCreateVisible] = useState(false)
  const [detailVisible, setDetailVisible] = useState(false)
  const [itemsVisible, setItemsVisible] = useState(false)
  const [editItemVisible, setEditItemVisible] = useState(false)

  const [currentStocktake, setCurrentStocktake] = useState(null)
  const [currentItem, setCurrentItem] = useState(null)
  const [items, setItems] = useState([])
  const [itemsLoading, setItemsLoading] = useState(false)
  const [diffTypeFilter, setDiffTypeFilter] = useState(null)
  const [loading, setLoading] = useState(false)

  const handleCreate = () => {
    form.resetFields()
    setCreateVisible(true)
  }

  const handleCreateSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)
      await createStocktake(values)
      message.success('创建盘点单成功')
      setCreateVisible(false)
      actionRef.current?.reload()
    } catch (e) {
      console.error('创建失败', e)
    } finally {
      setLoading(false)
    }
  }

  const handleViewDetail = async (record) => {
    try {
      const res = await getStocktakeDetail(record.id)
      setCurrentStocktake(res.data)
      setDetailVisible(true)
    } catch (e) {
      message.error('获取详情失败')
    }
  }

  const handleViewItems = async (record) => {
    setCurrentStocktake(record)
    setItemsVisible(true)
    fetchItems(record.id)
  }

  const fetchItems = async (id, diffType = null) => {
    setItemsLoading(true)
    try {
      const params = {
        pageNum: 1,
        pageSize: 1000,
        diffType: diffType !== null ? diffType : undefined
      }
      const res = await getStocktakeItems(id, params)
      setItems(res.data?.list || [])
    } catch (e) {
      message.error('获取明细失败')
    } finally {
      setItemsLoading(false)
    }
  }

  const handleStart = async (id) => {
    try {
      await startStocktake(id)
      message.success('开始盘点')
      actionRef.current?.reload()
    } catch (e) {
      message.error(e.message || '操作失败')
    }
  }

  const handleComplete = async (id) => {
    Modal.confirm({
      title: '确认完成盘点',
      icon: <ExclamationCircleOutlined />,
      content: '完成盘点后将自动调整库存并生成出入库记录，此操作不可撤销，是否继续？',
      okText: '确认完成',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await completeStocktake(id)
          message.success('盘点完成，库存已调整')
          actionRef.current?.reload()
          if (itemsVisible) {
            fetchItems(currentStocktake.id)
          }
        } catch (e) {
          message.error(e.message || '操作失败')
        }
      }
    })
  }

  const handleCancel = async (id) => {
    Modal.confirm({
      title: '确认取消盘点',
      content: '取消后该盘点单将无法继续使用，是否继续？',
      okText: '确认取消',
      okType: 'danger',
      cancelText: '返回',
      onOk: async () => {
        try {
          await cancelStocktake(id)
          message.success('盘点已取消')
          actionRef.current?.reload()
        } catch (e) {
          message.error(e.message || '操作失败')
        }
      }
    })
  }

  const handleEditItem = (item) => {
    if (currentStocktake?.status !== 1) {
      message.warning('只有盘点中的单据可以修改明细')
      return
    }
    setCurrentItem(item)
    itemForm.setFieldsValue({
      itemId: item.id,
      actualQuantity: item.actualQuantity,
      remark: item.remark
    })
    setEditItemVisible(true)
  }

  const handleSaveItem = async () => {
    try {
      const values = await itemForm.validateFields()
      setLoading(true)
      await updateStocktakeItem(values)
      message.success('保存成功')
      setEditItemVisible(false)
      fetchItems(currentStocktake.id, diffTypeFilter)
    } catch (e) {
      console.error('保存失败', e)
    } finally {
      setLoading(false)
    }
  }

  const handleExportDiff = async (id) => {
    try {
      const blob = await exportStocktakeDiff(id)
      const url = window.URL.createObjectURL(new Blob([blob]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', `盘点差异报表_${dayjs().format('YYYYMMDD')}.xlsx`)
      document.body.appendChild(link)
      link.click()
      link.remove()
      message.success('导出成功')
    } catch (e) {
      message.error('导出失败')
    }
  }

  const handleDiffTypeChange = (value) => {
    setDiffTypeFilter(value)
    if (currentStocktake) {
      fetchItems(currentStocktake.id, value)
    }
  }

  const getRowClassName = (record) => {
    if (record.diffType === 2) return 'row-danger'
    if (record.diffType === 1) return 'row-warning'
    return ''
  }

  const columns = [
    {
      title: '盘点单号',
      dataIndex: 'stocktakeNo',
      key: 'stocktakeNo',
      width: 160,
      render: (text, record) => (
        <a onClick={() => handleViewDetail(record)} style={{ fontFamily: 'monospace' }}>{text}</a>
      )
    },
    {
      title: '盘点名称',
      dataIndex: 'stocktakeName',
      key: 'stocktakeName',
      width: 200,
      ellipsis: true
    },
    {
      title: '盘点类型',
      dataIndex: 'stocktakeType',
      key: 'stocktakeType',
      width: 100,
      render: (val) => {
        const type = STOCKTAKE_TYPE_MAP[val]
        return <Tag color={type?.color}>{type?.text}</Tag>
      }
    },
    {
      title: '仓库',
      dataIndex: 'warehouse',
      key: 'warehouse',
      width: 120
    },
    {
      title: '备件类型',
      dataIndex: 'partType',
      key: 'partType',
      width: 100
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 110,
      render: (val) => {
        const status = STATUS_MAP[val]
        return (
          <Tag color={status?.color}>
            {status?.icon} {status?.text}
          </Tag>
        )
      }
    },
    {
      title: '总项数',
      dataIndex: 'totalCount',
      key: 'totalCount',
      width: 80,
      align: 'right'
    },
    {
      title: '差异项',
      dataIndex: 'diffCount',
      key: 'diffCount',
      width: 80,
      align: 'right',
      render: (val) => (
        <span style={{ color: val > 0 ? '#ff4d4f' : undefined, fontWeight: val > 0 ? 600 : undefined }}>
          {val}
        </span>
      )
    },
    {
      title: '盘盈总数',
      dataIndex: 'profitQuantity',
      key: 'profitQuantity',
      width: 90,
      align: 'right',
      render: (val) => val > 0 ? <span style={{ color: '#1890ff' }}>+{val}</span> : val
    },
    {
      title: '盘亏总数',
      dataIndex: 'lossQuantity',
      key: 'lossQuantity',
      width: 90,
      align: 'right',
      render: (val) => val > 0 ? <span style={{ color: '#ff4d4f' }}>-{val}</span> : val
    },
    {
      title: '库存总金额',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 120,
      align: 'right',
      render: (val) => val ? `¥${Number(val).toLocaleString()}` : '-'
    },
    {
      title: '差异总金额',
      dataIndex: 'diffAmount',
      key: 'diffAmount',
      width: 120,
      align: 'right',
      render: (val) => val ? (
        <span style={{ color: val > 0 ? '#ff4d4f' : undefined }}>
          ¥{Number(val).toLocaleString()}
        </span>
      ) : '-'
    },
    {
      title: '盘点人',
      dataIndex: 'operatorName',
      key: 'operatorName',
      width: 100
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 170,
      render: (val) => dayjs(val).format('YYYY-MM-DD HH:mm')
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      fixed: 'right',
      render: (_, record) => {
        const status = STATUS_MAP[record.status]
        return (
          <Space size="small">
            <Button
              type="link"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => handleViewDetail(record)}
            >
              详情
            </Button>
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleViewItems(record)}
            >
              明细
            </Button>
            {record.status === 0 && (
              <>
                <Button
                  type="link"
                  size="small"
                  icon={<PlayCircleOutlined />}
                  onClick={() => handleStart(record.id)}
                >
                  开始
                </Button>
                <Button
                  type="link"
                  size="small"
                  danger
                  onClick={() => handleCancel(record.id)}
                >
                  取消
                </Button>
              </>
            )}
            {record.status === 1 && (
              <>
                <Button
                  type="link"
                  size="small"
                  icon={<CheckCircleOutlined />}
                  onClick={() => handleComplete(record.id)}
                >
                  完成
                </Button>
                <Button
                  type="link"
                  size="small"
                  danger
                  onClick={() => handleCancel(record.id)}
                >
                  取消
                </Button>
              </>
            )}
            {record.status === 2 && (
              <Button
                type="link"
                size="small"
                icon={<DownloadOutlined />}
                onClick={() => handleExportDiff(record.id)}
              >
                导出差异
              </Button>
            )}
          </Space>
        )
      }
    }
  ]

  const itemColumns = [
    {
      title: '备件编号',
      dataIndex: 'partCode',
      key: 'partCode',
      width: 140,
      fixed: 'left'
    },
    {
      title: '备件名称',
      dataIndex: 'partName',
      key: 'partName',
      width: 120
    },
    {
      title: '型号',
      dataIndex: 'partModel',
      key: 'partModel',
      width: 150
    },
    {
      title: '类型',
      dataIndex: 'partTypeDesc',
      key: 'partTypeDesc',
      width: 80
    },
    {
      title: '系统库存',
      dataIndex: 'systemQuantity',
      key: 'systemQuantity',
      width: 100,
      align: 'right'
    },
    {
      title: '实际盘点',
      dataIndex: 'actualQuantity',
      key: 'actualQuantity',
      width: 100,
      align: 'right',
      render: (val, record) => (
        <span style={{
          color: record.diffType === 1 ? '#1890ff' : record.diffType === 2 ? '#ff4d4f' : undefined,
          fontWeight: record.diffType !== 0 ? 600 : undefined
        }}>
          {val}
        </span>
      )
    },
    {
      title: '差异数量',
      dataIndex: 'diffQuantity',
      key: 'diffQuantity',
      width: 100,
      align: 'right',
      render: (val) => (
        <span style={{
          color: val > 0 ? '#1890ff' : val < 0 ? '#ff4d4f' : undefined,
          fontWeight: val !== 0 ? 600 : undefined
        }}>
          {val > 0 ? `+${val}` : val}
        </span>
      )
    },
    {
      title: '差异类型',
      dataIndex: 'diffType',
      key: 'diffType',
      width: 100,
      render: (val) => {
        const type = DIFF_TYPE_MAP[val]
        return <Tag color={type?.color}>{type?.text}</Tag>
      }
    },
    {
      title: '差异金额',
      dataIndex: 'diffAmount',
      key: 'diffAmount',
      width: 120,
      align: 'right',
      render: (val) => val ? `¥${Number(val).toLocaleString()}` : '-'
    },
    {
      title: '存放位置',
      dataIndex: 'storageLocation',
      key: 'storageLocation',
      width: 150
    },
    {
      title: '差异原因',
      dataIndex: 'remark',
      key: 'remark',
      width: 150,
      ellipsis: true
    },
    {
      title: '操作',
      key: 'action',
      width: 80,
      fixed: 'right',
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          onClick={() => handleEditItem(record)}
        >
          录入
        </Button>
      )
    }
  ]

  return (
    <div className="stocktake-page" style={{ padding: 16 }}>
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
              stocktakeType: params.stocktakeType,
              warehouse: params.warehouse,
              status: params.status
            }
            const res = await getStocktakeList(queryParams)
            return {
              data: res.data?.list || [],
              success: true,
              total: res.data?.total || 0
            }
          }}
          headerTitle="库存盘点列表"
          toolBarRender={() => [
            <Button
              key="add"
              type="primary"
              icon={<PlusOutlined />}
              onClick={handleCreate}
            >
              创建盘点单
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
        title="创建盘点单"
        open={createVisible}
        onOk={handleCreateSubmit}
        onCancel={() => setCreateVisible(false)}
        confirmLoading={loading}
        width={600}
        okText="创建"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={24}>
              <Form.Item
                name="stocktakeName"
                label="盘点名称"
                rules={[{ required: true, message: '请输入盘点名称' }]}
              >
                <Input placeholder="例如：2024年Q1季度全盘" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="stocktakeType"
                label="盘点类型"
                rules={[{ required: true, message: '请选择盘点类型' }]}
                initialValue={1}
              >
                <Select>
                  {STOCKTAKE_TYPE_OPTIONS.map(item => (
                    <Option key={item.value} value={item.value}>{item.label}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="warehouse"
                label="盘点仓库"
              >
                <Select placeholder="不选则盘点所有仓库">
                  {WAREHOUSE_OPTIONS.map(item => (
                    <Option key={item.value} value={item.value}>{item.label}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="partType"
                label="盘点备件类型"
              >
                <Select placeholder="不选则盘点所有类型">
                  {PART_TYPE_OPTIONS.map(item => (
                    <Option key={item.value} value={item.value}>{item.label}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="operatorName"
                label="盘点人"
              >
                <Input placeholder="请输入盘点人姓名" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item
                name="remark"
                label="备注"
              >
                <TextArea rows={3} placeholder="请输入备注" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title="盘点单详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={700}
      >
        {currentStocktake && (
          <div>
            <Descriptions column={2} size="small" bordered>
              <Descriptions.Item label="盘点单号">{currentStocktake.stocktakeNo}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={STATUS_MAP[currentStocktake.status]?.color}>
                  {STATUS_MAP[currentStocktake.status]?.icon} {STATUS_MAP[currentStocktake.status]?.text}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="盘点名称">{currentStocktake.stocktakeName}</Descriptions.Item>
              <Descriptions.Item label="盘点类型">
                <Tag color={STOCKTAKE_TYPE_MAP[currentStocktake.stocktakeType]?.color}>
                  {STOCKTAKE_TYPE_MAP[currentStocktake.stocktakeType]?.text}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="仓库">{currentStocktake.warehouse || '全部'}</Descriptions.Item>
              <Descriptions.Item label="备件类型">{currentStocktake.partType || '全部'}</Descriptions.Item>
              <Descriptions.Item label="盘点人">{currentStocktake.operatorName || '-'}</Descriptions.Item>
              <Descriptions.Item label="创建时间">{dayjs(currentStocktake.createTime).format('YYYY-MM-DD HH:mm')}</Descriptions.Item>
            </Descriptions>

            <Divider />

            <Row gutter={[16, 16]}>
              <Col span={6}>
                <Card size="small">
                  <Statistic
                    title="总项数"
                    value={currentStocktake.totalCount}
                    valueStyle={{ color: '#1890ff' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card size="small">
                  <Statistic
                    title="差异项"
                    value={currentStocktake.diffCount}
                    valueStyle={{ color: currentStocktake.diffCount > 0 ? '#ff4d4f' : '#52c41a' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card size="small">
                  <Statistic
                    title="盘盈总数"
                    value={currentStocktake.profitQuantity}
                    prefix="+"
                    valueStyle={{ color: '#1890ff' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card size="small">
                  <Statistic
                    title="盘亏总数"
                    value={currentStocktake.lossQuantity}
                    prefix="-"
                    valueStyle={{ color: '#ff4d4f' }}
                  />
                </Card>
              </Col>
            </Row>

            <Divider />

            <Card size="small" title="库存统计" type="inner">
              <Row gutter={16}>
                <Col span={12}>
                  <Statistic
                    title="库存总金额"
                    value={currentStocktake.totalAmount}
                    prefix="¥"
                    valueStyle={{ color: '#52c41a' }}
                  />
                </Col>
                <Col span={12}>
                  <Statistic
                    title="差异总金额"
                    value={currentStocktake.diffAmount}
                    prefix="¥"
                    valueStyle={{ color: '#ff4d4f' }}
                  />
                </Col>
              </Row>
              {currentStocktake.totalCount > 0 && (
                <div style={{ marginTop: 16 }}>
                  <div style={{ marginBottom: 8 }}>
                    盘点完成度: {currentStocktake.status === 2 ? '100%' : `${Math.round((1 - currentStocktake.diffCount / currentStocktake.totalCount) * 100)}%`}
                  </div>
                  <Progress
                    percent={currentStocktake.status === 2 ? 100 : Math.round((1 - currentStocktake.diffCount / currentStocktake.totalCount) * 100)}
                    status={currentStocktake.status === 2 ? 'success' : 'active'}
                  />
                </div>
              )}
            </Card>

            {currentStocktake.remark && (
              <div style={{ marginTop: 16 }}>
                <strong>备注:</strong> {currentStocktake.remark}
              </div>
            )}

            <Divider />

            <Space>
              <Button type="primary" onClick={() => {
                setDetailVisible(false)
                handleViewItems(currentStocktake)
              }}>
                查看盘点明细
              </Button>
              {currentStocktake.status === 2 && (
                <Button onClick={() => handleExportDiff(currentStocktake.id)}>
                  导出差异报表
                </Button>
              )}
              {currentStocktake.status === 0 && (
                <Button type="primary" onClick={() => {
                  handleStart(currentStocktake.id)
                  setDetailVisible(false)
                }}>
                  开始盘点
                </Button>
              )}
              {currentStocktake.status === 1 && (
                <Button type="primary" danger onClick={() => {
                  handleComplete(currentStocktake.id)
                  setDetailVisible(false)
                }}>
                  完成盘点
                </Button>
              )}
            </Space>
          </div>
        )}
      </Modal>

      <Drawer
        title={
          <Space>
            <span>盘点明细 - {currentStocktake?.stocktakeNo}</span>
            {currentStocktake && (
              <Tag color={STATUS_MAP[currentStocktake.status]?.color}>
                {STATUS_MAP[currentStocktake.status]?.text}
              </Tag>
            )}
          </Space>
        }
        width={1200}
        open={itemsVisible}
        onClose={() => {
          setItemsVisible(false)
          setItems([])
          setDiffTypeFilter(null)
        }}
        extra={
          <Space>
            <Select
              placeholder="筛选差异类型"
              style={{ width: 150 }}
              value={diffTypeFilter}
              onChange={handleDiffTypeChange}
            >
              {DIFF_TYPE_OPTIONS.map(item => (
                <Option key={item.value} value={item.value}>{item.label}</Option>
              ))}
            </Select>
            {currentStocktake?.status === 1 && (
              <Button
                type="primary"
                danger
                onClick={() => handleComplete(currentStocktake.id)}
              >
                <CheckCircleOutlined /> 完成盘点
              </Button>
            )}
            {currentStocktake?.status === 2 && (
              <Button onClick={() => handleExportDiff(currentStocktake.id)}>
                <DownloadOutlined /> 导出差异
              </Button>
            )}
          </Space>
        }
      >
        {currentStocktake && (
          <div>
            <Card size="small" style={{ marginBottom: 16 }}>
              <Row gutter={16}>
                <Col span={6}>
                  <Statistic
                    title="总项数"
                    value={currentStocktake.totalCount}
                    suffix="项"
                    valueStyle={{ fontSize: 18 }}
                  />
                </Col>
                <Col span={6}>
                  <Statistic
                    title="已盘点"
                    value={items.filter(i => i.actualQuantity > 0).length}
                    suffix="项"
                    valueStyle={{ fontSize: 18, color: '#1890ff' }}
                  />
                </Col>
                <Col span={6}>
                  <Statistic
                    title="盘盈"
                    value={currentStocktake.profitQuantity}
                    suffix="件"
                    valueStyle={{ fontSize: 18, color: '#1890ff' }}
                  />
                </Col>
                <Col span={6}>
                  <Statistic
                    title="盘亏"
                    value={currentStocktake.lossQuantity}
                    suffix="件"
                    valueStyle={{ fontSize: 18, color: '#ff4d4f' }}
                  />
                </Col>
              </Row>
            </Card>

            <Table
              rowKey="id"
              columns={itemColumns}
              dataSource={items}
              loading={itemsLoading}
              rowClassName={getRowClassName}
              scroll={{ x: 1400 }}
              pagination={{
                pageSize: 20,
                showSizeChanger: true,
                showQuickJumper: true
              }}
            />
          </div>
        )}
      </Drawer>

      <Modal
        title="录入盘点数量"
        open={editItemVisible}
        onOk={handleSaveItem}
        onCancel={() => setEditItemVisible(false)}
        confirmLoading={loading}
        width={500}
        okText="保存"
        cancelText="取消"
      >
        {currentItem && (
          <div>
            <Card size="small" style={{ marginBottom: 16 }}>
              <Descriptions column={2} size="small">
                <Descriptions.Item label="备件编号">{currentItem.partCode}</Descriptions.Item>
                <Descriptions.Item label="备件名称">{currentItem.partName}</Descriptions.Item>
                <Descriptions.Item label="型号">{currentItem.partModel}</Descriptions.Item>
                <Descriptions.Item label="系统库存">
                  <span style={{ color: '#1890ff', fontWeight: 600 }}>{currentItem.systemQuantity}</span>
                </Descriptions.Item>
              </Descriptions>
            </Card>
            <Form form={itemForm} layout="vertical">
              <Form.Item name="itemId" hidden>
                <Input />
              </Form.Item>
              <Form.Item
                name="actualQuantity"
                label="实际盘点数量"
                rules={[{ required: true, message: '请输入实际盘点数量' }]}
              >
                <InputNumber
                  style={{ width: '100%' }}
                  min={0}
                  placeholder="请输入实际盘点数量"
                  size="large"
                />
              </Form.Item>
              {currentItem.systemQuantity !== itemForm.getFieldValue('actualQuantity') && (
                <Alert
                  message={
                    itemForm.getFieldValue('actualQuantity') > currentItem.systemQuantity
                      ? `盘盈: ${itemForm.getFieldValue('actualQuantity') - currentItem.systemQuantity} 件`
                      : `盘亏: ${currentItem.systemQuantity - itemForm.getFieldValue('actualQuantity')} 件`
                  }
                  type={itemForm.getFieldValue('actualQuantity') > currentItem.systemQuantity ? 'info' : 'warning'}
                  showIcon
                />
              )}
              <Form.Item
                name="remark"
                label="差异原因（如有差异请填写）"
              >
                <TextArea rows={3} placeholder="请描述差异原因" />
              </Form.Item>
            </Form>
          </div>
        )}
      </Modal>

      <style>{`
        .row-danger {
          background-color: #fff1f0 !important;
        }
        .row-danger:hover > td {
          background-color: #fff1f0 !important;
        }
        .row-warning {
          background-color: #e6f7ff !important;
        }
        .row-warning:hover > td {
          background-color: #e6f7ff !important;
        }
      `}</style>
    </div>
  )
}

export default StocktakeList
