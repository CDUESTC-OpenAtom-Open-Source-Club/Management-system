export interface OperationLog {
  id: number
  operatorName: string
  operatorDepartment?: string
  operatorPosition?: string
  moduleName: string
  actionType: string
  targetId?: string
  description?: string
  createdAt?: string
}
