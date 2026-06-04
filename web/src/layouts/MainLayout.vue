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
    <el-aside width="248px" class="sidebar">
      <div class="logo-block">
        <div class="logo">AR</div>
        <div>
          <strong>AiResume</strong>
          <span>{{ ROLE_LABEL_MAP[auth.role || 'USER'] || auth.role }}</span>
        </div>
      </div>

      <el-menu :default-active="route.path" router class="menu">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-foot">
        <div v-if="auth.role === 'USER'" class="credit-card">
          <div class="credit-card-top">
            <span><el-icon><Coin /></el-icon> AI 币余额</span>
            <strong>{{ auth.creditBalance ?? 0 }}</strong>
          </div>
          <div class="credit-card-tip">AI 调用成功后按规则扣费</div>
        </div>
        <button class="foot-link danger" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon> 退出登录
        </button>
      </div>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <div class="topbar-title">AiResume 工作台</div>
        </div>
        <div class="topbar-actions">
          <el-button :icon="Refresh" circle plain @click="auth.fetchCurrentUser()" title="刷新数据" />
          <div class="user-chip">
            <div class="avatar">{{ (auth.displayName || 'U').slice(0, 1).toUpperCase() }}</div>
            <div class="user-meta">
              <span class="user-name">{{ auth.displayName }}</span>
              <span class="user-role">{{ ROLE_LABEL_MAP[auth.role || 'USER'] || auth.role }}</span>
            </div>
          </div>
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
  display: flex;
  flex-direction: column;
  padding: 8px 0;
}

.logo-block {
  height: 76px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  color: #fff;
  font-weight: 800;
  font-family: var(--font-display);
  background: var(--ai-grad);
  box-shadow: 0 8px 18px rgba(16, 110, 190, 0.3);
}

.logo-block strong,
.logo-block span {
  display: block;
}

.logo-block strong {
  font-family: var(--font-display);
  font-size: 17px;
  color: var(--app-text);
}

.logo-block span {
  margin-top: 2px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.menu {
  flex: 1;
  border-right: none;
  padding: 12px 14px;
}

.menu :deep(.el-menu-item) {
  height: 46px;
  margin-bottom: 4px;
  border-radius: 12px;
  color: #4a5d72;
  font-weight: 600;
}

.menu :deep(.el-menu-item:hover) {
  background: var(--brand-50);
  color: var(--brand-600);
}

.menu :deep(.el-menu-item.is-active) {
  background: var(--brand-grad);
  color: #fff;
  box-shadow: 0 10px 20px rgba(16, 110, 190, 0.26);
}

.menu :deep(.el-menu-item.is-active .el-icon) {
  color: #fff;
}

.sidebar-foot {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.credit-card {
  padding: 14px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(16, 110, 190, 0.08), rgba(34, 184, 239, 0.08));
  border: 1px solid var(--brand-100);
}

.credit-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.credit-card-top span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--app-text-muted);
  font-weight: 600;
}

.credit-card-top strong {
  font-family: var(--font-display);
  font-size: 20px;
  color: var(--brand-600);
}

.credit-card-tip {
  margin-top: 6px;
  font-size: 11px;
  color: var(--app-text-muted);
}

.foot-link {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 11px 14px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: #4a5d72;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.foot-link:hover {
  background: var(--app-surface-soft);
}

.foot-link.danger:hover {
  background: rgba(226, 61, 75, 0.08);
  color: var(--app-danger);
}

.topbar {
  height: 76px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--app-border);
}

.topbar-title {
  font-family: var(--font-display);
  font-weight: 800;
  font-size: 17px;
  color: var(--app-text);
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 14px 6px 6px;
  border-radius: var(--radius-pill);
  background: var(--app-surface-soft);
  border: 1px solid var(--app-border);
}

.avatar {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--ai-grad);
  color: #fff;
  font-weight: 700;
  font-family: var(--font-display);
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.user-name {
  color: var(--app-text);
  font-weight: 700;
  font-size: 13px;
}

.user-role {
  color: var(--app-text-muted);
  font-size: 11px;
}

.main-content {
  padding: 26px 30px 40px;
}
</style>
