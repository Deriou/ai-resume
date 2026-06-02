<script setup lang="ts">
import { RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchCaptcha, register } from '@/api/auth'

const router = useRouter()
const loading = ref(false)
const captchaLoading = ref(false)
const captchaImage = ref('')
const form = reactive({
  username: '',
  nickName: '',
  password: '',
  role: 'USER' as 'USER' | 'ENTERPRISE',
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
  if (!form.username || !form.nickName || !form.password || !form.captchaCode) {
    ElMessage.warning('请完整填写注册信息')
    return
  }
  loading.value = true
  try {
    await register({ ...form })
    ElMessage.success('注册成功，请登录')
    router.replace('/login')
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
      <h2>创建账号</h2>
      <p>管理员账号不开放前台注册</p>
    </div>
    <el-form label-position="top" @keyup.enter="submit">
      <el-form-item label="角色">
        <el-segmented
          v-model="form.role"
          :options="[
            { label: '求职者', value: 'USER' },
            { label: '企业', value: 'ENTERPRISE' },
          ]"
        />
      </el-form-item>
      <el-form-item label="用户名">
        <el-input v-model="form.username" maxlength="64" placeholder="用于登录" />
      </el-form-item>
      <el-form-item label="昵称 / 企业名">
        <el-input v-model="form.nickName" maxlength="64" placeholder="展示名称" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" show-password placeholder="6-64 位密码" />
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
      <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">注册</el-button>
    </el-form>
    <div class="switch-line">
      已有账号？
      <router-link to="/login">返回登录</router-link>
    </div>
  </div>
</template>

<style scoped>
.auth-card {
  width: 100%;
  max-width: 420px;
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
