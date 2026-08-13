// 通用 API 响应类型
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

// 分页结果
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}
