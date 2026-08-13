// auth 工具：JWT token 和用户信息的本地存储

const TOKEN_KEY = 'club_token'
const USER_KEY = 'club_user'

export interface CurrentUser {
  userId: number
  username: string
  memberId?: number
  name?: string
  studentNo?: string
  phone?: string
  major?: string
  department?: string
  position?: string
  fullAccess?: boolean
  profileCompleted?: boolean
  initialPasswordChanged?: boolean
}

export function getToken(): string | null { return localStorage.getItem(TOKEN_KEY) }
export function setToken(token: string): void { localStorage.setItem(TOKEN_KEY, token) }
export function removeToken(): void { localStorage.removeItem(TOKEN_KEY) }
export function getCurrentUser(): CurrentUser | null { try { const raw = localStorage.getItem(USER_KEY); return raw ? JSON.parse(raw) as CurrentUser : null } catch { return null } }
export function setCurrentUser(user: CurrentUser): void { localStorage.setItem(USER_KEY, JSON.stringify(user)) }
export function clearAuth(): void { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY) }
export function isLoggedIn(): boolean { return !!getToken() && !!getCurrentUser() }
export function isFullAccess(): boolean { return getCurrentUser()?.fullAccess === true }
