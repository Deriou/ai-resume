import type { JobStatus } from './api'

export interface JobVO {
  id: number
  enterpriseId: number
  title: string
  jdContent: string
  techStack: string
  location: string
  status: JobStatus
  createdAt: string
  updatedAt: string
}

export interface JobCreateRequest {
  title: string
  jdContent: string
  techStack?: string
  location?: string
}

export type JobUpdateRequest = JobCreateRequest

export interface HotJobVO {
  jobId: number
  title: string
  enterpriseId: number
  location: string
  techStack: string
  applicationCount: number
}

export interface HotCompanyVO {
  enterpriseId: number
  enterpriseName: string
  openJobCount: number
  applicationCount: number
}
