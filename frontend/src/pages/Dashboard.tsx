import React, { useEffect, useMemo, useState } from 'react'
import { Button, Card, Carousel, Progress, Space, Statistic, Tag, Tooltip, Typography, Spin } from 'antd'
import {
  TeamOutlined,
  TrophyOutlined,
  FormOutlined,
  CheckCircleOutlined,
  TableOutlined,
  FolderOpenOutlined,
  FileTextOutlined,
  AccountBookOutlined,
  AuditOutlined,
  UserOutlined,
  ArrowRightOutlined,
  CalendarOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { getCurrentUser } from '../utils/auth'
import { canManage, canViewFinance, canViewLogs } from '../utils/permission'
import { getDashboardStats } from '../api/dashboard'
import type { DashboardStats } from '../types/dashboard'
import logo from '../assets/logo.png'
import banner1 from '../assets/banner-operations.svg'
import banner2 from '../assets/banner-operations.svg'
import banner3 from '../assets/banner-operations.svg'

const { Title, Paragraph, Text } = Typography

const introSlides = [
  {
    key: '1',
    title: '成员管理与资料完善',
    desc: '围绕成员信息、账号管理和资料完善构建统一工作台。',
    action: '查看成员管理',
    to: '/members',
    image: banner1,
  },
  {
    key: '2',
    title: '积分登记与审核',
    desc: '支持登记、审核、统计与排名，形成清晰的积分闭环。',
    action: '进入积分审核',
    to: '/point-applications',
    image: banner2,
  },
  {
    key: '3',
    title: '会议纪要与活动归档',
    desc: '会议纪要、活动归档、财务台账与日志统一收口。',
    action: '查看资料归档',
    to: '/archive-links',
    image: banner3,
  },
]

const toolbox = [
  { label: '成员管理', to: '/members', icon: <TeamOutlined /> },
  { label: '账号管理', to: '/users', icon: <UserOutlined />, managerOnly: true },
  { label: '积分项目管理', to: '/point-items', icon: <TrophyOutlined />, managerOnly: true },
  { label: '积分审核', to: '/point-applications', icon: <CheckCircleOutlined />, managerOnly: true },
  { label: '我的资料', to: '/my-profile', icon: <FormOutlined /> },
  { label: '活动资料归档', to: '/archive-links', icon: <FolderOpenOutlined /> },
  { label: '会议纪要', to: '/meeting-minutes', icon: <FileTextOutlined /> },
  { label: '财务台账', to: '/finance', icon: <AccountBookOutlined />, managerOnly: true },
  { label: '操作日志', to: '/operation-logs', icon: <AuditOutlined />, managerOnly: true },
]

const Dashboard: React.FC = () => {
  const navigate = useNavigate()
  const user = getCurrentUser()
  const fullAccess = user?.fullAccess === true
  const [hoveredBar, setHoveredBar] = useState<number | null>(null)
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getDashboardStats()
      .then(setStats)
      .catch(() => { /* 接口失败时显示空状态 */ })
      .finally(() => setLoading(false))
  }, [])

  const visibleToolbox = useMemo(() => toolbox.filter((item) => !item.managerOnly || fullAccess), [fullAccess])

  const statsCards = stats ? [
    { title: '社团成员总数', value: stats.totalMembers, icon: <TeamOutlined />, color: '#2f6bff', trend: '实时统计' },
    { title: '积分项目数量', value: stats.totalPointItems, icon: <TrophyOutlined />, color: '#f59e0b', trend: '实时统计' },
    { title: '待审核登记数', value: stats.pendingApplications, icon: <CheckCircleOutlined />, color: stats.pendingApplications > 0 ? '#ef4444' : '#19a974', trend: stats.pendingApplications > 0 ? '需优先处理' : '暂无待审' },
    { title: '活动资料数', value: stats.totalArchiveLinks, icon: <FolderOpenOutlined />, color: '#0ea5e9', trend: '实时统计' },
    { title: '会议纪要数', value: stats.totalMeetingMinutes, icon: <FileTextOutlined />, color: '#8b5cf6', trend: '实时统计' },
    { title: '财务月份数', value: stats.totalFinancePeriods, icon: <AccountBookOutlined />, color: '#19a974', trend: '实时统计' },
  ] : []

  const maxBarValue = stats?.weeklyTrend?.length
    ? Math.max(...stats.weeklyTrend.map((w) => w.count), 1)
    : 40

  if (loading) {
    return (
      <div className="app-page dashboard-page" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
        <Spin size="large" />
      </div>
    )
  }

  return (
    <div className="app-page dashboard-page">
      <section className="dashboard-hero">
        <Card className="app-card" bordered={false}>
          <div className="dashboard-hero-left">
            <div>
              <div className="dashboard-hello">你好，{user?.name || user?.username || '同学'}</div>
              <Title level={2} style={{ margin: '8px 0 8px' }}>欢迎进入开放原子开源社团秘书处管理系统</Title>
              <Paragraph style={{ marginBottom: 0, color: '#667085', maxWidth: 680 }}>
                用于成员管理、资料完善、积分登记、会议归档与日常办公，保持流程清晰、记录完整、操作可追溯。
              </Paragraph>
              <div className="dashboard-hero-tags">
                <Tag color="blue">部门：{user?.department || '未填写'}</Tag>
                <Tag color="green">职务：{user?.position || '成员'}</Tag>
                <Tag color="default">权限：{fullAccess ? 'fullAccess' : 'normal'}</Tag>
              </div>
            </div>
            <div className="dashboard-hero-actions">
              <Button type="primary" onClick={() => navigate('/members')}>成员管理</Button>
              <Button onClick={() => navigate('/my-profile')}>我的资料</Button>
              <Button onClick={() => navigate('/points-table')}>积分总表</Button>
            </div>
          </div>
        </Card>

        <Card className="dashboard-carousel-card app-card" bordered={false}>
          <div className="dashboard-carousel-wrap">
            <Carousel autoplay autoplaySpeed={3600} effect="fade" dots>
              {introSlides.map((item) => (
                <div key={item.key}>
                  <div className="carousel-slide" style={{ backgroundImage: `url(${item.image})` }}>
                    <div>
                      <h3 className="carousel-title">{item.title}</h3>
                      <p className="carousel-desc">{item.desc}</p>
                    </div>
                    <Button onClick={() => navigate(item.to)}>{item.action} <ArrowRightOutlined /></Button>
                  </div>
                </div>
              ))}
            </Carousel>
          </div>
        </Card>
      </section>

      {stats && (
        <>
          <section className="dashboard-stats-grid">
            {statsCards.map((item, index) => (
              <Card key={item.title} className="stat-card app-card enter-up" bordered={false} style={{ animationDelay: `${index * 70}ms` }}>
                <Statistic title={item.title} value={item.value} prefix={<span style={{ color: item.color }}>{item.icon}</span>} suffix={<span style={{ fontSize: 12, color: '#667085' }}>{item.trend}</span>} />
              </Card>
            ))}
          </section>

          <section className="dashboard-two-col">
            <Card className="app-card enter-up" bordered={false} style={{ animationDelay: '80ms' }} title="近期积分登记趋势">
              <Text type="secondary">近 7 周登记数量变化，用于观察工作节奏。</Text>
              <div className="bar-chart" style={{ marginTop: 16 }}>
                {stats.weeklyTrend.map((item, index) => (
                  <Tooltip key={index} title={`第 ${item.week} 周：${item.count} 条`}>
                    <div className="bar-chart-item" onMouseEnter={() => setHoveredBar(index)} onMouseLeave={() => setHoveredBar(null)}>
                      <div className="bar-chart-bar-wrap">
                        <div className={`bar-chart-bar ${hoveredBar === index ? 'active' : ''}`} style={{ height: `${Math.max(4, (item.count / maxBarValue) * 100)}px` }} />
                      </div>
                      <span>W{item.week}</span>
                    </div>
                  </Tooltip>
                ))}
              </div>
            </Card>

            <Card className="app-card enter-up" bordered={false} style={{ animationDelay: '120ms' }} title="部门成员分布">
              <Text type="secondary">按部门统计人数，便于识别人员分布情况。</Text>
              <div className="dept-list" style={{ marginTop: 16 }}>
                {stats.departmentDistribution.map((item) => {
                  const maxDept = Math.max(...stats.departmentDistribution.map((d) => d.value), 1)
                  return (
                    <div className="dept-row" key={item.label}>
                      <div className="dept-head">
                        <span>{item.label}</span>
                        <strong>{item.value}</strong>
                      </div>
                      <div className="dept-bar"><div style={{ width: `${(item.value / maxDept) * 100}%` }} /></div>
                    </div>
                  )
                })}
              </div>
            </Card>
          </section>

          <section className="dashboard-two-col">
            <Card className="app-card enter-up" bordered={false} style={{ animationDelay: '100ms' }} title="待办事项">
              <div className="todo-list">
                {stats.pendingTasks.length > 0 ? stats.pendingTasks.map((item) => (
                  <div className="todo-item" key={item.title} onClick={() => navigate(item.to)} role="button" tabIndex={0}>
                    <div>
                      <div className="todo-title">{item.title}</div>
                      <div className="todo-desc">{item.desc}</div>
                    </div>
                    <Space>
                      <Tag color={item.count > 0 ? 'blue' : 'default'}>{item.count}</Tag>
                      <ArrowRightOutlined className="todo-arrow" />
                    </Space>
                  </div>
                )) : (
                  <Text type="secondary" style={{ padding: 16 }}>暂无待办事项</Text>
                )}
              </div>
            </Card>

            <Card className="app-card enter-up" bordered={false} style={{ animationDelay: '140ms' }} title="最新动态">
              <div className="timeline-list">
                {stats.recentActivities.length > 0 ? stats.recentActivities.map((item, index) => (
                  <div className="timeline-item" key={`${item.title}-${index}`}>
                    <div className="timeline-dot">{index + 1}</div>
                    <div className="timeline-content">
                      <div className="timeline-title">{item.title}</div>
                      <div className="timeline-desc">{item.desc}</div>
                    </div>
                    <div className="timeline-time">{item.time}</div>
                  </div>
                )) : (
                  <Text type="secondary" style={{ padding: 16 }}>暂无操作记录</Text>
                )}
              </div>
            </Card>
          </section>
        </>
      )}

      <section>
        <Card className="app-card enter-up" bordered={false} style={{ animationDelay: '180ms' }} title="常用功能工具箱">
          <div className="tool-grid">
            {visibleToolbox.map((item) => (
              <div key={item.label} className="tool-card" onClick={() => navigate(item.to)} role="button" tabIndex={0}>
                <div className="tool-icon">{item.icon}</div>
                <div className="tool-meta">
                  <div className="tool-title">{item.label}</div>
                  <div className="tool-sub">进入页面</div>
                </div>
                <ArrowRightOutlined className="tool-arrow" />
              </div>
            ))}
          </div>
        </Card>
      </section>
    </div>
  )
}

export default Dashboard
