import React, { useState, useEffect } from 'react'
import { Dropdown, Menu, Tag, Button, Badge, Space, Typography, Divider } from 'antd'
import {
  EnvironmentOutlined,
  ApartmentOutlined,
  DownOutlined,
  CheckOutlined,
  ThunderboltOutlined
} from '@ant-design/icons'
import { getStationTree, switchWorkspace, getWorkspaceInfo } from '../api/workspace'
import {
  getWorkspace,
  setWorkspace,
  getCurrentStationId,
  setCurrentStationId,
  getUser,
  isSuperAdmin
} from '../utils/auth'

const { Text } = Typography

const WorkspaceSwitcher = ({ onSwitch }) => {
  const [stationTree, setStationTree] = useState([])
  const [currentStation, setCurrentStation] = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadStationTree()
    loadCurrentWorkspace()
  }, [])

  const loadCurrentWorkspace = async () => {
    const stationId = getCurrentStationId()
    const workspace = getWorkspace()

    if (workspace) {
      if (stationId) {
        const station = workspace.stations?.find(s => String(s.id) === String(stationId))
        setCurrentStation(station || null)
      } else if (!isSuperAdmin() && workspace.stations?.length === 1) {
        setCurrentStation(workspace.stations[0])
      }
    }
  }

  const loadStationTree = async () => {
    try {
      const res = await getStationTree()
      if (res.data) {
        setStationTree(res.data)
      }
    } catch (e) {
      console.error('加载电站列表失败', e)
    }
  }

  const handleSwitch = async (station) => {
    if (station?.id === currentStation?.id) return

    setLoading(true)
    try {
      if (station && station.id) {
        setCurrentStationId(station.id)
        setCurrentStation(station)
      } else {
        setCurrentStationId(null)
        setCurrentStation(null)
      }

      const res = await switchWorkspace(station?.id || null)
      if (res.data) {
        setWorkspace(res.data)
      }

      onSwitch?.(station)
    } catch (e) {
      console.error('切换工作空间失败', e)
    } finally {
      setLoading(false)
    }
  }

  const renderMenuItems = () => {
    const items = []

    if (isSuperAdmin() || stationTree.length > 1) {
      items.push({
        key: 'all',
        label: (
          <Space>
            <ApartmentOutlined style={{ color: '#1890ff' }} />
            <span>全部电站</span>
            {!currentStation && <CheckOutlined style={{ color: '#52c41a', marginLeft: 'auto' }} />}
          </Space>
        ),
        onClick: () => handleSwitch(null)
      })
      items.push({ type: 'divider' })
    }

    stationTree.forEach(station => {
      items.push({
        key: station.id,
        label: (
          <Space>
            <EnvironmentOutlined style={{ color: '#52c41a' }} />
            <div style={{ flex: 1, minWidth: 0 }}>
              <div>
                <Text strong>{station.stationName}</Text>
                {currentStation?.id === station.id && (
                  <CheckOutlined style={{ color: '#52c41a', marginLeft: 8 }} />
                )}
              </div>
              <div style={{ fontSize: 12, color: '#999' }}>
                {station.stationCode} · {station.capacity}kW
              </div>
            </div>
            {station.orgName && (
              <Tag color="blue" style={{ marginLeft: 'auto' }} size="small">
                {station.orgName}
              </Tag>
            )}
          </Space>
        ),
        onClick: () => handleSwitch(station)
      })
    })

    return items
  }

  const displayLabel = () => {
    if (currentStation) {
      return (
        <Space>
          <Badge
            status={currentStation.status === 1 ? 'success' : 'default'}
            text={currentStation.stationName}
          />
        </Space>
      )
    }
    return (
      <Space>
        <ApartmentOutlined />
        <span>全部电站</span>
      </Space>
    )
  }

  const displayInfo = () => {
    if (currentStation) {
      return (
        <div style={{ fontSize: 12, color: '#999', marginTop: 2 }}>
          <Space size={8}>
            <span><ThunderboltOutlined /> {currentStation.capacity}kW</span>
            {currentStation.orgName && <span>{currentStation.orgName}</span>}
          </Space>
        </div>
      )
    }
    const workspace = getWorkspace()
    if (workspace?.stations?.length > 0) {
      return (
        <div style={{ fontSize: 12, color: '#999', marginTop: 2 }}>
          共 {workspace.stations.length} 个电站 · 总装机 {workspace.stations.reduce((sum, s) => sum + (Number(s.capacity) || 0), 0).toLocaleString()}kW
        </div>
      )
    }
    return null
  }

  if (stationTree.length === 0) {
    return null
  }

  return (
    <Dropdown
      menu={{ items: renderMenuItems() }}
      placement="bottomRight"
      trigger={['click']}
      loading={loading}
    >
      <div style={{ cursor: 'pointer', padding: '0 12px' }}>
        {displayLabel()}
        <DownOutlined style={{ fontSize: 12, marginLeft: 4, color: '#999' }} />
        {displayInfo()}
      </div>
    </Dropdown>
  )
}

export default WorkspaceSwitcher
