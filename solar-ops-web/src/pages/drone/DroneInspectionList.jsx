import React, { useState, useEffect, useCallback } from 'react'
import {
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  message,
  Card,
  Tag,
  Descriptions,
  Row,
  Col,
  Statistic,
  InputNumber,
  DatePicker,
  Upload,
  List,
  Progress,
  Divider,
  Tooltip,
  Popconfirm
} from 'antd'
import {
  PlusOutlined,
  EyeOutlined,
  PlayCircleOutlined,
  DeleteOutlined,
  UploadOutlined,
  DroneOutlined,
  CameraOutlined,
  BugOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined
} from '@ant-design/icons'
import {
  getDroneTaskPage,
  getDroneTaskDetail,
  createDroneTask,
  deleteDroneTask,
  getDroneTaskStatistics,
  getTaskImages,
  getTaskDefects,
  triggerBatchDetection,
  uploadDroneImage,
  batchUploadDroneImages
} from '../../api/drone'
import DroneImageViewer from './DroneImageViewer'
import { getStationList } from '../../api/station'

const { TextArea } = Input
const { Option } = Select
const { RangePicker } = DatePicker

const TASK_STATUS_MAP = {
  0: { color: 'default', text: '待执行' },
  1: { color: 'processing', text: '执行中' },
  2: { color: 'success', text: '已完成' },
  3: { color: 'default', text: '已取消' },
  4: { color: 'error', text: '异常' }
}

const DEFECT_TYPE_NAMES = {
  hot_spot: '热斑',
  microcrack: '隐裂',
  shadow: '遮挡',
  delamination: '脱层',
  broken: '破损',
  dirt: '脏污'
}

const PAGE_SIZE = 10

const DroneInspectionList = () => {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [statistics, setStatistics] = useState({})
  const [stationList, setStationList] = useState([])
  const [detailVisible, setDetailVisible] = useState(false)
  const [addVisible, setAddVisible] = useState(false)
  const [imageViewerVisible, setImageViewerVisible] = useState(false)
  const [currentTask, setCurrentTask] = useState(null)
  const [currentImage, setCurrentImage] = useState(null)
  const [taskImages, setTaskImages] = useState([])
  const [taskDefects, setTaskDefects] = useState([])
  const [detailLoading, setDetailLoading] = useState(false)
  const [addLoading, setAddLoading] = useState(false)
  const [addForm] = Form.useForm()
  const [uploading, setUploading] = useState(false)

  const fetchStatistics = useCallback(async () => {
    try {
      const res = await getDroneTaskStatistics()
      setStatistics(res.data || {})
    } catch {
    }
  }, [])

  const fetchStations = useCallback(async () => {
    try {
      const res = await getStationList()
      setStationList(res.data || [])
    } catch {
    }
  }, [])

  const fetchData = useCallback(async (page = 1) => {
    setLoading(true)
    try {
      const params = { pageNum: page, pageSize: PAGE_SIZE }
      const res = await getDroneTaskPage(params)
      const pageResult = res.data || {}
      setData(pageResult.list || [])
      setTotal(pageResult.total || 0)
      setPageNum(pageResult.pageNum || page)
    } catch {
      setData([])
      setTotal(0)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchStatistics()
    fetchStations()
    fetchData(1)
  }, [fetchStatistics, fetchStations, fetchData])

  const handlePageChange = (page) => {
    fetchData(page)
  }

  const handleViewDetail = async (record) => {
    setDetailLoading(true)
    try {
      const [detailRes, imagesRes, defectsRes] = await Promise.all([
        getDroneTaskDetail(record.id),
        getTaskImages(record.id),
        getTaskDefects(record.id)
      ])
      setCurrentTask(detailRes.data || record)
      setTaskImages(imagesRes.data || [])
      setTaskDefects(defectsRes.data || [])
      setDetailVisible(true)
    } catch {
      setCurrentTask(record)
      setTaskImages([])
      setTaskDefects([])
      setDetailVisible(true)
    } finally {
      setDetailLoading(false)
    }
  }

  const handleViewImage = (image) => {
    setCurrentImage(image)
    setImageViewerVisible(true)
  }

  const handleAdd = () => {
    addForm.resetFields()
    setAddVisible(true)
  }

  const handleAddOk = async () => {
    try {
      const values = await addForm.validateFields()
      setAddLoading(true)
      await createDroneTask({
        ...values,
        startTime: values.timeRange?.[0]?.format('YYYY-MM-DD HH:mm:ss'),
        endTime: values.timeRange?.[1]?.format('YYYY-MM-DD HH:mm:ss')
      })
      message.success('任务创建成功')
      setAddVisible(false)
      fetchData(1)
      fetchStatistics()
    } catch (error) {
      if (error.errorFields) return
      message.error(error.message || '创建失败')
    } finally {
      setAddLoading(false)
    }
  }

  const handleDelete = async (id) => {
    try {
      await deleteDroneTask(id)
      message.success('删除成功')
      fetchData(pageNum)
      fetchStatistics()
    } catch (error) {
      message.error(error.message || '删除失败')
    }
  }

  const handleDetectAll = async (record) => {
    try {
      await triggerBatchDetection(record.id)
      message.success('检测任务已提交，请稍候...')
      setTimeout(() => {
        handleViewDetail(record)
      }, 2000)
    } catch (error) {
      message.error(error.message || '启动检测失败')
    }
  }

  const handleUpload = async (file) => {
    if (!currentTask?.id) {
      message.error('请先选择巡检任务')
      return false
    }
    setUploading(true)
    try {
      await uploadDroneImage(currentTask.id, file, 'visible')
      message.success('图片上传成功')
      const imagesRes = await getTaskImages(currentTask.id)
      setTaskImages(imagesRes.data || [])
    } catch (e) {
      message.error('上传失败: ' + (e.message || ''))
    } finally {
      setUploading(false)
    }
    return false
  }

  const handleBatchUpload = (files) => {
    if (!currentTask?.id) {
      message.error('请先选择巡检任务')
      return false
    }
    setUploading(true)
    batchUploadDroneImages(currentTask.id, files, 'visible')
      .then(() => {
        message.success('批量上传成功')
        return getTaskImages(currentTask.id)
      })
      .then((res) => {
        setTaskImages(res.data || [])
      })
      .catch((e) => {
        message.error('上传失败: ' + (e.message || ''))
      })
      .finally(() => {
        setUploading(false)
      })
    return false
  }

  const columns = [
    {
      title: '任务编号',
      dataIndex: 'taskCode',
      key: 'taskCode',
      width: 160,
      render: (text, record) => (
        <a onClick={() => handleViewDetail(record)}>{text}</a>
      )
    },
    {
      title: '任务名称',
      dataIndex: 'taskName',
      key: 'taskName',
      width: 180,
      ellipsis: true
    },
    {
      title: '所属电站',
      dataIndex: 'stationName',
      key: 'stationName',
      width: 140
    },
    {
      title: '巡检区域',
      dataIndex: 'area',
      key: 'area',
      width: 120
    },
    {
      title: '飞行模式',
      dataIndex: 'flightMode',
      key: 'flightMode',
      width: 100,
      render: (mode) => {
        const modeMap = { manual: '手动', auto: '自动', waypoint: '航点' }
        return modeMap[mode] || mode
      }
    },
    {
      title: '图片数',
      dataIndex: 'imageCount',
      key: 'imageCount',
      width: 80,
      render: (count) => (
        <Tag color="blue">
          <CameraOutlined /> {count || 0}
        </Tag>
      )
    },
    {
      title: '缺陷数',
      dataIndex: 'defectCount',
      key: 'defectCount',
      width: 80,
      render: (count) => (
        <Tag color={count > 0 ? 'red' : 'green'}>
          <BugOutlined /> {count || 0}
        </Tag>
      )
    },
    {
      title: '工单数',
      dataIndex: 'workorderCount',
      key: 'workorderCount',
      width: 80,
      render: (count) => (
        <Tag color="orange">
          <FileTextOutlined /> {count || 0}
        </Tag>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (status) => {
        const info = TASK_STATUS_MAP[status] || { color: 'default', text: status }
        return <Tag color={info.color}>{info.text}</Tag>
      }
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 170
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record)}
          >
            详情
          </Button>
          {record.status === 0 && (
            <Button
              type="link"
              size="small"
              icon={<PlayCircleOutlined />}
              onClick={() => handleDetectAll(record)}
              disabled={record.imageCount === 0}
            >
              开始检测
            </Button>
          )}
          <Popconfirm
            title="确定删除该任务？"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  const defectTypeStats = taskDefects.reduce((acc, item) => {
    acc[item.defectType] = (acc[item.defectType] || 0) + 1
    return acc
  }, {})

  return (
    <div className="drone-inspection-list-page">
      <Card
        title={
          <Space>
            <DroneOutlined />
            <span>无人机巡检任务</span>
          </Space>
        }
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新建任务
          </Button>
        }
      >
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col xs={12} sm={6} md={4}>
            <Card size="small">
              <Statistic
                title="全部任务"
                value={statistics.totalCount || 0}
                prefix={<DroneOutlined />}
                valueStyle={{ fontSize: 20 }}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6} md={4}>
            <Card size="small">
              <Statistic
                title="待执行"
                value={statistics.pendingCount || 0}
                valueStyle={{ color: '#faad14', fontSize: 20 }}
                prefix={<ClockCircleOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6} md={4}>
            <Card size="small">
              <Statistic
                title="执行中"
                value={statistics.processingCount || 0}
                valueStyle={{ color: '#1890ff', fontSize: 20 }}
                prefix={<PlayCircleOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6} md={4}>
            <Card size="small">
              <Statistic
                title="已完成"
                value={statistics.completedCount || 0}
                valueStyle={{ color: '#52c41a', fontSize: 20 }}
                prefix={<CheckCircleOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6} md={4}>
            <Card size="small">
              <Statistic
                title="图片总数"
                value={statistics.imageCount || 0}
                valueStyle={{ color: '#13c2c2', fontSize: 20 }}
                prefix={<CameraOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6} md={4}>
            <Card size="small">
              <Statistic
                title="缺陷总数"
                value={statistics.defectCount || 0}
                valueStyle={{ color: '#f5222d', fontSize: 20 }}
                prefix={<BugOutlined />}
              />
            </Card>
          </Col>
        </Row>

        <Table
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1400 }}
          pagination={{
            current: pageNum,
            pageSize: PAGE_SIZE,
            total,
            showTotal: (t) => `共 ${t} 条`,
            showSizeChanger: false,
            onChange: handlePageChange
          }}
        />
      </Card>

      <Modal
        title="任务详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={1000}
        destroyOnClose
      >
        {detailLoading ? (
          <div style={{ textAlign: 'center', padding: '60px 0' }}>
            <Progress type="circle" percent={75} />
          </div>
        ) : currentTask && (
          <div>
            <Descriptions title="任务信息" bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="任务编号">{currentTask.taskCode}</Descriptions.Item>
              <Descriptions.Item label="任务名称">{currentTask.taskName}</Descriptions.Item>
              <Descriptions.Item label="所属电站">{currentTask.stationName || '-'}</Descriptions.Item>
              <Descriptions.Item label="巡检区域">{currentTask.area || '-'}</Descriptions.Item>
              <Descriptions.Item label="飞行模式">
                {currentTask.flightMode === 'manual' ? '手动' : currentTask.flightMode === 'auto' ? '自动' : currentTask.flightMode === 'waypoint' ? '航点' : currentTask.flightMode}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={(TASK_STATUS_MAP[currentTask.status] || {}).color}>
                  {(TASK_STATUS_MAP[currentTask.status] || {}).text || currentTask.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="无人机编号">{currentTask.droneCode || '-'}</Descriptions.Item>
              <Descriptions.Item label="飞手">{currentTask.pilot || '-'}</Descriptions.Item>
              <Descriptions.Item label="图片数量">
                <Tag color="blue">{currentTask.imageCount || 0}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="缺陷数量">
                <Tag color="red">{currentTask.defectCount || 0}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="工单数量">
                <Tag color="orange">{currentTask.workorderCount || 0}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">{currentTask.createTime || '-'}</Descriptions.Item>
              {currentTask.description && (
                <Descriptions.Item label="任务描述" span={2}>{currentTask.description}</Descriptions.Item>
              )}
            </Descriptions>

            <Divider orientation="left" plain>
              <Space>
                <CameraOutlined />
                <span>巡检图片 ({taskImages.length})</span>
              </Space>
            </Divider>

            <div style={{ marginBottom: 12 }}>
              <Space>
                <Upload
                  beforeUpload={handleUpload}
                  showUploadList={false}
                  disabled={uploading}
                >
                  <Button size="small" icon={<UploadOutlined />} loading={uploading}>
                    上传图片
                  </Button>
                </Upload>
                <Upload
                  multiple
                  beforeUpload={(file) => handleBatchUpload([file])}
                  showUploadList={false}
                  disabled={uploading}
                >
                  <Button size="small" icon={<UploadOutlined />} loading={uploading}>
                    批量上传
                  </Button>
                </Upload>
                {taskImages.length > 0 && (
                  <Button
                    size="small"
                    type="primary"
                    icon={<PlayCircleOutlined />}
                    onClick={() => handleDetectAll(currentTask)}
                  >
                    全部检测
                  </Button>
                )}
              </Space>
            </div>

            {taskImages.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
                暂无图片，请上传巡检图片
              </div>
            ) : (
              <List
                grid={{ gutter: 12, xs: 2, sm: 3, md: 4, lg: 5, xl: 6 }}
                dataSource={taskImages}
                renderItem={(item) => (
                  <List.Item>
                    <Card
                      size="small"
                      hoverable
                      cover={
                        <div
                          style={{
                            height: 120,
                            background: '#f5f5f5',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            overflow: 'hidden',
                            cursor: 'pointer'
                          }}
                          onClick={() => handleViewImage(item)}
                        >
                          {item.imageUrl || item.imagePath ? (
                            <img
                              src={item.imageUrl || item.imagePath}
                              alt={item.imageName}
                              style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                            />
                          ) : (
                            <CameraOutlined style={{ fontSize: 32, color: '#ccc' }} />
                          )}
                        </div>
                      }
                    >
                      <Card.Meta
                        title={
                          <Tooltip title={item.imageName}>
                            <div style={{ fontSize: 12, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                              {item.imageName}
                            </div>
                          </Tooltip>
                        }
                        description={
                          <Space size="small">
                            <Tag color={item.detectStatus === 2 ? 'success' : item.detectStatus === 1 ? 'processing' : 'default'}>
                              {item.defectCount || 0} 缺陷
                            </Tag>
                          </Space>
                        }
                      />
                    </Card>
                  </List.Item>
                )}
              />
            )}

            {taskDefects.length > 0 && (
              <>
                <Divider orientation="left" plain style={{ marginTop: 16 }}>
                  <Space>
                    <BugOutlined />
                    <span>缺陷统计 ({taskDefects.length})</span>
                  </Space>
                </Divider>
                <Row gutter={8}>
                  {Object.entries(defectTypeStats).map(([type, count]) => (
                    <Col key={type} span={4}>
                      <Card size="small" style={{ textAlign: 'center' }}>
                        <div style={{ fontSize: 12, color: '#666', marginBottom: 4 }}>
                          {DEFECT_TYPE_NAMES[type] || type}
                        </div>
                        <div style={{ fontSize: 18, fontWeight: 'bold', color: '#f5222d' }}>
                          {count}
                        </div>
                      </Card>
                    </Col>
                  ))}
                </Row>
              </>
            )}
          </div>
        )}
      </Modal>

      <Modal
        title="新建巡检任务"
        open={addVisible}
        onOk={handleAddOk}
        onCancel={() => setAddVisible(false)}
        confirmLoading={addLoading}
        okText="提交"
        cancelText="取消"
        width={520}
      >
        <Form form={addForm} layout="vertical">
          <Form.Item
            name="taskName"
            label="任务名称"
            rules={[{ required: true, message: '请输入任务名称' }]}
          >
            <Input placeholder="请输入任务名称" />
          </Form.Item>
          <Form.Item
            name="stationId"
            label="所属电站"
            rules={[{ required: true, message: '请选择电站' }]}
          >
            <Select placeholder="请选择电站">
              {stationList.map((station) => (
                <Option key={station.id} value={station.id}>
                  {station.stationName}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="area"
                label="巡检区域"
              >
                <Input placeholder="如：东区1号方阵" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="flightMode"
                label="飞行模式"
              >
                <Select placeholder="请选择飞行模式">
                  <Option value="manual">手动</Option>
                  <Option value="auto">自动</Option>
                  <Option value="waypoint">航点</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="droneCode"
                label="无人机编号"
              >
                <Input placeholder="如：DRONE-001" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="pilot"
                label="飞手"
              >
                <Input placeholder="请输入飞手姓名" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item
            name="timeRange"
            label="计划时间"
          >
            <RangePicker showTime style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="description"
            label="任务描述"
          >
            <TextArea rows={3} placeholder="请描述巡检任务内容" />
          </Form.Item>
        </Form>
      </Modal>

      <DroneImageViewer
        visible={imageViewerVisible}
        imageData={currentImage}
        onClose={() => setImageViewerVisible(false)}
      />
    </div>
  )
}

export default DroneInspectionList
