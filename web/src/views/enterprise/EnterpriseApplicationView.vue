<script setup lang="ts">
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { listReceivedApplications, reviewApplication } from '@/api/application'
import { downloadResumeFile } from '@/api/resume'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import type { ApplicationStatus } from '@/types/api'
import type { ApplicationVO } from '@/types/application'
import type { ResumeFileVO } from '@/types/resume'
import { formatDateTime, formatFileSize } from '@/utils/format'

const loading = ref(false)
const applications = ref<ApplicationVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const reviewVisible = ref(false)
const resumeVisible = ref(false)
const reviewLoading = ref(false)
const current = ref<ApplicationVO | null>(null)
const reviewForm = reactive<{ status: Exclude<ApplicationStatus, 'PENDING'>; remark: string }>({
  status: 'VIEWED',
  remark: '',
})

async function loadData() {
  loading.value = true
  try {
    const data = await listReceivedApplications(page.value, pageSize.value)
    applications.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openReview(row: ApplicationVO, status: Exclude<ApplicationStatus, 'PENDING'>) {
  current.value = row
  reviewForm.status = status
  reviewForm.remark = row.remark || ''
  reviewVisible.value = true
}

function openResume(row: ApplicationVO) {
  current.value = row
  resumeVisible.value = true
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

async function submitReview() {
  if (!current.value) return
  reviewLoading.value = true
  try {
    await reviewApplication(current.value.id, {
      status: reviewForm.status,
      remark: reviewForm.remark || undefined,
    })
    ElMessage.success('审核状态已更新')
    reviewVisible.value = false
    await loadData()
  } finally {
    reviewLoading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <PageHeader title="收到的投递" description="查看投递到企业岗位的申请，并完成查看、通过或拒绝。" />

    <el-card shadow="never">
      <el-table v-loading="loading" :data="applications" empty-text="暂无投递">
        <el-table-column prop="userId" label="求职者 ID" width="110" />
        <el-table-column prop="jobTitle" label="岗位" min-width="180" />
        <el-table-column prop="resumeTitle" label="简历" min-width="180" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag kind="application" :value="row.status" /></template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="投递时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openResume(row)">查看简历</el-button>
            <el-button link type="primary" :disabled="row.status !== 'PENDING'" @click="openReview(row, 'VIEWED')">
              已查看
            </el-button>
            <el-button
              link
              type="success"
              :disabled="row.status === 'ACCEPTED' || row.status === 'REJECTED'"
              @click="openReview(row, 'ACCEPTED')"
            >
              通过
            </el-button>
            <el-button
              link
              type="danger"
              :disabled="row.status === 'ACCEPTED' || row.status === 'REJECTED'"
              @click="openReview(row, 'REJECTED')"
            >
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="toolbar" style="justify-content: flex-end; margin-top: 16px">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          layout="total, prev, pager, next"
          :total="total"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="reviewVisible" title="审核投递" width="520px">
      <el-form label-position="top">
        <el-form-item label="状态">
          <el-select v-model="reviewForm.status" style="width: 100%">
            <el-option label="已查看" value="VIEWED" />
            <el-option label="通过" value="ACCEPTED" />
            <el-option label="拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="reviewForm.remark" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewLoading" @click="submitReview">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resumeVisible" :title="current?.resumeTitle || '简历详情'" width="860px">
      <div class="resume-preview">
        <pre>{{ current?.resumeContentMd || '暂无简历正文' }}</pre>
      </div>
      <div class="resume-files">
        <div class="resume-files__head">
          <strong>简历附件</strong>
          <span class="muted">{{ current?.resumeFiles?.length || 0 }} 个文件</span>
        </div>
        <el-table :data="current?.resumeFiles || []" empty-text="暂无附件">
          <el-table-column prop="originalName" label="文件名" min-width="240" show-overflow-tooltip />
          <el-table-column label="大小" width="110">
            <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column prop="fileExt" label="类型" width="90" />
          <el-table-column label="上传时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :icon="Download" @click="handleDownload(row)">下载</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button type="primary" @click="resumeVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.resume-preview {
  max-height: 60vh;
  overflow: auto;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-fill-color-light);
  padding: 16px;
}

.resume-preview pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  line-height: 1.7;
  color: var(--el-text-color-primary);
}

.resume-files {
  margin-top: 16px;
}

.resume-files__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
</style>
