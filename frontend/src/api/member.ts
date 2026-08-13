import request from './request'
import type { Member, MemberForm } from '../types/member'
import type { PageResult } from '../types/common'

export function getMembers(params: {
  keyword?: string
  page?: number
  size?: number
}): Promise<PageResult<Member>> {
  return request.get('/api/members', { params })
}

export function getMember(id: number): Promise<Member> {
  return request.get(`/api/members/${id}`)
}

export function createMember(data: MemberForm): Promise<Member> {
  return request.post('/api/members', data)
}

export function updateMember(id: number, data: MemberForm): Promise<Member> {
  return request.put(`/api/members/${id}`, data)
}

export function deleteMember(id: number): Promise<void> {
  return request.delete(`/api/members/${id}`)
}
