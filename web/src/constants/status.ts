export const ROLE_LABEL_MAP = {
  USER: '求职者',
  ENTERPRISE: '企业',
  ADMIN: '管理员',
} as const

export const JOB_STATUS_MAP = {
  OPEN: { label: '开放', type: 'success' },
  CLOSED: { label: '已关闭', type: 'info' },
} as const

export const APPLICATION_STATUS_MAP = {
  PENDING: { label: '待处理', type: 'warning' },
  VIEWED: { label: '已查看', type: 'primary' },
  ACCEPTED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已拒绝', type: 'danger' },
} as const

export const CREDIT_TYPE_MAP: Record<string, string> = {
  CHECK_IN: '每日签到',
  ADMIN_GRANT: '管理员发放',
  AI_RESUME_SCORE: '简历评分',
  AI_RESUME_OPTIMIZE: '简历优化',
  AI_JOB_MATCH: '岗位匹配',
}

export const OPERATION_LABEL_MAP: Record<string, string> = {
  RESUME_SCORE: '简历评分',
  RESUME_OPTIMIZE: '简历优化',
  JOB_MATCH: '岗位匹配',
}
