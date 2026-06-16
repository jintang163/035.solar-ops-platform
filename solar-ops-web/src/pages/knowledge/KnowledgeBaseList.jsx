import React, { useState, useEffect, useCallback, useRef } from 'react'
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
  Row,
  Col,
  InputNumber,
  Upload,
  Tooltip,
  Divider,
  Rate,
  Statistic,
  Empty
} from 'antd'
import {
  PlusOutlined,
  SearchOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  LikeOutlined,
  LikeFilled,
  DislikeOutlined,
  DislikeFilled,
  UploadOutlined,
  VideoCameraOutlined,
  FileTextOutlined,
  ReloadOutlined,
  BulbOutlined
} from '@ant-design/icons'
import {
  getKnowledgePage,
  getKnowledgeDetail,
  addKnowledge,
  updateKnowledge,
  deleteKnowledge,
  refreshKnowledgeCache,
  submitKnowledgeFeedback,
  getUserFeedback,
  recordKnowledgeUsage
} from '../../api/knowledge'
import { getUser } from '../../utils/auth'

const { TextArea } = Input
const { Option } = Select

const FAULT_LEVEL_MAP = {
  1: { color: 'green', text: '低级' },
  2: { color: 'orange', text: '中级' },
  3: { color: 'red', text: '高级' },
  4: { color: '#cf1322', text: '紧急' }
}

const STATUS_MAP = {
  0: { color: 'default', text: '草稿' },
  1: { color: 'green', text: '已发布' },
  2: { color: 'gray', text: '已归档' }
}

const FAULT_TYPES = [
  '通讯故障', '温度故障', '电气故障', '电网故障', '组件故障',
  '设备故障', '效率异常', '软件故障', '其他'
]

const PAGE_SIZE = 10

const RichTextEditor = ({ value, onChange }) => {
  const editorRef = useRef(null)

  useEffect(() => {
    if (editorRef.current && editorRef.current.innerHTML !== value) {
      editorRef.current.innerHTML = value || ''
    }
  }, [value])

  const execCommand = (command, value = null) => {
    document.execCommand(command, false, value)
    if (editorRef.current) {
      onChange(editorRef.current.innerHTML)
    }
  }

  const toolbarButtons = [
    { cmd: 'bold', label: 'B', style: { fontWeight: 'bold' }, title: '加粗' },
    { cmd: 'italic', label: 'I', style: { fontStyle: 'italic' }, title: '斜体' },
    { cmd: 'underline', label: 'U', style: { textDecoration: 'underline' }, title: '下划线' },
    { cmd: 'insertUnorderedList', label: '• 列表', title: '无序列表' },
    { cmd: 'insertOrderedList', label: '1. 列表', title: '有序列表' },
    { cmd: 'formatBlock', value: 'h3', label: 'H3', title: '标题3' },
    { cmd: 'formatBlock', value: 'p', label: '正文', title: '正文' }
  ]

  return (
    <div className="rich-text-editor" style={{ border: '1px solid #d9d9d9', borderRadius: 6 }}>
      <div style={{ padding: '8px', borderBottom: '1px solid #f0f0f0', background: '#fafafa' }}>
        <Space wrap>
          {toolbarButtons.map((btn, idx) => (
            <Button
              key={idx}
              size="small"
              onClick={() => execCommand(btn.cmd, btn.value)}
              title={btn.title}
              style={btn.style}
            >
              {btn.label}
            </Button>
          ))}
          <Select
            size="small"
            placeholder="文字颜色"
            style={{ width: 100 }}
            onChange={(v) => execCommand('foreColor', v)}
          >
            <Option value="#000000">黑色</Option>
            <Option value="#ff4d4f">红色</Option>
            <Option value="#faad14">橙色</Option>
            <Option value="#52c41a">绿色</Option>
            <Option value="#1890ff">蓝色</Option>
            <Option value="#722ed1">紫色</Option>
          </Select>
        </Space>
      </div>
      <div
        ref={editorRef}
        contentEditable
        suppressContentEditableWarning
        onInput={(e) => onChange(e.target.innerHTML)}
        style={{
          minHeight: 200,
          padding: '12px',
          outline: 'none',
          lineHeight: 1.8
        }}
        placeholder="请输入解决方案内容，支持富文本格式..."
      />
    </div>
  )
}

const KnowledgeBaseList = () => {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [searchForm] = Form.useForm()
  const [editForm] = Form.useForm()

  const [detailVisible, setDetailVisible] = useState(false)
  const [editVisible, setEditVisible] = useState(false)
  const [currentRecord, setCurrentRecord] = useState(null)
  const [editMode, setEditMode] = useState('add')
  const [editLoading, setEditLoading] = useState(false)
  const [userFeedback, setUserFeedback] = useState(null)
  const [richTextValue, setRichTextValue] = useState('')
  const [attachments, setAttachments] = useState([])
  const [stats, setStats] = useState({ total: 0, published: 0, totalLikes: 0, totalUses: 0 })

  const currentUser = getUser() || {}

  const fetchStats = useCallback(async () => {
    try {
      const res = await getKnowledgePage({ pageNum: 1, pageSize: 1, status: 1 })
      const totalCount = res.data?.total || 0
      const list = res.data?.list || []
      setStats({
        total: totalCount,
        published: totalCount,
        totalLikes: list.reduce((s, i) => s + (i.likeCount || 0), 0),
        totalUses: list.reduce((s, i) => s + (i.useCount || 0), 0)
      })
    } catch (e) {
      // ignore
    }
  }, [])

  const fetchData = useCallback(async (page = 1, searchValues = null) => {
    setLoading(true)
    try {
      const params = { pageNum: page, pageSize: PAGE_SIZE, ...searchValues }
      const res = await getKnowledgePage(params)
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
    fetchData(1)
    fetchStats()
  }, [fetchData, fetchStats])

  const handleSearch = async () => {
    const values = await searchForm.validateFields()
    fetchData(1, values)
  }

  const handleReset = () => {
    searchForm.resetFields()
    fetchData(1)
  }

  const handlePageChange = (page) => {
    searchForm.validateFields().then(values => {
      fetchData(page, values)
    }).catch(() => {
      fetchData(page)
    })
  }

  const handleViewDetail = async (record) => {
    try {
      const res = await getKnowledgeDetail(record.id)
      setCurrentRecord(res.data || record)
      if (currentUser.id) {
        const fbRes = await getUserFeedback(record.id, currentUser.id)
        setUserFeedback(fbRes.data || null)
      }
      setDetailVisible(true)
    } catch {
      setCurrentRecord(record)
      setDetailVisible(true)
    }
  }

  const handleAdd = () => {
    setEditMode('add')
    setRichTextValue('')
    setAttachments([])
    editForm.resetFields()
    editForm.setFieldsValue({
      faultLevel: 2,
      status: 1,
      likeCount: 0,
      dislikeCount: 0,
      viewCount: 0,
      useCount: 0,
      creatorId: currentUser.id,
      creatorName: currentUser.name || currentUser.username || '系统管理员'
    })
    setEditVisible(true)
  }

  const handleEdit = async (record) => {
    setEditMode('edit')
    setCurrentRecord(record)
    try {
      const res = await getKnowledgeDetail(record.id)
      const detail = res.data || record
      editForm.setFieldsValue(detail)
      setRichTextValue(detail.solutionRichText || '')
      setAttachments(detail.attachments ? JSON.parse(detail.attachments) : [])
    } catch {
      editForm.setFieldsValue(record)
      setRichTextValue(record.solutionRichText || '')
      setAttachments(record.attachments ? JSON.parse(record.attachments) : [])
    }
    setEditVisible(true)
  }

  const handleDelete = (record) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定删除知识库「${record.faultName}」吗？此操作不可恢复。`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await deleteKnowledge(record.id)
          message.success('删除成功')
          fetchData(pageNum)
          fetchStats()
        } catch (e) {
          message.error(e.message || '删除失败')
        }
      }
    })
  }

  const handleEditOk = async () => {
    try {
      const values = await editForm.validateFields()
      setEditLoading(true)
      values.solutionRichText = richTextValue
      if (!values.solution && values.solutionRichText) {
        values.solution = values.solutionRichText.replace(/<[^>]*>/g, '').slice(0, 500)
      }
      values.attachments = JSON.stringify(attachments)

      if (editMode === 'add') {
        await addKnowledge(values)
        message.success('新增成功')
      } else {
        values.id = currentRecord.id
        await updateKnowledge(values)
        message.success('更新成功')
      }
      setEditVisible(false)
      fetchData(pageNum)
      fetchStats()
    } catch (error) {
      if (error.errorFields) return
      message.error(error.message || '操作失败')
    } finally {
      setEditLoading(false)
    }
  }

  const handleFeedback = async (feedbackType) => {
    if (!currentUser.id) {
      message.warning('请先登录')
      return
    }
    try {
      await submitKnowledgeFeedback({
        knowledgeId: currentRecord.id,
        userId: currentUser.id,
        userName: currentUser.name || currentUser.username,
        feedbackType
      })
      message.success('反馈已提交')
      const res = await getKnowledgeDetail(currentRecord.id)
      setCurrentRecord(res.data)
      const fbRes = await getUserFeedback(currentRecord.id, currentUser.id)
      setUserFeedback(fbRes.data || null)
    } catch (e) {
      message.error(e.message || '反馈失败')
    }
  }

  const handleUseKnowledge = async () => {
    if (!currentRecord) return
    try {
      await recordKnowledgeUsage({
        knowledgeId: currentRecord.id,
        userId: currentUser.id,
        userName: currentUser.name || currentUser.username,
        sourceType: 1
      })
      const res = await getKnowledgeDetail(currentRecord.id)
      setCurrentRecord(res.data)
      message.success('已记录使用')
    } catch (e) {
      // ignore
    }
  }

  const uploadProps = {
    fileList: attachments.map((url, idx) => ({
      uid: idx,
      name: url.split('/').pop(),
      status: 'done',
      url
    })),
    beforeUpload: (file) => {
      const reader = new FileReader()
      reader.onload = (e) => {
        setAttachments([...attachments, e.target.result])
      }
      reader.readAsDataURL(file)
      return false
    },
    onRemove: (file) => {
      setAttachments(attachments.filter((_, i) => i !== file.uid))
    }
  }

  const columns = [
    {
      title: '故障码',
      dataIndex: 'faultCode',
      key: 'faultCode',
      width: 140,
      render: (text, record) => (
        <a onClick={() => handleViewDetail(record)} style={{ fontFamily: 'monospace' }}>{text}</a>
      )
    },
    {
      title: '故障名称',
      dataIndex: 'faultName',
      key: 'faultName',
      width: 160,
      ellipsis: true
    },
    {
      title: '故障级别',
      dataIndex: 'faultLevel',
      key: 'faultLevel',
      width: 90,
      render: (level) => {
        const info = FAULT_LEVEL_MAP[level] || { color: 'default', text: level }
        return <Tag color={info.color}>{info.text}</Tag>
      }
    },
    {
      title: '类型',
      dataIndex: 'faultType',
      key: 'faultType',
      width: 100,
      render: (text) => text ? <Tag>{text}</Tag> : '-'
    },
    {
      title: '标签',
      dataIndex: 'tags',
      key: 'tags',
      width: 180,
      render: (text) => {
        if (!text) return '-'
        return text.split(',').slice(0, 3).map(tag => (
          <Tag key={tag} color="blue" style={{ marginBottom: 4 }}>{tag}</Tag>
        ))
      }
    },
    {
      title: '点赞',
      dataIndex: 'likeCount',
      key: 'likeCount',
      width: 70,
      render: (n) => <span style={{ color: '#52c41a' }}>👍 {n || 0}</span>
    },
    {
      title: '使用次数',
      dataIndex: 'useCount',
      key: 'useCount',
      width: 80,
      render: (n) => <span style={{ color: '#1890ff' }}>📋 {n || 0}</span>
    },
    {
      title: '浏览量',
      dataIndex: 'viewCount',
      key: 'viewCount',
      width: 75,
      render: (n) => <span style={{ color: '#722ed1' }}>👁 {n || 0}</span>
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (s) => {
        const info = STATUS_MAP[s] || { color: 'default', text: s }
        return <Tag color={info.color}>{info.text}</Tag>
      }
    },
    {
      title: '创建人',
      dataIndex: 'creatorName',
      key: 'creatorName',
      width: 100,
      render: (t) => t || '-'
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 170
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>
            查看
          </Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Button type="link" size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record)}>
            删除
          </Button>
        </Space>
      )
    }
  ]

  return (
    <div className="knowledge-base-page">
      <Card
        title={
          <Space>
            <span>📚 运维知识库</span>
            <Tag color="blue">智能推荐</Tag>
          </Space>
        }
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => {
              refreshKnowledgeCache().then(() => message.success('缓存刷新成功'))
              fetchData(pageNum)
            }}>刷新缓存</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增知识
            </Button>
          </Space>
        }
      >
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col xs={12} sm={6}>
            <Card size="small">
              <Statistic
                title="知识总量"
                value={stats.total}
                prefix={<FileTextOutlined />}
                valueStyle={{ fontSize: 20, color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6}>
            <Card size="small">
              <Statistic
                title="已发布"
                value={stats.published}
                valueStyle={{ fontSize: 20, color: '#52c41a' }}
                prefix={<CheckCircle />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6}>
            <Card size="small">
              <Statistic
                title="累计点赞"
                value={stats.totalLikes}
                valueStyle={{ fontSize: 20, color: '#faad14' }}
                prefix={<LikeOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6}>
            <Card size="small">
              <Statistic
                title="累计使用"
                value={stats.totalUses}
                valueStyle={{ fontSize: 20, color: '#722ed1' }}
                prefix={<BulbOutlined />}
              />
            </Card>
          </Col>
        </Row>

        <Card size="small" style={{ marginBottom: 16 }}>
          <Form form={searchForm} layout="inline" onFinish={handleSearch}>
            <Form.Item name="keyword" label="关键词">
              <Input placeholder="故障码/名称/描述/标签" style={{ width: 220 }} allowClear />
            </Form.Item>
            <Form.Item name="faultLevel" label="级别">
              <Select placeholder="全部" allowClear style={{ width: 120 }}>
                <Option value={1}>低级</Option>
                <Option value={2}>中级</Option>
                <Option value={3}>高级</Option>
                <Option value={4}>紧急</Option>
              </Select>
            </Form.Item>
            <Form.Item name="faultType" label="类型">
              <Select placeholder="全部" allowClear style={{ width: 140 }}>
                {FAULT_TYPES.map(t => <Option key={t} value={t}>{t}</Option>)}
              </Select>
            </Form.Item>
            <Form.Item name="status" label="状态">
              <Select placeholder="全部" allowClear style={{ width: 110 }}>
                <Option value={0}>草稿</Option>
                <Option value={1}>已发布</Option>
                <Option value={2}>已归档</Option>
              </Select>
            </Form.Item>
            <Form.Item>
              <Space>
                <Button type="primary" icon={<SearchOutlined />} htmlType="submit">搜索</Button>
                <Button onClick={handleReset}>重置</Button>
              </Space>
            </Form.Item>
          </Form>
        </Card>

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
        title="知识库详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailVisible(false)}>关闭</Button>,
          <Button key="use" type="primary" icon={<BulbOutlined />} onClick={handleUseKnowledge}>
            引用此方案
          </Button>
        ]}
        width={780}
      >
        {currentRecord && (
          <div>
            <Row gutter={12} style={{ marginBottom: 12 }}>
              <Col span={12}>
                <Space>
                  <Tag color="blue" style={{ fontFamily: 'monospace', fontSize: 14 }}>
                    {currentRecord.faultCode}
                  </Tag>
                  <Tag color={(FAULT_LEVEL_MAP[currentRecord.faultLevel] || {}).color}>
                    {(FAULT_LEVEL_MAP[currentRecord.faultLevel] || {}).text}
                  </Tag>
                  {currentRecord.faultType && <Tag>{currentRecord.faultType}</Tag>}
                </Space>
              </Col>
              <Col span={12} style={{ textAlign: 'right' }}>
                <Space>
                  <Tooltip title="点赞">
                    <Button
                      type={userFeedback?.feedbackType === 1 ? 'primary' : 'default'}
                      shape="circle"
                      icon={userFeedback?.feedbackType === 1 ? <LikeFilled /> : <LikeOutlined />}
                      onClick={() => handleFeedback(1)}
                    />
                    <span style={{ marginRight: 8, color: '#52c41a' }}>{currentRecord.likeCount || 0}</span>
                  </Tooltip>
                  <Tooltip title="点踩">
                    <Button
                      type={userFeedback?.feedbackType === 2 ? 'primary' : 'default'}
                      danger
                      shape="circle"
                      icon={userFeedback?.feedbackType === 2 ? <DislikeFilled /> : <DislikeOutlined />}
                      onClick={() => handleFeedback(2)}
                    />
                    <span style={{ color: '#ff4d4f' }}>{currentRecord.dislikeCount || 0}</span>
                  </Tooltip>
                </Space>
              </Col>
            </Row>

            <h3 style={{ marginBottom: 8 }}>{currentRecord.faultName}</h3>

            {currentRecord.tags && (
              <div style={{ marginBottom: 12 }}>
                {currentRecord.tags.split(',').map(tag => (
                  <Tag key={tag} color="geekblue">{tag}</Tag>
                ))}
              </div>
            )}

            <Divider orientation="left" style={{ margin: '12px 0' }}>故障描述</Divider>
            <p style={{ lineHeight: 1.8, whiteSpace: 'pre-wrap' }}>
              {currentRecord.faultDesc || '暂无描述'}
            </p>

            <Divider orientation="left" style={{ margin: '12px 0' }}>解决方案</Divider>
            {currentRecord.solutionRichText ? (
              <div
                style={{ lineHeight: 1.8 }}
                dangerouslySetInnerHTML={{ __html: currentRecord.solutionRichText }}
              />
            ) : (
              <p style={{ lineHeight: 1.8, whiteSpace: 'pre-wrap', color: '#666' }}>
                {currentRecord.solution || '暂无解决方案'}
              </p>
            )}

            {currentRecord.videoUrl && (
              <>
                <Divider orientation="left" style={{ margin: '12px 0' }}>视频教程</Divider>
                <div style={{ textAlign: 'center' }}>
                  <VideoCameraOutlined style={{ fontSize: 48, color: '#1890ff' }} />
                  <p><a href={currentRecord.videoUrl} target="_blank" rel="noreferrer">点击播放视频教程</a></p>
                </div>
              </>
            )}

            {currentRecord.attachments && JSON.parse(currentRecord.attachments)?.length > 0 && (
              <>
                <Divider orientation="left" style={{ margin: '12px 0' }}>相关附件</Divider>
                <Space wrap>
                  {JSON.parse(currentRecord.attachments).map((url, idx) => (
                    <Button key={idx} icon={<FileTextOutlined />} href={url} target="_blank">
                      附件 {idx + 1}
                    </Button>
                  ))}
                </Space>
              </>
            )}

            <Divider style={{ margin: '16px 0' }} />
            <Row style={{ fontSize: 12, color: '#999' }}>
              <Col span={6}>👁 浏览: {currentRecord.viewCount || 0}</Col>
              <Col span={6}>📋 使用: {currentRecord.useCount || 0}</Col>
              <Col span={6}>创建人: {currentRecord.creatorName || '-'}</Col>
              <Col span={6} style={{ textAlign: 'right' }}>更新: {currentRecord.updateTime}</Col>
            </Row>
          </div>
        )}
      </Modal>

      <Modal
        title={editMode === 'add' ? '新增知识库' : '编辑知识库'}
        open={editVisible}
        onOk={handleEditOk}
        onCancel={() => setEditVisible(false)}
        confirmLoading={editLoading}
        okText="提交"
        cancelText="取消"
        width={820}
        destroyOnClose
      >
        <Form form={editForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="faultCode"
                label="故障码"
                rules={[{ required: true, message: '请输入故障码' }]}
              >
                <Input placeholder="如: INV_NO_COMM" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="faultName"
                label="故障名称"
                rules={[{ required: true, message: '请输入故障名称' }]}
              >
                <Input placeholder="如: 逆变器通讯中断" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                name="faultLevel"
                label="故障级别"
                rules={[{ required: true, message: '请选择故障级别' }]}
              >
                <Select>
                  <Option value={1}>低级</Option>
                  <Option value={2}>中级</Option>
                  <Option value={3}>高级</Option>
                  <Option value={4}>紧急</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="faultType" label="故障类型">
                <Select placeholder="请选择类型" allowClear>
                  {FAULT_TYPES.map(t => <Option key={t} value={t}>{t}</Option>)}
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="status" label="状态">
                <Select>
                  <Option value={0}>草稿</Option>
                  <Option value={1}>已发布</Option>
                  <Option value={2}>已归档</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="tags" label="标签（多个用逗号分隔）">
            <Input placeholder="如: 通讯,4G,网络,离线,重启" />
          </Form.Item>
          <Form.Item
            name="faultDesc"
            label="故障描述"
            rules={[{ required: true, message: '请输入故障描述' }]}
          >
            <TextArea rows={3} placeholder="请详细描述故障现象" />
          </Form.Item>
          <Form.Item label="纯文本解决方案摘要">
            <Form.Item name="solution" noStyle>
              <TextArea rows={2} placeholder="简短描述解决方案（用于列表展示），可留空自动从富文本提取" />
            </Form.Item>
          </Form.Item>
          <Form.Item
            label="富文本解决方案"
            required
            tooltip="支持格式化文字、列表、标题等"
          >
            <RichTextEditor value={richTextValue} onChange={setRichTextValue} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="videoUrl" label="视频教程URL">
                <Input placeholder="请输入视频地址，支持mp4等" prefix={<VideoCameraOutlined />} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="附件上传">
                <Upload {...uploadProps}>
                  <Button icon={<UploadOutlined />}>上传附件（图片/PDF/文档等）</Button>
                </Upload>
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="creatorId" hidden>
            <InputNumber />
          </Form.Item>
          <Form.Item name="creatorName" hidden>
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

const CheckCircle = () => <span>✅</span>

export default KnowledgeBaseList
