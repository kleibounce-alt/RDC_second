<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { productApi } from '@/api/product'
import { favoriteApi, commentApi, followApi } from '@/api/social'
import { orderApi } from '@/api/order'
import { adminApi } from '@/api/admin'
import Toast from '@/components/Toast.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const product = ref(null)
const images = ref([])
const tags = ref([])
const seller = ref(null)
const comments = ref([])
const loading = ref(true)
const commentContent = ref('')
const replyTo = ref(null)
const submittingComment = ref(false)
const orderLoading = ref(false)
const favLoading = ref(false)
const isFaved = ref(false)
const isFollowed = ref(false)
const followLoading = ref(false)
const toast = ref(null)
const showBuyConfirm = ref(false)
const showBuySuccess = ref(false)

function imgUrl(url) {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('/p/')) return url
  return '/p/' + url
}

const currentImageIndex = ref(0)
const currentImage = computed(() => imgUrl(images.value[currentImageIndex.value]?.imageUrl))

onMounted(async () => {
  const id = route.params.id
  if (!id) return
  await loadDetail(id)
})

async function loadDetail(id) {
  loading.value = true
  try {
    const res = await productApi.detail(id)
    const data = res.data
    product.value = data.product
    images.value = data.images || []
    tags.value = data.tags || []
    seller.value = data.seller || null
    isFaved.value = !!data.isFavorited
    await loadComments()
  } catch (e) {
    toast.value?.show('加载商品失败', 'error')
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  try {
    const res = await commentApi.list(product.value.id)
    comments.value = res.data || []
  } catch { /* ignore */ }
}

function handleBuy() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  if (product.value.userId === userStore.user?.id) {
    toast.value?.show('不能购买自己的商品', 'error')
    return
  }
  showBuyConfirm.value = true
}

async function confirmBuy() {
  showBuyConfirm.value = false
  orderLoading.value = true
  try {
    await orderApi.create({ productId: product.value.id })
    showBuySuccess.value = true
    loadDetail(route.params.id)
  } catch (e) {
    toast.value?.show(e.message || '购买失败', 'error')
  } finally {
    orderLoading.value = false
  }
}

function closeSuccess() {
  showBuySuccess.value = false
}

async function toggleFav() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  favLoading.value = true
  try {
    await favoriteApi.toggle(product.value.id)
    isFaved.value = !isFaved.value
  } catch (e) { console.error('[fav]', e.message) }
  finally { favLoading.value = false }
}

async function submitComment() {
  if (!commentContent.value.trim()) return
  submittingComment.value = true
  try {
    if (replyTo.value) {
      await commentApi.reply({
        productId: product.value.id,
        parentId: replyTo.value,
        content: commentContent.value.trim(),
      })
    } else {
      await commentApi.add({
        productId: product.value.id,
        content: commentContent.value.trim(),
      })
    }
    commentContent.value = ''
    replyTo.value = null
    await loadComments()
  } catch (e) {
    toast.value?.show(e.message || '评论失败', 'error')
  } finally {
    submittingComment.value = false
  }
}

async function handleLike(comment) {
  if (!userStore.isLoggedIn) return
  try {
    await commentApi.like(comment.id)
    if (comment._liked) {
      comment.likeCount = Math.max(0, (comment.likeCount || 0) - 1)
      comment._liked = false
    } else {
      comment.likeCount = (comment.likeCount || 0) + 1
      comment._liked = true
    }
  } catch (e) { console.error('[like]', e.message) }
}

function setReply(comment) {
  replyTo.value = comment.id
  commentContent.value = ''
  window.scrollTo({ top: document.querySelector('.comment-form')?.offsetTop - 100, behavior: 'smooth' })
}

function cancelReply() {
  replyTo.value = null
  commentContent.value = ''
}

async function handleAdminRelist() {
  try {
    await adminApi.relist(product.value.id)
    toast.value?.show('已上架', 'success')
    product.value.status = 'PUBLISHED'
  } catch (e) {
    toast.value?.show(e.message || '操作失败', 'error')
  }
}

async function handleAdminOffShelf() {
  if (!confirm('确定下架该商品吗？')) return
  try {
    await adminApi.forceOffShelf(product.value.id)
    toast.value?.show('已下架', 'success')
    product.value.status = 'REJECTED'
  } catch (e) {
    toast.value?.show(e.message || '操作失败', 'error')
  }
}

async function handleAdminDelete() {
  if (!confirm('确定删除该商品吗？该操作不可恢复。')) return
  try {
    await adminApi.deleteProduct(product.value.id)
    toast.value?.show('已删除', 'success')
    router.replace('/')
  } catch (e) {
    toast.value?.show(e.message || '操作失败', 'error')
  }
}

function goSeller() {
  if (seller.value) {
    router.push(`/seller/${seller.value.id}`)
  }
}

async function toggleFollow() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  followLoading.value = true
  try {
    await followApi.toggle(seller.value.id)
    isFollowed.value = !isFollowed.value
  } catch { /* ignore */ }
  finally { followLoading.value = false }
}

function goChat() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  router.push(`/chat/${product.value.userId}`)
}
</script>

<template>
  <div class="container detail-page" v-if="!loading && product">
    <div class="detail-grid">
      <!-- images -->
      <div class="detail-gallery">
        <div class="main-image">
          <img v-if="currentImage" :src="currentImage" alt="" />
          <div v-else class="no-image">暂无图片</div>
        </div>
        <div class="thumb-list" v-if="images.length > 1">
          <button
            v-for="(img, i) in images"
            :key="i"
            :class="['thumb', { active: i === currentImageIndex }]"
            @click="currentImageIndex = i"
          >
            <img :src="imgUrl(img.imageUrl)" alt="" />
          </button>
        </div>
      </div>

      <!-- info -->
      <div class="detail-info">
        <div class="detail-status" v-if="product.status !== 'PUBLISHED'">
          <span class="badge" :class="product.status === 'SOLD' ? 'badge-danger' : product.status === 'AUDITING' ? 'badge-warning' : 'badge-danger'">
            {{ product.status === 'SOLD' ? '已售出' : product.status === 'AUDITING' ? '审核中' : product.status === 'PENDING' ? '待审核' : '已下架' }}
          </span>
        </div>

        <h1 class="detail-title">{{ product.title }} <span class="text-xs text-muted">#{{ product.id }}</span></h1>

        <div class="detail-price">&yen;{{ product.price }}</div>

        <div class="detail-meta">
          <span>{{ product.viewCount || 0 }} 次浏览</span>
          <span v-if="product.stock !== undefined">库存 {{ product.stock }}</span>
          <span>发布于 {{ product.createdAt?.substring(0, 10) }}</span>
        </div>

        <div class="detail-tags" v-if="tags.length">
          <span class="badge badge-primary" v-for="tag in tags" :key="tag.id">{{ tag.name }}</span>
        </div>

        <p class="detail-desc" v-if="product.description">{{ product.description }}</p>

        <div class="detail-actions">
          <button
            v-if="userStore.user?.id !== product.userId"
            class="btn btn-primary btn-lg"
            @click="handleBuy"
            :disabled="orderLoading || product.status !== 'PUBLISHED' || (product.stock !== undefined && product.stock <= 0)"
            style="flex:1"
          >
            {{ orderLoading ? '处理中...' : product.status === 'SOLD' ? '已售出' : (product.stock !== undefined && product.stock <= 0) ? '已卖完' : product.status === 'AUDITING' ? '审核中' : '立即购买' }}
          </button>
          <button class="btn btn-outline btn-lg" @click="toggleFav" :disabled="favLoading">
            {{ isFaved ? '♥' : '♡' }}
          </button>
        </div>

        <!-- seller card -->
        <div class="seller-card card mt-2" v-if="seller">
          <div class="seller-header">
            <div class="seller-avatar" @click="goSeller" role="button" tabindex="0">
              <img v-if="seller.avatar" :src="seller.avatar" alt="" />
              <span v-else class="avatar-placeholder">{{ (seller.nickname || seller.username || '?')[0] }}</span>
            </div>
            <div class="seller-info">
              <span class="seller-name" @click="goSeller" role="button" tabindex="0">
                {{ seller.nickname || seller.username }}
                <span class="badge badge-vip" v-if="seller.vipLevel">VIP{{ seller.vipLevel }}</span>
              </span>
              <span class="seller-username text-xs text-muted">@{{ seller.username }}</span>
              <span class="text-xs text-muted" v-if="seller.createdAt">{{ seller.createdAt.substring(0, 10) }} 加入</span>
            </div>
            <div class="seller-actions">
              <button class="btn btn-outline btn-sm" @click="toggleFollow" :disabled="followLoading" v-if="userStore.isLoggedIn && userStore.user?.id !== product.userId">
                {{ isFollowed ? '已关注' : '+ 关注' }}
              </button>
              <button class="btn btn-outline btn-sm" @click="goSeller">查看主页</button>
              <button class="btn btn-primary btn-sm" @click="goChat" v-if="userStore.isLoggedIn && userStore.user?.id !== product.userId">
                联系卖家
              </button>
            </div>
          </div>
        </div>

        <div class="admin-actions mt-2" v-if="userStore.isAdmin">
          <button class="btn btn-success btn-sm" @click="handleAdminRelist" v-if="product.status === 'REJECTED'">重新上架</button>
          <button class="btn btn-danger btn-sm" @click="handleAdminOffShelf" v-if="product.status === 'PUBLISHED'">强制下架</button>
          <button class="btn btn-danger btn-sm" @click="handleAdminDelete">删除商品</button>
        </div>
      </div>
    </div>

    <!-- comments -->
    <div class="comment-section mt-3">
      <h2 class="section-title">评论 ({{ comments.length }})</h2>

      <!-- comment form -->
      <div class="comment-form card" v-if="userStore.isLoggedIn">
        <div class="reply-to" v-if="replyTo">
          <span>正在回复 #{{ replyTo }}</span>
          <button class="link" @click="cancelReply">取消回复</button>
        </div>
        <textarea
          v-model="commentContent"
          class="form-input"
          :placeholder="replyTo ? '输入回复...' : '写下你的评论...'"
          rows="3"
        ></textarea>
        <div class="comment-form-footer">
          <button class="btn btn-primary btn-sm" @click="submitComment" :disabled="!commentContent.trim() || submittingComment">
            {{ submittingComment ? '发送中...' : '发送' }}
          </button>
        </div>
      </div>
      <div class="comment-form card text-center" v-else>
        <p class="text-secondary text-sm">请<router-link to="/login" class="link">登录</router-link>后评论</p>
      </div>

      <!-- comment list -->
      <div class="comment-list mt-2" v-if="comments.length">
        <div class="comment-item" v-for="comment in comments" :key="comment.id">
          <div class="comment-body">
            <span class="comment-user font-medium">{{ comment.userId }}</span>
            <p class="comment-content">{{ comment.content }}</p>
            <div class="comment-actions">
              <button class="action-btn" @click="handleLike(comment)">
                <span v-if="comment._liked" style="color:#e74c3c">&#9829;</span>
                <span v-else>&#9825;</span>
                {{ comment.likeCount || 0 }}
              </button>
              <button class="action-btn" @click="setReply(comment)" v-if="userStore.isLoggedIn">回复</button>
              <span class="text-xs text-muted">{{ comment.createdAt?.substring(0, 10) }}</span>
            </div>
          </div>

          <!-- replies -->
          <div class="reply-list" v-if="comment.children?.length">
            <div class="reply-item" v-for="reply in comment.children" :key="reply.id">
              <span class="font-medium text-sm">{{ reply.userId }}</span>
              <span class="text-sm text-secondary" v-if="reply.parentId"> 回复 </span>
              <p class="text-sm">{{ reply.content }}</p>
              <div class="comment-actions">
                <button class="action-btn" @click="handleLike(reply)">
                  <span v-if="reply._liked" style="color:#e74c3c">&#9829;</span>
                  <span v-else>&#9825;</span>
                  {{ reply.likeCount || 0 }}
                </button>
                <button class="action-btn" @click="setReply(reply)" v-if="userStore.isLoggedIn">回复</button>
                <span class="text-xs text-muted">{{ reply.createdAt?.substring(0, 10) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="empty-state" v-else>
        <p>暂无评论</p>
      </div>
    </div>

    <ConfirmModal
      :visible="showBuyConfirm && !showBuySuccess"
      :title="product?.title"
      :price="product?.price"
      @confirm="confirmBuy"
      @cancel="showBuyConfirm = false"
    />
    <ConfirmModal
      :visible="showBuySuccess"
      :title="product?.title"
      :price="product?.price"
      success
      @confirm="closeSuccess"
    />
    <Toast ref="toast" />
  </div>

  <div class="container detail-page" v-else-if="loading">
    <div class="empty-state"><p>加载中...</p></div>
  </div>

  <div class="container detail-page" v-else>
    <div class="empty-state">
      <div class="icon">&#128722;</div>
      <p>商品不存在或已删除</p>
    </div>
  </div>
</template>

<style scoped>
.detail-page {
  max-width: 1000px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
}

.detail-gallery {
  position: sticky;
  top: calc(var(--nav-height) + 20px);
  align-self: start;
}
.main-image {
  aspect-ratio: 1;
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--border-light);
}
.main-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.no-image {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 1.2rem;
}
.thumb-list {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
.thumb {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  overflow: hidden;
  opacity: 0.5;
  transition: opacity var(--transition);
  border: 2px solid transparent;
  padding: 0;
}
.thumb.active {
  opacity: 1;
  border-color: var(--primary);
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-status { margin-bottom: 12px; }
.detail-title {
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1.3;
}
.detail-price {
  font-size: 1.8rem;
  font-weight: 700;
  color: var(--primary);
  margin: 16px 0;
}
.detail-meta {
  display: flex;
  gap: 20px;
  font-size: 0.82rem;
  color: var(--text-muted);
  margin-bottom: 16px;
}
.detail-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.detail-desc {
  color: var(--text-secondary);
  line-height: 1.8;
  white-space: pre-wrap;
  margin-bottom: 24px;
}

.detail-actions {
  display: flex;
  gap: 12px;
  padding: 20px 0;
  border-top: 1px solid var(--border-light);
}

.section-title {
  font-size: 1.15rem;
  font-weight: 600;
  margin-bottom: 16px;
}

.comment-form {
  padding: 16px;
}
.reply-to {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.82rem;
  color: var(--primary);
  margin-bottom: 8px;
}
.comment-form-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}
.link { color: var(--primary); font-weight: 500; }

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.comment-item {
  background: var(--bg-white);
  border-radius: var(--radius-sm);
  padding: 16px;
  border: 1px solid var(--border-light);
}
.comment-content {
  margin: 4px 0 8px;
  font-size: 0.92rem;
  line-height: 1.5;
}
.comment-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}
.action-btn {
  font-size: 0.8rem;
  color: var(--text-muted);
  cursor: pointer;
  transition: color var(--transition);
}
.action-btn:hover { color: var(--primary); }

.reply-list {
  margin-top: 10px;
  padding-left: 16px;
  border-left: 2px solid var(--border-light);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* seller card */
.seller-card {
  padding: 16px;
}
.seller-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.seller-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  background: var(--border-light);
}
.seller-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.1rem;
  color: var(--primary);
  background: rgba(255, 107, 74, 0.12);
}
.seller-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.seller-name {
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.seller-name:hover { color: var(--primary); }
.seller-username {
  font-size: 0.78rem;
}
.seller-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.badge-vip {
  background: linear-gradient(135deg, #f7971e, #ffd200);
  color: #000;
  font-size: 0.65rem;
  padding: 1px 5px;
  border-radius: 4px;
}

@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
    gap: 24px;
  }
  .detail-gallery {
    position: static;
  }
}
</style>
