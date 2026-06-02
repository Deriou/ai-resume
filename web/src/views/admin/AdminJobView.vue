<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { fetchAdminJobs } from '@/api/admin'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import type { JobStatus } from '@/types/api'
import type { AdminJobVO } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const jobs = ref<AdminJobVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const filters = reactive<{ status: '' | JobStatus; keyword: string }>({ status: '', keyword: '' })

async function loadData() {
  loading.value = true
  try {
    const data = await fetchAdminJobs(page.value, pageSize.value, filters.status || undefined, filters.keyword || undefined)
    jobs.value = data.records
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
    <PageHeader title="岗位查看" description="管理员只读查看平台岗位和投递数量。" />
    <el-card shadow="never">
      <div class="filter-line">
        <el-select v-model="filters.status" clearable placeholder="状态" style="width: 140px">
          <el-option label="开放" value="OPEN" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-input v-model="filters.keyword" clearable placeholder="岗位关键词" style="width: 240px" />
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </el-card>
    <el-card shadow="never">
      <el-table v-loading="loading" :data="jobs" empty-text="暂无岗位">
        <el-table-column prop="title" label="岗位" min-width="180" />
        <el-table-column prop="enterpriseName" label="企业" min-width="150" />
        <el-table-column prop="techStack" label="技术栈" min-width="220" show-overflow-tooltip />
        <el-table-column prop="location" label="地点" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag kind="job" :value="row.status" /></template>
        </el-table-column>
        <el-table-column prop="applicationCount" label="投递数" width="100" />
        <el-table-column label="创建时间" width="180">
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
