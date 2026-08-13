export interface FinancePeriod {
  id: number
  financeYear: number
  financeMonth: number
  createdAt?: string
  updatedAt?: string
}

export interface FinanceFile {
  id: number
  periodId: number
  fileId: number
  fileType: 'REPORT' | 'VOUCHER'
  remark?: string
  originalName?: string
  fileSize?: number
  contentType?: string
  uploadedBy?: string
  createdAt?: string
}

export interface FinanceMonthDetail {
  period: FinancePeriod | null
  report: FinanceFile | null
  vouchers: FinanceFile[]
}
