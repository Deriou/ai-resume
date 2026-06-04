import type { ApplicationStatus } from './api'
import type { ResumeFileVO } from './resume'

export interface ApplicationVO {
  id: number
  userId: number
  resumeId: number
  resumeTitle: string
  resumeContentMd: string | null
  resumeFiles: ResumeFileVO[]
  jobId: number
  jobTitle: string
  status: ApplicationStatus
  remark: string | null
  reviewedBy: number | null
  reviewedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface ApplicationCreateRequest {
  resumeId: number
  jobId: number
  remark?: string
}

export interface ApplicationReviewRequest {
  status: Exclude<ApplicationStatus, 'PENDING'>
  remark?: string
}
