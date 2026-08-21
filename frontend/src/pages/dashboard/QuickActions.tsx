import React from 'react'
import {
  UserOutlined,
  TrophyOutlined,
  CheckCircleOutlined,
  EditOutlined,
  FileTextOutlined,
  AccountBookOutlined,
  FormOutlined,
  BookOutlined,
  TableOutlined,
  IdcardOutlined,
  ArrowRightOutlined,
} from '@ant-design/icons'
import { isFullAccess, isMinister } from '../../utils/permission'

interface Action {
  label: string
  sub: string
  to: string
  icon: React.ReactNode
  color: string
  soft: string
}

const FULL_ACCESS_ACTIONS: Action[] = [
  { label: '创建账号', sub: '创建成员账号并生成档案', to: '/users', icon: <UserOutlined />, color: '#2f6bff', soft: '#eef4ff' },
  { label: '创建积分项目', sub: '配置新的积分规则', to: '/point-items', icon: <TrophyOutlined />, color: '#f79009', soft: '#fff6ed' },
  { label: '审核积分', sub: '处理成员的积分申请', to: '/point-applications', icon: <CheckCircleOutlined />, color: '#12b76a', soft: '#ecfdf3' },
  { label: '发布作业', sub: '向成员发布新作业', to: '/homework-management', icon: <EditOutlined />, color: '#f79009', soft: '#fff6ed' },
  { label: '新建会议纪要', sub: '归档一次会议记录', to: '/meeting-minutes', icon: <FileTextOutlined />, color: '#7f56d9', soft: '#f4f3ff' },
  { label: '财务台账', sub: '维护月度财务文件', to: '/finance', icon: <AccountBookOutlined />, color: '#12b76a', soft: '#ecfdf3' },
]

const MINISTER_ACTIONS: Action[] = [
  { label: '发布作业', sub: '向本部门发布新作业', to: '/homework-management', icon: <EditOutlined />, color: '#f79009', soft: '#fff6ed' },
  { label: '批改作业', sub: '批改本部门提交', to: '/homework-review', icon: <CheckCircleOutlined />, color: '#2f6bff', soft: '#eef4ff' },
  { label: '我的作业', sub: '查看自己的作业', to: '/my-homework', icon: <BookOutlined />, color: '#7f56d9', soft: '#f4f3ff' },
  { label: '我的活动登记', sub: '登记个人积分活动', to: '/my-applications', icon: <FormOutlined />, color: '#12b76a', soft: '#ecfdf3' },
  { label: '我的资料', sub: '维护个人资料信息', to: '/my-profile', icon: <IdcardOutlined />, color: '#0ba5ec', soft: '#f0f9ff' },
]

const MEMBER_ACTIONS: Action[] = [
  { label: '登记活动', sub: '登记个人积分活动', to: '/my-applications', icon: <FormOutlined />, color: '#12b76a', soft: '#ecfdf3' },
  { label: '我的作业', sub: '查看并提交作业', to: '/my-homework', icon: <BookOutlined />, color: '#f79009', soft: '#fff6ed' },
  { label: '查看积分', sub: '查看积分总表', to: '/points-table', icon: <TableOutlined />, color: '#2f6bff', soft: '#eef4ff' },
  { label: '完善资料', sub: '维护个人资料信息', to: '/my-profile', icon: <IdcardOutlined />, color: '#7f56d9', soft: '#f4f3ff' },
  { label: '查看会议纪要', sub: '浏览会议记录', to: '/meeting-minutes', icon: <FileTextOutlined />, color: '#0ba5ec', soft: '#f0f9ff' },
]

interface Props {
  onNavigate: (to: string) => void
}

const QuickActions: React.FC<Props> = ({ onNavigate }) => {
  const actions = isFullAccess() ? FULL_ACCESS_ACTIONS : isMinister() ? MINISTER_ACTIONS : MEMBER_ACTIONS

  return (
    <div className="quick-actions">
      <h3 className="section-title">快捷操作</h3>
      <div className="quick-grid">
        {actions.map((a) => (
          <div
            key={a.label}
            className="quick-card"
            role="button"
            tabIndex={0}
            onClick={() => onNavigate(a.to)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault()
                onNavigate(a.to)
              }
            }}
          >
            <span className="quick-icon" style={{ background: a.soft, color: a.color }}>
              {a.icon}
            </span>
            <div className="quick-meta">
              <div className="quick-title">{a.label}</div>
              <div className="quick-sub">{a.sub}</div>
            </div>
            <ArrowRightOutlined className="quick-arrow" />
          </div>
        ))}
      </div>
    </div>
  )
}

export default QuickActions
