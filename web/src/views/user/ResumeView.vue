<script setup lang="ts">
import {
  Clock,
  Delete,
  Document,
  Edit,
  Folder,
  MagicStick,
  TrendCharts,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type UploadUserFile } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { optimizeResume, scoreResume } from '@/api/ai'
import { fetchCreditBalance } from '@/api/credit'
import {
  createResume,
  deleteResume,
  importPdfResume,
  listLatestResumeScores,
  listResumeOptimizations,
  listResumes,
  updateResume,
} from '@/api/resume'
import OptimizeResultDrawer from '@/components/ai/OptimizeResultDrawer.vue'
import ScoreResultDialog from '@/components/ai/ScoreResultDialog.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import MarkdownEditor from '@/components/common/MarkdownEditor.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import TargetDirectionSelect from '@/components/common/TargetDirectionSelect.vue'
import ResumeFileManager from '@/components/resume/ResumeFileManager.vue'
import {
  EXPERIENCE_LEVELS,
  PROJECT_SCENARIOS,
  RESUME_DIRECTIONS,
  RESUME_HIGHLIGHTS,
} from '@/constants/resumeTemplates'
import { useAuthStore } from '@/stores/auth'
import type { ResumeOptimizeRecordVO, ResumeOptimizeVO, ResumeScoreSummaryVO, ResumeScoreVO } from '@/types/ai'
import type { ResumeVO } from '@/types/resume'
import { formatDateTime } from '@/utils/format'

const auth = useAuthStore()
const loading = ref(false)
const resumes = ref<ResumeVO[]>([])
const scores = ref<ResumeScoreSummaryVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(9)

const scoreMap = computed(() => {
  const map = new Map<number, ResumeScoreSummaryVO>()
  scores.value.forEach((s) => map.set(s.resumeId, s))
  return map
})

function scoreTone(score: number): 'green' | 'orange' | 'red' {
  if (score >= 85) return 'green'
  if (score >= 70) return 'orange'
  return 'red'
}

const historyVisible = ref(false)
const historyLoading = ref(false)
const historyResume = ref<ResumeVO | null>(null)
const historyRecords = ref<ResumeOptimizeRecordVO[]>([])

const editVisible = ref(false)
const editLoading = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ title: '', contentMd: '' })
const createMode = ref<'template' | 'pdf'>('template')
const templateForm = reactive({
  direction: 'Java 后端开发',
  level: '实习生',
  name: '你的姓名',
  targetRole: 'Java 后端开发实习生',
  stacks: ['Java', 'Spring Boot', 'MySQL', 'Redis'],
  scenarios: ['后台管理系统', 'Redis 缓存与登录态', 'AI 简历优化应用'],
  highlights: ['能独立完成前后端联调', '熟悉 RESTful 接口设计', '有 Redis 多场景实践'],
})
const pdfTitle = ref('')
const pdfFiles = ref<UploadUserFile[]>([])
const pdfImportLoading = ref(false)

const fileVisible = ref(false)
const fileResume = ref<ResumeVO | null>(null)

const aiVisible = ref(false)
const aiMode = ref<'score' | 'optimize'>('score')
const aiLoading = ref(false)
const aiResume = ref<ResumeVO | null>(null)
const targetDirection = ref('')
const scoreVisible = ref(false)
const scoreResult = ref<ResumeScoreVO | null>(null)
const optimizeVisible = ref(false)
const optimizeResult = ref<ResumeOptimizeVO | null>(null)

const directionMeta = computed(() => {
  return RESUME_DIRECTIONS.find((item) => item.value === templateForm.direction) || RESUME_DIRECTIONS[0]
})

const templateMarkdown = computed(() => {
  const level = EXPERIENCE_LEVELS.find((item) => item.value === templateForm.level) || EXPERIENCE_LEVELS[0]
  const stacks = templateForm.stacks.length ? templateForm.stacks : directionMeta.value.stacks
  return [
    `# ${templateForm.name} - ${templateForm.targetRole}`,
    '',
    '## 求职意向',
    `- 目标方向：${templateForm.direction}`,
    `- 经验定位：${templateForm.level}`,
    `- 个人定位：${level.wording}`,
    '',
    '## 技能栈',
    ...stacks.map((item) => `- ${item}`),
    '',
    '## 项目经历',
    '### AiResume 简历智能优化与岗位匹配系统',
    `项目定位：面向大学生求职场景的智能简历优化、岗位匹配和投递管理系统，重点体现 ${templateForm.direction} 相关能力。`,
    '',
    '主要工作：',
    ...templateForm.scenarios.map((item) => `- 参与 ${item} 相关模块设计、开发与联调，保证演示流程可闭环。`),
    '- 结合 MySQL、Redis 和 Spring Boot 完成业务数据持久化、登录态管理和热点数据缓存。',
    '- 接入 DeepSeek API，完成简历评分、定向优化和岗位匹配，并记录 AI 币流水与 Token 消耗。',
    '',
    '## 项目亮点',
    ...templateForm.highlights.map((item) => `- ${item}`),
    '',
    '## 自我评价',
    `- 对 ${templateForm.direction} 方向有持续实践兴趣，能够围绕真实业务目标完成开发、联调和问题排查。`,
    '- 关注系统可演示性、稳定性和工程交付质量，能够把功能实现和项目叙事结合起来。',
  ].join('\n')
})

async function loadStats() {
  const balance = await fetchCreditBalance()
  auth.updateCreditBalance(balance.balance)
}

async function loadScores() {
  scores.value = await listLatestResumeScores()
}

async function loadData() {
  loading.value = true
  try {
    const [data] = await Promise.all([listResumes(page.value, pageSize.value), loadScores()])
    resumes.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function openHistory(row: ResumeVO) {
  historyResume.value = row
  historyVisible.value = true
  historyLoading.value = true
  try {
    historyRecords.value = await listResumeOptimizations(row.id)
  } finally {
    historyLoading.value = false
  }
}

function openCreate() {
  editingId.value = null
  createMode.value = 'template'
  pdfTitle.value = ''
  pdfFiles.value = []
  Object.assign(form, {
    title: `${templateForm.direction}简历`,
    contentMd: templateMarkdown.value,
  })
  editVisible.value = true
}

function openEdit(row: ResumeVO) {
  editingId.value = row.id
  Object.assign(form, { title: row.title, contentMd: row.contentMd })
  editVisible.value = true
}

async function submitEdit() {
  if (!editingId.value && createMode.value === 'template') {
    form.title = form.title.trim() || `${templateForm.direction}简历`
    form.contentMd = templateMarkdown.value
  }
  if (!form.title.trim() || !form.contentMd.trim()) {
    ElMessage.warning('请填写简历标题和正文内容')
    return
  }
  editLoading.value = true
  try {
    if (editingId.value) {
      await updateResume(editingId.value, { ...form })
      ElMessage.success('简历已更新')
    } else {
      await createResume({ ...form })
      ElMessage.success('简历已创建')
    }
    editVisible.value = false
    await Promise.all([loadData(), loadStats()])
  } finally {
    editLoading.value = false
  }
}

function applyDirection(value: string) {
  const meta = RESUME_DIRECTIONS.find((item) => item.value === value)
  if (!meta) return
  templateForm.targetRole = `${value}实习生`
  templateForm.stacks = meta.stacks.slice(0, 4)
  form.title = `${value}简历`
}

async function submitPdfImport() {
  const rawFile = pdfFiles.value[0]?.raw
  if (!rawFile) {
    ElMessage.warning('请先选择 PDF 文件')
    return
  }
  if (!rawFile.name.toLowerCase().endsWith('.pdf')) {
    ElMessage.warning('第一版仅支持 PDF')
    return
  }
  pdfImportLoading.value = true
  try {
    await importPdfResume(rawFile, pdfTitle.value || undefined)
    ElMessage.success('PDF 已导入并创建简历')
    editVisible.value = false
    await Promise.all([loadData(), loadStats()])
  } finally {
    pdfImportLoading.value = false
  }
}

function openFiles(row: ResumeVO) {
  fileResume.value = row
  fileVisible.value = true
}

async function handleDelete(row: ResumeVO) {
  await ElMessageBox.confirm(`确认删除简历「${row.title}」吗？如存在附件，请先删除附件。`, '删除简历', {
    type: 'warning',
  })
  await deleteResume(row.id)
  ElMessage.success('简历已删除')
  await Promise.all([loadData(), loadStats()])
}

function openAi(row: ResumeVO, mode: 'score' | 'optimize') {
  aiResume.value = row
  aiMode.value = mode
  targetDirection.value = ''
  aiVisible.value = true
}

async function submitAi() {
  if (!aiResume.value || !targetDirection.value.trim()) {
    ElMessage.warning('请选择目标方向')
    return
  }
  aiLoading.value = true
  try {
    if (aiMode.value === 'score') {
      scoreResult.value = await scoreResume(aiResume.value.id, { targetDirection: targetDirection.value })
      scoreVisible.value = true
      await loadScores()
    } else {
      optimizeResult.value = await optimizeResume(aiResume.value.id, { targetDirection: targetDirection.value })
      optimizeVisible.value = true
    }
    aiVisible.value = false
    await loadStats()
  } finally {
    aiLoading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadData(), loadStats()])
})
</script>

<template>
  <div class="page">
    <PageHeader title="我的简历" description="维护简历正文、管理原始附件，并调用真实 AI 评分和优化。">
      <template #actions>
        <el-button type="primary" @click="openCreate">新建简历</el-button>
      </template>
    </PageHeader>

    <div v-loading="loading">
      <div v-if="resumes.length" class="resume-grid">
        <article v-for="resume in resumes" :key="resume.id" class="resume-card surface-card is-hoverable">
          <div class="card-top">
            <div class="card-icon icon-badge"><el-icon><Document /></el-icon></div>
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
          </div>

          <h3 class="card-title">{{ resume.title }}</h3>
          <div class="card-time muted">更新于 {{ formatDateTime(resume.updatedAt) }}</div>

          <el-button class="is-ai card-cta" :icon="MagicStick" @click="openAi(resume, 'optimize')">AI 润色</el-button>

          <div class="card-actions">
            <el-tooltip content="编辑" placement="top">
              <button class="act-btn" @click="openEdit(resume)"><el-icon><Edit /></el-icon></button>
            </el-tooltip>
            <el-tooltip content="AI 评分" placement="top">
              <button class="act-btn" @click="openAi(resume, 'score')"><el-icon><TrendCharts /></el-icon></button>
            </el-tooltip>
            <el-tooltip content="润色历史" placement="top">
              <button class="act-btn" @click="openHistory(resume)"><el-icon><Clock /></el-icon></button>
            </el-tooltip>
            <el-tooltip content="附件" placement="top">
              <button class="act-btn" @click="openFiles(resume)"><el-icon><Folder /></el-icon></button>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <button class="act-btn danger" @click="handleDelete(resume)"><el-icon><Delete /></el-icon></button>
            </el-tooltip>
          </div>
        </article>
      </div>
      <EmptyState v-else title="还没有简历" description="点击右上角「新建简历」，按模板或导入 PDF 创建。" />

      <div v-if="total > pageSize" class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          layout="total, prev, pager, next"
          :total="total"
          @current-change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="editVisible" :title="editingId ? '编辑简历' : '新建简历'" width="980px" destroy-on-close>
      <template v-if="editingId">
        <el-form label-position="top">
          <el-form-item label="标题">
            <el-input v-model="form.title" maxlength="128" show-word-limit />
          </el-form-item>
          <el-form-item label="正文内容">
            <MarkdownEditor v-model="form.contentMd" />
          </el-form-item>
        </el-form>
      </template>

      <template v-else>
        <el-tabs v-model="createMode" class="create-tabs">
          <el-tab-pane label="按模板创建" name="template">
            <div class="template-layout">
              <section class="template-panel">
                <el-form label-position="top">
                  <el-form-item label="简历标题">
                    <el-input v-model="form.title" maxlength="128" show-word-limit />
                  </el-form-item>
                  <div class="two-column-grid">
                    <el-form-item label="姓名">
                      <el-input v-model="templateForm.name" placeholder="用于生成草稿标题" />
                    </el-form-item>
                    <el-form-item label="目标岗位">
                      <el-input v-model="templateForm.targetRole" placeholder="如 Java 后端开发实习生" />
                    </el-form-item>
                  </div>
                  <div class="two-column-grid">
                    <el-form-item label="目标方向">
                      <el-select v-model="templateForm.direction" style="width: 100%" @change="applyDirection">
                        <el-option
                          v-for="item in RESUME_DIRECTIONS"
                          :key="item.value"
                          :label="item.label"
                          :value="item.value"
                        />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="经验定位">
                      <el-select v-model="templateForm.level" style="width: 100%">
                        <el-option
                          v-for="item in EXPERIENCE_LEVELS"
                          :key="item.value"
                          :label="item.label"
                          :value="item.value"
                        />
                      </el-select>
                    </el-form-item>
                  </div>
                  <el-alert :title="directionMeta.summary" type="primary" :closable="false" show-icon />
                  <el-form-item label="核心技能">
                    <el-checkbox-group v-model="templateForm.stacks">
                      <el-checkbox-button v-for="item in directionMeta.stacks" :key="item" :label="item" />
                    </el-checkbox-group>
                  </el-form-item>
                  <el-form-item label="项目场景">
                    <el-checkbox-group v-model="templateForm.scenarios">
                      <el-checkbox v-for="item in PROJECT_SCENARIOS" :key="item" :label="item" />
                    </el-checkbox-group>
                  </el-form-item>
                  <el-form-item label="亮点表达">
                    <el-checkbox-group v-model="templateForm.highlights">
                      <el-checkbox v-for="item in RESUME_HIGHLIGHTS" :key="item" :label="item" />
                    </el-checkbox-group>
                  </el-form-item>
                </el-form>
              </section>
              <section class="template-preview">
                <div class="preview-head">
                  <strong>生成预览</strong>
                  <span>实时生成，创建后可继续编辑</span>
                </div>
                <pre>{{ templateMarkdown }}</pre>
              </section>
            </div>
          </el-tab-pane>

          <el-tab-pane label="导入 PDF 创建" name="pdf">
            <div class="pdf-import">
              <el-alert
                title="第一版只支持可复制文字的 PDF，不支持扫描件 OCR。导入后会自动创建简历，并把原 PDF 保存为附件。"
                type="info"
                show-icon
                :closable="false"
              />
              <el-form label-position="top">
                <el-form-item label="简历标题（可选）">
                  <el-input v-model="pdfTitle" maxlength="128" show-word-limit placeholder="不填则使用 PDF 文件名" />
                </el-form-item>
                <el-form-item label="PDF 文件">
                  <el-upload
                    v-model:file-list="pdfFiles"
                    drag
                    :auto-upload="false"
                    :limit="1"
                    accept=".pdf,application/pdf"
                  >
                    <div class="upload-text">
                      <strong>拖拽 PDF 到这里，或点击选择文件</strong>
                      <span>导入不会扣 AI 币，后续可再手动点击 AI 优化</span>
                    </div>
                  </el-upload>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>

      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button
          v-if="editingId || createMode === 'template'"
          type="primary"
          :loading="editLoading"
          @click="submitEdit"
        >
          {{ editingId ? '保存' : '按模板创建' }}
        </el-button>
        <el-button v-else type="primary" :loading="pdfImportLoading" @click="submitPdfImport">导入并创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="fileVisible" :title="`附件管理：${fileResume?.title || ''}`" width="820px" destroy-on-close>
      <ResumeFileManager v-if="fileResume" :resume-id="fileResume.id" />
    </el-dialog>

    <el-dialog v-model="aiVisible" :title="aiMode === 'score' ? 'AI 简历评分' : 'AI 简历优化'" width="520px">
      <TargetDirectionSelect v-model="targetDirection" />
      <template #footer>
        <el-button @click="aiVisible = false">取消</el-button>
        <el-button type="primary" :loading="aiLoading" @click="submitAi">开始调用</el-button>
      </template>
    </el-dialog>

    <ScoreResultDialog v-model:visible="scoreVisible" :result="scoreResult" />
    <OptimizeResultDrawer v-model:visible="optimizeVisible" :result="optimizeResult" />

    <el-drawer v-model="historyVisible" :title="`润色历史：${historyResume?.title || ''}`" size="560px">
      <div v-loading="historyLoading">
        <div v-if="historyRecords.length" class="history-list">
          <article v-for="record in historyRecords" :key="record.id" class="history-card surface-card">
            <div class="history-head">
              <span class="app-chip app-chip--ai">{{ record.targetDirection }}</span>
              <span class="muted history-time">{{ formatDateTime(record.createdAt) }}</span>
            </div>
            <p class="history-summary">{{ record.summary }}</p>
            <div v-if="record.optimizedBullets.length" class="history-block">
              <strong>可替换表达</strong>
              <ul>
                <li v-for="item in record.optimizedBullets" :key="item">{{ item }}</li>
              </ul>
            </div>
            <div v-if="record.rewriteSuggestions.length" class="history-block">
              <strong>改写方向</strong>
              <ul>
                <li v-for="item in record.rewriteSuggestions" :key="item">{{ item }}</li>
              </ul>
            </div>
            <div class="muted history-meta">{{ record.llmModel }} · {{ record.totalTokens }} tokens · {{ record.latencyMs }}ms</div>
          </article>
        </div>
        <EmptyState v-else title="暂无润色记录" description="对这份简历点击「AI 润色」后，结果会自动保存在这里。" />
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.resume-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
}

.resume-card {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-icon {
  font-size: 20px;
}

.card-title {
  margin: 4px 0 0;
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-time {
  font-size: 12px;
}

.card-cta {
  margin-top: 8px;
  width: 100%;
}

.card-actions {
  margin-top: 6px;
  padding-top: 14px;
  border-top: 1px solid var(--app-surface-soft);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.act-btn {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border: none;
  border-radius: 10px;
  background: var(--app-surface-soft);
  color: #5a6c82;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.16s ease, color 0.16s ease;
}

.act-btn:hover {
  background: var(--brand-50);
  color: var(--brand-600);
}

.act-btn.danger:hover {
  background: rgba(226, 61, 75, 0.1);
  color: var(--app-danger);
}

.pager {
  margin-top: 22px;
  display: flex;
  justify-content: flex-end;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.history-card {
  padding: 16px;
}

.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.history-time {
  font-size: 12px;
}

.history-summary {
  margin: 0 0 12px;
  line-height: 1.7;
  color: #3a4a5e;
}

.history-block {
  margin-bottom: 10px;
}

.history-block strong {
  display: block;
  margin-bottom: 4px;
  font-size: 13px;
  color: var(--app-text);
}

.history-block ul {
  margin: 0;
  padding-left: 18px;
}

.history-block li {
  line-height: 1.7;
  font-size: 13px;
  color: #44566c;
}

.history-meta {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--app-border);
  font-size: 12px;
}

.create-tabs {
  margin-top: -8px;
}

.template-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(320px, 0.75fr);
  gap: 18px;
}

.template-panel {
  min-width: 0;
}

.template-panel :deep(.el-checkbox-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 10px;
}

.template-preview {
  min-height: 560px;
  padding: 14px;
  border: 1px solid var(--app-border);
  border-radius: 10px;
  background: #f8fbff;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.template-preview pre {
  max-height: 510px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  color: #334155;
  line-height: 1.65;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
}

.pdf-import {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.pdf-import :deep(.el-upload),
.pdf-import :deep(.el-upload-dragger) {
  width: 100%;
}

.upload-text {
  min-height: 180px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
}

.upload-text strong {
  color: #102033;
  font-size: 16px;
}

.upload-text span {
  color: var(--app-text-muted);
}
</style>
