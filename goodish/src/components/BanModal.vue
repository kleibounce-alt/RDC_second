<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const isBanned = computed(() => userStore.user?.status === 'BANNED')

const banEndTime = computed(() => {
  const t = userStore.user?.banEndTime
  if (!t) return '永久封禁'
  return new Date(t).toLocaleString()
})
</script>

<template>
  <Teleport to="body">
    <div v-if="isBanned" class="ban-banner">
      <span class="ban-icon">&#9888;</span>
      <span class="ban-text">账号已被封禁 · 解封时间：<strong>{{ banEndTime }}</strong> · 仅可浏览，无法操作</span>
    </div>
  </Teleport>
</template>

<style scoped>
.ban-banner {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 20px;
  background: rgba(180, 30, 20, 0.92);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: #fff;
  font-size: 0.85rem;
  animation: slideDown 0.3s ease;
}
.ban-icon {
  font-size: 1rem;
}
.ban-text strong {
  font-weight: 700;
}
@keyframes slideDown {
  from { transform: translateY(-100%); }
  to { transform: translateY(0); }
}
</style>
