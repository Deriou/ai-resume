<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'

const model = defineModel<string>({ required: true })

const html = computed(() => String(marked.parse(model.value || '')))
</script>

<template>
  <div class="markdown-editor">
    <el-input v-model="model" type="textarea" :rows="12" resize="vertical" placeholder="请输入 Markdown 或纯文本简历内容" />
    <div class="preview" v-html="html" />
  </div>
</template>

<style scoped>
.markdown-editor {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.preview {
  min-height: 264px;
  padding: 12px 14px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: #f8fbff;
  line-height: 1.7;
  overflow: auto;
}

.preview :deep(p) {
  margin: 0 0 8px;
}
</style>
