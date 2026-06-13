import React, { useState, useEffect } from 'react'
import { Row, Col, Card, List } from 'antd'
import {
  ThunderboltOutlined,
  RiseOutlined,
  ApartmentOutlined,
  HeartOutlined
} from '@ant-design/icons'
import StatCard from '../../components/StatCard'
import ChartCard from '../../components/ChartCard'
import StatusTag from '../../components/StatusTag'

const Dashboard = () => {
  const [loading, setLoading] = useState(true)

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
    </div>
  )
}

export default Dashboard
