<script setup lang="ts">
import {
  Briefcase,
  Coin,
  DataBoard,
  Document,
  Refresh,
  SwitchButton,
  Tickets,
  User,
} from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ROLE_LABEL_MAP } from '@/constants/status'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menus = computed(() => {
  if (auth.role === 'USER') {
    return [
      { label: '工作台', path: '/user/dashboard', icon: DataBoard },
      { label: '我的简历', path: '/user/resumes', icon: Document },
      { label: '岗位列表', path: '/user/jobs', icon: Briefcase },
      { label: '我的投递', path: '/user/applications', icon: Tickets },
      { label: 'AI 币中心', path: '/user/credits', icon: Coin },
    ]
  }
  if (auth.role === 'ENTERPRISE') {
    return [
      { label: '企业工作台', path: '/enterprise/dashboard', icon: DataBoard },
      { label: '我的岗位', path: '/enterprise/jobs', icon: Briefcase },
      { label: '收到的投递', path: '/enterprise/applications', icon: Tickets },
    ]
  }
  if (auth.role === 'ADMIN') {
    return [
      { label: '管理员大盘', path: '/admin/dashboard', icon: DataBoard },
      { label: '用户管理', path: '/admin/users', icon: User },
      { label: '岗位查看', path: '/admin/jobs', icon: Briefcase },
      { label: '投递查看', path: '/admin/applications', icon: Tickets },
    ]
  }
  return []
})

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="app-shell">
    <el-aside width="236px" class="sidebar">
      <div class="logo-block">
        <div class="logo">AR</div>
        <div>
          <strong>AiResume</strong>
          <span>Management</span>
        </div>
      </div>
      <el-menu :default-active="route.path" router class="menu">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <div class="topbar-title">AiResume 工作台</div>
          <div class="topbar-subtitle">真实接口联调模式，AI 调用默认走后端 DeepSeek</div>
        </div>
        <div class="topbar-actions">
          <div v-if="auth.role === 'USER'" class="credit-pill">
            <el-icon><Coin /></el-icon>
            AI 币 {{ auth.creditBalance ?? 0 }}
          </div>
          <el-tag>{{ ROLE_LABEL_MAP[auth.role || 'USER'] || auth.role }}</el-tag>
          <span class="user-name">{{ auth.displayName }}</span>
          <el-button :icon="Refresh" plain @click="auth.fetchCurrentUser()">刷新</el-button>
          <el-button :icon="SwitchButton" type="danger" plain @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: var(--app-bg);
}

.sidebar {
  background: #ffffff;
  border-right: 1px solid var(--app-border);
}

.logo-block {
  height: 76px;
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  color: #fff;
  font-weight: 800;
  background: linear-gradient(135deg, #1d4ed8, #38bdf8);
}

.logo-block strong,
.logo-block span {
  display: block;
}

.logo-block strong {
  font-size: 16px;
}

.logo-block span {
  margin-top: 2px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.menu {
  border-right: none;
  padding: 8px 12px;
}

.menu :deep(.el-menu-item) {
  height: 44px;
  border-radius: 8px;
  color: #334155;
}

.menu :deep(.el-menu-item.is-active) {
  background: var(--app-primary-soft);
  color: var(--app-primary);
  font-weight: 700;
}

.topbar {
  height: 76px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.86);
  border-bottom: 1px solid var(--app-border);
}

.topbar-title {
  font-weight: 800;
  color: #102033;
}

.topbar-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: var(--app-text-muted);
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.credit-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: var(--app-primary);
  font-weight: 700;
}

.user-name {
  color: #334155;
  font-weight: 600;
}

.main-content {
  padding: 24px 28px 36px;
}
</style>
