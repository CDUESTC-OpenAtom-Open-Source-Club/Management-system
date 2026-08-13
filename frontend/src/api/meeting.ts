import request from './request'
import type { PageResult } from '../types/common'
import type { MeetingMinute } from '../types/meeting'

export function getMeetingMinutes(params: {
  year?: number
  month?: number
  keyword?: string
  page?: number
  size?: number
}): Promise<PageResult<MeetingMinute>> {
  return request.get('/api/meeting-minutes', { params })
}

export function createMeetingMinute(formData: FormData): Promise<MeetingMinute> {
  return request.post('/api/meeting-minutes', formData)
}

export function updateMeetingMinute(id: number, formData: FormData): Promise<MeetingMinute> {
  return request.put(`/api/meeting-minutes/${id}`, formData)
}

export function deleteMeetingMinute(id: number): Promise<void> {
  return request.delete(`/api/meeting-minutes/${id}`)
}

// 下载接口返回 blob，由 download.ts 处理
export function getMeetingDownloadUrl(id: number): string {
  return `/api/meeting-minutes/${id}/download`
}
