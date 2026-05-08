<script setup>
defineProps({
  visible: Boolean,
  title: String,
  price: Number,
  success: Boolean,
})

const emit = defineEmits(['confirm', 'cancel'])
</script>

<template>
  <Teleport to="body">
    <!-- confirm -->
    <div v-if="visible && !success" class="modal-overlay" @click.self="emit('cancel')">
      <div class="modal-card">
        <div class="modal-icon">&#128722;</div>
        <h3 class="modal-title">确认购买</h3>
        <p class="modal-product">{{ title }}</p>
        <div class="modal-price">&yen;{{ Number(price).toFixed(2) }}</div>
        <div class="modal-actions">
          <button class="btn btn-outline" @click="emit('cancel')">取消</button>
          <button class="btn btn-primary" @click="emit('confirm')">确认购买</button>
        </div>
      </div>
    </div>

    <!-- success -->
    <div v-if="visible && success" class="modal-overlay" @click.self="emit('confirm')">
      <div class="modal-card">
        <div class="modal-icon success-icon">&#10003;</div>
        <h3 class="modal-title">购买成功</h3>
        <p class="modal-product">{{ title }}</p>
        <div class="modal-price">&yen;{{ Number(price).toFixed(2) }}</div>
        <div class="modal-actions">
          <button class="btn btn-primary" style="flex:1" @click="emit('confirm')">确定</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  animation: fadeIn 0.2s ease;
}
.modal-card {
  background: rgba(24, 24, 36, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 36px 32px 28px;
  text-align: center;
  max-width: 360px;
  width: 90%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  animation: slideUp 0.25s ease;
}
.modal-icon {
  font-size: 2.4rem;
  margin-bottom: 12px;
}
.success-icon {
  color: #2ecc71;
  font-size: 2.8rem;
  font-weight: 700;
}
.modal-title {
  font-size: 1.15rem;
  font-weight: 700;
  margin-bottom: 8px;
}
.modal-product {
  font-size: 0.92rem;
  color: var(--text-secondary);
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.modal-price {
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--primary);
  margin-bottom: 24px;
}
.modal-actions {
  display: flex;
  gap: 12px;
}
.modal-actions .btn {
  flex: 1;
  padding: 10px 0;
  font-size: 0.9rem;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
