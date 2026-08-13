import React from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { getCurrentUser, getToken } from '../utils/auth'

interface Props { children: React.ReactNode }
const AuthGuard: React.FC<Props> = ({ children }) => { const location = useLocation(); if (!getToken() || !getCurrentUser()) return <Navigate to="/login" state={{ from: location }} replace />; return <>{children}</> }
export default AuthGuard
