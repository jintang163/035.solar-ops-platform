import React, { useState, useEffect, useCallback } from 'react'
import {
  Card,
  Table,
  Button,
  Form,
  InputNumber,
  Select,
  Checkbox,
  message,
  Row,
  Col,
  Statistic,
  Tooltip,
  Empty,
  Divider,
  Tag
} from 'antd'
import {
  BarChartOutlined,
  ThunderboltOutlined,
  DollarOutlined,
  CalculatorOutlined,
  FileTextOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import {
  getPriceSchemeAll,
  comparePriceSchemes
} from '../../api/revenue'

const { Option } = Select
const { Group: CheckboxGroup } = Checkbox

const PriceSchemeCompare = () => {
  const [loading, setLoading] = useState(false)
  const [schemeList, setSchemeList] = useState([])
  const [selectedSchemes, setSelectedSchemes] = useState([])
  const [compareResult, setCompareResult] = useState([])
  const [compareLoading, setCompareLoading] = useState(false)
  const [paramForm] = Form.useForm()

  const loadSchemeList = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getPriceSchemeAll({ status: 1 })
      setSchemeList(res.data || [])
    } catch {
      setSchemeList([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadSchemeList()
  }, [loadSchemeList])

  const handleCompare = async () => {
    if (selectedSchemes.length === 0) {
      message.warning('请至少选择一个电价方案')
      return
    }
    if (selectedSchemes.length === 1) {
      message.warning('请至少选择两个电价方案进行对比')
      return
    }

    try {
      const values = await paramForm.validateFields()
      setCompareLoading(true)
      const data = {
        schemeIds: selectedSchemes
      }
      const params = {
        totalInvestment: values.totalInvestment,
        annualOperationCost: values.annualOperationCost,
        designLife: values.designLife
      }
      const res = await comparePriceSchemes(data, params)
      setCompareResult(res.data || [])
      message.success('对比计算完成')
    } catch (error) {
      if (error.errorFields) return
      message.error(error.message || '对比计算失败')
      setCompareResult([])
    } finally {
      setCompareLoading(false)
    }
  }

  const formatNumber = (num, decimals = 2) => {
    if (num == null) return '0.00'
    return Number(num).toFixed(decimals)
  }

  const getBestIndicator = (field, isHigher = true) => {
    if (compareResult.length === 0) return null
    let best = compareResult[0]
    for (let i = 1; i < compareResult.length; i++) {
      const current = Number(compareResult[i][field] || 0)
      const bestVal = Number(best[field] || 0)
      if (isHigher ? current > bestVal : current < bestVal) {
        best = compareResult[i]
      }
    }
    return best.schemeId
  }

  const columns = [
    {
      title: '方案名称',
      dataIndex: 'schemeName',
      key: 'schemeName',
      width: 180,
      fixed: 'left',
      render: (text) => (
        <div style={{ fontWeight: 500 }}>{text}</div>
      )
    },
    {
      title: '综合电价',
      dataIndex: 'totalPrice',
      key: 'totalPrice',
      width: 140,
      align: 'right',
      render: (val, record) => {
        const bestId = getBestIndicator('totalPrice', true)
        return (
          <div>
            <span style={{ color: '#1890ff', fontWeight: record.schemeId === bestId ? 600 : 500 }}>
              ¥{formatNumber(val, 4)}
            </span>
            {record.schemeId === bestId && <Tag color="gold" style={{ marginLeft: 4 }}>最优</Tag>}
            <div style={{ fontSize: 11, color: '#999' }}>元/kWh</div>
          </div>
        )
      }
    },
    {
      title: '预计年发电量',
      dataIndex: 'estimatedYearEnergy',
      key: 'estimatedYearEnergy',
      width: 140,
      align: 'right',
      render: (val, record) => {
        const bestId = getBestIndicator('estimatedYearEnergy', true)
        return (
          <div>
            <span style={{ fontWeight: record.schemeId === bestId ? 600 : 500 }}>{formatNumber(val)}</span>
            {record.schemeId === bestId && <Tag color="blue" style={{ marginLeft: 4 }}>最高</Tag>}
            <div style={{ fontSize: 11, color: '#999' }}>kWh</div>
          </div>
        )
      }
    },
    {
      title: '预计年收益',
      dataIndex: 'estimatedYearRevenue',
      key: 'estimatedYearRevenue',
      width: 140,
      align: 'right',
      render: (val, record) => {
        const bestId = getBestIndicator('estimatedYearRevenue', true)
        return (
          <div>
            <span style={{ color: '#52c41a', fontWeight: record.schemeId === bestId ? 600 : 500 }}>
              ¥{formatNumber(val)}
            </span>
            {record.schemeId === bestId && <Tag color="green" style={{ marginLeft: 4 }}>最高</Tag>}
            <div style={{ fontSize: 11, color: '#999' }}>元/年</div>
          </div>
        )
      }
    },
    {
      title: 'ROI',
      dataIndex: 'roi',
      key: 'roi',
      width: 120,
      align: 'right',
      render: (val, record) => {
        const bestId = getBestIndicator('roi', true)
        return (
          <div>
            <span style={{ color: '#722ed1', fontWeight: record.schemeId === bestId ? 600 : 500 }}>
              {formatNumber(val, 2)}%
            </span>
            {record.schemeId === bestId && <Tag color="purple" style={{ marginLeft: 4 }}>最优</Tag>}
          </div>
        )
      }
    },
    {
      title: '投资回收期',
      dataIndex: 'paybackPeriod',
      key: 'paybackPeriod',
      width: 130,
      align: 'right',
      render: (val, record) => {
        const bestId = getBestIndicator('paybackPeriod', false)
        return (
          <div>
            <span style={{ color: '#13c2c2', fontWeight: record.schemeId === bestId ? 600 : 500 }}>
              {formatNumber(val, 2)}
            </span>
            {record.schemeId === bestId && <Tag color="cyan" style={{ marginLeft: 4 }}>最短</Tag>}
            <div style={{ fontSize: 11, color: '#999' }}>年</div>
          </div>
        )
      }
    }
  ]

  const compareChartOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['年收益(万元)', 'ROI(%)'],
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: compareResult.map(d => d.schemeName || ''),
      axisLabel: {
        fontSize: 11,
        interval: 0,
        rotate: 0
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '年收益(万元)',
        axisLabel: {
          fontSize: 11
        }
      },
      {
        type: 'value',
        name: 'ROI(%)',
        axisLabel: {
          fontSize: 11
        }
      }
    ],
    series: [
      {
        name: '年收益(万元)',
        type: 'bar',
        data: compareResult.map(d => Number(d.estimatedYearRevenue || 0) / 10000),
        itemStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: '#52c41a' },
              { offset: 1, color: '#95de64' }
            ]
          },
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: '40%'
      },
      {
        name: 'ROI(%)',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: compareResult.map(d => Number(d.roi || 0)),
        itemStyle: {
          color: '#722ed1'
        },
        lineStyle: {
          width: 2
        },
        symbol: 'circle',
        symbolSize: 8,
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(114, 46, 209, 0.3)' },
              { offset: 1, color: 'rgba(114, 46, 209, 0.05)' }
            ]
          }
        }
      }
    ]
  }

  const paybackChartOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['投资回收期(年)'],
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: compareResult.map(d => d.schemeName || ''),
      axisLabel: {
        fontSize: 11
      }
    },
    yAxis: {
      type: 'value',
      name: '年',
      axisLabel: {
        fontSize: 11
      }
    },
    series: [
      {
        name: '投资回收期(年)',
        type: 'bar',
        data: compareResult.map(d => ({
          value: Number(d.paybackPeriod || 0),
          itemStyle: {
            color: Number(d.paybackPeriod || 0) < 10 ? '#52c41a' : Number(d.paybackPeriod || 0) < 15 ? '#fa8c16' : '#ff4d4f',
            borderRadius: [4, 4, 0, 0]
          }
        })),
        label: {
          show: true,
          position: 'top',
          formatter: '{c} 年'
        },
        barWidth: '50%'
      }
    ]
  }

  return (
    <div className="price-scheme-compare-page">
      <Card
        title={
          <span>
            <BarChartOutlined style={{ color: '#1890ff', marginRight: 6 }} />
            电价方案对比分析
          </span>
        }
        extra={
          <Tooltip title="选择多个电价方案，输入投资参数，系统将自动计算并对比各方案的收益情况">
            <Button type="primary" icon={<CalculatorOutlined />} onClick={handleCompare} loading={compareLoading}>
              开始对比
            </Button>
          </Tooltip>
        }
      >
        <Row gutter={[16, 16]}>
          <Col xs={24} lg={6}>
            <Card
              title={
                <span>
                  <FileTextOutlined style={{ color: '#1890ff', marginRight: 4 }} />
                  选择电价方案
                </span>
              }
              size="small"
              style={{ height: '100%' }}
              loading={loading}
            >
              <div style={{ marginBottom: 12 }}>
                <div style={{ fontSize: 12, color: '#999', marginBottom: 8 }}>
                  已选择 {selectedSchemes.length} 个方案
                </div>
                <CheckboxGroup
                  value={selectedSchemes}
                  onChange={(values) => setSelectedSchemes(values)}
                  style={{ width: '100%' }}
                >
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                    {schemeList.map(scheme => (
                      <Checkbox key={scheme.id} value={scheme.id}>
                        <div>
                          <div style={{ fontWeight: 500 }}>{scheme.schemeName}</div>
                          <div style={{ fontSize: 11, color: '#999' }}>
                            {scheme.stationName} · 综合电价 ¥{Number(scheme.totalPrice || 0).toFixed(4)}
                          </div>
                        </div>
                      </Checkbox>
                    ))}
                    {schemeList.length === 0 && (
                      <Empty description="暂无生效的电价方案" image={Empty.PRESENTED_IMAGE_SIMPLE} style={{ padding: '20px 0' }} />
                    )}
                  </div>
                </CheckboxGroup>
              </div>
            </Card>
          </Col>

          <Col xs={24} lg={6}>
            <Card
              title={
                <span>
                  <CalculatorOutlined style={{ color: '#722ed1', marginRight: 4 }} />
                  投资参数设置
                </span>
              }
              size="small"
              style={{ height: '100%' }}
            >
              <Form form={paramForm} layout="vertical" initialValues={{
                totalInvestment: 5000000,
                annualOperationCost: 150000,
                designLife: 25
              }}>
                <Form.Item
                  name="totalInvestment"
                  label={
                    <Tooltip title="项目总投资金额，包括设备采购、安装施工、土地成本等">
                      总投资金额 (元)
                    </Tooltip>
                  }
                  rules={[{ required: true, message: '请输入总投资金额' }]}
                >
                  <InputNumber
                    placeholder="请输入"
                    min={0}
                    style={{ width: '100%' }}
                    formatter={value => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                    parser={value => value.replace(/\$\s?|(,*)/g, '')}
                  />
                </Form.Item>
                <Form.Item
                  name="annualOperationCost"
                  label={
                    <Tooltip title="年度运维成本，包括人工、耗材、维修等费用">
                      年运维成本 (元)
                    </Tooltip>
                  }
                  rules={[{ required: true, message: '请输入年运维成本' }]}
                >
                  <InputNumber
                    placeholder="请输入"
                    min={0}
                    style={{ width: '100%' }}
                    formatter={value => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                    parser={value => value.replace(/\$\s?|(,*)/g, '')}
                  />
                </Form.Item>
                <Form.Item
                  name="designLife"
                  label={
                    <Tooltip title="项目设计使用年限，通常为20-25年">
                      设计寿命 (年)
                    </Tooltip>
                  }
                  rules={[{ required: true, message: '请输入设计寿命' }]}
                >
                  <InputNumber
                    placeholder="请输入"
                    min={1}
                    max={50}
                    style={{ width: '100%' }}
                  />
                </Form.Item>
              </Form>

              <Divider style={{ margin: '12px 0' }} />

              <div style={{ fontSize: 12, color: '#999', lineHeight: 1.6 }}>
                <div style={{ fontWeight: 500, color: '#666', marginBottom: 6 }}>计算公式说明：</div>
                <div>• 预计年收益 = 年发电量 × 综合电价</div>
                <div>• ROI = (年收益 - 年运维成本) / 总投资 × 100%</div>
                <div>• 投资回收期 = 总投资 / (年收益 - 年运维成本)</div>
              </div>
            </Card>
          </Col>

          <Col xs={24} lg={12}>
            <Card
              title={
                <span>
                  <BarChartOutlined style={{ color: '#52c41a', marginRight: 4 }} />
                  对比结果
                </span>
              }
              size="small"
              loading={compareLoading}
            >
              {compareResult.length > 0 ? (
                <>
                  <Row gutter={16} style={{ marginBottom: 16 }}>
                    {compareResult.map((item, index) => (
                      <Col span={12} key={item.schemeId}>
                        <Card size="small" style={{ background: index === 0 ? '#f6ffed' : '#fff' }}>
                          <Statistic
                            title={
                              <span style={{ fontSize: 12 }}>
                                {item.schemeName}
                                {index === 0 && <Tag color="gold" style={{ marginLeft: 4 }}>推荐</Tag>}
                              </span>
                            }
                            value={item.roi}
                            suffix="%"
                            precision={2}
                            valueStyle={{ fontSize: 20, color: '#722ed1' }}
                            prefix={<DollarOutlined />}
                          />
                          <div style={{ fontSize: 11, color: '#999', marginTop: 4 }}>
                            回收期: {formatNumber(item.paybackPeriod, 2)} 年
                          </div>
                        </Card>
                      </Col>
                    ))}
                  </Row>
                  <Table
                    columns={columns}
                    dataSource={compareResult}
                    rowKey="schemeId"
                    scroll={{ x: 900 }}
                    pagination={false}
                    size="small"
                  />
                </>
              ) : (
                <Empty
                  description="请选择电价方案并设置投资参数，点击开始对比"
                  style={{ padding: '60px 0' }}
                />
              )}
            </Card>
          </Col>
        </Row>

        {compareResult.length > 0 && (
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col xs={24} lg={12}>
              <Card title="年收益 & ROI 对比" loading={compareLoading}>
                <ReactECharts option={compareChartOption} style={{ height: 320 }} />
              </Card>
            </Col>
            <Col xs={24} lg={12}>
              <Card title="投资回收期对比" loading={compareLoading}>
                <ReactECharts option={paybackChartOption} style={{ height: 320 }} />
              </Card>
            </Col>
          </Row>
        )}
      </Card>
    </div>
  )
}

export default PriceSchemeCompare
