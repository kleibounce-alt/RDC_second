<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { productApi } from '@/api/product'
import Toast from '@/components/Toast.vue'

const router = useRouter()
const products = ref([])
const loading = ref(true)
const toast = ref(null)

onMounted(loadProducts)

async function loadProducts() {
  loading.value = true
  try {
    const res = await productApi.myProducts()
    products.value = res.data || []
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function goEdit(product) {
  router.push(`/edit/${product.id}`)
}

async function handleOffShelf(product) {
  if (!confirm(`确定下架「${product.title}」吗？`)) return
  try {
    await productApi.offShelf(product.id)
    toast.value?.show('已下架', 'success')
    product.status = 'REJECTED'
  } catch (e) {
    toast.value?.show(e.message || '操作失败', 'error')
  }
}

async function handleDelete(product) {
  if (!confirm(`确定删除「${product.title}」吗？`)) return
  try {
    await productApi.delete(product.id)
    toast.value?.show('删除成功', 'success')
    products.value = products.value.filter(p => p.id !== product.id)
  } catch (e) {
    toast.value?.show(e.message || '删除失败', 'error')
  }
}

const statusMap = {
  PENDING: { label: '待审核', cls: 'badge-warning' },
  AUDITING: { label: '审核中', cls: 'badge-warning' },
  PUBLISHED: { label: '已发布', cls: 'badge-success' },
  REJECTED: { label: '已下架', cls: 'badge-danger' },
  SOLD: { label: '已售出', cls: 'badge-danger' },
}
</script>

<template>
  <div class="container">
    <div class="flex items-center justify-between mb-3">
      <h1 class="page-title" style="margin-bottom:0">我的发布</h1>
      <router-link to="/publish" class="btn btn-primary btn-sm">发布商品</router-link>
    </div>

    <div class="product-list" v-if="products.length">
      <div :class="['card product-item', { 'is-off': p.status === 'REJECTED' }]" v-for="p in products" :key="p.id">
        <div class="item-info">
          <h3 class="item-title">{{ p.title }}</h3>
          <div class="item-meta">
            <span class="item-price">&yen;{{ p.price }}</span>
            <span :class="['badge', (statusMap[p.status] || {}).cls || 'badge-primary']">
              {{ (statusMap[p.status] || {}).label || p.status }}
            </span>
            <span class="text-xs text-muted">{{ p.createdAt?.substring(0, 10) }}</span>
          </div>
          <p class="text-xs text-muted" v-if="p.rejectReason">驳回原因：{{ p.rejectReason }}</p>
        </div>
        <div class="item-actions">
          <button class="btn btn-ghost btn-sm" @click="goEdit(p)">编辑</button>
          <button v-if="p.status === 'PUBLISHED'" class="btn btn-ghost btn-sm" @click="handleOffShelf(p)">下架</button>
          <button class="btn btn-ghost btn-sm" style="color:#e74c3c" @click="handleDelete(p)">删除</button>
        </div>
      </div>
    </div>

    <div class="empty-state" v-else-if="!loading">
      <div class="icon">&#128722;</div>
      <p>还没有发布过商品</p>
      <router-link to="/publish" class="btn btn-primary mt-2">去发布</router-link>
    </div>

    <Toast ref="toast" />
  </div>
</template>

<style scoped>
.product-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.product-item {
  padding: 18px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.item-info {
  flex: 1;
  min-width: 0;
}
.item-title {
  font-size: 0.95rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
}
.item-price {
  font-weight: 700;
  color: var(--primary);
  font-size: 0.95rem;
}
.product-item.is-off {
  opacity: 0.5;
  position: relative;
}
.product-item.is-off::after {
  content: '已下架';
  position: absolute;
  top: 8px;
  right: 12px;
  font-size: 0.7rem;
  color: #e74c3c;
  font-weight: 600;
  pointer-events: none;
}
.item-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}
</style>
