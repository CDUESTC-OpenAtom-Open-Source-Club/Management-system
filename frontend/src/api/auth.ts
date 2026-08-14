import request from './request'
import type { CurrentUser } from '../utils/auth'

export interface LoginRequest { username: string; password: string }
export interface LoginResponse { token: string; user: CurrentUser }
export interface ChangePasswordRequest { oldPassword: string; newPassword: string }

export interface UserAccountResponse {
  id: number
  username: string
  memberId?: number
  name?: string
  studentNo?: string
  phone?: string
  major?: string
  department?: string
  position?: string
  cohortId?: number | null
  cohortYear?: number | null
  enabled: boolean
  profileCompleted?: boolean
  initialPasswordChanged?: boolean
  lastLoginAt?: string
  createdAt: string
}

export interface CreateUserRequest { username: string; initialPassword: string; cohortId: number; enabled?: boolean }
export interface BatchCreateAccountItem { username: string; initialPassword: string }
export interface BatchCreateUsersRequest { cohortId: number; accounts: BatchCreateAccountItem[] }
export interface BatchCreateUsersResponse { created: { username: string; memberId: number }[]; failed: { username: string; reason: string }[] }
export interface MyProfileResponse { userId: number; username: string; memberId?: number; name?: string; studentNo?: string; phone?: string; major?: string; department?: string; position?: string; cohortId?: number | null; cohortYear?: number | null; fullAccess: boolean; profileCompleted: boolean; initialPasswordChanged: boolean }
export interface UpdateMyProfileRequest { name?: string; studentNo?: string; phone?: string; major?: string }

export function login(data: LoginRequest): Promise<LoginResponse> { return request.post('/api/auth/login', data) }
export function getMe(): Promise<CurrentUser> { return request.get('/api/auth/me') }
export function changePassword(data: ChangePasswordRequest): Promise<void> { return request.put('/api/auth/me/password', data) }
