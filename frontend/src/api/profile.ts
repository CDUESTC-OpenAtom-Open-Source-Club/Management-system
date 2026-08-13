import request from './request'
import type { MyProfileResponse, UpdateMyProfileRequest } from './auth'

export function getMyProfile(): Promise<MyProfileResponse> { return request.get('/api/my/profile') }
export function updateMyProfile(data: UpdateMyProfileRequest): Promise<MyProfileResponse> { return request.put('/api/my/profile', data) }
