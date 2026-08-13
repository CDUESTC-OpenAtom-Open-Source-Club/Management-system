import request from './request'
import type { FinancePeriod, FinanceMonthDetail } from '../types/finance'

export function getFinancePeriods(year?: number): Promise<FinancePeriod[]> {
  return request.get('/api/finance/periods', { params: year ? { year } : {} })
}

export function getFinanceMonthDetail(year: number, month: number): Promise<FinanceMonthDetail> {
  return request.get(`/api/finance/${year}/${month}`)
}

export function uploadFinanceReport(
  year: number,
  month: number,
  formData: FormData
): Promise<unknown> {
  return request.post(`/api/finance/${year}/${month}/report`, formData)
}

export function uploadFinanceVouchers(
  year: number,
  month: number,
  formData: FormData
): Promise<unknown> {
  return request.post(`/api/finance/${year}/${month}/vouchers`, formData)
}

export function getFinanceDownloadUrl(financeFileId: number): string {
  return `/api/finance/files/${financeFileId}/download`
}

export function getFinanceViewUrl(financeFileId: number): string {
  return `/api/finance/files/${financeFileId}/view`
}

export function deleteFinanceFile(financeFileId: number): Promise<void> {
  return request.delete(`/api/finance/files/${financeFileId}`)
}
