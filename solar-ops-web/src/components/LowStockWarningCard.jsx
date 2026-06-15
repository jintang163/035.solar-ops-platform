import React from 'react'
import { Card, Statistic, Row, Col, Tag, Progress, List, Typography } from 'antd'
import {
  WarningOutlined,
  ExclamationCircleOutlined,
  InboxOutlined,
  ArrowDownOutlined,
  ArrowUpOutlined,
  ShoppingCartOutlined
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'

const { Text, Link } = Typography

const LowStockWarningCard = ({ dashboardData, loading }) => {
  const navigate = useNavigate()

  const handleCardClick = (path) => {
    navigate(path)
  }

  if (!dashboardData) {
    return <Card loading={true} />
  }

  const { totalSkuCount, totalQuantity, totalAmount, lowWarnCount, insufficientCount,
    pendingSuggestionCount, todayInboundCount, todayOutboundCount, typeStats, warnParts } = dashboardData

  const stockHealthRate = totalQuantity > 0
    ? Math.round(((totalQuantity - lowWarnCount - insufficientCount) / totalQuantity) * 100)
    : 100

  const getWarnColor = (warnStatus) => {
    switch (warnStatus) {
      case 1: return '#faad14'
      case 2: return '#ff4d4f'
      default: return '#52c41a'
    }
  }

  const getWarnIcon = (warnStatus) => {
    switch (warnStatus) {
      case 1: return <WarningOutlined />
      case 2: return <ExclamationCircleOutlined />
      default: return null
    }
  }

  return (
    <div className="low-stock-warning-cards">
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={6}>
          <Card
            hoverable
            onClick={() => handleCardClick('/spare-parts')}
            className="stat-card"
          >
            <div className="stat-card-content">
              <div className="stat-card-info">
                <div className="stat-card-title">备件总数</div>
                <Statistic
                  value={totalSkuCount}
                  suffix="种"
                  valueStyle={{ color: '#1890ff', fontSize: '28px', fontWeight: 600 }}
                />
                <div className="stat-card-sub">
                  总库存: {totalQuantity} 件
                </div>
              </div>
              <div className="stat-card-icon" style={{ backgroundColor: '#e6f7ff', color: '#1890ff' }}>
                <InboxOutlined style={{ fontSize: '32px' }} />
              </div>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card
            hoverable
            onClick={() => handleCardClick('/spare-parts?warnStatus=1')}
            className="stat-card warning-card"
          >
            <div className="stat-card-content">
              <div className="stat-card-info">
                <div className="stat-card-title">低库存预警</div>
                <Statistic
                  value={lowWarnCount}
                  suffix="种"
                  valueStyle={{ color: '#faad14', fontSize: '28px', fontWeight: 600 }}
                  prefix={<WarningOutlined />}
                />
                <div className="stat-card-sub" style={{ color: '#faad14' }}>
                  需及时采购
                </div>
              </div>
              <div className="stat-card-icon" style={{ backgroundColor: '#fffbe6', color: '#faad14' }}>
                <WarningOutlined style={{ fontSize: '32px' }} />
              </div>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card
            hoverable
            onClick={() => handleCardClick('/spare-parts?warnStatus=2')}
            className="stat-card danger-card"
          >
            <div className="stat-card-content">
              <div className="stat-card-info">
                <div className="stat-card-title">库存不足</div>
                <Statistic
                  value={insufficientCount}
                  suffix="种"
                  valueStyle={{ color: '#ff4d4f', fontSize: '28px', fontWeight: 600 }}
                  prefix={<ExclamationCircleOutlined />}
                />
                <div className="stat-card-sub" style={{ color: '#ff4d4f' }}>
                  紧急！急需补货
                </div>
              </div>
              <div className="stat-card-icon" style={{ backgroundColor: '#fff1f0', color: '#ff4d4f' }}>
                <ExclamationCircleOutlined style={{ fontSize: '32px' }} />
              </div>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card
            hoverable
            onClick={() => handleCardClick('/purchase-suggestions')}
            className="stat-card"
          >
            <div className="stat-card-content">
              <div className="stat-card-info">
                <div className="stat-card-title">待处理采购建议</div>
                <Statistic
                  value={pendingSuggestionCount}
                  suffix="条"
                  valueStyle={{ color: '#722ed1', fontSize: '28px', fontWeight: 600 }}
                  prefix={<ShoppingCartOutlined />}
                />
                <div className="stat-card-sub">
                  库存总金额: ¥{totalAmount?.toLocaleString()}
                </div>
              </div>
              <div className="stat-card-icon" style={{ backgroundColor: '#f9f0ff', color: '#722ed1' }}>
                <ShoppingCartOutlined style={{ fontSize: '32px' }} />
              </div>
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: '16px' }}>
        <Col xs={24} lg={12}>
          <Card
            title="今日出入库统计"
            size="small"
            className="stats-card"
            extra={
              <Link onClick={() => handleCardClick('/spare-part-records')}>查看明细 &gt;</Link>
            }
          >
            <Row gutter={16}>
              <Col span={12}>
                <div className="trend-item">
                  <div className="trend-icon" style={{ backgroundColor: '#f6ffed', color: '#52c41a' }}>
                    <ArrowDownOutlined />
                  </div>
                  <div>
                    <div className="trend-title">今日入库</div>
                    <div className="trend-value" style={{ color: '#52c41a' }}>
                      {todayInboundCount} 件
                    </div>
                  </div>
                </div>
              </Col>
              <Col span={12}>
                <div className="trend-item">
                  <div className="trend-icon" style={{ backgroundColor: '#fff1f0', color: '#ff4d4f' }}>
                    <ArrowUpOutlined />
                  </div>
                  <div>
                    <div className="trend-title">今日出库</div>
                    <div className="trend-value" style={{ color: '#ff4d4f' }}>
                      {todayOutboundCount} 件
                    </div>
                  </div>
                </div>
              </Col>
            </Row>
            {typeStats && (
              <div style={{ marginTop: '16px' }}>
                <div className="sub-title">各类型库存占比</div>
                {typeStats.map((item, index) => (
                  <div key={index} style={{ marginBottom: '8px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                      <Text>{item.partTypeDesc}</Text>
                      <Text type="secondary">{item.quantity} 件</Text>
                    </div>
                    <Progress
                      percent={totalQuantity > 0 ? Math.round((item.quantity / totalQuantity) * 100) : 0}
                      showInfo={false}
                      size="small"
                      strokeColor={[
                        { color: '#1890ff', percent: 100 }
                      ][index % 5]?.color || '#1890ff'}
                    />
                  </div>
                ))}
              </div>
            )}
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card
            title={
              <span>
                <ExclamationCircleOutlined style={{ color: '#ff4d4f', marginRight: '8px' }} />
                库存预警备件 TOP 10
              </span>
            }
            size="small"
            className="warn-parts-card"
            extra={
              <Link onClick={() => handleCardClick('/spare-parts?warnStatus=1')}>全部预警 &gt;</Link>
            }
          >
            <List
              size="small"
              dataSource={warnParts || []}
              locale={{ emptyText: '暂无预警备件' }}
              renderItem={(item) => (
                <List.Item
                  className="warn-item"
                  onClick={() => handleCardClick(`/spare-parts?id=${item.id}`)}
                  style={{ cursor: 'pointer' }}
                >
                  <List.Item.Meta
                    avatar={
                      <div
                        className="warn-avatar"
                        style={{ backgroundColor: `${getWarnColor(item.warnStatus)}15`, color: getWarnColor(item.warnStatus) }}
                      >
                        {getWarnIcon(item.warnStatus)}
                      </div>
                    }
                    title={
                      <div className="item-title">
                        <span>{item.partName}</span>
                        <Tag
                          color={item.warnStatus === 2 ? 'red' : 'orange'}
                          size="small"
                        >
                          {item.warnStatusDesc}
                        </Tag>
                      </div>
                    }
                    description={
                      <div className="item-desc">
                        <span style={{ marginRight: '16px' }}>
                          <Text type="secondary">型号:</Text> {item.partModel}
                        </span>
                        <span>
                          <Text type="secondary">库存:</Text>
                          <Text strong style={{ color: getWarnColor(item.warnStatus) }}>
                            {' '}{item.quantity}/{item.safeQuantity}
                          </Text>
                        </span>
                      </div>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: '16px' }}>
        <Col xs={24}>
          <Card title="库存健康度" size="small">
            <Row align="middle">
              <Col span={6}>
                <Progress
                  type="dashboard"
                  percent={stockHealthRate}
                  strokeColor={stockHealthRate >= 80 ? '#52c41a' : stockHealthRate >= 50 ? '#faad14' : '#ff4d4f'}
                />
              </Col>
              <Col span={18}>
                <div style={{ padding: '0 24px' }}>
                  <div className="sub-title">类型库存明细</div>
                  <Row gutter={[16, 8]}>
                    {typeStats?.map((item, index) => (
                      <Col xs={24} sm={12} md={6} key={index}>
                        <Card size="small" className="type-card">
                          <div className="type-name">{item.partTypeDesc}</div>
                          <div className="type-stats">
                            <div>
                              <span className="type-count">{item.skuCount}</span>
                              <span className="type-label"> 种</span>
                            </div>
                            <div>
                              <span className="type-count">{item.quantity}</span>
                              <span className="type-label"> 件</span>
                            </div>
                            <div>
                              <span className="type-amount">¥{item.amount?.toLocaleString()}</span>
                            </div>
                          </div>
                        </Card>
                      </Col>
                    ))}
                  </Row>
                </div>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>

      <style>{`
        .stat-card {
          border-radius: 8px;
          transition: all 0.3s;
        }
        .stat-card:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(0,0,0,0.1);
        }
        .warning-card {
          border-left: 4px solid #faad14;
        }
        .danger-card {
          border-left: 4px solid #ff4d4f;
        }
        .stat-card-content {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }
        .stat-card-info {
          flex: 1;
        }
        .stat-card-title {
          color: #666;
          font-size: 14px;
          margin-bottom: 4px;
        }
        .stat-card-sub {
          color: #999;
          font-size: 12px;
          margin-top: 4px;
        }
        .stat-card-icon {
          width: 64px;
          height: 64px;
          borderRadius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-left: 16px;
        }
        .trend-item {
          display: flex;
          align-items: center;
          gap: 12px;
        }
        .trend-icon {
          width: 40px;
          height: 40px;
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 18px;
        }
        .trend-title {
          color: #666;
          font-size: 13px;
        }
        .trend-value {
          font-size: 24px;
          font-weight: 600;
        }
        .sub-title {
          color: #666;
          font-size: 13px;
          margin-bottom: 8px;
        }
        .warn-item:hover {
          background-color: #f5f5f5;
          border-radius: 4px;
        }
        .warn-avatar {
          width: 40px;
          height: 40px;
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 18px;
        }
        .item-title {
          display: flex;
          align-items: center;
          gap: 8px;
        }
        .item-desc {
          font-size: 12px;
        }
        .type-card {
          text-align: center;
          background: linear-gradient(135deg, #f5f7fa 0%, #e4e7eb 100%);
        }
        .type-name {
          font-size: 14px;
          color: #666;
          margin-bottom: 8px;
        }
        .type-stats {
          display: flex;
          justify-content: space-around;
          align-items: baseline;
        }
        .type-count {
          font-size: 20px;
          font-weight: 600;
          color: #1890ff;
        }
        .type-label {
          font-size: 12px;
          color: #999;
        }
        .type-amount {
          font-size: 14px;
          color: #52c41a;
          font-weight: 500;
        }
      `}</style>
    </div>
  )
}

export default LowStockWarningCard
