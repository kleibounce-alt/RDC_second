<script setup>
import SideBar from '@/components/SideBar.vue'
import BanModal from '@/components/BanModal.vue'
import { ref } from 'vue'

const collapsed = ref(false)
</script>

<template>
  <div class="app-layout">
    <SideBar :collapsed="collapsed" @toggle="collapsed = !collapsed" />
    <main :class="['main-area', { expanded: collapsed }]">
      <router-view />
    </main>
    <BanModal />
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
}

.main-area {
  flex: 1;
  margin-left: var(--sidebar-width);
  padding: 32px;
  position: relative;
  z-index: 1;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.main-area.expanded {
  margin-left: 72px;
}

@media (max-width: 768px) {
  .main-area {
    margin-left: 0;
    padding: 20px 16px;
    padding-bottom: 80px;
  }
  .main-area.expanded {
    margin-left: 0;
  }
}
</style>
