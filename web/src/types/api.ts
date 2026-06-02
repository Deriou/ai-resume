export type UserRole = 'USER' | 'ENTERPRISE' | 'ADMIN'

export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  traceId?: string
}

export interface PageVO<T> {
  records: T[]
  page: number
  size: number
  total: number
  pages: number
}

export type JobStatus = 'OPEN' | 'CLOSED'
export type ApplicationStatus = 'PENDING' | 'VIEWED' | 'ACCEPTED' | 'REJECTED'
export type UserStatus = 'ACTIVE' | 'DISABLED'
