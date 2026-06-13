import React from 'react'
import { Navigate } from 'react-router-dom'
import MainLayout from '../layouts/MainLayout'
import Login from '../pages/login/Login'
import Dashboard from '../pages/dashboard/Dashboard'
import InverterMonitor from '../pages/monitor/InverterMonitor'
import StationList from '../pages/station/StationList'
import EfficiencyAnalysis from '../pages/efficiency/EfficiencyAnalysis'
import WorkOrderList from '../pages/workorder/WorkOrderList'
import UserManagement from '../pages/settings/UserManagement'

const routes = [
  {
    path: '/login',
    element: <Login />
  },
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: 'dashboard', element: <Dashboard /> },
      { path: 'monitor/inverter', element: <InverterMonitor /> },
      { path: 'station/list', element: <StationList /> },
      { path: 'efficiency/analysis', element: <EfficiencyAnalysis /> },
      { path: 'workorder/list', element: <WorkOrderList /> },
      { path: 'settings/user', element: <UserManagement /> }
    ]
  },
  {
    path: '*',
    element: <Navigate to="/dashboard" replace />
  }
]

export default routes
