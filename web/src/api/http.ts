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
  async (error: AxiosError) => {
    const payload = await resolveErrorPayload(error)
    const message = payload?.message || resolveFallbackMessage(error)

    if (payload?.code === 'UNAUTHORIZED' || error.response?.status === 401) {
      clearStoredToken()
      if (!location.pathname.startsWith('/login') && !location.pathname.startsWith('/register')) {
        location.assign('/login')
      }
    }

    ElMessage.error(payload?.traceId ? `${message}（traceId: ${payload.traceId}）` : message)
    return Promise.reject(new Error(message))
  },
)

export function downloadBlob(url: string) {
  return http.get<never, Blob>(url, { responseType: 'blob' })
}

function resolveFallbackMessage(error: AxiosError): string {
  if (error.response?.status === 413) {
    return '文件过大'
  }
  if (error.response?.status) {
    return `请求失败：HTTP ${error.response.status}`
  }
  return error.message || '网络错误'
}

async function resolveErrorPayload(error: AxiosError): Promise<ApiResponse<unknown> | null> {
  const data = error.response?.data
  if (isApiResponse(data)) {
    return data
  }
  if (data instanceof Blob) {
    return parseBlobPayload(data)
  }
  return null
}

async function parseBlobPayload(blob: Blob): Promise<ApiResponse<unknown> | null> {
  if (!blob.type.includes('json')) {
    return null
  }
  try {
    const text = await blob.text()
    const parsed: unknown = JSON.parse(text)
    return isApiResponse(parsed) ? parsed : null
  } catch {
    return null
  }
}

function isApiResponse(value: unknown): value is ApiResponse<unknown> {
  if (!value || typeof value !== 'object') {
    return false
  }
  const candidate = value as Partial<ApiResponse<unknown>>
  return typeof candidate.code === 'string' && typeof candidate.message === 'string'
}

export default http
