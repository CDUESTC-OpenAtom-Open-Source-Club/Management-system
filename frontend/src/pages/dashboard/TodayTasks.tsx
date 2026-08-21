import React from 'react'
import { TeamOutlined, CheckCircleOutlined, EditOutlined, AccountBookOutlined, ArrowRightOutlined, CheckOutlined } from '@ant-design/icons'
import type { PendingTask } from '../../types/dashboard'

interface TaskMeta {
  color: string
  soft: string
  icon: React.ReactNode
  action: string
}

const TASK_META: Record<string, TaskMeta> = {
  profile: { color: '#7f56d9', soft: '#f4f3ff', icon: <TeamOutlined />, action: '去处理' },
  review: { color: '#2f6bff', soft: '#eef4ff', icon: <CheckCircleOutlined />, action: '去审核' },
  homework: { color: '#f79009', soft: '#fff6ed', icon: <EditOutlined />, action: '去批改' },
  finance: { color: '#12b76a', soft: '#ecfdf3', icon: <AccountBookOutlined />, action: '去记录' },
}

/** 折叠为零待办时用的短业务名 */
const ZERO_SHORT: Record<string, string> = {
  profile: '资料完善',
  review: '积分审核',
  homework: '作业批改',
  finance: '财务台账',
}

/** 单个零待办时的完整表述 */
const ZERO_SINGLE: Record<string, string> = {
  profile: '资料完善暂无待处理',
  review: '积分审核暂无待处理',
  homework: '作业批改暂无待处理',
  finance: '本月财务暂无待更新',
}

function metaOf(task: PendingTask): TaskMeta {
  return TASK_META[task.type ?? ''] ?? { color: '#2f6bff', soft: '#eef4ff', icon: <CheckCircleOutlined />, action: '去处理' }
}

function shortName(task: PendingTask): string {
  return ZERO_SHORT[task.type ?? ''] ?? task.title
}

function gridClass(count: number): string {
  if (count >= 5) return 'count-many'
  return `count-${count}`
}

interface Props {
  tasks: PendingTask[]
  onNavigate: (to: string) => void
}

const TodayTasks: React.FC<Props> = ({ tasks, onNavigate }) => {
  const total = tasks.reduce((s, t) => s + t.count, 0)
  const nonZero = tasks.filter((t) => t.count > 0)
  const zero = tasks.filter((t) => t.count <= 0)

  const doneText =
    zero.length === 1
      ? ZERO_SINGLE[zero[0].type ?? ''] ?? `${shortName(zero[0])}暂无待处理`
      : `${zero.map(shortName).join('、')}目前均无待处理`

  return (
    <div className="today-tasks">
      <div className="today-tasks-head">
        <h3 className="section-title">今日待处理</h3>
        <span className="today-tasks-count">共 {total} 条待办</span>
      </div>

      {total === 0 ? (
        <div className="today-tasks-empty">
          <div className="today-tasks-empty-title">今天暂无待处理事项</div>
          <div className="today-tasks-empty-sub">所有待办均已处理完成。</div>
        </div>
      ) : (
        <>
          <div className={`task-grid ${gridClass(nonZero.length)}`}>
            {nonZero.map((task) => {
              const meta = metaOf(task)
              return (
                <div
                  key={task.type || task.title}
                  className="task-card"
                  role="button"
                  tabIndex={0}
                  onClick={() => onNavigate(task.to)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault()
                      onNavigate(task.to)
                    }
                  }}
                >
                  <div className="task-card-head">
                    <span className="task-icon" style={{ background: meta.soft, color: meta.color }}>
                      {meta.icon}
                    </span>
                    <span className="task-title">{task.title}</span>
                    <span className="task-count">{task.count}</span>
                  </div>
                  <div className="task-desc">{task.desc}</div>
                  <div className="task-action" style={{ color: meta.color }}>
                    {meta.action} <ArrowRightOutlined />
                  </div>
                </div>
              )
            })}
          </div>

          {zero.length > 0 && (
            <div className="today-tasks-done">
              <CheckOutlined />
              <span>{doneText}</span>
            </div>
          )}
        </>
      )}
    </div>
  )
}

export default TodayTasks
