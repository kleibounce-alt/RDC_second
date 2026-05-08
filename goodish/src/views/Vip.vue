<script setup>
import { ref, onMounted } from 'vue'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'
import Toast from '@/components/Toast.vue'

const userStore = useUserStore()
const toast = ref(null)
const loading = ref(false)

const plans = [
  { level: 1, name: '月度会员', months: 1, price: 15, desc: '商品额外获得 10 次曝光推流', color: '#4caf50', bg: 'rgba(76,175,80,0.06)' },
  { level: 2, name: '季度会员', months: 3, price: 36, desc: '商品额外获得 40 次曝光推流 · 商品优先展示', color: '#2196f3', bg: 'rgba(33,150,243,0.06)' },
  { level: 3, name: '年度会员', months: 12, price: 108, desc: '商品额外获得 200 次曝光推流 · 首页推荐位 · 专属皇冠标识', color: '#f5a623', bg: 'rgba(245,166,35,0.08)' },
]

async function buyVip(plan) {
  if (!confirm(`确定购买${plan.name}？价格 ¥${plan.price}`)) return
  loading.value = true
  try {
    const orderRes = await userApi.vipOrder({
      vipLevel: plan.level,
      durationMonths: plan.months,
      price: plan.price,
    })
    const orderId = orderRes.data
    await userApi.vipPay({ orderId })
    await userStore.fetchProfile()
    toast.value?.show('开通成功！', 'success')
  } catch (e) {
    toast.value?.show(e.message || '开通失败，请确保余额充足', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="container vip-page">
    <h1 class="page-title">VIP 会员</h1>

    <div class="vip-banner card" v-if="userStore.user?.vipLevel > 0">
      <p class="vip-banner-title">当前等级：<strong>VIP{{ userStore.user.vipLevel }}</strong></p>
      <p class="text-sm" v-if="userStore.user.vipExpireTime">
        到期时间：{{ userStore.user.vipExpireTime?.substring(0, 10) }}
      </p>
    </div>

    <div class="plan-grid">
      <div
        class="card plan-card"
        v-for="plan in plans"
        :key="plan.level"
        :style="{ borderTop: `3px solid ${plan.color}` }"
      >
        <div class="plan-header" :style="{ background: plan.bg, color: plan.color }">
          <span class="plan-badge">VIP{{ plan.level }}</span>
        </div>
        <div class="plan-body">
          <h3>{{ plan.name }}</h3>
          <div class="plan-price">&yen;{{ plan.price }}</div>
          <p class="text-sm text-secondary">{{ plan.desc }}</p>
          <button
            class="btn"
            :class="userStore.user?.vipLevel === plan.level ? 'btn-outline' : 'btn-primary'"
            style="width:100%;margin-top:16px"
            @click="buyVip(plan)"
            :disabled="loading || userStore.user?.vipLevel === plan.level"
          >
            {{ userStore.user?.vipLevel === plan.level ? '当前等级' : '立即开通' }}
          </button>
        </div>
      </div>
    </div>

    <Toast ref="toast" />
  </div>
</template>

<style scoped>
.vip-page { max-width: 760px; }

.vip-banner {
  padding: 20px 24px;
  background: linear-gradient(135deg, rgba(255,107,74,0.06), rgba(20,20,30,0.5));
  margin-bottom: 24px;
}
.vip-banner-title {
  font-size: 1.05rem;
}

.plan-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}
.plan-card {
  overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.plan-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}
.plan-header {
  padding: 14px;
  text-align: center;
  font-weight: 700;
  font-size: 1rem;
}
.plan-body {
  padding: 20px;
  text-align: center;
}
.plan-body h3 {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 8px;
}
.plan-price {
  font-size: 1.8rem;
  font-weight: 700;
  color: var(--text);
  margin: 8px 0;
}

@media (max-width: 640px) {
  .plan-grid { grid-template-columns: 1fr; }
}
</style>
