import request from './request'
import type { PageResult } from '../types/common'
import type { ArchiveLink, ArchiveLinkForm } from '../types/archive'

export function getArchiveLinks(params: {
  year?: number
  type?: string
  cohortId?: number
  keyword?: string
  page?: number
  size?: number
}): Promise<PageResult<ArchiveLink>> {
  return request.get('/api/archive-links', { params })
}

export function createArchiveLink(data: ArchiveLinkForm): Promise<ArchiveLink> {
  return request.post('/api/archive-links', data)
}

export function updateArchiveLink(id: number, data: ArchiveLinkForm): Promise<ArchiveLink> {
  return request.put(`/api/archive-links/${id}`, data)
}

export function deleteArchiveLink(id: number): Promise<void> {
  return request.delete(`/api/archive-links/${id}`)
}
