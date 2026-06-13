import React, { useState, useEffect, useCallback } from 'react'
import {
  Row,
  Col,
  Card,
  Tabs,
  Select,
  List,
  Progress,
  Space,
  Spin
} from 'antd'
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import dayjs from 'dayjs'
import {
  getEfficiencyRank,
  getStationEfficiency,
  getStationHealthList
} from '../../api/efficiency'
import { getStationListAll } from '../../api/station'

const { Option } = Select

const TIME_TYPE_MAP = { week: 2, month: 3, year: 4 }

const getDateByTimeType = (timeType) => {
  return dayjs().format('YYYY-MM-DD')
}

const getDateRangeByTimeType = (timeType) => {
  const now = dayjs()
  if (timeType === 'week') {
    return { startDate: now.startOf('week').format('YYYY-MM-DD'), endDate: now.endOf('week').format('YYYY-MM-DD') }
  }
  if (timeType === 'month') {
    return { startDate: now.startOf('month').format('YYYY-MM-DD'), endDate: now.endOf('month').format('YYYY-MM-DD') }
  }
  return { startDate: now.startOf('year').format('YYYY-MM-DD'), endDate: now.endOf('year').format('YYYY-MM-DD') }
}

const EfficiencyAnalysis = () => {
  const [timeType, setTimeType] = useState('week')
  const [selectedStation, setSelectedStation] = useState('all')
  const [stationList, setStationList] = useState([])
  const [prRankData, setPrRankData] = useState([])
  const [healthValue, setHealthValue] = useState(0)
  const [trendData, setTrendData] = useState({})
  const [comparisonData, setComparisonData] = useState({})
  const [loading, setLoading] = useState(false)

  const fetchStationList = useCallback(async () => {
    try {
      const res = await getStationListAll()
      setStationList(res.data || [])
    } catch (e) {
      console.error('获取电站列表失败', e)
    }
  }, [])

  const fetchPrRank = useCallback(async (tType) => {
    try {
      const res = await getEfficiencyRank({
        statisticsType: TIME_TYPE_MAP[tType],
        date: getDateByTimeType(tType),
        topN: 10
      })
      setPrRankData(res.data || [])
    } catch (e) {
      console.error('获取PR排名失败', e)
    }
  }, [])

  const fetchHealth = useCallback(async () => {
    try {
      const stationIds = selectedStation === 'all'
        ? stationList.map(s => s.id)
        : [selectedStation]
      if (stationIds.length === 0) return
      const res = await getStationHealthList(stationIds)
      const list = res.data || []
      if (list.length > 0) {
        const avg = list.reduce((sum, item) => sum + (item.healthScore || 0), 0) / list.length
        setHealthValue(Number(avg.toFixed(1)))
      }
    } catch (e) {
      console.error('获取健康度失败', e)
    }
  }, [selectedStation, stationList])

  const fetchEfficiencyTrend = useCallback(async (tType) => {
    try {
      const { startDate, endDate } = getDateRangeByTimeType(tType)
      const stationId = selectedStation === 'all' ? 0 : selectedStation
      const res = await getStationEfficiency(stationId, {
        statisticsType: TIME_TYPE_MAP[tType],
        startDate,
        endDate
      })
      setTrendData(res.data || {})
    } catch (e) {
      console.error('获取效率趋势失败', e)
    }
  }, [selectedStation])

  const fetchComparison = useCallback(async (tType) => {
    try {
      const stationIds = selectedStation === 'all'
        ? stationList.slice(0, 5).map(s => s.id)
        : [selectedStation]
      const { startDate, endDate } = getDateRangeByTimeType(tType)
      const results = await Promise.all(
        stationIds.map(id => getStationEfficiency(id, {
          statisticsType: TIME_TYPE_MAP[tType],
          startDate,
          endDate
        }))
      )
      setComparisonData({
        stationIds,
        results: results.map(r => r.data || {})
      })
    } catch (e) {
      console.error('获取对比数据失败', e)
    }
  }, [selectedStation, stationList])

  const fetchAllData = useCallback(async (tType) => {
    setLoading(true)
    await Promise.all([
      fetchPrRank(tType),
      fetchHealth(),
      fetchEfficiencyTrend(tType),
      fetchComparison(tType)
    ])
    setLoading(false)
  }, [fetchPrRank, fetchHealth, fetchEfficiencyTrend, fetchComparison])

  useEffect(() => {
    fetchStationList()
  }, [fetchStationList])

  useEffect(() => {
    if (stationList.length > 0) {
      fetchAllData(timeType)
    }
  }, [stationList.length, timeType, selectedStation, fetchAllData])

  const getTrendOption = () => {
    const xData = trendData.xAxis || []
    const currentData = trendData.current || []
    const lastYearData = trendData.lastYear || []

    return {
      tooltip: { trigger: 'axis' },
      legend: { data: ['系统效率PR', '去年同期'] },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: xData },
      yAxis: { type: 'value', name: 'PR(%)', min: 70, max: 95 },
      series: [
        {
          name: '系统效率PR',
          type: 'line',
          smooth: true,
          data: currentData,
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
          data: lastYearData,
          itemStyle: { color: '#8c8c8c' }
        }
      ]
    }
  }

  const healthGaugeOption = {
    series: [
      {
        type: 'gauge',
        progress: { show: true, width: 20 },
        axisLine: { lineStyle: { width: 20 } },
        axisTick: { show: false },
        splitLine: {
          length: 15,
          lineStyle: { width: 2, color: '#999' }
        },
        axisLabel: { distance: 25, color: '#999', fontSize: 12 },
        pointer: { width: 6 },
        anchor: {
          show: true,
          size: 20,
          itemStyle: { borderWidth: 2, borderColor: '#1890ff' }
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
        data: [{ value: healthValue, name: '系统健康度' }]
      }
    ]
  }

  const getComparisonOption = () => {
    const { stationIds: ids, results } = comparisonData
    if (!ids || ids.length === 0) {
      return {
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { data: ['当前', '上期', '目标'] },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: [] },
        yAxis: { type: 'value', name: '发电量(kWh)', axisLabel: { formatter: '{value}k' } },
        series: [
          { name: '当前', type: 'bar', data: [], itemStyle: { color: '#1890ff' }, barWidth: '20%' },
          { name: '上期', type: 'bar', data: [], itemStyle: { color: '#8c8c8c' }, barWidth: '20%' },
          { name: '目标', type: 'bar', data: [], itemStyle: { color: '#52c41a' }, barWidth: '20%' }
        ]
      }
    }
    const xData = ids.map(id => {
      const station = stationList.find(s => s.id === id)
      return station ? station.stationName : `电站${id}`
    })
    const currentVals = results.map(r => r.current || 0)
    const previousVals = results.map(r => r.previous || 0)
    const targetVals = results.map(r => r.target || 0)

    return {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['当前', '上期', '目标'] },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: xData },
      yAxis: { type: 'value', name: '发电量(kWh)', axisLabel: { formatter: '{value}k' } },
      series: [
        { name: '当前', type: 'bar', data: currentVals, itemStyle: { color: '#1890ff' }, barWidth: '20%' },
        { name: '上期', type: 'bar', data: previousVals, itemStyle: { color: '#8c8c8c' }, barWidth: '20%' },
        { name: '目标', type: 'bar', data: targetVals, itemStyle: { color: '#52c41a' }, barWidth: '20%' }
      ]
    }
  }

  const handleTimeTypeChange = (key) => {
    setTimeType(key)
  }

  const handleStationChange = (value) => {
    setSelectedStation(value)
  }

  return (
    <div className="efficiency-analysis-page">
      <Card
        title="效率分析"
        extra={
          <Space>
            <Select
              value={selectedStation}
              onChange={handleStationChange}
              style={{ width: 150 }}
            >
              <Option value="all">全部电站</Option>
              {stationList.map(s => (
                <Option key={s.id} value={s.id}>{s.stationName}</Option>
              ))}
            </Select>
            <Tabs
              activeKey={timeType}
              onChange={handleTimeTypeChange}
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
        <Spin spinning={loading}>
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
                  renderItem={(item, index) => (
                    <List.Item key={item.stationName || index}>
                      <div className="pr-rank-item">
                        <span className={`pr-rank-num rank-${index + 1}`}>{index + 1}</span>
                        <span className="pr-rank-name">{item.stationName}</span>
                        <div className="pr-rank-right">
                          <span className="pr-rank-value">{item.prValue}%</span>
                          <span className={`pr-rank-change ${item.change > 0 ? 'up' : 'down'}`}>
                            {item.change > 0 ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
                            {Math.abs(item.change)}%
                          </span>
                        </div>
                      </div>
                      <Progress
                        percent={item.prValue}
                        showInfo={false}
                        strokeColor={index < 3 ? '#52c41a' : '#1890ff'}
                        style={{ marginTop: 8 }}
                      />
                    </List.Item>
                  )}
                />
              </Card>
            </Col>
            <Col xs={24} lg={16}>
              <Card title="发电量对比分析">
                <ReactECharts option={getComparisonOption()} style={{ height: 300 }} />
              </Card>
            </Col>
          </Row>
        </Spin>
      </Card>
    </div>
  )
}

export default EfficiencyAnalysis
