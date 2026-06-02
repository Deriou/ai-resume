import http from './http'
import type { PageVO } from '@/types/api'
import type { HotCompanyVO, HotJobVO, JobCreateRequest, JobUpdateRequest, JobVO } from '@/types/job'

export function listOpenJobs(page = 1, size = 10) {
  return http.get<never, PageVO<JobVO>>('/jobs', { params: { page, size } })
}

export function listMyJobs(page = 1, size = 10) {
  return http.get<never, PageVO<JobVO>>('/jobs/mine', { params: { page, size } })
}

export function getJob(id: number) {
  return http.get<never, JobVO>(`/jobs/${id}`)
}

export function createJob(data: JobCreateRequest) {
  return http.post<never, JobVO>('/jobs', data)
}

export function updateJob(id: number, data: JobUpdateRequest) {
  return http.put<never, JobVO>(`/jobs/${id}`, data)
}

export function closeJob(id: number) {
  return http.patch<never, JobVO>(`/jobs/${id}/close`)
}

export function listHotJobs() {
  return http.get<never, HotJobVO[]>('/hot/jobs')
}

export function listHotCompanies() {
  return http.get<never, HotCompanyVO[]>('/hot/companies')
}
