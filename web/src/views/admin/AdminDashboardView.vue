<script setup lang="ts">
import { Coin, DataAnalysis, TrendCharts, User } from '@element-plus/icons-vue'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, onMounted, ref } from 'vue'
import VChart from 'vue-echarts'
import {
  fetchAdminCreditDaily,
  fetchAdminCreditTopUsers,
  fetchAdminLlmDaily,
  fetchAdminLlmOperations,
  fetchAdminOverview,
} from '@/api/admin'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import { OPERATION_LABEL_MAP } from '@/constants/status'
import type { AdminCreditTopUserVO, AdminOverviewVO } from '@/types/admin'
import { formatNumber } from '@/utils/format'

use([CanvasRenderer, LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const loading = ref(false)
const overview = ref<AdminOverviewVO | null>(null)
const topUsers = ref<AdminCreditTopUserVO[]>([])
const tokenTrendOption = ref<Record<string, unknown>>({})
const callTrendOption = ref<Record<string, unknown>>({})
const operationOption = ref<Record<string, unknown>>({})
const creditTrendOption = ref<Record<string, unknown>>({})
const topUserOption = ref<Record<string, unknown>>({})

const cards = computed(() => {
  const o = overview.value
  if (!o) return []
  return [
    { label: '用户总数', value: o.totalUsers, caption: `求职者 ${o.userCount} / 企业 ${o.enterpriseCount}`, icon: User },
    { label: '简历数量', value: o.resumeCount, caption: '求职者维护的简历正文', icon: DataAnalysis },
    { label: '开放岗位', value: o.openJobCount, caption: `全部岗位 ${o.jobCount}`, icon: TrendCharts, tone: 'green' as const },
    { label: '投递数量', value: o.applicationCount, caption: '用户到企业的投递记录', icon: DataAnalysis },
    { label: '今日 AI 调用', value: o.todayAiCalls, caption: `失败 ${o.todayAiFailures} 次`, icon: TrendCharts },
    { label: '今日 Token', value: formatNumber(o.todayTokens), caption: '真实模型 token 消耗', icon: DataAnalysis },
    { label: '今日 AI 币消耗', value: o.todayCreditCost, caption: '业务侧固定扣费', icon: Coin, tone: 'orange' as const },
    { label: '今日平均耗时', value: `${o.todayAvgLatencyMs}ms`, caption: 'LLM 调用平均耗时', icon: TrendCharts },
  ]
})

async function loadData() {
  loading.value = true
  try {
    const [overviewData, llmDaily, operations, creditDaily, topUserData] = await Promise.all([
      fetchAdminOverview(),
      fetchAdminLlmDaily(),
      fetchAdminLlmOperations(),
      fetchAdminCreditDaily(),
      fetchAdminCreditTopUsers(5),
    ])
    overview.value = overviewData
    topUsers.value = topUserData

    tokenTrendOption.value = {
      tooltip: { trigger: 'axis' },
      grid: { left: 36, right: 20, top: 32, bottom: 32 },
      xAxis: { type: 'category', data: llmDaily.map((item) => item.date) },
      yAxis: { type: 'value' },
      series: [{ name: 'Token', type: 'line', smooth: true, areaStyle: {}, data: llmDaily.map((item) => item.totalTokens) }],
    }
    callTrendOption.value = {
      tooltip: { trigger: 'axis' },
      grid: { left: 36, right: 20, top: 32, bottom: 32 },
      xAxis: { type: 'category', data: llmDaily.map((item) => item.date) },
      yAxis: { type: 'value' },
      series: [{ name: '调用次数', type: 'bar', data: llmDaily.map((item) => item.callCount) }],
    }
    operationOption.value = {
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [
        {
          type: 'pie',
          radius: ['42%', '66%'],
          data: operations.map((item) => ({
            name: OPERATION_LABEL_MAP[item.operation] || item.operation,
            value: item.callCount,
          })),
        },
      ],
    }
    creditTrendOption.value = {
      tooltip: { trigger: 'axis' },
      legend: { top: 0 },
      grid: { left: 36, right: 20, top: 44, bottom: 32 },
      xAxis: { type: 'category', data: creditDaily.map((item) => item.date) },
      yAxis: { type: 'value' },
      series: [
        { name: '发放', type: 'line', smooth: true, data: creditDaily.map((item) => item.grantedCredits) },
        { name: '消耗', type: 'line', smooth: true, data: creditDaily.map((item) => item.consumedCredits) },
      ],
    }
    topUserOption.value = {
      tooltip: { trigger: 'axis' },
      grid: { left: 80, right: 24, top: 26, bottom: 28 },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: topUserData.map((item) => item.nickName || item.username).reverse() },
      series: [{ name: 'AI 币消耗', type: 'bar', data: topUserData.map((item) => item.consumedCredits).reverse() }],
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="page">
    <PageHeader title="管理员大盘" description="查看业务运营、LLM 调用、Token 消耗和 AI 币成本趋势。" />

    <div class="stat-grid">
      <StatCard
        v-for="item in cards"
        :key="item.label"
        :label="item.label"
        :value="item.value"
        :caption="item.caption"
        :icon="item.icon"
        :tone="item.tone"
      />
    </div>

    <div class="two-column-grid">
      <el-card shadow="never" header="近 7 日 Token 消耗">
        <VChart autoresize style="height: 310px" :option="tokenTrendOption" />
      </el-card>
      <el-card shadow="never" header="近 7 日 AI 调用次数">
        <VChart autoresize style="height: 310px" :option="callTrendOption" />
      </el-card>
    </div>

    <div class="two-column-grid">
      <el-card shadow="never" header="功能调用占比">
        <VChart autoresize style="height: 310px" :option="operationOption" />
      </el-card>
      <el-card shadow="never" header="AI 币发放 / 消耗趋势">
        <VChart autoresize style="height: 310px" :option="creditTrendOption" />
      </el-card>
    </div>

    <el-card shadow="never" header="用户 AI 币消耗 TOP 5">
      <VChart autoresize style="height: 320px" :option="topUserOption" />
      <el-table :data="topUsers" size="small">
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickName" label="昵称" />
        <el-table-column prop="consumedCredits" label="消耗 AI 币" />
        <el-table-column prop="totalTokens" label="Token" />
        <el-table-column prop="aiCallCount" label="调用次数" />
      </el-table>
    </el-card>
  </div>
</template>
