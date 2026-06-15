import React, { useState, useEffect, useRef } from 'react'
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
  Image,
  Input as AntInput,
  Alert,
  Descriptions
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  DownloadOutlined,
  QrcodeOutlined,
  ImportOutlined,
  ExportOutlined,
  ArrowDownOutlined,
  ArrowUpOutlined,
  EyeOutlined,
  ExclamationCircleOutlined,
  WarningOutlined
} from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import { useNavigate, useLocation } from 'react-router-dom'
import dayjs from 'dayjs'
import {
  getSparePartList,
  getSparePartDetail,
  addSparePart,
  updateSparePart,
  deleteSparePart,
  deleteBatchSpareParts,
  sparePartInbound,
  sparePartOutbound,
  getSparePartQrCode,
  batchGenerateQrCode,
  scanSparePart,
  exportSpareParts,
  getInventoryDashboard
} from '../../api/spareParts'
import LowStockWarningCard from '../../components/LowStockWarningCard'

const { Search } = AntInput
const { Option } = Select
const { TextArea } = Input

const PART_TYPE_MAP = {
  fan: { color: 'blue', text: '风扇' },
  capacitor: { color: 'green', text: '电容' },
  board: { color: 'geekblue', text: '板卡' },
  other: { color: 'default', text: '其他' }
}

const WARN_STATUS_MAP = {
  0: { color: 'success', text: '正常', icon: null },
  1: { color: 'warning', text: '低库存预警', icon: <WarningOutlined /> },
  2: { color: 'error', text: '库存不足', icon: <ExclamationCircleOutlined /> }
}

const STATUS_MAP = {
  0: { color: 'default', text: '停用' },
  1: { color: 'success', text: '启用' }
}

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

const SparePartInventoryList = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const actionRef = useRef()
  const [form] = Form.useForm()
  const [inboundForm] = Form.useForm()
  const [outboundForm] = Form.useForm()

  const [modalVisible, setModalVisible] = useState(false)
  const [inboundVisible, setInboundVisible] = useState(false)
  const [outboundVisible, setOutboundVisible] = useState(false)
  const [qrModalVisible, setQrModalVisible] = useState(false)
  const [scanModalVisible, setScanModalVisible] = useState(false)
  const [detailVisible, setDetailVisible] = useState(false)

  const [editingItem, setEditingItem] = useState(null)
  const [currentItem, setCurrentItem] = useState(null)
  const [currentQrCode, setCurrentQrCode] = useState('')
  const [scanCode, setScanCode] = useState('')
  const [scanResult, setScanResult] = useState(null)
  const [selectedRowKeys, setSelectedRowKeys] = useState([])
  const [dashboardData, setDashboardData] = useState(null)
  const [loading, setLoading] = useState(false)

  const searchParams = new URLSearchParams(location.search)
  const initialWarnStatus = searchParams.get('warnStatus')
    ? parseInt(searchParams.get('warnStatus'))
    : undefined

  useEffect(() => {
    fetchDashboard()
  }, [])

  const fetchDashboard = async () => {
    try {
      const res = await getInventoryDashboard()
      setDashboardData(res.data)
    } catch (e) {
      console.error('获取仪表盘数据失败', e)
    }
  }

  const handleScanCode = async () => {
    if (!scanCode) {
      message.warning('请输入备件编号')
      return
    }
    try {
      const res = await scanSparePart(scanCode)
      setScanResult(res.data)
    } catch (e) {
      message.error('未找到该备件')
      setScanResult(null)
    }
  }

  const handleAdd = () => {
    setEditingItem(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record) => {
    setEditingItem(record)
    form.setFieldsValue({
      ...record
    })
    setModalVisible(true)
  }

  const handleViewDetail = async (record) => {
    try {
      const res = await getSparePartDetail(record.id)
      setCurrentItem(res.data)
      setDetailVisible(true)
    } catch (e) {
      message.error('获取详情失败')
    }
  }

  const handleInbound = (record) => {
    setCurrentItem(record)
    inboundForm.resetFields()
    inboundForm.setFieldsValue({
      partId: record.id,
      partName: record.partName,
      partCode: record.partCode,
      unitPrice: record.unitPrice,
      inOutType: 11
    })
    setInboundVisible(true)
  }

  const handleOutbound = (record) => {
    setCurrentItem(record)
    outboundForm.resetFields()
    outboundForm.setFieldsValue({
      partId: record.id,
      partName: record.partName,
      partCode: record.partCode,
      inOutType: 21
    })
    setOutboundVisible(true)
  }

  const handleShowQrCode = async (record) => {
    try {
      const res = await getSparePartQrCode(record.id)
      setCurrentQrCode(res.data)
      setCurrentItem(record)
      setQrModalVisible(true)
    } catch (e) {
      message.error('获取二维码失败')
    }
  }

  const handleBatchGenerateQr = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要生成二维码的备件')
      return
    }
    try {
      await batchGenerateQrCode(selectedRowKeys)
      message.success(`成功生成 ${selectedRowKeys.length} 个二维码`)
      actionRef.current?.reload()
    } catch (e) {
      message.error('批量生成二维码失败')
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      if (editingItem) {
        await updateSparePart({ ...editingItem, ...values })
        message.success('更新成功')
      } else {
        await addSparePart(values)
        message.success('新增成功')
      }

      setModalVisible(false)
      actionRef.current?.reload()
      fetchDashboard()
    } catch (e) {
      console.error('提交失败', e)
    } finally {
      setLoading(false)
    }
  }

  const handleInboundSubmit = async () => {
    try {
      const values = await inboundForm.validateFields()
      setLoading(true)
      await sparePartInbound(values)
      message.success('入库成功')
      setInboundVisible(false)
      actionRef.current?.reload()
      fetchDashboard()
    } catch (e) {
      console.error('入库失败', e)
    } finally {
      setLoading(false)
    }
  }

  const handleOutboundSubmit = async () => {
    try {
      const values = await outboundForm.validateFields()
      setLoading(true)
      await sparePartOutbound(values)
      message.success('出库成功')
      setOutboundVisible(false)
      actionRef.current?.reload()
      fetchDashboard()
    } catch (e) {
      console.error('出库失败', e)
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async (id) => {
    try {
      await deleteSparePart(id)
      message.success('删除成功')
      actionRef.current?.reload()
      fetchDashboard()
    } catch (e) {
      message.error('删除失败')
    }
  }

  const handleBatchDelete = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要删除的备件')
      return
    }
    try {
      await deleteBatchSpareParts(selectedRowKeys)
      message.success(`成功删除 ${selectedRowKeys.length} 条记录`)
      setSelectedRowKeys([])
      actionRef.current?.reload()
      fetchDashboard()
    } catch (e) {
      message.error('批量删除失败')
    }
  }

  const handleExport = async () => {
    try {
      const params = {}
      if (initialWarnStatus !== undefined) {
        params.warnStatus = initialWarnStatus
      }
      const blob = await exportSpareParts(params)
      const url = window.URL.createObjectURL(new Blob([blob]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', `备件库存台账_${dayjs().format('YYYYMMDD')}.xlsx`)
      document.body.appendChild(link)
      link.click()
      link.remove()
      message.success('导出成功')
    } catch (e) {
      message.error('导出失败')
    }
  }

  const getRowClassName = (record) => {
    if (record.warnStatus === 2) {
      return 'row-danger'
    }
    if (record.warnStatus === 1) {
      return 'row-warning'
    }
    return ''
  }

  const columns = [
    {
      title: '备件编号',
      dataIndex: 'partCode',
      key: 'partCode',
      width: 140,
      render: (text) => <span style={{ fontFamily: 'monospace' }}>{text}</span>
    },
    {
      title: '备件名称',
      dataIndex: 'partName',
      key: 'partName',
      width: 120,
      render: (text, record) => (
        <a onClick={() => handleViewDetail(record)}>{text}</a>
      )
    },
    {
      title: '类型',
      dataIndex: 'partType',
      key: 'partType',
      width: 80,
      render: (text) => {
        const type = PART_TYPE_MAP[text] || PART_TYPE_MAP.other
        return <Tag color={type.color}>{type.text}</Tag>
      }
    },
    {
      title: '型号',
      dataIndex: 'partModel',
      key: 'partModel',
      width: 150
    },
    {
      title: '品牌',
      dataIndex: 'brand',
      key: 'brand',
      width: 100
    },
    {
      title: '规格',
      dataIndex: 'specification',
      key: 'specification',
      width: 150,
      ellipsis: true
    },
    {
      title: '单位',
      dataIndex: 'unit',
      key: 'unit',
      width: 60
    },
    {
      title: '单价',
      dataIndex: 'unitPrice',
      key: 'unitPrice',
      width: 100,
      render: (val) => val ? `¥${Number(val).toLocaleString()}` : '-'
    },
    {
      title: '库存数量',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 100,
      render: (val, record) => {
        const warn = WARN_STATUS_MAP[record.warnStatus]
        return (
          <span style={{
            color: warn?.color === 'error' ? '#ff4d4f' : warn?.color === 'warning' ? '#faad14' : '#52c41a',
            fontWeight: 600
          }}>
            {warn?.icon} {val}
          </span>
        )
      }
    },
    {
      title: '安全库存',
      dataIndex: 'safeQuantity',
      key: 'safeQuantity',
      width: 100
    },
    {
      title: '库存金额',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 120,
      render: (val) => val ? `¥${Number(val).toLocaleString()}` : '-'
    },
    {
      title: '存放位置',
      dataIndex: 'storageLocation',
      key: 'storageLocation',
      width: 120,
      ellipsis: true
    },
    {
      title: '所属仓库',
      dataIndex: 'warehouse',
      key: 'warehouse',
      width: 100
    },
    {
      title: '预警状态',
      dataIndex: 'warnStatus',
      key: 'warnStatus',
      width: 110,
      render: (val) => {
        const warn = WARN_STATUS_MAP[val]
        return (
          <Tag color={warn?.color}>
            {warn?.icon} {warn?.text}
          </Tag>
        )
      }
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (val) => {
        const status = STATUS_MAP[val]
        return <Tag color={status?.color}>{status?.text}</Tag>
      }
    },
    {
      title: '操作',
      key: 'action',
      width: 260,
      fixed: 'right',
      render: (_, record) => (
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
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            icon={<ArrowDownOutlined />}
            onClick={() => handleInbound(record)}
          >
            入库
          </Button>
          <Button
            type="link"
            size="small"
            icon={<ArrowUpOutlined />}
            danger
            onClick={() => handleOutbound(record)}
          >
            出库
          </Button>
          <Button
            type="link"
            size="small"
            icon={<QrcodeOutlined />}
            onClick={() => handleShowQrCode(record)}
          >
            二维码
          </Button>
          <Popconfirm
            title="确定删除该备件吗？"
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

  return (
    <div className="spare-part-page">
      <LowStockWarningCard dashboardData={dashboardData} />

      <Card style={{ marginTop: 16 }}>
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col xs={24} sm={12} md={8} lg={6}>
            <Search
              placeholder="输入备件编号模拟扫码查询"
              allowClear
              enterButton={
                <Space>
                  <QrcodeOutlined />
                  扫码查询
                </Space>
              }
              size="large"
              value={scanCode}
              onChange={(e) => setScanCode(e.target.value)}
              onSearch={handleScanCode}
              onClick={() => setScanModalVisible(true)}
            />
          </Col>
        </Row>

        <ProTable
          rowKey="id"
          actionRef={actionRef}
          columns={columns}
          request={async (params, sort, filter) => {
            const queryParams = {
              pageNum: params.current,
              pageSize: params.pageSize,
              keyword: params.keyword,
              partType: params.partType,
              warehouse: params.warehouse,
              warnStatus: initialWarnStatus !== undefined ? initialWarnStatus : params.warnStatus,
              status: params.status
            }
            const res = await getSparePartList(queryParams)
            return {
              data: res.data?.list || [],
              success: true,
              total: res.data?.total || 0
            }
          }}
          rowClassName={getRowClassName}
          rowSelection={{
            selectedRowKeys,
            onChange: (keys) => setSelectedRowKeys(keys)
          }}
          headerTitle={
            <Space>
              <span>备件库存列表</span>
              {initialWarnStatus !== undefined && (
                <Tag color={initialWarnStatus === 2 ? 'red' : 'orange'}>
                  {WARN_STATUS_MAP[initialWarnStatus]?.text}
                </Tag>
              )}
            </Space>
          }
          toolBarRender={() => [
            <Button
              key="add"
              type="primary"
              icon={<PlusOutlined />}
              onClick={handleAdd}
            >
              新增备件
            </Button>,
            <Button
              key="inbound"
              icon={<ImportOutlined />}
              onClick={() => {
                message.info('请在列表中选择备件后点击入库按钮')
              }}
            >
              入库管理
            </Button>,
            <Button
              key="outbound"
              icon={<ExportOutlined />}
              onClick={() => navigate('/spare-part-outbound-scan')}
            >
              扫码出库
            </Button>,
            <Button
              key="qrcode"
              icon={<QrcodeOutlined />}
              onClick={handleBatchGenerateQr}
              disabled={selectedRowKeys.length === 0}
            >
              批量生成二维码
            </Button>,
            <Button
              key="delete"
              danger
              icon={<DeleteOutlined />}
              onClick={handleBatchDelete}
              disabled={selectedRowKeys.length === 0}
            >
              批量删除
            </Button>,
            <Button
              key="export"
              icon={<DownloadOutlined />}
              onClick={handleExport}
            >
              导出台账
            </Button>,
            <Button
              key="stocktake"
              onClick={() => navigate('/stocktakes')}
            >
              库存盘点
            </Button>,
            <Button
              key="purchase"
              onClick={() => navigate('/purchase-suggestions')}
            >
              采购建议
            </Button>
          ]}
          search={{
            labelWidth: 'auto',
            defaultCollapsed: false,
            optionRender: ({ searchText, resetText }, { form }) => [
              <Button
                key="submit"
                type="primary"
                onClick={() => {
                  form.submit()
                }}
              >
                <SearchOutlined />
                {searchText}
              </Button>,
              <Button
                key="reset"
                onClick={() => {
                  form.resetFields()
                }}
              >
                {resetText}
              </Button>
            ]
          }}
          form={{
            syncToUrl: true
          }}
          columnsState={{
            persistenceKey: 'spare-part-columns',
            persistenceType: 'localStorage'
          }}
          pagination={{
            defaultPageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`
          }}
          options={{
            density: true,
            setting: true,
            fullScreen: true
          }}
        />
      </Card>

      <Modal
        title={editingItem ? '编辑备件' : '新增备件'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        confirmLoading={loading}
        width={700}
        okText="确定"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="partName"
                label="备件名称"
                rules={[{ required: true, message: '请输入备件名称' }]}
              >
                <Input placeholder="请输入备件名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="partType"
                label="备件类型"
                rules={[{ required: true, message: '请选择备件类型' }]}
              >
                <Select placeholder="请选择备件类型">
                  {PART_TYPE_OPTIONS.map(item => (
                    <Option key={item.value} value={item.value}>{item.label}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="partModel" label="备件型号">
                <Input placeholder="请输入备件型号" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="brand" label="品牌">
                <Input placeholder="请输入品牌" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="specification" label="规格参数">
                <Input placeholder="请输入规格参数" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="unit" label="单位" initialValue="个">
                <Input placeholder="请输入单位" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="unitPrice" label="单价(元)">
                <InputNumber
                  style={{ width: '100%' }}
                  placeholder="请输入单价"
                  min={0}
                  precision={2}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="quantity" label="库存数量" initialValue={0}>
                <InputNumber
                  style={{ width: '100%' }}
                  placeholder="请输入库存数量"
                  min={0}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="safeQuantity" label="安全库存数量" initialValue={10}>
                <InputNumber
                  style={{ width: '100%' }}
                  placeholder="请输入安全库存数量"
                  min={0}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="minPurchaseQuantity" label="最小采购数量" initialValue={10}>
                <InputNumber
                  style={{ width: '100%' }}
                  placeholder="请输入最小采购数量"
                  min={1}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="storageLocation" label="存放位置">
                <Input placeholder="例如：A区-01货架-03层" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="warehouse" label="所属仓库">
                <Select placeholder="请选择仓库">
                  {WAREHOUSE_OPTIONS.map(item => (
                    <Option key={item.value} value={item.value}>{item.label}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="supplier" label="供应商">
                <Input placeholder="请输入供应商" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="manufacturer" label="生产厂家">
                <Input placeholder="请输入生产厂家" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="状态" initialValue={1}>
                <Select>
                  <Option value={1}>启用</Option>
                  <Option value={0}>停用</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="remark" label="备注">
                <TextArea rows={3} placeholder="请输入备注" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title="备件入库"
        open={inboundVisible}
        onOk={handleInboundSubmit}
        onCancel={() => setInboundVisible(false)}
        confirmLoading={loading}
        okText="确认入库"
        cancelText="取消"
      >
        <Form form={inboundForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="partName" label="备件名称">
                <Input disabled />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="partCode" label="备件编号">
                <Input disabled />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Divider style={{ margin: '8px 0' }} />
            </Col>
            <Col span={12}>
              <Form.Item
                name="inOutType"
                label="入库类型"
                rules={[{ required: true, message: '请选择入库类型' }]}
              >
                <Select>
                  <Option value={11}>采购入库</Option>
                  <Option value={12}>盘盈入库</Option>
                  <Option value={13}>退库入库</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="quantity"
                label="入库数量"
                rules={[{ required: true, message: '请输入入库数量' }]}
              >
                <InputNumber
                  style={{ width: '100%' }}
                  min={1}
                  placeholder="请输入入库数量"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="unitPrice" label="入库单价(元)">
                <InputNumber
                  style={{ width: '100%' }}
                  min={0}
                  precision={2}
                  placeholder="请输入单价"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="supplier" label="供应商">
                <Input placeholder="请输入供应商" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="batchNo" label="批次号">
                <Input placeholder="请输入批次号" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="operatorName" label="操作人">
                <Input placeholder="请输入操作人姓名" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="remark" label="备注">
                <TextArea rows={2} placeholder="请输入备注" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title="备件出库"
        open={outboundVisible}
        onOk={handleOutboundSubmit}
        onCancel={() => setOutboundVisible(false)}
        confirmLoading={loading}
        okText="确认出库"
        okButtonProps={{ danger: true }}
        cancelText="取消"
      >
        <Form form={outboundForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="partName" label="备件名称">
                <Input disabled />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="partCode" label="备件编号">
                <Input disabled />
              </Form.Item>
            </Col>
            {currentItem && (
              <Col span={24}>
                <Alert
                  message={`当前库存: ${currentItem.quantity} ${currentItem.unit}`}
                  type="warning"
                  showIcon
                  size="small"
                />
              </Col>
            )}
            <Col span={24}>
              <Divider style={{ margin: '8px 0' }} />
            </Col>
            <Col span={12}>
              <Form.Item
                name="inOutType"
                label="出库类型"
                rules={[{ required: true, message: '请选择出库类型' }]}
              >
                <Select>
                  <Option value={21}>工单出库</Option>
                  <Option value={22}>盘亏出库</Option>
                  <Option value={23}>报废出库</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="quantity"
                label="出库数量"
                rules={[
                  { required: true, message: '请输入出库数量' },
                  {
                    validator: (_, value) => {
                      if (currentItem && value > currentItem.quantity) {
                        return Promise.reject('出库数量不能大于当前库存')
                      }
                      return Promise.resolve()
                    }
                  }
                ]}
              >
                <InputNumber
                  style={{ width: '100%' }}
                  min={1}
                  max={currentItem?.quantity}
                  placeholder="请输入出库数量"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="workOrderNo" label="关联工单编号">
                <Input placeholder="请输入工单编号" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="operatorName" label="操作人">
                <Input placeholder="请输入操作人姓名" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="remark" label="备注">
                <TextArea rows={2} placeholder="请输入备注" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title="备件二维码"
        open={qrModalVisible}
        onCancel={() => setQrModalVisible(false)}
        footer={null}
        width={400}
      >
        {currentItem && (
          <div style={{ textAlign: 'center' }}>
            <div style={{ marginBottom: 16 }}>
              <Image
                width={250}
                height={250}
                src={`data:image/png;base64,${currentQrCode}`}
                preview={false}
              />
            </div>
            <Divider />
            <div>
              <p><strong>备件名称:</strong> {currentItem.partName}</p>
              <p><strong>备件编号:</strong> <span style={{ fontFamily: 'monospace' }}>{currentItem.partCode}</span></p>
              <p><strong>存放位置:</strong> {currentItem.storageLocation}</p>
            </div>
          </div>
        )}
      </Modal>

      <Modal
        title="扫码查询"
        open={scanModalVisible}
        onCancel={() => {
          setScanModalVisible(false)
          setScanResult(null)
          setScanCode('')
        }}
        footer={null}
        width={600}
      >
        <Search
          placeholder="扫描二维码或输入备件编号"
          allowClear
          enterButton="查询"
          size="large"
          value={scanCode}
          onChange={(e) => setScanCode(e.target.value)}
          onSearch={handleScanCode}
          style={{ marginBottom: 16 }}
        />
        {scanResult && (
          <Card size="small">
            <Row gutter={16}>
              <Col span={12}>
                <p><strong>备件编号:</strong> {scanResult.partCode}</p>
                <p><strong>备件名称:</strong> {scanResult.partName}</p>
                <p><strong>类型:</strong> {scanResult.partTypeDesc}</p>
                <p><strong>型号:</strong> {scanResult.partModel}</p>
              </Col>
              <Col span={12}>
                <p><strong>库存数量:</strong> {scanResult.quantity} {scanResult.unit}</p>
                <p><strong>安全库存:</strong> {scanResult.safeQuantity}</p>
                <p><strong>预警状态:</strong>
                  <Tag color={WARN_STATUS_MAP[scanResult.warnStatus]?.color}>
                    {WARN_STATUS_MAP[scanResult.warnStatus]?.text}
                  </Tag>
                </p>
                <p><strong>存放位置:</strong> {scanResult.storageLocation}</p>
              </Col>
            </Row>
            <Divider />
            <Space>
              <Button type="primary" onClick={() => handleInbound(scanResult)}>
                入库
              </Button>
              <Button type="primary" danger onClick={() => handleOutbound(scanResult)}>
                出库
              </Button>
              <Button onClick={() => handleShowQrCode(scanResult)}>
                查看二维码
              </Button>
            </Space>
          </Card>
        )}
      </Modal>

      <Modal
        title="备件详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={700}
      >
        {currentItem && (
          <div>
            <Row gutter={16}>
              <Col span={12}>
                <Descriptions column={1} size="small">
                  <Descriptions.Item label="备件编号">{currentItem.partCode}</Descriptions.Item>
                  <Descriptions.Item label="备件名称">{currentItem.partName}</Descriptions.Item>
                  <Descriptions.Item label="类型">
                    <Tag color={PART_TYPE_MAP[currentItem.partType]?.color}>
                      {PART_TYPE_MAP[currentItem.partType]?.text}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="型号">{currentItem.partModel}</Descriptions.Item>
                  <Descriptions.Item label="品牌">{currentItem.brand}</Descriptions.Item>
                  <Descriptions.Item label="规格">{currentItem.specification}</Descriptions.Item>
                </Descriptions>
              </Col>
              <Col span={12}>
                <Descriptions column={1} size="small">
                  <Descriptions.Item label="库存数量">
                    <span style={{
                      color: currentItem.warnStatus === 2 ? '#ff4d4f'
                        : currentItem.warnStatus === 1 ? '#faad14' : '#52c41a',
                      fontWeight: 600,
                      fontSize: 18
                    }}>
                      {currentItem.quantity} {currentItem.unit}
                    </span>
                  </Descriptions.Item>
                  <Descriptions.Item label="安全库存">{currentItem.safeQuantity} {currentItem.unit}</Descriptions.Item>
                  <Descriptions.Item label="单价">¥{currentItem.unitPrice?.toLocaleString()}</Descriptions.Item>
                  <Descriptions.Item label="库存金额">¥{currentItem.totalAmount?.toLocaleString()}</Descriptions.Item>
                  <Descriptions.Item label="预警状态">
                    <Tag color={WARN_STATUS_MAP[currentItem.warnStatus]?.color}>
                      {WARN_STATUS_MAP[currentItem.warnStatus]?.icon} {WARN_STATUS_MAP[currentItem.warnStatus]?.text}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="存放位置">{currentItem.storageLocation}</Descriptions.Item>
                </Descriptions>
              </Col>
            </Row>
            <Divider />
            <Row gutter={16}>
              <Col span={24}>
                <Card title="库存统计" size="small" type="inner">
                  <Row gutter={16}>
                    <Col span={8}>
                      <Statistic
                        title="当前库存"
                        value={currentItem.quantity}
                        suffix={currentItem.unit}
                        valueStyle={{ color: '#1890ff' }}
                      />
                    </Col>
                    <Col span={8}>
                      <Statistic
                        title="安全库存"
                        value={currentItem.safeQuantity}
                        suffix={currentItem.unit}
                        valueStyle={{ color: '#faad14' }}
                      />
                    </Col>
                    <Col span={8}>
                      <Statistic
                        title="库存金额"
                        value={currentItem.totalAmount}
                        prefix="¥"
                        valueStyle={{ color: '#52c41a' }}
                      />
                    </Col>
                  </Row>
                </Card>
              </Col>
            </Row>
            {currentItem.remark && (
              <div style={{ marginTop: 16 }}>
                <strong>备注:</strong> {currentItem.remark}
              </div>
            )}
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
          background-color: #fffbe6 !important;
        }
        .row-warning:hover > td {
          background-color: #fffbe6 !important;
        }
        .spare-part-page {
          padding: 16px;
        }
      `}</style>
    </div>
  )
}

export default SparePartInventoryList
