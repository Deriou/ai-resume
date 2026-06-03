<script setup lang="ts">
import type { JobMatchVO } from '@/types/ai'

defineProps<{
  result: JobMatchVO | null
  jobTitle?: string
}>()

const visible = defineModel<boolean>('visible', { required: true })
</script>

<template>
  <el-dialog v-model="visible" title="AI 岗位匹配结果" width="700px" class="ai-dialog">
    <template v-if="result">
      <div class="match-head">
        <el-progress type="dashboard" :percentage="result.matchScore" :width="120" :stroke-width="10" />
        <div class="head-meta">
          <span class="app-chip app-chip--ai">岗位匹配</span>
          <h3>{{ jobTitle || `岗位 #${result.jobId}` }}</h3>
          <p>匹配度越高，说明简历内容和岗位 JD 越贴近。</p>
        </div>
      </div>
      <div class="three-column-grid blocks">
        <section class="block-pos">
          <strong>优势</strong>
          <p v-for="item in result.strengths" :key="item">{{ item }}</p>
        </section>
        <section class="block-gap">
          <strong>差距</strong>
          <p v-for="item in result.gaps" :key="item">{{ item }}</p>
        </section>
        <section class="block-sug">
          <strong>建议</strong>
          <p v-for="item in result.suggestions" :key="item">{{ item }}</p>
        </section>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.match-head {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 22px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.08), rgba(34, 184, 239, 0.08));
  border: 1px solid rgba(79, 70, 229, 0.16);
}

.head-meta h3 {
  margin: 10px 0 6px;
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 800;
}

.head-meta p {
  margin: 0;
  line-height: 1.6;
  color: var(--app-text-muted);
  font-size: 13px;
}

.blocks {
  margin-top: 18px;
}

section {
  padding: 16px;
  border-radius: var(--radius-md);
}

section strong {
  display: block;
  margin-bottom: 10px;
  font-family: var(--font-display);
  font-weight: 700;
}

section p {
  margin: 0 0 8px;
  line-height: 1.6;
  font-size: 13px;
  color: #44566c;
}

.block-pos {
  background: rgba(21, 169, 124, 0.08);
  border: 1px solid rgba(21, 169, 124, 0.2);
}
.block-pos strong {
  color: #0f7c5b;
}

.block-gap {
  background: rgba(224, 138, 0, 0.08);
  border: 1px solid rgba(224, 138, 0, 0.22);
}
.block-gap strong {
  color: #b06d00;
}

.block-sug {
  background: var(--brand-50);
  border: 1px solid var(--brand-100);
}
.block-sug strong {
  color: var(--brand-600);
}
</style>
