import request from './request'
import type { PageResult } from '../types/common'
import type { OperationLog } from '../types/log'

export function getOperationLogs(params: {
  moduleName?: string
  actionType?: string
  keyword?: string
  page?: number
  size?: number
}): Promise<PageResult<OperationLog>> {
  return request.get('/api/operation-logs', { params })
}
