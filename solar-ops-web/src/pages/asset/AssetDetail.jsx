import React, { useState, useEffect } from 'react'
import {
  Card,
  Descriptions,
  Tag,
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  DatePicker,
  InputNumber,
  message,
  Row,
  Col,
  Tabs,
  Image,
  Upload,
  List
} from 'antd'
import {
  ArrowLeftOutlined,
  PlusOutlined,
  QrcodeOutlined,
  UploadOutlined,
  WrenchOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import dayjs from 'dayjs'
import {
  getAssetDetail,
  getAssetQrCode
} from '../../api/asset'
import {
  getMaintenanceListByAssetId,
  getMaintenanceDetail,
  addMaintenanceWithSpareParts,
  uploadFile
} from '../../api/maintenance'

const { TextArea } = Input
const { Option } = Select

const ASSET_TYPE_MAP = {
  station: { color: 'blue', text: '电站' },
  inverter: { color: 'green', text: '逆变器' },
  combiner: { color: 'cyan', text: '汇流箱' },
  panel: { color: 'geekblue', text: '光伏组件' },
  transformer: { color: 'purple', text: '变压器' },
  other: { color: 'default', text: '其他' }
}

const ASSET_STATUS_MAP = {
  1: { color: 'success', text: '正常' },
  2: { color: 'processing', text: '运维中' },
  3: { color: 'warning', text: '已退役' },
  4: { color: 'error', text: '已报废' }
}

const WARRANTY_STATUS_MAP = {
  1: { color: 'success', text: '正常' },
  2: { color: 'warning', text: '即将到期' },
  3: { color: 'error', text: '已过期' }
}

const MAINTENANCE_TYPE_MAP = {
  1: { color: 'blue', text: '日常维护' },
  2: { color: 'red', text: '故障维修' },
  3: { color: 'cyan', text: '定期巡检' },
  4: { color: 'orange', text: '备件更换' }
}

const AssetDetail = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const [asset, setAsset] = useState(null)
  const [maintenanceList, setMaintenanceList] = useState([])
  const [loading, setLoading] = useState(false)
  const [maintenanceLoading, setMaintenanceLoading] = useState(false)
  const [qrModalVisible, setQrModalVisible] = useState(false)
  const [addModalVisible, setAddModalVisible] = useState(false)
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [currentQrCode, setCurrentQrCode] = useState('')
  const [currentMaintenance, setCurrentMaintenance] = useState(null)
  const [addForm] = Form.useForm()
  const [spareParts, setSpareParts] = useState([])
  const [photoUrls, setPhotoUrls] = useState([])

  const fetchAssetDetail = async () => {
    setLoading(true)
    try {
      const res = await getAssetDetail(id)
      setAsset(res.data)
    } catch (e) {
      message.error('获取资产详情失败')
    } finally {
      setLoading(false)
    }
  }

  const fetchMaintenanceList = async () => {
    setMaintenanceLoading(true)
    try {
      const res = await getMaintenanceListByAssetId(id)
      setMaintenanceList(res.data || [])
    } catch (e) {
      setMaintenanceList([])
    } finally {
      setMaintenanceLoading(false)
    }
  }

  useEffect(() => {
    if (id) {
      fetchAssetDetail()
      fetchMaintenanceList()
    }
  }, [id])

  const handleShowQrCode = async () => {
    try {
      const res = await getAssetQrCode(id)
      setCurrentQrCode(res.data)
      setQrModalVisible(true)
    } catch (e) {
      message.error('获取二维码失败')
    }
  }

  const handleAddMaintenance = () => {
    addForm.resetFields()
    setSpareParts([])
    setPhotoUrls([])
    setAddModalVisible(true)
  }

  const handleViewMaintenanceDetail = async (record) => {
    try {
      const res = await getMaintenanceDetail(record.id)
      setCurrentMaintenance(res.data || record)
      setDetailModalVisible(true)
    } catch (e) {
      setCurrentMaintenance(record)
      setDetailModalVisible(true)
    }
  }

  const handleAddSparePart = () => {
    setSpareParts([...spareParts, { key: Date.now() }])
  }

  const handleRemoveSparePart = (key) => {
    setSpareParts(spareParts.filter(item => item.key !== key))
  }

  const handleSparePartChange = (key, field, value) => {
    setSpareParts(spareParts.map(item =>
      item.key === key ? { ...item, [field]: value } : item
    ))
  }

  const handlePhotoUpload = async (file) => {
    try {
      const res = await uploadFile(file)
      if (res.data && res.data.url) {
        setPhotoUrls([...photoUrls, res.data.url])
      }
    } catch (e) {
      message.error('照片上传失败')
    }
    return false
  }

  const handleRemovePhoto = (index) => {
    setPhotoUrls(photoUrls.filter((_, i) => i !== index))
  }

  const handleAddOk = async () => {
    try {
      const values = await addForm.validateFields()

      const validSpareParts = spareParts
        .filter(part => part.partName && part.partCode)
        .map(part => ({
          partCode: part.partCode,
          partName: part.partName,
          partModel: part.partModel,
          brand: part.brand,
          specification: part.specification,
          quantity: part.quantity || 1,
          unitPrice: part.unitPrice,
          totalPrice: part.unitPrice && part.quantity ?
            new Number(part.unitPrice * part.quantity).toFixed(2) : null,
          supplier: part.supplier,
          operator: part.operator,
          remark: part.remark
        }))

      const data = {
        assetId: Number(id),
        faultDescription: values.faultDescription,
        faultType: values.faultType,
        maintenanceType: values.maintenanceType,
        maintenanceTime: values.maintenanceTime ? values.maintenanceTime.format('YYYY-MM-DD HH:mm:ss') : null,
        maintenancePerson: values.maintenancePerson,
        maintenanceContent: values.maintenanceContent,
        solution: values.solution,
        cost: values.cost,
        remark: values.remark,
        photos: JSON.stringify(photoUrls),
        spareParts: validSpareParts
      }

      await addMaintenanceWithSpareParts(data)
      message.success('维修记录添加成功')
      setAddModalVisible(false)
      fetchMaintenanceList()
      fetchAssetDetail()
    } catch (error) {
      if (error.errorFields) return
      message.error(error.message || '添加失败')
    }
  }

  const renderPhotos = (photos) => {
    if (!photos) return null
    let photoList = []
    try {
      photoList = JSON.parse(photos)
    } catch (e) {
      photoList = [photos]
    }
    if (!Array.isArray(photoList) || photoList.length === 0) return null

    return (
      <Image.PreviewGroup>
        <Row gutter={8}>
          {photoList.map((url, index) => (
            <Col key={index} span={6}>
              <Image
                width="100%"
                height={80}
                src={url}
                style={{ objectFit: 'cover', borderRadius: 4 }}
              />
            </Col>
          ))}
        </Row>
      </Image.PreviewGroup>
    )
  }

  const maintenanceColumns = [
    {
      title: '维修记录编号',
      dataIndex: 'recordNo',
      key: 'recordNo',
      width: 160,
      render: (text, record) => (
        <a onClick={() => handleViewMaintenanceDetail(record)}>{text}</a>
      )
    },
    {
      title: '维修类型',
      dataIndex: 'maintenanceType',
      key: 'maintenanceType',
      width: 100,
      render: (type) => {
        const info = MAINTENANCE_TYPE_MAP[type] || { color: 'default', text: type }
        return <Tag color={info.color}>{info.text}</Tag>
      }
    },
    {
      title: '故障描述',
      dataIndex: 'faultDescription',
      key: 'faultDescription',
      width: 200,
      ellipsis: true
    },
    {
      title: '维修内容',
      dataIndex: 'maintenanceContent',
      key: 'maintenanceContent',
      width: 200,
      ellipsis: true
    },
    {
      title: '维修人员',
      dataIndex: 'maintenancePerson',
      key: 'maintenancePerson',
      width: 100
    },
    {
      title: '维修时间',
      dataIndex: 'maintenanceTime',
      key: 'maintenanceTime',
      width: 160
    },
    {
      title: '维修费用',
      dataIndex: 'cost',
      key: 'cost',
      width: 100,
      render: (val) => val ? `¥${val}` : '-'
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 160
    }
  ]

  const tabItems = [
    {
      key: 'info',
      label: '基本信息'
    },
    {
      key: 'maintenance',
      label: `维修记录 (${maintenanceList.length})`
    }
  ]

  return (
    <div className="asset-detail-page">
      <Card
        loading={loading}
        title={
          <Space>
            <Button
              type="link"
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate('/asset/list')}
            >
              返回
            </Button>
            <span>{asset?.assetName || '资产详情'}</span>
          </Space>
        }
        extra={
          <Space>
            <Button icon={<QrcodeOutlined />} onClick={handleShowQrCode}>
              查看二维码
            </Button>
            <Button
              type="primary"
              icon={<WrenchOutlined />}
              onClick={handleAddMaintenance}
              disabled={asset?.assetStatus === 3 || asset?.assetStatus === 4}
            >
              添加维修记录
            </Button>
          </Space>
        }
      >
        {asset && (
          <Tabs defaultActiveKey="info" items={tabItems}>
            <Tabs.TabPane tab="基本信息" key="info">
              <Descriptions title="资产信息" bordered column={2} size="middle">
                <Descriptions.Item label="资产编号">{asset.assetCode}</Descriptions.Item>
                <Descriptions.Item label="资产名称">{asset.assetName}</Descriptions.Item>
                <Descriptions.Item label="资产类型">
                  <Tag color={(ASSET_TYPE_MAP[asset.assetType] || {}).color}>
                    {(ASSET_TYPE_MAP[asset.assetType] || {}).text || asset.assetType}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="所属电站">{asset.stationName || '-'}</Descriptions.Item>
                <Descriptions.Item label="设备序列号">{asset.deviceSn || '-'}</Descriptions.Item>
                <Descriptions.Item label="设备型号">{asset.deviceModel || '-'}</Descriptions.Item>
                <Descriptions.Item label="品牌">{asset.brand || '-'}</Descriptions.Item>
                <Descriptions.Item label="规格参数">{asset.specification || '-'}</Descriptions.Item>
                <Descriptions.Item label="容量(kW)">{asset.capacity || '-'}</Descriptions.Item>
                <Descriptions.Item label="安装日期">{asset.installDate || '-'}</Descriptions.Item>
                <Descriptions.Item label="质保开始日期">{asset.warrantyStartDate || '-'}</Descriptions.Item>
                <Descriptions.Item label="质保到期日期">
                  <Space direction="vertical" size={0}>
                    <span>{asset.warrantyEndDate || '-'}</span>
                    {asset.warrantyStatus && (
                      <Tag color={(WARRANTY_STATUS_MAP[asset.warrantyStatus] || {}).color}>
                        {asset.warrantyDaysLeft !== undefined ?
                          `剩余${asset.warrantyDaysLeft}天` :
                          (WARRANTY_STATUS_MAP[asset.warrantyStatus] || {}).text}
                      </Tag>
                    )}
                  </Space>
                </Descriptions.Item>
                <Descriptions.Item label="质保期限(月)">{asset.warrantyMonths || '-'}</Descriptions.Item>
                <Descriptions.Item label="供应商">{asset.supplier || '-'}</Descriptions.Item>
                <Descriptions.Item label="生产厂家">{asset.manufacturer || '-'}</Descriptions.Item>
                <Descriptions.Item label="安装位置">{asset.installLocation || '-'}</Descriptions.Item>
                <Descriptions.Item label="采购金额">{asset.purchaseAmount ? `¥${asset.purchaseAmount}` : '-'}</Descriptions.Item>
                <Descriptions.Item label="责任人">{asset.responsiblePerson || '-'}</Descriptions.Item>
                <Descriptions.Item label="资产状态">
                  <Tag color={(ASSET_STATUS_MAP[asset.assetStatus] || {}).color}>
                    {(ASSET_STATUS_MAP[asset.assetStatus] || {}).text || asset.assetStatus}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="经度">{asset.longitude || '-'}</Descriptions.Item>
                <Descriptions.Item label="纬度">{asset.latitude || '-'}</Descriptions.Item>
                <Descriptions.Item label="备注" span={2}>{asset.remark || '-'}</Descriptions.Item>
                <Descriptions.Item label="创建时间">{asset.createTime || '-'}</Descriptions.Item>
                <Descriptions.Item label="更新时间">{asset.updateTime || '-'}</Descriptions.Item>
              </Descriptions>
            </Tabs.TabPane>

            <Tabs.TabPane tab={`维修记录 (${maintenanceList.length})`} key="maintenance">
              <Table
                columns={maintenanceColumns}
                dataSource={maintenanceList}
                rowKey="id"
                loading={maintenanceLoading}
                pagination={{ pageSize: 10 }}
                scroll={{ x: 1100 }}
                locale={{ emptyText: '暂无维修记录' }}
              />
            </Tabs.TabPane>
          </Tabs>
        )}
      </Card>

      <Modal
        title="资产二维码"
        open={qrModalVisible}
        onCancel={() => setQrModalVisible(false)}
        footer={null}
        width={400}
      >
        <div style={{ textAlign: 'center', padding: '20px 0' }}>
          <Image
            width={280}
            height={280}
            src={currentQrCode}
            preview={false}
          />
          <p style={{ marginTop: 16, color: '#666' }}>
            使用运维APP扫描二维码查看设备详情和维修记录
          </p>
        </div>
      </Modal>

      <Modal
        title="添加维修记录"
        open={addModalVisible}
        onOk={handleAddOk}
        onCancel={() => setAddModalVisible(false)}
        okText="提交"
        cancelText="取消"
        width={700}
        destroyOnClose
      >
        <Form form={addForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="maintenanceType"
                label="维修类型"
                rules={[{ required: true, message: '请选择维修类型' }]}
              >
                <Select placeholder="请选择维修类型">
                  <Option value={1}>日常维护</Option>
                  <Option value={2}>故障维修</Option>
                  <Option value={3}>定期巡检</Option>
                  <Option value={4}>备件更换</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="maintenanceTime"
                label="维修时间"
                rules={[{ required: true, message: '请选择维修时间' }]}
              >
                <DatePicker
                  showTime
                  style={{ width: '100%' }}
                  format="YYYY-MM-DD HH:mm:ss"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="maintenancePerson"
                label="维修人员"
                rules={[{ required: true, message: '请输入维修人员' }]}
              >
                <Input placeholder="请输入维修人员姓名" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="cost" label="维修费用">
                <InputNumber
                  style={{ width: '100%' }}
                  min={0}
                  placeholder="请输入维修费用"
                  prefix="¥"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="faultType" label="故障类型">
                <Input placeholder="请输入故障类型" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="faultDescription" label="故障描述">
                <TextArea rows={2} placeholder="请输入故障描述" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="maintenanceContent" label="维修内容">
                <TextArea rows={3} placeholder="请详细描述维修内容" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="solution" label="解决方案">
                <TextArea rows={3} placeholder="请输入解决方案" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item label="维修照片">
                <div>
                  <Upload
                    showUploadList={false}
                    beforeUpload={handlePhotoUpload}
                    accept="image/*"
                    multiple
                  >
                    <Button icon={<UploadOutlined />}>上传照片</Button>
                  </Upload>
                  {photoUrls.length > 0 && (
                    <Row gutter={8} style={{ marginTop: 12 }}>
                      {photoUrls.map((url, index) => (
                        <Col key={index} span={6}>
                          <div style={{ position: 'relative' }}>
                            <Image
                              width="100%"
                              height={80}
                              src={url}
                              style={{ objectFit: 'cover', borderRadius: 4 }}
                            />
                            <Button
                              type="text"
                              danger
                              size="small"
                              style={{
                                position: 'absolute',
                                top: 0,
                                right: 0,
                                padding: 0,
                                width: 20,
                                height: 20
                              }}
                              onClick={() => handleRemovePhoto(index)}
                            >
                              ×
                            </Button>
                          </div>
                        </Col>
                      ))}
                    </Row>
                  )}
                </div>
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item label="更换备件">
                <div>
                  <Button
                    type="dashed"
                    style={{ width: '100%', marginBottom: 12 }}
                    onClick={handleAddSparePart}
                    icon={<PlusOutlined />}
                  >
                    添加备件
                  </Button>
                  {spareParts.map(part => (
                    <Card
                      key={part.key}
                      size="small"
                      style={{ marginBottom: 12 }}
                      extra={
                        <Button
                          type="text"
                          danger
                          size="small"
                          onClick={() => handleRemoveSparePart(part.key)}
                        >
                          删除
                        </Button>
                      }
                    >
                      <Row gutter={8}>
                        <Col span={6}>
                          <Input
                            placeholder="备件编号"
                            value={part.partCode}
                            onChange={(e) => handleSparePartChange(part.key, 'partCode', e.target.value)}
                          />
                        </Col>
                        <Col span={6}>
                          <Input
                            placeholder="备件名称"
                            value={part.partName}
                            onChange={(e) => handleSparePartChange(part.key, 'partName', e.target.value)}
                          />
                        </Col>
                        <Col span={4}>
                          <Input
                            placeholder="型号"
                            value={part.partModel}
                            onChange={(e) => handleSparePartChange(part.key, 'partModel', e.target.value)}
                          />
                        </Col>
                        <Col span={4}>
                          <Input
                            placeholder="品牌"
                            value={part.brand}
                            onChange={(e) => handleSparePartChange(part.key, 'brand', e.target.value)}
                          />
                        </Col>
                        <Col span={4}>
                          <InputNumber
                            placeholder="数量"
                            min={1}
                            value={part.quantity}
                            onChange={(value) => handleSparePartChange(part.key, 'quantity', value)}
                            style={{ width: '100%' }}
                          />
                        </Col>
                        <Col span={4}>
                          <InputNumber
                            placeholder="单价"
                            min={0}
                            value={part.unitPrice}
                            onChange={(value) => handleSparePartChange(part.key, 'unitPrice', value)}
                            style={{ width: '100%' }}
                            prefix="¥"
                          />
                        </Col>
                      </Row>
                    </Card>
                  ))}
                </div>
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="remark" label="备注">
                <TextArea rows={2} placeholder="请输入备注" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title="维修记录详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={null}
        width={700}
      >
        {currentMaintenance && (
          <div>
            <Descriptions title="基本信息" bordered column={2} size="small">
              <Descriptions.Item label="记录编号">{currentMaintenance.recordNo}</Descriptions.Item>
              <Descriptions.Item label="维修类型">
                <Tag color={(MAINTENANCE_TYPE_MAP[currentMaintenance.maintenanceType] || {}).color}>
                  {(MAINTENANCE_TYPE_MAP[currentMaintenance.maintenanceType] || {}).text}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="故障类型">{currentMaintenance.faultType || '-'}</Descriptions.Item>
              <Descriptions.Item label="维修人员">{currentMaintenance.maintenancePerson || '-'}</Descriptions.Item>
              <Descriptions.Item label="维修时间">{currentMaintenance.maintenanceTime || '-'}</Descriptions.Item>
              <Descriptions.Item label="维修费用">{currentMaintenance.cost ? `¥${currentMaintenance.cost}` : '-'}</Descriptions.Item>
              <Descriptions.Item label="故障描述" span={2}>{currentMaintenance.faultDescription || '-'}</Descriptions.Item>
              <Descriptions.Item label="维修内容" span={2}>{currentMaintenance.maintenanceContent || '-'}</Descriptions.Item>
              <Descriptions.Item label="解决方案" span={2}>{currentMaintenance.solution || '-'}</Descriptions.Item>
              <Descriptions.Item label="备注" span={2}>{currentMaintenance.remark || '-'}</Descriptions.Item>
            </Descriptions>

            {currentMaintenance.photos && (
              <div style={{ marginTop: 16 }}>
                <h4>维修照片</h4>
                {renderPhotos(currentMaintenance.photos)}
              </div>
            )}

            {currentMaintenance.spareParts && currentMaintenance.spareParts.length > 0 && (
              <div style={{ marginTop: 16 }}>
                <h4>更换备件</h4>
                <Table
                  size="small"
                  dataSource={currentMaintenance.spareParts}
                  rowKey="id"
                  pagination={false}
                  columns={[
                    { title: '备件编号', dataIndex: 'partCode', key: 'partCode' },
                    { title: '备件名称', dataIndex: 'partName', key: 'partName' },
                    { title: '型号', dataIndex: 'partModel', key: 'partModel' },
                    { title: '品牌', dataIndex: 'brand', key: 'brand' },
                    { title: '数量', dataIndex: 'quantity', key: 'quantity' },
                    { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', render: (v) => v ? `¥${v}` : '-' },
                    { title: '总价', dataIndex: 'totalPrice', key: 'totalPrice', render: (v) => v ? `¥${v}` : '-' }
                  ]}
                />
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  )
}

export default AssetDetail
