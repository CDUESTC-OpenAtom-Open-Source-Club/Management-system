import request from './request'
import { getToken } from '../utils/auth'
import type { PageResult } from '../types/common'
import type { HomeworkAssignment, HomeworkSubmission, GradeRequest } from '../types/homework'

// ---- 作业管理 ----

/** 管理员获取作业列表 */
export function listAssignments(params?: {
  status?: string
  cohortId?: number
  department?: string
  page?: number
  size?: number
}) {
  return request.get<unknown, PageResult<HomeworkAssignment>>('/api/homeworks/manage', { params })
}

/** 成员获取自己的作业列表 */
export function listMyHomework() {
  return request.get<unknown, HomeworkAssignment[]>('/api/homeworks')
}

/** 获取作业详情 */
export function getAssignment(id: number) {
  return request.get<unknown, HomeworkAssignment>(`/api/homeworks/${id}`)
}

/** 创建作业 */
export function createAssignment(data: {
  title: string
  description?: string
  targetType: string
  targetDepartment?: string
  cohortId: number
  deadline: string
  maxPoints?: number
  pointItemId?: number
}) {
  return request.post<unknown, HomeworkAssignment>('/api/homeworks', data)
}

/** 编辑作业 */
export function updateAssignment(id: number, data: {
  title: string
  description?: string
  targetType: string
  targetDepartment?: string
  cohortId: number
  deadline: string
  maxPoints?: number
  pointItemId?: number
}) {
  return request.put<unknown, HomeworkAssignment>(`/api/homeworks/${id}`, data)
}

/** 发布作业 */
export function publishAssignment(id: number) {
  return request.post<unknown, HomeworkAssignment>(`/api/homeworks/${id}/publish`)
}

/** 关闭作业 */
export function closeAssignment(id: number) {
  return request.post<unknown, HomeworkAssignment>(`/api/homeworks/${id}/close`)
}

/** 删除作业 */
export function deleteAssignment(id: number) {
  return request.delete<unknown, void>(`/api/homeworks/${id}`)
}

// ---- 提交与批改 ----

/** 成员提交作业（multipart/form-data） */
export async function submitHomework(homeworkId: number, content: string, files: File[]): Promise<HomeworkSubmission> {
  const token = getToken()
  const formData = new FormData()
  if (content) formData.append('content', content)
  files.forEach((f) => formData.append('files', f))
  const resp = await fetch(`/api/homeworks/${homeworkId}/submit`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: formData,
  })
  if (!resp.ok) {
    const err = await resp.json().catch(() => ({ message: '提交失败' }))
    throw new Error(err.message || '提交失败')
  }
  const json = await resp.json()
  if (json.code !== 0) throw new Error(json.message || '提交失败')
  return json.data
}

/** 成员查看自己的提交 */
export function getMySubmission(homeworkId: number) {
  return request.get<unknown, HomeworkSubmission>(`/api/homeworks/${homeworkId}/my-submission`)
}

/** 管理员获取提交列表 */
export function listSubmissions(homeworkId: number, status?: string, page = 1, size = 20) {
  return request.get<unknown, PageResult<HomeworkSubmission>>(
    `/api/homeworks/${homeworkId}/submissions`,
    { params: { status, page, size } }
  )
}

/** 管理员获取单个提交 */
export function getSubmission(id: number) {
  return request.get<unknown, HomeworkSubmission>(`/api/homework-submissions/${id}`)
}

/** 批改作业 */
export function gradeSubmission(id: number, data: GradeRequest) {
  return request.post<unknown, HomeworkSubmission>(`/api/homework-submissions/${id}/grade`, data)
}

/** 下载作业附件 URL */
export function getDownloadFileUrl(submissionId: number, fileId: number) {
  return `/api/homework-submissions/${submissionId}/files/${fileId}/download`
}

/** 在线查看作业附件 URL */
export function getViewFileUrl(submissionId: number, fileId: number) {
  return `/api/homework-submissions/${submissionId}/files/${fileId}/view`
}
