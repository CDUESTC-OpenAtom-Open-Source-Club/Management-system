// 作业管理相关类型

export interface HomeworkAssignment {
  id: number
  title: string
  description?: string
  targetType: 'ALL' | 'DEPARTMENT'
  targetDepartment?: string
  cohortId?: number | null
  cohortYear?: number | null
  deadline: string
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED'
  maxPoints?: number
  pointItemId?: number
  pointItemName?: string
  createdByUserId?: number
  createdByName?: string
  /** 所有有效提交总数（含已批改）= 提交人数；批改不会减少 */
  submissionCount: number
  /** 当前仍处于 SUBMITTED 状态 = 待批改数；成员视角「我的作业」被后端覆盖为本人是否已提交 0/1 */
  submittedCount: number
  /** 已批改数 */
  gradedCount: number
  attachments?: AssignmentFileInfo[]
  createdAt?: string
  updatedAt?: string
}

export interface AssignmentFileInfo {
  id: number
  fileId: number
  originalName: string
  fileSize?: number
  contentType?: string
  createdAt?: string
}

export interface HomeworkAssignmentForm {
  title: string
  description?: string
  targetType: 'ALL' | 'DEPARTMENT'
  targetDepartment?: string
  cohortId: number
  deadline: string
  maxPoints?: number
  pointItemId?: number
}

export interface SubmissionFileInfo {
  id: number
  fileId: number
  originalName: string
  fileSize?: number
  contentType?: string
}

export interface HomeworkSubmission {
  id: number
  homeworkId: number
  homeworkTitle?: string
  memberId: number
  memberName?: string
  memberStudentNo?: string
  memberDepartment?: string
  memberPosition?: string
  content?: string
  status: 'SUBMITTED' | 'GRADED'
  submittedAt: string
  reviewComment?: string
  awardedPoints?: number
  reviewedByUserId?: number
  reviewedByName?: string
  reviewedAt?: string
  pointRecordId?: number
  files: SubmissionFileInfo[]
  createdAt?: string
  updatedAt?: string
}

export interface GradeRequest {
  points: number
  comment?: string
}
