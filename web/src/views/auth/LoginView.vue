<script setup lang="ts">
import { RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCaptcha } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const captchaLoading = ref(false)
const captchaImage = ref('')
const form = reactive({
  username: '',
  password: '',
  captchaUuid: '',
  captchaCode: '',
})

async function loadCaptcha() {
  captchaLoading.value = true
  try {
    const data = await fetchCaptcha()
    form.captchaUuid = data.uuid
    captchaImage.value = data.imageBase64
    form.captchaCode = ''
  } finally {
    captchaLoading.value = false
  }
}

async function submit() {
  if (!form.username || !form.password || !form.captchaCode) {
    ElMessage.warning('请填写账号、密码和验证码')
    return
  }
  loading.value = true
  try {
    const user = await auth.login({ ...form })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : auth.defaultRoute(user.role)
    router.replace(redirect)
  } catch {
    await loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<template>
  <div class="auth-card">
    <div class="auth-title">
      <h2>登录 AiResume</h2>
      <p>进入你的角色工作台</p>
    </div>
    <el-form label-position="top" @keyup.enter="submit">
      <el-form-item label="用户名">
        <el-input v-model="form.username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-form-item label="验证码">
        <div class="captcha-row">
          <el-input v-model="form.captchaCode" maxlength="8" placeholder="验证码" />
          <button class="captcha" type="button" @click="loadCaptcha">
            <el-icon v-if="captchaLoading"><RefreshRight /></el-icon>
            <img v-else-if="captchaImage" :src="captchaImage" alt="captcha" />
          </button>
        </div>
      </el-form-item>
      <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">登录</el-button>
    </el-form>
    <div class="switch-line">
      还没有账号？
      <router-link to="/register">注册求职者或企业账号</router-link>
    </div>
  </div>
</template>

<style scoped>
.auth-card {
  width: 100%;
  max-width: 396px;
  padding: 30px;
  border: 1px solid var(--app-border);
  border-radius: 12px;
  background: #fff;
  box-shadow: var(--app-shadow);
}

.auth-title h2 {
  margin: 0;
  font-size: 26px;
  color: #102033;
}

.auth-title p {
  margin: 8px 0 24px;
  color: var(--app-text-muted);
}

.captcha-row {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 132px;
  gap: 10px;
}

.captcha {
  height: 40px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: #f8fbff;
  cursor: pointer;
}

.captcha img {
  max-width: 100%;
  max-height: 36px;
}

.submit {
  width: 100%;
}

.switch-line {
  margin-top: 18px;
  text-align: center;
  color: var(--app-text-muted);
}

.switch-line a {
  color: var(--app-primary);
  font-weight: 700;
}
</style>
