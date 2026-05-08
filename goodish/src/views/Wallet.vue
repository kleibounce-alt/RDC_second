<script setup>
import { ref, onMounted } from 'vue'
import { walletApi } from '@/api/order'
import Toast from '@/components/Toast.vue'

const toast = ref(null)
const balance = ref(0)
const records = ref([])
const loading = ref(true)
const rechargeAmount = ref('')
const recharging = ref(false)

const typeMap = {
  RECHARGE: { label: '充值', cls: 'badge-success' },
  PAYMENT: { label: '支付', cls: 'badge-danger' },
  REFUND: { label: '退款', cls: 'badge-primary' },
  VIP: { label: 'VIP', cls: 'badge-warning' },
}

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const [bRes, rRes] = await Promise.all([
      walletApi.balance(),
      walletApi.records(),
    ])
    balance.value = bRes.data?.balance ?? 0
    records.value = rRes.data || []
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function recharge() {
  const amount = Number(rechargeAmount.value)
  if (!amount || amount <= 0) {
    toast.value?.show('请输入正确的金额', 'error')
    return
  }
  recharging.value = true
  try {
    await walletApi.recharge(amount)
    toast.value?.show('充值成功', 'success')
    rechargeAmount.value = ''
    loadData()
  } catch (e) {
    toast.value?.show(e.message || '充值失败', 'error')
  } finally {
    recharging.value = false
  }
}
</script>

<template>
  <div class="container wallet-page">
    <h1 class="page-title">我的钱包</h1>

    <!-- balance card -->
    <div class="card balance-card">
      <p class="text-sm text-secondary">当前余额</p>
      <div class="balance-amount">&yen;{{ Number(balance).toFixed(2) }}</div>
    </div>

    <!-- recharge -->
    <div class="card recharge-card mt-2">
      <h3 class="section-title">充值</h3>
      <form @submit.prevent="recharge" class="recharge-form">
        <input
          v-model="rechargeAmount"
          type="number"
          class="form-input"
          placeholder="输入充值金额"
          step="0.01"
          min="0.01"
          style="flex:1"
        />
        <button type="submit" class="btn btn-primary" :disabled="recharging">
          {{ recharging ? '充值中...' : '充值' }}
        </button>
      </form>
    </div>

    <!-- records -->
    <div class="mt-2">
      <h2 class="section-title">交易记录</h2>
      <div class="record-list" v-if="records.length">
        <div class="card record-item" v-for="r in records" :key="r.id">
          <div class="record-left">
            <span :class="['badge', (typeMap[r.type] || {}).cls || 'badge-primary']">
              {{ (typeMap[r.type] || {}).label || r.type }}
            </span>
            <span class="text-sm text-secondary">{{ r.remark || '' }}</span>
          </div>
          <div class="record-right">
            <span class="record-amount" :class="{ income: r.type === 'RECHARGE' || r.type === 'REFUND' || (r.remark && r.remark.includes('收入')) }">
              {{ r.type === 'RECHARGE' || r.type === 'REFUND' || (r.remark && r.remark.includes('收入')) ? '+' : '-' }}&yen;{{ r.amount }}
            </span>
            <span class="text-xs text-muted">{{ r.createdAt?.substring(0, 10) }}</span>
          </div>
        </div>
      </div>
      <div class="empty-state" v-else-if="!loading">
        <p>暂无交易记录</p>
      </div>
    </div>

    <Toast ref="toast" />
  </div>
</template>

<style scoped>
.wallet-page { max-width: 600px; }

.balance-card {
  padding: 28px;
  text-align: center;
  background: linear-gradient(135deg, rgba(255,107,74,0.06), rgba(20,20,30,0.5));
}
.balance-amount {
  font-size: 2.4rem;
  font-weight: 700;
  color: var(--primary);
  margin-top: 8px;
}

.recharge-card {
  padding: 20px;
}
.section-title {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 14px;
}
.recharge-form {
  display: flex;
  gap: 12px;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.record-item {
  padding: 14px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.record-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.record-right {
  text-align: right;
}
.record-amount {
  font-weight: 600;
  display: block;
}
.record-amount.income {
  color: #2e7d32;
}
</style>
