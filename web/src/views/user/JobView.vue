<script setup lang="ts">
import { Location, MagicStick, Promotion, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { matchJob } from '@/api/ai'
import { createApplication } from '@/api/application'
import { fetchCreditBalance } from '@/api/credit'
import { getJob, listHotCompanies, listOpenJobs } from '@/api/job'
import { listResumes } from '@/api/resume'
import MatchResultDialog from '@/components/ai/MatchResultDialog.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { useAuthStore } from '@/stores/auth'
import type { JobMatchVO } from '@/types/ai'
import type { HotCompanyVO, JobVO } from '@/types/job'
import type { ResumeVO } from '@/types/resume'

const auth = useAuthStore()
const loading = ref(false)
const jobs = ref<JobVO[]>([])
const resumes = ref<ResumeVO[]>([])
const hotCompanies = ref<HotCompanyVO[]>([])

const keyword = ref('')
const activeTag = ref('')
const page = ref(1)
const pageSize = 9

const detailVisible = ref(false)
const detailJob = ref<JobVO | null>(null)
const matchDialogVisible = ref(false)
const matchVisible = ref(false)
const matchLoading = ref(false)
const matchTarget = ref<JobVO | null>(null)
const matchResumeId = ref<number | null>(null)
const matchResult = ref<JobMatchVO | null>(null)

const applyVisible = ref(false)
const applyLoading = ref(false)
const applyTarget = ref<JobVO | null>(null)
const applyResumeId = ref<number | null>(null)
const applyRemark = ref('')

function tagsOf(job: JobVO): string[] {
  return (job.techStack || '')
    .split(/[,，/\s]+/)
    .map((t) => t.trim())
    .filter(Boolean)
}

const hotTags = computed(() => {
  const counter = new Map<string, number>()
  jobs.value.forEach((job) => tagsOf(job).forEach((t) => counter.set(t, (counter.get(t) ?? 0) + 1)))
  return [...counter.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, 6)
    .map(([t]) => t)
})

const filteredJobs = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return jobs.value.filter((job) => {
    const hitKw =
      !kw ||
      job.title.toLowerCase().includes(kw) ||
      (job.techStack || '').toLowerCase().includes(kw) ||
      (job.location || '').toLowerCase().includes(kw)
    const hitTag = !activeTag.value || tagsOf(job).includes(activeTag.value)
    return hitKw && hitTag
  })
})

const pagedJobs = computed(() => filteredJobs.value.slice((page.value - 1) * pageSize, page.value * pageSize))

function toggleTag(tag: string) {
  activeTag.value = activeTag.value === tag ? '' : tag
  page.value = 1
}

async function loadData() {
  loading.value = true
  try {
    const [jobPage, resumePage, hotCompanyList] = await Promise.all([
      listOpenJobs(1, 60),
      listResumes(1, 100),
      listHotCompanies(),
    ])
    jobs.value = jobPage.records
    resumes.value = resumePage.records
    hotCompanies.value = hotCompanyList.slice(0, 5)
  } finally {
    loading.value = false
  }
}

async function openDetail(row: JobVO) {
  detailJob.value = await getJob(row.id)
  detailVisible.value = true
}

function openMatch(row: JobVO) {
  matchTarget.value = row
  matchResumeId.value = resumes.value[0]?.id ?? null
  matchDialogVisible.value = true
}

async function submitMatch() {
  if (!matchTarget.value || !matchResumeId.value) {
    ElMessage.warning('请选择简历')
    return
  }
  matchLoading.value = true
  try {
    matchResult.value = await matchJob(matchTarget.value.id, { resumeId: matchResumeId.value })
    matchVisible.value = true
    matchDialogVisible.value = false
    const balance = await fetchCreditBalance()
    auth.updateCreditBalance(balance.balance)
  } finally {
    matchLoading.value = false
  }
}

function openApply(row: JobVO) {
  applyTarget.value = row
  applyResumeId.value = resumes.value[0]?.id ?? null
  applyRemark.value = ''
  applyVisible.value = true
}

async function submitApply() {
  if (!applyTarget.value || !applyResumeId.value) {
    ElMessage.warning('请选择简历')
    return
  }
  applyLoading.value = true
  try {
    await createApplication({
      jobId: applyTarget.value.id,
      resumeId: applyResumeId.value,
      remark: applyRemark.value || undefined,
    })
    ElMessage.success('投递成功')
    applyVisible.value = false
  } finally {
    applyLoading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="page">
    <PageHeader eyebrow="岗位广场" title="发现下一个职业机遇" description="浏览开放岗位，用 AI 评估匹配度，再发起投递。" />

    <section class="search-hero surface-card">
      <el-input
        v-model="keyword"
        size="large"
        class="search-input"
        placeholder="搜索职位、技术栈或地点…"
        :prefix-icon="Search"
        clearable
        @input="page = 1"
      />
      <div v-if="hotTags.length" class="hot-tags">
        <span class="hot-label">热门搜索</span>
        <button
          v-for="tag in hotTags"
          :key="tag"
          class="tag-btn"
          :class="{ active: activeTag === tag }"
          @click="toggleTag(tag)"
        >
          {{ tag }}
        </button>
      </div>
    </section>

    <div class="board">
      <main class="board-main">
        <div class="board-count muted">共 {{ filteredJobs.length }} 个岗位</div>
        <div v-if="pagedJobs.length" class="job-grid">
          <article v-for="job in pagedJobs" :key="job.id" class="job-card surface-card is-hoverable">
            <div class="job-card-head">
              <div class="job-logo">{{ job.title.slice(0, 1) }}</div>
              <StatusTag kind="job" :value="job.status" />
            </div>
            <h3 class="job-title">{{ job.title }}</h3>
            <div class="job-meta muted"><el-icon><Location /></el-icon> {{ job.location || '地点不限' }}</div>
            <div class="job-tags">
              <span v-for="tag in tagsOf(job).slice(0, 4)" :key="tag" class="app-chip app-chip--soft">{{ tag }}</span>
            </div>
            <div class="job-actions">
              <el-button text type="primary" @click="openDetail(job)">详情</el-button>
              <el-button class="is-ai" size="small" :icon="MagicStick" :disabled="!resumes.length" @click="openMatch(job)">
                AI 匹配
              </el-button>
              <el-button type="primary" size="small" :icon="Promotion" :disabled="!resumes.length" @click="openApply(job)">
                投递
              </el-button>
            </div>
          </article>
        </div>
        <EmptyState v-else title="没有匹配的岗位" description="换个关键词或清除筛选标签试试。" />

        <div v-if="filteredJobs.length > pageSize" class="pager">
          <el-pagination
            v-model:current-page="page"
            :page-size="pageSize"
            layout="prev, pager, next"
            :total="filteredJobs.length"
          />
        </div>
      </main>

      <aside class="board-side">
        <section class="surface-card side-card">
          <h4>热门公司</h4>
          <div v-if="hotCompanies.length" class="company-list">
            <div v-for="(c, i) in hotCompanies" :key="c.enterpriseId" class="company-row">
              <span class="rank">{{ i + 1 }}</span>
              <div class="company-body">
                <div class="company-name">{{ c.enterpriseName }}</div>
                <div class="muted company-sub">{{ c.openJobCount }} 个在招 · {{ c.applicationCount }} 投递</div>
              </div>
            </div>
          </div>
          <EmptyState v-else title="暂无数据" />
        </section>
      </aside>
    </div>

    <el-dialog v-model="detailVisible" title="岗位详情" width="720px">
      <template v-if="detailJob">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="岗位">{{ detailJob.title }}</el-descriptions-item>
          <el-descriptions-item label="技术栈">{{ detailJob.techStack || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地点">{{ detailJob.location || '-' }}</el-descriptions-item>
          <el-descriptions-item label="JD"><div class="content-preview">{{ detailJob.jdContent }}</div></el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <el-dialog v-model="matchDialogVisible" title="AI 岗位匹配" width="520px">
      <el-form label-position="top">
        <el-form-item label="选择简历">
          <el-select v-model="matchResumeId" placeholder="请选择简历" style="width: 100%">
            <el-option v-for="item in resumes" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
        <div class="dialog-tip muted">AI 岗位匹配会消耗 1 AI 币。</div>
      </el-form>
      <template #footer>
        <el-button @click="matchDialogVisible = false">取消</el-button>
        <el-button class="is-ai" :loading="matchLoading" @click="submitMatch">开始匹配</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="applyVisible" title="投递岗位" width="520px">
      <el-form label-position="top">
        <el-form-item label="选择简历">
          <el-select v-model="applyResumeId" placeholder="请选择简历" style="width: 100%">
            <el-option v-for="item in resumes" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="applyRemark" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="applyLoading" @click="submitApply">确认投递</el-button>
      </template>
    </el-dialog>

    <MatchResultDialog v-model:visible="matchVisible" :result="matchResult" :job-title="matchTarget?.title" />
  </div>
</template>

<style scoped>
.search-hero {
  padding: 22px;
}

.search-input {
  --el-input-border-radius: var(--radius-pill);
}

.search-input :deep(.el-input__wrapper) {
  border-radius: var(--radius-pill);
  padding: 6px 20px;
}

.hot-tags {
  margin-top: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.hot-label {
  font-size: 13px;
  color: var(--app-text-muted);
  font-weight: 600;
  margin-right: 2px;
}

.tag-btn {
  padding: 6px 14px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--app-border);
  background: var(--app-surface-soft);
  color: #5a6c82;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.16s ease;
}

.tag-btn:hover {
  border-color: var(--brand-300);
  color: var(--brand-600);
}

.tag-btn.active {
  background: var(--brand-grad);
  border-color: transparent;
  color: #fff;
}

.board {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 18px;
  align-items: start;
}

.board-count {
  margin-bottom: 12px;
  font-size: 13px;
}

.job-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.job-card {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.job-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.job-logo {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: var(--brand-50);
  color: var(--brand-600);
  font-family: var(--font-display);
  font-weight: 800;
  font-size: 18px;
}

.job-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
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
  min-height: 24px;
}

.job-actions {
  margin-top: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.pager {
  margin-top: 22px;
  display: flex;
  justify-content: center;
}

.side-card {
  padding: 20px;
}

.side-card h4 {
  margin: 0 0 16px;
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 700;
}

.company-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.company-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rank {
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: var(--brand-50);
  color: var(--brand-600);
  font-weight: 800;
  font-size: 13px;
  flex-shrink: 0;
}

.company-name {
  font-weight: 600;
  font-size: 14px;
}

.company-sub {
  font-size: 12px;
  margin-top: 2px;
}

.dialog-tip {
  font-size: 12px;
}

@media (max-width: 1080px) {
  .board {
    grid-template-columns: 1fr;
  }
}
</style>
