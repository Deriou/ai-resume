<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { matchJob } from '@/api/ai'
import { createApplication } from '@/api/application'
import { fetchCreditBalance } from '@/api/credit'
import { getJob, listHotCompanies, listHotJobs, listOpenJobs } from '@/api/job'
import { listResumes } from '@/api/resume'
import MatchResultDialog from '@/components/ai/MatchResultDialog.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { useAuthStore } from '@/stores/auth'
import type { JobMatchVO } from '@/types/ai'
import type { HotCompanyVO, HotJobVO, JobVO } from '@/types/job'
import type { ResumeVO } from '@/types/resume'
import { formatDateTime } from '@/utils/format'

const auth = useAuthStore()
const loading = ref(false)
const jobs = ref<JobVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const resumes = ref<ResumeVO[]>([])
const hotJobs = ref<HotJobVO[]>([])
const hotCompanies = ref<HotCompanyVO[]>([])

const detailVisible = ref(false)
const detailJob = ref<JobVO | null>(null)
const matchVisible = ref(false)
const matchDialogVisible = ref(false)
const matchLoading = ref(false)
const matchTarget = ref<JobVO | null>(null)
const matchResumeId = ref<number | null>(null)
const matchResult = ref<JobMatchVO | null>(null)

const applyVisible = ref(false)
const applyLoading = ref(false)
const applyTarget = ref<JobVO | null>(null)
const applyResumeId = ref<number | null>(null)
const applyRemark = ref('')

async function loadData() {
  loading.value = true
  try {
    const [jobPage, resumePage, hotJobList, hotCompanyList] = await Promise.all([
      listOpenJobs(page.value, pageSize.value),
      listResumes(1, 100),
      listHotJobs(),
      listHotCompanies(),
    ])
    jobs.value = jobPage.records
    total.value = jobPage.total
    resumes.value = resumePage.records
    hotJobs.value = hotJobList
    hotCompanies.value = hotCompanyList
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
  <div class="page">
    <PageHeader title="岗位列表" description="查看开放岗位、热点数据，并使用 AI 做岗位匹配。" />

    <div class="two-column-grid">
      <el-card shadow="never" header="热门岗位">
        <el-table :data="hotJobs" size="small" empty-text="暂无热门岗位">
          <el-table-column prop="title" label="岗位" min-width="160" />
          <el-table-column prop="location" label="地点" width="100" />
          <el-table-column prop="applicationCount" label="投递数" width="90" />
        </el-table>
      </el-card>
      <el-card shadow="never" header="热门公司">
        <el-table :data="hotCompanies" size="small" empty-text="暂无热门公司">
          <el-table-column prop="enterpriseName" label="企业" min-width="160" />
          <el-table-column prop="openJobCount" label="开放岗位" width="100" />
          <el-table-column prop="applicationCount" label="投递数" width="90" />
        </el-table>
      </el-card>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="jobs" empty-text="暂无开放岗位">
        <el-table-column prop="title" label="岗位名称" min-width="180" />
        <el-table-column prop="techStack" label="技术栈" min-width="220" show-overflow-tooltip />
        <el-table-column prop="location" label="地点" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag kind="job" :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="发布时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" :disabled="!resumes.length" @click="openMatch(row)">AI 匹配</el-button>
            <el-button link type="success" :disabled="!resumes.length" @click="openApply(row)">投递</el-button>
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
      </el-form>
      <template #footer>
        <el-button @click="matchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="matchLoading" @click="submitMatch">开始匹配</el-button>
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
