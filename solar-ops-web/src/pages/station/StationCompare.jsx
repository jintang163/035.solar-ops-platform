import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Tabs,
  Select,
  DatePicker,
  Button,
  Table,
  Card,
  Row,
  Col,
  Space,
  Alert,
  Input,
  message,
  Spin,
  Tag,
  Tooltip
} from 'antd'
import {
  DownloadOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  PlusOutlined,
  MinusCircleOutlined,
  TrophyOutlined,
  WarningOutlined,
  BulbOutlined,
  ThunderboltOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import dayjs from 'dayjs'
import { compareStations, exportCompareReport } from '../../api/efficiency'
import { getStationListAll } from '../../api/station'

const { RangePicker } = DatePicker
const { Option } = Select

const STATISTICS_TYPE_MAP = {
  day: 1,
  week: 2,
  month: 3,
  year: 4
}

const METRICS_CONFIG = [
  { key: 'pr', label: 'PR', unit: '%', weight: 0.4, positive: true },
  { key: 'systemEfficiency', label: '系统效率', unit: '%', weight: 0, positive: true },
  { key: 'equivalentHours', label: '等效小时', unit: 'h', weight: 0.2, positive: true },
  { key: 'generation', label: '发电量', unit: 'kWh', weight: 0, positive: true },
  { key: 'faultRate', label: '故障率', unit: '%', weight: -0.3, positive: false },
  { key: 'healthScore', label: '健康度', unit: '%', weight: 0.1, positive: true },
  { key: 'onlineRate', label: '在线率', unit: '%', weight: 0, positive: true },
  { key: 'capacity', label: '装机', unit: 'kW', weight: 0, positive: true },
  { key: 'revenue', label: '收益', unit: '元', weight: 0, positive: true }
]

const CHART_METRIC_OPTIONS = [
  { key: 'pr', label: 'PR' },
  { key: 'equivalentHours', label: '等效小时' },
  { key: 'faultRate', label: '故障率' },
  { key: 'generation', label: '发电量' },
  { key: 'healthScore', label: '健康度' }
]

const STATION_COLORS = [
  '#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1',
  '#13c2c2', '#eb2f96', '#fa8c16', '#a0d911', '#2f54eb'
]

const StationCompare = () => {
  const [mode, setMode] = useState('multi')
  const [stationList, setStationList] = useState([])
  const [selectedStations, setSelectedStations] = useState([])
  const [singleStation, setSingleStation] = useState(null)
  const [periods, setPeriods] = useState([
    { id: Date.now(), label: '对比组1', range: null }
  ])
  const [statisticsType, setStatisticsType] = useState('month')
  const [dateRange, setDateRange] = useState([
    dayjs().startOf('month'),
    dayjs().endOf('month')
  ])
  const [loading, setLoading] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [compareData, setCompareData] = useState(null)
  const [barChartMetric, setBarChartMetric] = useState('pr')

  const fetchStationList = useCallback(async () => {
    try {
      const res = await getStationListAll()
      setStationList(res.data || [])
    } catch (e) {
      console.error('获取电站列表失败', e)
    }
  }, [])

  useEffect(() => {
    fetchStationList()
  }, [fetchStationList])

  const handleCompare = async () => {
    if (mode === 'multi') {
      if (selectedStations.length < 2) {
        message.warning('请至少选择2个电站进行对比')
        return
      }
    } else {
      if (!singleStation) {
        message.warning('请选择电站')
        return
      }
      const validPeriods = periods.filter(p => p.range && p.range.length === 2)
      if (validPeriods.length < 2) {
        message.warning('请至少添加2个有效时间对比组')
        return
      }
    }
    if (!dateRange || dateRange.length !== 2) {
      message.warning('请选择对比时间范围')
      return
    }

    let params
    if (mode === 'multi') {
      params = {
        stationIds: selectedStations.map(id => Number(id)),
        startTime: dateRange[0].format('YYYY-MM-DD'),
        endTime: dateRange[1].format('YYYY-MM-DD'),
        statisticsType: STATISTICS_TYPE_MAP[statisticsType]
      }
    } else {
      const validPeriods = periods.filter(p => p.range && p.range.length === 2)
      params = {
        stationIds: [Number(singleStation)],
        startTime: dateRange[0].format('YYYY-MM-DD'),
        endTime: dateRange[1].format('YYYY-MM-DD'),
        statisticsType: STATISTICS_TYPE_MAP[statisticsType],
        periods: validPeriods.map(p => ({
          label: p.label,
          startTime: p.range[0].format('YYYY-MM-DD'),
          endTime: p.range[1].format('YYYY-MM-DD')
        }))
      }
    }

    setLoading(true)
    try {
      const res = await compareStations(params)
      setCompareData(res.data || null)
      if (!res.data) {
        message.info('暂无对比数据')
      }
    } catch (e) {
      console.error('对比分析失败', e)
    } finally {
      setLoading(false)
    }
  }

  const handleExport = async () => {
    if (!compareData) {
      message.warning('请先执行对比分析再导出报告')
      return
    }

    let params
    if (mode === 'multi') {
      params = {
        stationIds: selectedStations.map(id => Number(id)).join(','),
        startTime: dateRange[0].format('YYYY-MM-DD'),
        endTime: dateRange[1].format('YYYY-MM-DD'),
        statisticsType: STATISTICS_TYPE_MAP[statisticsType]
      }
    } else {
      const validPeriods = periods.filter(p => p.range && p.range.length === 2)
      params = {
        stationIds: Number(singleStation),
        startTime: dateRange[0].format('YYYY-MM-DD'),
        endTime: dateRange[1].format('YYYY-MM-DD'),
        statisticsType: STATISTICS_TYPE_MAP[statisticsType],
        periods: JSON.stringify(validPeriods.map(p => ({
          label: p.label,
          startTime: p.range[0].format('YYYY-MM-DD'),
          endTime: p.range[1].format('YYYY-MM-DD')
        })))
      }
    }

    setExporting(true)
    try {
      const res = await exportCompareReport(params)
      const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `电站对比报告_${dayjs().format('YYYYMMDDHHmmss')}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
      message.success('导出成功')
    } catch (e) {
      console.error('导出失败', e)
    } finally {
      setExporting(false)
    }
  }

  const addPeriod = () => {
    setPeriods(prev => [
      ...prev,
      { id: Date.now(), label: `对比组${prev.length + 1}`, range: null }
    ])
  }

  const removePeriod = (id) => {
    if (periods.length <= 2) {
      message.warning('至少保留2个对比组')
      return
    }
    setPeriods(prev => prev.filter(p => p.id !== id))
  }

  const updatePeriod = (id, field, value) => {
    setPeriods(prev => prev.map(p =>
      p.id === id ? { ...p, [field]: value } : p
    ))
  }

  const rowsWithStats = useMemo(() => {
    if (!compareData?.rows || compareData.rows.length === 0) return []
    const rows = compareData.rows
    const stats = {}

    METRICS_CONFIG.forEach(metric => {
      const values = rows.map(r => r[metric.key]).filter(v => v !== null && v !== undefined && !isNaN(v))
      if (values.length > 0) {
        const sum = values.reduce((a, b) => a + b, 0)
        stats[metric.key] = {
          avg: sum / values.length,
          max: Math.max(...values),
          min: Math.min(...values)
        }
      }
    })

    const rowsWithScore = rows.map(row => {
      let score = 0
      METRICS_CONFIG.forEach(metric => {
        if (metric.weight !== 0 && stats[metric.key] && row[metric.key] !== null && row[metric.key] !== undefined) {
          const { max, min } = stats[metric.key]
          const range = max - min
          if (range > 0) {
            const normalized = metric.positive
              ? (row[metric.key] - min) / range
              : (max - row[metric.key]) / range
            score += normalized * Math.abs(metric.weight)
          }
        }
      })
      return { ...row, _score: score * 100 }
    })

    return rowsWithScore
      .map((row, idx) => ({ ...row, _stats: stats, _rowIndex: idx }))
      .sort((a, b) => b._score - a._score)
  }, [compareData])

  const formatValue = (value, unit) => {
    if (value === null || value === undefined || isNaN(value)) return '-'
    return `${typeof value === 'number' ? value.toFixed(2) : value}${unit}`
  }

  const renderMetricCell = (row, metric) => {
    const value = row[metric.key]
    const stats = row._stats?.[metric.key]
    if (value === null || value === undefined || isNaN(value)) {
      return <span>-</span>
    }

    const isMax = stats && value === stats.max
    const isMin = stats && value === stats.min
    const diff = stats ? value - stats.avg : 0

    let style = {}
    if (isMax && metric.positive) {
      style = { color: '#52c41a', fontWeight: 'bold' }
    } else if (isMin && !metric.positive) {
      style = { color: '#52c41a', fontWeight: 'bold' }
    } else if (isMin && metric.positive) {
      style = { color: '#ff4d4f', fontWeight: 'bold' }
    } else if (isMax && !metric.positive) {
      style = { color: '#ff4d4f', fontWeight: 'bold' }
    }

    const diffText = diff > 0
      ? <span style={{ color: '#52c41a', marginLeft: 6 }}><ArrowUpOutlined /> {diff.toFixed(2)}</span>
      : diff < 0
        ? <span style={{ color: '#ff4d4f', marginLeft: 6 }}><ArrowDownOutlined /> {Math.abs(diff).toFixed(2)}</span>
        : null

    return (
      <Space>
        <span style={style}>{formatValue(value, metric.unit)}</span>
        {diffText}
      </Space>
    )
  }

  const tableColumns = useMemo(() => {
    const cols = [
      {
        title: '排名',
        key: 'rank',
        width: 70,
        fixed: 'left',
        render: (_, __, index) => (
          <Tag color={index === 0 ? 'gold' : index === 1 ? 'silver' : index === 2 ? 'bronze' : 'default'}>
            {index + 1}
          </Tag>
        )
      },
      {
        title: mode === 'multi' ? '电站名称' : '对比组',
        dataIndex: 'name',
        key: 'name',
        width: 180,
        fixed: 'left',
        render: (text, record) => {
          if (compareData?.bestStation === record.id) {
            return (
              <Space>
                <TrophyOutlined style={{ color: '#faad14' }} />
                <span style={{ color: '#faad14', fontWeight: 'bold' }}>{text}</span>
              </Space>
            )
          }
          if (compareData?.worstStation === record.id) {
            return (
              <Space>
                <WarningOutlined style={{ color: '#ff4d4f' }} />
                <span style={{ color: '#ff4d4f', fontWeight: 'bold' }}>{text}</span>
              </Space>
            )
          }
          return text
        }
      },
      ...METRICS_CONFIG.map(metric => ({
        title: `${metric.label}(${metric.unit})`,
        key: metric.key,
        width: 130,
        render: (_, row) => renderMetricCell(row, metric)
      })),
      {
        title: '综合评分',
        key: 'score',
        width: 110,
        fixed: 'right',
        render: (_, row) => (
          <span style={{ fontWeight: 'bold', color: '#1890ff' }}>
            {row._score?.toFixed(1)}分
          </span>
        )
      }
    ]
    return cols
  }, [mode, compareData])

  const getRadarOption = () => {
    if (!rowsWithStats.length) {
      return { tooltip: {}, radar: { indicator: [] }, series: [] }
    }
    const indicators = [
      { name: 'PR', max: 100 },
      { name: '等效小时', max: 10 },
      { name: '健康度', max: 100 },
      { name: '在线率', max: 100 },
      { name: '低故障率', max: 100 }
    ]

    const seriesData = rowsWithStats.map((row, idx) => ({
      name: row.name,
      value: [
        row.pr || 0,
        row.equivalentHours || 0,
        row.healthScore || 0,
        row.onlineRate || 0,
        100 - (row.faultRate || 0)
      ],
      itemStyle: { color: STATION_COLORS[idx % STATION_COLORS.length] },
      lineStyle: { width: 2 }
    }))

    return {
      tooltip: { trigger: 'item' },
      legend: { data: rowsWithStats.map(r => r.name), bottom: 0 },
      radar: {
        indicator: indicators,
        shape: 'polygon',
        splitNumber: 5,
        axisName: { color: '#666' },
        splitLine: { lineStyle: { color: 'rgba(0,0,0,0.1)' } },
        splitArea: { areaStyle: { color: ['rgba(24,144,255,0.02)', 'rgba(24,144,255,0.05)'] } }
      },
      series: [
        {
          type: 'radar',
          data: seriesData,
          areaStyle: { opacity: 0.1 }
        }
      ]
    }
  }

  const getBarOption = () => {
    if (!rowsWithStats.length) {
      return { tooltip: {}, legend: {}, xAxis: {}, yAxis: {}, series: [] }
    }
    const metric = METRICS_CONFIG.find(m => m.key === barChartMetric)
    const xData = rowsWithStats.map(r => r.name)
    const values = rowsWithStats.map(r => r[barChartMetric] || 0)

    return {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
      xAxis: {
        type: 'category',
        data: xData,
        axisLabel: { rotate: 30, interval: 0, fontSize: 11 }
      },
      yAxis: {
        type: 'value',
        name: `${metric.label}(${metric.unit})`
      },
      series: [
        {
          name: metric.label,
          type: 'bar',
          data: values.map((v, i) => ({
            value: v,
            itemStyle: { color: STATION_COLORS[i % STATION_COLORS.length] }
          })),
          barWidth: '50%',
          label: { show: true, position: 'top', formatter: `{c}${metric.unit}` }
        }
      ]
    }
  }

  const getTrendOption = () => {
    if (!compareData?.trends || compareData.trends.length === 0) {
      return { tooltip: {}, legend: {}, xAxis: {}, yAxis: {}, series: [] }
    }
    const trends = compareData.trends
    const xData = trends[0]?.dates || []
    const series = trends.map((t, idx) => ({
      name: t.name,
      type: 'line',
      smooth: true,
      data: t.values || [],
      itemStyle: { color: STATION_COLORS[idx % STATION_COLORS.length] },
      lineStyle: { width: 2 }
    }))

    return {
      tooltip: { trigger: 'axis' },
      legend: { data: trends.map(t => t.name), bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
      xAxis: { type: 'category', data: xData },
      yAxis: { type: 'value', name: 'PR(%)' },
      series
    }
  }

  const getAlertIcon = (type) => {
    switch (type) {
      case 'success': return <TrophyOutlined />
      case 'warning': return <WarningOutlined />
      case 'info': return <BulbOutlined />
      case 'error': return <ThunderboltOutlined />
      default: return <BulbOutlined />
    }
  }

  return (
    <div className="station-compare-page">
      <Card
        title="电站对比分析"
        className="filter-card"
      >
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Tabs
            activeKey={mode}
            onChange={setMode}
            items={[
              { key: 'multi', label: '多电站对比' },
              { key: 'single', label: '同电站不同时期' }
            ]}
          />

          {mode === 'multi' ? (
            <Row gutter={16}>
              <Col xs={24} sm={12} md={8}>
                <div className="form-item">
                  <label>选择电站</label>
                  <Select
                    mode="multiple"
                    placeholder="请选择至少2个电站"
                    value={selectedStations}
                    onChange={setSelectedStations}
                    style={{ width: '100%' }}
                    maxTagCount="responsive"
                    allowClear
                  >
                    {stationList.map(s => (
                      <Option key={s.id} value={s.id}>{s.stationName}</Option>
                    ))}
                  </Select>
                </div>
              </Col>
            </Row>
          ) : (
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Row gutter={16}>
                <Col xs={24} sm={12} md={8}>
                  <div className="form-item">
                    <label>选择电站</label>
                    <Select
                      placeholder="请选择电站"
                      value={singleStation}
                      onChange={setSingleStation}
                      style={{ width: '100%' }}
                      allowClear
                    >
                      {stationList.map(s => (
                        <Option key={s.id} value={s.id}>{s.stationName}</Option>
                      ))}
                    </Select>
                  </div>
                </Col>
              </Row>
              {periods.map((period, idx) => (
                <Row gutter={16} key={period.id} align="middle">
                  <Col xs={2} sm={1}>
                    <span style={{ fontWeight: 'bold' }}>#{idx + 1}</span>
                  </Col>
                  <Col xs={10} sm={8} md={5}>
                    <Input
                      placeholder="对比组标签"
                      value={period.label}
                      onChange={e => updatePeriod(period.id, 'label', e.target.value)}
                    />
                  </Col>
                  <Col xs={10} sm={12} md={8}>
                    <RangePicker
                      style={{ width: '100%' }}
                      value={period.range}
                      onChange={dates => updatePeriod(period.id, 'range', dates)}
                      format="YYYY-MM-DD"
                    />
                  </Col>
                  <Col xs={2} sm={1}>
                    <Tooltip title="删除对比组">
                      <Button
                        type="text"
                        danger
                        icon={<MinusCircleOutlined />}
                        onClick={() => removePeriod(period.id)}
                      />
                    </Tooltip>
                  </Col>
                </Row>
              ))}
              <Button
                type="dashed"
                icon={<PlusOutlined />}
                onClick={addPeriod}
                block
              >
                添加对比组
              </Button>
            </Space>
          )}

          <Row gutter={16}>
            <Col xs={24} sm={12} md={6}>
              <div className="form-item">
                <label>统计周期</label>
                <Select
                  value={statisticsType}
                  onChange={setStatisticsType}
                  style={{ width: '100%' }}
                >
                  <Option value="day">日</Option>
                  <Option value="week">周</Option>
                  <Option value="month">月</Option>
                  <Option value="year">年</Option>
                </Select>
              </div>
            </Col>
            <Col xs={24} sm={12} md={10}>
              <div className="form-item">
                <label>对比时间范围</label>
                <RangePicker
                  style={{ width: '100%' }}
                  value={dateRange}
                  onChange={setDateRange}
                  format="YYYY-MM-DD"
                />
              </div>
            </Col>
            <Col xs={24} md={8} style={{ display: 'flex', alignItems: 'flex-end' }}>
              <Space>
                <Button type="primary" onClick={handleCompare} loading={loading}>
                  开始对比
                </Button>
                <Button
                  icon={<DownloadOutlined />}
                  onClick={handleExport}
                  loading={exporting}
                  disabled={!compareData}
                >
                  导出报告
                </Button>
              </Space>
            </Col>
          </Row>
        </Space>
      </Card>

      <Spin spinning={loading} style={{ marginTop: 16 }}>
        {compareData && (
          <>
            <Card
              title="对比指标矩阵"
              style={{ marginTop: 16 }}
              className="matrix-card"
            >
              <Table
                columns={tableColumns}
                dataSource={rowsWithStats}
                rowKey="id"
                pagination={false}
                scroll={{ x: 1400 }}
                size="middle"
              />
            </Card>

            <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
              <Col xs={24} lg={12}>
                <Card title="综合能力雷达图">
                  <ReactECharts option={getRadarOption()} style={{ height: 380 }} />
                </Card>
              </Col>
              <Col xs={24} lg={12}>
                <Card
                  title="指标柱状对比"
                  extra={
                    <Select
                      value={barChartMetric}
                      onChange={setBarChartMetric}
                      style={{ width: 140 }}
                    >
                      {CHART_METRIC_OPTIONS.map(opt => (
                        <Option key={opt.key} value={opt.key}>{opt.label}</Option>
                      ))}
                    </Select>
                  }
                >
                  <ReactECharts option={getBarOption()} style={{ height: 380 }} />
                </Card>
              </Col>
            </Row>

            <Card title="PR趋势对比" style={{ marginTop: 16 }}>
              <ReactECharts option={getTrendOption()} style={{ height: 360 }} />
            </Card>

            {compareData.recommendations && compareData.recommendations.length > 0 && (
              <Card title="优化建议" style={{ marginTop: 16 }}>
                <Space direction="vertical" size={12} style={{ width: '100%' }}>
                  {compareData.recommendations.map((rec, idx) => (
                    <Alert
                      key={idx}
                      type={rec.type || 'info'}
                      showIcon
                      icon={getAlertIcon(rec.type)}
                      message={rec.title || '建议'}
                      description={rec.content}
                    />
                  ))}
                </Space>
              </Card>
            )}
          </>
        )}
      </Spin>
    </div>
  )
}

export default StationCompare
