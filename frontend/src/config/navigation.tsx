import React from 'react'
import {
  HomeOutlined,
  TeamOutlined,
  TrophyOutlined,
  FormOutlined,
  CheckCircleOutlined,
  TableOutlined,
  FileTextOutlined,
  AccountBookOutlined,
  AuditOutlined,
  IdcardOutlined,
  UserOutlined,
  BookOutlined,
  EditOutlined,
  FileSearchOutlined,
  CalendarOutlined,
  CloudOutlined,
} from '@ant-design/icons'
import { isFullAccess, canManage, canManageHomework, canViewFinance, canViewLogs } from '../utils/permission'

export interface NavItem {
  key: string
  label: string
  icon: React.ReactNode
  /** 权限门控；不传则默认可见 */
  show?: () => boolean
}

export interface NavGroup {
  key: string
  label: string
  items: NavItem[]
}

/**
 * 后台导航结构（按业务分组）。
 * 仅做视觉分组，route key 保持不变；Sidebar 与 Command Palette 共用此配置。
 */
export const NAV_GROUPS: NavGroup[] = [
  {
    key: 'personal',
    label: '个人',
    items: [
      { key: '/', label: '首页概览', icon: <HomeOutlined /> },
      { key: '/my-profile', label: '我的资料', icon: <IdcardOutlined /> },
      { key: '/my-applications', label: '我的活动登记', icon: <FormOutlined /> },
      { key: '/my-homework', label: '我的作业', icon: <BookOutlined /> },
    ],
  },
  {
    key: 'business',
    label: '业务管理',
    items: [
      { key: '/members', label: '成员管理', icon: <TeamOutlined />, show: canManage },
      { key: '/cohorts', label: '届次管理', icon: <CalendarOutlined />, show: isFullAccess },
      { key: '/point-items', label: '积分项目管理', icon: <TrophyOutlined />, show: canManage },
      { key: '/point-applications', label: '积分审核', icon: <CheckCircleOutlined />, show: canManage },
      { key: '/points-table', label: '积分总表', icon: <TableOutlined /> },
    ],
  },
  {
    key: 'collab',
    label: '协作办公',
    items: [
      { key: '/homework-review', label: '作业批改', icon: <EditOutlined />, show: canManageHomework },
      { key: '/homework-management', label: '作业管理', icon: <FileSearchOutlined />, show: canManageHomework },
      { key: '/meeting-minutes', label: '会议纪要', icon: <FileTextOutlined /> },
      { key: '/finance', label: '财务台账', icon: <AccountBookOutlined />, show: canViewFinance },
      { key: 'alist', label: '开源网盘', icon: <CloudOutlined /> },
    ],
  },
  {
    key: 'system',
    label: '系统',
    items: [
      { key: '/users', label: '账号管理', icon: <UserOutlined />, show: isFullAccess },
      { key: '/operation-logs', label: '操作日志', icon: <AuditOutlined />, show: canViewLogs },
    ],
  },
]

/** 展平的可见导航项（供命令面板搜索 / 侧边栏选中态判断）。 */
export function visibleNavItems(): NavItem[] {
  return NAV_GROUPS.flatMap((g) => g.items).filter((i) => !i.show || i.show())
}
