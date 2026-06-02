<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <div class="page-header">
    <div>
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

h1 {
  margin: 0;
  font-size: 26px;
  line-height: 1.25;
  color: #102033;
}

p {
  margin: 8px 0 0;
  color: var(--app-text-muted);
}

.actions {
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>
