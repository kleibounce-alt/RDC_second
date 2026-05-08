<script setup>
import { ref, onMounted } from 'vue'
import { orderApi } from '@/api/order'
import Toast from '@/components/Toast.vue'

const toast = ref(null)
const activeTab = ref('buy')
const buyOrders = ref([])
const sellOrders = ref([])
const loading = ref(true)

const statusMap = {
  PENDING: { label: '待支付', cls: 'badge-warning' },
  PAID: { label: '已支付', cls: 'badge-primary' },
  CANCELLED: { label: '已取消', cls: 'badge-danger' },
  COMPLETED: { label: '已完成', cls: 'badge-success' },
  REFUNDING: { label: '退款中', cls: 'badge-warning' },
  REFUND_REJECTED: { label: '退款被拒', cls: 'badge-danger' },
}

onMounted(loadOrders)

async function loadOrders() {
  loading.value = true
  try {
    const [buy, sell] = await Promise.all([
      orderApi.myOrders(),
      orderApi.mySells(),
    ])
    buyOrders.value = buy.data || []
    sellOrders.value = sell.data || []
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function cancelOrder(order) {
  if (!confirm('确定取消该订单吗？')) return
  try {
    await orderApi.cancel(order.id)
    toast.value?.show('已取消', 'success')
    loadOrders()
  } catch (e) {
    toast.value?.show(e.message || '取消失败', 'error')
  }
}

async function requestRefund(order) {
  if (!confirm('确定申请退款吗？')) return
  try {
    await orderApi.requestRefund(order.id)
    toast.value?.show('退款申请已提交', 'success')
    loadOrders()
  } catch (e) {
    toast.value?.show(e.message || '操作失败', 'error')
  }
}

async function handleRefund(order, approve) {
  const msg = approve ? '确定同意退款吗？' : '确定拒绝退款吗？'
  if (!confirm(msg)) return
  try {
    await orderApi.handleRefund(order.id, approve)
    toast.value?.show(approve ? '已退款' : '已拒绝', 'success')
    loadOrders()
  } catch (e) {
    toast.value?.show(e.message || '操作失败', 'error')
  }
}

async function completeOrder(order) {
  if (!confirm('确认收货吗？')) return
  try {
    await orderApi.complete(order.id)
    toast.value?.show('已确认收货', 'success')
    loadOrders()
  } catch (e) {
    toast.value?.show(e.message || '操作失败', 'error')
  }
}
</script>

<template>
  <div class="container">
    <h1 class="page-title">我的订单</h1>

    <div class="tabs mb-2">
      <button :class="['tab-item', { active: activeTab === 'buy' }]" @click="activeTab = 'buy'">
        我买的 ({{ buyOrders.length }})
      </button>
      <button :class="['tab-item', { active: activeTab === 'sell' }]" @click="activeTab = 'sell'">
        我卖的 ({{ sellOrders.length }})
      </button>
    </div>

    <!-- buy orders -->
    <div class="order-list" v-if="activeTab === 'buy' && buyOrders.length">
      <div class="card order-item" v-for="o in buyOrders" :key="o.id">
        <div class="order-header">
          <span class="text-sm text-secondary">订单号：{{ o.orderNo }}</span>
          <span :class="['badge', (statusMap[o.status] || {}).cls || 'badge-primary']">
            {{ (statusMap[o.status] || {}).label || o.status }}
          </span>
        </div>
        <div class="order-body">
          <span class="text-secondary">商品 #{{ o.productId }}</span>
          <span class="order-price">&yen;{{ o.price }}</span>
        </div>
        <div class="order-footer" v-if="o.status === 'PAID'">
          <button class="btn btn-ghost btn-sm" style="color:#e74c3c" @click="requestRefund(o)">申请退款</button>
          <button class="btn btn-primary btn-sm" @click="completeOrder(o)">确认收货</button>
        </div>
        <div class="order-footer" v-if="o.status === 'PENDING'">
          <button class="btn btn-ghost btn-sm" style="color:#e74c3c" @click="cancelOrder(o)">取消订单</button>
        </div>
        <span class="text-xs text-muted">{{ o.createdAt?.substring(0, 10) }}</span>
      </div>
    </div>

    <!-- sell orders -->
    <div class="order-list" v-else-if="activeTab === 'sell' && sellOrders.length">
      <div class="card order-item" v-for="o in sellOrders" :key="o.id">
        <div class="order-header">
          <span class="text-sm text-secondary">订单号：{{ o.orderNo }}</span>
          <span :class="['badge', (statusMap[o.status] || {}).cls || 'badge-primary']">
            {{ (statusMap[o.status] || {}).label || o.status }}
          </span>
        </div>
        <div class="order-body">
          <span class="text-secondary">商品 #{{ o.productId }}</span>
          <span class="order-price">&yen;{{ o.price }}</span>
        </div>
        <div class="order-footer" v-if="o.status === 'REFUNDING'">
          <button class="btn btn-primary btn-sm" @click="handleRefund(o, true)">同意退款</button>
          <button class="btn btn-ghost btn-sm" style="color:#e74c3c" @click="handleRefund(o, false)">拒绝退款</button>
        </div>
        <span class="text-xs text-muted">{{ o.createdAt?.substring(0, 10) }}</span>
      </div>
    </div>

    <div class="empty-state" v-else-if="!loading">
      <p>{{ activeTab === 'buy' ? '还没有购买记录' : '还没有卖出记录' }}</p>
    </div>

    <Toast ref="toast" />
  </div>
</template>

<style scoped>
.order-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.order-item {
  padding: 16px 20px;
}
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.order-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.order-price {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--primary);
}
.order-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
</style>
