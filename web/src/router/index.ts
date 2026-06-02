import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { UserRole } from '@/types/api'

declare module 'vue-router' {
  interface RouteMeta {
    guest?: boolean
    roles?: UserRole[]
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: () => import('@/layouts/AuthLayout.vue'),
      meta: { guest: true },
      children: [{ path: '', name: 'login', component: () => import('@/views/auth/LoginView.vue') }],
    },
    {
      path: '/register',
      component: () => import('@/layouts/AuthLayout.vue'),
      meta: { guest: true },
      children: [{ path: '', name: 'register', component: () => import('@/views/auth/RegisterView.vue') }],
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          redirect: () => useAuthStore().defaultRoute(),
        },
        {
          path: 'user/dashboard',
          name: 'user-dashboard',
          component: () => import('@/views/user/UserDashboardView.vue'),
          meta: { roles: ['USER'] },
        },
        {
          path: 'user/resumes',
          name: 'user-resumes',
          component: () => import('@/views/user/ResumeView.vue'),
          meta: { roles: ['USER'] },
        },
        {
          path: 'user/jobs',
          name: 'user-jobs',
          component: () => import('@/views/user/JobView.vue'),
          meta: { roles: ['USER'] },
        },
        {
          path: 'user/applications',
          name: 'user-applications',
          component: () => import('@/views/user/ApplicationView.vue'),
          meta: { roles: ['USER'] },
        },
        {
          path: 'user/credits',
          name: 'user-credits',
          component: () => import('@/views/user/CreditView.vue'),
          meta: { roles: ['USER'] },
        },
        {
          path: 'enterprise/dashboard',
          name: 'enterprise-dashboard',
          component: () => import('@/views/enterprise/EnterpriseDashboardView.vue'),
          meta: { roles: ['ENTERPRISE'] },
        },
        {
          path: 'enterprise/jobs',
          name: 'enterprise-jobs',
          component: () => import('@/views/enterprise/EnterpriseJobView.vue'),
          meta: { roles: ['ENTERPRISE'] },
        },
        {
          path: 'enterprise/applications',
          name: 'enterprise-applications',
          component: () => import('@/views/enterprise/EnterpriseApplicationView.vue'),
          meta: { roles: ['ENTERPRISE'] },
        },
        {
          path: 'admin/dashboard',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/AdminDashboardView.vue'),
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'admin/users',
          name: 'admin-users',
          component: () => import('@/views/admin/AdminUserView.vue'),
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'admin/jobs',
          name: 'admin-jobs',
          component: () => import('@/views/admin/AdminJobView.vue'),
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'admin/applications',
          name: 'admin-applications',
          component: () => import('@/views/admin/AdminApplicationView.vue'),
          meta: { roles: ['ADMIN'] },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.initialized) await auth.fetchCurrentUser()

  if (to.meta.guest) {
    return auth.isLoggedIn ? auth.defaultRoute() : true
  }

  if (!auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  const roles = to.meta.roles
  if (roles && auth.role && !roles.includes(auth.role)) {
    ElMessage.warning('无权限访问该页面')
    return auth.defaultRoute()
  }

  return true
})

export default router
