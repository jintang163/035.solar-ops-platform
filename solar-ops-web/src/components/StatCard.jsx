import React from 'react'
import { Card, Statistic } from 'antd'

const StatCard = ({ title, value, prefix, suffix, icon, color = '#1890ff', trend, trendValue }) => {
  return (
    <Card className="stat-card" hoverable>
      <div className="stat-card-content">
        <div className="stat-card-info">
          <div className="stat-card-title">{title}</div>
          <Statistic
            value={value}
            prefix={prefix}
            suffix={suffix}
            valueStyle={{ color, fontSize: '28px', fontWeight: 600 }}
          />
          {trend && (
            <div className={`stat-card-trend ${trend}`}>
              {trend === 'up' ? '↑' : '↓'} {trendValue}
            </div>
          )}
        </div>
        <div className="stat-card-icon" style={{ backgroundColor: `${color}15`, color }}>
          {icon}
        </div>
      </div>
    </Card>
  )
}

export default StatCard
