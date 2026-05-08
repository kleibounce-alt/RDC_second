<script setup>
import { ref } from 'vue'

const visible = ref(false)
const message = ref('')
const type = ref('success')
let timer = null

function show(msg, t = 'success') {
  message.value = msg
  type.value = t
  visible.value = true
  clearTimeout(timer)
  timer = setTimeout(() => { visible.value = false }, 2500)
}

defineExpose({ show })
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" :class="['toast', type]" @click="visible = false">
      <span class="toast-icon">{{ type === 'success' ? '✓' : '✗' }}</span>
      {{ message }}
    </div>
  </Teleport>
</template>

<style scoped>
.toast {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 28px;
  border-radius: 30px;
  font-size: 0.88rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  z-index: 9999;
  animation: toastIn 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.toast.success {
  background: rgba(46, 125, 50, 0.25);
  color: #81c784;
  border-color: rgba(46, 125, 50, 0.3);
  box-shadow: 0 4px 24px rgba(46, 125, 50, 0.2);
}
.toast.error {
  background: rgba(198, 40, 40, 0.25);
  color: #ef5350;
  border-color: rgba(198, 40, 40, 0.3);
  box-shadow: 0 4px 24px rgba(198, 40, 40, 0.2);
}
.toast-icon {
  font-size: 1rem;
  font-weight: 700;
}
@keyframes toastIn {
  from { opacity: 0; transform: translateX(-50%) translateY(-16px); }
  to { opacity: 1; transform: translateX(-50%) translateY(0); }
}
</style>
