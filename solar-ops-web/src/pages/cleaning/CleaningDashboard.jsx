import React, { useState, useEffect, useCallback } from 'react'
import {
  Card,
  Row,
  Col,
  Statistic,
  Progress,
  List,
  Select,
  Empty,
  Tooltip,
  Tag
} from 'antd'
import {
  ThunderboltOutlined,
  RiseOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  PlayCircleOutlined,
  ClockCircleOutlined,
  WarningOutlined,
  DollarOutlined,
  FileTextOutlined,
  TrophyOutlined,
  HeartOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import {
  getCleaningDashboard,
  getImprovementTrend,
  getStationCleaningRank,
  getDustLevelStats
} from '../../api/cleaning'

const { Option } = Select

const CleaningDashboard = () => {
  const [loading, setLoading] = useState(false)
  const [stationId, setStationId] = useState(null)
  const [dashboardData, setDashboardData] = useState(null)
  const [trendData, setTrendData] = useState([])
  const [rankData, setRankData] = useState([])
  const [dustLevelData, setDustLevelData] = useState([])

  const fetchDashboardData = useCallback(async (sid) => {
    setLoading(true)
    try {
      const params = sid ? { stationId: sid } : {}
      const [dashboardRes, trendRes, rankRes, dustRes] = await Promise.all([
        getCleaningDashboard(params),
        getImprovementTrend(params),
        getStationCleaningRank(params),
        getDustLevelStats(params)
      ])
      setDashboardData(dashboardRes.data || {})
      setTrendData(trendRes.data || [])
      setRankData(rankRes.data || [])
      setDustLevelData(dustRes.data || [])
    } catch {
      setDashboardData({})
      setTrendData([])
      setRankData([])
      setDustLevelData([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchDashboardData(stationId)
  }, [stationId, fetchDashboardData])

  const trendOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['清洗次数', '提升发电量'],
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
      boundaryGap: false,
      data: trendData.map(d => d.date.slice(5)),
      axisLabel: {
        fontSize: 11
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '清洗次数',
        axisLabel: {
          fontSize: 11
        }
      },
      {
        type: 'value',
        name: '提升量(kWh)',
        axisLabel: {
          fontSize: 11
        }
      }
    ],
    series: [
      {
        name: '清洗次数',
        type: 'bar',
        data: trendData.map(d => d.cleaningCount),
        itemStyle: {
          color: '#1890ff'
        },
        barWidth: '40%'
      },
      {
        name: '提升发电量',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: trendData.map(d => Number(d.improvedEnergy || 0)),
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

  const pieOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      bottom: 0,
      left: 'center',
      itemWidth: 12,
      itemHeight: 12,
      textStyle: {
        fontSize: 11
      }
    },
    series: [
      {
        name: '积灰分布',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: dustLevelData.filter(d => d.count > 0).map(d => ({
          value: d.count,
          name: d.dustLevelDesc,
          itemStyle: { color: d.color }
        }))
      }
    ]
  }

  const formatNumber = (num, decimals = 2) => {
    if (num == null) return 0
    const n = Number(num)
    if (n >= 10000) {
      return (n / 10000).toFixed(decimals) + '万'
    }
    return n.toFixed(decimals)
  }

  const savedCostTip = (
    <div>
      按 0.5元/kWh 电价估算：<br />
      提升发电量 × 0.5元/kWh
    </div>
  )

  return (
    <div className="cleaning-dashboard-page">
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
          <Card loading={loading}>
            <Statistic
              title="累计清洗次数"
              value={dashboardData?.totalCleaningCount || 0}
              suffix="次"
              prefix={<CalendarOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={12} md={6}>
          <Card loading={loading}>
            <Statistic
              title="本月清洗次数"
              value={dashboardData?.monthlyCleaningCount || 0}
              suffix="次"
              prefix={<FileTextOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={12} md={6}>
          <Card loading={loading}>
            <Statistic
              title="待执行计划"
              value={dashboardData?.pendingPlanCount || 0}
              suffix="个"
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={12} md={6}>
          <Card loading={loading}>
            <Statistic
              title="执行中计划"
              value={dashboardData?.inProgressPlanCount || 0}
              suffix="个"
              prefix={<PlayCircleOutlined />}
              valueStyle={{ color: '#13c2c2' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 8 }}>
        <Col xs={12} sm={12} md={6}>
          <Card loading={loading}>
            <Statistic
              title="未处理清洗提醒"
              value={dashboardData?.unhandledReminderCount || 0}
              suffix="条"
              prefix={<WarningOutlined />}
              valueStyle={{ color: '#fa541c' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={12} md={6}>
          <Card loading={loading}>
            <Statistic
              title="累计提升发电量"
              value={formatNumber(dashboardData?.totalImprovedEnergy)}
              suffix="kWh"
              prefix={<RiseOutlined />}
              valueStyle={{ color: '#52c41a', fontWeight: 600 }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={12} md={6}>
          <Card loading={loading}>
            <Statistic
              title="本月提升发电量"
              value={formatNumber(dashboardData?.monthlyImprovedEnergy)}
              suffix="kWh"
              prefix={<ThunderboltOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={12} md={6}>
          <Card loading={loading}>
            <Tooltip title={savedCostTip}>
              <Statistic
                title="累计节省费用估算"
                value={formatNumber(dashboardData?.totalSavedCost)}
                prefix="¥"
                valueStyle={{ color: '#eb2f96' }}
                suffix={<DollarOutlined style={{ fontSize: 14 }} />}
              />
            </Tooltip>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={16}>
          <Card title="近30天清洗效果趋势" loading={loading}>
            {trendData.some(d => d.cleaningCount > 0 || Number(d.improvedEnergy) > 0) ? (
              <ReactECharts
                option={trendOption}
                style={{ height: 320 }}
                notMerge
                lazyUpdate
              />
            ) : (
              <Empty description="暂无数据" style={{ padding: '60px 0' }} />
            )}
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="积灰等级分布（近7日）" loading={loading}>
            {dustLevelData.some(d => d.count > 0) ? (
              <ReactECharts
                option={pieOption}
                style={{ height: 320 }}
                notMerge
                lazyUpdate
              />
            ) : (
              <Empty description="暂无数据" style={{ padding: '60px 0' }} />
            )}
            <div style={{ marginTop: 12 }}>
              {dustLevelData.map(d => (
                <div key={d.dustLevel} style={{ display: 'flex', alignItems: 'center', marginBottom: 8 }}>
                  <span
                    style={{
                      width: 10,
                      height: 10,
                      borderRadius: 2,
                      background: d.color,
                      marginRight: 8,
                      flexShrink: 0
                    }}
                  />
                  <span style={{ flex: 1, fontSize: 12, color: '#666' }}>{d.dustLevelDesc}</span>
                  <Progress
                    percent={Number((d.ratio * 100).toFixed(1))}
                    size="small"
                    style={{ width: 100 }}
                    strokeColor={d.color}
                    showInfo={false}
                  />
                  <span style={{ width: 48, textAlign: 'right', fontSize: 12 }}>{d.count}次</span>
                </div>
              ))}
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={12}>
          <Card
            title={
              <span>
                <TrophyOutlined style={{ color: '#faad14', marginRight: 6 }} />
                电站清洗效果排名
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
                          +{formatNumber(item.improvedEnergy)} kWh
                        </div>
                        <div style={{ fontSize: 12, color: '#999' }}>
                          清洗 {item.cleaningCount} 次
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
                <HeartOutlined style={{ color: '#eb2f96', marginRight: 6 }} />
                清洗ROI概览
              </span>
            }
            loading={loading}
          >
            <div style={{ padding: '16px 0' }}>
              <div style={{ display: 'flex', justifyContent: 'space-around', marginBottom: 24 }}>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 28, fontWeight: 600, color: '#52c41a' }}>
                    +{formatNumber(dashboardData?.totalImprovedEnergy)}
                  </div>
                  <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>累计提升 (kWh)</div>
                </div>
                <div style={{ width: 1, background: '#f0f0f0' }} />
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 28, fontWeight: 600, color: '#eb2f96' }}>
                    ¥{formatNumber(dashboardData?.totalSavedCost)}
                  </div>
                  <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>节省费用 (元)</div>
                </div>
                <div style={{ width: 1, background: '#f0f0f0' }} />
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 28, fontWeight: 600, color: '#1890ff' }}>
                    ¥{formatNumber(dashboardData?.monthlyCleaningCost)}
                  </div>
                  <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>本月投入 (元)</div>
                </div>
              </div>

              <div style={{ marginTop: 16 }}>
                <div style={{ fontSize: 13, color: '#666', marginBottom: 12 }}>
                  <CheckCircleOutlined style={{ color: '#52c41a', marginRight: 6 }} />
                  投入产出分析
                </div>
                <div style={{ background: '#f6ffed', borderRadius: 6, padding: 12, marginBottom: 12 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                    <span style={{ color: '#666' }}>ROI（投入产出比）</span>
                    <span style={{ color: '#52c41a', fontWeight: 600 }}>
                      {Number(dashboardData?.monthlyCleaningCost) > 0
                        ? `${((Number(dashboardData?.totalSavedCost || 0) / Math.max(Number(dashboardData?.monthlyCleaningCost), 1))).toFixed(2)} : 1`
                        : '—'}
                    </span>
                  </div>
                  <Progress
                    percent={
                      Number(dashboardData?.monthlyCleaningCost) > 0
                        ? Math.min(100, Number((Number(dashboardData?.totalSavedCost || 0) / (Number(dashboardData?.monthlyCleaningCost) * 10) * 100).toFixed(1)))
                        : 0
                    }
                    strokeColor="#52c41a"
                    trailColor="#b7eb8f"
                  />
                </div>
                <div style={{ fontSize: 12, color: '#999', lineHeight: 1.6 }}>
                  <div style={{ marginBottom: 4 }}>
                    <Tag color="blue">说明</Tag>
                    定期清洗组件可显著提升发电量：
                  </div>
                  <div>• 轻度积灰（5%-10%衰减）：月均损失约 3-5% 发电量</div>
                  <div>• 中度积灰（10%-15%衰减）：月均损失约 8-12% 发电量</div>
                  <div>• 重度积灰（>15%衰减）：月均损失约 15-25% 发电量，建议立即清洗</div>
                </div>
              </div>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default CleaningDashboard
