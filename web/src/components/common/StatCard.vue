<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  label: string
  value: string | number
  caption?: string
  icon?: Component
  tone?: 'blue' | 'green' | 'orange' | 'red'
}>()
</script>

<template>
  <div class="stat-card" :class="tone || 'blue'">
    <div class="icon-box" v-if="icon">
      <el-icon><component :is="icon" /></el-icon>
    </div>
    <div class="label">{{ label }}</div>
    <div class="value">{{ value }}</div>
    <div v-if="caption" class="caption">{{ caption }}</div>
  </div>
</template>

<style scoped>
.stat-card {
  min-height: 116px;
  padding: 18px;
  position: relative;
  overflow: hidden;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  background: linear-gradient(180deg, #fff, #f8fbff);
  box-shadow: var(--app-shadow-soft);
}

.icon-box {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 9px;
  background: var(--app-primary-soft);
  color: var(--app-primary);
}

.label {
  color: var(--app-text-muted);
  font-size: 13px;
}

.value {
  margin-top: 12px;
  font-size: 28px;
  font-weight: 800;
  color: #102033;
}

.caption {
  margin-top: 8px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.green .icon-box {
  color: var(--app-success);
  background: #ecfdf3;
}

.orange .icon-box {
  color: var(--app-warning);
  background: #fff7ed;
}

.red .icon-box {
  color: var(--app-danger);
  background: #fef2f2;
}
</style>
