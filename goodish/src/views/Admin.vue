<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { adminApi } from '@/api/admin'
import Toast from '@/components/Toast.vue'

const router = useRouter()
const userStore = useUserStore()

onMounted(() => {
  if (!userStore.isAdmin) {
    router.replace('/')
    return
  }
  loadPending()
  loadOffShelved()
  loadSensitive()
})

// Audit
const pendingProducts = ref([])
const rejectReason = ref('')

async function loadPending() {
  try {
    const res = await adminApi.auditPending()
    pendingProducts.value = res.data || []
  } catch (e) {
    toast.value?.show('加载审核列表失败: ' + (e.message || '未知错误'), 'error')
  }
}

async function approve(product) {
  try {
    await adminApi.auditApprove(product.id || product.product?.id)
    toast.value?.show('已通过', 'success')
    pendingProducts.value = pendingProducts.value.filter(p => p !== product)
  } catch (e) {
    if (e.message?.includes('已被其他管理员处理')) {
      toast.value?.show('已审核', 'success')
      pendingProducts.value = pendingProducts.value.filter(p => p !== product)
    } else {
      toast.value?.show(e.message || '操作失败', 'error')
    }
  }
}

async function rejectProd(item) {
  const pid = item.product?.id || item.id
  const reason = rejectReason.value || '不符合要求'
  try {
    await adminApi.auditReject({ productId: pid, reason })
    toast.value?.show('已驳回', 'success')
    rejectReason.value = ''
    pendingProducts.value = pendingProducts.value.filter(p => p !== item)
  } catch (e) {
    if (e.message?.includes('已被其他管理员处理')) {
      toast.value?.show('已审核', 'success')
      pendingProducts.value = pendingProducts.value.filter(p => p !== item)
    } else {
      toast.value?.show(e.message || '操作失败', 'error')
    }
  }
}

// User management
const userInput = ref('')
const banEndTime = ref('')

async function banUser() {
  const val = userInput.value.trim()
  if (!val) return toast.value?.show('请输入用户名', 'error')
  try {
    await adminApi.ban({ username: val, banEndTime: banEndTime.value || null })
    toast.value?.show('封禁成功', 'success')
  } catch (e) {
    toast.value?.show(e.message || '操作失败', 'error')
  }
}

async function unbanUser() {
  const val = userInput.value.trim()
  if (!val) return toast.value?.show('请输入用户名', 'error')
  try {
    await adminApi.unban({ username: val })
    toast.value?.show('解封成功', 'success')
  } catch (e) {
    toast.value?.show(e.message || '操作失败', 'error')
  }
}

// Product management
const productIdInput = ref('')

async function deleteProduct() {
  if (!productIdInput.value) return toast.value?.show('请输入商品ID', 'error')
  if (!confirm('确定删除该商品吗？')) return
  try {
    await adminApi.deleteProduct(Number(productIdInput.value))
    toast.value?.show('已删除', 'success')
  } catch (e) {
    toast.value?.show(e.message || '操作失败', 'error')
  }
}

// Exposure adjustment
const exposureProductId = ref('')
const exposureLevel = ref(1)
const exposureLevels = [
  { v: 1, label: '1档 · 默认', desc: '权重0' },
  { v: 2, label: '2档 · 轻度', desc: '权重10' },
  { v: 3, label: '3档 · 中度', desc: '权重30' },
  { v: 4, label: '4档 · 重度', desc: '权重60' },
  { v: 5, label: '5档 · 置顶', desc: '权重100' },
]

async function setExposure() {
  if (!exposureProductId.value) return toast.value?.show('请输入商品ID', 'error')
  try {
    await adminApi.setExposure(Number(exposureProductId.value), exposureLevel.value)
    toast.value?.show('曝光度已调整', 'success')
  } catch (e) {
    toast.value?.show(e.message || '操作失败', 'error')
  }
}

// Sensitive words
const sensitiveWord = ref('')
const sensitiveWords = ref([])

async function loadSensitive() {
  try {
    const res = await adminApi.listSensitive()
    sensitiveWords.value = res.data || []
  } catch { /* ignore */ }
}

async function addSensitive() {
  if (!sensitiveWord.value.trim()) return
  try {
    await adminApi.addSensitive(sensitiveWord.value.trim())
    toast.value?.show('已添加', 'success')
    sensitiveWord.value = ''
    loadSensitive()
  } catch (e) {
    toast.value?.show(e.message || '失败', 'error')
  }
}

async function deleteSensitive(id) {
  try {
    await adminApi.deleteSensitive(id)
    sensitiveWords.value = sensitiveWords.value.filter(w => w.id !== id)
  } catch (e) {
    toast.value?.show(e.message || '失败', 'error')
  }
}

// Off-shelved products
const offShelvedProducts = ref([])

async function loadOffShelved() {
  try {
    const res = await adminApi.offShelved()
    offShelvedProducts.value = res.data || []
  } catch { /* ignore */ }
}

async function relistProduct(productId) {
  try {
    await adminApi.relist(productId)
    toast.value?.show('已重新上架', 'success')
    offShelvedProducts.value = offShelvedProducts.value.filter(p => p.id !== productId)
  } catch (e) {
    toast.value?.show(e.message || '操作失败', 'error')
  }
}

const toast = ref(null)
</script>

<template>
  <div class="container admin-page">
    <h1 class="page-title">后台管理</h1>

    <div class="admin-grid">
      <!-- audit -->
      <div class="card section-card">
        <h2 class="section-title">商品审核</h2>
        <div v-if="pendingProducts.length">
          <div class="audit-item" v-for="item in pendingProducts" :key="item.product?.id || item.id">
            <div class="audit-info">
              <p class="font-medium text-sm">
                #{{ item.product?.id || item.id }} {{ item.product?.title || item.title }}
                <router-link :to="'/product/' + (item.product?.id || item.id)" target="_blank" class="link">查看</router-link>
              </p>
              <p class="text-xs text-muted">
                ¥{{ item.product?.price || item.price }}
                · 卖家: {{ item.seller?.nickname || item.seller?.username || '未知' }}
                (ID:{{ item.product?.userId || item.userId }})
              </p>
            </div>
            <div class="audit-actions">
              <input v-model="rejectReason" class="form-input" placeholder="驳回理由" style="width:120px;padding:4px 8px;font-size:0.8rem" />
              <button class="btn btn-primary btn-sm" @click="approve(item)">通过</button>
              <button class="btn btn-danger btn-sm" @click="rejectProd(item)">驳回</button>
            </div>
          </div>
        </div>
        <p class="text-sm text-muted" v-else>暂无待审核商品</p>
      </div>

      <!-- user -->
      <div class="card section-card">
        <h2 class="section-title">用户管理</h2>
        <div class="form-inline mb-2">
          <input v-model="userInput" class="form-input" placeholder="用户名" style="flex:1" />
        </div>
        <div class="form-inline mb-2">
          <input v-model="banEndTime" class="form-input" placeholder="封禁截止时间 (可选)" style="flex:1" type="datetime-local" />
        </div>
        <div class="btn-row">
          <button class="btn btn-danger btn-sm" @click="banUser">封禁用户</button>
          <button class="btn btn-outline btn-sm" @click="unbanUser">解封用户</button>
        </div>
      </div>

      <!-- product -->
      <div class="card section-card">
        <h2 class="section-title">商品管理</h2>
        <div class="form-inline mb-2">
          <input v-model="productIdInput" class="form-input" placeholder="商品ID" style="flex:1" type="number" />
        </div>
        <div class="btn-row">
          <button class="btn btn-danger btn-sm" @click="deleteProduct">删除商品</button>
        </div>
      </div>

      <!-- exposure -->
      <div class="card section-card">
        <h2 class="section-title">曝光度调整</h2>
        <div class="form-inline mb-2">
          <input v-model="exposureProductId" class="form-input" placeholder="商品ID" style="flex:1" type="number" />
        </div>
        <div class="exposure-options mb-2">
          <label v-for="l in exposureLevels" :key="l.v" class="exposure-option" :class="{ selected: exposureLevel === l.v }">
            <input type="radio" v-model="exposureLevel" :value="l.v" />
            <span class="exposure-label">{{ l.label }}</span>
            <span class="exposure-desc text-xs text-muted">{{ l.desc }}</span>
          </label>
        </div>
        <button class="btn btn-primary btn-sm" @click="setExposure">确认调整</button>
      </div>

      <!-- off-shelved -->
      <div class="card section-card">
        <h2 class="section-title">下架商品 ({{ offShelvedProducts.length }})</h2>
        <div v-if="offShelvedProducts.length">
          <div class="audit-item" v-for="p in offShelvedProducts" :key="p.id">
            <div class="audit-info">
              <p class="font-medium text-sm">
                #{{ p.id }} {{ p.title }}
                <router-link :to="'/product/' + p.id" target="_blank" class="link">查看</router-link>
              </p>
              <p class="text-xs text-muted">
                ¥{{ p.price }} · 卖家ID:{{ p.userId }}
                <span v-if="p.rejectReason"> · 原因: {{ p.rejectReason }}</span>
              </p>
            </div>
            <div class="audit-actions">
              <button class="btn btn-primary btn-sm" @click="relistProduct(p.id)">重新上架</button>
            </div>
          </div>
        </div>
        <p class="text-sm text-muted" v-else>暂无下架商品</p>
      </div>

      <!-- sensitive -->
      <div class="card section-card">
        <h2 class="section-title">敏感词管理 ({{ sensitiveWords.length }})</h2>
        <div class="form-inline mb-2">
          <input v-model="sensitiveWord" class="form-input" placeholder="输入敏感词" style="flex:1" @keyup.enter="addSensitive" />
          <button class="btn btn-primary btn-sm" @click="addSensitive">添加</button>
        </div>
        <div class="sensitive-list" v-if="sensitiveWords.length">
          <div class="sensitive-item" v-for="w in sensitiveWords" :key="w.id">
            <span class="text-sm">{{ w.word }}</span>
            <button class="btn btn-ghost btn-sm" style="color:#e74c3c" @click="deleteSensitive(w.id)">删除</button>
          </div>
        </div>
        <p class="text-sm text-muted" v-else>暂无敏感词</p>
      </div>
    </div>

    <Toast ref="toast" />
  </div>
</template>

<style scoped>
.admin-page { max-width: 900px; }
.admin-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.section-card { padding: 22px; }
.section-title { font-size: 1.05rem; font-weight: 600; margin-bottom: 16px; }
.audit-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-light);
}
.audit-item:last-child { border-bottom: none; }
.audit-info { flex: 1; }
.audit-actions { display: flex; gap: 6px; align-items: center; flex-shrink: 0; }
.link { color: var(--primary); font-size: 0.8rem; margin-left: 6px; }
.link:hover { text-decoration: underline; }
.form-inline { display: flex; gap: 8px; }
.btn-row { display: flex; gap: 8px; flex-wrap: wrap; }
.exposure-options { display: flex; flex-direction: column; gap: 6px; }
.exposure-option { display: flex; align-items: center; gap: 8px; padding: 6px 10px; border-radius: 6px; border: 1px solid var(--border); cursor: pointer; transition: all var(--transition); }
.exposure-option.selected { border-color: var(--primary); background: rgba(255,107,74,0.06); }
.exposure-option input[type="radio"] { accent-color: var(--primary); }
.exposure-label { font-size: 0.85rem; font-weight: 500; }
.exposure-desc { margin-left: auto; }
.sensitive-list { display: flex; flex-direction: column; gap: 4px; max-height: 200px; overflow-y: auto; }
.sensitive-item { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; border-bottom: 1px solid var(--border-light); }
.sensitive-item:last-child { border-bottom: none; }
@media (max-width: 768px) { .admin-grid { grid-template-columns: 1fr; } }
</style>
