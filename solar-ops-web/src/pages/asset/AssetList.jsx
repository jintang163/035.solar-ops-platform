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
  DatePicker,
  Upload,
  Row,
  Col,
  Image
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  DownloadOutlined,
  UploadOutlined,
  QrcodeOutlined,
  EyeOutlined,
  LogoutOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import { useNavigate } from 'react-router-dom'
import dayjs from 'dayjs'
import {
  getAssetList,
  getAssetDetail,
  addAsset,
  updateAsset,
  deleteAsset,
  deleteBatchAssets,
  retireAsset,
  scrapAsset,
  getAssetQrCode,
  exportAssets,
  importAsset,
  getStationListAll
} from '../../api/asset'

const { Search } = Input
const { Option } = Select
const { RangePicker } = DatePicker

const ASSET_TYPE_MAP = {
  station: { color: 'blue', text: '电站' },
  inverter: { color: 'green', text: '逆变器' },
  combiner: { color: 'cyan', text: '汇流箱' },
  panel: { color: 'geekblue', text: '光伏组件' },
  transformer: { color: 'purple', text: '变压器' },
  other: { color: 'default', text: '其他' }
}

const ASSET_STATUS_MAP = {
  1: { color: 'success', text: '正常' },
  2: { color: 'processing', text: '运维中' },
  3: { color: 'warning', text: '已退役' },
  4: { color: 'error', text: '已报废' }
}

const WARRANTY_STATUS_MAP = {
  1: { color: 'success', text: '正常' },
  2: { color: 'warning', text: '即将到期' },
  3: { color: 'error', text: '已过期' }
}

const MAINTENANCE_TYPE_OPTIONS = [
  { value: 'station', label: '电站' },
  { value: 'inverter', label: '逆变器' },
  { value: 'combiner', label: '汇流箱' },
  { value: 'panel', label: '光伏组件' },
  { value: 'transformer', label: '变压器' },
  { value: 'other', label: '其他' }
]

const ASSET_STATUS_OPTIONS = [
  { value: 1, label: '正常' },
  { value: 2, label: '运维中' },
  { value: 3, label: '已退役' },
  { value: 4, label: '已报废' }
]

const AssetList = () => {
  const navigate = useNavigate()
  const actionRef = useRef()
  const [modalVisible, setModalVisible] = useState(false)
  const [qrModalVisible, setQrModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState(null)
  const [form] = Form.useForm()
  const [stationList, setStationList] = useState([])
  const [currentQrCode, setCurrentQrCode] = useState('')
  const [loading, setLoading] = useState(false)

  const fetchStationList = async () => {
    try {
      const res = await getStationListAll()
      setStationList(res.data || [])
    } catch (e) {
      console.error('获取电站列表失败', e)
    }
  }

  React.useEffect(() => {
    fetchStationList()
  }, [])

  const columns = [
    {
      title: '资产编号',
      dataIndex: 'assetCode',
      key: 'assetCode',
      width: 140,
      fixed: 'left',
      copyable: true
    },
    {
      title: '资产名称',
      dataIndex: 'assetName',
      key: 'assetName',
      width: 160,
      render: (text, record) => (
        <a onClick={() => navigate(`/asset/detail/${record.id}`)}>{text}</a>
      )
    },
    {
      title: '资产类型',
      dataIndex: 'assetType',
      key: 'assetType',
      width: 100,
      render: (type) => {
        const info = ASSET_TYPE_MAP[type] || { color: 'default', text: type }
        return <Tag color={info.color}>{info.text}</Tag>
      },
      valueType: 'select',
      valueEnum: MAINTENANCE_TYPE_OPTIONS.reduce((acc, item) => {
        acc[item.value] = { text: item.label }
        return acc
      }, {})
    },
    {
      title: '所属电站',
      dataIndex: 'stationName',
      key: 'stationName',
      width: 140
    },
    {
      title: '设备型号',
      dataIndex: 'deviceModel',
      key: 'deviceModel',
      width: 140
    },
    {
      title: '品牌',
      dataIndex: 'brand',
      key: 'brand',
      width: 100
    },
    {
      title: '容量(kW)',
      dataIndex: 'capacity',
      key: 'capacity',
      width: 100,
      sorter: true,
      render: (val) => val || '-'
    },
    {
      title: '安装日期',
      dataIndex: 'installDate',
      key: 'installDate',
      width: 120,
      valueType: 'date'
    },
    {
      title: '质保到期',
      dataIndex: 'warrantyEndDate',
      key: 'warrantyEndDate',
      width: 120,
      valueType: 'date',
      render: (date, record) => {
        const status = record.warrantyStatus
        const statusInfo = WARRANTY_STATUS_MAP[status] || { color: 'default', text: '' }
        return (
          <Space direction="vertical" size={0}>
            <span>{date}</span>
            {statusInfo.text && (
              <Tag color={statusInfo.color} style={{ margin: 0 }}>
                {record.warrantyDaysLeft !== undefined ? `剩余${record.warrantyDaysLeft}天` : statusInfo.text}
              </Tag>
            )}
          </Space>
        )
      }
    },
    {
      title: '责任人',
      dataIndex: 'responsiblePerson',
      key: 'responsiblePerson',
      width: 100
    },
    {
      title: '资产状态',
      dataIndex: 'assetStatus',
      key: 'assetStatus',
      width: 100,
      render: (status) => {
        const info = ASSET_STATUS_MAP[status] || { color: 'default', text: status }
        return <Tag color={info.color}>{info.text}</Tag>
      },
      valueType: 'select',
      valueEnum: ASSET_STATUS_OPTIONS.reduce((acc, item) => {
        acc[item.value] = { text: item.label }
        return acc
      }, {})
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 170,
      valueType: 'dateTime'
    },
    {
      title: '操作',
      key: 'action',
      width: 320,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/asset/detail/${record.id}`)}
          >
            详情
          </Button>
          <Button
            type="link"
            size="small"
            icon={<QrcodeOutlined />}
            onClick={() => handleShowQrCode(record.id)}
          >
            二维码
          </Button>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
            disabled={record.assetStatus === 3 || record.assetStatus === 4}
          >
            编辑
          </Button>
          {record.assetStatus === 1 && (
            <Popconfirm
              title="确认退役该资产？"
              description="资产退役后将不能再进行运维操作"
              onConfirm={() => handleRetire(record.id)}
              okText="确认"
              cancelText="取消"
            >
              <Button type="link" size="small" icon={<LogoutOutlined />}>
                退役
              </Button>
            </Popconfirm>
          )}
          {record.assetStatus !== 4 && (
            <Popconfirm
              title="确认报废该资产？"
              description={
                <div>
                  <p>资产报废后将无法恢复，请确认以下事项：</p>
                  <ul>
                    <li>已完成资产处置审批流程</li>
                    <li>已完成资产残值处理</li>
                    <li>已解除相关运维合同</li>
                  </ul>
                </div>
              }
              onConfirm={() => handleScrap(record.id)}
              okText="确认报废"
              cancelText="取消"
              okButtonProps={{ danger: true }}
              icon={<ExclamationCircleOutlined style={{ color: '#ff4d4f' }} />}
            >
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                报废
              </Button>
            </Popconfirm>
          )}
        </Space>
      )
    }
  ]

  const handleShowQrCode = async (id) => {
    try {
      const res = await getAssetQrCode(id)
      setCurrentQrCode(res.data)
      setQrModalVisible(true)
    } catch (e) {
      message.error('获取二维码失败')
    }
  }

  const handleAdd = () => {
    setEditingItem(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = async (record) => {
    try {
      const res = await getAssetDetail(record.id)
      const data = res.data || {}
      if (data.installDate) {
        data.installDate = dayjs(data.installDate)
      }
      if (data.warrantyStartDate) {
        data.warrantyStartDate = dayjs(data.warrantyStartDate)
      }
      if (data.warrantyEndDate) {
        data.warrantyEndDate = dayjs(data.warrantyEndDate)
      }
      setEditingItem(data)
      form.setFieldsValue(data)
      setModalVisible(true)
    } catch (e) {
      message.error('获取资产详情失败')
    }
  }

  const handleDelete = async (id) => {
    try {
      await deleteAsset(id)
      message.success('删除成功')
      actionRef.current?.reload()
    } catch (e) {
      message.error(e.message || '删除失败')
    }
  }

  const handleRetire = async (id) => {
    try {
      await retireAsset(id)
      message.success('资产退役成功')
      actionRef.current?.reload()
    } catch (e) {
      message.error(e.message || '退役失败')
    }
  }

  const handleScrap = async (id) => {
    try {
      await scrapAsset(id)
      message.success('资产报废成功')
      actionRef.current?.reload()
    } catch (e) {
      message.error(e.message || '报废失败')
    }
  }

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      if (values.installDate) {
        values.installDate = values.installDate.format('YYYY-MM-DD')
      }
      if (values.warrantyStartDate) {
        values.warrantyStartDate = values.warrantyStartDate.format('YYYY-MM-DD')
      }
      if (values.warrantyEndDate) {
        values.warrantyEndDate = values.warrantyEndDate.format('YYYY-MM-DD')
      }

      if (editingItem) {
        values.id = editingItem.id
        await updateAsset(values)
        message.success('编辑成功')
      } else {
        await addAsset(values)
        message.success('新增成功')
      }
      setModalVisible(false)
      actionRef.current?.reload()
    } catch (error) {
      if (error.errorFields) return
      message.error(error.message || '操作失败')
    } finally {
      setLoading(false)
    }
  }

  const handleExport = async () => {
    try {
      const params = actionRef.current?.getParams?.() || {}
      const res = await exportAssets(params)
      const url = window.URL.createObjectURL(new Blob([res]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', `资产台账_${dayjs().format('YYYYMMDD')}.xlsx`)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    } catch (e) {
      message.error('导出失败')
    }
  }

  const handleImport = async (file) => {
    try {
      await importAsset(file)
      message.success('导入成功')
      actionRef.current?.reload()
    } catch (e) {
      message.error(e.message || '导入失败')
    }
    return false
  }

  const handleBatchDelete = async (keys) => {
    try {
      await deleteBatchAssets(keys)
      message.success('批量删除成功')
      actionRef.current?.reload()
    } catch (e) {
      message.error(e.message || '批量删除失败')
    }
  }

  const request = async (params = {}, sort, filter) => {
    try {
      const queryParams = {
        pageNum: params.current,
        pageSize: params.pageSize,
        ...params
      }
      if (params.keyword) {
        queryParams.keyword = params.keyword
      }
      if (params.assetType) {
        queryParams.assetType = params.assetType
      }
      if (params.assetStatus) {
        queryParams.assetStatus = params.assetStatus
      }
      if (params.stationId) {
        queryParams.stationId = params.stationId
      }
      const res = await getAssetList(queryParams)
      const pageResult = res.data || {}
      return {
        data: pageResult.list || [],
        success: true,
        total: pageResult.total || 0
      }
    } catch (e) {
      return {
        data: [],
        success: false,
        total: 0
      }
    }
  }

  const toolBarRender = () => [
    <Button key="add" type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
      新增资产
    </Button>,
    <Button key="export" icon={<DownloadOutlined />} onClick={handleExport}>
      导出Excel
    </Button>,
    <Upload
      key="import"
      showUploadList={false}
      beforeUpload={handleImport}
      accept=".xlsx,.xls"
    >
      <Button icon={<UploadOutlined />}>导入Excel</Button>
    </Upload>
  ]

  return (
    <div className="asset-list-page">
      <Card title="资产管理">
        <ProTable
          actionRef={actionRef}
          columns={columns}
          request={request}
          rowKey="id"
          search={{
            labelWidth: 'auto',
            defaultCollapsed: false
          }}
          toolBarRender={toolBarRender}
          tableAlertRender={({ selectedRowKeys }) => (
            <Space size={24}>
              <span>
                已选择 <a style={{ fontWeight: 600 }}>{selectedRowKeys.length}</a> 项
              </span>
              <Popconfirm
                title="确认批量删除选中资产？"
                onConfirm={() => handleBatchDelete(selectedRowKeys)}
                okText="确认"
                cancelText="取消"
              >
                <a style={{ color: '#ff4d4f' }}>批量删除</a>
              </Popconfirm>
            </Space>
          )}
          rowSelection={{
            alwaysShowAlert: false
          }}
          pagination={{
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`
          }}
          scroll={{ x: 1600 }}
        />
      </Card>

      <Modal
        title={editingItem ? '编辑资产' : '新增资产'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        okText="确定"
        cancelText="取消"
        confirmLoading={loading}
        width={800}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="assetName"
                label="资产名称"
                rules={[{ required: true, message: '请输入资产名称' }]}
              >
                <Input placeholder="请输入资产名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="assetType"
                label="资产类型"
                rules={[{ required: true, message: '请选择资产类型' }]}
              >
                <Select placeholder="请选择资产类型">
                  {MAINTENANCE_TYPE_OPTIONS.map(item => (
                    <Option key={item.value} value={item.value}>{item.label}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="stationId"
                label="所属电站"
                rules={[{ required: true, message: '请选择所属电站' }]}
              >
                <Select placeholder="请选择所属电站">
                  {stationList.map(item => (
                    <Option key={item.id} value={item.id}>{item.stationName}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="deviceSn" label="设备序列号">
                <Input placeholder="请输入设备序列号" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="deviceModel" label="设备型号">
                <Input placeholder="请输入设备型号" />
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
              <Form.Item name="capacity" label="容量(kW)">
                <InputNumber style={{ width: '100%' }} min={0} placeholder="请输入容量" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="installDate" label="安装日期">
                <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="warrantyStartDate" label="质保开始日期">
                <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="warrantyEndDate" label="质保到期日期">
                <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="warrantyMonths" label="质保期限(月)">
                <InputNumber style={{ width: '100%' }} min={0} placeholder="请输入质保期限" />
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
              <Form.Item name="installLocation" label="安装位置">
                <Input placeholder="请输入安装位置" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="purchaseAmount" label="采购金额">
                <InputNumber style={{ width: '100%' }} min={0} placeholder="请输入采购金额" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="responsiblePerson" label="责任人">
                <Input placeholder="请输入责任人" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="remark" label="备注">
                <Input.TextArea rows={3} placeholder="请输入备注" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title="资产二维码"
        open={qrModalVisible}
        onCancel={() => setQrModalVisible(false)}
        footer={null}
        width={400}
      >
        <div style={{ textAlign: 'center', padding: '20px 0' }}>
          <Image
            width={280}
            height={280}
            src={currentQrCode}
            preview={false}
          />
          <p style={{ marginTop: 16, color: '#666' }}>
            使用运维APP扫描二维码查看设备详情和维修记录
          </p>
        </div>
      </Modal>
    </div>
  )
}

export default AssetList
