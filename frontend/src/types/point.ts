import type { Cohort } from './cohort'

export interface PointItem {
  id: number
  itemName: string
  pointValue: number
  itemType: string
  description: string
  sortOrder: number
  enabled: boolean
  allowMemberApply: boolean
  cohorts?: Cohort[]
  createdAt?: string
}

export interface PointItemForm {
  itemName: string
  pointValue: number
  itemType?: string
  description?: string
  sortOrder?: number
  enabled?: boolean
  allowMemberApply?: boolean
  cohortIds?: number[]
}

export interface PointApplication {
  id: number
  memberId: number
  memberName?: string
  studentNo?: string
  pointItemId: number
  itemName?: string
  pointValue?: number
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  reviewComment?: string
  reviewedAt?: string
  reviewedBy?: string
  createdAt?: string
}

export interface PointRecord {
  id: number
  memberId: number
  pointItemId?: number
  itemName?: string
  applicationId?: number
  score: number
  reason: string
  sourceType: 'APPLICATION' | 'MANUAL'
  operatorName?: string
  occurredAt?: string
  createdAt?: string
}

export interface PointsTableColumn {
  pointItemId: number
  itemName: string
  pointValue: number
}

export interface PointsTableRow {
  rankNo: number
  memberId: number
  name: string
  studentNo: string
  phone: string
  major: string
  department: string
  position: string
  scores: Record<string, number>
  totalScore: number
}

export interface PointsTableResponse {
  columns: PointsTableColumn[]
  rows: PointsTableRow[]
  page: number
  size: number
  total: number
}

export interface SearchPositionResponse {
  keyword: string
  matchCount: number
  matchIndex: number
  memberId: number
  name: string
  studentNo: string
  rankNo: number
  pageNo: number
  rowNoInPage: number
  totalScore: number
}

export interface PointRecordForm {
  pointItemId?: number
  score: number
  reason: string
  occurredAt?: string
}
