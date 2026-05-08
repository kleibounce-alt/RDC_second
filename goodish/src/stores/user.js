import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const roles = ref([])

  const isLoggedIn = computed(() => !!user.value)
  const isAdmin = computed(() => roles.value.includes('ROLE_ADMIN'))

  function setAuth(data) {
    localStorage.setItem('accessToken', data.accessToken)
    if (data.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken)
    }
    localStorage.setItem('roles', JSON.stringify(data.user?.roles || []))
    user.value = data.user
    roles.value = data.user?.roles || []
  }

  async function fetchProfile() {
    try {
      const res = await userApi.profile()
      user.value = res.data
      roles.value = res.data?.roles || []
      localStorage.setItem('roles', JSON.stringify(res.data?.roles || []))
    } catch {
      if (user.value?.status === 'BANNED') return
      logout()
    }
  }

  function logout() {
    const token = localStorage.getItem('accessToken')
    if (token) {
      userApi.logout().catch(() => {})
    }
    localStorage.clear()
    user.value = null
    roles.value = []
  }

  return { user, roles, isLoggedIn, isAdmin, setAuth, fetchProfile, logout }
})
