<script setup lang="ts">
import { CircleCheck, DataAnalysis, Stopwatch, Tickets } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { fetchAdminApplicationSummary, fetchAdminApplications } from '@/api/admin'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import type { ApplicationStatus } from '@/types/api'
import type { AdminApplicationSummaryVO, AdminApplicationVO } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const applications = ref<AdminApplicationVO[]>([])
const summary = ref<AdminApplicationSummaryVO | null>(null)
const selectedApplication = ref<AdminApplicationVO | null>(null)
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const status = ref<'' | ApplicationStatus>('')

const reviewedRatePercent = computed(() => Math.round((summary.value?.reviewedRate ?? 0) * 100))

async function loadData() {
  loading.value = true
  try {
    const [summaryData, data] = await Promise.all([
      fetchAdminApplicationSummary(),
      fetchAdminApplications(page.value, pageSize.value, status.value || undefined),
    ])
    summary.value = summaryData
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
    <PageHeader
      eyebrow="投递漏斗治理"
      title="投递查看"
      description="查看求职者投递流转、企业审核状态和待处理压力，支撑平台运营闭环。"
    />

    <div class="stat-grid">
      <StatCard label="总投递" :value="summary?.applicationCount ?? 0" caption="求职者到企业的转化记录" :icon="Tickets" />
      <StatCard label="待处理" :value="summary?.pendingCount ?? 0" caption="需要企业及时响应" :icon="Stopwatch" tone="orange" />
      <StatCard label="已通过" :value="summary?.acceptedCount ?? 0" caption="企业认可的投递" :icon="CircleCheck" tone="green" />
      <StatCard label="处理率" :value="`${reviewedRatePercent}%`" caption="非待处理投递占比" :icon="DataAnalysis" />
    </div>

    <el-card shadow="never" class="funnel-card">
      <div class="funnel-head">
        <div>
          <strong>审核漏斗</strong>
          <div class="muted">用于观察企业是否及时处理投递，避免求职者长时间等待。</div>
        </div>
        <span class="app-chip app-chip--ai">管理员运营视角</span>
      </div>
      <div class="funnel-bars">
        <div class="bar-row">
          <span>已查看</span>
          <el-progress :percentage="summary?.applicationCount ? Math.round((summary.viewedCount / summary.applicationCount) * 100) : 0" />
        </div>
        <div class="bar-row">
          <span>已通过</span>
          <el-progress :percentage="summary?.applicationCount ? Math.round((summary.acceptedCount / summary.applicationCount) * 100) : 0" color="#15a97c" />
        </div>
        <div class="bar-row">
          <span>已拒绝</span>
          <el-progress :percentage="summary?.applicationCount ? Math.round((summary.rejectedCount / summary.applicationCount) * 100) : 0" color="#e23d4b" />
        </div>
      </div>
    </el-card>

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
          <template #default="{ row }">
            <el-button text type="primary" @click="selectedApplication = row">
              {{ row.nickName || row.username }}
            </el-button>
          </template>
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
        <el-table-column label="运营提示" width="140">
          <template #default="{ row }">
            <span v-if="row.status === 'PENDING'" class="app-chip app-chip--warning">待企业处理</span>
            <span v-else class="app-chip app-chip--success">已进入闭环</span>
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

    <el-drawer :model-value="!!selectedApplication" title="投递运营详情" size="540px" @close="selectedApplication = null">
      <template v-if="selectedApplication">
        <div class="detail-head">
          <div>
            <h3>{{ selectedApplication.jobTitle }}</h3>
            <p class="muted">{{ selectedApplication.enterpriseName }} · {{ selectedApplication.resumeTitle }}</p>
          </div>
          <StatusTag kind="application" :value="selectedApplication.status" />
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="求职者">
            {{ selectedApplication.nickName || selectedApplication.username }}（{{ selectedApplication.username }}）
          </el-descriptions-item>
          <el-descriptions-item label="企业">{{ selectedApplication.enterpriseName }}</el-descriptions-item>
          <el-descriptions-item label="投递时间">{{ formatDateTime(selectedApplication.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ formatDateTime(selectedApplication.reviewedAt) }}</el-descriptions-item>
          <el-descriptions-item label="审核备注">{{ selectedApplication.remark || '暂无备注' }}</el-descriptions-item>
        </el-descriptions>
        <div class="drawer-section">
          <h4>运营判断</h4>
          <p class="muted">
            {{
              selectedApplication.status === 'PENDING'
                ? '这条投递尚未处理，可以作为“企业响应效率治理”的演示样本。'
                : '这条投递已经完成企业侧处理，能够进入平台投递闭环统计。'
            }}
          </p>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.funnel-card {
  background: linear-gradient(135deg, rgba(16, 110, 190, 0.05), rgba(21, 169, 124, 0.05));
}

.funnel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.funnel-head strong {
  font-family: var(--font-display);
  font-size: 17px;
}

.funnel-head .muted {
  margin-top: 6px;
  font-size: 13px;
}

.funnel-bars {
  display: grid;
  gap: 12px;
}

.bar-row {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 14px;
  align-items: center;
  color: var(--app-text-muted);
  font-weight: 600;
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
</style>
