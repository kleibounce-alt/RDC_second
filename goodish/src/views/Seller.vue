<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'
import { productApi } from '@/api/product'
import { followApi } from '@/api/social'
import ProductCard from '@/components/ProductCard.vue'
import Toast from '@/components/Toast.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const seller = ref(null)
const products = ref([])
const loading = ref(true)
const isFollowed = ref(false)
const followLoading = ref(false)
const toast = ref(null)

onMounted(async () => {
  const id = route.params.id
  if (!id) return
  try {
    const promises = [
      userApi.userInfo(id),
      productApi.userProducts(id),
    ]
    if (userStore.isLoggedIn) {
      promises.push(followApi.follows())
    }
    const results = await Promise.all(promises)
    seller.value = results[0].data
    products.value = results[1].data || []
    if (userStore.isLoggedIn && results[2]) {
      const follows = results[2].data || []
      isFollowed.value = follows.some(f => f.followUserId === Number(id))
    }
  } catch (e) {
    toast.value?.show('加载卖家信息失败', 'error')
  } finally {
    loading.value = false
  }
})

async function toggleFollow() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  followLoading.value = true
  try {
    await followApi.toggle(seller.value.id)
    isFollowed.value = !isFollowed.value
  } catch { /* ignore */ }
  finally { followLoading.value = false }
}

function goChat() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  router.push(`/chat/${seller.value.id}`)
}

function goProduct(id) {
  router.push(`/product/${id}`)
}
</script>

<template>
  <div class="container seller-page" v-if="!loading && seller">
    <div class="seller-profile card">
      <div class="seller-hero">
        <div class="seller-avatar-lg">
          <img v-if="seller.avatar" :src="seller.avatar" alt="" />
          <span v-else class="avatar-placeholder-lg">{{ (seller.nickname || seller.username || '?')[0] }}</span>
        </div>
        <div class="seller-meta">
          <h1 class="seller-nickname">
            {{ seller.nickname || seller.username }}
            <span class="badge badge-vip" v-if="seller.vipLevel">VIP{{ seller.vipLevel }}</span>
          </h1>
          <p class="text-secondary text-sm">@{{ seller.username }}</p>
          <p class="text-xs text-muted" v-if="seller.createdAt">{{ seller.createdAt.substring(0, 10) }} 加入 · {{ products.length }} 件商品</p>
        </div>
        <div class="seller-actions">
          <button class="btn btn-outline" @click="toggleFollow" :disabled="followLoading" v-if="userStore.isLoggedIn && userStore.user?.id !== seller.id">
            {{ isFollowed ? '已关注' : '+ 关注' }}
          </button>
          <button class="btn btn-primary" @click="goChat" v-if="userStore.user?.id !== seller.id">
            联系卖家
          </button>
        </div>
      </div>
    </div>

    <div class="seller-products mt-3">
      <h2 class="section-title">TA的商品 ({{ products.length }})</h2>
      <div class="product-grid" v-if="products.length">
        <ProductCard
          v-for="p in products"
          :key="p.id"
          :product="p"
          @click="goProduct(p.id)"
        />
      </div>
      <div class="empty-state" v-else>
        <p>暂无在售商品</p>
      </div>
    </div>

    <Toast ref="toast" />
  </div>

  <div class="container seller-page" v-else-if="loading">
    <div class="empty-state"><p>加载中...</p></div>
  </div>

  <div class="container seller-page" v-else>
    <div class="empty-state"><p>用户不存在</p></div>
  </div>
</template>

<style scoped>
.seller-page {
  max-width: 900px;
}
.seller-profile {
  padding: 24px;
}
.seller-hero {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}
.seller-avatar-lg {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--border-light);
  flex-shrink: 0;
}
.seller-avatar-lg img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-placeholder-lg {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 2rem;
  color: var(--primary);
  background: rgba(255, 107, 74, 0.12);
}
.seller-meta {
  flex: 1;
  min-width: 0;
}
.seller-nickname {
  font-size: 1.4rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.badge-vip {
  background: linear-gradient(135deg, #f7971e, #ffd200);
  color: #000;
  font-size: 0.7rem;
  padding: 2px 6px;
  border-radius: 4px;
}
.seller-actions {
  flex-shrink: 0;
}
.section-title {
  font-size: 1.15rem;
  font-weight: 600;
  margin-bottom: 16px;
}
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}
@media (max-width: 768px) {
  .seller-hero {
    gap: 12px;
  }
  .seller-avatar-lg {
    width: 60px;
    height: 60px;
  }
}
</style>
