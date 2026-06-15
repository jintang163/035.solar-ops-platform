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
import AssetList from '../pages/asset/AssetList'
import AssetDetail from '../pages/asset/AssetDetail'
import QrCodeManager from '../pages/asset/QrCodeManager'
import PowerPrediction from '../pages/prediction/PowerPrediction'
import DeviceLifetime from '../pages/prediction/DeviceLifetime'
import DroneInspectionList from '../pages/drone/DroneInspectionList'
import DroneDefectList from '../pages/drone/DroneDefectList'
import CleaningDashboard from '../pages/cleaning/CleaningDashboard'
import CleaningPlanList from '../pages/cleaning/CleaningPlanList'
import CleaningReminderList from '../pages/cleaning/CleaningReminderList'

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
      { path: 'prediction/power', element: <PowerPrediction /> },
      { path: 'prediction/lifetime', element: <DeviceLifetime /> },
      { path: 'asset/list', element: <AssetList /> },
      { path: 'asset/detail/:id', element: <AssetDetail /> },
      { path: 'asset/qrcode', element: <QrCodeManager /> },
      { path: 'workorder/list', element: <WorkOrderList /> },
      { path: 'drone/tasks', element: <DroneInspectionList /> },
      { path: 'drone/defects', element: <DroneDefectList /> },
      { path: 'cleaning/dashboard', element: <CleaningDashboard /> },
      { path: 'cleaning/plan', element: <CleaningPlanList /> },
      { path: 'cleaning/reminder', element: <CleaningReminderList /> },
      { path: 'settings/user', element: <UserManagement /> }
    ]
  },
  {
    path: '*',
    element: <Navigate to="/dashboard" replace />
  }
]

export default routes
