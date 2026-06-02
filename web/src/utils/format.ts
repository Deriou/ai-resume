export function formatDateTime(value?: string | null): string {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

export function formatDate(value?: string | null): string {
  if (!value) return '-'
  return value.slice(0, 10)
}

export function formatNumber(value?: number | null): string {
  return Number(value ?? 0).toLocaleString('zh-CN')
}

export function formatFileSize(bytes?: number | null): string {
  const value = Number(bytes ?? 0)
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}
