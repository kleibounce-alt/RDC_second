<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { followApi } from '@/api/social'
import Toast from '@/components/Toast.vue'

const route = useRoute()
const router = useRouter()
const toast = ref(null)
const activeTab = ref(route.query.tab === 'fans' ? 'fans' : 'follows')
const follows = ref([])
const fans = ref([])
const loading = ref(true)

const followsPage = ref(1)
const fansPage = ref(1)
const followsTotal = ref(0)
const fansTotal = ref(0)
const size = 20
const loadingMore = ref(false)

onMounted(() => loadData(true))

async function loadData(reset = false) {
  if (reset) {
    followsPage.value = 1
    fansPage.value = 1
  }
  loading.value = true
  try {
    const [fRes, fanRes] = await Promise.all([
      followApi.follows({ page: followsPage.value, size }),
      followApi.fans({ page: fansPage.value, size }),
    ])
    follows.value = reset ? (fRes.data?.list || []) : [...follows.value, ...(fRes.data?.list || [])]
    fans.value = reset ? (fanRes.data?.list || []) : [...fans.value, ...(fanRes.data?.list || [])]
    followsTotal.value = fRes.data?.total || 0
    fansTotal.value = fanRes.data?.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false; loadingMore.value = false }
}

function loadMore() {
  if (activeTab.value === 'follows') {
    followsPage.value++
  } else {
    fansPage.value++
  }
  loadingMore.value = true
  loadData()
}

async function toggleFollow(user) {
  try {
    await followApi.toggle(user.followUserId || user.userId)
    toast.value?.show('操作成功', 'success')
    loadData(true)
  } catch (e) {
    toast.value?.show(e.message || '操作失败', 'error')
  }
}

function goToUser(followUserId) {
  router.push(`/seller/${followUserId}`)
}
</script>

<template>
  <div class="container">
    <h1 class="page-title">我的关注</h1>

    <div class="tabs mb-2">
      <button :class="['tab-item', { active: activeTab === 'follows' }]" @click="activeTab = 'follows'">
        我的关注 ({{ follows.length }})
      </button>
      <button :class="['tab-item', { active: activeTab === 'fans' }]" @click="activeTab = 'fans'">
        我的粉丝 ({{ fans.length }})
      </button>
    </div>

    <div class="follow-list" v-if="activeTab === 'follows' && follows.length">
      <div class="card follow-item" v-for="f in follows" :key="f.followUserId">
        <div class="follow-user" @click="goToUser(f.followUserId)">
          <div class="follow-avatar">
            <img v-if="f.avatar" :src="f.avatar" alt="" />
            <span v-else>{{ (f.nickname || '?')[0] }}</span>
          </div>
          <div>
            <p class="font-medium text-sm">{{ f.nickname }}</p>
            <p class="text-xs text-muted">{{ f.createdAt?.substring(0, 10) }}</p>
          </div>
        </div>
        <button class="btn btn-outline btn-sm" @click.stop="toggleFollow(f)">取消关注</button>
      </div>
    </div>

    <div class="follow-list" v-else-if="activeTab === 'fans' && fans.length">
      <div class="card follow-item" v-for="f in fans" :key="f.followUserId">
        <div class="follow-user" @click="goToUser(f.followUserId)">
          <div class="follow-avatar">
            <img v-if="f.avatar" :src="f.avatar" alt="" />
            <span v-else>{{ (f.nickname || '?')[0] }}</span>
          </div>
          <div>
            <p class="font-medium text-sm">{{ f.nickname }}</p>
            <p class="text-xs text-muted">{{ f.createdAt?.substring(0, 10) }}</p>
          </div>
        </div>
      </div>
    </div>

    <div class="empty-state" v-else-if="!loading">
      <p>{{ activeTab === 'follows' ? '还没有关注任何人' : '还没有粉丝' }}</p>
    </div>

    <div class="load-more" v-if="(activeTab === 'follows' ? followsTotal : fansTotal) > (activeTab === 'follows' ? follows.length : fans.length)">
      <button class="btn btn-outline" @click="loadMore" :disabled="loadingMore">
        {{ loadingMore ? '加载中...' : '加载更多' }}
      </button>
    </div>

    <Toast ref="toast" />
  </div>
</template>

<style scoped>
.follow-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.load-more { text-align: center; margin-top: 20px; }
.follow-item {
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.follow-user {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}
.follow-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--primary-light);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.9rem;
  overflow: hidden;
  flex-shrink: 0;
}
.follow-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
