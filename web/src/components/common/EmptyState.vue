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
  min-height: 180px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  color: var(--app-text-muted);
  text-align: center;
}

.dot {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  background: var(--brand-50);
  border: 1px solid var(--brand-100);
  position: relative;
}

.dot::after {
  content: "";
  position: absolute;
  inset: 16px;
  border-radius: 6px;
  border: 2px dashed var(--brand-300);
}

strong {
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--app-text);
}
</style>
