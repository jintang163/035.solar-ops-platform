import React from 'react'
import { Navigate } from 'react-router-dom'
import MainLayout from '../layouts/MainLayout'
import Login from '../pages/login/Login'
import Dashboard from '../pages/dashboard/Dashboard'
import InverterMonitor from '../pages/monitor/InverterMonitor'
import StationList from '../pages/station/StationList'
import EfficiencyAnalysis from '../pages/efficiency/EfficiencyAnalysis'
import WorkOrderList from '../pages/workorder/WorkOrderList'
import DispatchMap from '../pages/workorder/DispatchMap'
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
import RevenueDashboard from '../pages/revenue/RevenueDashboard'
import PriceSchemeList from '../pages/revenue/PriceSchemeList'
import PriceSchemeCompare from '../pages/revenue/PriceSchemeCompare'
import RevenueStatistics from '../pages/revenue/RevenueStatistics'
import SparePartInventoryList from '../pages/spare-parts/SparePartInventoryList'
import StocktakeList from '../pages/stocktakes/StocktakeList'
import PurchaseSuggestionList from '../pages/purchase-suggestions/PurchaseSuggestionList'
import GroupReport from '../pages/report/GroupReport'
import OrgManagement from '../pages/settings/OrgManagement'
import KnowledgeBaseList from '../pages/knowledge/KnowledgeBaseList'
import StationCompare from '../pages/station/StationCompare'
import DataPlayback from '../pages/playback/DataPlayback'
import FaultReview from '../pages/playback/FaultReview'
import BigScreenDashboard from '../pages/big-screen/BigScreenDashboard'

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
      { path: 'revenue/dashboard', element: <RevenueDashboard /> },
      { path: 'revenue/scheme', element: <PriceSchemeList /> },
      { path: 'revenue/compare', element: <PriceSchemeCompare /> },
      { path: 'revenue/statistics', element: <RevenueStatistics /> },
      { path: 'spare-parts/inventory', element: <SparePartInventoryList /> },
      { path: 'stocktakes', element: <StocktakeList /> },
      { path: 'purchase-suggestions', element: <PurchaseSuggestionList /> },
      { path: 'report/group', element: <GroupReport /> },
      { path: 'knowledge/list', element: <KnowledgeBaseList /> },
      { path: 'station/compare', element: <StationCompare /> },
      { path: 'playback/data', element: <DataPlayback /> },
      { path: 'playback/fault-review/:workOrderId', element: <FaultReview /> },
      { path: 'settings/org', element: <OrgManagement /> },
      { path: 'settings/user', element: <UserManagement /> },
      { path: 'big-screen', element: <BigScreenDashboard /> },
      { path: 'mobile/dashboard', element: <BigScreenDashboard /> }
    ]
  },
  {
    path: '*',
    element: <Navigate to="/dashboard" replace />
  }
]

export default routes
