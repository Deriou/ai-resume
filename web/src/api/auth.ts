import http from './http'
import type { CaptchaResponse, LoginRequest, LoginResponse, RegisterRequest } from '@/types/auth'

export function fetchCaptcha() {
  return http.get<never, CaptchaResponse>('/auth/captcha')
}

export function login(data: LoginRequest) {
  return http.post<never, LoginResponse>('/auth/login', data)
}

export function register(data: RegisterRequest) {
  return http.post<never, void>('/auth/register', data)
}

export function fetchMe() {
  return http.get<never, LoginResponse>('/auth/me')
}

export function logout() {
  return http.post<never, void>('/auth/logout')
}
