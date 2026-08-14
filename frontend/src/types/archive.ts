import type { Cohort } from './cohort'

export interface ArchiveLink {
  id: number
  title: string
  archiveYear: number
  archiveType: string
  url: string
  description?: string
  cohorts?: Cohort[]
  createdBy?: string
  createdAt?: string
  updatedAt?: string
}

export interface ArchiveLinkForm {
  title: string
  archiveYear: number
  archiveType: string
  url: string
  description?: string
  cohortIds?: number[]
}
