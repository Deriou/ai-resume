<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchAdminApplications } from '@/api/admin'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import type { ApplicationStatus } from '@/types/api'
import type { AdminApplicationVO } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const applications = ref<AdminApplicationVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const status = ref<'' | ApplicationStatus>('')

async function loadData() {
  loading.value = true
  try {
    const data = await fetchAdminApplications(page.value, pageSize.value, status.value || undefined)
    applications.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <PageHeader title="投递查看" description="管理员只读查看求职者投递和企业审核状态。" />
    <el-card shadow="never">
      <div class="filter-line">
        <el-select v-model="status" clearable placeholder="投递状态" style="width: 160px">
          <el-option label="待处理" value="PENDING" />
          <el-option label="已查看" value="VIEWED" />
          <el-option label="已通过" value="ACCEPTED" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </el-card>
    <el-card shadow="never">
      <el-table v-loading="loading" :data="applications" empty-text="暂无投递">
        <el-table-column prop="username" label="求职者" min-width="130">
          <template #default="{ row }">{{ row.nickName || row.username }}</template>
        </el-table-column>
        <el-table-column prop="enterpriseName" label="企业" min-width="150" />
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
