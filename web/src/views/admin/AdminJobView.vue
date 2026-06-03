<script setup lang="ts">
import { Briefcase, DataAnalysis, TrendCharts, Warning } from '@element-plus/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { fetchAdminJobs, fetchAdminJobSummary } from '@/api/admin'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import type { JobStatus } from '@/types/api'
import type { AdminJobSummaryVO, AdminJobVO } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const jobs = ref<AdminJobVO[]>([])
const summary = ref<AdminJobSummaryVO | null>(null)
const selectedJob = ref<AdminJobVO | null>(null)
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const filters = reactive<{ status: '' | JobStatus; keyword: string }>({ status: '', keyword: '' })

const avgApplications = computed(() => (summary.value ? summary.value.avgApplicationsPerJob.toFixed(1) : '0.0'))

function tagsOf(job: AdminJobVO): string[] {
  return (job.techStack || '')
    .split(/[,，/\s]+/)
    .map((tag) => tag.trim())
    .filter(Boolean)
}

async function loadData() {
  loading.value = true
  try {
    const [summaryData, data] = await Promise.all([
      fetchAdminJobSummary(),
      fetchAdminJobs(page.value, pageSize.value, filters.status || undefined, filters.keyword || undefined),
    ])
    summary.value = summaryData
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
    <PageHeader
      eyebrow="平台供给治理"
      title="岗位查看"
      description="查看企业岗位供给、开放状态和投递热度，识别零投递岗位和供需偏差。"
    />

    <div class="stat-grid">
      <StatCard label="全部岗位" :value="summary?.jobCount ?? 0" caption="平台岗位供给规模" :icon="Briefcase" />
      <StatCard label="开放岗位" :value="summary?.openJobCount ?? 0" caption="当前可投递岗位" :icon="TrendCharts" tone="green" />
      <StatCard label="总投递" :value="summary?.applicationCount ?? 0" :caption="`均值 ${avgApplications} / 岗位`" :icon="DataAnalysis" />
      <StatCard label="零投递岗位" :value="summary?.noApplicationJobCount ?? 0" caption="需要企业优化 JD 或曝光" :icon="Warning" tone="orange" />
    </div>

    <el-card shadow="never" class="insight-card">
      <div class="insight-title">管理员观察点</div>
      <div class="insight-grid">
        <div>开放岗位占比：{{ summary?.jobCount ? Math.round((summary.openJobCount / summary.jobCount) * 100) : 0 }}%</div>
        <div>已关闭岗位：{{ summary?.closedJobCount ?? 0 }} 个</div>
        <div>治理动作：关注长期零投递岗位，提醒企业补充技术栈、地点和 JD 亮点。</div>
      </div>
    </el-card>

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
        <el-table-column prop="title" label="岗位" min-width="180">
          <template #default="{ row }">
            <el-button text type="primary" @click="selectedJob = row">{{ row.title }}</el-button>
          </template>
        </el-table-column>
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
        <el-table-column label="治理提示" width="140">
          <template #default="{ row }">
            <span v-if="row.applicationCount === 0" class="app-chip app-chip--warning">待提升曝光</span>
            <span v-else class="app-chip app-chip--success">已有转化</span>
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

    <el-drawer :model-value="!!selectedJob" title="岗位运营详情" size="520px" @close="selectedJob = null">
      <template v-if="selectedJob">
        <div class="detail-head">
          <div>
            <h3>{{ selectedJob.title }}</h3>
            <p class="muted">{{ selectedJob.enterpriseName }} · {{ selectedJob.location || '地点不限' }}</p>
          </div>
          <StatusTag kind="job" :value="selectedJob.status" />
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="投递数">{{ selectedJob.applicationCount }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(selectedJob.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDateTime(selectedJob.updatedAt) }}</el-descriptions-item>
        </el-descriptions>
        <div class="drawer-section">
          <h4>技术栈</h4>
          <div class="tag-row">
            <span v-for="tag in tagsOf(selectedJob)" :key="tag" class="app-chip app-chip--soft">{{ tag }}</span>
            <span v-if="!tagsOf(selectedJob).length" class="muted">未填写</span>
          </div>
        </div>
        <div class="drawer-section">
          <h4>运营判断</h4>
          <p class="muted">
            {{
              selectedJob.applicationCount === 0
                ? '该岗位暂无投递，管理员可以提醒企业优化 JD 描述或在演示中说明平台能识别低转化岗位。'
                : '该岗位已有投递，可结合投递状态继续观察企业审核效率。'
            }}
          </p>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.insight-card {
  background: linear-gradient(135deg, rgba(16, 110, 190, 0.05), rgba(34, 184, 239, 0.06));
}

.insight-title {
  font-family: var(--font-display);
  font-weight: 800;
  margin-bottom: 12px;
}

.insight-grid {
  display: grid;
  grid-template-columns: 180px 160px 1fr;
  gap: 12px;
  color: var(--app-text-muted);
  line-height: 1.6;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
}

.detail-head h3 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 20px;
}

.detail-head p {
  margin: 8px 0 0;
}

.drawer-section {
  margin-top: 22px;
}

.drawer-section h4 {
  margin: 0 0 10px;
  font-size: 15px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
