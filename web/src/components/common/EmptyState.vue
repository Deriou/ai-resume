<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <div class="empty-state">
    <div class="dot" />
    <strong>{{ title }}</strong>
    <span v-if="description">{{ description }}</span>
    <slot />
  </div>
</template>

<style scoped>
.empty-state {
  min-height: 160px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  color: var(--app-text-muted);
  text-align: center;
}

.dot {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #dbeafe, #ffffff);
  border: 1px solid var(--app-border);
}

strong {
  color: #334155;
}
</style>
