import React, { useEffect, useState } from 'react'
import { Button, Card, Segmented, Skeleton } from 'antd'
import {
  TeamOutlined,
  TrophyOutlined,
  CheckCircleOutlined,
  FileTextOutlined,
  AccountBookOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { getCurrentUser } from '../utils/auth'
import { canViewLogs } from '../utils/permission'
import { getDashboardStats } from '../api/dashboard'
import type { DashboardStats } from '../types/dashboard'
import { getCohorts } from '../api/cohort'
import type { Cohort } from '../types/cohort'
import DashboardGreeting from './dashboard/DashboardGreeting'
import TodayTasks from './dashboard/TodayTasks'
import TrendChart from './dashboard/TrendChart'
import DepartmentDonut from './dashboard/DepartmentDonut'
import ActivityList from './dashboard/ActivityList'
import QuickActions from './dashboard/QuickActions'

const Dashboard: React.FC = () => {
  const navigate = useNavigate()
  const user = getCurrentUser()
  const fullAccess = user?.fullAccess === true

  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [reloadTick, setReloadTick] = useState(0)
  const [cohorts, setCohorts] = useState<Cohort[]>([])
  const [activeCohort, setActiveCohort] = useState<string>(
    fullAccess ? 'all' : (user?.cohortId != null ? String(user.cohortId) : 'all'),
  )

  const cohortId = activeCohort === 'all' ? undefined : Number(activeCohort)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(false)
    getDashboardStats(cohortId)
      .then((s) => {
        if (!cancelled) setStats(s)
      })
      .catch(() => {
        if (!cancelled) {
          setStats(null)
          setError(true)
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [cohortId, reloadTick])

  useEffect(() => {
    getCohorts().then((list) => {
      setCohorts(list)
      if (fullAccess) {
        const latest = list.find((c) => c.enabled)
        if (latest) setActiveCohort(String(latest.id))
      }
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const kpis = stats
    ? [
        { key: 'members', title: '成员总数', value: stats.totalMembers, sub: '当前届次', icon: <TeamOutlined />, color: '#2f6bff', soft: '#eef4ff' },
        { key: 'pointItems', title: '积分项目', value: stats.totalPointItems, sub: '已配置项目', icon: <TrophyOutlined />, color: '#f79009', soft: '#fff6ed' },
        ...(fullAccess
          ? [
              {
                key: 'pending',
                title: '待审核积分',
                value: stats.pendingApplications,
                sub: stats.pendingApplications > 0 ? '需优先处理' : '暂无待审',
                icon: <CheckCircleOutlined />,
                color: stats.pendingApplications > 0 ? '#f79009' : '#12b76a',
                soft: stats.pendingApplications > 0 ? '#fff6ed' : '#ecfdf3',
              },
            ]
          : []),
        { key: 'meetings', title: '会议纪要', value: stats.totalMeetingMinutes, sub: '累计归档', icon: <FileTextOutlined />, color: '#7f56d9', soft: '#f4f3ff' },
        ...(fullAccess
          ? [
              {
                key: 'finance',
                title: '财务台账',
                value: stats.totalFinancePeriods,
                sub: '已建立月份',
                icon: <AccountBookOutlined />,
                color: '#12b76a',
                soft: '#ecfdf3',
              },
            ]
          : []),
      ]
    : []

  if (loading) {
    return (
      <div className="app-page dashboard-page">
        <Card className="app-card">
          <Skeleton active title={{ width: 220 }} paragraph={{ rows: 2 }} />
          <div className="kpi-grid" style={{ marginTop: 20 }}>
            {[1, 2, 3, 4, 5].map((i) => (
              <div className="kpi-card" key={i}>
                <Skeleton active paragraph={false} />
              </div>
            ))}
          </div>
        </Card>
        <div className="dash-two-col">
          <Card className="app-card">
            <Skeleton active paragraph={{ rows: 6 }} />
          </Card>
          <Card className="app-card">
            <Skeleton active paragraph={{ rows: 6 }} />
          </Card>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="app-page dashboard-page">
        <Card className="app-card">
          <div className="dash-error">
            <div className="dash-error-title">工作台数据加载失败</div>
            <div className="dash-error-sub">请检查网络或后端服务后重试。</div>
            <Button type="primary" onClick={() => setReloadTick((t) => t + 1)}>
              重新加载
            </Button>
          </div>
        </Card>
      </div>
    )
  }

  if (!stats) return null

  return (
    <div className="app-page dashboard-page">
      {fullAccess && (
        <Segmented
          className="dash-cohort"
          value={activeCohort}
          options={[
            { label: '全部', value: 'all' },
            ...cohorts.map((c) => ({ label: `${c.year}届`, value: String(c.id) })),
          ]}
          onChange={(v) => setActiveCohort(v as string)}
        />
      )}

      <Card className="app-card dash-hero">
        <DashboardGreeting user={user} />
        <TodayTasks tasks={stats.pendingTasks} onNavigate={navigate} />
      </Card>

      <section className="kpi-grid">
        {kpis.map((k) => (
          <Card key={k.key} className="app-card kpi-card">
            <div className="kpi-inner">
              <span className="kpi-icon" style={{ background: k.soft, color: k.color }}>
                {k.icon}
              </span>
              <div className="kpi-meta">
                <div className="kpi-title">{k.title}</div>
                <div className="kpi-value">{k.value}</div>
                <div className="kpi-sub">{k.sub}</div>
              </div>
            </div>
          </Card>
        ))}
      </section>

      <section className="dash-two-col dash-charts">
        <Card
          className="app-card"
          title="积分登记趋势"
          extra={<span className="section-sub">近 7 周</span>}
        >
          <TrendChart data={stats.weeklyTrend} />
        </Card>
        <Card className="app-card donut-card" title="部门成员构成">
          <DepartmentDonut data={stats.departmentDistribution} />
        </Card>
      </section>

      <section className="dash-two-col dash-bottom">
        <Card className="app-card">
          <ActivityList
            activities={stats.recentActivities}
            canViewLogs={canViewLogs()}
            onViewAll={() => navigate('/operation-logs')}
          />
        </Card>
        <Card className="app-card">
          <QuickActions onNavigate={navigate} />
        </Card>
      </section>
    </div>
  )
}

export default Dashboard
