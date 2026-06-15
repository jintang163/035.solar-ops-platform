import React, { useState, useEffect } from 'react'
import { Row, Col, Card, List, Progress } from 'antd'
import {
  ThunderboltOutlined,
  RiseOutlined,
  ApartmentOutlined,
  HeartOutlined,
  DropletOutlined,
  DollarOutlined,
  BellOutlined,
  TrophyOutlined
} from '@ant-design/icons'
import StatCard from '../../components/StatCard'
import ChartCard from '../../components/ChartCard'
import StatusTag from '../../components/StatusTag'
import { getCleaningDashboard } from '../../api/cleaning'

const Dashboard = () => {
  const [loading, setLoading] = useState(true)
  const [cleaningStats, setCleaningStats] = useState({
    totalCleaningCount: 0,
    totalImprovedEnergy: 0,
    totalSavedCost: 0,
    pendingPlanCount: 0,
    pendingReminderCount: 0,
    averageImprovementRate: 0,
    cleaningTrend: [],
    dustLevelStats: [],
    stationRanks: []
  })

  useEffect(() => {
    loadCleaningStats()
    const timer = setTimeout(() => {
      setLoading(false)
    }, 500)
    return () => clearTimeout(timer)
  }, [])

  const loadCleaningStats = async () => {
    try {
      const data = await getCleaningDashboard()
      if (data) {
        setCleaningStats({
          totalCleaningCount: data.totalCleaningCount || 0,
          totalImprovedEnergy: data.totalImprovedEnergy || 0,
          totalSavedCost: data.totalSavedCost || 0,
          pendingPlanCount: data.pendingPlanCount || 0,
          pendingReminderCount: data.pendingReminderCount || 0,
          averageImprovementRate: data.averageImprovementRate || 0,
          cleaningTrend: data.cleaningTrend || [],
          dustLevelStats: data.dustLevelStats || [
            { level: 0, levelName: '无积灰', count: 0, color: '#52c41a' },
            { level: 1, levelName: '轻度', count: 0, color: '#faad14' },
            { level: 2, levelName: '中度', count: 0, color: '#fa8c16' },
            { level: 3, levelName: '重度', count: 0, color: '#f5222d' }
          ],
          stationRanks: data.stationRanks || []
        })
      }
    } catch (err) {
      console.error('加载清洗统计失败:', err)
    }
  }

  useEffect(() => {
    const timer = setTimeout(() => {
      setLoading(false)
    }, 500)
    return () => clearTimeout(timer)
  }, [])

  const generationTrendOption = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['发电量', '计划发电量']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['00:00', '02:00', '04:00', '06:00', '08:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00']
    },
    yAxis: {
      type: 'value',
      name: 'kWh'
    },
    series: [
      {
        name: '发电量',
        type: 'line',
        smooth: true,
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
              { offset: 1, color: 'rgba(24, 144, 255, 0.05)' }
            ]
          }
        },
        lineStyle: {
          color: '#1890ff',
          width: 2
        },
        itemStyle: {
          color: '#1890ff'
        },
        data: [0, 0, 0, 50, 200, 450, 680, 720, 580, 300, 80, 0]
      },
      {
        name: '计划发电量',
        type: 'line',
        smooth: true,
        lineStyle: {
          color: '#faad14',
          width: 2,
          type: 'dashed'
        },
        itemStyle: {
          color: '#faad14'
        },
        data: [0, 0, 0, 60, 220, 500, 700, 750, 600, 320, 100, 0]
      }
    ]
  }

  const healthGaugeOption = {
    tooltip: {
      formatter: '{b}: {c}%'
    },
    series: [
      {
        name: '健康度',
        type: 'gauge',
        progress: {
          show: true,
          width: 18
        },
        axisLine: {
          lineStyle: {
            width: 18
          }
        },
        axisTick: {
          show: false
        },
        splitLine: {
          length: 12,
          lineStyle: {
            width: 2,
            color: '#999'
          }
        },
        axisLabel: {
          distance: 25,
          color: '#999',
          fontSize: 12
        },
        anchor: {
          show: true,
          showAbove: true,
          size: 20,
          itemStyle: {
            borderWidth: 10,
            borderColor: '#1890ff'
          }
        },
        title: {
          show: true,
          offsetCenter: [0, '70%'],
          fontSize: 14,
          color: '#666'
        },
        detail: {
          valueAnimation: true,
          fontSize: 32,
          offsetCenter: [0, '30%'],
          formatter: '{value}%'
        },
        data: [
          {
            value: 86.5,
            name: '电站健康度'
          }
        ]
      }
    ]
  }

  const stationHealthList = [
    { name: '一号光伏电站', health: 92, status: 'excellent' },
    { name: '二号光伏电站', health: 85, status: 'good' },
    { name: '三号光伏电站', health: 78, status: 'normal' },
    { name: '四号光伏电站', health: 65, status: 'poor' },
    { name: '五号光伏电站', health: 88, status: 'good' }
  ]

  const pieOption = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      bottom: 0,
      left: 'center'
    },
    series: [
      {
        name: '设备状态',
        type: 'pie',
        radius: ['40%', '65%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
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
            fontSize: 18,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: 156, name: '在线', itemStyle: { color: '#52c41a' } },
          { value: 12, name: '离线', itemStyle: { color: '#8c8c8c' } },
          { value: 5, name: '故障', itemStyle: { color: '#ff4d4f' } },
          { value: 8, name: '告警', itemStyle: { color: '#faad14' } }
        ]
      }
    ]
  }

  const cleaningTrendData = cleaningStats.cleaningTrend.length > 0 ? cleaningStats.cleaningTrend : [
    { date: '6-01', improvedEnergy: 120, cleaningCount: 2 },
    { date: '6-05', improvedEnergy: 180, cleaningCount: 3 },
    { date: '6-10', improvedEnergy: 250, cleaningCount: 2 },
    { date: '6-15', improvedEnergy: 320, cleaningCount: 4 },
    { date: '6-20', improvedEnergy: 280, cleaningCount: 3 },
    { date: '6-25', improvedEnergy: 380, cleaningCount: 5 },
    { date: '6-30', improvedEnergy: 420, cleaningCount: 4 }
  ]

  const cleaningTrendOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['提升电量(kWh)', '清洗次数'],
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: 40,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: cleaningTrendData.map(d => d.date)
    },
    yAxis: [
      {
        type: 'value',
        name: 'kWh',
        position: 'left'
      },
      {
        type: 'value',
        name: '次',
        position: 'right'
      }
    ],
    series: [
      {
        name: '提升电量(kWh)',
        type: 'bar',
        yAxisIndex: 0,
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
        data: cleaningTrendData.map(d => d.improvedEnergy || 0)
      },
      {
        name: '清洗次数',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        lineStyle: { color: '#fa8c16', width: 2 },
        itemStyle: { color: '#fa8c16' },
        data: cleaningTrendData.map(d => d.cleaningCount || 0)
      }
    ]
  }

  const totalDustCount = (cleaningStats.dustLevelStats || []).reduce((s, d) => s + (d.count || 0), 0) || 1

  return (
    <div className="dashboard-page">
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={6}>
          <StatCard
            title="总发电量"
            value={12586.42}
            suffix="万kWh"
            icon={<ThunderboltOutlined />}
            color="#1890ff"
            trend="up"
            trendValue="12.5%"
          />
        </Col>
        <Col xs={24} sm={12} md={6}>
          <StatCard
            title="今日发电量"
            value={3658.2}
            suffix="kWh"
            icon={<RiseOutlined />}
            color="#52c41a"
            trend="up"
            trendValue="8.3%"
          />
        </Col>
        <Col xs={24} sm={12} md={6}>
          <StatCard
            title="设备数量"
            value={181}
            suffix="台"
            icon={<ApartmentOutlined />}
            color="#722ed1"
          />
        </Col>
        <Col xs={24} sm={12} md={6}>
          <StatCard
            title="设备在线率"
            value={94.5}
            suffix="%"
            icon={<HeartOutlined />}
            color="#faad14"
            trend="up"
            trendValue="2.1%"
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} sm={12} md={6}>
          <StatCard
            title="清洗提升电量"
            value={cleaningStats.totalImprovedEnergy || 12850}
            suffix="kWh"
            icon={<DropletOutlined />}
            color="#13c2c2"
            trend="up"
            trendValue={`${cleaningStats.averageImprovementRate || 10.2}%`}
          />
        </Col>
        <Col xs={24} sm={12} md={6}>
          <StatCard
            title="累计清洗次数"
            value={cleaningStats.totalCleaningCount || 48}
            suffix="次"
            icon={<TrophyOutlined />}
            color="#52c41a"
          />
        </Col>
        <Col xs={24} sm={12} md={6}>
          <StatCard
            title="待处理提醒"
            value={cleaningStats.pendingReminderCount || 6}
            suffix="条"
            icon={<BellOutlined />}
            color="#fa8c16"
          />
        </Col>
        <Col xs={24} sm={12} md={6}>
          <StatCard
            title="节省费用"
            value={cleaningStats.totalSavedCost || 8650}
            suffix="元"
            icon={<DollarOutlined />}
            color="#f5222d"
            trend="up"
            trendValue="ROI 3.5x"
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={16}>
          <ChartCard
            title="发电量趋势"
            option={generationTrendOption}
            height={320}
            loading={loading}
          />
        </Col>
        <Col xs={24} lg={8}>
          <ChartCard
            title="设备状态分布"
            option={pieOption}
            height={320}
            loading={loading}
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={8}>
          <Card title="电站健康度概览" loading={loading} className="health-card">
            <div style={{ height: 200 }}>
              <ChartCard option={healthGaugeOption} height={200} />
            </div>
          </Card>
        </Col>
        <Col xs={24} lg={16}>
          <Card title="各电站健康度排名" loading={loading}>
            <List
              dataSource={stationHealthList}
              renderItem={(item, index) => (
                <List.Item key={item.name}>
                  <div className="health-list-item">
                    <span className={`health-rank rank-${index + 1}`}>{index + 1}</span>
                    <span className="health-name">{item.name}</span>
                    <div className="health-bar">
                      <div
                        className="health-bar-inner"
                        style={{ width: `${item.health}%` }}
                      />
                    </div>
                    <span className="health-value">{item.health}%</span>
                    <StatusTag status={item.status} />
                  </div>
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={16}>
          <ChartCard
            title="💧 清洗效果趋势（近30天）"
            option={cleaningTrendOption}
            height={300}
            loading={loading}
          />
        </Col>
        <Col xs={24} lg={8}>
          <Card title="📊 积灰等级分布" loading={loading} className="dust-dist-card">
            <div className="dust-stats">
              {(cleaningStats.dustLevelStats.length > 0 ? cleaningStats.dustLevelStats : [
                { level: 0, levelName: '无积灰', count: 45, color: '#52c41a' },
                { level: 1, levelName: '轻度', count: 18, color: '#faad14' },
                { level: 2, levelName: '中度', count: 8, color: '#fa8c16' },
                { level: 3, levelName: '重度', count: 3, color: '#f5222d' }
              ]).map(item => (
                <div key={item.level} className="dust-stat-item">
                  <div className="dust-stat-head">
                    <span className="dust-dot" style={{ backgroundColor: item.color }} />
                    <span className="dust-name">{item.levelName}</span>
                    <span className="dust-count">{item.count} 处</span>
                  </div>
                  <Progress
                    percent={Math.round((item.count / totalDustCount) * 100)}
                    showInfo={false}
                    strokeColor={item.color}
                    size="small"
                  />
                </div>
              ))}
            </div>
            <div className="dust-total-note">
              共检测 <strong>{totalDustCount}</strong> 个方阵/逆变器单元
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={24}>
          <Card
            title="🏆 各电站清洗效果排名"
            loading={loading}
            extra={<a href="#/cleaning/dashboard">查看详细仪表盘 →</a>}
          >
            <List
              dataSource={cleaningStats.stationRanks.length > 0 ? cleaningStats.stationRanks : [
                { stationName: '一号光伏电站', totalCleaningCount: 12, improvedEnergy: 3850, improvementRate: 11.2, roi: 3.8 },
                { stationName: '三号光伏电站', totalCleaningCount: 10, improvedEnergy: 3200, improvementRate: 10.8, roi: 3.5 },
                { stationName: '二号光伏电站', totalCleaningCount: 8, improvedEnergy: 2680, improvementRate: 9.5, roi: 3.2 },
                { stationName: '五号光伏电站', totalCleaningCount: 7, improvedEnergy: 1950, improvementRate: 8.7, roi: 2.9 },
                { stationName: '四号光伏电站', totalCleaningCount: 5, improvedEnergy: 1170, improvementRate: 7.3, roi: 2.4 }
              ]}
              renderItem={(item, index) => (
                <List.Item key={item.stationName}>
                  <div className="cleaning-rank-item">
                    <span className={`cleaning-rank rank-${index + 1}`}>{index + 1}</span>
                    <span className="cleaning-station">{item.stationName}</span>
                    <div className="cleaning-info-group">
                      <span className="cleaning-info-chip">
                        清洗 {item.totalCleaningCount} 次
                      </span>
                      <span className="cleaning-info-chip energy">
                        +{item.improvedEnergy} kWh
                      </span>
                      <Progress
                        className="cleaning-progress"
                        percent={item.improvementRate}
                        format={percent => `${percent}%`}
                        strokeColor="#52c41a"
                        size="small"
                      />
                      <span className={`cleaning-roi roi-${item.roi >= 3 ? 'high' : item.roi >= 2.5 ? 'mid' : 'low'}`}>
                        ROI {item.roi}x
                      </span>
                    </div>
                  </div>
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Dashboard
