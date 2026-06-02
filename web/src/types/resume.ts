export interface ResumeVO {
  id: number
  userId: number
  title: string
  contentMd: string
  createdAt: string
  updatedAt: string
}

export interface ResumeCreateRequest {
  title: string
  contentMd: string
}

export type ResumeUpdateRequest = ResumeCreateRequest

export interface ResumeFileVO {
  id: number
  resumeId: number
  originalName: string
  contentType: string
  fileSize: number
  fileExt: string
  createdAt: string
}
