import type { Cohort } from './cohort'
import type { PointItemType } from '../utils/pointItemTypes'

export interface PointItem {
  id: number
  itemName: string
  pointValue: number
  itemType: PointItemType
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
  itemType?: PointItemType
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
  sourceType: 'APPLICATION' | 'MANUAL' | 'HOMEWORK'
  operatorName?: string
  occurredAt?: string
  createdAt?: string
}

export interface PointsTableRow {
  rankNo: number
  memberId: number
  name: string
  studentNo?: string
  totalScore: number
  activityScore: number
  competitionScore: number
  openSourceLearningScore: number
  communityContributionScore: number
  speechHostingScore: number
  otherScore: number
}

export interface PointsTableResponse {
  rows: PointsTableRow[]
  page: number
  size: number
  total: number
}

export interface PointDetail {
  id: number
  memberId: number
  pointItemId?: number
  pointItemName?: string
  pointItemType?: PointItemType
  score: number
  sourceType: 'APPLICATION' | 'MANUAL' | 'HOMEWORK'
  sourceLabel?: string
  reason?: string
  operatorName?: string
  occurredAt?: string
  createdAt?: string
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
