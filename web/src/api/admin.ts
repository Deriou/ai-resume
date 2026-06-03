import http from './http'
import type { ApplicationStatus, JobStatus, PageVO, UserRole, UserStatus } from '@/types/api'
import type {
  AdminApplicationVO,
  AdminApplicationSummaryVO,
  AdminCreditDailyVO,
  AdminCreditGrantRequest,
  AdminCreditTopUserVO,
  AdminJobVO,
  AdminJobSummaryVO,
  AdminLlmDailyVO,
  AdminLlmOperationVO,
  AdminOverviewVO,
  AdminUserVO,
} from '@/types/admin'
import type { CreditTransactionVO } from '@/types/credit'

export function fetchAdminOverview() {
  return http.get<never, AdminOverviewVO>('/admin/overview')
}

export function fetchAdminLlmDaily() {
  return http.get<never, AdminLlmDailyVO[]>('/admin/llm/daily')
}

export function fetchAdminLlmOperations() {
  return http.get<never, AdminLlmOperationVO[]>('/admin/llm/operations')
}

export function fetchAdminCreditDaily() {
  return http.get<never, AdminCreditDailyVO[]>('/admin/credits/daily')
}

export function fetchAdminCreditTopUsers(limit = 5) {
  return http.get<never, AdminCreditTopUserVO[]>('/admin/credits/top-users', { params: { limit } })
}

export function fetchAdminUsers(
  page = 1,
  size = 10,
  role?: UserRole,
  status?: UserStatus,
  keyword?: string,
) {
  return http.get<never, PageVO<AdminUserVO>>('/admin/users', { params: { page, size, role, status, keyword } })
}

export function grantUserCredit(userId: number, data: AdminCreditGrantRequest) {
  return http.post<never, CreditTransactionVO>(`/admin/users/${userId}/credits/grant`, data)
}

export function fetchAdminJobs(page = 1, size = 10, status?: JobStatus, keyword?: string) {
  return http.get<never, PageVO<AdminJobVO>>('/admin/jobs', { params: { page, size, status, keyword } })
}

export function fetchAdminJobSummary() {
  return http.get<never, AdminJobSummaryVO>('/admin/jobs/summary')
}

export function fetchAdminApplicationSummary() {
  return http.get<never, AdminApplicationSummaryVO>('/admin/applications/summary')
}

export function fetchAdminApplications(page = 1, size = 10, status?: ApplicationStatus) {
  return http.get<never, PageVO<AdminApplicationVO>>('/admin/applications', { params: { page, size, status } })
}
