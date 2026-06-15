import React, { useState, useEffect, useCallback } from 'react'
import {
  Calendar,
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
  Drawer,
  Descriptions,
  Row,
  Col,
  Badge,
  Tooltip,
  Image,
  Upload,
  Progress,
  Statistic
} from 'antd'
import {
  PlusOutlined,
  CalendarOutlined,
  UnorderedListOutlined,
  EyeOutlined,
  PlayCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  UploadOutlined,
  ArrowUpOutlined,
  CloudUploadOutlined
} from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  getCleaningPlanPage,
  getCleaningPlanCalendar,
  getCleaningPlanDetail,
  createCleaningPlan,
  updateCleaningPlan,
  startCleaningPlan,
  completeCleaningPlan,
  cancelCleaningPlan,
  uploadCleaningPhotos
} from '../../api/cleaning'
import { getUser } from '../../utils/auth'

const { RangePicker } = DatePicker
const { TextArea } = Input
const { Option } = Select

const STATUS_MAP = {
  0: { color: 'orange', text: '待执行' },
  1: { color: 'processing', text: '执行中' },
  2: { color: 'green', text: '已完成' },
  3: { color: 'default', text: '已取消' }
}

const CLEANING_METHODS = ['人工清洗', '机械清洗', '机器人清洗', '无水清洗']

const PAGE_SIZE = 10

const CleaningPlanManagement = () => {
  const [viewMode, setViewMode] = useState('calendar')
  const [loading, setLoading] = useState(false)
  const [planList, setPlanList] = useState([])
  const [calendarPlans, setCalendarPlans] = useState([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [queryForm] = Form.useForm()
  const [selectedMonth, setSelectedMonth] = useState(dayjs())

  const [addVisible, setAddVisible] = useState(false)
  const [addLoading, setAddLoading] = useState(false)
  const [addForm] = Form.useForm()
  const [editingPlan, setEditingPlan] = useState(null)

  const [detailVisible, setDetailVisible] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [currentPlan, setCurrentPlan] = useState(null)

  const [executeVisible, setExecuteVisible] = useState(false)
  const [executeMode, setExecuteMode] = useState('start')
  const [executeLoading, setExecuteLoading] = useState(false)
  const [executeForm] = Form.useForm()

  const [statusCount, setStatusCount] = useState({ PENDING: 0, IN_PROGRESS: 0, COMPLETED: 0, CANCELLED: 0 })

  const getCurrentUser = () => {
    const user = getUser() || {}
    return { id: user.id, name: user.name || user.username || '管理员' }
  }

  const fetchPlanList = useCallback(async (page = 1, values = {}) => {
    setLoading(true)
    try {
      const params = { pageNum: page, pageSize: PAGE_SIZE, ...values }
      if (values.startDate) {
        params.startDate = values.startDate
      }
      if (values.endDate) {
        params.endDate = values.endDate
      }
      const res = await getCleaningPlanPage(params)
      const pageResult = res.data || {}
      setPlanList(pageResult.list || [])
      setTotal(pageResult.total || 0)
      setPageNum(pageResult.pageNum || page)

      const counts = { PENDING: 0, IN_PROGRESS: 0, COMPLETED: 0, CANCELLED: 0 }
      ;(pageResult.list || []).forEach(p => {
        if (p.status === 0) counts.PENDING++
        else if (p.status === 1) counts.IN_PROGRESS++
        else if (p.status === 2) counts.COMPLETED++
        else if (p.status === 3) counts.CANCELLED++
      })
      setStatusCount(prev => ({ ...prev, ...counts }))
    } catch {
      setPlanList([])
      setTotal(0)
    } finally {
      setLoading(false)
    }
  }, [])

  const fetchCalendarPlans = useCallback(async (month) => {
    try {
      const startDate = month.startOf('month').format('YYYY-MM-DD')
      const endDate = month.endOf('month').format('YYYY-MM-DD')
      const res = await getCleaningPlanCalendar({ startDate, endDate })
      setCalendarPlans(res.data || [])
    } catch {
      setCalendarPlans([])
    }
  }, [])

  useEffect(() => {
    fetchPlanList(1)
  }, [fetchPlanList])

  useEffect(() => {
    fetchCalendarPlans(selectedMonth)
  }, [selectedMonth, fetchCalendarPlans])

  const handleQuery = async () => {
    try {
      const values = await queryForm.validateFields()
      const queryParams = { ...values }
      if (values.dateRange) {
        queryParams.startDate = values.dateRange[0].format('YYYY-MM-DD')
        queryParams.endDate = values.dateRange[1].format('YYYY-MM-DD')
        delete queryParams.dateRange
      }
      fetchPlanList(1, queryParams)
    } catch {
      // ignore
    }
  }

  const handleReset = () => {
    queryForm.resetFields()
    fetchPlanList(1)
  }

  const handlePageChange = (page) => {
    fetchPlanList(page)
  }

  const dateCellRender = (value) => {
    const dateStr = value.format('YYYY-MM-DD')
    const dayPlans = calendarPlans.filter(p => dayjs(p.planDate).format('YYYY-MM-DD') === dateStr)

    if (dayPlans.length === 0) return null

    return (
      <ul className="events">
        {dayPlans.slice(0, 3).map(plan => {
          const statusInfo = STATUS_MAP[plan.status] || {}
          return (
            <li key={plan.id}>
              <Badge
                status={statusInfo.color === 'processing' ? 'processing'
                  : statusInfo.color === 'green' ? 'success'
                    : statusInfo.color === 'orange' ? 'warning'
                      : statusInfo.color === 'red' ? 'error' : 'default'}
                text={
                  <Tooltip title={`${plan.title} - ${statusInfo.text || ''}`}>
                    <span
                      className="plan-item-text"
                      onClick={(e) => {
                        e.stopPropagation()
                        handleViewDetail(plan)
                      }}
                    >
                      {plan.title.length > 8 ? plan.title.slice(0, 8) + '...' : plan.title}
                    </span>
                  </Tooltip>
                }
              />
            </li>
          )
        })}
        {dayPlans.length > 3 && (
          <li>
            <Tag color="blue" style={{ cursor: 'pointer' }}>
              +{dayPlans.length - 3} 更多
            </Tag>
          </li>
        )}
      </ul>
    )
  }

  const dateFullCellRender = (value) => {
    const dateStr = value.format('YYYY-MM-DD')
    const dayPlans = calendarPlans.filter(p => dayjs(p.planDate).format('YYYY-MM-DD') === dateStr)

    return (
      <div className="ant-picker-calendar-date-content" style={{ height: '100%' }}>
        {dateCellRender(value)}
      </div>
    )
  }

  const monthCellRender = (value) => {
    const monthStart = value.startOf('month')
    const monthEnd = value.endOf('month')
    const monthPlans = calendarPlans.filter(p => {
      const pd = dayjs(p.planDate)
      return pd.isAfter(monthStart.subtract(1, 'day')) && pd.isBefore(monthEnd.add(1, 'day'))
    })

    if (monthPlans.length === 0) return null

    return (
      <div className="notes-month">
        <section>
          <Tag color="blue">{monthPlans.length} 个计划</Tag>
        </section>
      </div>
    )
  }

  const onPanelChange = (value, mode) => {
    if (mode === 'month' || mode === 'year') {
      setSelectedMonth(value)
    }
  }

  const handleAdd = (plan = null) => {
    setEditingPlan(plan)
    addForm.resetFields()
    if (plan) {
      addForm.setFieldsValue({
        ...plan,
        planDate: plan.planDate ? dayjs(plan.planDate) : null
      })
    }
    setAddVisible(true)
  }

  const handleAddOk = async () => {
    try {
      const values = await addForm.validateFields()
      const submitData = {
        ...values,
        planDate: values.planDate ? values.planDate.format('YYYY-MM-DD') : undefined
      }
      const user = getCurrentUser()
      setAddLoading(true)

      if (editingPlan && editingPlan.id) {
        submitData.id = editingPlan.id
        await updateCleaningPlan(submitData, { operatorId: user.id, operatorName: user.name })
        message.success('计划更新成功')
      } else {
        await createCleaningPlan(submitData, { creatorId: user.id, creatorName: user.name })
        message.success('计划创建成功')
      }

      setAddVisible(false)
      fetchPlanList(pageNum)
      fetchCalendarPlans(selectedMonth)
    } catch (error) {
      if (error.errorFields) return
      message.error(error.message || (editingPlan ? '更新失败' : '创建失败'))
    } finally {
      setAddLoading(false)
    }
  }

  const handleViewDetail = async (plan) => {
    setDetailLoading(true)
    setDetailVisible(true)
    try {
      const res = await getCleaningPlanDetail(plan.id)
      setCurrentPlan(res.data || plan)
    } catch {
      setCurrentPlan(plan)
    } finally {
      setDetailLoading(false)
    }
  }

  const handleStartExecute = (plan) => {
    setExecuteMode('start')
    setCurrentPlan(plan)
    executeForm.resetFields()
    setExecuteVisible(true)
  }

  const handleCompleteExecute = (plan) => {
    setExecuteMode('complete')
    setCurrentPlan(plan)
    executeForm.resetFields()
    setExecuteVisible(true)
  }

  const handleExecuteOk = async () => {
    try {
      const values = await executeForm.validateFields()
      const user = getCurrentUser()
      const baseParams = { planId: currentPlan.id, operatorId: user.id, operatorName: user.name, ...values }
      setExecuteLoading(true)

      if (executeMode === 'start') {
        await startCleaningPlan(baseParams)
        message.success('已开始执行')
      } else {
        await completeCleaningPlan(baseParams)
        message.success('已完成清洗计划')
      }

      setExecuteVisible(false)
      fetchPlanList(pageNum)
      fetchCalendarPlans(selectedMonth)
      if (currentPlan) {
        handleViewDetail(currentPlan)
      }
    } catch (error) {
      if (error.errorFields) return
      message.error(error.message || '操作失败')
    } finally {
      setExecuteLoading(false)
    }
  }

  const handleCancelPlan = (plan) => {
    Modal.confirm({
      title: '确认取消',
      content: `确定要取消清洗计划「${plan.title}」吗？`,
      okText: '确认取消',
      okType: 'danger',
      cancelText: '再想想',
      onOk: async () => {
        const user = getCurrentUser()
        try {
          await cancelCleaningPlan(plan.id, {
            operatorId: user.id,
            operatorName: user.name,
            reason: '手动取消'
          })
          message.success('已取消')
          fetchPlanList(pageNum)
          fetchCalendarPlans(selectedMonth)
        } catch (error) {
          message.error(error.message || '取消失败')
        }
      }
    })
  }

  const columns = [
    {
      title: '计划编号',
      dataIndex: 'planNo',
      key: 'planNo',
      width: 160,
      render: (text, record) => (
        <a onClick={() => handleViewDetail(record)} style={{ color: '#1890ff' }}>{text}</a>
      )
    },
    {
      title: '计划标题',
      dataIndex: 'title',
      key: 'title',
      width: 200,
      ellipsis: true
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
      title: '计划日期',
      dataIndex: 'planDate',
      key: 'planDate',
      width: 120
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
      title: '负责人',
      dataIndex: 'ownerName',
      key: 'ownerName',
      width: 100
    },
    {
      title: '发电量提升',
      dataIndex: 'improvedEnergy',
      key: 'improvedEnergy',
      width: 140,
      render: (val, record) => {
        if (record.status !== 2) return <span style={{ color: '#999' }}>未完成</span>
        return (
          <span style={{ color: val > 0 ? '#52c41a' : '#999' }}>
            {val > 0 && <ArrowUpOutlined style={{ marginRight: 4 }} />}
            {val || 0} kWh
            {record.improvementRatePercent && (
              <Tag color="green" style={{ marginLeft: 6 }}>
                +{record.improvementRatePercent}%
              </Tag>
            )}
          </span>
        )
      }
    },
    {
      title: '操作',
      key: 'action',
      width: 240,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>
            详情
          </Button>
          {record.status === 0 && (
            <>
              <Button type="link" size="small" onClick={() => handleAdd(record)}>编辑</Button>
              <Button type="link" size="small" icon={<PlayCircleOutlined />} onClick={() => handleStartExecute(record)}>
                开始
              </Button>
              <Button type="link" size="small" danger onClick={() => handleCancelPlan(record)}>取消</Button>
            </>
          )}
          {record.status === 1 && (
            <Button type="link" size="small" icon={<CheckCircleOutlined />}
              style={{ color: '#52c41a' }} onClick={() => handleCompleteExecute(record)}>
              完成
            </Button>
          )}
        </Space>
      )
    }
  ]

  return (
    <div className="cleaning-plan-page">
      <Card
        title="清洁计划管理"
        extra={
          <Space>
            <Button
              type={viewMode === 'calendar' ? 'primary' : 'default'}
              icon={<CalendarOutlined />}
              onClick={() => setViewMode('calendar')}
            >
              日历视图
            </Button>
            <Button
              type={viewMode === 'list' ? 'primary' : 'default'}
              icon={<UnorderedListOutlined />}
              onClick={() => setViewMode('list')}
            >
              列表视图
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => handleAdd()}>
              新建计划
            </Button>
          </Space>
        }
      >
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col xs={12} sm={6}>
            <Card size="small">
              <Statistic
                title="待执行"
                value={statusCount.PENDING}
                valueStyle={{ color: '#faad14', fontSize: 20 }}
                prefix={<CalendarOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6}>
            <Card size="small">
              <Statistic
                title="执行中"
                value={statusCount.IN_PROGRESS}
                valueStyle={{ color: '#1890ff', fontSize: 20 }}
                prefix={<PlayCircleOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6}>
            <Card size="small">
              <Statistic
                title="已完成"
                value={statusCount.COMPLETED}
                valueStyle={{ color: '#52c41a', fontSize: 20 }}
                prefix={<CheckCircleOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6}>
            <Card size="small">
              <Statistic
                title="已取消"
                value={statusCount.CANCELLED}
                valueStyle={{ color: '#999', fontSize: 20 }}
                prefix={<CloseCircleOutlined />}
              />
            </Card>
          </Col>
        </Row>

        <Form form={queryForm} layout="inline" style={{ marginBottom: 16 }}>
          <Form.Item name="keyword" label="关键字">
            <Input placeholder="计划编号/标题" allowClear style={{ width: 200 }} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select placeholder="全部" allowClear style={{ width: 140 }}>
              <Option value={0}>待执行</Option>
              <Option value={1}>执行中</Option>
              <Option value={2}>已完成</Option>
              <Option value={3}>已取消</Option>
            </Select>
          </Form.Item>
          <Form.Item name="dateRange" label="日期范围">
            <RangePicker />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" onClick={handleQuery}>查询</Button>
              <Button onClick={handleReset}>重置</Button>
            </Space>
          </Form.Item>
        </Form>

        {viewMode === 'calendar' ? (
          <div className="site-calendar-demo-card">
            <Calendar
              fullscreen={false}
              cellRender={dateCellRender}
              fullCellRender={dateFullCellRender}
              monthCellRender={monthCellRender}
              onPanelChange={onPanelChange}
              headerRender={({ value, onChange, type, onChangeType }) => (
                <div className="ant-picker-calendar-header">
                  <Select
                    value={type}
                    onChange={onChangeType}
                    style={{ width: 80, marginRight: 8 }}
                  >
                    <Option value="month">月</Option>
                    <Option value="year">年</Option>
                  </Select>
                  <Button
                    onClick={() => onChange(value.subtract(1, type === 'month' ? 'month' : 'year'))}
                    icon={<span>{'<'}</span>}
                  />
                  <Button
                    onClick={() => onChange(value.add(1, type === 'month' ? 'month' : 'year'))}
                    icon={<span>{'>'}</span>}
                  />
                  <span style={{ marginLeft: 12, fontWeight: 500 }}>
                    {type === 'month'
                      ? value.format('YYYY年MM月')
                      : value.format('YYYY年')}
                  </span>
                </div>
              )}
            />
          </div>
        ) : (
          <Table
            columns={columns}
            dataSource={planList}
            rowKey="id"
            loading={loading}
            scroll={{ x: 1300 }}
            pagination={{
              current: pageNum,
              pageSize: PAGE_SIZE,
              total,
              showTotal: (t) => `共 ${t} 条`,
              showSizeChanger: false,
              onChange: handlePageChange
            }}
          />
        )}
      </Card>

      <Modal
        title={editingPlan && editingPlan.id ? '编辑清洗计划' : '新建清洗计划'}
        open={addVisible}
        onOk={handleAddOk}
        onCancel={() => setAddVisible(false)}
        confirmLoading={addLoading}
        okText="提交"
        cancelText="取消"
        width={600}
        destroyOnClose
      >
        <Form form={addForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="title"
                label="计划标题"
                rules={[{ required: true, message: '请输入计划标题' }]}
              >
                <Input placeholder="如：3号方阵清洗作业" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="planDate"
                label="计划清洗日期"
                rules={[{ required: true, message: '请选择日期' }]}
              >
                <DatePicker style={{ width: '100%' }} placeholder="选择日期" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="stationId"
                label="电站ID"
                rules={[{ required: true, message: '请输入电站ID' }]}
              >
                <InputNumber placeholder="请输入" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="stationName" label="电站名称">
                <Input placeholder="请输入" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="inverterId" label="逆变器ID">
                <InputNumber placeholder="请输入" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="arrayNumber" label="方阵编号">
                <Input placeholder="如：3号方阵" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="ownerId" label="负责人ID">
                <InputNumber placeholder="请输入" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="ownerName" label="负责人姓名">
                <Input placeholder="请输入" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="cleaningMethod" label="清洗方式">
                <Select placeholder="请选择" allowClear>
                  {CLEANING_METHODS.map(m => (
                    <Option key={m} value={m}>{m}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="cleaningCost" label="清洗费用(元)">
                <InputNumber placeholder="请输入" min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="teamMembers" label="参与人员/团队">
            <Input placeholder="多人请用逗号分隔" />
          </Form.Item>
          <Form.Item name="description" label="计划描述">
            <TextArea rows={3} placeholder="请详细描述清洗作业范围和要求" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title="清洗计划详情"
        width={720}
        onClose={() => setDetailVisible(false)}
        open={detailVisible}
        loading={detailLoading}
      >
        {currentPlan && (
          <>
            <Descriptions title="基本信息" bordered column={2} size="small" style={{ marginBottom: 24 }}>
              <Descriptions.Item label="计划编号">{currentPlan.planNo}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={(STATUS_MAP[currentPlan.status] || {}).color}>
                  {(STATUS_MAP[currentPlan.status] || {}).text || currentPlan.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="计划标题" span={2}>{currentPlan.title}</Descriptions.Item>
              <Descriptions.Item label="所属电站">{currentPlan.stationName || '-'}</Descriptions.Item>
              <Descriptions.Item label="方阵/逆变器">
                {currentPlan.arrayNumber || currentPlan.inverterName || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="计划日期">{currentPlan.planDate || '-'}</Descriptions.Item>
              <Descriptions.Item label="负责人">{currentPlan.ownerName || '-'}</Descriptions.Item>
              <Descriptions.Item label="参与人员">{currentPlan.teamMembers || '-'}</Descriptions.Item>
              <Descriptions.Item label="清洗方式">{currentPlan.cleaningMethod || '-'}</Descriptions.Item>
              <Descriptions.Item label="创建时间">{currentPlan.createTime || '-'}</Descriptions.Item>
              <Descriptions.Item label="实际开始时间">{currentPlan.actualStartTime || '-'}</Descriptions.Item>
              <Descriptions.Item label="实际完成时间">{currentPlan.actualEndTime || '-'}</Descriptions.Item>
              <Descriptions.Item label="计划描述" span={2}>{currentPlan.description || '-'}</Descriptions.Item>
              {currentPlan.workRemark && (
                <Descriptions.Item label="工作备注" span={2}>{currentPlan.workRemark}</Descriptions.Item>
              )}
            </Descriptions>

            {currentPlan.status === 2 && (
              <Card title="清洗效果统计" size="small" style={{ marginBottom: 24 }} type="inner">
                <Row gutter={16}>
                  <Col span={8}>
                    <Statistic
                      title="清洗前日均发电量"
                      value={currentPlan.beforeCleanEnergy || 0}
                      suffix="kWh"
                      valueStyle={{ fontSize: 18 }}
                    />
                  </Col>
                  <Col span={8}>
                    <Statistic
                      title="清洗后日均发电量"
                      value={currentPlan.afterCleanEnergy || 0}
                      suffix="kWh"
                      valueStyle={{ fontSize: 18, color: '#52c41a' }}
                    />
                  </Col>
                  <Col span={8}>
                    <div>
                      <div style={{ fontSize: 14, color: '#666', marginBottom: 8 }}>发电量提升</div>
                      <div style={{ display: 'flex', alignItems: 'baseline' }}>
                        <span style={{ fontSize: 20, fontWeight: 600, color: '#52c41a' }}>
                          +{currentPlan.improvedEnergy || 0}
                        </span>
                        <span style={{ fontSize: 14, color: '#999', marginLeft: 6 }}>kWh/日</span>
                      </div>
                      {currentPlan.improvementRatePercent != null && (
                        <Progress
                          percent={Number(currentPlan.improvementRatePercent)}
                          status="active"
                          strokeColor="#52c41a"
                          showInfo
                          style={{ marginTop: 8 }}
                        />
                      )}
                    </div>
                  </Col>
                </Row>
                {(currentPlan.cleaningCost != null || currentPlan.waterUsage != null) && (
                  <Row gutter={16} style={{ marginTop: 16 }}>
                    <Col span={12}>
                      <Statistic
                        title="清洗费用"
                        value={currentPlan.cleaningCost || 0}
                        prefix="¥"
                        valueStyle={{ fontSize: 16 }}
                      />
                    </Col>
                    <Col span={12}>
                      <Statistic
                        title="用水量"
                        value={currentPlan.waterUsage || 0}
                        suffix="L"
                        valueStyle={{ fontSize: 16 }}
                      />
                    </Col>
                  </Row>
                )}
              </Card>
            )}

            <Card
              title="照片对比"
              size="small"
              type="inner"
              extra={<Tag color="blue">{`清洗前 ${currentPlan.beforeCleanPhotoList?.length || 0} 张 | 清洗后 ${currentPlan.afterCleanPhotoList?.length || 0} 张`}</Tag>}
            >
              <Row gutter={16}>
                <Col span={12}>
                  <div style={{ fontSize: 14, fontWeight: 500, marginBottom: 8, color: '#666' }}>清洗前照片</div>
                  {currentPlan.beforeCleanPhotoList?.length > 0 ? (
                    <Image.PreviewGroup>
                      <Row gutter={8}>
                        {currentPlan.beforeCleanPhotoList.map((url, idx) => (
                          <Col span={8} key={idx} style={{ marginBottom: 8 }}>
                            <Image
                              src={url.startsWith('http') ? url : `http://localhost:8080${url}`}
                              alt={`清洗前-${idx + 1}`}
                              width="100%"
                              height={80}
                              style={{ objectFit: 'cover', borderRadius: 4 }}
                            />
                          </Col>
                        ))}
                      </Row>
                    </Image.PreviewGroup>
                  ) : (
                    <div style={{ color: '#999', textAlign: 'center', padding: '20px 0' }}>
                      <CloudUploadOutlined style={{ fontSize: 24 }} />
                      <div style={{ marginTop: 4 }}>暂无照片</div>
                    </div>
                  )}
                </Col>
                <Col span={12}>
                  <div style={{ fontSize: 14, fontWeight: 500, marginBottom: 8, color: '#666' }}>清洗后照片</div>
                  {currentPlan.afterCleanPhotoList?.length > 0 ? (
                    <Image.PreviewGroup>
                      <Row gutter={8}>
                        {currentPlan.afterCleanPhotoList.map((url, idx) => (
                          <Col span={8} key={idx} style={{ marginBottom: 8 }}>
                            <Image
                              src={url.startsWith('http') ? url : `http://localhost:8080${url}`}
                              alt={`清洗后-${idx + 1}`}
                              width="100%"
                              height={80}
                              style={{ objectFit: 'cover', borderRadius: 4 }}
                            />
                          </Col>
                        ))}
                      </Row>
                    </Image.PreviewGroup>
                  ) : (
                    <div style={{ color: '#999', textAlign: 'center', padding: '20px 0' }}>
                      <CloudUploadOutlined style={{ fontSize: 24 }} />
                      <div style={{ marginTop: 4 }}>暂无照片</div>
                    </div>
                  )}
                </Col>
              </Row>
            </Card>
          </>
        )}
      </Drawer>

      <Modal
        title={executeMode === 'start' ? '开始执行清洗计划' : '完成清洗计划'}
        open={executeVisible}
        onOk={handleExecuteOk}
        onCancel={() => setExecuteVisible(false)}
        confirmLoading={executeLoading}
        okText="确认提交"
        cancelText="取消"
        width={600}
        destroyOnClose
      >
        <Form form={executeForm} layout="vertical">
          {executeMode === 'start' && (
            <Form.Item
              name="beforeCleanPhotos"
              label="清洗前照片URL"
              tooltip="多张照片请用逗号分隔URL"
            >
              <TextArea rows={2} placeholder="https://..., https://..." />
            </Form.Item>
          )}

          {executeMode === 'complete' && (
            <>
              <Form.Item
                name="beforeCleanPhotos"
                label="清洗前照片URL"
                tooltip="如未在开始时上传，请补充"
              >
                <TextArea rows={2} placeholder="https://..., https://..." />
              </Form.Item>
              <Form.Item
                name="afterCleanPhotos"
                label="清洗后照片URL"
                tooltip="多张照片请用逗号分隔URL"
              >
                <TextArea rows={2} placeholder="https://..., https://..." />
              </Form.Item>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item name="cleaningMethod" label="清洗方式">
                    <Select placeholder="请选择" allowClear style={{ width: '100%' }}>
                      {CLEANING_METHODS.map(m => (
                        <Option key={m} value={m}>{m}</Option>
                      ))}
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item name="waterUsage" label="用水量(升)">
                    <InputNumber placeholder="请输入" min={0} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
              </Row>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item name="cleaningCost" label="清洗费用(元)">
                    <InputNumber placeholder="请输入" min={0} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
              </Row>
              <Form.Item
                name="workRemark"
                label="工作备注"
              >
                <TextArea rows={3} placeholder="清洗过程中的问题、发现或说明" />
              </Form.Item>
              <Form.Item
                name="inspectionRemark"
                label="验收意见"
              >
                <TextArea rows={2} placeholder="验收人的评价或意见" />
              </Form.Item>
            </>
          )}
        </Form>
      </Modal>
    </div>
  )
}

export default CleaningPlanManagement
