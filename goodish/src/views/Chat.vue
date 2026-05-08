<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { messageApi } from '@/api/message'
import { userApi } from '@/api/user'

const route = useRoute()
const userStore = useUserStore()

const otherId = computed(() => Number(route.params.otherId))
const messages = ref([])
const inputText = ref('')
const ws = ref(null)
const chatBox = ref(null)
const loadingHistory = ref(true)
const connected = ref(false)
const connError = ref('')
const otherUser = ref(null)
const otherOnline = ref(false)

onMounted(async () => {
  await loadOtherUser()
  await loadHistory()
  connectWs()
})

onUnmounted(() => {
  if (ws.value) {
    ws.value.close()
  }
})

async function loadOtherUser() {
  try {
    const res = await userApi.userInfo(otherId.value)
    otherUser.value = res.data
  } catch { /* ignore */ }
}

async function loadHistory() {
  loadingHistory.value = true
  try {
    const res = await messageApi.chatHistory(otherId.value)
    messages.value = res.data || []
    scrollBottom()
  } catch { /* ignore */ }
  finally { loadingHistory.value = false }
}

function connectWs() {
  const token = localStorage.getItem('accessToken')
  if (!token) {
    connError.value = '未登录'
    return
  }

  connError.value = ''
  const wsUrl = `ws://127.0.0.1:8080/message/chat?token=${token}`
  ws.value = new WebSocket(wsUrl)

  ws.value.onopen = () => {
    connected.value = true
    connError.value = ''
  }

  ws.value.onmessage = (e) => {
    try {
      const data = JSON.parse(e.data)
      if (data.type === 'status') {
        if (data.userId === otherId.value) {
          otherOnline.value = data.online
        }
      } else {
        messages.value.push(data)
        scrollBottom()
      }
    } catch { /* ignore */ }
  }

  ws.value.onclose = () => {
    connected.value = false
    if (!connError.value) {
      connError.value = '连接断开，3秒后重连...'
    }
    setTimeout(() => {
      if (!connected.value) connectWs()
    }, 3000)
  }

  ws.value.onerror = () => {
    connected.value = false
    connError.value = '连接失败'
  }
}

function sendMessage() {
  const text = inputText.value.trim()
  if (!text || !ws.value || ws.value.readyState !== WebSocket.OPEN) return

  ws.value.send(JSON.stringify({
    receiverId: otherId.value,
    productId: null,
    content: text,
  }))
  inputText.value = ''
  nextTick(() => scrollBottom())
}

function scrollBottom() {
  nextTick(() => {
    if (chatBox.value) {
      chatBox.value.scrollTop = chatBox.value.scrollHeight
    }
  })
}

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return d.toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function avatarUrl(url) {
  if (!url) return ''
  return url
}
</script>

<template>
  <div class="container chat-page">
    <div class="chat-header">
      <div class="header-user" v-if="otherUser">
        <div class="avatar-sm">
          <img v-if="otherUser.avatar" :src="avatarUrl(otherUser.avatar)" alt="" />
          <span v-else class="avatar-txt">{{ (otherUser.nickname || otherUser.username || '?')[0] }}</span>
        </div>
        <div class="header-user-info">
          <span class="font-medium">{{ otherUser.nickname || otherUser.username }}</span>
          <span class="online-dot" :class="{ on: otherOnline }">{{ otherOnline ? '在线' : '离线' }}</span>
        </div>
      </div>
      <span class="chat-status" :class="{ online: connected }"></span>
      <span v-if="connError" class="conn-error text-xs">{{ connError }}</span>
    </div>

    <div class="chat-box" ref="chatBox">
      <div v-if="loadingHistory" class="empty-state"><p>加载中...</p></div>
      <div v-else-if="!messages.length" class="empty-state">
        <p>暂无消息，发送第一条吧</p>
      </div>

      <div
        v-for="(msg, i) in messages"
        :key="i"
        :class="['chat-msg', { mine: msg.senderId === userStore.user?.id }]"
      >
        <div class="msg-sender" v-if="msg.senderId !== userStore.user?.id && msg.senderNickname">
          <span class="avatar-xs">
            <img v-if="msg.senderAvatar" :src="avatarUrl(msg.senderAvatar)" alt="" />
            <span v-else class="avatar-txt-xs">{{ (msg.senderNickname || '?')[0] }}</span>
          </span>
          <span class="text-xs text-muted">{{ msg.senderNickname }}</span>
        </div>
        <div class="chat-bubble">{{ msg.content }}</div>
        <div class="chat-time text-xs text-muted">{{ formatTime(msg.createdAt) }}</div>
      </div>
    </div>

    <div class="chat-input-wrap">
      <input
        v-model="inputText"
        class="form-input chat-input"
        :placeholder="connected ? '输入消息...' : '连接中...'"
        @keyup.enter="sendMessage"
        :disabled="!connected"
      />
      <button class="btn btn-primary btn-sm" @click="sendMessage" :disabled="!connected || !inputText.trim()">
        发送
      </button>
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  max-width: 640px;
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--nav-height) - 80px);
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: var(--bg-white);
  border-radius: var(--radius) var(--radius) 0 0;
  border: 1px solid var(--border-light);
  border-bottom: none;
}
.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}
.header-user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.online-dot {
  font-size: 0.7rem;
  color: #999;
}
.online-dot.on {
  color: #4caf50;
}
.avatar-sm {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--border-light);
  flex-shrink: 0;
}
.avatar-sm img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-txt {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: var(--primary);
  background: rgba(255,107,74,0.12);
}
.chat-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ccc;
  flex-shrink: 0;
}
.chat-status.online {
  background: #4caf50;
}
.conn-error {
  color: #e74c3c;
}

.chat-box {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: var(--bg-white);
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chat-msg {
  display: flex;
  flex-direction: column;
  max-width: 75%;
}
.chat-msg.mine {
  align-self: flex-end;
  align-items: flex-end;
}
.msg-sender {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 2px;
}
.avatar-xs {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--border-light);
  flex-shrink: 0;
}
.avatar-xs img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-txt-xs {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.55rem;
  font-weight: 700;
  color: var(--primary);
  background: rgba(255,107,74,0.12);
}
.chat-bubble {
  padding: 10px 16px;
  border-radius: 18px;
  font-size: 0.9rem;
  line-height: 1.5;
  word-break: break-word;
}
.chat-msg:not(.mine) .chat-bubble {
  background: var(--border-light);
  border-bottom-left-radius: 6px;
}
.chat-msg.mine .chat-bubble {
  background: var(--primary);
  color: #fff;
  border-bottom-right-radius: 6px;
}
.chat-time {
  margin-top: 4px;
}

.chat-input-wrap {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  background: var(--bg-white);
  border: 1px solid var(--border-light);
  border-top: none;
  border-radius: 0 0 var(--radius) var(--radius);
}
.chat-input {
  flex: 1;
}
</style>
