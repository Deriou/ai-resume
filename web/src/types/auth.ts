import type { UserRole } from './api'

export interface CaptchaResponse {
  uuid: string
  imageBase64: string
}

export interface LoginRequest {
  username: string
  password: string
  captchaUuid: string
  captchaCode: string
}

export interface RegisterRequest extends LoginRequest {
  nickName: string
  role: Exclude<UserRole, 'ADMIN'>
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  role: UserRole
  nickName: string
  creditBalance: number | null
}
