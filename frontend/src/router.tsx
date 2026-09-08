import React from 'react'
import { createBrowserRouter } from 'react-router-dom'
import MainLayout from './layouts/MainLayout'
import AuthGuard from './components/AuthGuard'
import PermissionGuard from './components/PermissionGuard'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Members from './pages/Members'
import PointItems from './pages/PointItems'
import MyApplications from './pages/MyApplications'
import PointApplications from './pages/PointApplications'
import PointsTable from './pages/PointsTable'
import MeetingMinutes from './pages/MeetingMinutes'
import Finance from './pages/Finance'
import OperationLogs from './pages/OperationLogs'
import Users from './pages/Users'
import MyProfile from './pages/MyProfile'
import MyHomework from './pages/homework/MyHomework'
import HomeworkReview from './pages/homework/HomeworkReview'
import HomeworkManagement from './pages/homework/HomeworkManagement'
import CohortManagement from './pages/CohortManagement'

const basename = import.meta.env.BASE_URL.replace(/\/$/, '') || '/'

const router = createBrowserRouter([{ path: '/login', element: <Login /> }, { path: '/', element: <AuthGuard><MainLayout /></AuthGuard>, children: [{ index: true, element: <Dashboard /> }, { path: 'members', element: <Members /> }, { path: 'point-items', element: <PointItems /> }, { path: 'my-applications', element: <MyApplications /> }, { path: 'point-applications', element: <PointApplications /> }, { path: 'points-table', element: <PointsTable /> }, { path: 'meeting-minutes', element: <MeetingMinutes /> }, { path: 'finance', element: <Finance /> }, { path: 'operation-logs', element: <OperationLogs /> }, { path: 'my-profile', element: <MyProfile /> }, { path: 'users', element: <PermissionGuard><Users /></PermissionGuard> }, { path: 'my-homework', element: <MyHomework /> }, { path: 'homework-review', element: <HomeworkReview /> }, { path: 'homework-management', element: <HomeworkManagement /> }, { path: 'cohorts', element: <PermissionGuard><CohortManagement /></PermissionGuard> }] }], { basename })

export default router
