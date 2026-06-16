import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Row,
  Col,
  Card,
  Button,
  Space,
  Tag,
  Descriptions,
  Spin,
  message,
  Empty,
  Tooltip,
  Progress,
  Divider,
  List,
  Rate,
  Popconfirm
} from 'antd'
import {
  ArrowLeftOutlined,
  PrinterOutlined,
  FileTextOutlined,
  WarningOutlined,
  EnvironmentOutlined,
  UserOutlined,
  ThunderboltOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  BulbOutlined,
  SafetyCertificateOutlined
} from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import dayjs from 'dayjs'
import { getWorkOrderDetail } from '../../api/workorder'
import { recommendKnowledge, submitKnowledgeFeedback, getUserFeedback, recordKnowledgeUsage } from '../../api/knowledge'
import { getUser } from '../../utils/auth'
import DataPlayback from './DataPlayback'

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
  2: { color: 'orange', text: '中级' },
  3: { color: 'red', text: '高级' },
  4: { color: '#cf1322', text: '紧急' }
}

const renderConfidenceBadge = (item) => {
  const info = {
    high: { color: '#52c41a', bg: '#f6ffed', text: '高置信度' },
    medium: { color: '#faad14', bg: '#fffbe6', text: '中置信度' },
    low: { color: '#ff7a45', bg: '#fff2e8', text: '低置信度' }
  }[item.confidenceLevel] || { color: '#999', bg: '#f5f5f5', text: '匹配中' }
  const percent = Math.round(Number(item.confidence || 0) * 100)
  return (
    <span style={{
      display: 'inline-flex',
      alignItems: 'center',
      gap: 6,
      background: info.bg,
      color: info.color,
      border: `1px solid ${info.color}40`,
      borderRadius: 10,
      padding: '2px 8px',
      fontSize: 12,
      fontWeight: 500
    }}>
      {info.text}
      <Progress
        type="circle"
        size={20}
        percent={percent}
        strokeColor={info.color}
        showInfo={false}
      />
      <span>{percent}%</span>
    </span>
  )
}

const FaultReview = () => {
  const navigate = useNavigate()
  const { workOrderId } = useParams()

  const [orderDetail, setOrderDetail] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [recommendLoading, setRecommendLoading] = useState(false)
  const [recommendList, setRecommendList] = useState([])
  const [userFeedbacks, setUserFeedbacks] = useState({})

  const playbackProps = useMemo(() => {
    if (!orderDetail) return null
    let startTime, endTime
    if (orderDetail.createTime) {
      const createTime = dayjs(orderDetail.createTime)
      startTime = createTime.subtract(2, 'hour')
      endTime = createTime.add(2, 'hour')
    } else {
      endTime = dayjs()
      startTime = dayjs().subtract(4, 'hour')
    }
    return {
      readOnly: true,
      defaultInverterId: orderDetail.inverterId,
      defaultTimeRange: [startTime, endTime],
      workOrderId: workOrderId
    }
  }, [orderDetail, workOrderId])

  const fetchOrderDetail = useCallback(async () => {
    if (!workOrderId) return
    setDetailLoading(true)
    try {
      const res = await getWorkOrderDetail(workOrderId)
      if (res.code === 200) {
        setOrderDetail(res.data || {})
      } else {
        message.error(res.message || '获取工单详情失败')
      }
    } catch (e) {
      message.error(e.message || '获取工单详情失败')
    } finally {
      setDetailLoading(false)
    }
  }, [workOrderId])

  const fetchRecommendations = useCallback(async () => {
    if (!orderDetail || (!orderDetail.faultCode && !orderDetail.faultName && !orderDetail.description)) {
      setRecommendList([])
      return
    }
    setRecommendLoading(true)
    try {
      const res = await recommendKnowledge({
        faultCode: orderDetail.faultCode || '',
        faultName: orderDetail.faultName || '',
        description: orderDetail.description || '',
        faultLevel: orderDetail.faultLevel || null,
        stationId: orderDetail.stationId || null,
        inverterId: orderDetail.inverterId || null,
        topN: 5,
        minConfidence: 0.2
      })
      const list = res.data || []
      setRecommendList(list)

      const currentUser = getUser() || {}
      if (currentUser.id && list.length > 0) {
        const fbMap = {}
        for (const item of list) {
          try {
            const fbRes = await getUserFeedback(item.id, currentUser.id)
            if (fbRes.data) {
              fbMap[item.id] = fbRes.data
            }
          } catch (e) {
            // ignore
          }
        }
        setUserFeedbacks(fbMap)
      }
    } catch (e) {
      console.warn('获取推荐方案失败', e)
      setRecommendList([])
    } finally {
      setRecommendLoading(false)
    }
  }, [orderDetail])

  useEffect(() => {
    fetchOrderDetail()
  }, [fetchOrderDetail])

  useEffect(() => {
    if (orderDetail) {
      fetchRecommendations()
    }
  }, [orderDetail?.id])

  const handleBack = () => {
    navigate('/workorder/list')
  }

  const handlePrint = () => {
    message.info('正在生成复盘报告，请稍候...')
    setTimeout(() => {
      window.print()
    }, 300)
  }

  const handleQuickFeedback = async (item, feedbackType) => {
    const currentUser = getUser() || {}
    if (!currentUser.id) {
      message.warning('请先登录')
      return
    }
    try {
      await submitKnowledgeFeedback({
        knowledgeId: item.id,
        userId: currentUser.id,
        userName: currentUser.name || currentUser.username,
        feedbackType
      })
      message.success('反馈已提交')
      setRecommendList(prev => prev.map(r => {
        if (r.id === item.id) {
          const newR = { ...r }
          const oldFb = userFeedbacks[item.id]
          if (oldFb && oldFb.feedbackType !== feedbackType) {
            if (oldFb.feedbackType === 1) newR.likeCount = Math.max(0, (newR.likeCount || 0) - 1)
            else if (oldFb.feedbackType === 2) newR.dislikeCount = Math.max(0, (newR.dislikeCount || 0) - 1)
          }
          if (feedbackType === 1 && (!oldFb || oldFb.feedbackType !== 1)) {
            newR.likeCount = (newR.likeCount || 0) + 1
          }
          if (feedbackType === 2 && (!oldFb || oldFb.feedbackType !== 2)) {
            newR.dislikeCount = (newR.dislikeCount || 0) + 1
          }
          return newR
        }
        return r
      }))
      setUserFeedbacks(prev => ({ ...prev, [item.id]: { ...prev[item.id], feedbackType } }))
    } catch (e) {
      message.error(e.message || '反馈失败')
    }
  }

  const handleRecordUsage = async (item) => {
    const user = getUser() || {}
    if (user?.id) {
      recordKnowledgeUsage({
        knowledgeId: item.id,
        userId: user.id,
        userName: user.name || user.username,
        sourceType: 2
      }).catch(() => {})
    }
    message.success('已记录方案使用')
  }

  const solutionRichTextToText = (richText) => {
    if (!richText) return ''
    return richText.replace(/<[^>]*>/g, '').trim()
  }

  return (
    <div className="fault-review-page">
      <Card
        title={
          <Space>
            <Button
              icon={<ArrowLeftOutlined />}
              onClick={handleBack}
              type="text"
              style={{ marginRight: 8 }}
            >
              返回
            </Button>
            <WarningOutlined style={{ color: '#ff4d4f', fontSize: 18 }} />
            <span style={{ fontSize: 16, fontWeight: 600 }}>故障复盘分析</span>
            {orderDetail?.orderNo && (
              <Tag color="blue" style={{ fontSize: 13 }}>
                {orderDetail.orderNo}
              </Tag>
            )}
          </Space>
        }
        extra={
          <Space>
            <Popconfirm
              title="确认打印复盘报告？"
              description="将打开打印对话框，可保存为PDF"
              onConfirm={handlePrint}
              okText="确认"
              cancelText="取消"
            >
              <Button
                icon={<PrinterOutlined />}
                type="primary"
              >
                打印复盘报告
              </Button>
            </Popconfirm>
          </Space>
        }
      />

      <Spin spinning={detailLoading} style={{ marginTop: 16 }}>
        {orderDetail && (
          <Card
            title={
              <Space>
                <FileTextOutlined />
                <span>工单基本信息</span>
              </Space>
            }
            style={{ marginTop: 16 }}
            size="small"
          >
            <Descriptions bordered column={2} size="small">
              <Descriptions.Item label="工单编号">
                <Tag color="blue">{orderDetail.orderNo || '-'}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="工单状态">
                <Tag color={(STATUS_MAP[orderDetail.status] || {}).color}>
                  {(STATUS_MAP[orderDetail.status] || {}).text || orderDetail.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="故障名称" span={2}>
                {orderDetail.faultName || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="故障代码">
                {orderDetail.faultCode ? (
                  <Tag color="red" style={{ fontFamily: 'monospace' }}>
                    {orderDetail.faultCode}
                  </Tag>
                ) : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="故障等级">
                {orderDetail.faultLevel ? (
                  <Tag color={(FAULT_LEVEL_MAP[orderDetail.faultLevel] || {}).color}>
                    {(FAULT_LEVEL_MAP[orderDetail.faultLevel] || {}).text || orderDetail.faultLevel}
                  </Tag>
                ) : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="所属电站">
                <Space>
                  <EnvironmentOutlined style={{ color: '#52c41a' }} />
                  {orderDetail.stationName || orderDetail.stationId || '-'}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="关联逆变器">
                <Space>
                  <ThunderboltOutlined style={{ color: '#1890ff' }} />
                  {orderDetail.inverterName || orderDetail.inverterId || '-'}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">
                <Space>
                  <ClockCircleOutlined style={{ color: '#8c8c8c' }} />
                  {orderDetail.createTime || '-'}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="处理人">
                <Space>
                  <UserOutlined style={{ color: '#722ed1' }} />
                  {orderDetail.handlerName || '-'}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="问题描述" span={2}>
                {orderDetail.description || '-'}
              </Descriptions.Item>
              {orderDetail.solution && (
                <Descriptions.Item label="处理方案" span={2}>
                  {orderDetail.solution}
                </Descriptions.Item>
              )}
            </Descriptions>
          </Card>
        )}
      </Spin>

      <div style={{ marginTop: 16 }}>
        {playbackProps && (
          <DataPlayback {...playbackProps} />
        )}
      </div>

      <Card
        title={
          <Space>
            <SafetyCertificateOutlined style={{ color: '#1890ff' }} />
            <span>推荐解决方案</span>
            <Tag color="purple">AI 智能推荐</Tag>
          </Space>
        }
        style={{ marginTop: 16 }}
        size="small"
        extra={
          <Space>
            <span style={{ fontSize: 12, color: '#8c8c8c' }}>
              基于知识库 + TF-IDF 算法匹配
            </span>
          </Space>
        }
      >
        <Spin spinning={recommendLoading}>
          {recommendList.length === 0 ? (
            <Empty
              description={recommendLoading ? '正在匹配推荐方案...' : '暂未找到匹配的解决方案'}
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
          ) : (
            <List
              itemLayout="vertical"
              dataSource={recommendList}
              renderItem={(item, idx) => {
                const feedback = userFeedbacks[item.id]
                const isLiked = feedback?.feedbackType === 1
                const isDisliked = feedback?.feedbackType === 2
                const confidence = Math.round(Number(item.confidence || 0) * 100)
                return (
                  <List.Item
                    key={item.id}
                    style={{
                      padding: '16px',
                      marginBottom: 12,
                      border: '1px solid #e6f4ff',
                      background: '#fafcff',
                      borderRadius: 8
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 12 }}>
                      <div style={{ flex: 1, marginRight: 16 }}>
                        <div style={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', marginBottom: 8 }}>
                          <Space size={6} wrap>
                            <Tag color="blue" style={{ margin: 0, fontFamily: 'monospace', fontSize: 12 }}>
                              #{idx + 1}
                            </Tag>
                            {item.faultCode && (
                              <Tag color="red" style={{ margin: 0, fontFamily: 'monospace', fontSize: 12 }}>
                                {item.faultCode}
                              </Tag>
                            )}
                            {item.faultLevel && (
                              <Tag
                                color={(FAULT_LEVEL_MAP[item.faultLevel] || {}).color}
                                style={{ margin: 0, fontSize: 12 }}
                              >
                                {(FAULT_LEVEL_MAP[item.faultLevel] || {}).text}
                              </Tag>
                            )}
                            <span style={{ fontWeight: 600, fontSize: 14 }}>
                              {item.faultName || '故障方案'}
                            </span>
                          </Space>
                        </div>
                        {item.matchReason && (
                          <div style={{ fontSize: 12, color: '#8c8c8c', marginBottom: 8 }}>
                            <span style={{ color: '#595959' }}>匹配原因：</span>
                            {item.matchReason}
                          </div>
                        )}
                      </div>
                      <div style={{ flexShrink: 0 }}>
                        {renderConfidenceBadge(item)}
                      </div>
                    </div>

                    {(item.solution || item.solutionRichText) && (
                      <div
                        style={{
                          padding: '12px',
                          background: '#fff',
                          border: '1px solid #f0f0f0',
                          borderRadius: 6,
                          marginBottom: 12,
                          lineHeight: 1.8,
                          fontSize: 13,
                          color: '#262626',
                          whiteSpace: 'pre-wrap'
                        }}
                      >
                        <div style={{ fontWeight: 600, color: '#1890ff', marginBottom: 6 }}>
                          <BulbOutlined /> 解决方案：
                        </div>
                        {solutionRichTextToText(item.solutionRichText) || item.solution}
                      </div>
                    )}

                    {item.causeAnalysis && (
                      <div
                        style={{
                          padding: '10px 12px',
                          background: '#fffbe6',
                          border: '1px solid #ffe58f',
                          borderRadius: 6,
                          marginBottom: 12,
                          lineHeight: 1.7,
                          fontSize: 12,
                          color: '#ad6800'
                        }}
                      >
                        <div style={{ fontWeight: 600, marginBottom: 4 }}>
                          <WarningOutlined /> 根因分析：
                        </div>
                        {item.causeAnalysis}
                      </div>
                    )}

                    {item.preventionMeasures && (
                      <div
                        style={{
                          padding: '10px 12px',
                          background: '#f6ffed',
                          border: '1px solid #b7eb8f',
                          borderRadius: 6,
                          marginBottom: 12,
                          lineHeight: 1.7,
                          fontSize: 12,
                          color: '#389e0d'
                        }}
                      >
                        <div style={{ fontWeight: 600, marginBottom: 4 }}>
                          <CheckCircleOutlined /> 预防措施：
                        </div>
                        {item.preventionMeasures}
                      </div>
                    )}

                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
                      <Space size={6} wrap>
                        <Tooltip title="方案对您是否有帮助？">
                          <Button
                            size="small"
                            type={isLiked ? 'primary' : 'text'}
                            icon={<CheckCircleOutlined />}
                            onClick={() => handleQuickFeedback(item, 1)}
                            style={{
                              padding: '0 8px',
                              color: isLiked ? '#52c41a' : '#8c8c8c'
                            }}
                          >
                            有用 {item.likeCount || 0}
                          </Button>
                        </Tooltip>
                        <Button
                          size="small"
                          type={isDisliked ? 'primary' : 'text'}
                          danger={isDisliked}
                          icon={<CloseCircleOutlined />}
                          onClick={() => handleQuickFeedback(item, 2)}
                          style={{
                            padding: '0 8px',
                            color: isDisliked ? '#ff4d4f' : '#8c8c8c'
                          }}
                        >
                          无用 {item.dislikeCount || 0}
                        </Button>
                        <span style={{ fontSize: 12, color: '#bfbfbf' }}>
                          📋 {item.useCount || 0} 人使用
                        </span>
                        {item.avgRating != null && (
                          <Space size={4} style={{ fontSize: 12, color: '#faad14' }}>
                            <Rate disabled defaultValue={Number(item.avgRating) || 0} allowHalf size={10} />
                            <span>{Number(item.avgRating).toFixed(1)}</span>
                          </Space>
                        )}
                      </Space>
                      <Space size={8}>
                        <Button
                          size="small"
                          icon={<FileTextOutlined />}
                          onClick={() => {
                            message.info(`知识库详情 - ${item.faultCode || item.id}`)
                          }}
                        >
                          查看详情
                        </Button>
                        <Button
                          size="small"
                          type="primary"
                          icon={<SafetyCertificateOutlined />}
                          onClick={() => handleRecordUsage(item)}
                        >
                          采用此方案
                        </Button>
                      </Space>
                    </div>
                  </List.Item>
                )
              }}
            />
          )}
        </Spin>
      </Card>
    </div>
  )
}

export default FaultReview
