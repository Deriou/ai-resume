import http from './http'
import type { PageVO } from '@/types/api'
import type { ResumeCreateRequest, ResumeFileVO, ResumeUpdateRequest, ResumeVO } from '@/types/resume'

export function listResumes(page = 1, size = 10) {
  return http.get<never, PageVO<ResumeVO>>('/resumes', { params: { page, size } })
}

export function getResume(id: number) {
  return http.get<never, ResumeVO>(`/resumes/${id}`)
}

export function createResume(data: ResumeCreateRequest) {
  return http.post<never, ResumeVO>('/resumes', data)
}

export function importPdfResume(file: File, title?: string) {
  const form = new FormData()
  form.append('file', file)
  return http.post<never, ResumeVO>('/resumes/import/pdf', form, { params: { title } })
}

export function updateResume(id: number, data: ResumeUpdateRequest) {
  return http.put<never, ResumeVO>(`/resumes/${id}`, data)
}

export function deleteResume(id: number) {
  return http.delete<never, void>(`/resumes/${id}`)
}

export function uploadResumeFile(resumeId: number, file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.post<never, ResumeFileVO>(`/resumes/${resumeId}/files`, form)
}

export function listResumeFiles(resumeId: number) {
  return http.get<never, ResumeFileVO[]>(`/resumes/${resumeId}/files`)
}

export function deleteResumeFile(fileId: number) {
  return http.delete<never, void>(`/resume-files/${fileId}`)
}

export function downloadResumeFile(fileId: number) {
  return http.get<never, Blob>(`/resume-files/${fileId}/download`, { responseType: 'blob' })
}
