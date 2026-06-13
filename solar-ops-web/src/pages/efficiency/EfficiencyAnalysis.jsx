import React, { useState } from 'react'
import {
  Row,
  Col,
  Card,
  Tabs,
  Select,
  List,
  Progress,
  Space,
  DatePicker
} from 'antd'
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import dayjs from 'dayjs'

const { RangePicker } = DatePicker
const { Option } = Select

const EfficiencyAnalysis = () => {
  const [timeType, setTimeType] = useState('week')
  const [selectedStation, setSelectedStation] = useState('all')

  const prRankData = [
    { rank: 1, name: '一号光伏电站', pr: 85.6, change: 2.3 },
    { rank: 2, name: '二号光伏电站', pr: 83.2, change: 1.5 },
    { rank: 3, name: '五号光伏电站', pr: 80.8, change: -0.5 },
    { rank: 4, name: '三号光伏电站', pr: 78.5, change: 1.2 },
    { rank: 5, name: '四号光伏电站', pr: 75.3, change: -1.8 }
  ]

  const getTrendOption = () => {
    const xData = timeType === 'week'
      ? ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      : timeType === 'month'
        ? ['第1周', '第2周', '第3周', '第4周']
        : ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']

    const data1 = timeType === 'week'
      ? [82, 85, 83, 86, 84, 87, 85]
      : timeType === 'month'
        ? [83, 84, 85, 86]
        : [78, 80, 82, 84, 86, 87, 88, 87, 85, 83, 80, 78]

    const data2 = timeType === 'week'
      ? [78, 80, 82, 81, 83, 82, 84]
      : timeType === 'month'
        ? [80, 81, 82, 83]
        : [75, 77, 79, 81, 82, 84, 85, 84, 82, 80, 78, 76]

    return {
      tooltip: {
        trigger: 'axis'
      },
      legend: {
        data: ['系统效率PR', '去年同期']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: xData
      },
      yAxis: {
        type: 'value',
        name: 'PR(%)',
        min: 70,
        max: 95
      },
      series: [
        {
          name: '系统效率PR',
          type: 'line',
          smooth: true,
          data: data1,
          itemStyle: { color: '#1890ff' },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
                { offset: 1, color: 'rgba(24, 144, 255, 0.05)' }
              ]
            }
          }
        },
        {
          name: '去年同期',
          type: 'line',
          smooth: true,
          lineStyle: { type: 'dashed' },
          data: data2,
          itemStyle: { color: '#8c8c8c' }
        }
      ]
    }
  }

  const healthGaugeOption = {
    series: [
      {
        type: 'gauge',
        progress: {
          show: true,
          width: 20
        },
        axisLine: {
          lineStyle: {
            width: 20
          }
        },
        axisTick: {
          show: false
        },
        splitLine: {
          length: 15,
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
        pointer: {
          width: 6
        },
        anchor: {
          show: true,
          size: 20,
          itemStyle: {
            borderWidth: 2,
            borderColor: '#1890ff'
          }
        },
        title: {
          show: true,
          offsetCenter: [0, '75%'],
          fontSize: 14,
          color: '#666'
        },
        detail: {
          valueAnimation: true,
          fontSize: 36,
          offsetCenter: [0, '35%'],
          formatter: '{value}%'
        },
        data: [
          {
            value: 82.5,
            name: '系统健康度'
          }
        ]
      }
    ]
  }

  const comparisonOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['本周', '上周', '目标']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['一号站', '二号站', '三号站', '四号站', '五号站']
    },
    yAxis: {
      type: 'value',
      name: '发电量(kWh)',
      axisLabel: {
        formatter: '{value}k'
      }
    },
    series: [
      {
        name: '本周',
        type: 'bar',
        data: [850, 1200, 480, 1600, 950],
        itemStyle: { color: '#1890ff' },
        barWidth: '20%'
      },
      {
        name: '上周',
        type: 'bar',
        data: [820, 1150, 450, 1550, 900],
        itemStyle: { color: '#8c8c8c' },
        barWidth: '20%'
      },
      {
        name: '目标',
        type: 'bar',
        data: [900, 1300, 500, 1700, 1000],
        itemStyle: { color: '#52c41a' },
        barWidth: '20%'
      }
    ]
  }

  return (
    <div className="efficiency-analysis-page">
      <Card
        title="效率分析"
        extra={
          <Space>
            <Select
              value={selectedStation}
              onChange={setSelectedStation}
              style={{ width: 150 }}
            >
              <Option value="all">全部电站</Option>
              <Option value="1">一号光伏电站</Option>
              <Option value="2">二号光伏电站</Option>
              <Option value="3">三号光伏电站</Option>
              <Option value="4">四号光伏电站</Option>
              <Option value="5">五号光伏电站</Option>
            </Select>
            <Tabs
              activeKey={timeType}
              onChange={setTimeType}
              size="small"
              items={[
                { key: 'week', label: '周' },
                { key: 'month', label: '月' },
                { key: 'year', label: '年' }
              ]}
            />
          </Space>
        }
      >
        <Row gutter={[16, 16]}>
          <Col xs={24} lg={8}>
            <Card title="健康度仪表盘" className="health-gauge-card">
              <ReactECharts option={healthGaugeOption} style={{ height: 250 }} />
            </Card>
          </Col>
          <Col xs={24} lg={16}>
            <Card title="系统效率PR趋势">
              <ReactECharts option={getTrendOption()} style={{ height: 300 }} />
            </Card>
          </Col>
        </Row>

        <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
          <Col xs={24} lg={8}>
            <Card title="PR排名">
              <List
                dataSource={prRankData}
                renderItem={(item) => (
                  <List.Item key={item.rank}>
                    <div className="pr-rank-item">
                      <span className={`pr-rank-num rank-${item.rank}`}>{item.rank}</span>
                      <span className="pr-rank-name">{item.name}</span>
                      <div className="pr-rank-right">
                        <span className="pr-rank-value">{item.pr}%</span>
                        <span className={`pr-rank-change ${item.change > 0 ? 'up' : 'down'}`}>
                          {item.change > 0 ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
                          {Math.abs(item.change)}%
                        </span>
                      </div>
                    </div>
                    <Progress
                      percent={item.pr}
                      showInfo={false}
                      strokeColor={item.rank <= 3 ? '#52c41a' : '#1890ff'}
                      style={{ marginTop: 8 }}
                    />
                  </List.Item>
                )}
              />
            </Card>
          </Col>
          <Col xs={24} lg={16}>
            <Card title="发电量对比分析">
              <ReactECharts option={comparisonOption} style={{ height: 300 }} />
            </Card>
          </Col>
        </Row>
      </Card>
    </div>
  )
}

export default EfficiencyAnalysis
