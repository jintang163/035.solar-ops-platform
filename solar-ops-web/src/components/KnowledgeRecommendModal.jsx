import React, { useState, useEffect, useCallback } from 'react'
import {
  Modal,
  List,
  Tag,
  Button,
  Space,
  Tooltip,
  Rate,
  Progress,
  Empty,
  Card,
  Row,
  Col,
  Divider,
  message,
  Spin
} from 'antd'
import {
  LikeOutlined,
  LikeFilled,
  DislikeOutlined,
  DislikeFilled,
  BulbOutlined,
  CheckCircleOutlined,
  InfoCircleOutlined,
  VideoCameraOutlined,
  FileTextOutlined,
  CopyOutlined,
  ThunderboltOutlined
} from '@ant-design/icons'
import {
  recommendKnowledge,
  submitKnowledgeFeedback,
  getUserFeedback,
  getKnowledgeDetail,
  recordKnowledgeUsage
} from '../api/knowledge'
import { getUser } from '../utils/auth'

const FAULT_LEVEL_MAP = {
  1: { color: 'green', text: '低级' },
  2: { color: 'orange', text: '中级' },
  3: { color: 'red', text: '高级' },
  4: { color: '#cf1322', text: '紧急' }
}

const CONFIDENCE_LEVEL_MAP = {
  high: { color: '#52c41a', bgColor: '#f6ffed', text: '高置信度', desc: '强烈推荐参考此方案' },
  medium: { color: '#faad14', bgColor: '#fffbe6', text: '中置信度', desc: '建议参考此方案' },
  low: { color: '#ff7a45', bgColor: '#fff2e8', text: '低置信度', desc: '仅供参考' }
}

const KnowledgeRecommendModal = ({
  visible,
  onCancel,
  faultCode,
  faultName,
  description,
  faultLevel,
  stationId,
  inverterId,
  onSelectSolution,
  workOrderId
}) => {
  const [recommendations, setRecommendations] = useState([])
  const [loading, setLoading] = useState(false)
  const [selectedId, setSelectedId] = useState(null)
  const [detailVisible, setDetailVisible] = useState(false)
  const [currentDetail, setCurrentDetail] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [userFeedbacks, setUserFeedbacks] = useState({})

  const currentUser = getUser() || {}

  const fetchRecommendations = useCallback(async () => {
    if (!faultCode && !faultName && !description) {
      setRecommendations([])
      return
    }
    setLoading(true)
    try {
      const res = await recommendKnowledge({
        faultCode,
        faultName,
        description,
        faultLevel,
        stationId,
        inverterId,
        workOrderId,
        topN: 5,
        minConfidence: 0.2
      })
      const list = res.data || []
      setRecommendations(list)

      if (currentUser.id && list.length > 0) {
        const feedbackMap = {}
        for (const item of list) {
          try {
            const fbRes = await getUserFeedback(item.id, currentUser.id)
            if (fbRes.data) {
              feedbackMap[item.id] = fbRes.data
            }
          } catch (e) {
            // ignore
          }
        }
        setUserFeedbacks(feedbackMap)
      }
    } catch (e) {
      message.error(e.message || '获取推荐失败')
      setRecommendations([])
    } finally {
      setLoading(false)
    }
  }, [faultCode, faultName, description, faultLevel, stationId, inverterId, workOrderId, currentUser.id])

  useEffect(() => {
    if (visible) {
      fetchRecommendations()
    }
  }, [visible, fetchRecommendations])

  const getConfidenceInfo = (level) => {
    return CONFIDENCE_LEVEL_MAP[level] || CONFIDENCE_LEVEL_MAP.low
  }

  const getConfidenceStarCount = (confidence) => {
    if (!confidence) return 1
    const val = Number(confidence)
    if (val >= 0.85) return 5
    if (val >= 0.7) return 4
    if (val >= 0.5) return 3
    if (val >= 0.35) return 2
    return 1
  }

  const handleViewDetail = async (item) => {
    setDetailLoading(true)
    try {
      const res = await getKnowledgeDetail(item.id)
      setCurrentDetail(res.data || item)
      setSelectedId(item.id)
      setDetailVisible(true)
    } catch {
      setCurrentDetail(item)
      setSelectedId(item.id)
      setDetailVisible(true)
    } finally {
      setDetailLoading(false)
    }
  }

  const handleFeedback = async (item, feedbackType) => {
    if (!currentUser.id) {
      message.warning('请先登录')
      return
    }
    try {
      await submitKnowledgeFeedback({
        knowledgeId: item.id,
        workOrderId,
        userId: currentUser.id,
        userName: currentUser.name || currentUser.username,
        feedbackType
      })
      message.success('反馈已提交，感谢您的建议')

      setRecommendations(prev => prev.map(r => {
        if (r.id === item.id) {
          const newR = { ...r }
          const oldFb = userFeedbacks[item.id]
          if (oldFb && oldFb.feedbackType !== feedbackType) {
            if (oldFb.feedbackType === 1) {
              newR.likeCount = Math.max(0, (newR.likeCount || 0) - 1)
            } else if (oldFb.feedbackType === 2) {
              newR.dislikeCount = Math.max(0, (newR.dislikeCount || 0) - 1)
            }
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

      setUserFeedbacks(prev => ({
        ...prev,
        [item.id]: { ...prev[item.id], feedbackType }
      }))
    } catch (e) {
      message.error(e.message || '反馈失败')
    }
  }

  const handleApplySolution = (item) => {
    if (onSelectSolution) {
      onSelectSolution(item)
    }
    if (currentUser.id) {
      recordKnowledgeUsage({
        knowledgeId: item.id,
        workOrderId,
        userId: currentUser.id,
        userName: currentUser.name || currentUser.username,
        sourceType: 1
      }).catch(() => {})
    }
    message.success('已应用推荐方案')
    onCancel && onCancel()
  }

  const handleCopySolution = (item) => {
    const text = item.solutionRichText
      ? item.solutionRichText.replace(/<[^>]*>/g, '').trim()
      : (item.solution || '')
    if (text) {
      navigator.clipboard?.writeText(text)
      message.success('已复制解决方案到剪贴板')
    } else {
      message.warning('暂无解决方案内容')
    }
  }

  const renderConfidenceBadge = (item) => {
    const info = getConfidenceInfo(item.confidenceLevel)
    const percent = Math.round(Number(item.confidence || 0) * 100)
    return (
      <div
        style={{
          background: info.bgColor,
          border: `1px solid ${info.color}30`,
          borderRadius: 8,
          padding: '8px 12px',
          marginBottom: 8
        }}
      >
        <Row align="middle" gutter={8}>
          <Col flex="auto">
            <Space>
              <Tag color={info.color} icon={<BulbOutlined />} style={{ margin: 0 }}>
                {info.text}
              </Tag>
              <Rate
                disabled
                allowHalf
                count={5}
                value={getConfidenceStarCount(item.confidence)}
                style={{ fontSize: 12 }}
              />
              <span style={{ fontSize: 12, color: '#999' }}>{item.matchReason}</span>
            </Space>
          </Col>
          <Col>
            <Progress
              type="circle"
              size={42}
              percent={percent}
              strokeColor={info.color}
              format={(p) => `${p}%`}
            />
          </Col>
        </Row>
      </div>
    )
  }

  return (
    <>
      <Modal
        title={
          <Space>
            <ThunderboltOutlined style={{ color: '#faad14' }} />
            <span>智能推荐 - 相似故障解决方案</span>
            <Tag color="purple">AI推荐</Tag>
          </Space>
        }
        open={visible}
        onCancel={onCancel}
        footer={null}
        width={760}
        destroyOnClose
      >
        {(faultCode || faultName) && (
          <Card size="small" style={{ marginBottom: 12, background: '#f0f5ff' }}>
            <Row gutter={16} align="middle">
              <Col flex="auto">
                <Space wrap>
                  {faultCode && <Tag color="blue" style={{ fontFamily: 'monospace' }}>{faultCode}</Tag>}
                  {faultName && <span style={{ fontWeight: 500 }}>{faultName}</span>}
                  {faultLevel && (
                    <Tag color={(FAULT_LEVEL_MAP[faultLevel] || {}).color}>
                      {(FAULT_LEVEL_MAP[faultLevel] || {}).text}
                    </Tag>
                  )}
                </Space>
              </Col>
              <Col>
                <Space>
                  <InfoCircleOutlined style={{ color: '#1890ff' }} />
                  <span style={{ fontSize: 12, color: '#666' }}>基于TF-IDF智能匹配</span>
                </Space>
              </Col>
            </Row>
          </Card>
        )}

        <Spin spinning={loading} tip="正在为您搜索相似案例...">
          {!loading && recommendations.length === 0 ? (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={
                <Space direction="vertical">
                  <span>暂未找到相似的历史案例</span>
                  <span style={{ fontSize: 12, color: '#999' }}>
                    建议：完善故障描述后再试，或在知识库中搜索
                  </span>
                </Space>
              }
              style={{ padding: '40px 0' }}
            />
          ) : (
            <List
              dataSource={recommendations}
              locale={{ emptyText: '暂无推荐' }}
              renderItem={(item, index) => {
                const feedback = userFeedbacks[item.id]
                const isLiked = feedback?.feedbackType === 1
                const isDisliked = feedback?.feedbackType === 2
                const likes = item.likeCount || 0
                const dislikes = item.dislikeCount || 0
                const likeRate = likes + dislikes > 0 ? Math.round(likes / (likes + dislikes) * 100) : 0

                return (
                  <List.Item
                    key={item.id}
                    style={{
                      padding: 12,
                      marginBottom: 12,
                      border: '1px solid #e8e8e8',
                      borderRadius: 8,
                      background: selectedId === item.id ? '#e6f7ff' : '#fff'
                    }}
                  >
                    {renderConfidenceBadge(item)}
                    <Row align="middle" style={{ marginBottom: 8 }}>
                      <Col flex="auto">
                        <Space wrap>
                          <Tag color="blue" style={{ fontFamily: 'monospace', fontSize: 13 }}>
                            {item.faultCode}
                          </Tag>
                          <Tag color={(FAULT_LEVEL_MAP[item.faultLevel] || {}).color}>
                            {(FAULT_LEVEL_MAP[item.faultLevel] || {}).text}
                          </Tag>
                          {item.faultType && <Tag>{item.faultType}</Tag>}
                          <span style={{ fontWeight: 600, fontSize: 15 }}>{item.faultName}</span>
                        </Space>
                      </Col>
                      <Col>
                        <Space size={4}>
                          <span style={{ fontSize: 12, color: '#52c41a' }}>好评率 {likeRate}%</span>
                        </Space>
                      </Col>
                    </Row>

                    {item.faultDesc && (
                      <p style={{ fontSize: 12, color: '#666', margin: '4px 0 8px' }}>
                        <InfoCircleOutlined /> 故障描述：{item.faultDesc.slice(0, 100)}
                        {item.faultDesc.length > 100 ? '...' : ''}
                      </p>
                    )}

                    {item.tags && (
                      <div style={{ marginBottom: 8 }}>
                        {item.tags.split(',').slice(0, 5).map(tag => (
                          <Tag key={tag} color="geekblue" style={{ fontSize: 11 }}>{tag}</Tag>
                        ))}
                      </div>
                    )}

                    <Card size="small" style={{ background: '#fafafa', marginBottom: 8 }}>
                      <p
                        style={{
                          fontSize: 13,
                          lineHeight: 1.7,
                          margin: 0,
                          color: '#333',
                          maxHeight: 80,
                          overflow: 'hidden',
                          display: '-webkit-box',
                          WebkitLineClamp: 3,
                          WebkitBoxOrient: 'vertical'
                        }}
                      >
                        {item.solutionRichText ? (
                          <span dangerouslySetInnerHTML={{ __html: item.solutionRichText }} />
                        ) : (
                          item.solution || '暂无解决方案描述'
                        )}
                      </p>
                    </Card>

                    <Row gutter={[8, 8]} align="middle" justify="space-between">
                      <Col>
                        <Space wrap size={8}>
                          <Space size={2}>
                            <Button
                              type={isLiked ? 'primary' : 'default'}
                              size="small"
                              shape="round"
                              icon={isLiked ? <LikeFilled /> : <LikeOutlined />}
                              onClick={() => handleFeedback(item, 1)}
                            >
                              有用 {likes > 0 && `(${likes})`}
                            </Button>
                          </Space>
                          <Space size={2}>
                            <Button
                              type={isDisliked ? 'primary' : 'default'}
                              danger
                              size="small"
                              shape="round"
                              icon={isDisliked ? <DislikeFilled /> : <DislikeOutlined />}
                              onClick={() => handleFeedback(item, 2)}
                            >
                              无用 {dislikes > 0 && `(${dislikes})`}
                            </Button>
                          </Space>
                          <Space size={4} style={{ fontSize: 12, color: '#999' }}>
                            {item.videoUrl && <><VideoCameraOutlined /> 视频</>}
                            <FileTextOutlined /> {item.useCount || 0}人使用
                          </Space>
                        </Space>
                      </Col>
                      <Col>
                        <Space>
                          <Button size="small" icon={<CopyOutlined />} onClick={() => handleCopySolution(item)}>
                            复制
                          </Button>
                          <Button size="small" icon={<InfoCircleOutlined />} onClick={() => handleViewDetail(item)}>
                            查看详情
                          </Button>
                          <Button
                            type="primary"
                            size="small"
                            icon={<CheckCircleOutlined />}
                            onClick={() => handleApplySolution(item)}
                          >
                            应用方案
                          </Button>
                        </Space>
                      </Col>
                    </Row>
                  </List.Item>
                )
              }}
            />
          )}
        </Spin>
      </Modal>

      <Modal
        title="方案详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailVisible(false)}>关闭</Button>,
          <Button
            key="apply"
            type="primary"
            icon={<CheckCircleOutlined />}
            onClick={() => {
              const target = recommendations.find(r => r.id === selectedId) || currentDetail
              if (target) handleApplySolution(target)
            }}
          >
            应用此方案
          </Button>
        ]}
        width={680}
      >
        <Spin spinning={detailLoading}>
          {currentDetail && (
            <div>
              {renderConfidenceBadge(currentDetail)}
              <Space style={{ marginBottom: 8 }}>
                <Tag color="blue" style={{ fontFamily: 'monospace', fontSize: 14 }}>
                  {currentDetail.faultCode}
                </Tag>
                <Tag color={(FAULT_LEVEL_MAP[currentDetail.faultLevel] || {}).color}>
                  {(FAULT_LEVEL_MAP[currentDetail.faultLevel] || {}).text}
                </Tag>
                {currentDetail.faultType && <Tag>{currentDetail.faultType}</Tag>}
              </Space>
              <h3 style={{ margin: '8px 0 12px' }}>{currentDetail.faultName}</h3>

              {currentDetail.tags && (
                <div style={{ marginBottom: 12 }}>
                  {currentDetail.tags.split(',').map(tag => (
                    <Tag key={tag} color="geekblue">{tag}</Tag>
                  ))}
                </div>
              )}

              <Divider orientation="left" style={{ margin: '12px 0', fontSize: 13 }}>
                <InfoCircleOutlined /> 故障描述
              </Divider>
              <p style={{ lineHeight: 1.8, whiteSpace: 'pre-wrap' }}>
                {currentDetail.faultDesc || '暂无描述'}
              </p>

              <Divider orientation="left" style={{ margin: '12px 0', fontSize: 13 }}>
                <BulbOutlined /> 解决方案
              </Divider>
              {currentDetail.solutionRichText ? (
                <div
                  style={{ lineHeight: 1.8, fontSize: 14 }}
                  dangerouslySetInnerHTML={{ __html: currentDetail.solutionRichText }}
                />
              ) : (
                <p style={{ lineHeight: 1.8, whiteSpace: 'pre-wrap', color: '#666' }}>
                  {currentDetail.solution || '暂无解决方案'}
                </p>
              )}

              {currentDetail.videoUrl && (
                <>
                  <Divider orientation="left" style={{ margin: '12px 0', fontSize: 13 }}>
                    <VideoCameraOutlined /> 视频教程
                  </Divider>
                  <div style={{ textAlign: 'center', padding: 20, background: '#f5f5f5', borderRadius: 8 }}>
                    <VideoCameraOutlined style={{ fontSize: 48, color: '#1890ff' }} />
                    <p style={{ marginTop: 8 }}>
                      <a href={currentDetail.videoUrl} target="_blank" rel="noreferrer">点击播放视频教程</a>
                    </p>
                  </div>
                </>
              )}

              <Divider style={{ margin: '16px 0' }} />
              <Row style={{ fontSize: 12, color: '#999' }}>
                <Col span={8}>创建人: {currentDetail.creatorName || '-'}</Col>
                <Col span={8}>使用次数: {currentDetail.useCount || 0}</Col>
                <Col span={8} style={{ textAlign: 'right' }}>更新: {currentDetail.updateTime}</Col>
              </Row>
            </div>
          )}
        </Spin>
      </Modal>
    </>
  )
}

export default KnowledgeRecommendModal
