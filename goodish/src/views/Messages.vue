<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { messageApi } from '@/api/message'
import Toast from '@/components/Toast.vue'

const router = useRouter()
const messages = ref([])
const loading = ref(true)
const toast = ref(null)
const page = ref(1)
const size = 20
const total = ref(0)
const loadingMore = ref(false)

const typeMap = {
  PRODUCT_SOLD: '商品售出',
  FOLLOW_NEW_PRODUCT: '关注动态',
  AUDIT_RESULT: '审核结果',
  SYSTEM: '系统通知',
}

onMounted(() => loadMessages(true))

function parseContent(msg) {
  const content = msg.content || ''
  if (content.startsWith('CHAT|')) {
    const parts = content.split('|')
    if (parts.length >= 3) {
      return { senderId: Number(parts[1]), text: parts.slice(2).join('|') }
    }
  }
  return null
}

function handleClick(msg) {
  const parsed = parseContent(msg)
  console.log('[msg-click]', msg.content, parsed)
  if (parsed && parsed.senderId) {
    markRead(msg)
    router.push(`/chat/${parsed.senderId}`)
  } else {
    markRead(msg)
  }
}

async function loadMessages(reset = false) {
  if (reset) page.value = 1
  loading.value = true
  try {
    const res = await messageApi.list({ page: page.value, size })
    messages.value = reset ? (res.data?.list || []) : [...messages.value, ...(res.data?.list || [])]
    total.value = res.data?.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false; loadingMore.value = false }
}

function loadMore() {
  page.value++
  loadingMore.value = true
  loadMessages()
}

async function markRead(msg) {
  if (msg.isRead) return
  try {
    await messageApi.read(msg.id)
    msg.isRead = 1
  } catch { /* ignore */ }
}

async function markAllRead() {
  try {
    await messageApi.readAll()
    messages.value.forEach(m => m.isRead = 1)
    toast.value?.show('全部已读', 'success')
  } catch { /* ignore */ }
}

function displayText(msg) {
  const parsed = parseContent(msg)
  if (parsed) return parsed.text
  return msg.content
}

function isChatMsg(msg) {
  return !!parseContent(msg)
}
</script>

<template>
  <div class="container msg-page">
    <div class="flex items-center justify-between mb-3">
      <h1 class="page-title" style="margin-bottom:0">消息通知</h1>
      <button class="btn btn-ghost btn-sm" @click="markAllRead" v-if="messages.some(m => !m.isRead)">全部已读</button>
    </div>

    <div class="msg-list" v-if="messages.length">
      <div
        class="card msg-item"
        :class="{ unread: !msg.isRead, clickable: isChatMsg(msg) }"
        v-for="msg in messages"
        :key="msg.id"
        @click="handleClick(msg)"
      >
        <div class="msg-dot" v-if="!msg.isRead"></div>
        <div class="msg-content">
          <div class="flex items-center gap-1">
            <span class="badge badge-primary text-xs">{{ typeMap[msg.type] || msg.type }}</span>
            <span class="text-xs text-muted">{{ msg.createdAt?.substring(0, 10) }}</span>
          </div>
          <p class="msg-text text-sm mt-1">{{ displayText(msg) }}</p>
          <span class="text-xs" style="color:var(--primary)" v-if="isChatMsg(msg)">点击查看 →</span>
        </div>
      </div>
    </div>

    <div class="empty-state" v-else-if="!loading">
      <div class="icon">&#9993;</div>
      <p>暂无消息</p>
    </div>

    <div class="load-more" v-if="total > messages.length">
      <button class="btn btn-outline" @click="loadMore" :disabled="loadingMore">
        {{ loadingMore ? '加载中...' : '加载更多' }}
      </button>
    </div>

    <Toast ref="toast" />
  </div>
</template>

<style scoped>
.msg-page { max-width: 640px; }

.msg-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.msg-item {
  padding: 14px 18px;
  display: flex;
  gap: 12px;
  cursor: pointer;
  position: relative;
}
.msg-item.unread {
  background: rgba(255,107,74,0.04);
  border-color: var(--primary-light);
}
.msg-item.clickable:hover {
  border-color: var(--primary);
}
.msg-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary);
  margin-top: 6px;
  flex-shrink: 0;
}
.msg-content { flex: 1; }
.msg-text { color: var(--text-secondary); line-height: 1.5; }
.load-more { text-align: center; margin-top: 20px; }
</style>
