<script setup lang="ts">
import type { ResumeScoreVO } from '@/types/ai'

defineProps<{
  result: ResumeScoreVO | null
}>()

const visible = defineModel<boolean>('visible', { required: true })
</script>

<template>
  <el-dialog v-model="visible" title="AI 简历评分" width="720px" class="ai-dialog">
    <template v-if="result">
      <div class="score-hero">
        <el-progress type="dashboard" :percentage="result.overallScore" :width="120" :stroke-width="10" />
        <div class="hero-meta">
          <span class="app-chip app-chip--ai">目标方向 · {{ result.targetDirection }}</span>
          <h3>综合评分 {{ result.overallScore }}</h3>
          <p>模型 {{ result.llmModel }} · 共消耗 {{ result.totalTokens }} tokens</p>
        </div>
      </div>

      <h4 class="section-h">各维度得分</h4>
      <div class="dim-list">
        <div v-for="dim in result.dimensions" :key="dim.name" class="dim-row">
          <div class="dim-top">
            <span class="dim-name">{{ dim.name }}</span>
            <span class="dim-score">{{ dim.score }}</span>
          </div>
          <el-progress :percentage="dim.score" :show-text="false" :stroke-width="7" />
          <p class="dim-comment muted">{{ dim.comment }}</p>
        </div>
      </div>

      <h4 class="section-h">优化建议</h4>
      <ul class="suggestion-list">
        <li v-for="item in result.suggestions" :key="item">{{ item }}</li>
      </ul>
    </template>
  </el-dialog>
</template>

<style scoped>
.score-hero {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 22px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.08), rgba(34, 184, 239, 0.08));
  border: 1px solid rgba(79, 70, 229, 0.16);
}

.hero-meta h3 {
  margin: 10px 0 6px;
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 800;
}

.hero-meta p {
  margin: 0;
  color: var(--app-text-muted);
  font-size: 13px;
}

.section-h {
  margin: 24px 0 14px;
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 700;
}

.dim-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dim-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.dim-name {
  font-weight: 600;
}

.dim-score {
  font-family: var(--font-display);
  font-weight: 800;
  color: var(--brand-600);
}

.dim-comment {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.6;
}

.suggestion-list {
  margin: 0;
  padding-left: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.suggestion-list li {
  position: relative;
  padding: 12px 14px 12px 40px;
  border-radius: var(--radius-sm);
  background: var(--app-surface-soft);
  border: 1px solid var(--app-border);
  line-height: 1.6;
}

.suggestion-list li::before {
  content: "✦";
  position: absolute;
  left: 14px;
  top: 12px;
  color: var(--accent-indigo);
  font-weight: 700;
}
</style>
