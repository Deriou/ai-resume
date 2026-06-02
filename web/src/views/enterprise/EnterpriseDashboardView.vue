<script setup lang="ts">
import { Box, Briefcase, Tickets, View } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listReceivedApplications } from '@/api/application'
import { listMyJobs } from '@/api/job'
import EmptyState from '@/components/common/EmptyState.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import type { ApplicationVO } from '@/types/application'
import type { JobVO } from '@/types/job'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const jobs = ref<JobVO[]>([])
const applications = ref<ApplicationVO[]>([])
const applicationTotal = ref(0)

const openJobs = computed(() => jobs.value.filter((item) => item.status === 'OPEN').length)
const pendingApplications = computed(() => applications.value.filter((item) => item.status === 'PENDING').length)

async function loadData() {
  loading.value = true
  try {
    const [jobPage, applicationPage] = await Promise.all([listMyJobs(1, 100), listReceivedApplications(1, 6)])
    jobs.value = jobPage.records
    applications.value = applicationPage.records
    applicationTotal.value = applicationPage.total
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="page">
    <PageHeader title="企业工作台" description="发布岗位、接收投递，并完成候选人审核流转。">
      <template #actions>
        <el-button type="primary" @click="router.push('/enterprise/jobs')">发布岗位</el-button>
        <el-button plain @click="router.push('/enterprise/applications')">审核投递</el-button>
      </template>
    </PageHeader>

    <div class="stat-grid">
      <StatCard label="我的岗位" :value="jobs.length" caption="包含开放和已关闭岗位" :icon="Briefcase" />
      <StatCard label="开放岗位" :value="openJobs" caption="求职者可见并可投递" :icon="View" tone="green" />
      <StatCard label="收到投递" :value="applicationTotal" caption="全部历史投递" :icon="Box" />
      <StatCard label="待审核" :value="pendingApplications" caption="建议优先处理" :icon="Tickets" tone="orange" />
    </div>

    <div class="two-column-grid">
      <el-card shadow="never" header="最近投递">
        <el-table v-if="applications.length" :data="applications">
          <el-table-column prop="jobTitle" label="岗位" min-width="150" />
          <el-table-column prop="resumeTitle" label="简历" min-width="150" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }"><StatusTag kind="application" :value="row.status" /></template>
          </el-table-column>
          <el-table-column label="时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
        <EmptyState v-else title="暂无投递" description="岗位收到投递后会显示在这里。" />
      </el-card>
      <el-card shadow="never" header="岗位状态">
        <el-table :data="jobs.slice(0, 6)" empty-text="暂无岗位">
          <el-table-column prop="title" label="岗位" min-width="180" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }"><StatusTag kind="job" :value="row.status" /></template>
          </el-table-column>
          <el-table-column prop="location" label="地点" width="100" />
        </el-table>
      </el-card>
    </div>
  </div>
</template>
