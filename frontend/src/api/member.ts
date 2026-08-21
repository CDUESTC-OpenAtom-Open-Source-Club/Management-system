import request from './request'
import type { Member, MemberForm, BatchDeleteMembersResult } from '../types/member'
import type { PageResult } from '../types/common'

export function getMembers(params: {
  keyword?: string
  cohortId?: number
  page?: number
  size?: number
}): Promise<PageResult<Member>> {
  return request.get('/api/members', { params })
}

export function getMember(id: number): Promise<Member> {
  return request.get(`/api/members/${id}`)
}

export function updateMember(id: number, data: MemberForm): Promise<Member> {
  return request.put(`/api/members/${id}`, data)
}

export function deleteMember(id: number): Promise<void> {
  return request.delete(`/api/members/${id}`)
}

export function batchSetCohort(memberIds: number[], cohortId: number): Promise<number> {
  return request.put('/api/members/batch-cohort', { memberIds, cohortId })
}

export function batchDeleteMembers(memberIds: number[]): Promise<BatchDeleteMembersResult> {
  return request.post('/api/members/batch-delete', { memberIds })
}
