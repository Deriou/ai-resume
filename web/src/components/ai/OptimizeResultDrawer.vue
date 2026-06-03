<script setup lang="ts">
import type { ResumeOptimizeVO } from '@/types/ai'

defineProps<{
  result: ResumeOptimizeVO | null
}>()

const visible = defineModel<boolean>('visible', { required: true })
</script>

<template>
  <el-drawer v-model="visible" title="AI 优化建议" size="560px">
    <template v-if="result">
      <div class="summary-box">
        <span class="app-chip app-chip--ai">AI 总结</span>
        <p>{{ result.summary }}</p>
      </div>

      <h3 class="section-h">可直接替换的表达</h3>
      <div class="bullet-list">
        <div v-for="item in result.optimizedBullets" :key="item" class="bullet-item">
          {{ item }}
        </div>
      </div>

      <h3 class="section-h">改写方向</h3>
      <ul class="rewrite-list">
        <li v-for="item in result.rewriteSuggestions" :key="item">{{ item }}</li>
      </ul>

      <div class="meta-foot muted">
        模型 {{ result.llmModel }} · {{ result.totalTokens }} tokens · {{ result.latencyMs }}ms
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.summary-box {
  padding: 18px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.08), rgba(34, 184, 239, 0.08));
  border: 1px solid rgba(79, 70, 229, 0.16);
}

.summary-box p {
  margin: 10px 0 0;
  line-height: 1.7;
  color: #3a4a5e;
}

.section-h {
  margin: 24px 0 12px;
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 700;
}

.bullet-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bullet-item {
  padding: 13px 15px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--app-border);
  background: var(--app-surface-soft);
  line-height: 1.7;
}

.rewrite-list {
  margin: 0;
  padding-left: 20px;
}

.rewrite-list li {
  line-height: 1.9;
}

.meta-foot {
  margin-top: 24px;
  padding-top: 14px;
  border-top: 1px solid var(--app-border);
  font-size: 12px;
}
</style>
