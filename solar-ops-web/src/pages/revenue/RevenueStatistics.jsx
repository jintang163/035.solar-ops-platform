import React, { useState, useEffect, useCallback } from 'react'
import {
  Card,
  Table,
  Button,
  Form,
  Input,
  Select,
  DatePicker,
  message,
  Tag,
  Row,
  Col,
  Statistic,
  Radio,
  Tooltip,
  Empty,
  Space
} from 'antd'
import {
  SearchOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
  DollarOutlined,
  CalculatorOutlined,
  FileTextOutlined,
  DownloadOutlined,
  BarChartOutlined
} from '@ant-design/icons'
import dayjs from 'dayjs'
import StatCard from '../../components/StatCard'
import {
  getRevenuePage,
  getTotalRevenue,
  calcDailyRevenue,
  calcMonthlyRevenue
} from '../../api/revenue'

const { RangePicker } = DatePicker
const { Option } = Select
const { Group: RadioGroup } = Radio

const PAGE_SIZE = 10

const STATUS_MAP = {
  0: { color: 'default', text: '未核算' },
  1: { color: 'processing', text: '核算中' },
  2: { color: 'success', text: '已核算' },
  3: { color: 'error', text: '核算失败' }
}

const RevenueStatistics = () => {
  const [loading, setLoading] = useState(false)
  const [revenueList, setRevenueList] = useState([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [queryForm] = Form.useForm()
  const [summaryData, setSummaryData] = useState(null)
  const [statType, setStatType] = useState('day')
  const [calcLoading, setCalcLoading] = useState(false)

  const formatNumber = (num, decimals = 2) => {
    if (num == null) return '0.00'
    const n = Number(num)
    if (n >= 10000) {
      return (n / 10000).toFixed(decimals) + '万'
    }
    return n.toFixed(decimals)
  }

  const fetchRevenueList = useCallback(async (page = 1, values = {}) => {
    setLoading(true)
    try {
      const params = {
        pageNum: page,
        pageSize: PAGE_SIZE,
        statType: statType,
        ...values
      }
      if (values.dateRange) {
        params.startDate = values.dateRange[0].format('YYYY-MM-DD')
        params.endDate = values.dateRange[1].format('YYYY-MM-DD')
        delete params.dateRange
      }
      const [listRes, totalRes] = await Promise.all([
        getRevenuePage(params),
        getTotalRevenue(params)
      ])
      const pageResult = listRes.data || {}
      setRevenueList(pageResult.list || [])
      setTotal(pageResult.total || 0)
      setPageNum(pageResult.pageNum || page)
      setSummaryData(totalRes.data || {})
    } catch {
      setRevenueList([])
      setTotal(0)
      setSummaryData(null)
    } finally {
      setLoading(false)
    }
  }, [statType])

  useEffect(() => {
    const endDate = dayjs()
    const startDate = statType === 'day' ? endDate.subtract(30, 'day') : endDate.subtract(12, 'month')
    queryForm.setFieldsValue({
      dateRange: [startDate, endDate]
    })
    fetchRevenueList(1, { dateRange: [startDate, endDate] })
  }, [statType, fetchRevenueList, queryForm])

  const handleQuery = async () => {
    try {
      const values = await queryForm.validateFields()
      fetchRevenueList(1, values)
    } catch {
      // ignore
    }
  }

  const handleReset = () => {
    const endDate = dayjs()
    const startDate = statType === 'day' ? endDate.subtract(30, 'day') : endDate.subtract(12, 'month')
    queryForm.resetFields()
    queryForm.setFieldsValue({
      dateRange: [startDate, endDate],
      stationId: undefined,
      keyword: undefined
    })
    fetchRevenueList(1, { dateRange: [startDate, endDate] })
  }

  const handlePageChange = (page) => {
    fetchRevenueList(page, queryForm.getFieldsValue())
  }

  const handleCalc = async (type) => {
    try {
      setCalcLoading(true)
      const values = await queryForm.validateFields()
      const params = {
        startDate: values.dateRange?.[0]?.format('YYYY-MM-DD'),
        endDate: values.dateRange?.[1]?.format('YYYY-MM-DD'),
        stationId: values.stationId
      }
      if (type === 'daily') {
        await calcDailyRevenue(params)
        message.success('日收益核算任务已提交')
      } else {
        await calcMonthlyRevenue(params)
        message.success('月收益核算任务已提交')
      }
      setTimeout(() => {
        fetchRevenueList(pageNum, values)
      }, 2000)
    } catch (error) {
      message.error(error.message || '核算失败')
    } finally {
      setCalcLoading(false)
    }
  }

  const columns = [
    {
      title: statType === 'day' ? '日期' : '月份',
      dataIndex: 'statDate',
      key: 'statDate',
      width: 140,
      fixed: 'left'
    },
    {
      title: '电站',
      dataIndex: 'stationName',
      key: 'stationName',
      width: 140
    },
    {
      title: '上网电量',
      dataIndex: 'gridPower',
      key: 'gridPower',
      width: 140,
      align: 'right',
      render: (val) => (
        <div>
          <span style={{ fontWeight: 500 }}>{formatNumber(val)}</span>
          <div style={{ fontSize: 11, color: '#999' }}>kWh</div>
        </div>
      )
    },
    {
      title: '上网电费',
      dataIndex: 'gridFee',
      key: 'gridFee',
      width: 140,
      align: 'right',
      render: (val) => (
        <div>
          <span style={{ color: '#1890ff', fontWeight: 500 }}>¥{formatNumber(val)}</span>
          <div style={{ fontSize: 11, color: '#999' }}>元</div>
        </div>
      )
    },
    {
      title: '国家补贴',
      dataIndex: 'nationalSubsidyFee',
      key: 'nationalSubsidyFee',
      width: 130,
      align: 'right',
      render: (val) => (
        <div>
          <span style={{ color: '#52c41a', fontWeight: 500 }}>¥{formatNumber(val)}</span>
          <div style={{ fontSize: 11, color: '#999' }}>元</div>
        </div>
      )
    },
    {
      title: '省级补贴',
      dataIndex: 'provincialSubsidyFee',
      key: 'provincialSubsidyFee',
      width: 130,
      align: 'right',
      render: (val) => (
        <div>
          <span style={{ color: '#52c41a', fontWeight: 500 }}>¥{formatNumber(val)}</span>
          <div style={{ fontSize: 11, color: '#999' }}>元</div>
        </div>
      )
    },
    {
      title: '市级补贴',
      dataIndex: 'citySubsidyFee',
      key: 'citySubsidyFee',
      width: 130,
      align: 'right',
      render: (val) => (
        <div>
          <span style={{ color: '#52c41a', fontWeight: 500 }}>¥{formatNumber(val)}</span>
          <div style={{ fontSize: 11, color: '#999' }}>元</div>
        </div>
      )
    },
    {
      title: '补贴收益',
      dataIndex: 'subsidyFee',
      key: 'subsidyFee',
      width: 140,
      align: 'right',
      render: (val, record) => {
        const totalSubsidy = Number(record.nationalSubsidyFee || 0) +
          Number(record.provincialSubsidyFee || 0) +
          Number(record.citySubsidyFee || 0)
        return (
          <div>
            <span style={{ color: '#52c41a', fontWeight: 600 }}>¥{formatNumber(totalSubsidy)}</span>
            <div style={{ fontSize: 11, color: '#999' }}>元</div>
          </div>
        )
      }
    },
    {
      title: '总收益',
      dataIndex: 'totalRevenue',
      key: 'totalRevenue',
      width: 140,
      align: 'right',
      render: (val, record) => {
        const total = Number(record.gridFee || 0) +
          Number(record.nationalSubsidyFee || 0) +
          Number(record.provincialSubsidyFee || 0) +
          Number(record.citySubsidyFee || 0)
        return (
          <div>
            <span style={{ color: '#fa8c16', fontWeight: 600, fontSize: 15 }}>¥{formatNumber(total)}</span>
            <div style={{ fontSize: 11, color: '#999' }}>元</div>
          </div>
        )
      }
    },
    {
      title: '度电成本',
      dataIndex: 'unitCost',
      key: 'unitCost',
      width: 120,
      align: 'right',
      render: (val) => (
        <div>
          <span style={{ color: '#eb2f96', fontWeight: 500 }}>¥{Number(val || 0).toFixed(4)}</span>
          <div style={{ fontSize: 11, color: '#999' }}>元/kWh</div>
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
      title: '操作',
      key: 'action',
      width: 120,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="查看明细">
            <Button type="link" size="small">详情</Button>
          </Tooltip>
        </Space>
      )
    }
  ]

  return (
    <div className="revenue-statistics-page">
      <Card
        title={
          <span>
            <FileTextOutlined style={{ color: '#1890ff', marginRight: 6 }} />
            收益统计明细
          </span>
        }
        extra={
          <Space>
            <RadioGroup value={statType} onChange={(e) => setStatType(e.target.value)} buttonStyle="solid">
              <Radio.Button value="day">日统计</Radio.Button>
              <Radio.Button value="month">月统计</Radio.Button>
            </RadioGroup>
            <Tooltip title="重新核算选中日期范围的日收益">
              <Button
                icon={<CalculatorOutlined />}
                onClick={() => handleCalc('daily')}
                loading={calcLoading}
              >
                核算日收益
              </Button>
            </Tooltip>
            <Tooltip title="重新核算选中日期范围的月收益">
              <Button
                icon={<BarChartOutlined />}
                onClick={() => handleCalc('monthly')}
                loading={calcLoading}
              >
                核算月收益
              </Button>
            </Tooltip>
            <Tooltip title="导出当前查询结果">
              <Button icon={<DownloadOutlined />}>导出</Button>
            </Tooltip>
          </Space>
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
          <Form.Item name="dateRange" label="日期范围">
            <RangePicker
              picker={statType === 'day' ? 'date' : 'month'}
              style={{ width: 280 }}
            />
          </Form.Item>
          <Form.Item name="keyword" label="关键词">
            <Input placeholder="电站名称" allowClear style={{ width: 160 }} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" icon={<SearchOutlined />} onClick={handleQuery}>查询</Button>
              <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
            </Space>
          </Form.Item>
        </Form>

        <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
          <Col xs={24} sm={12} md={8}>
            <StatCard
              title="总收益"
              value={formatNumber(summaryData?.totalRevenue)}
              prefix="¥"
              icon={<DollarOutlined />}
              color="#52c41a"
            />
          </Col>
          <Col xs={24} sm={12} md={8}>
            <StatCard
              title="总电量"
              value={formatNumber(summaryData?.totalPower)}
              suffix="kWh"
              icon={<ThunderboltOutlined />}
              color="#1890ff"
            />
          </Col>
          <Col xs={24} sm={12} md={8}>
            <StatCard
              title="平均度电成本"
              value={Number(summaryData?.avgUnitCost || 0).toFixed(4)}
              suffix="元/kWh"
              icon={<CalculatorOutlined />}
              color="#eb2f96"
            />
          </Col>
        </Row>

        <Table
          columns={columns}
          dataSource={revenueList}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1600 }}
          locale={{ emptyText: <Empty description="暂无统计数据" /> }}
          pagination={{
            current: pageNum,
            pageSize: PAGE_SIZE,
            total,
            showTotal: (t) => `共 ${t} 条`,
            showSizeChanger: false,
            onChange: handlePageChange
          }}
          summary={(pageData) => {
            if (pageData.length === 0) return null
            let totalGridPower = 0
            let totalGridFee = 0
            let totalNationalSubsidy = 0
            let totalProvincialSubsidy = 0
            let totalCitySubsidy = 0

            pageData.forEach(item => {
              totalGridPower += Number(item.gridPower || 0)
              totalGridFee += Number(item.gridFee || 0)
              totalNationalSubsidy += Number(item.nationalSubsidyFee || 0)
              totalProvincialSubsidy += Number(item.provincialSubsidyFee || 0)
              totalCitySubsidy += Number(item.citySubsidyFee || 0)
            })

            const totalSubsidy = totalNationalSubsidy + totalProvincialSubsidy + totalCitySubsidy
            const totalRevenue = totalGridFee + totalSubsidy

            return (
              <Table.Summary fixed>
                <Table.Summary.Row>
                  <Table.Summary.Cell index={0} colSpan={2}>
                    <strong>本页合计</strong>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={1} align="right">
                    <strong>{formatNumber(totalGridPower)} kWh</strong>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={2} align="right">
                    <strong style={{ color: '#1890ff' }}>¥{formatNumber(totalGridFee)}</strong>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={3} align="right">
                    <strong style={{ color: '#52c41a' }}>¥{formatNumber(totalNationalSubsidy)}</strong>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={4} align="right">
                    <strong style={{ color: '#52c41a' }}>¥{formatNumber(totalProvincialSubsidy)}</strong>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={5} align="right">
                    <strong style={{ color: '#52c41a' }}>¥{formatNumber(totalCitySubsidy)}</strong>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={6} align="right">
                    <strong style={{ color: '#52c41a' }}>¥{formatNumber(totalSubsidy)}</strong>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={7} align="right">
                    <strong style={{ color: '#fa8c16', fontSize: 15 }}>¥{formatNumber(totalRevenue)}</strong>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={8} colSpan={3} />
                </Table.Summary.Row>
              </Table.Summary>
            )
          }}
        />
      </Card>
    </div>
  )
}

export default RevenueStatistics
