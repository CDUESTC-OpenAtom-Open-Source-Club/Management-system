import request from './request'
import type { Cohort } from '../types/cohort'

export function getCohorts(): Promise<Cohort[]> {
  return request.get('/api/cohorts')
}

export function createCohort(data: { year: number; enabled?: boolean }): Promise<Cohort> {
  return request.post('/api/cohorts', data)
}

export function updateCohort(id: number, data: { year?: number; enabled?: boolean }): Promise<Cohort> {
  return request.put(`/api/cohorts/${id}`, data)
}

export function deleteCohort(id: number): Promise<void> {
  return request.delete(`/api/cohorts/${id}`)
}
