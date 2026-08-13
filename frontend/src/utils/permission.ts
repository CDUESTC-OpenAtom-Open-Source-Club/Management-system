import { getCurrentUser } from './auth'

/** 是否具有全部管理权限（会长、副会长、秘书处） */
export function isFullAccess(): boolean {
  return getCurrentUser()?.fullAccess === true
}

/** 向下兼容：isAdmin = isFullAccess */
export function isAdmin(): boolean {
  return isFullAccess()
}

export function isPresident(): boolean {
  return getCurrentUser()?.position === '会长'
}

export function isVicePresident(): boolean {
  return getCurrentUser()?.position === '副会长'
}

export function isSecretary(): boolean {
  return getCurrentUser()?.department === '秘书处'
}

/** 可管理：积分项目、积分审核、成员管理 */
export function canManage(): boolean {
  return isFullAccess()
}

/** 可查看财务台账 */
export function canViewFinance(): boolean {
  return isFullAccess()
}

/** 可查看操作日志 */
export function canViewLogs(): boolean {
  return isFullAccess()
}

// ======================== 作业模块权限 ========================

/** 是否是部长 */
export function isMinister(): boolean {
  return getCurrentUser()?.position === '部长'
}

/** 是否可以管理作业（查看批改列表、发布作业等）：fullAccess 或 部长 */
export function canManageHomework(): boolean {
  return isFullAccess() || isMinister()
}

/** 是否可以批改作业 */
export function canReviewHomework(): boolean {
  return canManageHomework()
}
