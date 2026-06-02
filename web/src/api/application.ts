import http from './http'
import type { PageVO } from '@/types/api'
import type { ApplicationCreateRequest, ApplicationReviewRequest, ApplicationVO } from '@/types/application'

export function createApplication(data: ApplicationCreateRequest) {
  return http.post<never, ApplicationVO>('/applications', data)
}

export function listMyApplications(page = 1, size = 10) {
  return http.get<never, PageVO<ApplicationVO>>('/applications/my', { params: { page, size } })
}

export function listReceivedApplications(page = 1, size = 10) {
  return http.get<never, PageVO<ApplicationVO>>('/applications/received', { params: { page, size } })
}

export function reviewApplication(id: number, data: ApplicationReviewRequest) {
  return http.patch<never, ApplicationVO>(`/applications/${id}/status`, data)
}
