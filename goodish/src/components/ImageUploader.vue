<script setup>
import { ref } from 'vue'
import { productApi } from '@/api/product'

const emit = defineEmits(['update:images'])
const props = defineProps({ images: { type: Array, default: () => [] } })

const uploading = ref(false)
const fileInput = ref(null)

function triggerUpload() { fileInput.value?.click() }

async function handleUpload(e) {
  const files = e.target.files
  if (!files?.length) return
  const formData = new FormData()
  for (const file of files) formData.append('files', file)
  uploading.value = true
  try {
    const res = await productApi.upload(formData)
    const urls = res.data || []
    const newImages = [...props.images, ...urls.map((url, i) => ({ imageUrl: url, sort: props.images.length + i }))]
    emit('update:images', newImages)
  } catch (e) {
    alert('上传失败: ' + e.message)
  } finally {
    uploading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

function removeImage(index) {
  const newImages = props.images.filter((_, i) => i !== index)
  emit('update:images', newImages)
}
</script>

<template>
  <div class="uploader">
    <div class="image-grid" v-if="images.length">
      <div class="image-item" v-for="(img, i) in images" :key="i">
        <img :src="img.imageUrl" alt="" />
        <button class="image-remove" @click="removeImage(i)">&times;</button>
        <span class="image-sort">{{ i === 0 ? '封面' : i + 1 }}</span>
      </div>
    </div>
    <button type="button" class="upload-btn" @click="triggerUpload" :disabled="uploading">
      <span class="upload-icon">+</span>
      <span>{{ uploading ? '上传中...' : '上传图片' }}</span>
    </button>
    <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple hidden @change="handleUpload" />
    <p class="upload-hint text-xs text-muted mt-1">支持 JPG / PNG / GIF / WebP，单张不超过 5MB</p>
  </div>
</template>

<style scoped>
.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}
.image-item {
  aspect-ratio: 1;
  border-radius: var(--radius-sm);
  overflow: hidden;
  position: relative;
  border: 1px solid var(--border);
}
.image-item img { width: 100%; height: 100%; object-fit: cover; }
.image-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0,0,0,0.7);
  color: #fff;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(8px);
}
.image-sort {
  position: absolute;
  bottom: 4px;
  left: 4px;
  background: rgba(0,0,0,0.7);
  color: #fff;
  font-size: 0.65rem;
  padding: 2px 6px;
  border-radius: 3px;
  backdrop-filter: blur(8px);
}
.upload-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 16px 20px;
  border: 2px dashed var(--border);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 0.88rem;
  transition: all var(--transition);
  width: 100%;
  justify-content: center;
  background: transparent;
}
.upload-btn:hover { border-color: var(--primary); color: var(--primary); }
.upload-icon { font-size: 1.3rem; font-weight: 300; }
</style>
