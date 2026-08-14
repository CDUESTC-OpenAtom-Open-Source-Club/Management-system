export interface Member {
  id: number
  name: string
  studentNo: string
  phone: string
  major: string
  department: string
  position: string
  cohortId?: number | null
  cohortYear?: number | null
  createdAt?: string
  updatedAt?: string
}

export interface MemberForm {
  name: string
  studentNo: string
  phone: string
  major: string
  department?: string
  position?: string
  cohortId?: number | null
}

export interface BatchDeleteMembersResult {
  deletedMemberCount: number
  deletedPointApplicationCount: number
  deletedPointRecordCount: number
  disabledAccountCount: number
}
