import http from './http'
import type {
  JobMatchRequest,
  JobMatchVO,
  ResumeOptimizeRequest,
  ResumeOptimizeVO,
  ResumeScoreRequest,
  ResumeScoreVO,
} from '@/types/ai'

export function scoreResume(resumeId: number, data: ResumeScoreRequest) {
  return http.post<never, ResumeScoreVO>(`/ai/resumes/${resumeId}/score`, data)
}

export function optimizeResume(resumeId: number, data: ResumeOptimizeRequest) {
  return http.post<never, ResumeOptimizeVO>(`/ai/resumes/${resumeId}/optimize`, data)
}

export function matchJob(jobId: number, data: JobMatchRequest) {
  return http.post<never, JobMatchVO>(`/ai/jobs/${jobId}/match`, data)
}
