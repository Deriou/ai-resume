<script setup lang="ts">
import { Coin, Present, Tickets } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { checkIn, fetchCheckInStatus, fetchCreditBalance, fetchCreditTransactions } from '@/api/credit'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import { CREDIT_TYPE_MAP } from '@/constants/status'
import { useAuthStore } from '@/stores/auth'
import type { CheckInStatusVO, CreditTransactionVO } from '@/types/credit'
import { formatDateTime } from '@/utils/format'

const auth = useAuthStore()
const loading = ref(false)
const checking = ref(false)
const checkStatus = ref<CheckInStatusVO | null>(null)
const transactions = ref<CreditTransactionVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

async function loadData() {
  loading.value = true
  try {
    const [balance, status, tx] = await Promise.all([
      fetchCreditBalance(),
      fetchCheckInStatus(),
      fetchCreditTransactions(page.value, pageSize.value),
    ])
    auth.updateCreditBalance(balance.balance)
    checkStatus.value = status
    transactions.value = tx.records
    total.value = tx.total
  } finally {
    loading.value = false
  }
}

async function handleCheckIn() {
  checking.value = true
  try {
    const status = await checkIn()
    checkStatus.value = status
    auth.updateCreditBalance(status.balance)
    ElMessage.success(`签到成功，获得 ${status.creditReward} AI 币`)
    await loadData()
  } finally {
    checking.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="page">
    <PageHeader title="AI 币中心" description="查看余额、每日签到和 AI 额度流水。" />

    <div class="stat-grid">
      <StatCard label="当前余额" :value="auth.creditBalance ?? 0" caption="余额以 MySQL 为准" :icon="Coin" />
      <StatCard label="今日签到" :value="checkStatus?.checkedIn ? '已签到' : '未签到'" caption="签到奖励 1 AI 币" :icon="Present" tone="green" />
      <StatCard label="流水总数" :value="total" caption="包含发放、签到和 AI 消耗" :icon="Tickets" />
      <StatCard label="AI 扣费规则" value="1 / 2" caption="评分/匹配 1，优化 2" :icon="Coin" />
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <div>
          <strong>每日签到</strong>
          <div class="muted">日期：{{ checkStatus?.date || '-' }}</div>
        </div>
        <el-button type="primary" :disabled="checkStatus?.checkedIn" :loading="checking" @click="handleCheckIn">
          {{ checkStatus?.checkedIn ? '今日已签到' : '签到领 1 AI 币' }}
        </el-button>
      </div>
    </el-card>

    <el-card shadow="never" header="额度流水">
      <el-table :data="transactions" empty-text="暂无流水">
        <el-table-column label="变化" width="110">
          <template #default="{ row }">
            <span :class="row.changeAmount >= 0 ? 'text-success' : 'text-danger'">
              {{ row.changeAmount > 0 ? '+' : '' }}{{ row.changeAmount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="140">
          <template #default="{ row }">{{ CREDIT_TYPE_MAP[row.type] || row.type }}</template>
        </el-table-column>
        <el-table-column prop="balanceAfter" label="变动后余额" width="120" />
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="时间" width="180">
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
