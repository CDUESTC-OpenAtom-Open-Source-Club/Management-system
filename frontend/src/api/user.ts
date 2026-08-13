import request from './request'
import type { BatchCreateUsersRequest, BatchCreateUsersResponse, CreateUserRequest, UserAccountResponse } from './auth'
import type { PageResult } from '../types/common'

export function getUsers(params: { keyword?: string; page?: number; size?: number }): Promise<PageResult<UserAccountResponse>> { return request.get('/api/users', { params }) }
export function createUser(data: CreateUserRequest): Promise<UserAccountResponse> { return request.post('/api/users', data) }
export function batchCreateUsers(data: BatchCreateUsersRequest): Promise<BatchCreateUsersResponse> { return request.post('/api/users/batch', data) }
export function updateUserEnabled(id: number, enabled: boolean): Promise<void> { return request.put(`/api/users/${id}/enabled`, { enabled }) }
export function resetUserPassword(id: number, newPassword: string): Promise<void> { return request.post(`/api/users/${id}/reset-password`, { newPassword }) }
export function deleteUser(id: number): Promise<void> { return request.delete(`/api/users/${id}`) }
