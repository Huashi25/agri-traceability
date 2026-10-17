<template>
  <div class="product-list-page">
    <div class="page-banner">
      <h1>全部商品</h1>
      <p>每一件商品都可溯源，从田间到餐桌全程透明</p>
    </div>

    <div class="filter-bar">
      <div class="cat-tabs">
        <div class="cat-tab" :class="{ active: !filter.categoryId }" @click="filter.categoryId = null; loadProducts()">全部</div>
        <div v-for="c in categories" :key="c.id" class="cat-tab" :class="{ active: filter.categoryId === c.id }" @click="filter.categoryId = c.id; loadProducts()">{{ c.name }}</div>
      </div>
      <el-input v-model="filter.name" placeholder="搜索商品..." clearable @clear="loadProducts" @keyup.enter="loadProducts" style="width: 240px">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <div class="sort-bar">
      <span class="result-count">共 {{ products.length }} 件商品</span>
      <div class="sort-tabs">
        <span class="sort-tab" :class="{ active: sortKey === 'default' }" @click="sortKey = 'default'">默认</span>
        <span class="sort-tab" :class="{ active: sortKey === 'price-asc' }" @click="sortKey = 'price-asc'">价格升序</span>
        <span class="sort-tab" :class="{ active: sortKey === 'price-desc' }" @click="sortKey = 'price-desc'">价格降序</span>
      </div>
    </div>

    <div class="product-grid">
      <div v-for="p in sortedProducts" :key="p.id" class="product-card" @click="goDetail(p.id)">
        <div class="card-image">
          <img :src="getProductImage(p)" :alt="p.name" />
          <div class="card-tag" v-if="p.origin">{{ p.origin }}</div>
          <div class="card-trace-badge" v-if="traceableIds.includes(p.id)">
            <el-icon size="12"><CircleCheck /></el-icon> 溯源
          </div>
        </div>
        <div class="card-body">
          <h3 class="card-title">{{ p.name }}</h3>
          <p class="card-desc">{{ p.description || '优质农产品' }}</p>
          <div class="card-footer">
            <div class="price-box">
              <span class="price-symbol">￥</span>
              <span class="price-num">{{ p.price }}</span>
            </div>
            <el-button type="primary" circle @click.stop="addToCart(p)" aria-label="加入购物车">
              <el-icon><ShoppingCart /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-if="products.length === 0" description="暂无商品" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productApi, categoryApi, traceBatchApi } from '../api'
import { useCartStore } from '../stores/cart'

import durianImg from '../assets/images/product-durian.jpg'
import orangeImg from '../assets/images/product-orange.jpg'
import riceImg from '../assets/images/product-rice.jpg'
import scallionImg from '../assets/images/product-scallion.jpg'

const router = useRouter()
const cartStore = useCartStore()
const productImages = { '榴莲': durianImg, '脐橙': orangeImg, '大米': riceImg, '葱': scallionImg }
const getProductImage = (p) => {
  if (!p) return durianImg
  if (p.image) return p.image
  for (const key in productImages) { if (p.name?.includes(key)) return productImages[key] }
  return durianImg
}

const products = ref([])
const categories = ref([])
const traceableIds = ref([])
const filter = ref({ categoryId: null, name: '' })
const sortKey = ref('default')

const sortedProducts = computed(() => {
  if (sortKey.value === 'price-asc') return [...products.value].sort((a, b) => a.price - b.price)
  if (sortKey.value === 'price-desc') return [...products.value].sort((a, b) => b.price - a.price)
  return products.value
})

const loadProducts = async () => {
  const res = await productApi.list({ name: filter.value.name, categoryId: filter.value.categoryId })
  products.value = res.data
}

const loadCategories = async () => { categories.value = (await categoryApi.list()).data }
const loadTraceable = async () => {
  try { const res = await traceBatchApi.list(); traceableIds.value = [...new Set(res.data.map(b => b.productId))] } catch {}
}

const addToCart = async (product) => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (!userInfo.id) { ElMessage.warning('请先登录'); return }
  const ok = await cartStore.addToCart(product.id, 1)
  if (ok) ElMessage.success(`${product.name} 已加入购物车`)
}

const goDetail = (id) => router.push(`/product/${id}`)

onMounted(() => { loadProducts(); loadCategories(); loadTraceable() })
</script>

<style scoped>
.product-list-page { max-width: 1200px; margin: 0 auto; }
.page-banner { background: linear-gradient(135deg, #1a5c25, #2d8c3c); border-radius: 16px; padding: 32px 40px; margin-bottom: 24px; color: white; }
.page-banner h1 { font-size: 28px; margin-bottom: 8px; }
.page-banner p { font-size: 14px; opacity: 0.85; }

.filter-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.cat-tabs { display: flex; gap: 8px; flex-wrap: wrap; }
.cat-tab { padding: 8px 18px; border-radius: 20px; font-size: 14px; color: #666; background: #fff; border: 1px solid #e8e8e8; cursor: pointer; transition: all 0.3s; }
.cat-tab:hover { border-color: #67c23a; color: #2d8c3c; }
.cat-tab.active { background: #2d8c3c; color: white; border-color: #2d8c3c; }

.sort-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 1px solid #f0f0f0; }
.result-count { font-size: 14px; color: #999; }
.sort-tabs { display: flex; gap: 16px; }
.sort-tab { font-size: 13px; color: #999; cursor: pointer; transition: color 0.2s; }
.sort-tab.active { color: #2d8c3c; font-weight: 600; }
.sort-tab:hover { color: #2d8c3c; }

.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 20px; }
.product-card { background: #fff; border-radius: 12px; overflow: hidden; cursor: pointer; transition: all 0.3s; border: 1px solid #f0f0f0; }
.product-card:hover { transform: translateY(-4px); box-shadow: 0 12px 24px rgba(0,0,0,0.08); }
.card-image { position: relative; height: 200px; overflow: hidden; background: #f9f9f9; }
.card-image img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s; }
.product-card:hover .card-image img { transform: scale(1.05); }
.card-tag { position: absolute; top: 10px; left: 10px; padding: 2px 10px; background: rgba(76,175,80,0.12); color: #2E7D32; border-radius: 12px; font-size: 12px; font-weight: 500; }
.card-trace-badge { position: absolute; top: 10px; right: 10px; padding: 3px 8px; background: rgba(45,140,60,0.9); color: white; border-radius: 4px; font-size: 11px; display: flex; align-items: center; gap: 3px; }
.card-body { padding: 16px; }
.card-title { font-size: 16px; font-weight: 600; color: #333; margin-bottom: 6px; }
.card-desc { font-size: 13px; color: #999; margin-bottom: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-footer { display: flex; align-items: center; justify-content: space-between; }
.price-box { display: flex; align-items: baseline; }
.price-symbol { font-size: 14px; color: #f56c6c; }
.price-num { font-size: 22px; font-weight: 700; color: #f56c6c; }
</style>
