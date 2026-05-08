<script setup>
defineProps({ product: { type: Object, required: true } })
defineEmits(['click', 'fav'])

function imgUrl(url) {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('/p/')) return url
  return '/p/' + url
}
</script>

<template>
  <div class="product-card card" @click="$emit('click', product)">
    <div class="card-img">
      <img v-if="product.images?.length" :src="imgUrl(product.images[0].imageUrl)" alt="" loading="lazy" />
      <div v-else class="card-img-placeholder">
        <span>&#9654;</span>
      </div>
      <button class="card-fav" :class="{ active: product.isFavorited }" @click.stop="$emit('fav', product)" :title="product.isFavorited ? '取消收藏' : '收藏'">
        {{ product.isFavorited ? '&#9829;' : '&#9825;' }}
      </button>
      <div class="card-tags" v-if="product.tags?.length">
        <span class="card-tag" v-for="tag in product.tags.slice(0, 3)" :key="tag.id">{{ tag.name }}</span>
      </div>
      <div class="card-status" v-if="product.status && product.status !== 'PUBLISHED'">
        {{ product.status === 'SOLD' ? '已售' : product.status === 'AUDITING' ? '审核中' : '已下架' }}
      </div>
      <div class="card-shine"></div>
    </div>
    <div class="card-body">
      <h3 class="card-title">{{ product.title }}</h3>
      <p class="card-desc text-sm text-secondary" v-if="product.description">{{ product.description.slice(0, 60) }}{{ product.description.length > 60 ? '...' : '' }}</p>
      <div class="card-footer">
        <span class="card-price">&yen;{{ product.price }}</span>
        <span class="card-meta text-xs text-muted">{{ product.viewCount || 0 }} 浏览</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.product-card {
  overflow: hidden;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}
.product-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-lg);
  border-color: rgba(255, 107, 74, 0.2);
}

.card-img {
  aspect-ratio: 4/3;
  background: rgba(255, 255, 255, 0.02);
  position: relative;
  overflow: hidden;
}
.card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}
.product-card:hover .card-img img {
  transform: scale(1.06);
}
.card-img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  color: var(--text-muted);
  opacity: 0.2;
}
.card-shine {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.06) 0%, transparent 60%);
  pointer-events: none;
}
.card-tags {
  position: absolute;
  bottom: 8px;
  left: 8px;
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
.card-tag {
  background: rgba(0,0,0,0.6);
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.7rem;
  backdrop-filter: blur(8px);
}
.card-fav {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  color: rgba(255,255,255,0.8);
  background: rgba(0,0,0,0.35);
  backdrop-filter: blur(6px);
  opacity: 0;
  transition: all 0.25s ease;
  z-index: 2;
}
.card-fav.active {
  opacity: 1;
  color: #e74c3c;
  background: rgba(0,0,0,0.45);
}
.product-card:hover .card-fav {
  opacity: 1;
}
.card-fav:hover {
  transform: scale(1.15);
  background: rgba(0,0,0,0.55);
}
.card-status {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0,0,0,0.6);
  color: #fff;
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 0.7rem;
  backdrop-filter: blur(8px);
}
.card-body {
  padding: 16px;
}
.card-title {
  font-size: 0.9rem;
  font-weight: 600;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: var(--text);
}
.card-desc {
  margin-top: 6px;
}
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}
.card-price {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--primary);
  text-shadow: 0 0 10px rgba(255, 107, 74, 0.25);
}
</style>
