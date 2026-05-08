<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/user'
import Toast from '@/components/Toast.vue'

const router = useRouter()
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: '',
  phone: '',
  inviteCode: '',
})
const loading = ref(false)
const toast = ref(null)
const showInvite = ref(false)

async function handleRegister() {
  if (!form.username || !form.password) {
    toast.value?.show('请填写用户名和密码', 'error')
    return
  }
  if (form.username.length < 3 || form.username.length > 20) {
    toast.value?.show('用户名需要 3-20 个字符', 'error')
    return
  }
  if (form.password !== form.confirmPassword) {
    toast.value?.show('两次密码不一致', 'error')
    return
  }
  if (form.password.length < 6) {
    toast.value?.show('密码至少6位', 'error')
    return
  }
  if (!form.email) {
    toast.value?.show('请填写邮箱', 'error')
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    toast.value?.show('邮箱格式不正确', 'error')
    return
  }
  if (form.phone && !/^1[3-9]\d{9}$/.test(form.phone)) {
    toast.value?.show('手机号格式不正确（11位手机号）', 'error')
    return
  }
  loading.value = true
  try {
    const payload = {
      username: form.username,
      password: form.password,
      nickname: form.nickname || form.username,
      email: form.email || '',
      phone: form.phone || '',
    }
    // 管理端注册走独立接口
    if (form.inviteCode) {
      payload.inviteCode = form.inviteCode
      await userApi.adminRegister(payload)
      toast.value?.show('管理员注册成功，请登录', 'success')
    } else {
      await userApi.register(payload)
      toast.value?.show('注册成功，请登录', 'success')
    }
    setTimeout(() => router.push('/login'), 1000)
  } catch (e) {
    toast.value?.show(e.message || '注册失败', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card card">
      <div class="auth-header">
        <h1>创建账号</h1>
        <p>加入闲鱼集市，发现好物</p>
      </div>

      <form @submit.prevent="handleRegister" class="auth-form">
        <div class="form-group">
          <label class="form-label">用户名 <span class="required">*</span></label>
          <input v-model="form.username" type="text" class="form-input" placeholder="字母、数字、下划线" autocomplete="username" />
        </div>

        <div class="form-group">
          <label class="form-label">昵称</label>
          <input v-model="form.nickname" type="text" class="form-input" placeholder="你的昵称" />
        </div>

        <div class="form-group">
          <label class="form-label">密码 <span class="required">*</span></label>
          <input v-model="form.password" type="password" class="form-input" placeholder="至少6位密码" autocomplete="new-password" />
        </div>

        <div class="form-group">
          <label class="form-label">确认密码 <span class="required">*</span></label>
          <input v-model="form.confirmPassword" type="password" class="form-input" placeholder="再次输入密码" autocomplete="new-password" />
        </div>

        <div class="form-group">
          <label class="form-label">邮箱 <span class="required">*</span></label>
          <input v-model="form.email" type="email" class="form-input" placeholder="用于找回密码" />
        </div>

        <div class="form-group">
          <label class="form-label">手机号 <span class="optional">(选填)</span></label>
          <input v-model="form.phone" type="text" class="form-input" placeholder="手机号码" />
        </div>

        <!-- invite code toggle -->
        <div class="invite-toggle" v-if="!showInvite">
          <button type="button" class="btn btn-ghost btn-sm" @click="showInvite = true">+ 使用邀请码注册管理员</button>
        </div>
        <div class="form-group" v-else>
          <label class="form-label">邀请码</label>
          <div class="invite-row">
            <input v-model="form.inviteCode" type="text" class="form-input" placeholder="输入管理员邀请码" />
            <button type="button" class="btn btn-ghost btn-sm" @click="showInvite = false; form.inviteCode = ''">取消</button>
          </div>
        </div>

        <button type="submit" class="btn btn-primary btn-lg" style="width:100%" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>

        <p class="auth-footer">
          已有账号？<router-link to="/login" class="link">去登录</router-link>
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
  max-width: 440px;
  padding: 40px 36px;
}
.auth-header { text-align: center; margin-bottom: 32px; }
.auth-header h1 { font-size: 1.6rem; font-weight: 700; margin-bottom: 4px; }
.auth-header p { color: var(--text-secondary); font-size: 0.9rem; }
.required { color: var(--primary); }
.optional { color: var(--text-muted); font-weight: 400; }
.link { color: var(--primary); font-size: 0.88rem; font-weight: 500; }
.link:hover { text-decoration: underline; }
.auth-footer { text-align: center; margin-top: 20px; font-size: 0.9rem; color: var(--text-secondary); }

.invite-toggle {
  margin-bottom: 18px;
}
.invite-toggle button {
  color: var(--text-muted);
  font-size: 0.8rem;
}
.invite-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.invite-row .form-input {
  flex: 1;
}
</style>
