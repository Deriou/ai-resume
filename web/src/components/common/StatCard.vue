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
    <div class="stat-top">
      <div class="label">{{ label }}</div>
      <div class="icon-box" v-if="icon">
        <el-icon><component :is="icon" /></el-icon>
      </div>
    </div>
    <div class="value">{{ value }}</div>
    <div v-if="caption" class="caption">{{ caption }}</div>
  </div>
</template>

<style scoped>
.stat-card {
  min-height: 124px;
  padding: 20px;
  position: relative;
  overflow: hidden;
  border: 1px solid var(--app-border);
  border-radius: var(--radius-lg);
  background: var(--app-surface);
  box-shadow: var(--app-shadow-soft);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card::before {
  content: "";
  position: absolute;
  inset: 0 0 auto 0;
  height: 3px;
  background: var(--brand-grad);
  opacity: 0.9;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--app-shadow-hover);
}

.stat-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.icon-box {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: var(--app-primary-soft);
  color: var(--app-primary);
  font-size: 19px;
}

.label {
  color: var(--app-text-muted);
  font-size: 13px;
  font-weight: 600;
}

.value {
  margin-top: 14px;
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--app-text);
}

.caption {
  margin-top: 8px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.green::before {
  background: linear-gradient(135deg, #15a97c, #4fd1a5);
}
.green .icon-box {
  color: var(--app-success);
  background: rgba(21, 169, 124, 0.12);
}

.orange::before {
  background: linear-gradient(135deg, #e08a00, #ffb547);
}
.orange .icon-box {
  color: var(--app-warning);
  background: rgba(224, 138, 0, 0.12);
}

.red::before {
  background: linear-gradient(135deg, #e23d4b, #ff7a85);
}
.red .icon-box {
  color: var(--app-danger);
  background: rgba(226, 61, 75, 0.1);
}
</style>
