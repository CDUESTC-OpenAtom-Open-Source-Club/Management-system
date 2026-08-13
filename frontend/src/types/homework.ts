// 作业管理相关类型

export interface HomeworkAssignment {
  id: number
  title: string
  description?: string
  targetType: 'ALL' | 'DEPARTMENT'
  targetDepartment?: string
  deadline: string
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED'
  maxPoints?: number
  pointItemId?: number
  pointItemName?: string
  createdByUserId?: number
  createdByName?: string
  submissionCount: number
  submittedCount: number
  gradedCount: number
  createdAt?: string
  updatedAt?: string
}

export interface HomeworkAssignmentForm {
  title: string
  description?: string
  targetType: 'ALL' | 'DEPARTMENT'
  targetDepartment?: string
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
