import React, { useState } from 'react'
import { Layout, Menu, Avatar, Dropdown, Badge, Breadcrumb } from 'antd'
import {
  DashboardOutlined,
  ThunderboltOutlined,
  EnvironmentOutlined,
  BarChartOutlined,
  FileTextOutlined,
  SettingOutlined,
  UserOutlined,
  BellOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  AppstoreOutlined,
  QrcodeOutlined,
  RiseOutlined
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { getUser, clearAuth } from '../utils/auth'

const { Header, Sider, Content } = Layout

const menuItems = [
  {
    key: '/dashboard',
    icon: <DashboardOutlined />,
    label: '仪表盘'
  },
  {
    key: '/monitor',
    icon: <ThunderboltOutlined />,
    label: '设备监控',
    children: [
      {
        key: '/monitor/inverter',
        label: '逆变器监控'
      }
    ]
  },
  {
    key: '/station',
    icon: <EnvironmentOutlined />,
    label: '电站管理',
    children: [
      {
        key: '/station/list',
        label: '电站列表'
      }
    ]
  },
  {
    key: '/efficiency',
    icon: <BarChartOutlined />,
    label: '效率分析',
    children: [
      {
        key: '/efficiency/analysis',
        label: '效率分析'
      }
    ]
  },
  {
    key: '/prediction',
    icon: <RiseOutlined />,
    label: '功率预测',
    children: [
      {
        key: '/prediction/power',
        label: '预测与偏差分析'
      }
    ]
  },
  {
    key: '/asset',
    icon: <AppstoreOutlined />,
    label: '资产管理',
    children: [
      {
        key: '/asset/list',
        label: '资产列表'
      },
      {
        key: '/asset/qrcode',
        label: '二维码管理'
      }
    ]
  },
  {
    key: '/workorder',
    icon: <FileTextOutlined />,
    label: '工单管理',
    children: [
      {
        key: '/workorder/list',
        label: '工单列表'
      }
    ]
  },
  {
    key: '/settings',
    icon: <SettingOutlined />,
    label: '系统设置',
    children: [
      {
        key: '/settings/user',
        label: '用户管理'
      }
    ]
  }
]

const MainLayout = () => {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const user = getUser()

  const handleLogout = () => {
    clearAuth()
    navigate('/login')
  }

  const userMenuItems = [
    {
      key: '1',
      icon: <UserOutlined />,
      label: '个人中心'
    },
    {
      key: '2',
      icon: <SettingOutlined />,
      label: '账户设置'
    },
    {
      type: 'divider'
    },
    {
      key: '3',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout
    }
  ]

  const getSelectedKeys = () => {
    const pathname = location.pathname
    return [pathname]
  }

  const getOpenKeys = () => {
    const pathname = location.pathname
    const parts = pathname.split('/').filter(Boolean)
    if (parts.length > 1) {
      return [`/${parts[0]}`]
    }
    return []
  }

  const handleMenuClick = ({ key }) => {
    navigate(key)
  }

  const getBreadcrumbItems = () => {
    const pathname = location.pathname
    const items = [{ title: '首页', href: '/dashboard' }]
    
    const findMenuItem = (items, key) => {
      for (const item of items) {
        if (item.key === key) return item
        if (item.children) {
          const found = findMenuItem(item.children, key)
          if (found) return found
        }
      }
      return null
    }

    const parts = pathname.split('/').filter(Boolean)
    let currentPath = ''
    
    for (let i = 0; i < parts.length; i++) {
      currentPath += `/${parts[i]}`
      const menuItem = findMenuItem(menuItems, currentPath)
      if (menuItem) {
        items.push({ title: menuItem.label })
      }
    }

    return items
  }

  return (
    <Layout className="main-layout">
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        theme="dark"
        width={220}
        className="main-sider"
      >
        <div className="logo">
          <span className="logo-icon">☀</span>
          {!collapsed && <span className="logo-text">太阳能运维平台</span>}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={getSelectedKeys()}
          defaultOpenKeys={getOpenKeys()}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>
      <Layout>
        <Header className="main-header">
          <div className="header-left">
            <span
              className="collapse-trigger"
              onClick={() => setCollapsed(!collapsed)}
            >
              {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            </span>
            <Breadcrumb items={getBreadcrumbItems()} className="header-breadcrumb" />
          </div>
          <div className="header-right">
            <Badge count={5} size="small">
              <span className="header-icon">
                <BellOutlined />
              </span>
            </Badge>
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
              <div className="user-info">
                <Avatar size="small" icon={<UserOutlined />} />
                <span className="user-name">{user?.username || '管理员'}</span>
              </div>
            </Dropdown>
          </div>
        </Header>
        <Content className="main-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

export default MainLayout
