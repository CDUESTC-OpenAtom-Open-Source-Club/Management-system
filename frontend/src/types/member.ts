export interface Member {
  id: number
  name: string
  studentNo: string
  phone: string
  major: string
  department: string
  position: string
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
}
