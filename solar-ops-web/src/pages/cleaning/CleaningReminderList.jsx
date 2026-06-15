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
  message,
  Tag,
  Drawer,
  Descriptions,
  Row,
  Col,
  Statistic,
  Tooltip,
  Alert
} from 'antd'
import {
  EyeOutlined,
  CheckOutlined,
  CloseOutlined,
  PlusCircleOutlined,
  ThunderboltOutlined,
  WarningOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  getReminderPage,
  getReminderDetail,
  ignoreReminder,
  generateReminders
} from '../../api/cleaning'
import { getUser } from '../../utils/auth'

const { RangePicker } = DatePicker
const { Option } = Select

const DUST_LEVEL_MAP = {
  0: { color: '#52c41a', text: '无积灰', level: 0 },
  1: { color: '#faad14', text: '轻度积灰', level: 1 },
  2: { color: '#fa8c16', text: '中度积灰', level: 2 },
  3: { color: '#ff4d4f', text: '重度积灰', level: 3 }
}

const STATUS_MAP = {
  0: { color: 'orange', text: '未处理' },
  1: { color: 'green', text: '已创建计划' },
  2: { color: 'default', text: '已忽略' }
}

const PAGE_SIZE = 10

const CleaningReminderList = ({ onCreatePlan }) => {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [queryForm] = Form.useForm()
  const [activeStatus, setActiveStatus] = useState('all')

  const [detailVisible, setDetailVisible] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [currentReminder, setCurrentReminder] = useState(null)

  const [generateLoading, setGenerateLoading] = useState(false)

  const [statusStats, setStatusStats] = useState({
    unhandled: 0,
    processed: 0,
    ignored: 0,
    heavy: 0,
    moderate: 0
  })

  const getCurrentUser = () => {
    const user = getUser() || {}
    return { id: user.id, name: user.name || user.username || '管理员' }
  }

  const fetchData = useCallback(async (status = 'all', page = 1, extraParams = {}) => {
    setLoading(true)
    try {
      const params = { pageNum: page, pageSize: PAGE_SIZE, ...extraParams }
      if (status !== 'all') {
        params.status = Number(status)
      }
      const res = await getReminderPage(params)
      const pageResult = res.data || {}
      setData(pageResult.list || [])
      setTotal(pageResult.total || 0)
      setPageNum(pageResult.pageNum || page)

      let unhandled = 0, processed = 0, ignored = 0, heavy = 0, moderate = 0
      ;(pageResult.list || []).forEach(r => {
        if (r.status === 0) unhandled++
        else if (r.status === 1) processed++
        else if (r.status === 2) ignored++

        if (r.dustLevel === 3) heavy++
        else if (r.dustLevel === 2) moderate++
      })
      setStatusStats(prev => ({
        ...prev,
        unhandled, processed, ignored, heavy, moderate
      }))
    } catch {
      setData([])
      setTotal(0)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData(activeStatus, 1)
  }, [activeStatus, fetchData])

  const handleQuery = async () => {
    try {
      const values = await queryForm.validateFields()
      const queryParams = { ...values }
      if (values.dateRange) {
        queryParams.startDate = values.dateRange[0].format('YYYY-MM-DD')
        queryParams.endDate = values.dateRange[1].format('YYYY-MM-DD')
        delete queryParams.dateRange
      }
      fetchData(activeStatus, 1, queryParams)
    } catch {
      // ignore
    }
  }

  const handleReset = () => {
    queryForm.resetFields()
    fetchData(activeStatus, 1)
  }

  const handlePageChange = (page) => {
    fetchData(activeStatus, page)
  }

  const handleViewDetail = async (record) => {
    setDetailLoading(true)
    setDetailVisible(true)
    try {
      const res = await getReminderDetail(record.id)
      setCurrentReminder(res.data || record)
    } catch {
      setCurrentReminder(record)
    } finally {
      setDetailLoading(false)
    }
  }

  const handleCreatePlan = (record) => {
    if (onCreatePlan) {
      onCreatePlan(record)
    } else {
      message.info('请在清洗计划页面创建计划')
    }
  }

  const handleIgnore = (record) => {
    Modal.confirm({
      title: '忽略提醒',
      content: `确定要忽略「${record.title}」这条清洗建议吗？忽略后将不再提示。`,
      okText: '确认忽略',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        const user = getCurrentUser()
        try {
          await ignoreReminder(record.id, {
            handlerId: user.id,
            handlerName: user.name
          })
          message.success('已忽略')
          fetchData(activeStatus, pageNum)
        } catch (error) {
          message.error(error.message || '操作失败')
        }
      }
    })
  }

  const handleBatchGenerate = async () => {
    Modal.confirm({
      title: '生成清洗建议',
      content: `将根据昨日（${dayjs().subtract(1, 'day').format('YYYY-MM-DD')}）的积灰检测数据生成清洗建议，确定继续？`,
      okText: '生成',
      cancelText: '取消',
      onOk: async () => {
        setGenerateLoading(true)
        try {
          const detectDate = dayjs().subtract(1, 'day').format('YYYY-MM-DD')
          const res = await generateReminders({ detectDate })
          const count = (res.data || []).length
          message.success(`生成完成，共生成 ${count} 条清洗建议`)
          fetchData(activeStatus, 1)
        } catch (error) {
          message.error(error.message || '生成失败')
        } finally {
          setGenerateLoading(false)
        }
      }
    })
  }

  const getLevelIcon = (level) => {
    const iconMap = {
      3: <ExclamationCircleOutlined />,
      2: <WarningOutlined />,
      1: <ClockCircleOutlined />,
      0: <CheckOutlined />
    }
    return iconMap[level] || null
  }

  const columns = [
    {
      title: '提醒编号',
      dataIndex: 'reminderNo',
      key: 'reminderNo',
      width: 160,
      render: (text, record) => (
        <a onClick={() => handleViewDetail(record)} style={{ color: '#1890ff' }}>{text}</a>
      )
    },
    {
      title: '建议标题',
      dataIndex: 'title',
      key: 'title',
      width: 200,
      ellipsis: true,
      render: (text, record) => {
        const levelInfo = DUST_LEVEL_MAP[record.dustLevel] || {}
        const isUrgent = record.dustLevel >= 2 && record.status === 0
        return (
          <Space>
            {isUrgent && (
              <Tooltip title="紧急建议">
                <Tag color="red" style={{ margin: 0 }}>!</Tag>
              </Tooltip>
            )}
            <span>{text}</span>
          </Space>
        )
      }
    },
    {
      title: '积灰等级',
      dataIndex: 'dustLevel',
      key: 'dustLevel',
      width: 120,
      render: (level) => {
        const info = DUST_LEVEL_MAP[level] || { color: 'default', text: level }
        return (
          <Tag color={info.color} icon={getLevelIcon(level)}>
            {info.text}
          </Tag>
        )
      },
      sorter: (a, b) => a.dustLevel - b.dustLevel
    },
    {
      title: '衰减率',
      dataIndex: 'attenuationRatePercent',
      key: 'attenuationRatePercent',
      width: 110,
      render: (val, record) => {
        const percent = val != null ? val : ((record.attenuationRate || 0) * 100).toFixed(2)
        const color = (record.dustLevel >= 2) ? '#ff4d4f' : (record.dustLevel === 1 ? '#faad14' : '#52c41a')
        return <span style={{ color, fontWeight: 500 }}>{percent}%</span>
      },
      sorter: (a, b) => (a.attenuationRate || 0) - (b.attenuationRate || 0)
    },
    {
      title: '日损失电量',
      dataIndex: 'estimatedDailyLoss',
      key: 'estimatedDailyLoss',
      width: 120,
      render: (val) => (
        <span style={{ color: '#fa541c' }}>
          <ThunderboltOutlined style={{ marginRight: 4 }} />
          {val || 0} kWh
        </span>
      ),
      sorter: (a, b) => (a.estimatedDailyLoss || 0) - (b.estimatedDailyLoss || 0)
    },
    {
      title: '所属电站',
      dataIndex: 'stationName',
      key: 'stationName',
      width: 140
    },
    {
      title: '方阵/逆变器',
      dataIndex: 'arrayNumber',
      key: 'arrayNumber',
      width: 120,
      render: (text, record) => text || record.inverterName || '-'
    },
    {
      title: '建议日期',
      dataIndex: 'suggestCleanDate',
      key: 'suggestCleanDate',
      width: 120,
      render: (val, record) => (
        <div>
          <div>{val}</div>
          {record.deadlineDate && (
            <div style={{ fontSize: 12, color: '#999' }}>
              截止: {record.deadlineDate}
            </div>
          )}
        </div>
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
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 160
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>
            详情
          </Button>
          {record.status === 0 && (
            <>
              <Button
                type="link"
                size="small"
                icon={<PlusCircleOutlined />}
                style={{ color: '#52c41a' }}
                onClick={() => handleCreatePlan(record)}
              >
                创建计划
              </Button>
              <Button
                type="link"
                size="small"
                icon={<CloseOutlined />}
                danger
                onClick={() => handleIgnore(record)}
              >
                忽略
              </Button>
            </>
          )}
        </Space>
      )
    }
  ]

  const totalCount = statusStats.unhandled + statusStats.processed + statusStats.ignored

  return (
    <div className="cleaning-reminder-page">
      <Card
        title="清洗提醒管理"
        extra={
          <Space>
            <Button
              type="primary"
              icon={<PlusCircleOutlined />}
              loading={generateLoading}
              onClick={handleBatchGenerate}
            >
              生成清洗建议
            </Button>
          </Space>
        }
      >
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message={
            <Space>
              <WarningOutlined />
              <span>系统每日凌晨自动检测积灰情况并生成清洗建议，中度及重度积灰将优先推送。</span>
            </Space>
          }
        />

        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col xs={12} sm={8} md={4}>
            <Card size="small">
              <Statistic
                title="全部提醒"
                value={totalCount}
                prefix={<ClockCircleOutlined />}
                valueStyle={{ fontSize: 20 }}
              />
            </Card>
          </Col>
          <Col xs={12} sm={8} md={4}>
            <Card size="small">
              <Statistic
                title="待处理"
                value={statusStats.unhandled}
                valueStyle={{ color: '#fa8c16', fontSize: 20 }}
                prefix={<WarningOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={8} md={4}>
            <Card size="small">
              <Statistic
                title="重度积灰"
                value={statusStats.heavy}
                valueStyle={{ color: '#ff4d4f', fontSize: 20, fontWeight: 700 }}
                prefix={<ExclamationCircleOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={8} md={4}>
            <Card size="small">
              <Statistic
                title="中度积灰"
                value={statusStats.moderate}
                valueStyle={{ color: '#fa8c16', fontSize: 20 }}
                prefix={<ExclamationCircleOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={8} md={4}>
            <Card size="small">
              <Statistic
                title="已创建计划"
                value={statusStats.processed}
                valueStyle={{ color: '#52c41a', fontSize: 20 }}
                prefix={<CheckOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={8} md={4}>
            <Card size="small">
              <Statistic
                title="已忽略"
                value={statusStats.ignored}
                valueStyle={{ color: '#999', fontSize: 20 }}
                prefix={<CloseOutlined />}
              />
            </Card>
          </Col>
        </Row>

        <div style={{ marginBottom: 16 }}>
          <Space wrap>
            <Button
              type={activeStatus === 'all' ? 'primary' : 'default'}
              onClick={() => setActiveStatus('all')}
            >
              全部 ({totalCount})
            </Button>
            <Button
              type={activeStatus === '0' ? 'primary' : 'default'}
              onClick={() => setActiveStatus('0')}
              danger={statusStats.unhandled > 0}
            >
              未处理 ({statusStats.unhandled})
            </Button>
            <Button
              type={activeStatus === '1' ? 'primary' : 'default'}
              onClick={() => setActiveStatus('1')}
            >
              已创建计划 ({statusStats.processed})
            </Button>
            <Button
              type={activeStatus === '2' ? 'primary' : 'default'}
              onClick={() => setActiveStatus('2')}
            >
              已忽略 ({statusStats.ignored})
            </Button>
          </Space>
        </div>

        <Form form={queryForm} layout="inline" style={{ marginBottom: 16 }}>
          <Form.Item name="dustLevel" label="积灰等级">
            <Select placeholder="全部" allowClear style={{ width: 140 }}>
              {Object.entries(DUST_LEVEL_MAP).map(([key, val]) => (
                <Option key={key} value={Number(key)}>{val.text}</Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="dateRange" label="建议日期">
            <RangePicker />
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
          dataSource={data}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1500 }}
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

      <Drawer
        title="清洗建议详情"
        width={640}
        onClose={() => setDetailVisible(false)}
        open={detailVisible}
        loading={detailLoading}
        extra={
          currentReminder?.status === 0 && (
            <Space>
              <Button onClick={() => { setDetailVisible(false); handleIgnore(currentReminder) }} danger>
                <CloseOutlined /> 忽略
              </Button>
              <Button
                type="primary"
                icon={<PlusCircleOutlined />}
                onClick={() => { setDetailVisible(false); handleCreatePlan(currentReminder) }}
              >
                创建清洗计划
              </Button>
            </Space>
          )
        }
      >
        {currentReminder && (
          <>
            <Alert
              type={currentReminder.dustLevel >= 2 ? 'error' : currentReminder.dustLevel === 1 ? 'warning' : 'success'}
              showIcon
              icon={<ExclamationCircleOutlined />}
              style={{ marginBottom: 16 }}
              message={
                <Space>
                  <Tag color={(DUST_LEVEL_MAP[currentReminder.dustLevel] || {}).color}>
                    {(DUST_LEVEL_MAP[currentReminder.dustLevel] || {}).text || '未知'}
                  </Tag>
                  <span>{currentReminder.title}</span>
                </Space>
              }
              description={currentReminder.description}
            />

            <Descriptions title="详细信息" bordered column={2} size="small" style={{ marginBottom: 24 }}>
              <Descriptions.Item label="提醒编号">{currentReminder.reminderNo}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={(STATUS_MAP[currentReminder.status] || {}).color}>
                  {(STATUS_MAP[currentReminder.status] || {}).text || currentReminder.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="所属电站">{currentReminder.stationName || '-'}</Descriptions.Item>
              <Descriptions.Item label="方阵/逆变器">
                {currentReminder.arrayNumber || currentReminder.inverterName || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="积灰等级">
                <Tag color={(DUST_LEVEL_MAP[currentReminder.dustLevel] || {}).color}>
                  {(DUST_LEVEL_MAP[currentReminder.dustLevel] || {}).text || '-'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="衰减率">
                <span style={{ fontWeight: 500 }}>
                  {currentReminder.attenuationRatePercent != null
                    ? currentReminder.attenuationRatePercent
                    : ((currentReminder.attenuationRate || 0) * 100).toFixed(2)}%
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="预估日损失电量">
                <span style={{ color: '#fa541c', fontWeight: 500 }}>
                  {currentReminder.estimatedDailyLoss || 0} kWh
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="建议清洗日期">{currentReminder.suggestCleanDate || '-'}</Descriptions.Item>
              <Descriptions.Item label="建议截止日期">{currentReminder.deadlineDate || '-'}</Descriptions.Item>
              <Descriptions.Item label="创建时间">{currentReminder.createTime || '-'}</Descriptions.Item>
              {currentReminder.status !== 0 && (
                <>
                  <Descriptions.Item label="处理人">{currentReminder.handlerName || '-'}</Descriptions.Item>
                  <Descriptions.Item label="处理时间">{currentReminder.handleTime || '-'}</Descriptions.Item>
                </>
              )}
              {currentReminder.cleaningPlanId && (
                <Descriptions.Item label="关联清洗计划ID" span={2}>
                  <a style={{ color: '#1890ff' }}>{currentReminder.cleaningPlanId}</a>
                </Descriptions.Item>
              )}
              <Descriptions.Item label="关联积灰记录ID" span={2}>
                {currentReminder.dustRecordId || '-'}
              </Descriptions.Item>
            </Descriptions>

            <Card size="small" type="inner" title="AI诊断建议">
              <ul style={{ paddingLeft: 20, margin: 0 }}>
                {currentReminder.dustLevel >= 2 && (
                  <li style={{ marginBottom: 8, color: '#ff4d4f' }}>
                    <strong>紧急：</strong>建议在 {currentReminder.deadlineDate || '3天'} 内尽快安排清洗
                  </li>
                )}
                {currentReminder.dustLevel === 1 && (
                  <li style={{ marginBottom: 8, color: '#fa8c16' }}>
                    建议在 {currentReminder.deadlineDate || '14天'} 内安排清洗，以避免发电量进一步衰减
                  </li>
                )}
                <li style={{ marginBottom: 8 }}>
                  预估每日损失电量约 <strong>{currentReminder.estimatedDailyLoss || 0} kWh</strong>，
                  若按 0.5元/kWh 计算，日损失金额约 {(currentReminder.estimatedDailyLoss * 0.5).toFixed(2)} 元
                </li>
                <li style={{ marginBottom: 8 }}>
                  清洗后预计可恢复发电量约 <strong>{currentReminder.attenuationRatePercent != null
                    ? currentReminder.attenuationRatePercent
                    : ((currentReminder.attenuationRate || 0) * 100).toFixed(2)}%</strong>
                </li>
                <li>
                  可结合天气预报，优先选择晴天清洗，避免雨天后再次积灰
                </li>
              </ul>
            </Card>
          </>
        )}
      </Drawer>
    </div>
  )
}

export default CleaningReminderList
