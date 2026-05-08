<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/user'
import Toast from '@/components/Toast.vue'

const router = useRouter()
const step = ref(1)
const form = reactive({ username: '', code: '', newPassword: '', confirmPassword: '' })
const loading = ref(false)
const sending = ref(false)
const toast = ref(null)

async function sendCode() {
  if (!form.username) {
    toast.value?.show('请输入用户名', 'error')
    return
  }
  sending.value = true
  try {
    await userApi.forgotPassword({ username: form.username })
    toast.value?.show('验证码已发送至绑定邮箱', 'success')
    step.value = 2
  } catch (e) {
    toast.value?.show(e.message || '发送失败', 'error')
  } finally {
    sending.value = false
  }
}

async function handleReset() {
  if (!form.code) {
    toast.value?.show('请输入验证码', 'error')
    return
  }
  if (form.newPassword.length < 6) {
    toast.value?.show('密码至少6位', 'error')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    toast.value?.show('两次密码不一致', 'error')
    return
  }
  loading.value = true
  try {
    await userApi.resetPassword({
      username: form.username,
      code: form.code,
      newPassword: form.newPassword,
    })
    toast.value?.show('密码重置成功，请登录', 'success')
    setTimeout(() => router.push('/login'), 1000)
  } catch (e) {
    toast.value?.show(e.message || '重置失败', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card card">
      <div class="auth-header">
        <h1>找回密码</h1>
        <p>{{ step === 1 ? '输入用户名，发送验证码到绑定邮箱' : '输入收到的验证码和新密码' }}</p>
      </div>

      <!-- Step 1: send code -->
      <form v-if="step === 1" @submit.prevent="sendCode">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <input v-model="form.username" type="text" class="form-input" placeholder="输入你的用户名" />
        </div>

        <button type="submit" class="btn btn-primary btn-lg" style="width:100%" :disabled="sending">
          {{ sending ? '发送中...' : '发送验证码' }}
        </button>

        <p class="auth-footer">
          <router-link to="/login" class="link">返回登录</router-link>
        </p>
      </form>

      <!-- Step 2: reset -->
      <form v-else @submit.prevent="handleReset">
        <div class="form-group">
          <label class="form-label">验证码</label>
          <input v-model="form.code" type="text" class="form-input" placeholder="输入6位验证码" maxlength="6" />
        </div>

        <div class="form-group">
          <label class="form-label">新密码</label>
          <input v-model="form.newPassword" type="password" class="form-input" placeholder="至少6位" />
        </div>

        <div class="form-group">
          <label class="form-label">确认密码</label>
          <input v-model="form.confirmPassword" type="password" class="form-input" placeholder="再次输入密码" />
        </div>

        <button type="submit" class="btn btn-primary btn-lg" style="width:100%" :disabled="loading">
          {{ loading ? '重置中...' : '重置密码' }}
        </button>

        <p class="auth-footer">
          <button type="button" class="link" @click="step = 1">重新发送验证码</button>
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
.auth-card { width: 100%; max-width: 420px; padding: 40px 36px; }
.auth-header { text-align: center; margin-bottom: 32px; }
.auth-header h1 { font-size: 1.6rem; font-weight: 700; margin-bottom: 4px; }
.auth-header p { color: var(--text-secondary); font-size: 0.9rem; }
.link { color: var(--primary); font-size: 0.88rem; font-weight: 500; }
.link:hover { text-decoration: underline; }
.auth-footer { text-align: center; margin-top: 20px; font-size: 0.9rem; color: var(--text-secondary); }
</style>
