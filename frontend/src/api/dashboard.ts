import request from './request'
import type { DashboardStats } from '../types/dashboard'

export function getDashboardStats(cohortId?: number): Promise<DashboardStats> {
  return request.get('/api/dashboard/stats', { params: { cohortId } })
}
