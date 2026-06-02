<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { fetchAdminUsers, grantUserCredit } from '@/api/admin'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import type { UserRole, UserStatus } from '@/types/api'
import type { AdminUserVO } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const users = ref<AdminUserVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const filters = reactive<{ role: '' | UserRole; status: '' | UserStatus; keyword: string }>({
  role: '',
  status: '',
  keyword: '',
})

const grantVisible = ref(false)
const grantLoading = ref(false)
const currentUser = ref<AdminUserVO | null>(null)
const grantForm = reactive({ amount: 10, remark: '演示环境补充额度' })

async function loadData() {
  loading.value = true
  try {
    const data = await fetchAdminUsers(
      page.value,
      pageSize.value,
      filters.role || undefined,
      filters.status || undefined,
      filters.keyword || undefined,
    )
    users.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadData()
}

function openGrant(row: AdminUserVO) {
  currentUser.value = row
  grantForm.amount = 10
  grantForm.remark = '演示环境补充额度'
  grantVisible.value = true
}

async function submitGrant() {
  if (!currentUser.value) return
  grantLoading.value = true
  try {
    await grantUserCredit(currentUser.value.id, {
      amount: grantForm.amount,
      remark: grantForm.remark || undefined,
    })
    ElMessage.success('AI 币已发放')
    grantVisible.value = false
    await loadData()
  } finally {
    grantLoading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <PageHeader title="用户管理" description="查看用户、筛选角色状态，并向求职者发放 AI 币。" />

    <el-card shadow="never">
      <div class="toolbar">
        <div class="filter-line">
          <el-select v-model="filters.role" clearable placeholder="角色" style="width: 140px">
            <el-option label="求职者" value="USER" />
            <el-option label="企业" value="ENTERPRISE" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
          <el-select v-model="filters.status" clearable placeholder="状态" style="width: 140px">
            <el-option label="ACTIVE" value="ACTIVE" />
            <el-option label="DISABLED" value="DISABLED" />
          </el-select>
          <el-input v-model="filters.keyword" clearable placeholder="用户名 / 昵称" style="width: 220px" />
          <el-button type="primary" @click="search">查询</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="users" empty-text="暂无用户">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="130" />
        <el-table-column prop="nickName" label="昵称" min-width="140" />
        <el-table-column label="角色" width="110">
          <template #default="{ row }"><StatusTag kind="role" :value="row.role" /></template>
        </el-table-column>
        <el-table-column prop="creditBalance" label="AI 币余额" width="120" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.role !== 'USER'" @click="openGrant(row)">发币</el-button>
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

    <el-dialog v-model="grantVisible" title="发放 AI 币" width="460px">
      <el-form label-position="top">
        <el-form-item label="目标用户">
          <el-input :model-value="`${currentUser?.nickName || ''} (${currentUser?.username || ''})`" disabled />
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="grantForm.amount" :min="1" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="grantForm.remark" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" :loading="grantLoading" @click="submitGrant">确认发放</el-button>
      </template>
    </el-dialog>
  </div>
</template>
