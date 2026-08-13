export interface DepartmentStat {
  label: string
  value: number
}

export interface WeeklyTrend {
  week: number
  count: number
}

export interface PendingTask {
  title: string
  desc: string
  count: number
  to: string
}

export interface RecentActivity {
  title: string
  desc: string
  time: string
}

export interface DashboardStats {
  totalMembers: number
  totalPointItems: number
  pendingApplications: number
  totalArchiveLinks: number
  totalMeetingMinutes: number
  totalFinancePeriods: number
  departmentDistribution: DepartmentStat[]
  weeklyTrend: WeeklyTrend[]
  pendingTasks: PendingTask[]
  recentActivities: RecentActivity[]
}
