import React from 'react'
import { Tag } from 'antd'
import type { CurrentUser } from '../../utils/auth'
import { cohortLabel } from '../../utils/cohort'

function greetingForHour(hour: number): string {
  if (hour < 5) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
}

function permissionLabel(user: CurrentUser | null): string {
  if (user?.fullAccess) return '完整管理'
  if (user?.position === '部长') return '作业管理'
  return '普通成员'
}

interface Props {
  user: CurrentUser | null
}

const DashboardGreeting: React.FC<Props> = ({ user }) => {
  const hour = new Date().getHours()
  const name = user?.name || user?.username || '同学'
  const tags = [
    user?.department ? `部门 · ${user.department}` : null,
    user?.position,
    user?.cohortYear != null ? cohortLabel(user.cohortYear) : null,
    permissionLabel(user),
  ].filter((t): t is string => !!t)

  return (
    <div className="dash-greeting">
      <div>
        <h2 className="dash-greeting-title">
          {greetingForHour(hour)}，{name} 👋
        </h2>
        <p className="dash-greeting-sub">这里是你今天的社团工作概览</p>
      </div>
      <div className="dash-greeting-tags">
        {tags.map((t) => (
          <Tag
            key={t}
            className={`dash-identity-tag${t === '完整管理' ? ' dash-identity-tag--primary' : ''}`}
          >
            {t}
          </Tag>
        ))}
      </div>
    </div>
  )
}

export default DashboardGreeting
