import React, { useState, useRef, useEffect } from 'react'
import {
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  message,
  Popconfirm,
  Card,
  Tag,
  Row,
  Col,
  Statistic,
  Divider,
  Badge,
  Tooltip
} from 'antd'
import {
  PlusOutlined,
  ReloadOutlined,
  DownloadOutlined,
  ShoppingCartOutlined,
  ClockCircleOutlined,
  WarningOutlined,
  ExclamationCircleOutlined,
  CheckOutlined,
  CloseOutlined
} from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import dayjs from 'dayjs'
import {
  getPurchaseSuggestionList,
  generatePurchaseSuggestions,
  processPurchaseSuggestion,
  batchProcessPurchaseSuggestions,
  getPendingSuggestionCount
} from '../../api/spareParts'

const { Option } = Select
const { TextArea } = Input

const PART_TYPE_MAP = {
  fan: { color: 'blue', text: '风扇' },
  capacitor: { color: 'green', text: '电容' },
  board: { color: 'geekblue', text: '板卡' },
  other: { color: 'default', text: '其他' }
}

const URGENCY_MAP = {
  1: { color: 'default', text: '一般', icon: <ClockCircleOutlined /> },
  2: { color: 'orange', text: '紧急', icon: <WarningOutlined /> },
  3: { color: 'red', text: '非常紧急', icon: <ExclamationCircleOutlined /> }
}

const STATUS_MAP = {
  0: { color: 'processing', text: '待处理', action: '处理' },
  1: { color: 'success', text: '已采购', icon: <CheckOutlined />, action: null },
  2: { color: 'default', text: '已忽略', icon: <CloseOutlined />, action: null }
}

const PART_TYPE_OPTIONS = [
  { value: 'fan', label: '风扇' },
  { value: 'capacitor', label: '电容' },
  { value: 'board', label: '板卡' },
  { value: 'other', label: '其他' }
]

const URGENCY_OPTIONS = [
  { value: 1, label: '一般' },
  { value: 2, label: '紧急' },
  { value: 3, label: '非常紧急' }
]

const STATUS_OPTIONS = [
  { value: 0, label: '待处理' },
  { value: 1, label: '已采购' },
  { value: 2, label: '已忽略' }
]

const PurchaseSuggestionList = () => {
  const actionRef = useRef()
  const [form] = Form.useForm()

  const [processVisible, setProcessVisible] = useState(false)
  const [currentItem, setCurrentItem] = useState(null)
  const [processType, setProcessType] = useState(null)
  const [selectedRowKeys, setSelectedRowKeys] = useState([])
  const [pendingCount, setPendingCount] = useState(0)
  const [loading, setLoading] = useState(false)
  const [generating, setGenerating] = useState(false)

  useEffect(() => {
    fetchPendingCount()
  }, [])

  const fetchPendingCount = async () => {
    try {
      const res = await getPendingSuggestionCount()
      setPendingCount(res.data || 0)
    } catch (e) {
      console.error('获取待处理数量失败', e)
    }
  }

  const handleGenerate = async () => {
    Modal.confirm({
      title: '确认生成采购建议',
      icon: <ExclamationCircleOutlined />,
      content: '系统将自动分析低库存备件并生成采购建议，是否继续？',
      okText: '生成',
      okType: 'primary',
      cancelText: '取消',
      onOk: async () => {
        setGenerating(true)
        try {
          await generatePurchaseSuggestions()
          message.success('采购建议生成成功')
          actionRef.current?.reload()
          fetchPendingCount()
        } catch (e) {
          message.error('生成失败')
        } finally {
          setGenerating(false)
        }
      }
    })
  }

  const handleProcess = (record, type) => {
    setCurrentItem(record)
    setProcessType(type)
    form.resetFields()
    setProcessVisible(true)
  }

  const handleProcessSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)
      await processPurchaseSuggestion(
        currentItem.id,
        processType,
        values.processorName,
        values.remark
      )
      message.success(processType === 1 ? '已标记为已采购' : '已忽略')
      setProcessVisible(false)
      actionRef.current?.reload()
      fetchPendingCount()
    } catch (e) {
      console.error('处理失败', e)
    } finally {
      setLoading(false)
    }
  }

  const handleBatchProcess = (type) => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要处理的建议')
      return
    }

    Modal.confirm({
      title: type === 1 ? '批量标记为已采购' : '批量忽略',
      content: `确定要将选中的 ${selectedRowKeys.length} 条建议${type === 1 ? '标记为已采购' : '忽略'}吗？`,
      okText: '确定',
      okType: type === 2 ? 'danger' : 'primary',
      cancelText: '取消',
      onOk: async () => {
        try {
          await batchProcessPurchaseSuggestions(selectedRowKeys, type, '管理员')
          message.success(`成功处理 ${selectedRowKeys.length} 条建议`)
          setSelectedRowKeys([])
          actionRef.current?.reload()
          fetchPendingCount()
        } catch (e) {
          message.error('批量处理失败')
        }
      }
    })
  }

  const getRowClassName = (record) => {
    if (record.status === 0) {
      if (record.urgency === 3) return 'row-urgent'
      if (record.urgency === 2) return 'row-warning'
    }
    return ''
  }

  const columns = [
    {
      title: '建议单号',
      dataIndex: 'suggestionNo',
      key: 'suggestionNo',
      width: 160,
      render: (text) => <span style={{ fontFamily: 'monospace' }}>{text}</span>
    },
    {
      title: '紧急程度',
      dataIndex: 'urgency',
      key: 'urgency',
      width: 110,
      render: (val) => {
        const urgency = URGENCY_MAP[val]
        return (
          <Badge
            status={val === 3 ? 'error' : val === 2 ? 'warning' : 'default'}
            text={
              <Tag color={urgency?.color}>
                {urgency?.icon} {urgency?.text}
              </Tag>
            }
          />
        )
      },
      sorter: (a, b) => b.urgency - a.urgency
    },
    {
      title: '备件信息',
      key: 'partInfo',
      width: 250,
      render: (_, record) => (
        <div>
          <div style={{ fontWeight: 500 }}>{record.partName}</div>
          <div style={{ color: '#999', fontSize: 12 }}>
            编号: {record.partCode} | 型号: {record.partModel}
          </div>
        </div>
      )
    },
    {
      title: '类型',
      dataIndex: 'partType',
      key: 'partType',
      width: 80,
      render: (val) => {
        const type = PART_TYPE_MAP[val] || PART_TYPE_MAP.other
        return <Tag color={type.color}>{type.text}</Tag>
      }
    },
    {
      title: '当前库存',
      dataIndex: 'currentQuantity',
      key: 'currentQuantity',
      width: 100,
      align: 'right',
      render: (val, record) => (
        <span style={{
          color: val <= 0 ? '#ff4d4f' : val < record.safeQuantity ? '#faad14' : undefined,
          fontWeight: val < record.safeQuantity ? 600 : undefined
        }}>
          {val} {record.unit}
        </span>
      )
    },
    {
      title: '安全库存',
      dataIndex: 'safeQuantity',
      key: 'safeQuantity',
      width: 100,
      align: 'right',
      render: (val, record) => `${val} ${record.unit}`
    },
    {
      title: '建议采购量',
      dataIndex: 'suggestQuantity',
      key: 'suggestQuantity',
      width: 110,
      align: 'right',
      render: (val, record) => (
        <Tooltip title={`最小采购量: ${record.minPurchaseQuantity}`}>
          <span style={{ color: '#1890ff', fontWeight: 600 }}>{val}</span>
          {record.unit}
        </Tooltip>
      )
    },
    {
      title: '单价',
      dataIndex: 'unitPrice',
      key: 'unitPrice',
      width: 100,
      align: 'right',
      render: (val) => val ? `¥${Number(val).toLocaleString()}` : '-'
    },
    {
      title: '预估金额',
      dataIndex: 'estimatedAmount',
      key: 'estimatedAmount',
      width: 120,
      align: 'right',
      render: (val) => val ? `¥${Number(val).toLocaleString()}` : '-'
    },
    {
      title: '供应商',
      dataIndex: 'supplier',
      key: 'supplier',
      width: 150,
      ellipsis: true
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
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
      title: '生成时间',
      dataIndex: 'generateTime',
      key: 'generateTime',
      width: 170,
      render: (val) => dayjs(val).format('YYYY-MM-DD HH:mm')
    },
    {
      title: '处理人',
      dataIndex: 'processorName',
      key: 'processorName',
      width: 100
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right',
      render: (_, record) => {
        if (record.status !== 0) return null
        return (
          <Space size="small">
            <Button
              type="link"
              size="small"
              icon={<CheckOutlined />}
              onClick={() => handleProcess(record, 1)}
            >
              已采购
            </Button>
            <Button
              type="link"
              size="small"
              danger
              icon={<CloseOutlined />}
              onClick={() => handleProcess(record, 2)}
            >
              忽略
            </Button>
          </Space>
        )
      }
    }
  ]

  return (
    <div className="purchase-suggestion-page" style={{ padding: 16 }}>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={8} lg={6}>
          <Card>
            <div className="stat-item">
              <div className="stat-icon" style={{ backgroundColor: '#f6ffed', color: '#52c41a' }}>
                <ShoppingCartOutlined />
              </div>
              <div>
                <div className="stat-title">待处理建议</div>
                <div className="stat-value" style={{ color: '#52c41a' }}>{pendingCount}</div>
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={6}>
          <Card>
            <div className="stat-item">
              <div className="stat-icon" style={{ backgroundColor: '#fff1f0', color: '#ff4d4f' }}>
                <ExclamationCircleOutlined />
              </div>
              <div>
                <div className="stat-title">非常紧急</div>
                <div className="stat-value" style={{ color: '#ff4d4f' }}>--</div>
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={6}>
          <Card>
            <div className="stat-item">
              <div className="stat-icon" style={{ backgroundColor: '#fffbe6', color: '#faad14' }}>
                <WarningOutlined />
              </div>
              <div>
                <div className="stat-title">紧急</div>
                <div className="stat-value" style={{ color: '#faad14' }}>--</div>
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={6}>
          <Card>
            <div className="stat-item">
              <div className="stat-icon" style={{ backgroundColor: '#f0f5ff', color: '#722ed1' }}>
                <ClockCircleOutlined />
              </div>
              <div>
                <div className="stat-title">一般</div>
                <div className="stat-value" style={{ color: '#722ed1' }}>--</div>
              </div>
            </div>
          </Card>
        </Col>
      </Row>

      <Card style={{ marginTop: 16 }}>
        <ProTable
          rowKey="id"
          actionRef={actionRef}
          columns={columns}
          request={async (params) => {
            const queryParams = {
              pageNum: params.current,
              pageSize: params.pageSize,
              keyword: params.keyword,
              partType: params.partType,
              urgency: params.urgency,
              status: params.status !== undefined ? params.status : 0
            }
            const res = await getPurchaseSuggestionList(queryParams)
            const data = res.data?.list || []

            const veryUrgent = data.filter(d => d.urgency === 3 && d.status === 0).length
            const urgent = data.filter(d => d.urgency === 2 && d.status === 0).length
            const normal = data.filter(d => d.urgency === 1 && d.status === 0).length

            const cards = document.querySelectorAll('.stat-value')
            if (cards[1]) cards[1].textContent = veryUrgent
            if (cards[2]) cards[2].textContent = urgent
            if (cards[3]) cards[3].textContent = normal

            return {
              data,
              success: true,
              total: res.data?.total || 0
            }
          }}
          rowClassName={getRowClassName}
          rowSelection={{
            selectedRowKeys,
            onChange: (keys) => setSelectedRowKeys(keys),
            getCheckboxProps: (record) => ({
              disabled: record.status !== 0
            })
          }}
          headerTitle={
            <Space>
              <span>采购建议列表</span>
              <Badge count={pendingCount} size="small">
                <Tag color="processing">待处理</Tag>
              </Badge>
            </Space>
          }
          toolBarRender={() => [
            <Button
              key="generate"
              type="primary"
              icon={<ReloadOutlined spin={generating} />}
              onClick={handleGenerate}
              loading={generating}
            >
              生成采购建议
            </Button>,
            <Button
              key="purchased"
              icon={<CheckOutlined />}
              onClick={() => handleBatchProcess(1)}
              disabled={selectedRowKeys.length === 0}
            >
              批量标记已采购
            </Button>,
            <Button
              key="ignore"
              danger
              icon={<CloseOutlined />}
              onClick={() => handleBatchProcess(2)}
              disabled={selectedRowKeys.length === 0}
            >
              批量忽略
            </Button>,
            <Button
              key="export"
              icon={<DownloadOutlined />}
            >
              导出
            </Button>
          ]}
          search={{
            labelWidth: 'auto',
            defaultCollapsed: false,
            initialValues: { status: 0 }
          }}
          form={{
            syncToUrl: true
          }}
          defaultSize="small"
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
        title={processType === 1 ? '标记为已采购' : '忽略此建议'}
        open={processVisible}
        onOk={handleProcessSubmit}
        onCancel={() => setProcessVisible(false)}
        confirmLoading={loading}
        okText="确定"
        okButtonProps={{ danger: processType === 2 }}
        cancelText="取消"
      >
        {currentItem && (
          <div>
            <Card size="small" style={{ marginBottom: 16 }}>
              <Row gutter={16}>
                <Col span={12}>
                  <p style={{ marginBottom: 4 }}>
                    <strong>备件名称:</strong> {currentItem.partName}
                  </p>
                  <p style={{ marginBottom: 4 }}>
                    <strong>备件编号:</strong> {currentItem.partCode}
                  </p>
                  <p style={{ marginBottom: 4 }}>
                    <strong>型号:</strong> {currentItem.partModel}
                  </p>
                </Col>
                <Col span={12}>
                  <p style={{ marginBottom: 4 }}>
                    <strong>当前库存:</strong>
                    <span style={{
                      color: currentItem.currentQuantity <= 0 ? '#ff4d4f' : '#faad14',
                      fontWeight: 600,
                      marginLeft: 8
                    }}>
                      {currentItem.currentQuantity} {currentItem.unit}
                    </span>
                  </p>
                  <p style={{ marginBottom: 4 }}>
                    <strong>建议采购:</strong>
                    <span style={{ color: '#1890ff', fontWeight: 600, marginLeft: 8 }}>
                      {currentItem.suggestQuantity} {currentItem.unit}
                    </span>
                  </p>
                  <p style={{ marginBottom: 4 }}>
                    <strong>预估金额:</strong>
                    <span style={{ color: '#52c41a', fontWeight: 600, marginLeft: 8 }}>
                      ¥{currentItem.estimatedAmount?.toLocaleString()}
                    </span>
                  </p>
                </Col>
              </Row>
            </Card>
            <Form form={form} layout="vertical">
              <Form.Item
                name="processorName"
                label="处理人姓名"
                initialValue="管理员"
              >
                <Input placeholder="请输入处理人姓名" />
              </Form.Item>
              <Form.Item
                name="remark"
                label={processType === 1 ? '采购备注' : '忽略原因'}
              >
                <TextArea
                  rows={3}
                  placeholder={processType === 1
                    ? '请输入采购单号、供应商等备注信息'
                    : '请输入忽略此建议的原因'
                  }
                />
              </Form.Item>
            </Form>
          </div>
        )}
      </Modal>

      <style>{`
        .row-urgent {
          background-color: #fff1f0 !important;
        }
        .row-urgent:hover > td {
          background-color: #fff1f0 !important;
        }
        .row-warning {
          background-color: #fffbe6 !important;
        }
        .row-warning:hover > td {
          background-color: #fffbe6 !important;
        }
        .stat-item {
          display: flex;
          align-items: center;
          gap: 16px;
        }
        .stat-icon {
          width: 48px;
          height: 48px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 24px;
        }
        .stat-title {
          color: #666;
          font-size: 13px;
          margin-bottom: 4px;
        }
        .stat-value {
          font-size: 24px;
          font-weight: 600;
        }
      `}</style>
    </div>
  )
}

export default PurchaseSuggestionList
