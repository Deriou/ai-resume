<script setup lang="ts">
import { Coin, Document, MagicStick, Location, Tickets, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listMyApplications } from '@/api/application'
import { checkIn, fetchCheckInStatus, fetchCreditBalance } from '@/api/credit'
import { listHotJobs } from '@/api/job'
import { listLatestResumeScores, listResumes } from '@/api/resume'
import EmptyState from '@/components/common/EmptyState.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { useAuthStore } from '@/stores/auth'
import type { ResumeScoreSummaryVO } from '@/types/ai'
import type { ApplicationVO } from '@/types/application'
import type { HotJobVO } from '@/types/job'
import type { ResumeVO } from '@/types/resume'
import { formatDate, formatDateTime } from '@/utils/format'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const checking = ref(false)
const resumeTotal = ref(0)
const applicationTotal = ref(0)
const checkedIn = ref(false)
const resumes = ref<ResumeVO[]>([])
const scores = ref<ResumeScoreSummaryVO[]>([])
const hotJobs = ref<HotJobVO[]>([])
const recentApplications = ref<ApplicationVO[]>([])

const scoreMap = computed(() => {
  const map = new Map<number, ResumeScoreSummaryVO>()
  scores.value.forEach((s) => map.set(s.resumeId, s))
  return map
})

const topSuggestion = computed(() => {
  const withSuggestion = [...scores.value]
    .filter((s) => s.suggestions.length)
    .sort((a, b) => a.overallScore - b.overallScore)
  return withSuggestion[0] ?? null
})

function scoreTone(score: number): 'green' | 'orange' | 'red' {
  if (score >= 85) return 'green'
  if (score >= 70) return 'orange'
  return 'red'
}

async function loadData() {
  loading.value = true
  try {
    const [balance, checkStatus, resumePage, applications, jobs, scoreList] = await Promise.all([
      fetchCreditBalance(),
      fetchCheckInStatus(),
      listResumes(1, 4),
      listMyApplications(1, 5),
      listHotJobs(),
      listLatestResumeScores(),
    ])
    auth.updateCreditBalance(balance.balance)
    checkedIn.value = checkStatus.checkedIn
    resumeTotal.value = resumePage.total
    resumes.value = resumePage.records
    applicationTotal.value = applications.total
    recentApplications.value = applications.records
    hotJobs.value = jobs.slice(0, 4)
    scores.value = scoreList
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
    <PageHeader
      eyebrow="求职者工作台"
      :title="`欢迎回来，${auth.displayName}`"
      description="管理简历、寻找岗位，并用 AI 完成评分、优化和匹配。"
    >
      <template #actions>
        <el-button type="primary" @click="router.push('/user/resumes')">新建 / 优化简历</el-button>
        <el-button plain @click="router.push('/user/jobs')">浏览岗位</el-button>
      </template>
    </PageHeader>

    <div class="stat-grid">
      <StatCard label="AI 币余额" :value="auth.creditBalance ?? 0" caption="AI 调用成功后按规则扣费" :icon="Coin" />
      <StatCard label="我的简历" :value="resumeTotal" caption="正文内容是 AI 输入来源" :icon="Document" />
      <StatCard label="我的投递" :value="applicationTotal" caption="查看企业审核状态" :icon="Tickets" />
      <StatCard
        label="今日签到"
        :value="checkedIn ? '已签到' : '未签到'"
        caption="每天可领取 1 AI 币"
        :icon="Coin"
        tone="green"
      />
    </div>

    <div class="dash-grid">
      <div class="dash-main">
        <section class="surface-card block">
          <header class="block-head">
            <div>
              <h3>近期简历</h3>
              <span class="muted">AI 评分越高，命中岗位的概率越大。</span>
            </div>
            <el-button text type="primary" @click="router.push('/user/resumes')">查看全部</el-button>
          </header>

          <div v-if="resumes.length" class="resume-list">
            <article
              v-for="resume in resumes"
              :key="resume.id"
              class="resume-card surface-card is-hoverable"
              @click="router.push('/user/resumes')"
            >
              <div class="resume-icon icon-badge"><el-icon><Document /></el-icon></div>
              <div class="resume-body">
                <div class="resume-title">{{ resume.title }}</div>
                <div class="muted resume-time">更新于 {{ formatDate(resume.updatedAt) }}</div>
              </div>
              <span
                v-if="scoreMap.get(resume.id)"
                class="app-chip"
                :class="{
                  'app-chip--success': scoreTone(scoreMap.get(resume.id)!.overallScore) === 'green',
                  'app-chip--warning': scoreTone(scoreMap.get(resume.id)!.overallScore) !== 'green',
                }"
              >
                <el-icon><MagicStick /></el-icon> AI 评分 {{ scoreMap.get(resume.id)!.overallScore }}
              </span>
              <span v-else class="app-chip app-chip--soft">未评分</span>
            </article>
          </div>
          <EmptyState v-else title="还没有简历" description="先创建一份简历，再用 AI 评分和优化。" />
        </section>

        <section class="surface-card block">
          <header class="block-head">
            <div>
              <h3>热门岗位</h3>
              <span class="muted">来自 Redis 热点缓存的高投递岗位。</span>
            </div>
            <el-button text type="primary" @click="router.push('/user/jobs')">浏览岗位</el-button>
          </header>

          <div v-if="hotJobs.length" class="job-grid">
            <article
              v-for="job in hotJobs"
              :key="job.jobId"
              class="job-card surface-card is-hoverable"
              @click="router.push('/user/jobs')"
            >
              <div class="job-title">{{ job.title }}</div>
              <div class="job-meta muted">
                <el-icon><Location /></el-icon> {{ job.location || '不限' }}
              </div>
              <div class="job-tags">
                <span v-for="tag in (job.techStack || '').split(/[,，/\s]+/).filter(Boolean).slice(0, 3)" :key="tag" class="app-chip app-chip--soft">
                  {{ tag }}
                </span>
              </div>
              <div class="job-foot">
                <span class="app-chip"><el-icon><TrendCharts /></el-icon> {{ job.applicationCount }} 人投递</span>
              </div>
            </article>
          </div>
          <EmptyState v-else title="暂无热门岗位" description="等待企业发布更多岗位。" />
        </section>
      </div>

      <aside class="dash-side">
        <section class="surface-card block check-block">
          <div class="check-info">
            <strong>每日签到</strong>
            <span class="muted">领取免费 AI 币，继续评分、优化和匹配。</span>
          </div>
          <el-button type="primary" :disabled="checkedIn" :loading="checking" @click="handleCheckIn">
            {{ checkedIn ? '今日已签到' : '签到领 1 AI 币' }}
          </el-button>
        </section>

        <section class="surface-card block">
          <header class="block-head">
            <h3>求职追踪</h3>
          </header>
          <div v-if="recentApplications.length" class="track-list">
            <div v-for="app in recentApplications" :key="app.id" class="track-item">
              <div class="track-dot icon-badge"><el-icon><Tickets /></el-icon></div>
              <div class="track-body">
                <div class="track-title">{{ app.jobTitle }}</div>
                <div class="muted track-time">{{ formatDateTime(app.createdAt) }}</div>
              </div>
              <StatusTag kind="application" :value="app.status" />
            </div>
          </div>
          <EmptyState v-else title="暂无投递" description="在岗位列表选择合适岗位发起投递。" />
        </section>

        <section v-if="topSuggestion" class="surface-card block ai-block">
          <header class="block-head">
            <div class="ai-head">
              <span class="icon-badge icon-badge--ai"><el-icon><MagicStick /></el-icon></span>
              <h3>AI 建议</h3>
            </div>
          </header>
          <p class="ai-text">{{ topSuggestion.suggestions[0] }}</p>
          <el-button class="is-ai" @click="router.push('/user/resumes')">去优化简历</el-button>
        </section>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.dash-grid {
  display: grid;
  grid-template-columns: 1.7fr 1fr;
  gap: 18px;
  align-items: start;
}

.dash-main,
.dash-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.block {
  padding: 22px;
}

.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.block-head h3 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 700;
}

.block-head .muted {
  display: block;
  margin-top: 4px;
  font-size: 12px;
}

.resume-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resume-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  cursor: pointer;
}

.resume-body {
  flex: 1;
  min-width: 0;
}

.resume-title {
  font-weight: 700;
  color: var(--app-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resume-time {
  margin-top: 3px;
  font-size: 12px;
}

.job-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.job-card {
  padding: 16px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.job-title {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 15px;
}

.job-meta {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
}

.job-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.job-foot {
  margin-top: 2px;
}

.check-block {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  background: linear-gradient(135deg, rgba(16, 110, 190, 0.06), rgba(34, 184, 239, 0.06));
}

.check-info strong {
  font-family: var(--font-display);
  font-size: 15px;
}

.check-info span {
  display: block;
  margin-top: 4px;
  font-size: 12px;
}

.track-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.track-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.track-body {
  flex: 1;
  min-width: 0;
}

.track-title {
  font-weight: 600;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.track-time {
  font-size: 12px;
  margin-top: 2px;
}

.ai-block {
  background: linear-gradient(150deg, rgba(79, 70, 229, 0.07), rgba(34, 184, 239, 0.07));
  border-color: rgba(79, 70, 229, 0.18);
}

.ai-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-text {
  margin: 0 0 16px;
  line-height: 1.7;
  color: #3a4a5e;
  font-size: 14px;
}

@media (max-width: 1080px) {
  .dash-grid {
    grid-template-columns: 1fr;
  }
  .job-grid {
    grid-template-columns: 1fr;
  }
}
</style>
