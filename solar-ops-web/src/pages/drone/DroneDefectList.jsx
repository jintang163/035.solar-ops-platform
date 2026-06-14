import React, { useState, useEffect } from 'react'
import {
  Table,
  Button,
  Space,
  Select,
  Input,
  Tag,
  Card,
  Modal,
  Form,
  Row,
  Col,
  Descriptions,
  message,
  Popconfirm,
  Checkbox,
  Badge,
  Tooltip
} from 'antd'
import {
  EyeOutlined,
  CheckCircleOutlined,
  FileTextOutlined,
  FilterOutlined,
  PictureOutlined
} from '@ant-design/icons'
import {
  getDroneDefectPage,
  getDroneDefectDetail,
  verifyDroneDefect,
  batchVerifyDroneDefect,
  createWorkOrderFromDefect,
  handleDroneDefect
} from '../../api/drone'

const { Option } = Select
const { Search } = Input

const defectTypeColors = {
  hot_spot: '#ff4d4f',
  microcrack: '#faad14',
  shadow: '#8c8c8c',
  delamination: '#eb2f96',
  broken: '#f5222d',
  dirt: '#fa8c16'
}

const defectTypeNames = {
  hot_spot: '热斑',
  microcrack: '隐裂',
  shadow: '遮挡',
  delamination: '脱层',
  broken: '破损',
  dirt: '脏污'
}

const levelColors = ['', 'blue', 'orange', 'red', 'magenta']
const levelNames = ['', '轻微', '一般', '严重', '紧急']

const statusColors = ['default', 'processing', 'success', 'warning']
const statusNames = ['待处理', '处理中', '已修复', '已忽略']

const DroneDefectList = () => {
  const [defects, setDefects] = useState([])
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })
  const [loading, setLoading] = useState(false)
  const [selectedRowKeys, setSelectedRowKeys] = useState([])
  const [detailVisible, setDetailVisible] = useState(false)
  const [currentDefect, setCurrentDefect] = useState(null)
  const [filters, setFilters] = useState({
    defectType: undefined,
    defectLevel: undefined,
    status: undefined,
    verified: undefined
  })
  const [form] = Form.useForm()

  const loadDefects = async (page = 1, pageSize = 10) => {
    setLoading(true)
    try {
      const params = {
        pageNum: page,
        pageSize: pageSize,
        ...filters
      }
      const res = await getDroneDefectPage(params)
      setDefects(res.data?.list || [])
      setPagination({
        current: page,
        pageSize: pageSize,
        total: res.data?.total || 0
      })
    } catch (e) {
      message.error('加载缺陷列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadDefects()
  }, [filters])

  const handleViewDetail = async (id) => {
    try {
      const res = await getDroneDefectDetail(id)
      setCurrentDefect(res.data)
      setDetailVisible(true)
    } catch (e) {
      message.error('加载详情失败')
    }
  }

  const handleVerify = async (id) => {
    try {
      await verifyDroneDefect(id, 1)
      message.success('确认成功')
      loadDefects(pagination.current, pagination.pageSize)
    } catch (e) {
      message.error('确认失败')
    }
  }

  const handleBatchVerify = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择缺陷记录')
      return
    }
    try {
      await batchVerifyDroneDefect(selectedRowKeys, 1)
      message.success(`批量确认成功，共 ${selectedRowKeys.length} 条`)
      setSelectedRowKeys([])
      loadDefects(pagination.current, pagination.pageSize)
    } catch (e) {
      message.error('批量确认失败')
    }
  }

  const handleCreateWorkOrder = async (defect) => {
    try {
      const workorderId = await createWorkOrderFromDefect(defect.id, 1, '管理员')
      message.success(`工单创建成功，工单ID: ${workorderId}`)
      loadDefects(pagination.current, pagination.pageSize)
    } catch (e) {
      message.error('创建工单失败')
    }
  }

  const handleStatusChange = async (defectId, status) => {
    try {
      await handleDroneDefect({
        id: defectId,
        status: status
      })
      message.success('状态更新成功')
      loadDefects(pagination.current, pagination.pageSize)
    } catch (e) {
      message.error('状态更新失败')
    }
  }

  const columns = [
    {
      title: '缺陷编号',
      dataIndex: 'defectCode',
      width: 140,
      render: (text) => <Tag color="blue">{text || '-'}</Tag>
    },
    {
      title: '缺陷类型',
      dataIndex: 'defectType',
      width: 100,
      render: (type) => (
        <Tag color={defectTypeColors[type]}>{defectTypeNames[type] || type}</Tag>
      ),
      filters: Object.keys(defectTypeNames).map(k => ({
        text: defectTypeNames[k],
        value: k
      })),
      filteredValue: filters.defectType ? [filters.defectType] : null,
      onFilter: (value, record) => record.defectType === value
    },
    {
      title: '等级',
      dataIndex: 'defectLevel',
      width: 80,
      render: (level, record) => (
        <Tag color={levelColors[level]}>{record.defectLevelDesc || levelNames[level]}</Tag>
      )
    },
    {
      title: '置信度',
      dataIndex: 'confidence',
      width: 90,
      render: (val) => `${(val * 100).toFixed(1)}%`
    },
    {
      title: '组件坐标',
      dataIndex: 'center',
      width: 120,
      render: (val) => val ? `(${val[0]}, ${val[1]})` : '-'
    },
    {
      title: '温度',
      dataIndex: 'temperature',
      width: 80,
      render: (val) => {
        if (!val) return '-'
        return <span style={{ color: '#f5222d', fontWeight: 'bold' }}>{val}℃</span>
      }
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (status, record) => (
        <Select
          size="small"
          value={status}
          style={{ width: 90 }}
          onChange={(s) => handleStatusChange(record.id, s)}
        >
          <Option value={0}>待处理</Option>
          <Option value={1}>处理中</Option>
          <Option value={2}>已修复</Option>
          <Option value={3}>已忽略</Option>
        </Select>
      )
    },
    {
      title: '已确认',
      dataIndex: 'verified',
      width: 80,
      render: (val) => (
        <Checkbox checked={val === 1} disabled />
      )
    },
    {
      title: '工单',
      dataIndex: 'workorderId',
      width: 80,
      render: (val) => val ? (
        <Tooltip title={`工单ID: ${val}`}>
          <Tag color="orange">#{val}</Tag>
        </Tooltip>
      ) : '-'
    },
    {
      title: '描述',
      dataIndex: 'description',
      ellipsis: true
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 160
    },
    {
      title: '操作',
      width: 180,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record.id)}
          >
            详情
          </Button>
          {record.verified !== 1 && (
            <Button
              type="link"
              size="small"
              icon={<CheckCircleOutlined />}
              onClick={() => handleVerify(record.id)}
            >
              确认
            </Button>
          )}
          {!record.workorderId && (
            <Button
              type="link"
              size="small"
              icon={<FileTextOutlined />}
              onClick={() => handleCreateWorkOrder(record)}
            >
              工单
            </Button>
          )}
        </Space>
      )
    }
  ]

  const rowSelection = {
    selectedRowKeys,
    onChange: setSelectedRowKeys
  }

  return (
    <div className="drone-defect-page">
      <Card>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
          <Space>
            <Select
              placeholder="缺陷类型"
              allowClear
              style={{ width: 130 }}
              value={filters.defectType}
              onChange={(v) => setFilters({ ...filters, defectType: v })}
            >
              {Object.keys(defectTypeNames).map(k => (
                <Option key={k} value={k}>{defectTypeNames[k]}</Option>
              ))}
            </Select>
            <Select
              placeholder="缺陷等级"
              allowClear
              style={{ width: 120 }}
              value={filters.defectLevel}
              onChange={(v) => setFilters({ ...filters, defectLevel: v })}
            >
              <Option value={1}>轻微</Option>
              <Option value={2}>一般</Option>
              <Option value={3}>严重</Option>
              <Option value={4}>紧急</Option>
            </Select>
            <Select
              placeholder="处理状态"
              allowClear
              style={{ width: 120 }}
              value={filters.status}
              onChange={(v) => setFilters({ ...filters, status: v })}
            >
              <Option value={0}>待处理</Option>
              <Option value={1}>处理中</Option>
              <Option value={2}>已修复</Option>
              <Option value={3}>已忽略</Option>
            </Select>
            <Select
              placeholder="确认状态"
              allowClear
              style={{ width: 120 }}
              value={filters.verified}
              onChange={(v) => setFilters({ ...filters, verified: v })}
            >
              <Option value={0}>未确认</Option>
              <Option value={1}>已确认</Option>
            </Select>
            <Button icon={<FilterOutlined />} onClick={() => loadDefects()}>
              查询
            </Button>
            <Button onClick={() => {
              setFilters({ defectType: undefined, defectLevel: undefined, status: undefined, verified: undefined })
            }}>
              重置
            </Button>
          </Space>
          <Space>
            <Badge count={selectedRowKeys.length}>
              <Button
                type="primary"
                icon={<CheckCircleOutlined />}
                onClick={handleBatchVerify}
                disabled={selectedRowKeys.length === 0}
              >
                批量确认
              </Button>
            </Badge>
          </Space>
        </div>

        <Table
          rowSelection={rowSelection}
          columns={columns}
          dataSource={defects}
          rowKey="id"
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`,
            onChange: (page, pageSize) => loadDefects(page, pageSize)
          }}
          scroll={{ x: 1300 }}
        />
      </Card>

      <Modal
        title="缺陷详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={700}
      >
        {currentDefect && (
          <div>
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="缺陷编号" span={2}>
                {currentDefect.defectCode || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="缺陷类型">
                <Tag color={defectTypeColors[currentDefect.defectType]}>
                  {defectTypeNames[currentDefect.defectType]}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="缺陷等级">
                <Tag color={levelColors[currentDefect.defectLevel]}>
                  {currentDefect.defectLevelDesc || levelNames[currentDefect.defectLevel]}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="置信度">
                {(currentDefect.confidence * 100).toFixed(2)}%
              </Descriptions.Item>
              <Descriptions.Item label="缺陷占比">
                {currentDefect.areaRatio?.toFixed(2)}%
              </Descriptions.Item>
              <Descriptions.Item label="中心坐标">
                {currentDefect.center
                  ? `(${currentDefect.center[0]}, ${currentDefect.center[1]})`
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="边界框">
                {currentDefect.bbox
                  ? `(${currentDefect.bbox[0]}, ${currentDefect.bbox[1]}) - (${currentDefect.bbox[2]}, ${currentDefect.bbox[3]})`
                  : '-'}
              </Descriptions.Item>
              {currentDefect.temperature && (
                <>
                  <Descriptions.Item label="温度">
                    <span style={{ color: '#f5222d', fontWeight: 'bold' }}>
                      {currentDefect.temperature}℃
                    </span>
                  </Descriptions.Item>
                  {currentDefect.deltaTemperature && (
                    <Descriptions.Item label="温度差">
                      <span style={{ color: '#f5222d', fontWeight: 'bold' }}>
                        {currentDefect.deltaTemperature}℃
                      </span>
                    </Descriptions.Item>
                  )}
                </>
              )}
              <Descriptions.Item label="GPS坐标">
                {currentDefect.gpsLongitude && currentDefect.gpsLatitude
                  ? `(${currentDefect.gpsLongitude}, ${currentDefect.gpsLatitude})`
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={statusColors[currentDefect.status]}>
                  {currentDefect.statusDesc || statusNames[currentDefect.status]}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="已确认">
                {currentDefect.verified === 1 ? (
                  <Tag color="success">是</Tag>
                ) : (
                  <Tag color="default">否</Tag>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="关联工单">
                {currentDefect.workorderId ? (
                  <Tag color="orange">工单#{currentDefect.workorderId}</Tag>
                ) : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="缺陷描述" span={2}>
                {currentDefect.description || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="处理建议" span={2}>
                {currentDefect.suggestion || '-'}
              </Descriptions.Item>
            </Descriptions>

            <Row style={{ marginTop: 16, textAlign: 'right' }}>
              <Space>
                {currentDefect.verified !== 1 && (
                  <Button
                    type="primary"
                    icon={<CheckCircleOutlined />}
                    onClick={() => {
                      handleVerify(currentDefect.id)
                      setDetailVisible(false)
                    }}
                  >
                    人工确认
                  </Button>
                )}
                {!currentDefect.workorderId && (
                  <Button
                    type="primary"
                    icon={<FileTextOutlined />}
                    onClick={() => {
                      handleCreateWorkOrder(currentDefect)
                      setDetailVisible(false)
                    }}
                  >
                    生成工单
                  </Button>
                )}
                <Button onClick={() => setDetailVisible(false)}>关闭</Button>
              </Space>
            </Row>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default DroneDefectList
