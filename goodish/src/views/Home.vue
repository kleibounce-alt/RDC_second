<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { productApi } from '@/api/product'
import { favoriteApi } from '@/api/social'
import ProductCard from '@/components/ProductCard.vue'

const router = useRouter()
const userStore = useUserStore()
const products = ref([])
const tags = ref([
  { id: null, name: '全部' },
])
const activeTagId = ref(null)
const page = ref(1)
const total = ref(0)
const size = 12
const keyword = ref('')
const loading = ref(false)
const searchKeyword = ref('')
const searchPage = ref(1)
const searchTotal = ref(0)
const isSearchMode = ref(false)

onMounted(async () => {
  await loadProducts()
})

async function loadProducts(reset = false) {
  if (reset) page.value = 1
  loading.value = true
  try {
    const params = {
      page: page.value,
      size,
    }
    if (activeTagId.value) params.tagId = activeTagId.value
    const res = await productApi.list(params)
    // Handle both PageResult and plain array
    if (res.data?.list) {
      products.value = reset ? res.data.list : [...products.value, ...res.data.list]
      total.value = res.data.total || 0
    } else if (Array.isArray(res.data)) {
      products.value = reset ? res.data : [...products.value, ...res.data]
    }
  } catch (e) {
    if (!e.message?.includes('登录')) console.error('加载商品失败', e)
  } finally {
    loading.value = false
  }
}

function changeTag(tagId) {
  activeTagId.value = tagId
  loadProducts(true)
}

function goDetail(product) {
  router.push(`/product/${product.id}`)
}

async function toggleFav(product) {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    await favoriteApi.toggle(product.id)
    product.isFavorited = !product.isFavorited
  } catch { /* ignore */ }
}

async function search(reset = false) {
  if (!searchKeyword.value.trim()) {
    isSearchMode.value = false
    loadProducts(true)
    return
  }
  isSearchMode.value = true
  if (reset) searchPage.value = 1
  loading.value = true
  try {
    const res = await productApi.search({ keyword: searchKeyword.value.trim(), page: searchPage.value, size })
    if (res.data?.list) {
      products.value = reset ? res.data.list : [...products.value, ...res.data.list]
      searchTotal.value = res.data.total || 0
    } else if (Array.isArray(res.data)) {
      products.value = reset ? res.data : [...products.value, ...res.data]
    }
  } catch (e) {
    console.error('搜索失败', e)
  } finally {
    loading.value = false
  }
}

function loadMore() {
  if (isSearchMode.value) {
    searchPage.value++
    search()
  } else {
    page.value++
    loadProducts()
  }
}
</script>

<template>
  <div class="container">
    <!-- search bar -->
    <div class="search-bar">
      <div class="search-input-wrap">
        <span class="search-icon">&#128269;</span>
        <input
          v-model="searchKeyword"
          type="text"
          class="search-input"
          placeholder="搜索商品..."
          @keyup.enter="search(true)"
        />
        <button v-if="searchKeyword" class="search-clear" @click="searchKeyword=''; search(true)">&times;</button>
      </div>
    </div>

    <!-- tags -->
    <div class="tags-bar">
      <button
        v-for="tag in tags"
        :key="tag.id"
        :class="['tag-btn', { active: activeTagId === tag.id }]"
        @click="changeTag(tag.id)"
      >
        {{ tag.name }}
      </button>
    </div>

    <!-- product grid -->
    <div class="product-grid" v-if="products.length">
      <ProductCard
        v-for="product in products"
        :key="product.id"
        :product="product"
        @click="goDetail"
        @fav="toggleFav"
      />
    </div>

    <div class="empty-state" v-else-if="!loading">
      <div class="icon">&#128722;</div>
      <p>暂无商品</p>
    </div>

    <!-- load more -->
    <div class="load-more" v-if="(isSearchMode ? searchTotal : total) > products.length">
      <button class="btn btn-outline" @click="loadMore" :disabled="loading">
        {{ loading ? '加载中...' : '加载更多' }}
      </button>
    </div>

    <div class="loading-indicator" v-if="loading && products.length === 0">
      <p class="text-secondary text-sm">加载中...</p>
    </div>
  </div>
</template>

<style scoped>
.search-bar {
  margin-bottom: 20px;
}
.search-input-wrap {
  position: relative;
  max-width: 480px;
}
.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 1rem;
  opacity: 0.4;
}
.search-input {
  width: 100%;
  padding: 12px 40px 12px 42px;
  border: 1.5px solid transparent;
  border-radius: 24px;
  background: var(--bg-white);
  font-size: 0.95rem;
  outline: none;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition);
}
.search-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(255,107,74,0.1);
}
.search-clear {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 1.2rem;
  color: var(--text-muted);
}

.tags-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}
.tag-btn {
  padding: 7px 18px;
  border-radius: 20px;
  font-size: 0.85rem;
  background: var(--bg-white);
  color: var(--text-secondary);
  border: 1px solid var(--border);
  cursor: pointer;
  transition: all var(--transition);
}
.tag-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}
.tag-btn.active {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

.load-more {
  text-align: center;
  margin-top: 32px;
}
.loading-indicator {
  text-align: center;
  padding: 60px 0;
}

@media (max-width: 640px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}
</style>
