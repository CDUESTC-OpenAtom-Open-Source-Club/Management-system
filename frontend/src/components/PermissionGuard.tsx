import React from 'react'
import { Navigate } from 'react-router-dom'
import { isFullAccess } from '../utils/auth'

interface Props {
  children: React.ReactNode
  allowed?: boolean
  message?: string
}

const PermissionGuard: React.FC<Props> = ({ children, allowed, message }) => {
  const permitted = typeof allowed === 'boolean' ? allowed : isFullAccess()
  if (!permitted) return <Navigate to="/" replace state={message ? { message } : undefined} />
  return <>{children}</>
}

export default PermissionGuard
