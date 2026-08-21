import React from 'react'
import { Empty } from 'antd'
import {
  UserOutlined,
  CheckCircleOutlined,
  TrophyOutlined,
  FileTextOutlined,
  EditOutlined,
  AccountBookOutlined,
  CalendarOutlined,
  SettingOutlined,
  ArrowRightOutlined,
} from '@ant-design/icons'
import type { RecentActivity } from '../../types/dashboard'

interface ModuleMeta {
  icon: React.ReactNode
  color: string
  soft: string
}

const MODULE_META: Record<string, ModuleMeta> = {
  member: { icon: <UserOutlined />, color: '#2f6bff', soft: '#eef4ff' },
  point_application: { icon: <CheckCircleOutlined />, color: '#12b76a', soft: '#ecfdf3' },
  point_item: { icon: <TrophyOutlined />, color: '#2f6bff', soft: '#eef4ff' },
  point_record: { icon: <TrophyOutlined />, color: '#12b76a', soft: '#ecfdf3' },
  homework: { icon: <FileTextOutlined />, color: '#f79009', soft: '#fff6ed' },
  homework_submission: { icon: <EditOutlined />, color: '#f79009', soft: '#fff6ed' },
  meeting: { icon: <FileTextOutlined />, color: '#7f56d9', soft: '#f4f3ff' },
  finance: { icon: <AccountBookOutlined />, color: '#12b76a', soft: '#ecfdf3' },
  auth: { icon: <UserOutlined />, color: '#667085', soft: '#f2f4f7' },
  cohort: { icon: <CalendarOutlined />, color: '#667085', soft: '#f2f4f7' },
}

const DEFAULT_META: ModuleMeta = { icon: <SettingOutlined />, color: '#667085', soft: '#f2f4f7' }

/** 副标题使用的业务模块名 */
function businessLabel(module: string | undefined, action: string | undefined): string {
  switch (module) {
    case 'member':
      return '成员管理'
    case 'point_item':
      return '积分项目'
    case 'point_application':
      return '积分审核'
    case 'point_record':
      return '积分记录'
    case 'homework':
      return '作业管理'
    case 'homework_submission':
      return action === 'GRADE' || action === 'REGRADE' ? '作业批改' : '作业提交'
    case 'meeting':
      return '会议纪要'
    case 'finance':
      return '财务台账'
    case 'auth':
      return '账号管理'
    case 'cohort':
      return '届次管理'
    default:
      return '系统操作'
  }
}

interface Props {
  activities: RecentActivity[]
  canViewLogs: boolean
  onViewAll: () => void
}

const ActivityList: React.FC<Props> = ({ activities, canViewLogs, onViewAll }) => {
  return (
    <div className="activity-list">
      <div className="activity-head">
        <h3 className="section-title">最新动态</h3>
        {canViewLogs && (
          <button type="button" className="activity-view-all" onClick={onViewAll}>
            查看全部 <ArrowRightOutlined />
          </button>
        )}
      </div>

      {activities.length === 0 ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={
            <>
              <div className="chart-empty-title">暂无近期动态</div>
              <div className="chart-empty-sub">系统产生新的操作记录后会显示在这里。</div>
            </>
          }
        />
      ) : (
        <div className="activity-items">
          {activities.map((a, index) => {
            const meta = MODULE_META[a.moduleName ?? ''] ?? DEFAULT_META
            const label = businessLabel(a.moduleName, a.actionType)
            const dept = a.operatorDepartment && a.operatorDepartment !== '其他' ? `${a.operatorDepartment} · ` : ''
            return (
              <div className="activity-item" key={`${a.moduleName}-${a.title}-${index}`} title={a.desc}>
                <span className="activity-icon" style={{ background: meta.soft, color: meta.color }}>
                  {meta.icon}
                </span>
                <div className="activity-meta">
                  <div className="activity-title">{a.title}</div>
                  <div className="activity-sub">
                    {dept}
                    {label}
                  </div>
                </div>
                <span className="activity-time">{a.time}</span>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default ActivityList
