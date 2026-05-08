<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'
import Toast from '@/components/Toast.vue'

const router = useRouter()
const userStore = useUserStore()
const toast = ref(null)

const profileForm = reactive({ nickname: '', email: '', phone: '', avatar: '' })
const avatarUploading = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const emailForm = reactive({ email: '' })
const saving = ref(false)
const changingPw = ref(false)
const bindingEmail = ref(false)

function fillForm() {
  if (userStore.user) {
    Object.assign(profileForm, {
      nickname: userStore.user.nickname || '',
      email: userStore.user.email || '',
      phone: userStore.user.phone || '',
      avatar: userStore.user.avatar || '',
    })
  }
}

onMounted(fillForm)

watch(() => userStore.user, (u) => {
  if (u) fillForm()
})

async function handleAvatarUpload(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    toast.value?.show('请选择图片文件', 'error')
    return
  }
  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('files', file)
    const res = await userApi.uploadAvatar(formData)
    profileForm.avatar = res.data
    toast.value?.show('头像上传成功', 'success')
  } catch (err) {
    toast.value?.show(err.message || '上传失败', 'error')
  } finally {
    avatarUploading.value = false
    e.target.value = ''
  }
}

async function saveProfile() {
  saving.value = true
  try {
    await userApi.updateProfile({
      nickname: profileForm.nickname,
      email: profileForm.email,
      phone: profileForm.phone,
      avatar: profileForm.avatar,
    })
    await userStore.fetchProfile()
    toast.value?.show('保存成功', 'success')
  } catch (e) {
    toast.value?.show(e.message || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

async function changePassword() {
  if (passwordForm.newPassword.length < 6) {
    toast.value?.show('新密码至少6位', 'error')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    toast.value?.show('两次密码不一致', 'error')
    return
  }
  changingPw.value = true
  try {
    await userApi.updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
    toast.value?.show('密码修改成功', 'success')
  } catch (e) {
    toast.value?.show(e.message || '修改失败', 'error')
  } finally {
    changingPw.value = false
  }
}

async function bindEmail() {
  if (!emailForm.email) {
    toast.value?.show('请输入邮箱', 'error')
    return
  }
  bindingEmail.value = true
  try {
    await userApi.bindEmail({ email: emailForm.email })
    await userStore.fetchProfile()
    emailForm.email = ''
    toast.value?.show('邮箱绑定成功', 'success')
  } catch (e) {
    toast.value?.show(e.message || '绑定失败', 'error')
  } finally {
    bindingEmail.value = false
  }
}

function goVip() {
  router.push('/vip')
}
</script>

<template>
  <div class="container profile-page">
    <h1 class="page-title">个人中心</h1>

    <div class="profile-grid">
      <!-- left: user info -->
      <div class="profile-sidebar">
        <div class="card user-card">
          <div class="user-avatar">
            <img v-if="userStore.user?.avatar" :src="userStore.user.avatar" alt="" />
            <span v-else>{{ (userStore.user?.nickname || userStore.user?.username || '?')[0] }}</span>
          </div>
          <h2 class="user-name">{{ userStore.user?.nickname || userStore.user?.username }}</h2>
          <p class="text-secondary text-sm">@{{ userStore.user?.username }}</p>

          <div class="vip-badge" v-if="userStore.user?.vipLevel > 0" :class="'vip-' + userStore.user?.vipLevel">
            VIP{{ userStore.user?.vipLevel }}
          </div>
          <button v-else class="btn btn-outline btn-sm mt-1" @click="goVip">开通 VIP</button>

          <div class="user-meta mt-2">
            <div class="meta-item" v-if="userStore.user?.email">
              <span class="text-xs text-muted">邮箱</span>
              <span class="text-sm">{{ userStore.user.email }}</span>
            </div>
            <div class="meta-item" v-if="userStore.user?.phone">
              <span class="text-xs text-muted">手机</span>
              <span class="text-sm">{{ userStore.user.phone }}</span>
            </div>
            <div class="meta-item">
              <span class="text-xs text-muted">注册时间</span>
              <span class="text-sm">{{ userStore.user?.createdAt?.substring(0, 10) }}</span>
            </div>
          </div>
        </div>

        <div class="card quick-links mt-2">
          <router-link to="/my-products" class="quick-link">我的发布</router-link>
          <router-link to="/my-orders" class="quick-link">我的订单</router-link>
          <router-link to="/wallet" class="quick-link">我的钱包</router-link>
          <router-link to="/favorites" class="quick-link">我的收藏</router-link>
          <router-link to="/follows" class="quick-link">我的关注</router-link>
          <router-link to="/follows?tab=fans" class="quick-link">我的粉丝</router-link>
        </div>
      </div>

      <!-- right: forms -->
      <div class="profile-main">
        <!-- edit profile -->
        <div class="card section-card">
          <h3 class="section-title">编辑资料</h3>
          <form @submit.prevent="saveProfile">
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">昵称</label>
                <input v-model="profileForm.nickname" class="form-input" placeholder="你的昵称" />
              </div>
              <div class="form-group">
                <label class="form-label">头像</label>
                <div class="avatar-upload-row">
                  <div class="avatar-preview-sm" v-if="profileForm.avatar">
                    <img :src="profileForm.avatar" alt="头像预览" />
                  </div>
                  <label class="btn btn-outline upload-label" :class="{ disabled: avatarUploading }">
                    {{ avatarUploading ? '上传中...' : '选择图片' }}
                    <input type="file" accept="image/*" hidden @change="handleAvatarUpload" :disabled="avatarUploading" />
                  </label>
                </div>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">邮箱</label>
                <input v-model="profileForm.email" class="form-input" placeholder="邮箱地址" disabled />
              </div>
              <div class="form-group">
                <label class="form-label">手机号</label>
                <input v-model="profileForm.phone" class="form-input" placeholder="手机号码" />
              </div>
            </div>
            <button type="submit" class="btn btn-primary" :disabled="saving">
              {{ saving ? '保存中...' : '保存修改' }}
            </button>
          </form>
        </div>

        <!-- change password -->
        <div class="card section-card">
          <h3 class="section-title">修改密码</h3>
          <form @submit.prevent="changePassword">
            <div class="form-row" style="grid-template-columns:1fr 1fr 1fr">
              <div class="form-group">
                <label class="form-label">旧密码</label>
                <input v-model="passwordForm.oldPassword" type="password" class="form-input" placeholder="当前密码" />
              </div>
              <div class="form-group">
                <label class="form-label">新密码</label>
                <input v-model="passwordForm.newPassword" type="password" class="form-input" placeholder="至少6位" />
              </div>
              <div class="form-group">
                <label class="form-label">确认密码</label>
                <input v-model="passwordForm.confirmPassword" type="password" class="form-input" placeholder="再次输入" />
              </div>
            </div>
            <button type="submit" class="btn btn-outline" :disabled="changingPw">
              {{ changingPw ? '修改中...' : '修改密码' }}
            </button>
          </form>
        </div>

        <!-- bind email -->
        <div class="card section-card" v-if="!userStore.user?.email">
          <h3 class="section-title">绑定邮箱</h3>
          <p class="text-sm text-secondary mb-2">绑定邮箱后可用于找回密码</p>
          <form @submit.prevent="bindEmail" class="form-inline">
            <input v-model="emailForm.email" type="email" class="form-input" placeholder="输入邮箱地址" style="flex:1" />
            <button type="submit" class="btn btn-primary" :disabled="bindingEmail">
              {{ bindingEmail ? '绑定中...' : '绑定' }}
            </button>
          </form>
        </div>
      </div>
    </div>

    <Toast ref="toast" />
  </div>
</template>

<style scoped>
.profile-page { max-width: 960px; }

.profile-grid {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 24px;
  align-items: start;
}

.profile-sidebar {
  position: sticky;
  top: calc(var(--nav-height) + 20px);
}

.user-card {
  padding: 28px 20px;
  text-align: center;
}
.user-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--primary-light);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.8rem;
  font-weight: 600;
  margin: 0 auto 12px;
  overflow: hidden;
}
.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.user-name {
  font-size: 1.1rem;
  font-weight: 600;
}
.vip-badge {
  display: inline-block;
  margin-top: 6px;
  padding: 3px 14px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
}
.vip-1 { background: #e8f5e9; color: #2e7d32; }
.vip-2 { background: #e3f2fd; color: #1565c0; }
.vip-3 { background: rgba(239,108,0,0.2); color: #ffa726; }
.vip-4 { background: #f3e5f5; color: #7b1fa2; }
.vip-5 { background: #fce4ec; color: #c62828; }

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: left;
}
.meta-item {
  display: flex;
  flex-direction: column;
}

.quick-links {
  padding: 4px;
  display: flex;
  flex-direction: column;
}
.quick-link {
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 0.9rem;
  transition: background var(--transition);
}
.quick-link:hover {
  background: var(--border-light);
}

.profile-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-card {
  padding: 24px;
}
.section-title {
  font-size: 1.05rem;
  font-weight: 600;
  margin-bottom: 18px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.form-inline {
  display: flex;
  gap: 12px;
}

.avatar-upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.avatar-preview-sm {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid var(--border);
  flex-shrink: 0;
}
.avatar-preview-sm img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.upload-label {
  cursor: pointer;
}
.upload-label.disabled {
  opacity: 0.5;
  pointer-events: none;
}

@media (max-width: 768px) {
  .profile-grid { grid-template-columns: 1fr; }
  .profile-sidebar { position: static; }
  .form-row { grid-template-columns: 1fr; }
}
</style>
