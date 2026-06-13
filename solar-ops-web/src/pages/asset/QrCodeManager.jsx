import React, { useState, useRef } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  Select,
  message,
  Image,
  Row,
  Col,
  Checkbox,
  Tag,
  Modal
} from 'antd'
import {
  PrinterOutlined,
  QrcodeOutlined,
  DownloadOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import {
  getAssetList,
  getAssetQrCode,
  batchGenerateQrCode,
  getStationListAll
} from '../../api/asset'

const { Option } = Select

const ASSET_TYPE_MAP = {
  station: { color: 'blue', text: '电站' },
  inverter: { color: 'green', text: '逆变器' },
  combiner: { color: 'cyan', text: '汇流箱' },
  panel: { color: 'geekblue', text: '光伏组件' },
  transformer: { color: 'purple', text: '变压器' },
  other: { color: 'default', text: '其他' }
}

const QrCodeManager = () => {
  const actionRef = useRef()
  const [stationList, setStationList] = useState([])
  const [previewVisible, setPreviewVisible] = useState(false)
  const [batchQrCodes, setBatchQrCodes] = useState([])
  const [selectedRowKeys, setSelectedRowKeys] = useState([])
  const [loading, setLoading] = useState(false)

  React.useEffect(() => {
    fetchStationList()
  }, [])

  const fetchStationList = async () => {
    try {
      const res = await getStationListAll()
      setStationList(res.data || [])
    } catch (e) {
      console.error('获取电站列表失败', e)
    }
  }

  const columns = [
    {
      title: '资产编号',
      dataIndex: 'assetCode',
      key: 'assetCode',
      width: 140,
      fixed: 'left'
    },
    {
      title: '资产名称',
      dataIndex: 'assetName',
      key: 'assetName',
      width: 160
    },
    {
      title: '资产类型',
      dataIndex: 'assetType',
      key: 'assetType',
      width: 100,
      render: (type) => {
        const info = ASSET_TYPE_MAP[type] || { color: 'default', text: type }
        return <Tag color={info.color}>{info.text}</Tag>
      }
    },
    {
      title: '所属电站',
      dataIndex: 'stationName',
      key: 'stationName',
      width: 140
    },
    {
      title: '设备型号',
      dataIndex: 'deviceModel',
      key: 'deviceModel',
      width: 140
    },
    {
      title: '责任人',
      dataIndex: 'responsiblePerson',
      key: 'responsiblePerson',
      width: 100
    },
    {
      title: '二维码',
      dataIndex: 'qrCodeUrl',
      key: 'qrCodeUrl',
      width: 100,
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          icon={<QrcodeOutlined />}
          onClick={() => handlePreviewSingle(record.id)}
        >
          查看
        </Button>
      )
    }
  ]

  const handlePreviewSingle = async (id) => {
    try {
      const res = await getAssetQrCode(id)
      setBatchQrCodes([{ id, qrCode: res.data }])
      setPreviewVisible(true)
    } catch (e) {
      message.error('获取二维码失败')
    }
  }

  const handleBatchGenerate = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要生成二维码的资产')
      return
    }

    setLoading(true)
    try {
      const res = await batchGenerateQrCode(selectedRowKeys)
      const qrCodes = (res.data || []).map((qrCode, index) => ({
        id: selectedRowKeys[index],
        qrCode
      }))
      setBatchQrCodes(qrCodes)
      setPreviewVisible(true)
      message.success(`成功生成 ${qrCodes.length} 个二维码`)
    } catch (e) {
      message.error('批量生成二维码失败')
    } finally {
      setLoading(false)
    }
  }

  const handlePrint = () => {
    const printWindow = window.open('', '_blank')
    printWindow.document.write(`
      <html>
        <head>
          <title>二维码打印</title>
          <style>
            @page {
              size: A4;
              margin: 10mm;
            }
            body {
              font-family: Arial, sans-serif;
            }
            .qr-grid {
              display: grid;
              grid-template-columns: repeat(4, 1fr);
              gap: 15px;
            }
            .qr-item {
              text-align: center;
              padding: 10px;
              border: 1px solid #ddd;
              border-radius: 8px;
            }
            .qr-item img {
              width: 120px;
              height: 120px;
            }
            .qr-info {
              margin-top: 8px;
              font-size: 12px;
            }
            .qr-code {
              font-weight: bold;
              color: #1890ff;
            }
          </style>
        </head>
        <body>
          <div class="qr-grid">
            ${batchQrCodes.map(item => `
              <div class="qr-item">
                <img src="${item.qrCode}" />
                <div class="qr-info">
                  <div class="qr-code">${item.assetCode || item.assetName || '资产二维码'}</div>
                </div>
              </div>
            `).join('')}
          </div>
          <script>
            window.onload = function() {
              window.print();
              window.onafterprint = function() {
                window.close();
              };
            };
          </script>
        </body>
      </html>
    `)
    printWindow.document.close()
  }

  const handleDownloadAll = () => {
    batchQrCodes.forEach((item, index) => {
      const link = document.createElement('a')
      link.href = item.qrCode
      link.download = `二维码_${index + 1}.png`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    })
    message.success('已开始批量下载')
  }

  const request = async (params = {}) => {
    try {
      const queryParams = {
        pageNum: params.current,
        pageSize: params.pageSize,
        ...params
      }
      const res = await getAssetList(queryParams)
      const pageResult = res.data || {}
      return {
        data: pageResult.list || [],
        success: true,
        total: pageResult.total || 0
      }
    } catch (e) {
      return {
        data: [],
        success: false,
        total: 0
      }
    }
  }

  const rowSelection = {
    selectedRowKeys,
    onChange: (keys) => setSelectedRowKeys(keys)
  }

  const toolBarRender = () => [
    <Button
      key="batch"
      type="primary"
      icon={<QrcodeOutlined />}
      onClick={handleBatchGenerate}
      loading={loading}
      disabled={selectedRowKeys.length === 0}
    >
      批量生成二维码
    </Button>,
    selectedRowKeys.length > 0 && (
      <span key="count" style={{ color: '#666' }}>
        已选择 {selectedRowKeys.length} 项
      </span>
    )
  ]

  return (
    <div className="qrcode-manager-page">
      <Card title="二维码管理">
        <ProTable
          actionRef={actionRef}
          columns={columns}
          request={request}
          rowKey="id"
          search={{
            labelWidth: 'auto',
            defaultCollapsed: false,
            searchText: '查询',
            resetText: '重置'
          }}
          toolBarRender={toolBarRender}
          rowSelection={rowSelection}
          pagination={{
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`
          }}
          scroll={{ x: 1000 }}
        />
      </Card>

      <Modal
        title="二维码预览"
        open={previewVisible}
        onCancel={() => setPreviewVisible(false)}
        width={800}
        footer={[
          <Button key="back" onClick={() => setPreviewVisible(false)}>
            关闭
          </Button>,
          <Button
            key="download"
            icon={<DownloadOutlined />}
            onClick={handleDownloadAll}
          >
            批量下载
          </Button>,
          <Button
            key="print"
            type="primary"
            icon={<PrinterOutlined />}
            onClick={handlePrint}
          >
            打印
          </Button>
        ]}
      >
        <Row gutter={[16, 16]}>
          {batchQrCodes.map((item, index) => (
            <Col key={item.id || index} xs={12} sm={8} md={6}>
              <div style={{
                textAlign: 'center',
                padding: '16px',
                border: '1px solid #e8e8e8',
                borderRadius: '8px',
                background: '#fff'
              }}>
                <Image
                  width={150}
                  height={150}
                  src={item.qrCode}
                  preview={false}
                />
              </div>
            </Col>
          ))}
        </Row>
      </Modal>
    </div>
  )
}

export default QrCodeManager
