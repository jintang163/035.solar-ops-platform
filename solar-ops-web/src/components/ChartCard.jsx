import React from 'react'
import { Card } from 'antd'
import ReactECharts from 'echarts-for-react'

const ChartCard = ({ title, option, height = 300, extra, loading }) => {
  return (
    <Card
      title={title}
      extra={extra}
      className="chart-card"
      loading={loading}
    >
      <ReactECharts option={option} style={{ height }} />
    </Card>
  )
}

export default ChartCard
