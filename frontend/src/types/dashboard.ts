export interface DepartmentStat {
  label: string
  value: number
}

export interface WeeklyTrend {
  week: number
  count: number
}

export interface PendingTask {
  type?: string
  title: string
  desc: string
  count: number
  to: string
}

export interface RecentActivity {
  operatorName?: string
  operatorDepartment?: string
  moduleName?: string
  actionType?: string
  title: string
  desc: string
  time: string
}

export interface DashboardStats {
  totalMembers: number
  totalPointItems: number
  pendingApplications: number
  totalMeetingMinutes: number
  totalFinancePeriods: number
  pendingHomeworkReviews: number
  currentMonthFinancePending: number
  departmentDistribution: DepartmentStat[]
  weeklyTrend: WeeklyTrend[]
  pendingTasks: PendingTask[]
  recentActivities: RecentActivity[]
}
