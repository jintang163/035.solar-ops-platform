import React, { useState, useEffect, useRef } from 'react'
import {
  Modal,
  Button,
  Space,
  Upload,
  Select,
  Tag,
  Row,
  Col,
  List,
  Card,
  Descriptions,
  Divider,
  message,
  Progress,
  Popconfirm,
  Tooltip
} from 'antd'
import {
  UploadOutlined,
  PlayCircleOutlined,
  CheckCircleOutlined,
  FileTextOutlined,
  BugOutlined,
  DeleteOutlined,
  ReloadOutlined,
  ZoomInOutlined,
  ZoomOutOutlined
} from '@ant-design/icons'
import {
  uploadDroneImage,
  triggerImageDetection,
  getDroneImageDetail,
  getImageDefects,
  verifyDroneDefect,
  createWorkOrderFromDefect,
  deleteDroneImage
} from '../../api/drone'

const { Option } = Select

const defectTypeColors = {
  hot_spot: '#ff4d4f',
  microcrack: '#faad14',
  shadow: '#8c8c8c',
  delamination: '#eb2f96',
  broken: '#f5222d',
  dirt: '#fa8c16'
}

const defectTypeNames = {
  hot_spot: '热斑',
  microcrack: '隐裂',
  shadow: '遮挡',
  delamination: '脱层',
  broken: '破损',
  dirt: '脏污'
}

const levelColors = ['', 'blue', 'orange', 'red', 'magenta']
const levelNames = ['', '轻微', '一般', '严重', '紧急']

const DroneImageViewer = ({ visible, imageData, onClose }) => {
  const canvasRef = useRef(null)
  const [imageDetail, setImageDetail] = useState(null)
  const [defects, setDefects] = useState([])
  const [selectedDefect, setSelectedDefect] = useState(null)
  const [detecting, setDetecting] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [imageType, setImageType] = useState('visible')
  const [canvasScale, setCanvasScale] = useState(1)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (visible && imageData?.id) {
      loadImageDetail()
    }
  }, [visible, imageData?.id])

  const loadImageDetail = async () => {
    setLoading(true)
    try {
      const [detailRes, defectsRes] = await Promise.all([
        getDroneImageDetail(imageData.id),
        getImageDefects(imageData.id)
      ])
      setImageDetail(detailRes.data)
      setDefects(defectsRes.data || [])
      drawAnnotations(detailRes.data, defectsRes.data || [])
    } catch (e) {
      message.error('加载图片详情失败')
    } finally {
      setLoading(false)
    }
  }

  const drawAnnotations = (image, defectList) => {
    const canvas = canvasRef.current
    if (!canvas) return

    const ctx = canvas.getContext('2d')
    const img = new window.Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      canvas.width = image.imageWidth || img.width
      canvas.height = image.imageHeight || img.height
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height)

      defectList.forEach(defect => {
        const bbox = defect.bbox || [defect.xMin, defect.yMin, defect.xMax, defect.yMax]
        if (bbox && bbox.length === 4) {
          const color = defectTypeColors[defect.defectType] || '#1890ff'
          ctx.strokeStyle = color
          ctx.lineWidth = 3
          ctx.strokeRect(bbox[0], bbox[1], bbox[2] - bbox[0], bbox[3] - bbox[1])

          const center = defect.center || [defect.centerX, defect.centerY]
          if (center && center.length === 2) {
            ctx.beginPath()
            ctx.arc(center[0], center[1], 5, 0, 2 * Math.PI)
            ctx.fillStyle = color
            ctx.fill()

            ctx.fillStyle = color
            ctx.font = 'bold 12px Arial'
            ctx.fillText(`(${center[0]},${center[1]})`, center[0] + 8, center[1] + 4)
          }

          const label = `${defectTypeNames[defect.defectType] || defect.defectType} ${(defect.confidence * 100).toFixed(1)}%`
          if (defect.temperature) {
            label += ` ${defect.temperature}℃`
          }
          const textWidth = ctx.measureText(label).width
          ctx.fillStyle = color
          ctx.fillRect(bbox[0], bbox[1] - 22, textWidth + 12, 22)
          ctx.fillStyle = '#fff'
          ctx.fillText(label, bbox[0] + 6, bbox[1] - 6)
        }
      })

      if (selectedDefect) {
        const bbox = selectedDefect.bbox || [selectedDefect.xMin, selectedDefect.yMin, selectedDefect.xMax, selectedDefect.yMax]
        if (bbox && bbox.length === 4) {
          ctx.strokeStyle = '#fff'
          ctx.lineWidth = 4
          ctx.strokeRect(bbox[0] - 2, bbox[1] - 2, bbox[2] - bbox[0] + 4, bbox[3] - bbox[1] + 4)
        }
      }
    }
    img.src = image.annotatedPath || image.imagePath
  }

  useEffect(() => {
    if (imageDetail) {
      drawAnnotations(imageDetail, defects)
    }
  }, [defects, selectedDefect])

  const handleUpload = async (file) => {
    if (!imageData?.taskId) {
      message.error('请先选择巡检任务')
      return false
    }

    setUploading(true)
    try {
      await uploadDroneImage(imageData.taskId, file, imageType)
      message.success('图片上传成功，正在进行AI检测')
      loadImageDetail()
    } catch (e) {
      message.error('上传失败: ' + e.message)
    } finally {
      setUploading(false)
    }
    return false
  }

  const handleDetect = async () => {
    if (!imageData?.id) return

    setDetecting(true)
    try {
      await triggerImageDetection(imageData.id)
      message.success('检测任务已提交，请稍候...')
      setTimeout(() => {
        loadImageDetail()
      }, 3000)
    } catch (e) {
      message.error('启动检测失败')
    } finally {
      setDetecting(false)
    }
  }

  const handleVerify = async (defectId) => {
    try {
      await verifyDroneDefect(defectId, 1)
      message.success('确认成功')
      loadImageDetail()
    } catch (e) {
      message.error('确认失败')
    }
  }

  const handleCreateWorkOrder = async (defect) => {
    try {
      const workorderId = await createWorkOrderFromDefect(defect.id, 1, '管理员')
      message.success(`工单创建成功，工单ID: ${workorderId}`)
      loadImageDetail()
    } catch (e) {
      message.error('创建工单失败')
    }
  }

  const handleDelete = async () => {
    try {
      await deleteDroneImage(imageData.id)
      message.success('删除成功')
      onClose()
    } catch (e) {
      message.error('删除失败')
    }
  }

  const handleCanvasWheel = (e) => {
    e.preventDefault()
    const delta = e.deltaY > 0 ? -0.1 : 0.1
    const newScale = Math.max(0.5, Math.min(3, canvasScale + delta))
    setCanvasScale(newScale)
  }

  const handleDefectClick = (defect) => {
    setSelectedDefect(defect)
  }

  return (
    <Modal
      title="图片检测详情"
      open={visible}
      onCancel={onClose}
      footer={null}
      width={1200}
      destroyOnClose
    >
      <div className="drone-image-viewer">
        <Row gutter={16}>
          <Col span={16}>
            <Card
              size="small"
              title={
                <Space>
                  <span>{imageDetail?.imageName || '-'}</span>
                  {imageDetail?.detectStatus === 2 && (
                    <Tag color="success">检测完成</Tag>
                  )}
                  {imageDetail?.detectStatus === 1 && (
                    <Tag color="processing">检测中...</Tag>
                  )}
                  {imageDetail?.detectStatus === 3 && (
                    <Tag color="error">检测失败</Tag>
                  )}
                  {imageDetail?.detectStatus === 0 && (
                    <Tag color="default">待检测</Tag>
                  )}
                </Space>
              }
              extra={
                <Space>
                  <Upload
                    beforeUpload={handleUpload}
                    showUploadList={false}
                    disabled={!imageData?.taskId}
                  >
                    <Button
                      size="small"
                      icon={<UploadOutlined />}
                      loading={uploading}
                    >
                      上传图片
                    </Button>
                  </Upload>
                  <Select
                    size="small"
                    value={imageType}
                    onChange={setImageType}
                    style={{ width: 100 }}
                  >
                    <Option value="visible">可见光</Option>
                    <Option value="infrared">红外</Option>
                    <Option value="thermal">热成像</Option>
                  </Select>
                  {imageData?.id && (
                    <Tooltip title="重新检测">
                      <Button
                        size="small"
                        icon={<ReloadOutlined />}
                        onClick={handleDetect}
                        loading={detecting}
                      >
                        检测
                      </Button>
                    </Tooltip>
                  )}
                  {imageData?.id && (
                    <Tooltip title="放大">
                      <Button
                        size="small"
                        icon={<ZoomInOutlined />}
                        onClick={() => setCanvasScale(Math.min(3, canvasScale + 0.2))}
                      />
                    </Tooltip>
                  )}
                  {imageData?.id && (
                    <Tooltip title="缩小">
                      <Button
                        size="small"
                        icon={<ZoomOutOutlined />}
                        onClick={() => setCanvasScale(Math.max(0.5, canvasScale - 0.2))}
                      />
                    </Tooltip>
                  )}
                  {imageData?.id && (
                    <Popconfirm
                      title="确定删除该图片？"
                      onConfirm={handleDelete}
                      okText="确定"
                      cancelText="取消"
                    >
                      <Button size="small" danger icon={<DeleteOutlined />} />
                    </Popconfirm>
                  )}
                </Space>
              }
            >
              <div
                style={{
                  overflow: 'auto',
                  maxHeight: '600px',
                  background: '#f5f5f5',
                  padding: '20px',
                  textAlign: 'center'
                }}
                onWheel={handleCanvasWheel}
              >
                {loading ? (
                  <div style={{ padding: '100px 0' }}>
                    <Progress type="circle" percent={75} />
                  </div>
                ) : (
                  <canvas
                    ref={canvasRef}
                    style={{
                      transform: `scale(${canvasScale})`,
                      transformOrigin: 'top left',
                      cursor: 'crosshair',
                      boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
                      borderRadius: '4px'
                    }}
                  />
                )}
              </div>

              {imageDetail && (
                <Descriptions size="small" column={4} style={{ marginTop: 12 }}>
                  <Descriptions.Item label="图片类型">
                    {imageDetail.imageType === 'visible' ? '可见光' : imageDetail.imageType === 'infrared' ? '红外' : '热成像'}
                  </Descriptions.Item>
                  <Descriptions.Item label="尺寸">
                    {imageDetail.imageWidth} x {imageDetail.imageHeight}
                  </Descriptions.Item>
                  <Descriptions.Item label="文件大小">
                    {((imageDetail.fileSize || 0) / 1024 / 1024).toFixed(2)} MB
                  </Descriptions.Item>
                  <Descriptions.Item label="拍摄时间">
                    {imageDetail.shootTime || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="经度">{imageDetail.longitude || '-'}</Descriptions.Item>
                  <Descriptions.Item label="纬度">{imageDetail.latitude || '-'}</Descriptions.Item>
                  <Descriptions.Item label="飞行高度">{imageDetail.altitude ? imageDetail.altitude + 'm' : '-'}</Descriptions.Item>
                  <Descriptions.Item label="缺陷数">
                    <Tag color={imageDetail.defectCount > 0 ? 'red' : 'green'}>
                      {imageDetail.defectCount || 0}
                    </Tag>
                  </Descriptions.Item>
                </Descriptions>
              )}
            </Card>
          </Col>

          <Col span={8}>
            <Card
              size="small"
              title={
                <Space>
                  <BugOutlined />
                  <span>缺陷列表</span>
                  <Tag color="red">{defects.length}</Tag>
                </Space>
              }
              extra={
                defects.length > 0 && (
                  <Space>
                    <Button
                      type="link"
                      size="small"
                      onClick={() => setSelectedDefect(null)}
                    >
                      取消选中
                    </Button>
                  </Space>
                )
              }
              style={{ height: '100%' }}
              bodyStyle={{ padding: 0 }}
            >
              {defects.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '60px 0', color: '#999' }}>
                  暂无缺陷
                </div>
              ) : (
                <List
                  size="small"
                  dataSource={defects}
                  renderItem={(item) => (
                    <List.Item
                      key={item.id}
                      style={{
                        padding: '12px',
                        cursor: 'pointer',
                        borderLeft: selectedDefect?.id === item.id ? '3px solid #1890ff' : '3px solid transparent',
                        background: selectedDefect?.id === item.id ? '#e6f7ff' : 'transparent'
                      }}
                      onClick={() => handleDefectClick(item)}
                    >
                      <List.Item.Meta
                        title={
                          <Space>
                            <Tag color={defectTypeColors[item.defectType]}>
                              {defectTypeNames[item.defectType]}
                            </Tag>
                            <Tag color={levelColors[item.defectLevel]}>
                              {levelNames[item.defectLevel]}
                            </Tag>
                            {item.temperature && (
                              <Tag color="red">{item.temperature}℃</Tag>
                            )}
                          </Space>
                        }
                        description={
                          <div style={{ fontSize: '12px' }}>
                            <div style={{ color: '#666', marginBottom: 4 }}>
                              置信度: {(item.confidence * 100).toFixed(1)}%
                              {item.center && <span style={{ marginLeft: 8 }}>
                                坐标: ({item.center[0]}, {item.center[1]})
                              </span>}
                            </div>
                            <div style={{ color: '#999', marginBottom: 8 }}>
                              {item.description}
                            </div>
                            <Space size="small">
                              {item.verified !== 1 && (
                                <Button
                                  type="link"
                                  size="small"
                                  icon={<CheckCircleOutlined />}
                                  onClick={(e) => { e.stopPropagation(); handleVerify(item.id) }}
                                >
                                  确认
                                </Button>
                              )}
                              {item.verified === 1 && (
                                <Tag color="green" size="small">已确认</Tag>
                              )}
                              {!item.workorderId && (
                                <Button
                                  type="link"
                                  size="small"
                                  icon={<FileTextOutlined />}
                                  onClick={(e) => { e.stopPropagation(); handleCreateWorkOrder(item) }}
                                >
                                  生成工单
                                </Button>
                              )}
                              {item.workorderId && (
                                <Tag color="orange" size="small">
                                  工单#{item.workorderId}
                                </Tag>
                              )}
                            </Space>
                          </div>
                        }
                      />
                    </List.Item>
                  )}
                />
              )}
            </Card>

            {selectedDefect && (
              <Card
                size="small"
                title="缺陷详情"
                style={{ marginTop: 16 }}
              >
                <Descriptions size="small" column={1}>
                  <Descriptions.Item label="缺陷编号">
                    {selectedDefect.defectCode || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="缺陷类型">
                    <Tag color={defectTypeColors[selectedDefect.defectType]}>
                      {defectTypeNames[selectedDefect.defectType]}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="缺陷等级">
                    <Tag color={levelColors[selectedDefect.defectLevel]}>
                      {levelNames[selectedDefect.defectLevel]}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="置信度">
                    {(selectedDefect.confidence * 100).toFixed(2)}%
                  </Descriptions.Item>
                  <Descriptions.Item label="中心坐标">
                    {selectedDefect.center
                      ? `(${selectedDefect.center[0]}, ${selectedDefect.center[1]})`
                      : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="缺陷占比">
                    {selectedDefect.areaRatio?.toFixed(2)}%
                  </Descriptions.Item>
                  {selectedDefect.temperature && (
                    <>
                      <Descriptions.Item label="温度">
                        {selectedDefect.temperature}℃
                      </Descriptions.Item>
                      {selectedDefect.deltaTemperature && (
                        <Descriptions.Item label="温度差">
                          <span style={{ color: '#f5222d', fontWeight: 'bold' }}>
                            {selectedDefect.deltaTemperature}℃
                          </span>
                        </Descriptions.Item>
                      )}
                    </>
                  )}
                  <Descriptions.Item label="处理建议">
                    {selectedDefect.suggestion || '-'}
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            )}
          </Col>
        </Row>
      </div>
    </Modal>
  )
}

export default DroneImageViewer
