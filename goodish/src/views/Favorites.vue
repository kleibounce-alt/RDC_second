<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { favoriteApi } from '@/api/social'
import Toast from '@/components/Toast.vue'

const router = useRouter()
const favorites = ref([])
const loading = ref(true)
const toast = ref(null)
const page = ref(1)
const size = 20
const total = ref(0)
const loadingMore = ref(false)

onMounted(() => loadFavorites(true))

async function loadFavorites(reset = false) {
  if (reset) page.value = 1
  loading.value = true
  try {
    const res = await favoriteApi.list({ page: page.value, size })
    favorites.value = reset ? (res.data?.list || []) : [...favorites.value, ...(res.data?.list || [])]
    total.value = res.data?.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false; loadingMore.value = false }
}

function loadMore() {
  page.value++
  loadingMore.value = true
  loadFavorites()
}

function goDetail(item) {
  router.push(`/product/${item.productId}`)
}

async function removeFav(item) {
  try {
    await favoriteApi.toggle(item.productId)
    favorites.value = favorites.value.filter(f => f.productId !== item.productId)
    toast.value?.show('已取消收藏', 'success')
  } catch { /* ignore */ }
}
</script>

<template>
  <div class="container">
    <h1 class="page-title">我的收藏</h1>

    <div class="fav-grid" v-if="favorites.length">
      <div class="card fav-item" v-for="item in favorites" :key="item.productId" @click="goDetail(item)">
        <div class="fav-img">
          <img v-if="item.productImage" :src="item.productImage" alt="" />
          <div v-else class="fav-img-placeholder">&#9654;</div>
        </div>
        <div class="fav-info">
          <h3 class="fav-title">{{ item.productTitle || '商品' }}</h3>
          <span class="fav-price">&yen;{{ item.productPrice || 0 }}</span>
          <span class="text-xs text-muted">{{ item.createdAt?.substring(0, 10) }}</span>
        </div>
        <button class="fav-remove" @click.stop="removeFav(item)" title="取消收藏">&times;</button>
      </div>
    </div>

    <div class="empty-state" v-else-if="!loading">
      <div class="icon">&#9825;</div>
      <p>还没有收藏任何商品</p>
    </div>

    <div class="load-more" v-if="total > favorites.length">
      <button class="btn btn-outline" @click="loadMore" :disabled="loadingMore">
        {{ loadingMore ? '加载中...' : '加载更多' }}
      </button>
    </div>

    <Toast ref="toast" />
  </div>
</template>

<style scoped>
.fav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}
.load-more { text-align: center; margin-top: 20px; }
.fav-item {
  padding: 12px;
  display: flex;
  gap: 12px;
  cursor: pointer;
  position: relative;
}
.fav-img {
  width: 70px;
  height: 70px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--border-light);
  flex-shrink: 0;
}
.fav-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.fav-img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  opacity: 0.3;
}
.fav-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}
.fav-title {
  font-size: 0.88rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.fav-price {
  font-weight: 700;
  color: var(--primary);
  font-size: 0.88rem;
}
.fav-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  color: var(--text-muted);
  opacity: 0;
  transition: opacity var(--transition);
}
.fav-item:hover .fav-remove { opacity: 1; }
.fav-remove:hover { background: rgba(0,0,0,0.1); }
</style>
