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
      <el-alert :title="result.summary" type="primary" :closable="false" show-icon />
      <h3>可直接替换的表达</h3>
      <el-card v-for="item in result.optimizedBullets" :key="item" shadow="never" class="result-item">
        {{ item }}
      </el-card>
      <h3>改写方向</h3>
      <ul>
        <li v-for="item in result.rewriteSuggestions" :key="item">{{ item }}</li>
      </ul>
      <el-divider />
      <div class="muted">模型：{{ result.llmModel }}，Token：{{ result.totalTokens }}，耗时：{{ result.latencyMs }}ms</div>
    </template>
  </el-drawer>
</template>

<style scoped>
h3 {
  margin: 22px 0 10px;
}

.result-item {
  margin-bottom: 10px;
  line-height: 1.7;
}

li {
  line-height: 1.8;
}
</style>
