import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react'
import {
  Row,
  Col,
  Card,
  Select,
  DatePicker,
  Checkbox,
  Button,
  Space,
  Statistic,
  Tag,
  Progress,
  Empty,
  Spin,
  message,
  Tooltip,
  Timeline,
  Divider
} from 'antd'
import {
  ThunderboltOutlined,
  ThermometerOutlined,
  ThunderboltFilled,
  BulbOutlined,
  WarningOutlined,
  SearchOutlined,
  ReloadOutlined,
  ClockCircleOutlined,
  FireOutlined,
  CheckCircleOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { getDevicePlaybackData, getWorkOrderPlaybackData, getInverterOptions } from '../../api/device'

const { RangePicker } = DatePicker
const { Option } = Select
const { Group: CheckboxGroup } = Checkbox

const GROUP_ID = 'playback-charts'

const METRIC_OPTIONS = [
  { label: '功率', value: 'power', color: '#1890ff', unit: 'kW', icon: <ThunderboltOutlined /> },
  { label: '温度', value: 'temperature', color: '#fa8c16', unit: '℃', icon: <ThermometerOutlined /> },
  { label: '电压', value: 'voltage', color: '#52c41a', unit: 'V', icon: <ThunderboltFilled /> },
  { label: '电流', value: 'current', color: '#722ed1', unit: 'A', icon: <BulbOutlined /> }
]

const AGGREGATION_OPTIONS = [
  { label: '均值', value: 'avg' },
  { label: '最大值', value: 'max' },
  { label: '最小值', value: 'min' }
]

const QUICK_RANGES = [
  { label: '最近1小时', hours: 1 },
  { label: '最近6小时', hours: 6 },
  { label: '最近24小时', hours: 24 },
  { label: '最近7天', hours: 24 * 7 }
]

const METRIC_ICON_MAP = {
  power: <ThunderboltOutlined />,
  temperature: <ThermometerOutlined />,
  voltage: <ThunderboltFilled />,
  current: <BulbOutlined />
}

const DataPlayback = ({
  readOnly = false,
  defaultInverterId = null,
  defaultTimeRange = null,
  workOrderId = null,
  onFaultTimelineClick = null
}) => {
  const [inverterOptions, setInverterOptions] = useState([])
  const [selectedInverter, setSelectedInverter] = useState(defaultInverterId)
  const [timeRange, setTimeRange] = useState(defaultTimeRange || [
    dayjs().subtract(1, 'hour'),
    dayjs()
  ])
  const [selectedMetrics, setSelectedMetrics] = useState(['power', 'temperature', 'voltage', 'current'])
  const [aggregation, setAggregation] = useState('avg')
  const [loading, setLoading] = useState(false)
  const [playbackData, setPlaybackData] = useState(null)
  const [dataZoomRange, setDataZoomRange] = useState([0, 100])
  const [inverterLoading, setInverterLoading] = useState(false)

  const powerChartRef = useRef(null)
  const tempChartRef = useRef(null)
  const voltageChartRef = useRef(null)
  const currentChartRef = useRef(null)
  const dataZoomChartRef = useRef(null)
  const connectedRef = useRef(false)

  const fetchInverterOptions = useCallback(async () => {
    setInverterLoading(true)
    try {
      const res = await getInverterOptions({ pageNum: 1, pageSize: 1000 })
      if (res.code === 200) {
        const list = res.data?.list || res.data || []
        setInverterOptions(Array.isArray(list) ? list : [])
      }
    } catch (e) {
      console.warn('获取逆变器列表失败', e)
    } finally {
      setInverterLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchInverterOptions()
  }, [fetchInverterOptions])

  useEffect(() => {
    if (defaultInverterId) {
      setSelectedInverter(defaultInverterId)
    }
  }, [defaultInverterId])

  useEffect(() => {
    if (defaultTimeRange) {
      setTimeRange(defaultTimeRange)
    }
  }, [defaultTimeRange])

  useEffect(() => {
    if (!connectedRef.current) {
      echarts.connect(GROUP_ID)
      connectedRef.current = true
    }
    return () => {
      echarts.disconnect(GROUP_ID)
      connectedRef.current = false
    }
  }, [])

  const fetchData = useCallback(async () => {
    if (!selectedInverter && !workOrderId) {
      message.warning('请选择逆变器')
      return
    }
    if (!timeRange || timeRange.length !== 2) {
      message.warning('请选择时间范围')
      return
    }
    setLoading(true)
    try {
      let res
      if (workOrderId) {
        res = await getWorkOrderPlaybackData(workOrderId)
      } else {
        const params = {
          startTime: timeRange[0].valueOf(),
          endTime: timeRange[1].valueOf(),
          metrics: selectedMetrics,
          aggregation
        }
        res = await getDevicePlaybackData(selectedInverter, params)
      }
      if (res.code === 200) {
        setPlaybackData(res.data || {})
        setDataZoomRange([0, 100])
      } else {
        message.error(res.message || '获取数据失败')
      }
    } catch (e) {
      message.error(e.message || '获取数据失败')
    } finally {
      setLoading(false)
    }
  }, [selectedInverter, timeRange, selectedMetrics, aggregation, workOrderId])

  useEffect(() => {
    if ((selectedInverter || workOrderId) && timeRange) {
      fetchData()
    }
  }, [selectedInverter, workOrderId])

  const handleQuickRange = (hours) => {
    const end = dayjs()
    const start = dayjs().subtract(hours, 'hour')
    setTimeRange([start, end])
  }

  const handleMetricChange = (values) => {
    if (values.length === 0) {
      message.warning('至少选择一个指标')
      return
    }
    setSelectedMetrics(values)
  }

  const handleFaultPointClick = (fault) => {
    if (!playbackData?.timeSeries || playbackData.timeSeries.length === 0) return
    const faultTime = dayjs(fault.time).valueOf()
    const times = playbackData.timeSeries
    let nearestIdx = 0
    let minDiff = Infinity
    for (let i = 0; i < times.length; i++) {
      const t = typeof times[i] === 'number' ? times[i] : dayjs(times[i]).valueOf()
      const diff = Math.abs(t - faultTime)
      if (diff < minDiff) {
        minDiff = diff
        nearestIdx = i
      }
    }
    const total = times.length
    const startPct = Math.max(0, (nearestIdx / total) * 100 - 10)
    const endPct = Math.min(100, (nearestIdx / total) * 100 + 10)
    setDataZoomRange([startPct, endPct])
    const refs = [powerChartRef, tempChartRef, voltageChartRef, currentChartRef, dataZoomChartRef]
    refs.forEach(ref => {
      if (ref.current) {
        const instance = ref.current.getEchartsInstance()
        instance.dispatchAction({
          type: 'dataZoom',
          start: startPct,
          end: endPct
        })
      }
    })
    if (onFaultTimelineClick) {
      onFaultTimelineClick(fault)
    }
  }

  const formatTime = (t) => {
    if (typeof t === 'number') {
      return dayjs(t).format('MM-DD HH:mm:ss')
    }
    return dayjs(t).format('MM-DD HH:mm:ss')
  }

  const timeAxisData = useMemo(() => {
    return (playbackData?.timeSeries || []).map(formatTime)
  }, [playbackData])

  const faultMarkPoints = useMemo(() => {
    const faults = playbackData?.faultPoints || []
    if (!timeAxisData || timeAxisData.length === 0) return []
    return faults.map(fault => {
      const faultTime = typeof fault.time === 'number' ? fault.time : dayjs(fault.time).valueOf()
      return {
        name: fault.faultCode || '故障',
        coord: [formatTime(faultTime), null],
        value: fault.description || fault.faultCode || '故障',
        itemStyle: { color: '#ff4d4f' },
        symbol: 'pin',
        symbolSize: 40,
        label: {
          show: true,
          formatter: '!',
          color: '#fff',
          fontWeight: 'bold'
        },
        tooltip: {
          formatter: () => {
            const lines = [
              `<b>故障时间：</b>${formatTime(faultTime)}`,
              `<b>故障码：</b>${fault.faultCode || '-'}`,
              `<b>描述：</b>${fault.description || '-'}`
            ]
            return lines.join('<br/>')
          }
        }
      }
    })
  }, [playbackData, timeAxisData])

  const createChartOption = (metricKey, color, unit, yAxisName) => {
    const metric = METRIC_OPTIONS.find(m => m.value === metricKey)
    const seriesData = playbackData?.[metricKey] || []
    const faultColor = '#ff4d4f'

    return {
      group: GROUP_ID,
      color: [color],
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        formatter: (params) => {
          const p = params?.[0]
          if (!p) return ''
          const val = p.value != null && !isNaN(p.value) ? Number(p.value).toFixed(2) : '-'
          return `${p.axisValue}<br/>${metric?.label || metricKey}：${val} ${unit}`
        }
      },
      grid: { left: 55, right: 20, top: 10, bottom: 20 },
      xAxis: {
        type: 'category',
        data: timeAxisData,
        axisLabel: { show: false },
        axisLine: { lineStyle: { color: '#e8e8e8' } },
        axisTick: { show: false }
      },
      yAxis: {
        type: 'value',
        name: yAxisName,
        nameTextStyle: { color: '#8c8c8c', fontSize: 11 },
        axisLabel: { color: '#8c8c8c', fontSize: 11 },
        splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
      },
      dataZoom: [
        {
          type: 'inside',
          start: dataZoomRange[0],
          end: dataZoomRange[1],
          zoomLock: false
        }
      ],
      series: [
        {
          name: metric?.label || metricKey,
          type: 'line',
          smooth: true,
          showSymbol: false,
          data: seriesData.map(v => (v != null && !isNaN(v) ? Number(v) : null)),
          lineStyle: { width: 2 },
          itemStyle: { color },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: `${color}40` },
              { offset: 1, color: `${color}05` }
            ])
          },
          markPoint: {
            symbol: 'pin',
            symbolSize: 32,
            itemStyle: { color: faultColor },
            label: {
              show: true,
              formatter: '!',
              color: '#fff',
              fontWeight: 'bold',
              fontSize: 12
            },
            data: faultMarkPoints.map(mp => ({
              ...mp,
              yAxis: 0
            }))
          }
        }
      ]
    }
  }

  const dataZoomOption = useMemo(() => {
    const powerData = playbackData?.power || []
    return {
      group: GROUP_ID,
      color: ['#1890ff'],
      grid: { left: 55, right: 20, top: 5, bottom: 28 },
      xAxis: {
        type: 'category',
        data: timeAxisData,
        boundaryGap: false,
        axisLabel: { color: '#8c8c8c', fontSize: 10 },
        axisLine: { lineStyle: { color: '#e8e8e8' } }
      },
      yAxis: {
        type: 'value',
        show: false
      },
      dataZoom: [
        {
          type: 'slider',
          show: true,
          start: dataZoomRange[0],
          end: dataZoomRange[1],
          bottom: 5,
          height: 30,
          borderColor: '#e8e8e8',
          fillerColor: 'rgba(24, 144, 255, 0.2)',
          handleStyle: { color: '#1890ff' },
          moveHandleStyle: { color: '#1890ff' },
          selectedDataBackground: {
            lineStyle: { color: '#1890ff' },
            areaStyle: { color: 'rgba(24, 144, 255, 0.4)' }
          },
          textStyle: { color: '#8c8c8c' }
        },
        {
          type: 'inside',
          start: dataZoomRange[0],
          end: dataZoomRange[1]
        }
      ],
      series: [
        {
          type: 'line',
          smooth: true,
          showSymbol: false,
          data: powerData.map(v => (v != null && !isNaN(v) ? Number(v) : null)),
          lineStyle: { width: 1 },
          itemStyle: { color: '#1890ff' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
              { offset: 1, color: 'rgba(24, 144, 255, 0.05)' }
            ])
          }
        }
      ]
    }
  }, [playbackData, timeAxisData, dataZoomRange])

  const summaryStats = useMemo(() => {
    const s = playbackData?.statistics || {}
    return {
      avgPower: s.avgPower,
      peakPower: s.peakPower,
      avgTemp: s.avgTemp,
      peakTemp: s.peakTemp,
      faultCount: (playbackData?.faultPoints || []).length,
      dataPointCount: playbackData?.timeSeries?.length || 0
    }
  }, [playbackData])

  const rootCauses = useMemo(() => {
    return playbackData?.rootCauses || []
  }, [playbackData])

  const faultPoints = useMemo(() => {
    return playbackData?.faultPoints || []
  }, [playbackData])

  return (
    <div className="data-playback-page">
      <Card
        title={readOnly ? '故障数据回放' : '历史数据回放'}
        className="playback-filter-card"
        extra={
          !readOnly ? (
            <Button
              type="primary"
              icon={<SearchOutlined />}
              onClick={fetchData}
              loading={loading}
            >
              查询
            </Button>
          ) : null
        }
      >
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Row gutter={16}>
            <Col xs={24} md={6}>
              <div style={{ marginBottom: 4, color: '#8c8c8c', fontSize: 12 }}>选择逆变器</div>
              <Select
                placeholder="请选择逆变器"
                style={{ width: '100%' }}
                showSearch
                loading={inverterLoading}
                disabled={readOnly}
                value={selectedInverter || undefined}
                onChange={setSelectedInverter}
                optionFilterProp="children"
                filterOption={(input, option) => {
                  const label = option.label || ''
                  return String(label).toLowerCase().includes(input.toLowerCase())
                }}
              >
                {inverterOptions.map(inv => (
                  <Option
                    key={inv.id}
                    value={inv.id}
                    label={inv.deviceName || inv.deviceSn || `逆变器#${inv.id}`}
                  >
                    {inv.deviceName || inv.deviceSn || `逆变器#${inv.id}`}
                  </Option>
                ))}
              </Select>
            </Col>
            <Col xs={24} md={10}>
              <div style={{ marginBottom: 4, color: '#8c8c8c', fontSize: 12 }}>时间范围</div>
              <RangePicker
                showTime={{ format: 'HH:mm:ss' }}
                format="YYYY-MM-DD HH:mm:ss"
                style={{ width: '100%' }}
                disabled={readOnly}
                value={timeRange}
                onChange={setTimeRange}
              />
            </Col>
            {!readOnly && (
              <Col xs={24} md={8}>
                <div style={{ marginBottom: 4, color: '#8c8c8c', fontSize: 12 }}>快捷选择</div>
                <Space wrap>
                  {QUICK_RANGES.map(r => (
                    <Button
                      key={r.label}
                      size="small"
                      onClick={() => handleQuickRange(r.hours)}
                      disabled={readOnly}
                    >
                      {r.label}
                    </Button>
                  ))}
                </Space>
              </Col>
            )}
          </Row>
          {!readOnly && (
            <Row gutter={16}>
              <Col xs={24} md={14}>
                <div style={{ marginBottom: 4, color: '#8c8c8c', fontSize: 12 }}>指标选择（默认全选）</div>
                <CheckboxGroup
                  options={METRIC_OPTIONS.map(m => ({ label: m.label, value: m.value }))}
                  value={selectedMetrics}
                  onChange={handleMetricChange}
                />
              </Col>
              <Col xs={24} md={6}>
                <div style={{ marginBottom: 4, color: '#8c8c8c', fontSize: 12 }}>聚合方式</div>
                <Select
                  style={{ width: '100%' }}
                  value={aggregation}
                  onChange={setAggregation}
                  disabled={readOnly}
                >
                  {AGGREGATION_OPTIONS.map(a => (
                    <Option key={a.value} value={a.value}>{a.label}</Option>
                  ))}
                </Select>
              </Col>
              <Col xs={24} md={4}>
                <div style={{ marginBottom: 4, color: '#8c8c8c', fontSize: 12 }}>操作</div>
                <Button
                  icon={<ReloadOutlined />}
                  onClick={fetchData}
                  loading={loading}
                  block
                >
                  刷新
                </Button>
              </Col>
            </Row>
          )}
        </Space>
      </Card>

      <Row gutter={16} style={{ marginTop: 16 }}>
        <Col xs={24} sm={12} md={4}>
          <Card size="small">
            <Statistic
              title="平均功率"
              value={summaryStats.avgPower}
              precision={2}
              suffix="kW"
              valueStyle={{ color: '#1890ff' }}
              prefix={<ThunderboltOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={4}>
          <Card size="small">
            <Statistic
              title="峰值功率"
              value={summaryStats.peakPower}
              precision={2}
              suffix="kW"
              valueStyle={{ color: '#1890ff', fontWeight: 600 }}
              prefix={<FireOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={4}>
          <Card size="small">
            <Statistic
              title="平均温度"
              value={summaryStats.avgTemp}
              precision={1}
              suffix="℃"
              valueStyle={{ color: '#fa8c16' }}
              prefix={<ThermometerOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={4}>
          <Card size="small">
            <Statistic
              title="峰值温度"
              value={summaryStats.peakTemp}
              precision={1}
              suffix="℃"
              valueStyle={{ color: '#fa8c16', fontWeight: 600 }}
              prefix={<FireOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={4}>
          <Card size="small">
            <Statistic
              title="故障次数"
              value={summaryStats.faultCount}
              valueStyle={{ color: '#ff4d4f' }}
              prefix={<WarningOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={4}>
          <Card size="small">
            <Statistic
              title="数据点数"
              value={summaryStats.dataPointCount}
              valueStyle={{ color: '#52c41a' }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginTop: 16 }}>
        <Col xs={24} xl={18}>
          <Spin spinning={loading}>
            {!loading && !playbackData ? (
              <Card style={{ textAlign: 'center', padding: '60px 20px' }}>
                <Empty description="请选择逆变器和时间范围后点击查询" />
              </Card>
            ) : (
              <>
                {selectedMetrics.includes('power') && (
                  <Card
                    size="small"
                    style={{ marginBottom: 12 }}
                    title={
                      <Space>
                        <ThunderboltOutlined style={{ color: '#1890ff' }} />
                        <span>功率曲线 (kW)</span>
                      </Space>
                    }
                  >
                    <ReactECharts
                      ref={powerChartRef}
                      option={createChartOption('power', '#1890ff', 'kW', 'kW')}
                      style={{ height: 180 }}
                      notMerge
                    />
                  </Card>
                )}
                {selectedMetrics.includes('temperature') && (
                  <Card
                    size="small"
                    style={{ marginBottom: 12 }}
                    title={
                      <Space>
                        <ThermometerOutlined style={{ color: '#fa8c16' }} />
                        <span>温度曲线 (℃)</span>
                      </Space>
                    }
                  >
                    <ReactECharts
                      ref={tempChartRef}
                      option={createChartOption('temperature', '#fa8c16', '℃', '℃')}
                      style={{ height: 180 }}
                      notMerge
                    />
                  </Card>
                )}
                {selectedMetrics.includes('voltage') && (
                  <Card
                    size="small"
                    style={{ marginBottom: 12 }}
                    title={
                      <Space>
                        <ThunderboltFilled style={{ color: '#52c41a' }} />
                        <span>电压曲线 (V)</span>
                      </Space>
                    }
                  >
                    <ReactECharts
                      ref={voltageChartRef}
                      option={createChartOption('voltage', '#52c41a', 'V', 'V')}
                      style={{ height: 180 }}
                      notMerge
                    />
                  </Card>
                )}
                {selectedMetrics.includes('current') && (
                  <Card
                    size="small"
                    style={{ marginBottom: 12 }}
                    title={
                      <Space>
                        <BulbOutlined style={{ color: '#722ed1' }} />
                        <span>电流曲线 (A)</span>
                      </Space>
                    }
                  >
                    <ReactECharts
                      ref={currentChartRef}
                      option={createChartOption('current', '#722ed1', 'A', 'A')}
                      style={{ height: 180 }}
                      notMerge
                    />
                  </Card>
                )}

                <Card size="small" title="时间轴滑块（拖动缩放查看细节）">
                  <ReactECharts
                    ref={dataZoomChartRef}
                    option={dataZoomOption}
                    style={{ height: 110 }}
                    notMerge
                  />
                </Card>
              </>
            )}
          </Spin>
        </Col>

        <Col xs={24} xl={6}>
          <Card
            title={
              <Space>
                <WarningOutlined style={{ color: '#ff4d4f' }} />
                <span>根因分析</span>
              </Space>
            }
            className="root-cause-panel"
            size="small"
            style={{ marginBottom: 16 }}
          >
            {rootCauses.length === 0 ? (
              <Empty description="暂无根因分析数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
              <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                {rootCauses.map((cause, idx) => {
                  const confidence = Math.round((cause.confidence || 0) * 100)
                  const confColor = confidence >= 70 ? '#52c41a' : confidence >= 40 ? '#faad14' : '#ff4d4f'
                  const metricInfo = METRIC_OPTIONS.find(m => m.value === cause.metric)
                  return (
                    <div
                      key={cause.id || idx}
                      className="root-cause-card"
                      style={{
                        padding: 12,
                        border: '1px solid #f0f0f0',
                        borderRadius: 8,
                        background: '#fafafa'
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', marginBottom: 8 }}>
                        <span
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            width: 28,
                            height: 28,
                            borderRadius: '50%',
                            background: `${metricInfo?.color || '#1890ff'}20`,
                            color: metricInfo?.color || '#1890ff',
                            marginRight: 8
                          }}
                        >
                          {METRIC_ICON_MAP[cause.metric] || <WarningOutlined />}
                        </span>
                        <div style={{ flex: 1 }}>
                          <div style={{ fontSize: 13, fontWeight: 500, marginBottom: 2 }}>
                            {cause.anomalyType || '异常指标'}
                          </div>
                          <Tag color={confColor} style={{ margin: 0, fontSize: 11 }}>
                            {metricInfo?.label || cause.metric || '指标'}
                          </Tag>
                        </div>
                      </div>
                      <div style={{ fontSize: 12, color: '#666', marginBottom: 8, lineHeight: 1.6 }}>
                        {cause.description || '暂无描述'}
                      </div>
                      <Progress
                        percent={confidence}
                        size="small"
                        strokeColor={confColor}
                        showInfo
                        format={p => `${p}% 置信度`}
                      />
                      {cause.suggestion && (
                        <div
                          style={{
                            marginTop: 10,
                            padding: 8,
                            background: '#e6f7ff',
                            borderRadius: 4,
                            fontSize: 12,
                            color: '#096dd9',
                            lineHeight: 1.6
                          }}
                        >
                          <span style={{ fontWeight: 500 }}>💡 建议：</span>
                          {cause.suggestion}
                        </div>
                      )}
                    </div>
                  )
                })}
              </Space>
            )}
          </Card>

          <Card
            title={
              <Space>
                <ClockCircleOutlined style={{ color: '#722ed1' }} />
                <span>故障时间轴</span>
                <Tag color="red" style={{ marginLeft: 8 }}>{faultPoints.length}</Tag>
              </Space>
            }
            size="small"
          >
            {faultPoints.length === 0 ? (
              <Empty description="暂无故障记录" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
              <Timeline
                mode="left"
                style={{ fontSize: 12 }}
                items={faultPoints.map((fault, idx) => ({
                  color: '#ff4d4f',
                  dot: <WarningOutlined />,
                  label: formatTime(fault.time),
                  children: (
                    <Tooltip title={fault.description}>
                      <div
                        style={{
                          padding: 8,
                          border: '1px solid #ffccc7',
                          borderRadius: 6,
                          background: '#fff2f0',
                          cursor: 'pointer'
                        }}
                        onClick={() => handleFaultPointClick(fault)}
                      >
                        <div style={{ fontWeight: 500, color: '#cf1322', fontSize: 13 }}>
                          {fault.faultCode || `故障#${idx + 1}`}
                        </div>
                        <div style={{ fontSize: 12, color: '#666', marginTop: 4, lineHeight: 1.5 }}>
                          {fault.description || '点击查看详情'}
                        </div>
                        <div style={{ marginTop: 6, color: '#1890ff', fontSize: 11 }}>
                          点击跳转图表位置 →
                        </div>
                      </div>
                    </Tooltip>
                  )
                }))}
              />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default DataPlayback
