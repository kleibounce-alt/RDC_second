<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'
import axios from 'axios'
import Toast from '@/components/Toast.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const form = reactive({ username: '', password: '', captcha: '', rememberMe: false })
const captchaSrc = ref('')
const captchaUuid = ref('')
const captchaLoading = ref(true)
const loading = ref(false)
const toast = ref(null)

async function refreshCaptcha() {
  captchaLoading.value = true
  try {
    // Try proxy first
    const res = await axios.get('/u/captcha', { responseType: 'blob' })
    captchaUuid.value = res.headers['x-captcha-uuid']
    if (!captchaUuid.value) {
      throw new Error('响应头缺少 X-Captcha-UUID')
    }
    const blob = res.data
    if (captchaSrc.value) URL.revokeObjectURL(captchaSrc.value)
    captchaSrc.value = URL.createObjectURL(blob)
  } catch (e) {
    const errMsg = e.response?.status === 404
      ? '后端未启动或未部署——请确认 Tomcat 在 8080 端口运行且应用已部署'
      : (e.message || '网络错误')
    toast.value?.show('验证码获取失败: ' + errMsg, 'error')
    console.error('[captcha]', e.message)
  } finally {
    captchaLoading.value = false
  }
}

refreshCaptcha()

async function handleLogin() {
  if (!form.username || !form.password) {
    toast.value?.show('请填写用户名和密码', 'error')
    return
  }
  if (!form.captcha) {
    toast.value?.show('请填写验证码', 'error')
    return
  }
  loading.value = true
  try {
    const res = await userApi.login({
      ...form,
      captchaUuid: captchaUuid.value,
    })
    userStore.setAuth(res.data)
    toast.value?.show('登录成功', 'success')
    const redirect = route.query.redirect || '/'
    router.replace(redirect)
  } catch (e) {
    toast.value?.show(e.message || '登录失败', 'error')
    refreshCaptcha()
    form.captcha = ''
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card card">
      <div class="auth-header">
        <h1>欢迎回来</h1>
        <p>登录你的闲鱼集市账号</p>
      </div>

      <form @submit.prevent="handleLogin" class="auth-form">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <input v-model="form.username" type="text" class="form-input" placeholder="输入用户名" autocomplete="username" />
        </div>

        <div class="form-group">
          <label class="form-label">密码</label>
          <input v-model="form.password" type="password" class="form-input" placeholder="输入密码" autocomplete="current-password" />
        </div>

        <div class="form-group">
          <label class="form-label">验证码</label>
          <div class="captcha-row">
            <input v-model="form.captcha" type="text" class="form-input" placeholder="验证码" maxlength="4" style="width:140px" />
            <img v-if="captchaSrc" :src="captchaSrc" @click="refreshCaptcha" class="captcha-img" alt="验证码" title="点击刷新" />
            <div v-else class="captcha-img captcha-placeholder" @click="refreshCaptcha">
              <span v-if="captchaLoading">加载中...</span>
              <span v-else>点击加载</span>
            </div>
          </div>
        </div>

        <div class="flex items-center justify-between" style="margin-bottom:18px">
          <label class="remember-me">
            <input type="checkbox" v-model="form.rememberMe" />
            <span>记住我</span>
          </label>
          <router-link to="/forgot" class="link">忘记密码？</router-link>
        </div>

        <button type="submit" class="btn btn-primary btn-lg" style="width:100%" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <p class="auth-footer">
          还没有账号？<router-link to="/register" class="link">立即注册</router-link>
        </p>
      </form>
    </div>
    <Toast ref="toast" />
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: linear-gradient(135deg, #0f0f14 0%, #1a1520 50%, #0f0f18 100%);
}
.auth-card {
  width: 100%;
  max-width: 420px;
  padding: 40px 36px;
}
.auth-header {
  text-align: center;
  margin-bottom: 32px;
}
.auth-header h1 {
  font-size: 1.6rem;
  font-weight: 700;
  margin-bottom: 4px;
}
.auth-header p {
  color: var(--text-secondary);
  font-size: 0.9rem;
}
.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.captcha-img {
  height: 42px;
  width: 100px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  flex-shrink: 0;
  object-fit: cover;
}
.captcha-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.03);
  font-size: 0.75rem;
  color: var(--text-muted);
}
.remember-me {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.88rem;
  color: var(--text-secondary);
  cursor: pointer;
}
.link {
  color: var(--primary);
  font-size: 0.88rem;
  font-weight: 500;
}
.link:hover {
  text-decoration: underline;
}
.auth-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 0.9rem;
  color: var(--text-secondary);
}
</style>
