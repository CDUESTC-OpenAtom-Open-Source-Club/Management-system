export interface MeetingMinute {
  id: number
  title: string
  meetingDate: string
  meetingYear: number
  meetingMonth: number
  remark?: string
  fileId?: number
  createdBy?: string
  createdAt?: string
  updatedAt?: string
}

export interface MeetingMinuteForm {
  title: string
  meetingDate: string
  remark?: string
  file?: File
}
