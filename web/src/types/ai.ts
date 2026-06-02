export interface ResumeScoreRequest {
  targetDirection: string
}

export interface ResumeOptimizeRequest {
  targetDirection: string
}

export interface JobMatchRequest {
  resumeId: number
}

export interface ScoreDimensionVO {
  name: string
  score: number
  comment: string
}

export interface ResumeScoreVO {
  id: number
  resumeId: number
  targetDirection: string
  overallScore: number
  dimensions: ScoreDimensionVO[]
  suggestions: string[]
  llmModel: string
  promptTokens: number
  completionTokens: number
  totalTokens: number
  createdAt: string
}

export interface ResumeOptimizeVO {
  summary: string
  optimizedBullets: string[]
  rewriteSuggestions: string[]
  llmModel: string
  promptTokens: number
  completionTokens: number
  totalTokens: number
  latencyMs: number
}

export interface JobMatchVO {
  id: number
  resumeId: number
  jobId: number
  matchScore: number
  strengths: string[]
  gaps: string[]
  suggestions: string[]
  createdAt: string
}
