<script setup lang="ts">
import type { ResumeScoreVO } from '@/types/ai'

defineProps<{
  result: ResumeScoreVO | null
}>()

const visible = defineModel<boolean>('visible', { required: true })
</script>

<template>
  <el-dialog v-model="visible" title="AI 简历评分" width="720px">
    <template v-if="result">
      <div class="score-hero">
        <el-progress type="dashboard" :percentage="result.overallScore" :width="132" />
        <div>
          <h3>{{ result.targetDirection }}</h3>
          <p>模型：{{ result.llmModel }}，Token：{{ result.totalTokens }}</p>
        </div>
      </div>
      <el-divider />
      <el-table :data="result.dimensions" size="small">
        <el-table-column prop="name" label="维度" width="160" />
        <el-table-column prop="score" label="分数" width="90" />
        <el-table-column prop="comment" label="建议" />
      </el-table>
      <h4>优化建议</h4>
      <ul>
        <li v-for="item in result.suggestions" :key="item">{{ item }}</li>
      </ul>
    </template>
  </el-dialog>
</template>

<style scoped>
.score-hero {
  display: flex;
  align-items: center;
  gap: 22px;
}

h3 {
  margin: 0 0 8px;
}

p {
  margin: 0;
  color: var(--app-text-muted);
}

li {
  line-height: 1.8;
}
</style>
