<script setup lang="ts">
defineProps<{
  title: string
  description?: string
  eyebrow?: string
}>()
</script>

<template>
  <div class="page-header">
    <div>
      <span v-if="eyebrow" class="eyebrow">{{ eyebrow }}</span>
      <h1>{{ title }}</h1>
      <p v-if="description">{{ description }}</p>
    </div>
    <div class="actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.eyebrow {
  display: inline-block;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--brand-500);
}

h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--app-text);
}

p {
  margin: 10px 0 0;
  max-width: 640px;
  color: var(--app-text-muted);
  line-height: 1.6;
}

.actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-shrink: 0;
}
</style>
