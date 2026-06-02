<script setup lang="ts">
import type { JobMatchVO } from '@/types/ai'

defineProps<{
  result: JobMatchVO | null
  jobTitle?: string
}>()

const visible = defineModel<boolean>('visible', { required: true })
</script>

<template>
  <el-dialog v-model="visible" title="AI 岗位匹配结果" width="680px">
    <template v-if="result">
      <div class="match-head">
        <el-progress type="dashboard" :percentage="result.matchScore" :width="126" />
        <div>
          <h3>{{ jobTitle || `岗位 #${result.jobId}` }}</h3>
          <p>匹配度越高，说明简历内容和岗位 JD 越贴近。</p>
        </div>
      </div>
      <div class="three-column-grid blocks">
        <section>
          <strong>优势</strong>
          <p v-for="item in result.strengths" :key="item">{{ item }}</p>
        </section>
        <section>
          <strong>差距</strong>
          <p v-for="item in result.gaps" :key="item">{{ item }}</p>
        </section>
        <section>
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
  gap: 20px;
}

h3 {
  margin: 0 0 8px;
}

p {
  margin: 0;
  line-height: 1.7;
  color: var(--app-text-muted);
}

.blocks {
  margin-top: 18px;
}

section {
  padding: 14px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: #f8fbff;
}

strong {
  display: block;
  margin-bottom: 10px;
  color: #102033;
}
</style>
