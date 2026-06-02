<script setup lang="ts">
import { Coin, Document, Tickets } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listMyApplications } from '@/api/application'
import { checkIn, fetchCheckInStatus, fetchCreditBalance } from '@/api/credit'
import { listHotCompanies, listHotJobs } from '@/api/job'
import { listResumes } from '@/api/resume'
import EmptyState from '@/components/common/EmptyState.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { useAuthStore } from '@/stores/auth'
import type { ApplicationVO } from '@/types/application'
import type { HotCompanyVO, HotJobVO } from '@/types/job'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const checking = ref(false)
const resumeTotal = ref(0)
const applicationTotal = ref(0)
const checkedIn = ref(false)
const hotJobs = ref<HotJobVO[]>([])
const hotCompanies = ref<HotCompanyVO[]>([])
const recentApplications = ref<ApplicationVO[]>([])

async function loadData() {
  loading.value = true
  try {
    const [balance, checkStatus, resumes, applications, jobs, companies] = await Promise.all([
      fetchCreditBalance(),
      fetchCheckInStatus(),
      listResumes(1, 1),
      listMyApplications(1, 5),
      listHotJobs(),
      listHotCompanies(),
    ])
    auth.updateCreditBalance(balance.balance)
    checkedIn.value = checkStatus.checkedIn
    resumeTotal.value = resumes.total
    applicationTotal.value = applications.total
    recentApplications.value = applications.records
    hotJobs.value = jobs
    hotCompanies.value = companies
  } finally {
    loading.value = false
  }
}

async function handleCheckIn() {
  checking.value = true
  try {
    const status = await checkIn()
    checkedIn.value = status.checkedIn
    auth.updateCreditBalance(status.balance)
    ElMessage.success(`签到成功，获得 ${status.creditReward} AI 币`)
  } finally {
    checking.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="page">
    <PageHeader title="求职者工作台" description="管理简历、寻找岗位，并用 AI 完成评分、优化和匹配。">
      <template #actions>
        <el-button type="primary" @click="router.push('/user/resumes')">新建 / 优化简历</el-button>
        <el-button plain @click="router.push('/user/jobs')">浏览岗位</el-button>
      </template>
    </PageHeader>

    <div class="stat-grid">
      <StatCard label="AI 币余额" :value="auth.creditBalance ?? 0" caption="AI 调用成功后按规则扣费" :icon="Coin" />
      <StatCard label="我的简历" :value="resumeTotal" caption="正文内容是 AI 输入来源" :icon="Document" />
      <StatCard label="我的投递" :value="applicationTotal" caption="查看企业审核状态" :icon="Tickets" />
      <StatCard label="今日签到" :value="checkedIn ? '已签到' : '未签到'" caption="每天可领取 1 AI 币" :icon="Coin" tone="green" />
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <div>
          <strong>每日签到</strong>
          <div class="muted">领取免费 AI 币，便于继续评分、优化和匹配。</div>
        </div>
        <el-button type="primary" :disabled="checkedIn" :loading="checking" @click="handleCheckIn">
          {{ checkedIn ? '今日已签到' : '签到领 1 AI 币' }}
        </el-button>
      </div>
    </el-card>

    <div class="two-column-grid">
      <el-card shadow="never" header="热门岗位">
        <el-table :data="hotJobs" size="small" empty-text="暂无热门岗位">
          <el-table-column prop="title" label="岗位" min-width="150" />
          <el-table-column prop="location" label="地点" width="100" />
          <el-table-column prop="applicationCount" label="投递" width="80" />
        </el-table>
      </el-card>
      <el-card shadow="never" header="热门公司">
        <el-table :data="hotCompanies" size="small" empty-text="暂无热门公司">
          <el-table-column prop="enterpriseName" label="企业" />
          <el-table-column prop="openJobCount" label="开放岗位" width="100" />
          <el-table-column prop="applicationCount" label="投递" width="80" />
        </el-table>
      </el-card>
    </div>

    <el-card shadow="never" header="最近投递">
      <el-table v-if="recentApplications.length" :data="recentApplications">
        <el-table-column prop="jobTitle" label="岗位" min-width="180" />
        <el-table-column prop="resumeTitle" label="简历" min-width="180" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag kind="application" :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="投递时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <EmptyState v-else title="暂无投递" description="可以先在岗位列表选择合适岗位发起投递。" />
    </el-card>
  </div>
</template>
