import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { clearStoredToken, getStoredToken, setStoredToken } from '@/api/http'
import { fetchMe, login as loginApi, logout as logoutApi } from '@/api/auth'
import type { LoginRequest, LoginResponse } from '@/types/auth'
import type { UserRole } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getStoredToken())
  const user = ref<LoginResponse | null>(null)
  const initialized = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value && user.value))
  const role = computed<UserRole | null>(() => user.value?.role ?? null)
  const displayName = computed(() => user.value?.nickName || user.value?.username || '未登录')
  const creditBalance = computed(() => user.value?.creditBalance ?? null)

  function defaultRoute(roleValue = role.value): string {
    if (roleValue === 'USER') return '/user/dashboard'
    if (roleValue === 'ENTERPRISE') return '/enterprise/dashboard'
    if (roleValue === 'ADMIN') return '/admin/dashboard'
    return '/login'
  }

  function setSession(payload: LoginResponse) {
    token.value = payload.token
    user.value = payload
    setStoredToken(payload.token)
  }

  async function login(data: LoginRequest) {
    const payload = await loginApi(data)
    setSession(payload)
    return payload
  }

  async function fetchCurrentUser() {
    if (!token.value) {
      initialized.value = true
      return null
    }
    try {
      const payload = await fetchMe()
      setSession({ ...payload, token: token.value })
      return payload
    } catch {
      clearSession()
      return null
    } finally {
      initialized.value = true
    }
  }

  function updateCreditBalance(balance: number) {
    if (user.value) {
      user.value = { ...user.value, creditBalance: balance }
    }
  }

  function clearSession() {
    token.value = ''
    user.value = null
    clearStoredToken()
  }

  async function logout() {
    try {
      if (token.value) await logoutApi()
    } finally {
      clearSession()
    }
  }

  return {
    token,
    user,
    initialized,
    isLoggedIn,
    role,
    displayName,
    creditBalance,
    defaultRoute,
    login,
    fetchCurrentUser,
    updateCreditBalance,
    logout,
    clearSession,
  }
})
