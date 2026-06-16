import React, { useState, useEffect, useRef, useCallback } from 'react'
import {
  Card,
  Button,
  Table,
  Tag,
  Space,
  Modal,
  message,
  Row,
  Col,
  Statistic,
  Avatar,
  List,
  Progress,
  Tooltip,
  Badge,
  Divider,
  Empty,
  Spin
} from 'antd'
import {
  EnvironmentOutlined,
  UserOutlined,
  PhoneOutlined,
  ThunderboltOutlined,
  CheckCircleOutlined,
  RobotOutlined,
  ReloadOutlined,
  ArrowRightOutlined,
  SafetyOutlined
} from '@ant-design/icons'
import {
  getWorkOrderPage,
  getOperatorLocations,
  getRecommendOperators,
  assignDispatchOrder,
  autoAssignOrder
} from '../../api/workorder'
import { getUser } from '../../utils/auth'
import './DispatchMap.css'

const STATUS_MAP = {
  0: { color: 'orange', text: '待接单' },
  1: { color: 'blue', text: '已接单' },
  2: { color: 'processing', text: '处理中' },
  3: { color: 'purple', text: '待验收' },
  4: { color: 'green', text: '已完成' },
  5: { color: 'default', text: '已关闭' }
}

const FAULT_LEVEL_MAP = {
  1: { color: 'green', text: '低级' },
  2: { color: 'gold', text: '中级' },
  3: { color: 'orange', text: '高级' },
  4: { color: 'red', text: '紧急' }
}

const RECOMMEND_LEVEL_MAP = {
  '强烈推荐': { color: '#52c41a', bgColor: '#f6ffed' },
  '推荐': { color: '#1890ff', bgColor: '#e6f7ff' },
  '一般': { color: '#faad14', bgColor: '#fffbe6' },
  '可考虑': { color: '#8c8c8c', bgColor: '#fafafa' },
  '不推荐': { color: '#d9d9d9', bgColor: '#f5f5f5' }
}

const DispatchMap = () => {
  const [pendingOrders, setPendingOrders] = useState([])
  const [selectedOrder, setSelectedOrder] = useState(null)
  const [operators, setOperators] = useState([])
  const [recommendList, setRecommendList] = useState([])
  const [selectedOperator, setSelectedOperator] = useState(null)
  const [loading, setLoading] = useState(false)
  const [mapLoading, setMapLoading] = useState(false)
  const [recommendLoading, setRecommendLoading] = useState(false)
  const [autoAssignLoading, setAutoAssignLoading] = useState(false)
  const mapRef = useRef(null)
  const [mapCenter, setMapCenter] = useState({ lng: 116.397428, lat: 39.90923 })
  const [mapScale, setMapScale] = useState(1)

  const fetchPendingOrders = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getWorkOrderPage({ status: 0, pageNum: 1, pageSize: 50 })
      const pageResult = res.data || {}
      setPendingOrders(pageResult.list || [])
    } catch (e) {
      message.error('加载待派工单失败')
    } finally {
      setLoading(false)
    }
  }, [])

  const fetchOperators = useCallback(async () => {
    setMapLoading(true)
    try {
      const res = await getOperatorLocations()
      const data = res.data || []
      setOperators(data)
      if (data.length > 0) {
        const avgLng = data.reduce((sum, op) => sum + (Number(op.longitude) || 0), 0) / data.length
        const avgLat = data.reduce((sum, op) => sum + (Number(op.latitude) || 0), 0) / data.length
        if (avgLng && avgLat) {
          setMapCenter({ lng: avgLng, lat: avgLat })
        }
      }
    } catch (e) {
      console.error('加载人员位置失败', e)
    } finally {
      setMapLoading(false)
    }
  }, [])

  const fetchRecommendations = useCallback(async (order) => {
    if (!order) return
    setRecommendLoading(true)
    try {
      const res = await getRecommendOperators({
        orderId: order.id,
        stationId: order.stationId,
        stationLng: order.longitude,
        stationLat: order.latitude,
        requiredSkill: order.faultName,
        faultLevel: order.faultLevel
      })
      const data = res.data || []
      setRecommendList(data)
      if (data.length > 0) {
        setSelectedOperator(data[0])
      }
    } catch (e) {
      message.error('获取推荐人员失败')
      setRecommendList([])
    } finally {
      setRecommendLoading(false)
    }
  }, [])

  const handleSelectOrder = (order) => {
    setSelectedOrder(order)
    setSelectedOperator(null)
    fetchRecommendations(order)
  }

  const handleSelectOperator = (op) => {
    setSelectedOperator(op)
  }

  const handleAssign = async () => {
    if (!selectedOrder || !selectedOperator) {
      message.warning('请选择工单和运维人员')
      return
    }

    Modal.confirm({
      title: '确认派单',
      content: `确定将工单「${selectedOrder.orderNo}」派给「${selectedOperator.userName}」吗？`,
      okText: '确认派单',
      cancelText: '取消',
      onOk: async () => {
        try {
          const user = getUser() || {}
          await assignDispatchOrder({
            orderId: selectedOrder.id,
            handlerId: selectedOperator.userId,
            handlerName: selectedOperator.userName,
            operatorId: user.id,
            operatorName: user.name || user.username || '管理员'
          })
          message.success('派单成功')
          setSelectedOrder(null)
          setSelectedOperator(null)
          setRecommendList([])
          fetchPendingOrders()
          fetchOperators()
        } catch (e) {
          message.error(e.message || '派单失败')
        }
      }
    })
  }

  const handleAutoAssign = async () => {
    if (!selectedOrder) {
      message.warning('请先选择工单')
      return
    }

    Modal.confirm({
      title: '智能派单',
      content: `系统将自动选择最优运维人员派发工单「${selectedOrder.orderNo}」，是否继续？`,
      icon: <RobotOutlined style={{ color: '#1890ff' }} />,
      okText: '智能派单',
      cancelText: '取消',
      onOk: async () => {
        setAutoAssignLoading(true)
        try {
          const user = getUser() || {}
          const res = await autoAssignOrder({
            orderId: selectedOrder.id,
            stationId: selectedOrder.stationId,
            stationLng: selectedOrder.longitude,
            stationLat: selectedOrder.latitude,
            requiredSkill: selectedOrder.faultName,
            faultLevel: selectedOrder.faultLevel,
            operatorId: user.id,
            operatorName: user.name || user.username || '管理员'
          })
          message.success('智能派单成功')
          setSelectedOrder(null)
          setSelectedOperator(null)
          setRecommendList([])
          fetchPendingOrders()
          fetchOperators()
        } catch (e) {
          message.error(e.message || '派单失败')
        } finally {
          setAutoAssignLoading(false)
        }
      }
    })
  }

  const getMarkerPosition = (op) => {
    if (!op.longitude || !op.latitude) return null

    const lng = Number(op.longitude)
    const lat = Number(op.latitude)

    const rangeLng = 0.05
    const rangeLat = 0.05

    const x = ((lng - mapCenter.lng + rangeLng) / (rangeLng * 2)) * 100
    const y = ((mapCenter.lat - lat + rangeLat) / (rangeLat * 2)) * 100

    return { left: `${x}%`, top: `${y}%` }
  }

  const getOrderPosition = () => {
    if (!selectedOrder || !selectedOrder.longitude || !selectedOrder.latitude) return null

    const lng = Number(selectedOrder.longitude)
    const lat = Number(selectedOrder.latitude)

    const rangeLng = 0.05
    const rangeLat = 0.05

    const x = ((lng - mapCenter.lng + rangeLng) / (rangeLng * 2)) * 100
    const y = ((mapCenter.lat - lat + rangeLat) / (rangeLat * 2)) * 100

    return { left: `${x}%`, top: `${y}%` }
  }

  const refreshAll = () => {
    fetchPendingOrders()
    fetchOperators()
  }

  useEffect(() => {
    fetchPendingOrders()
    fetchOperators()

    const timer = setInterval(() => {
      fetchOperators()
    }, 30000)

    return () => clearInterval(timer)
  }, [fetchPendingOrders, fetchOperators])

  const orderColumns = [
    {
      title: '工单编号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 140,
      render: (text) => <a>{text}</a>
    },
    {
      title: '故障名称',
      dataIndex: 'faultName',
      key: 'faultName',
      ellipsis: true
    },
    {
      title: '故障等级',
      dataIndex: 'faultLevel',
      key: 'faultLevel',
      width: 90,
      render: (level) => {
        const info = FAULT_LEVEL_MAP[level] || { color: 'default', text: level }
        return <Tag color={info.color}>{info.text}</Tag>
      }
    },
    {
      title: '所属电站',
      dataIndex: 'stationName',
      key: 'stationName',
      width: 120
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 170
    }
  ]

  const getScoreColor = (score) => {
    if (score >= 85) return '#52c41a'
    if (score >= 70) return '#1890ff'
    if (score >= 55) return '#faad14'
    return '#8c8c8c'
  }

  return (
    <div className="dispatch-map-page">
      <div className="page-header">
        <div className="page-title">
          <h2>工单智能调度</h2>
          <Tag color="blue">实时位置</Tag>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={refreshAll}>
            刷新
          </Button>
          <Button
            type="primary"
            icon={<RobotOutlined />}
            onClick={handleAutoAssign}
            disabled={!selectedOrder}
            loading={autoAssignLoading}
          >
            智能派单
          </Button>
        </Space>
      </div>

      <Row gutter={16} className="stats-row">
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="待派工单"
              value={pendingOrders.length}
              valueStyle={{ color: '#faad14' }}
              prefix={<ThunderboltOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="在岗人员"
              value={operators.length}
              valueStyle={{ color: '#52c41a' }}
              prefix={<UserOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="当前选中"
              value={selectedOrder ? 1 : 0}
              valueStyle={{ color: '#1890ff' }}
              prefix={<SafetyOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="推荐人数"
              value={recommendList.length}
              valueStyle={{ color: '#722ed1' }}
              prefix={<EnvironmentOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16} className="main-content">
        <Col span={8} className="left-panel">
          <Card
            title="待派工单"
            size="small"
            className="orders-card"
            extra={
              <Button type="link" size="small" onClick={fetchPendingOrders}>
                刷新
              </Button>
            }
          >
            <Table
              size="small"
              columns={orderColumns}
              dataSource={pendingOrders}
              rowKey="id"
              loading={loading}
              pagination={false}
              scroll={{ y: 400 }}
              rowClassName={(record) =>
                selectedOrder?.id === record.id ? 'selected-row' : ''
              }
              onRow={(record) => ({
                onClick: () => handleSelectOrder(record)
              })}
            />
          </Card>
        </Col>

        <Col span={10} className="center-panel">
          <Card
            title="地图视图"
            size="small"
            className="map-card"
            extra={
              <Space size="small">
                <Button size="small" onClick={() => setMapScale(s => Math.min(s * 1.2, 3))}>+</Button>
                <Button size="small" onClick={() => setMapScale(s => Math.max(s / 1.2, 0.5))}>−</Button>
              </Space>
            }
          >
            <div className="map-container" ref={mapRef}>
              <Spin spinning={mapLoading} tip="加载中...">
                <div
                  className="map-view"
                  style={{ transform: `scale(${mapScale})` }}
                >
                  <div className="map-grid">
                    {[...Array(10)].map((_, i) => (
                      <div key={`h-${i}`} className="grid-line horizontal" style={{ top: `${i * 10}%` }} />
                    ))}
                    {[...Array(10)].map((_, i) => (
                      <div key={`v-${i}`} className="grid-line vertical" style={{ left: `${i * 10}%` }} />
                    ))}
                  </div>

                  <div className="map-center-marker">
                    <div className="center-dot" />
                    <div className="center-label">{mapCenter.lng.toFixed(4)}, {mapCenter.lat.toFixed(4)}</div>
                  </div>

                  {selectedOrder && getOrderPosition() && (
                    <div
                      className="order-marker"
                      style={getOrderPosition()}
                      title={selectedOrder.faultName}
                    >
                      <Badge dot color="#ff4d4f">
                        <div className="marker-pin order-pin">
                          <ThunderboltOutlined />
                        </div>
                      </Badge>
                      <div className="marker-label order-label">
                        {selectedOrder.orderNo}
                      </div>
                    </div>
                  )}

                  {operators.map((op) => {
                    const pos = getMarkerPosition(op)
                    if (!pos) return null
                    const isSelected = selectedOperator?.userId === op.userId
                    return (
                      <div
                        key={op.userId}
                        className={`operator-marker ${isSelected ? 'selected' : ''}`}
                        style={pos}
                        onClick={() => handleSelectOperator(op)}
                        title={op.userName}
                      >
                        <Avatar size="default" icon={<UserOutlined />} className="marker-avatar" />
                        <div className="marker-label">{op.userName}</div>
                      </div>
                    )
                  })}
                </div>
              </Spin>

              <div className="map-legend">
                <div className="legend-item">
                  <span className="legend-dot order-dot" />
                  <span>故障工单</span>
                </div>
                <div className="legend-item">
                  <span className="legend-dot person-dot" />
                  <span>运维人员</span>
                </div>
              </div>
            </div>
          </Card>
        </Col>

        <Col span={6} className="right-panel">
          <Card
            title="推荐人员"
            size="small"
            className="recommend-card"
            extra={
              <Button
                type="link"
                size="small"
                onClick={() => selectedOrder && fetchRecommendations(selectedOrder)}
                disabled={!selectedOrder}
              >
                重新推荐
              </Button>
            }
          >
            {!selectedOrder ? (
              <Empty description="请选择工单查看推荐" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : recommendLoading ? (
              <div className="loading-container">
                <Spin tip="正在计算推荐人员..." />
              </div>
            ) : recommendList.length === 0 ? (
              <Empty description="附近没有可用人员" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
              <List
                size="small"
                dataSource={recommendList}
                renderItem={(item, index) => {
                  const levelInfo = RECOMMEND_LEVEL_MAP[item.recommendLevel] || {}
                  const isSelected = selectedOperator?.userId === item.userId
                  return (
                    <List.Item
                      className={`recommend-item ${isSelected ? 'selected' : ''}`}
                      onClick={() => handleSelectOperator(item)}
                    >
                      <div className="item-rank">{index + 1}</div>
                      <Avatar icon={<UserOutlined />} className="item-avatar" />
                      <div className="item-info">
                        <div className="item-header">
                          <span className="item-name">{item.userName}</span>
                          <Tag
                            color={levelInfo.color}
                            style={{ backgroundColor: levelInfo.bgColor }}
                          >
                            {item.recommendLevel}
                          </Tag>
                        </div>
                        <div className="item-tags">
                          {item.skillTags?.slice(0, 3).map((tag, i) => (
                            <Tag key={i} size="small" color="blue">
                              {tag}
                            </Tag>
                          ))}
                        </div>
                        <div className="item-meta">
                          <span>📍 {item.distanceKm}km</span>
                          <span>⏱️ {item.etaMinutes}分钟</span>
                          <span>📋 {item.activeTaskCount}个任务</span>
                        </div>
                      </div>
                      <div className="item-score">
                        <Progress
                          type="circle"
                          size={50}
                          percent={item.totalScore}
                          strokeColor={getScoreColor(item.totalScore)}
                          format={(p) => `${p}分`}
                        />
                      </div>
                    </List.Item>
                  )
                }}
              />
            )}

            {selectedOrder && (
              <>
                <Divider />
                <Space direction="vertical" style={{ width: '100%' }}>
                  <Button
                    type="primary"
                    block
                    icon={<CheckCircleOutlined />}
                    onClick={handleAssign}
                    disabled={!selectedOperator}
                  >
                    确认派单给 {selectedOperator?.userName || '...'}
                  </Button>
                  <Button
                    block
                    icon={<RobotOutlined />}
                    onClick={handleAutoAssign}
                    loading={autoAssignLoading}
                  >
                    智能自动派单
                  </Button>
                </Space>
              </>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default DispatchMap
