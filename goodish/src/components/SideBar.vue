<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { messageApi } from '@/api/message'

defineProps({ collapsed: Boolean })
const emit = defineEmits(['toggle'])

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const unreadCount = ref(0)
const mobileOpen = ref(false)

onMounted(async () => {
  if (localStorage.getItem('accessToken')) {
    await userStore.fetchProfile()
    fetchUnread()
  }
})

async function fetchUnread() {
  if (!userStore.isLoggedIn) return
  try { const res = await messageApi.unreadCount(); unreadCount.value = res.data ?? 0 } catch (e) { console.error('[unread]', e.message) }
}

const navItems = computed(() => {
  const items = [
    { to: '/', label: '首页', icon: '&#9711;' },
    { to: '/publish', label: '发布商品', icon: '&#10009;', auth: true },
    { to: '/my-products', label: '我的发布', icon: '&#9776;', auth: true },
    { to: '/my-orders', label: '我的订单', icon: '&#10003;', auth: true },
    { to: '/wallet', label: '我的钱包', icon: '&#9702;', auth: true },
    { to: '/favorites', label: '我的收藏', icon: '&#9825;', auth: true },
    { to: '/follows', label: '关注', icon: '&#9830;', auth: true },
    { to: '/messages', label: '消息', icon: '&#9993;', badge: unreadCount.value, auth: true },
    { to: '/vip', label: 'VIP 会员', icon: '&#9733;', auth: true },
  ]
  if (userStore.isAdmin) {
    items.push({ to: '/admin', label: '后台管理', icon: '&#9881;', auth: true })
  }
  return items
})

function isActive(to) {
  if (to === '/') return route.path === '/'
  return route.path.startsWith(to)
}

function navigate(item) {
  mobileOpen.value = false
  if (item.auth && !userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  router.push(item.to)
}

async function handleLogout() {
  userStore.logout()
  unreadCount.value = 0
  mobileOpen.value = false
  router.push('/')
}

setInterval(fetchUnread, 30000)
</script>

<template>
  <!-- mobile overlay -->
  <div v-if="mobileOpen" class="mobile-overlay" @click="mobileOpen = false"></div>

  <aside :class="['sidebar', { collapsed, open: mobileOpen }]">
    <div class="sidebar-inner">
      <!-- logo -->
      <div class="sidebar-logo" @click="router.push('/')">
        <div class="logo-glow"></div>
        <span class="logo-icon">&#9783;</span>
        <span v-if="!collapsed" class="logo-text">闲鱼集市</span>
      </div>

      <button class="collapse-btn" @click="emit('toggle')" v-if="!collapsed">
        <span>&#171;</span>
      </button>
      <button class="collapse-btn" @click="emit('toggle')" v-else>
        <span>&#187;</span>
      </button>

      <!-- nav -->
      <nav class="sidebar-nav">
        <button
          v-for="item in navItems"
          :key="item.to"
          :class="['nav-item', { active: isActive(item.to) }]"
          @click="navigate(item)"
        >
          <span class="nav-icon" v-html="item.icon"></span>
          <span v-if="!collapsed" class="nav-label">{{ item.label }}</span>
          <span v-if="item.badge" class="nav-count">{{ item.badge > 99 ? '99+' : item.badge }}</span>
        </button>
      </nav>

      <!-- user area -->
      <div class="sidebar-footer" v-if="userStore.isLoggedIn">
        <div class="sidebar-user" @click="router.push('/profile')">
          <span class="user-avatar">{{ (userStore.user?.nickname || userStore.user?.username || '?')[0] }}</span>
          <span v-if="!collapsed" class="user-name">{{ userStore.user?.nickname || userStore.user?.username }}</span>
        </div>
        <button class="logout-btn" @click="handleLogout" :title="collapsed ? '退出' : ''">
          <span v-if="!collapsed">退出</span>
          <span v-else>&#10161;</span>
        </button>
      </div>
      <div class="sidebar-footer" v-else>
        <button class="auth-btn" @click="router.push('/login')">
          <span class="nav-icon">&#10164;</span>
          <span v-if="!collapsed">登录</span>
        </button>
      </div>
    </div>
  </aside>

  <!-- mobile bottom bar -->
  <nav class="mobile-nav">
    <button
      v-for="item in navItems.slice(0, 5)"
      :key="item.to"
      :class="['mobile-nav-item', { active: isActive(item.to) }]"
      @click="navigate(item)"
    >
      <span v-html="item.icon"></span>
      <span v-if="item.badge" class="mobile-badge">{{ item.badge > 9 ? '9+' : item.badge }}</span>
    </button>
    <button :class="['mobile-nav-item', { active: mobileOpen }]" @click="mobileOpen = !mobileOpen">
      <span>&#9776;</span>
    </button>
  </nav>
</template>

<style scoped>
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: var(--sidebar-width);
  z-index: 100;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.sidebar.collapsed { width: 72px; }

.sidebar-inner {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: rgba(20, 20, 30, 0.7);
  backdrop-filter: blur(40px) saturate(180%);
  -webkit-backdrop-filter: blur(40px) saturate(180%);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  padding: 20px 12px;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px;
  margin-bottom: 8px;
  cursor: pointer;
  position: relative;
}
.logo-glow {
  position: absolute;
  width: 50px;
  height: 50px;
  left: 2px;
  border-radius: 50%;
  background: var(--primary);
  filter: blur(30px);
  opacity: 0.2;
}
.logo-icon {
  font-size: 1.5rem;
  color: var(--primary);
  position: relative;
  z-index: 1;
  filter: drop-shadow(0 0 8px var(--primary-glow));
}
.logo-text {
  font-weight: 700;
  font-size: 1.05rem;
  background: linear-gradient(135deg, var(--text), var(--primary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.collapse-btn {
  position: absolute;
  top: 50%;
  right: -13px;
  transform: translateY(-50%);
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: rgba(30, 30, 40, 0.9);
  border: 1px solid var(--glass-border);
  color: var(--text-secondary);
  font-size: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  backdrop-filter: blur(10px);
  transition: all var(--transition);
}
.collapse-btn:hover {
  color: var(--text);
  border-color: var(--primary);
}

/* nav */
.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
  padding: 4px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 10px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  transition: all var(--transition);
  position: relative;
  font-size: 0.88rem;
  text-align: left;
  width: 100%;
}
.nav-item:hover {
  background: var(--glass-hover);
  color: var(--text);
}
.nav-item.active {
  background: var(--primary-light);
  color: var(--primary);
  box-shadow: inset 0 0 0 1px rgba(255, 107, 74, 0.2);
}
.nav-item.active::before {
  content: '';
  position: absolute;
  left: -2px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: var(--primary);
  border-radius: 3px;
  box-shadow: 0 0 12px var(--primary-glow);
}
.nav-icon {
  width: 20px;
  text-align: center;
  font-size: 1.05rem;
  flex-shrink: 0;
}
.nav-label { white-space: nowrap; }

.nav-count {
  margin-left: auto;
  background: var(--primary);
  color: #fff;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 1px 7px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
}

/* footer */
.sidebar-footer {
  padding-top: 12px;
  border-top: 1px solid var(--glass-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}
.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  transition: background var(--transition);
  flex: 1;
  overflow: hidden;
}
.sidebar-user:hover { background: var(--glass-hover); }
.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--primary-light);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.8rem;
  flex-shrink: 0;
  border: 1px solid rgba(255, 107, 74, 0.3);
}
.user-name {
  font-size: 0.85rem;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.logout-btn {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 0.8rem;
  color: var(--text-muted);
  transition: all var(--transition);
  flex-shrink: 0;
}
.logout-btn:hover {
  color: #ff6b6b;
  background: rgba(231, 76, 60, 0.1);
}
.auth-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  color: var(--primary);
  font-weight: 500;
  font-size: 0.88rem;
  width: 100%;
  transition: background var(--transition);
}
.auth-btn:hover { background: var(--primary-light); }

/* mobile */
.mobile-overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 99;
}
.mobile-nav {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(20, 20, 30, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-top: 1px solid var(--glass-border);
  padding: 6px 8px;
  z-index: 100;
  justify-content: space-around;
}
.mobile-nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 1.1rem;
  color: var(--text-muted);
  transition: all var(--transition);
  position: relative;
}
.mobile-nav-item.active { color: var(--primary); }
.mobile-badge {
  position: absolute;
  top: -2px;
  right: -4px;
  background: var(--primary);
  color: #fff;
  font-size: 0.6rem;
  font-weight: 700;
  width: 15px;
  height: 15px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.collapsed .sidebar-logo { padding: 6px 4px; justify-content: center; }
.collapsed .nav-item { justify-content: center; padding: 10px 0; }
.collapsed .nav-count { position: absolute; top: 2px; right: 4px; min-width: 16px; padding: 0 4px; font-size: 0.6rem; }
.collapsed .sidebar-footer { flex-direction: column; }
.collapsed .sidebar-user { justify-content: center; }
.collapsed .logout-btn { padding: 6px; }
.collapsed .auth-btn { justify-content: center; }

@media (max-width: 768px) {
  .sidebar { transform: translateX(-100%); transition: transform 0.3s ease; }
  .sidebar.open { transform: translateX(0); }
  .sidebar.collapsed { width: var(--sidebar-width); transform: translateX(-100%); }
  .collapse-btn { display: none; }
  .mobile-overlay { display: block; }
  .mobile-nav { display: flex; }
}
</style>
