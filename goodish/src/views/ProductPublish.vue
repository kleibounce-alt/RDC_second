<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productApi } from '@/api/product'
import ImageUploader from '@/components/ImageUploader.vue'
import Toast from '@/components/Toast.vue'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)

const form = reactive({
  title: '',
  description: '',
  price: '',
  stock: '1',
})
const images = ref([])
const loading = ref(false)
const toast = ref(null)

// 标签相关
const selectedTags = ref([])
const tagInput = ref('')
const allTags = ref([])
const showSuggestions = ref(false)

const filteredTags = computed(() => {
  const input = tagInput.value.trim().toLowerCase()
  if (!input) return []
  return allTags.value.filter(t =>
    t.name.toLowerCase().includes(input) &&
    !selectedTags.value.includes(t.name)
  )
})

onMounted(async () => {
  try {
    const res = await productApi.tagList()
    allTags.value = res.data || []
  } catch { /* ignore */ }

  if (isEdit.value) {
    try {
      const res = await productApi.detail(route.params.id)
      const p = res.data.product
      form.title = p.title || ''
      form.description = p.description || ''
      form.price = p.price || ''
      form.stock = p.stock ?? 1
      images.value = (res.data.images || []).map((img, i) => ({ imageUrl: img.imageUrl, sort: i }))
      selectedTags.value = (res.data.tags || []).map(t => t.name)
    } catch {
      toast.value?.show('加载商品失败', 'error')
      router.replace('/my-products')
    }
  }
})

function addTag(name) {
  name = name.trim()
  if (!name || selectedTags.value.includes(name)) return
  if (selectedTags.value.length >= 10) {
    toast.value?.show('最多添加10个标签', 'error')
    return
  }
  selectedTags.value.push(name)
  tagInput.value = ''
  showSuggestions.value = false
}

function removeTag(name) {
  selectedTags.value = selectedTags.value.filter(t => t !== name)
}

async function createAndAddTag() {
  const name = tagInput.value.trim()
  if (!name) return
  try {
    await productApi.tagCreate(name)
    allTags.value.push({ id: Date.now(), name })
    addTag(name)
  } catch (e) {
    toast.value?.show(e.message || '创建标签失败', 'error')
  }
}

function onTagKeydown(e) {
  if (e.key === 'Enter') {
    e.preventDefault()
    const match = filteredTags.value.find(t => t.name.toLowerCase() === tagInput.value.trim().toLowerCase())
    if (match) {
      addTag(match.name)
    } else if (tagInput.value.trim()) {
      createAndAddTag()
    }
  } else if (e.key === 'Backspace' && !tagInput.value && selectedTags.value.length) {
    selectedTags.value.pop()
  }
}

async function submit() {
  if (!form.title.trim()) {
    toast.value?.show('请输入标题', 'error')
    return
  }
  if (!form.price || Number(form.price) <= 0) {
    toast.value?.show('请输入正确的价格', 'error')
    return
  }
  if (!images.value.length) {
    toast.value?.show('请上传至少一张图片', 'error')
    return
  }

  const data = {
    title: form.title.trim(),
    description: form.description.trim(),
    price: Number(form.price),
    stock: Number(form.stock) || 1,
    images: images.value.map(img => img.imageUrl),
    tags: selectedTags.value,
  }

  loading.value = true
  try {
    if (isEdit.value) {
      await productApi.edit({ productId: Number(route.params.id), ...data })
      toast.value?.show('修改成功', 'success')
      router.replace('/my-products')
    } else {
      await productApi.publish(data)
      toast.value?.show('发布成功，等待审核', 'success')
      router.replace('/my-products')
    }
  } catch (e) {
    toast.value?.show(e.message || '操作失败', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="container publish-page">
    <h1 class="page-title">{{ isEdit ? '编辑商品' : '发布商品' }}</h1>

    <div class="card publish-card">
      <form @submit.prevent="submit">
        <div class="form-group">
          <label class="form-label">标题 <span class="required">*</span></label>
          <input v-model="form.title" class="form-input" placeholder="商品标题（最多200字）" maxlength="200" />
        </div>

        <div class="form-group">
          <label class="form-label">描述</label>
          <textarea v-model="form.description" class="form-input" rows="5" placeholder="详细描述商品信息..."></textarea>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">价格 (¥) <span class="required">*</span></label>
            <input v-model="form.price" type="number" class="form-input" placeholder="0.00" step="0.01" min="0" />
          </div>
          <div class="form-group">
            <label class="form-label">库存</label>
            <input v-model="form.stock" type="number" class="form-input" placeholder="1" min="1" />
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">图片 <span class="required">*</span></label>
          <ImageUploader v-model:images="images" />
        </div>

        <div class="form-group">
          <label class="form-label">标签</label>
          <div class="tag-chips" v-if="selectedTags.length">
            <span class="tag-chip" v-for="tag in selectedTags" :key="tag">
              {{ tag }}
              <button type="button" class="tag-chip-x" @click="removeTag(tag)">&times;</button>
            </span>
          </div>
          <div class="tag-input-wrapper">
            <input
              v-model="tagInput"
              class="form-input"
              placeholder="输入标签名，回车创建或选择已有标签"
              @focus="showSuggestions = true"
              @blur="setTimeout(() => showSuggestions = false, 200)"
              @keydown="onTagKeydown"
            />
            <div class="tag-suggestions" v-if="showSuggestions && filteredTags.length">
              <button
                type="button"
                class="tag-suggestion"
                v-for="t in filteredTags"
                :key="t.id"
                @mousedown.prevent="addTag(t.name)"
              >
                {{ t.name }}
              </button>
            </div>
          </div>
        </div>

        <div class="form-actions">
          <button type="button" class="btn btn-ghost" @click="router.back()">取消</button>
          <button type="submit" class="btn btn-primary btn-lg" :disabled="loading">
            {{ loading ? '提交中...' : (isEdit ? '保存修改' : '发布商品') }}
          </button>
        </div>
      </form>
    </div>

    <Toast ref="toast" />
  </div>
</template>

<style scoped>
.publish-page { max-width: 700px; }
.publish-card { padding: 28px; }
.required { color: var(--primary); }
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
  padding-top: 20px;
  border-top: 1px solid var(--border-light);
}

/* 标签样式 */
.tag-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}
.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 4px 10px;
  background: rgba(255,107,74,0.15);
  color: var(--primary);
  border-radius: 20px;
  font-size: 0.82rem;
}
.tag-chip-x {
  background: none;
  border: none;
  color: var(--primary);
  font-size: 1.1rem;
  cursor: pointer;
  padding: 0 2px;
  line-height: 1;
  opacity: 0.6;
}
.tag-chip-x:hover { opacity: 1; }

.tag-input-wrapper {
  position: relative;
}
.tag-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  max-height: 160px;
  overflow-y: auto;
  z-index: 10;
}
.tag-suggestion {
  display: block;
  width: 100%;
  padding: 8px 12px;
  border: none;
  background: none;
  color: var(--text);
  text-align: left;
  cursor: pointer;
  font-size: 0.85rem;
}
.tag-suggestion:hover {
  background: rgba(255,107,74,0.1);
}

@media (max-width: 480px) {
  .form-row { grid-template-columns: 1fr; }
}
</style>
