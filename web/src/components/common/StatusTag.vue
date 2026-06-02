<script setup lang="ts">
import { APPLICATION_STATUS_MAP, JOB_STATUS_MAP, ROLE_LABEL_MAP } from '@/constants/status'

const props = defineProps<{
  value: string
  kind?: 'job' | 'application' | 'role' | 'plain'
}>()

function meta() {
  if (props.kind === 'job') return JOB_STATUS_MAP[props.value as keyof typeof JOB_STATUS_MAP]
  if (props.kind === 'application') {
    return APPLICATION_STATUS_MAP[props.value as keyof typeof APPLICATION_STATUS_MAP]
  }
  if (props.kind === 'role') return { label: ROLE_LABEL_MAP[props.value as keyof typeof ROLE_LABEL_MAP], type: 'info' }
  return { label: props.value, type: 'info' }
}
</script>

<template>
  <el-tag :type="meta()?.type || 'info'">{{ meta()?.label || value }}</el-tag>
</template>
