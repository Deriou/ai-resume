<script setup lang="ts">
import { Download, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type UploadRequestOptions } from 'element-plus'
import { onMounted, ref } from 'vue'
import { deleteResumeFile, downloadResumeFile, listResumeFiles, uploadResumeFile } from '@/api/resume'
import type { ResumeFileVO } from '@/types/resume'
import { formatDateTime, formatFileSize } from '@/utils/format'

const props = defineProps<{
  resumeId: number
}>()

const loading = ref(false)
const files = ref<ResumeFileVO[]>([])

async function loadFiles() {
  loading.value = true
  try {
    files.value = await listResumeFiles(props.resumeId)
  } finally {
    loading.value = false
  }
}

async function upload(options: UploadRequestOptions) {
  const file = options.file
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || !['pdf', 'doc', 'docx'].includes(ext)) {
    ElMessage.warning('仅支持 PDF / DOC / DOCX')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('单个附件不能超过 10MB')
    return
  }
  if (files.value.length >= 3) {
    ElMessage.warning('单份简历最多上传 3 个附件')
    return
  }
  await uploadResumeFile(props.resumeId, file)
  ElMessage.success('附件已上传')
  await loadFiles()
}

async function handleDownload(row: ResumeFileVO) {
  const blob = await downloadResumeFile(row.id)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = row.originalName
  link.click()
  URL.revokeObjectURL(url)
}

async function handleDelete(row: ResumeFileVO) {
  await ElMessageBox.confirm(`确认删除附件「${row.originalName}」吗？`, '删除附件', { type: 'warning' })
  await deleteResumeFile(row.id)
  ElMessage.success('附件已删除')
  await loadFiles()
}

onMounted(loadFiles)
</script>

<template>
  <div class="file-manager">
    <el-alert
      title="附件仅用于保存原始简历文件，AI 评分、优化和岗位匹配仍基于简历正文内容。"
      type="info"
      show-icon
      :closable="false"
    />
    <div class="toolbar">
      <span class="muted">支持 PDF / DOC / DOCX，单文件 10MB，最多 3 个。</span>
      <el-upload :http-request="upload" :show-file-list="false" :disabled="files.length >= 3">
        <el-button type="primary" :icon="Upload" :disabled="files.length >= 3">上传附件</el-button>
      </el-upload>
    </div>
    <el-table v-loading="loading" :data="files" empty-text="暂无附件">
      <el-table-column prop="originalName" label="文件名" min-width="220" show-overflow-tooltip />
      <el-table-column label="大小" width="110">
        <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="fileExt" label="类型" width="90" />
      <el-table-column label="上传时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Download" @click="handleDownload(row)">下载</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.file-manager {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
</style>
