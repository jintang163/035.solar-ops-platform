import React, { useState, useEffect, useCallback } from 'react'
import {
  Row,
  Col,
  Card,
  List,
  Progress,
  Statistic,
  Tag,
  Select,
  Empty,
  Tooltip
} from 'antd'
import {
  ThunderboltOutlined,
  RiseOutlined,
  DollarOutlined,
  CalendarOutlined,
  TrophyOutlined,
  DashboardOutlined,
  LineChartOutlined,
  BarChartOutlined
} from '@ant-design/icons'
import StatCard from '../../components/StatCard'
import ChartCard from '../../components/ChartCard'
import {
  getRevenueDashboard,
  getRevenueTrend,
  getRevenueStationRank
} from '../../api/revenue'

const { Option } = Select

const RevenueDashboard = () => {
  const [loading, setLoading] = useState(false)
  const [stationId, setStationId] = useState(null)
  const [dashboardData, setDashboardData] = useState(null)
  const [trendData, setTrendData] = useState([])
  const [rankData, setRankData] = useState([])

  const fetchData = useCallback(async (sid) => {
    setLoading(true)
    try {
      const params = sid ? { stationId: sid } : {}
      const [dashboardRes, trendRes, rankRes] = await Promise.all([
        getRevenueDashboard(params),
        getRevenueTrend({ days: 30, ...params }),
        getRevenueStationRank(params)
      ])
      setDashboardData(dashboardRes.data || {})
      setTrendData(trendRes.data || [])
      setRankData(rankRes.data || [])
    } catch {
      setDashboardData({})
      setTrendData([])
      setRankData([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData(stationId)
  }, [stationId, fetchData])

  const formatNumber = (num, decimals = 2) => {
    if (num == null) return '0.00'
    const n = Number(num)
    if (n >= 10000) {
      return (n / 10000).toFixed(decimals) + '万'
    }
    return n.toFixed(decimals)
  }

  const revenueTrendOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['发电量', '收益'],
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
      data: trendData.map(d => d.date?.slice(5) || ''),
      axisLabel: {
        fontSize: 11
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '发电量(kWh)',
        axisLabel: {
          fontSize: 11
        }
      },
      {
        type: 'value',
        name: '收益(元)',
        axisLabel: {
          fontSize: 11
        }
      }
    ],
    series: [
      {
        name: '发电量',
        type: 'bar',
        data: trendData.map(d => Number(d.power || 0)),
        itemStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: '#1890ff' },
              { offset: 1, color: '#91d5ff' }
            ]
          },
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: '50%'
      },
      {
        name: '收益',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: trendData.map(d => Number(d.revenue || 0)),
        itemStyle: {
          color: '#52c41a'
        },
        lineStyle: {
          width: 2
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(82, 196, 26, 0.3)' },
              { offset: 1, color: 'rgba(82, 196, 26, 0.05)' }
            ]
          }
        }
      }
    ]
  }

  const costTrendOption = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['度电成本'],
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
      data: trendData.map(d => d.date?.slice(5) || ''),
      axisLabel: {
        fontSize: 11
      }
    },
    yAxis: {
      type: 'value',
      name: '元/kWh',
      axisLabel: {
        fontSize: 11
      }
    },
    series: [
      {
        name: '度电成本',
        type: 'line',
        smooth: true,
        data: trendData.map(d => Number(d.unitCost || 0)),
        itemStyle: {
          color: '#fa8c16'
        },
        lineStyle: {
          width: 2
        },
        symbol: 'circle',
        symbolSize: 6,
        markLine: {
          silent: true,
          data: [
            {
              type: 'average',
              name: '平均值'
            }
          ],
          lineStyle: {
            color: '#999',
            type: 'dashed'
          },
          label: {
            formatter: '平均: {c} 元/kWh'
          }
        }
      }
    ]
  }

  const roiPercent = Number(dashboardData?.roiPercent || 0)
  const paybackYears = Number(dashboardData?.paybackYears || 0)

  return (
    <div className="revenue-dashboard-page">
      <div style={{ textAlign: 'right', marginBottom: 16 }}>
        <Select
          placeholder="选择电站（默认全部）"
          style={{ width: 220 }}
          allowClear
          value={stationId}
          onChange={(value) => setStationId(value)}
        >
          <Option value={1}>一号光伏电站</Option>
          <Option value={2}>二号光伏电站</Option>
          <Option value={3}>三号光伏电站</Option>
          <Option value={4}>四号光伏电站</Option>
          <Option value={5}>五号光伏电站</Option>
        </Select>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={12} sm={12} md={6}>
          <StatCard
            title="今日收益"
            value={formatNumber(dashboardData?.todayRevenue)}
            prefix="¥"
            icon={<DollarOutlined />}
            color="#52c41a"
            trend="up"
            trendValue={`${Number(dashboardData?.todayGrowth || 0).toFixed(1)}%`}
          />
        </Col>
        <Col xs={12} sm={12} md={6}>
          <StatCard
            title="本月收益"
            value={formatNumber(dashboardData?.monthRevenue)}
            prefix="¥"
            icon={<CalendarOutlined />}
            color="#1890ff"
          />
        </Col>
        <Col xs={12} sm={12} md={6}>
          <StatCard
            title="本年收益"
            value={formatNumber(dashboardData?.yearRevenue)}
            prefix="¥"
            icon={<LineChartOutlined />}
            color="#722ed1"
          />
        </Col>
        <Col xs={12} sm={12} md={6}>
          <StatCard
            title="累计收益"
            value={formatNumber(dashboardData?.totalRevenue)}
            prefix="¥"
            icon={<TrophyOutlined />}
            color="#fa8c16"
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 8 }}>
        <Col xs={12} sm={12} md={6}>
          <StatCard
            title="今日电量"
            value={formatNumber(dashboardData?.todayPower)}
            suffix="kWh"
            icon={<ThunderboltOutlined />}
            color="#13c2c2"
          />
        </Col>
        <Col xs={12} sm={12} md={6}>
          <StatCard
            title="本月电量"
            value={formatNumber(dashboardData?.monthPower)}
            suffix="kWh"
            icon={<DashboardOutlined />}
            color="#2f54eb"
          />
        </Col>
        <Col xs={12} sm={12} md={6}>
          <StatCard
            title="平均度电成本"
            value={Number(dashboardData?.avgUnitCost || 0).toFixed(4)}
            suffix="元/kWh"
            icon={<BarChartOutlined />}
            color="#eb2f96"
          />
        </Col>
        <Col xs={12} sm={12} md={6}>
          <StatCard
            title="平均上网电价"
            value={Number(dashboardData?.avgPrice || 0).toFixed(4)}
            suffix="元/kWh"
            icon={<RiseOutlined />}
            color="#faad14"
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={16}>
          <Card title="收益趋势（近30天）" loading={loading}>
            {trendData.some(d => Number(d.power) > 0 || Number(d.revenue) > 0) ? (
              <ChartCard option={revenueTrendOption} height={320} />
            ) : (
              <Empty description="暂无数据" style={{ padding: '60px 0' }} />
            )}
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="度电成本趋势（近30天）" loading={loading}>
            {trendData.some(d => Number(d.unitCost) > 0) ? (
              <ChartCard option={costTrendOption} height={320} />
            ) : (
              <Empty description="暂无数据" style={{ padding: '60px 0' }} />
            )}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={12}>
          <Card
            title={
              <span>
                <TrophyOutlined style={{ color: '#faad14', marginRight: 6 }} />
                各电站收益排名
              </span>
            }
            loading={loading}
          >
            {rankData.length > 0 ? (
              <List
                dataSource={rankData}
                renderItem={(item, index) => (
                  <List.Item key={item.stationId}>
                    <div style={{ display: 'flex', alignItems: 'center', width: '100%' }}>
                      <span
                        style={{
                          width: 24,
                          height: 24,
                          borderRadius: '50%',
                          background: index === 0 ? '#faad14' : index === 1 ? '#bfbfbf' : index === 2 ? '#d48806' : '#f0f0f0',
                          color: index < 3 ? '#fff' : '#666',
                          fontSize: 12,
                          fontWeight: 600,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          marginRight: 12,
                          flexShrink: 0
                        }}
                      >
                        {index + 1}
                      </span>
                      <span style={{ flex: 1, fontWeight: 500 }}>
                        {item.stationName || `电站${item.stationId}`}
                      </span>
                      <div style={{ textAlign: 'right' }}>
                        <div style={{ color: '#52c41a', fontWeight: 500 }}>
                          ¥{formatNumber(item.revenue)}
                        </div>
                        <div style={{ fontSize: 12, color: '#999' }}>
                          {formatNumber(item.power)} kWh
                        </div>
                      </div>
                    </div>
                  </List.Item>
                )}
              />
            ) : (
              <Empty description="暂无数据" style={{ padding: '40px 0' }} />
            )}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card
            title={
              <span>
                <BarChartOutlined style={{ color: '#eb2f96', marginRight: 6 }} />
                ROI 投资回报率
              </span>
            }
            loading={loading}
          >
            <div style={{ padding: '16px 0' }}>
              <div style={{ display: 'flex', justifyContent: 'space-around', marginBottom: 24 }}>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 36, fontWeight: 600, color: '#52c41a' }}>
                    {roiPercent.toFixed(2)}%
                  </div>
                  <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>投资回报率 (ROI)</div>
                </div>
                <div style={{ width: 1, background: '#f0f0f0' }} />
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 36, fontWeight: 600, color: '#1890ff' }}>
                    {paybackYears.toFixed(1)}
                  </div>
                  <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>投资回收年限 (年)</div>
                </div>
              </div>

              <div style={{ marginTop: 16 }}>
                <div style={{ fontSize: 13, color: '#666', marginBottom: 12 }}>
                  ROI 分析
                </div>
                <Tooltip title={`当前投资回报率 ${roiPercent.toFixed(2)}%，预计 ${paybackYears.toFixed(1)} 年收回投资成本`}>
                  <Progress
                    percent={Math.min(100, roiPercent)}
                    strokeColor={{
                      '0%': '#108ee9',
                      '100%': '#87d068'
                    }}
                    format={percent => `${percent.toFixed(2)}%`}
                  />
                </Tooltip>

                <div style={{ marginTop: 20, fontSize: 12, color: '#999', lineHeight: 1.6 }}>
                  <div style={{ marginBottom: 4 }}>
                    <Tag color="blue">说明</Tag>
                    投资回报分析指标：
                  </div>
                  <div>• ROI = 年净利润 / 总投资 × 100%</div>
                  <div>• 投资回收年限 = 总投资 / 年净利润</div>
                  <div>• 数据基于当前电价方案和实际发电量计算</div>
                </div>

                <Row gutter={16} style={{ marginTop: 20 }}>
                  <Col span={12}>
                    <Card size="small" style={{ background: '#f6ffed' }}>
                      <Statistic
                        title="总投资"
                        value={formatNumber(dashboardData?.totalInvestment)}
                        prefix="¥"
                        valueStyle={{ fontSize: 16, color: '#52c41a' }}
                      />
                    </Card>
                  </Col>
                  <Col span={12}>
                    <Card size="small" style={{ background: '#e6f7ff' }}>
                      <Statistic
                        title="年净利润"
                        value={formatNumber(dashboardData?.annualProfit)}
                        prefix="¥"
                        valueStyle={{ fontSize: 16, color: '#1890ff' }}
                      />
                    </Card>
                  </Col>
                </Row>
              </div>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default RevenueDashboard
