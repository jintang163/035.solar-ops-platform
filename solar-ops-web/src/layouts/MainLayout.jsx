import React, { useState, useEffect } from 'react'
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
  RiseOutlined,
  DroneOutlined,
  BugOutlined,
  DropletOutlined,
  AreaChartOutlined,
  DollarOutlined,
  PieChartOutlined,
  ToolOutlined,
  InboxOutlined,
  ShoppingCartOutlined,
  StockOutlined,
  LineChartOutlined,
  TeamOutlined,
  ApartmentOutlined
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { getUser, clearAuth, getWorkspace, setWorkspace, setCurrentStationId } from '../utils/auth'
import WorkspaceSwitcher from '../components/WorkspaceSwitcher'
import { getWorkspaceInfo } from '../api/workspace'

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
    label: '智能预测',
    children: [
      {
        key: '/prediction/power',
        label: '功率预测'
      },
      {
        key: '/prediction/lifetime',
        label: '设备寿命预测'
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
    key: '/spare-parts',
    icon: <ToolOutlined />,
    label: '备件管理',
    children: [
      {
        key: '/spare-parts/inventory',
        icon: <InboxOutlined />,
        label: '库存管理'
      },
      {
        key: '/stocktakes',
        icon: <StockOutlined />,
        label: '库存盘点'
      },
      {
        key: '/purchase-suggestions',
        icon: <ShoppingCartOutlined />,
        label: '采购建议'
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
    key: '/drone',
    icon: <DroneOutlined />,
    label: '无人机巡检',
    children: [
      {
        key: '/drone/tasks',
        label: '巡检任务'
      },
      {
        key: '/drone/defects',
        label: '缺陷管理'
      }
    ]
  },
  {
    key: '/cleaning',
    icon: <DropletOutlined />,
    label: '清洁管理',
    children: [
      {
        key: '/cleaning/dashboard',
        icon: <AreaChartOutlined />,
        label: '清洗仪表盘'
      },
      {
        key: '/cleaning/reminder',
        label: '清洗提醒'
      },
      {
        key: '/cleaning/plan',
        label: '清洗计划'
      }
    ]
  },
  {
    key: '/revenue',
    icon: <DollarOutlined />,
    label: '收益管理',
    children: [
      {
        key: '/revenue/dashboard',
        icon: <PieChartOutlined />,
        label: '收益仪表盘'
      },
      {
        key: '/revenue/scheme',
        label: '电价方案'
      },
      {
        key: '/revenue/compare',
        label: '方案对比'
      },
      {
        key: '/revenue/statistics',
        label: '收益统计'
      }
    ]
  },
  {
    key: '/report',
    icon: <LineChartOutlined />,
    label: '数据报表',
    children: [
      {
        key: '/report/group',
        icon: <BarChartOutlined />,
        label: '集团版报表'
      }
    ]
  },
  {
    key: '/settings',
    icon: <SettingOutlined />,
    label: '系统设置',
    children: [
      {
        key: '/settings/org',
        icon: <ApartmentOutlined />,
        label: '组织架构'
      },
      {
        key: '/settings/user',
        icon: <TeamOutlined />,
        label: '用户管理'
      }
    ]
  }
]

const MainLayout = () => {
  const [collapsed, setCollapsed] = useState(false)
  const [workspaceLoaded, setWorkspaceLoaded] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const user = getUser()

  useEffect(() => {
    initWorkspace()
  }, [])

  const initWorkspace = async () => {
    const workspace = getWorkspace()
    if (!workspace) {
      try {
        const res = await getWorkspaceInfo()
        if (res.data) {
          setWorkspace(res.data)
        }
      } catch (e) {
        console.error('初始化工作空间失败', e)
      }
    }
    setWorkspaceLoaded(true)
  }

  const handleWorkspaceSwitch = (station) => {
    navigate(0)
  }

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
          <div className="header-center">
            {workspaceLoaded && (
              <WorkspaceSwitcher onSwitch={handleWorkspaceSwitch} />
            )}
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
