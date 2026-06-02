<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listMyApplications } from '@/api/application'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import type { ApplicationVO } from '@/types/application'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const applications = ref<ApplicationVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

async function loadData() {
  loading.value = true
  try {
    const data = await listMyApplications(page.value, pageSize.value)
    applications.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <PageHeader title="我的投递" description="查看已投递岗位的企业审核状态。" />
    <el-card shadow="never">
      <el-table v-loading="loading" :data="applications" empty-text="暂无投递记录">
        <el-table-column prop="jobTitle" label="岗位" min-width="180" />
        <el-table-column prop="resumeTitle" label="简历" min-width="180" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag kind="application" :value="row.status" /></template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="审核时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.reviewedAt) }}</template>
        </el-table-column>
        <el-table-column label="投递时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
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
  </div>
</template>
