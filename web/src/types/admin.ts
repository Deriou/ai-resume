import type { ApplicationStatus, JobStatus, UserRole, UserStatus } from './api'

export interface AdminOverviewVO {
  totalUsers: number
  userCount: number
  enterpriseCount: number
  adminCount: number
  resumeCount: number
  jobCount: number
  openJobCount: number
  applicationCount: number
  todayAiCalls: number
  todayTokens: number
  todayCreditCost: number
  todayAvgLatencyMs: number
  todayAiFailures: number
}

export interface AdminLlmDailyVO {
  date: string
  callCount: number
  successCount: number
  failureCount: number
  totalTokens: number
  creditCost: number
  avgLatencyMs: number
}

export interface AdminLlmOperationVO {
  operation: string
  callCount: number
  totalTokens: number
  creditCost: number
  avgLatencyMs: number
}

export interface AdminCreditDailyVO {
  date: string
  grantedCredits: number
  consumedCredits: number
  checkInCredits: number
  adminGrantCredits: number
  aiConsumedCredits: number
}

export interface AdminCreditTopUserVO {
  userId: number
  username: string
  nickName: string
  consumedCredits: number
  totalTokens: number
  aiCallCount: number
}

export interface AdminUserVO {
  id: number
  username: string
  role: UserRole
  nickName: string
  creditBalance: number | null
  status: UserStatus
  createdAt: string
  updatedAt: string
}

export interface AdminJobVO {
  id: number
  enterpriseId: number
  enterpriseName: string
  title: string
  techStack: string
  location: string
  status: JobStatus
  applicationCount: number
  createdAt: string
  updatedAt: string
}

export interface AdminApplicationVO {
  id: number
  userId: number
  username: string
  nickName: string
  resumeId: number
  resumeTitle: string
  jobId: number
  jobTitle: string
  enterpriseId: number
  enterpriseName: string
  status: ApplicationStatus
  remark: string | null
  reviewedBy: number | null
  reviewedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface AdminCreditGrantRequest {
  amount: number
  remark?: string
}
