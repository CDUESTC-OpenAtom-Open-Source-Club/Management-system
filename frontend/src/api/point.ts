import request from './request'
import type { PageResult } from '../types/common'
import type {
  PointItem,
  PointApplication,
  PointRecord,
  PointsTableResponse,
  SearchPositionResponse,
  PointRecordForm,
} from '../types/point'

// ── 积分项目 ──────────────────────────────────────────────
export function getPointItems(): Promise<PointItem[]> {
  return request.get('/api/point-items')
}

export function getPointApplyOptions(): Promise<PointItem[]> {
  return request.get('/api/point-items/apply-options')
}

export function createPointItem(data: Partial<PointItem>): Promise<PointItem> {
  return request.post('/api/point-items', data)
}

export function updatePointItem(id: number, data: Partial<PointItem>): Promise<PointItem> {
  return request.put(`/api/point-items/${id}`, data)
}

export function deletePointItem(id: number): Promise<void> {
  return request.delete(`/api/point-items/${id}`)
}

// ── 活动登记 ──────────────────────────────────────────────
export function submitPointApplications(data: {
  memberId: number
  pointItemIds: number[]
}): Promise<PointApplication[]> {
  return request.post('/api/point-applications', data)
}

export function getMyPointApplications(memberId: number): Promise<PointApplication[]> {
  return request.get('/api/point-applications/my', { params: { memberId } })
}

export function getPointApplications(params: {
  status?: string
  keyword?: string
  page?: number
  size?: number
}): Promise<PageResult<PointApplication>> {
  return request.get('/api/point-applications', { params })
}

export function approvePointApplication(id: number): Promise<void> {
  return request.post(`/api/point-applications/${id}/approve`)
}

export function rejectPointApplication(id: number, reviewComment: string): Promise<void> {
  return request.post(`/api/point-applications/${id}/reject`, { reviewComment })
}

// ── 积分总表 ──────────────────────────────────────────────
export function getPointsTable(params: {
  page?: number
  size?: number
  keyword?: string
}): Promise<PointsTableResponse> {
  return request.get('/api/points/table', { params })
}

export function searchPointTablePosition(params: {
  keyword: string
  pageSize: number
  matchIndex: number
}): Promise<SearchPositionResponse> {
  return request.get('/api/points/table/search-position', { params })
}

// ── 积分记录 ──────────────────────────────────────────────
export function getMemberPointRecords(memberId: number): Promise<PointRecord[]> {
  return request.get(`/api/members/${memberId}/point-records`)
}

export function createMemberPointRecord(
  memberId: number,
  data: PointRecordForm
): Promise<PointRecord> {
  return request.post(`/api/members/${memberId}/point-records`, data)
}

export function updatePointRecord(id: number, data: PointRecordForm): Promise<PointRecord> {
  return request.put(`/api/point-records/${id}`, data)
}

export function deletePointRecord(id: number): Promise<void> {
  return request.delete(`/api/point-records/${id}`)
}
