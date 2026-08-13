import request from './request'
import type { DashboardStats } from '../types/dashboard'

export function getDashboardStats(): Promise<DashboardStats> {
  return request.get('/api/dashboard/stats')
}
