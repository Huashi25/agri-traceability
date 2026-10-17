<template>
  <div class="favorites-page">
    <div class="page-header">
      <h1>我的收藏</h1>
      <p>{{ favorites.length }} 件商品</p>
    </div>

    <div v-if="favorites.length === 0" class="empty-state">
      <el-empty description="还没有收藏的商品" :image-size="120" />
      <button class="btn-go-shop" @click="$router.push('/')">去逛逛</button>
    </div>

    <div v-else class="fav-grid">
      <div v-for="(p, idx) in favorites" :key="p.id" class="fav-card" :style="{ '--i': idx }" @click="goDetail(p.id)">
        <div class="fav-image">
          <img :src="getProductImage(p)" :alt="p.name" />
          <div class="fav-origin-tag" v-if="p.origin">{{ p.origin }}</div>
        </div>
        <div class="fav-info">
          <h3 class="fav-name">{{ p.name }}</h3>
          <p class="fav-desc">{{ p.description || '优质农产品' }}</p>
          <div class="fav-meta">
            <el-rate :model-value="Number(p.rating) || 5" disabled size="small" />
            <span class="fav-sales">已售 {{ p.salesCount || 0 }} 件</span>
          </div>
          <div class="fav-price-row">
            <div class="fav-price-info">
              <span class="fav-price">￥{{ p.price }}</span>
              <span class="fav-price-original" v-if="p.originalPrice && Number(p.originalPrice) > Number(p.price)">￥{{ p.originalPrice }}</span>
            </div>
            <button class="btn-remove-fav" @click.stop="removeFavorite(p.id)">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" stroke="none">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { favoriteApi } from '../api'
import { useUserStore } from '../stores/user'

import durianImg from '../assets/images/product-durian.jpg'
import orangeImg from '../assets/images/product-orange.jpg'
import riceImg from '../assets/images/product-rice.jpg'
import scallionImg from '../assets/images/product-scallion.jpg'

const router = useRouter()
const userStore = useUserStore()
const favorites = ref([])

const productImages = { '榴莲': durianImg, '脐橙': orangeImg, '大米': riceImg, '葱': scallionImg }
const getProductImage = (p) => {
  if (!p) return durianImg
  if (p.image) return p.image
  for (const key in productImages) { if (p.name?.includes(key)) return productImages[key] }
  return durianImg
}

const loadFavorites = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const res = await favoriteApi.list()
    favorites.value = res.data || []
  } catch {
    ElMessage.error('加载收藏失败')
  }
}

const removeFavorite = async (productId) => {
  try {
    await favoriteApi.toggle(productId)
    favorites.value = favorites.value.filter(p => p.id !== productId)
    ElMessage.success('已取消收藏')
  } catch {
    ElMessage.error('操作失败')
  }
}

const goDetail = (id) => router.push(`/product/${id}`)

onMounted(() => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  loadFavorites()
})
</script>

<style scoped>
.favorites-page { max-width: 1100px; margin: 0 auto; }
.page-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 20px; }
.page-header h1 { font-size: 24px; color: var(--text-primary); font-family: var(--font-heading); }
.page-header p { font-size: 14px; color: var(--text-tertiary); }

.empty-state { text-align: center; padding: 80px 20px; }
.btn-go-shop {
  margin-top: 16px;
  padding: 10px 28px;
  background: var(--brand-primary);
  color: white;
  border: none;
  border-radius: var(--radius-full, 9999px);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-go-shop:hover { background: var(--brand-primary-dark); transform: translateY(-1px); }

.fav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}
.fav-card {
  background: var(--bg-card);
  border-radius: var(--radius-md, 12px);
  overflow: hidden;
  box-shadow: var(--shadow-sm, 0 1px 3px rgba(0,0,0,0.04));
  cursor: pointer;
  transition: all 0.3s;
  animation: fadeInUp 0.4s ease both;
  animation-delay: calc(var(--i) * 0.05s);
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
.fav-card:hover {
  box-shadow: var(--shadow-md, 0 4px 12px rgba(0,0,0,0.08));
  transform: translateY(-2px);
}
.fav-image { position: relative; height: 200px; overflow: hidden; background: var(--cream-200, #f5f0e8); }
.fav-image img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.6s; }
.fav-card:hover .fav-image img { transform: scale(1.05); }
.fav-origin-tag {
  position: absolute; top: 8px; left: 8px;
  background: rgba(26, 93, 58, 0.9); color: white;
  font-size: 11px; padding: 3px 8px; border-radius: 4px;
}
.fav-info { padding: 16px; }
.fav-name { font-size: 15px; font-weight: 600; color: var(--text-primary); margin-bottom: 6px; }
.fav-desc { font-size: 12px; color: var(--text-tertiary); margin-bottom: 10px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.fav-meta { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.fav-sales { font-size: 12px; color: var(--text-tertiary); }
.fav-price-row { display: flex; align-items: center; justify-content: space-between; }
.fav-price-info { display: flex; align-items: baseline; gap: 8px; }
.fav-price { font-size: 18px; font-weight: 700; color: var(--brand-primary); }
.fav-price-original { font-size: 13px; color: var(--text-tertiary); text-decoration: line-through; }
.btn-remove-fav {
  width: 32px; height: 32px;
  border: none; background: transparent;
  color: #e53935;
  border-radius: 50%;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.btn-remove-fav:hover { background: rgba(229, 57, 53, 0.1); }
</style>
