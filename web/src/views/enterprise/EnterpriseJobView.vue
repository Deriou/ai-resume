<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { closeJob, createJob, listMyJobs, updateJob } from '@/api/job'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import type { JobVO } from '@/types/job'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const jobs = ref<JobVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ title: '', jdContent: '', techStack: '', location: '' })

async function loadData() {
  loading.value = true
  try {
    const data = await listMyJobs(page.value, pageSize.value)
    jobs.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { title: '', jdContent: '', techStack: '', location: '' })
  dialogVisible.value = true
}

function openEdit(row: JobVO) {
  editingId.value = row.id
  Object.assign(form, {
    title: row.title,
    jdContent: row.jdContent,
    techStack: row.techStack || '',
    location: row.location || '',
  })
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.title.trim() || !form.jdContent.trim()) {
    ElMessage.warning('请填写岗位标题和 JD')
    return
  }
  submitLoading.value = true
  try {
    if (editingId.value) {
      await updateJob(editingId.value, { ...form })
      ElMessage.success('岗位已更新')
    } else {
      await createJob({ ...form })
      ElMessage.success('岗位已发布')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    submitLoading.value = false
  }
}

async function handleClose(row: JobVO) {
  await ElMessageBox.confirm(`确认关闭岗位「${row.title}」吗？关闭后求职者无法继续投递。`, '关闭岗位', {
    type: 'warning',
  })
  await closeJob(row.id)
  ElMessage.success('岗位已关闭')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <PageHeader title="我的岗位" description="发布、编辑和关闭企业岗位。">
      <template #actions>
        <el-button type="primary" @click="openCreate">发布岗位</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="jobs" empty-text="暂无岗位">
        <el-table-column prop="title" label="岗位" min-width="190" />
        <el-table-column prop="techStack" label="技术栈" min-width="220" show-overflow-tooltip />
        <el-table-column prop="location" label="地点" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag kind="job" :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.status === 'CLOSED'" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" :disabled="row.status === 'CLOSED'" @click="handleClose(row)">关闭</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑岗位' : '发布岗位'" width="720px">
      <el-form label-position="top">
        <el-form-item label="岗位标题">
          <el-input v-model="form.title" maxlength="128" show-word-limit />
        </el-form-item>
        <div class="two-column-grid">
          <el-form-item label="技术栈">
            <el-input v-model="form.techStack" maxlength="255" placeholder="如 Linux,Docker,Kubernetes,Redis" />
          </el-form-item>
          <el-form-item label="地点">
            <el-input v-model="form.location" maxlength="128" />
          </el-form-item>
        </div>
        <el-form-item label="岗位 JD">
          <el-input v-model="form.jdContent" type="textarea" :rows="8" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
