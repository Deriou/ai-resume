import axios, { type AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types/api'

const TOKEN_KEY = 'airesume_token'

export function getStoredToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setStoredToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearStoredToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export const http = axios.create({
  baseURL: '/api',
  timeout: 120000,
})

http.interceptors.request.use((config) => {
  const token = getStoredToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>
    if (payload?.code === 'OK') {
      return payload.data as never
    }

    const message = payload?.message || '请求失败'
    if (payload?.code === 'UNAUTHORIZED') {
      clearStoredToken()
      if (!location.pathname.startsWith('/login') && !location.pathname.startsWith('/register')) {
        location.assign('/login')
      }
    }
    ElMessage.error(payload?.traceId ? `${message}（traceId: ${payload.traceId}）` : message)
    return Promise.reject(new Error(message))
  },
  (error: AxiosError) => {
    const message = error.response?.status === 413 ? '文件过大' : error.message || '网络错误'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export function downloadBlob(url: string) {
  return http.get<never, Blob>(url, { responseType: 'blob' })
}

export default http
