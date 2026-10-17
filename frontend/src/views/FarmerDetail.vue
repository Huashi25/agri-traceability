<template>
  <div class="farmer-detail-page" v-loading="loading">
    <!-- Breadcrumb -->
    <div class="breadcrumb">
      <span class="crumb-link" @click="$router.push('/')">首页</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-link" @click="$router.push('/farmers')">农户列表</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">农户详情</span>
    </div>

    <div v-if="farmer" class="farmer-detail-content">
      <!-- Hero Section -->
      <div class="farmer-hero">
        <div class="hero-bg"></div>
        <div class="hero-body">
          <div class="hero-left">
            <div class="farmer-avatar-lg" :style="{ background: getFarmerColor(farmer.id) }">
              {{ (farmer.name || '?').charAt(0) }}
            </div>
            <div class="hero-info">
              <div class="hero-badge">
                <el-icon><CircleCheck /></el-icon> 认证农户
              </div>
              <h1 class="farmer-name-lg">{{ farmer.name }}</h1>
              <p class="farmer-farm-name">{{ farmer.farmName }}</p>
              <div class="hero-tags">
                <span class="hero-tag"><el-icon><LocationFilled /></el-icon> {{ farmer.location }}</span>
                <span class="hero-tag"><el-icon><Cherry /></el-icon> {{ farmer.mainCrop }}</span>
                <span class="hero-tag" v-if="farmer.farmArea"><el-icon><FullScreen /></el-icon> {{ farmer.farmArea }} 亩</span>
              </div>
            </div>
          </div>
          <div class="hero-right" v-if="farmer.longitude && farmer.latitude">
            <div class="mini-map" @click="$router.push('/origin-map')">
              <div class="mini-map-bg"></div>
              <div class="mini-map-pin" :style="pinStyle">
                <el-icon size="24" color="#1a5d3a"><MapLocation /></el-icon>
              </div>
              <div class="mini-map-label">点击查看完整地图</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Info Cards -->
      <div class="info-cards">
        <div class="info-card">
          <div class="info-icon"><el-icon><User /></el-icon></div>
          <div class="info-text">
            <div class="info-label">农场名称</div>
            <div class="info-value">{{ farmer.farmName }}</div>
          </div>
        </div>
        <div class="info-card">
          <div class="info-icon"><el-icon><LocationFilled /></el-icon></div>
          <div class="info-text">
            <div class="info-label">所在地</div>
            <div class="info-value">{{ farmer.location }}</div>
          </div>
        </div>
        <div class="info-card">
          <div class="info-icon"><el-icon><Cherry /></el-icon></div>
          <div class="info-text">
            <div class="info-label">主要作物</div>
            <div class="info-value">{{ farmer.mainCrop }}</div>
          </div>
        </div>
        <div class="info-card">
          <div class="info-icon"><el-icon><FullScreen /></el-icon></div>
          <div class="info-text">
            <div class="info-label">种植面积</div>
            <div class="info-value">{{ farmer.farmArea }} 亩</div>
          </div>
        </div>
      </div>

      <!-- Description -->
      <div class="desc-section" v-if="farmer.description">
        <div class="section-header">
          <span class="section-bar"></span>
          <h2 class="section-title">农户介绍</h2>
        </div>
        <div class="desc-content">{{ farmer.description }}</div>
      </div>

      <!-- Region Tags -->
      <div class="tags-section" v-if="regionTags.length > 0">
        <div class="section-header">
          <span class="section-bar"></span>
          <h2 class="section-title">产区标签</h2>
        </div>
        <div class="tags-list">
          <span v-for="tag in regionTags" :key="tag" class="region-tag-item">{{ tag }}</span>
        </div>
      </div>

      <!-- Related Products -->
      <div class="products-section" v-if="relatedProducts.length > 0">
        <div class="section-header">
          <span class="section-bar"></span>
          <h2 class="section-title">该产地的商品</h2>
        </div>
        <div class="products-grid">
          <div v-for="p in relatedProducts" :key="p.id" class="product-item" @click="$router.push(`/product/${p.id}`)">
            <div class="product-img">
              <img :src="getProductImage(p)" :alt="p.name" />
            </div>
            <div class="product-info">
              <h4>{{ p.name }}</h4>
              <div class="product-price">￥{{ p.price }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Back Button -->
      <div class="back-action">
        <el-button @click="$router.push('/farmers')" round>
          <el-icon><ArrowLeft /></el-icon>
          返回农户列表
        </el-button>
      </div>
    </div>

    <!-- Not Found -->
    <div v-else-if="!loading" class="not-found">
      <el-empty description="农户信息不存在">
        <el-button type="primary" @click="$router.push('/farmers')">返回农户列表</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { farmerApi, productApi, originMapApi } from '@/api'

import durianImg from '../assets/images/product-durian.jpg'
import orangeImg from '../assets/images/product-orange.jpg'
import riceImg from '../assets/images/product-rice.jpg'
import scallionImg from '../assets/images/product-scallion.jpg'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const farmer = ref(null)
const allProducts = ref([])
const mapFarmers = ref([])

const productImages = { '榴莲': durianImg, '脐橙': orangeImg, '大米': riceImg, '葱': scallionImg }
const getProductImage = (p) => {
  if (!p) return durianImg
  if (p.image) return p.image
  for (const key in productImages) { if (p.name?.includes(key)) return productImages[key] }
  return durianImg
}

const farmerColors = ['#1a5d3a', '#2d8e5a', '#4a9060', '#0f3d24']
const getFarmerColor = (id) => farmerColors[(id || 0) % farmerColors.length]

const regionTags = computed(() => {
  if (!farmer.value?.regionTags) return []
  if (Array.isArray(farmer.value.regionTags)) return farmer.value.regionTags
  return String(farmer.value.regionTags).split(/[,，、]/).map(t => t.trim()).filter(Boolean)
})

const relatedProducts = computed(() => {
  if (!farmer.value || !allProducts.value.length) return []
  const location = farmer.value.location
  const crop = farmer.value.mainCrop
  return allProducts.value.filter(p => {
    if (p.origin && location && p.origin.includes(location.substring(0, 2))) return true
    if (p.name && crop && p.name.includes(crop)) return true
    return false
  }).slice(0, 4)
})

const pinStyle = computed(() => {
  if (!farmer.value?.longitude || !farmer.value?.latitude) return {}
  const lng = parseFloat(farmer.value.longitude)
  const lat = parseFloat(farmer.value.latitude)
  const left = ((lng - 73) / (135 - 73)) * 100
  const top = ((53 - lat) / (53 - 18)) * 100
  return { left: `${Math.max(5, Math.min(95, left))}%`, top: `${Math.max(5, Math.min(95, top))}%` }
})

const loadFarmer = async () => {
  loading.value = true
  farmer.value = null
  try {
    const [farmerRes, productRes, mapRes] = await Promise.all([
      farmerApi.list(),
      productApi.list({ page: 1, pageSize: 100 }),
      originMapApi.mapData().catch(() => ({ data: [] }))
    ])
    const id = Number(route.params.id)
    farmer.value = farmerRes.data.find(f => f.id === id)
    allProducts.value = productRes.data.list || []
    mapFarmers.value = mapRes.data || []
    if (!farmer.value && mapFarmers.value.length > 0) {
      farmer.value = mapFarmers.value.find(f => f.id === id)
    }
  } catch (e) {
    console.error('Failed to load farmer:', e)
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, () => {
  if (route.params.id) {
    loadFarmer()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
})

onMounted(() => {
  if (route.params.id) loadFarmer()
})
</script>

<style scoped>
.farmer-detail-page { max-width: 1200px; margin: 0 auto; padding-bottom: 40px; }

.breadcrumb { margin-bottom: 24px; }
.crumb-link { font-size: 13px; color: #999; cursor: pointer; transition: color 0.2s; }
.crumb-link:hover { color: #1a5d3a; }
.crumb-sep { margin: 0 8px; color: #ddd; }
.crumb-current { font-size: 13px; color: #1a5d3a; font-weight: 500; }

/* Hero */
.farmer-hero { position: relative; border-radius: 20px; overflow: hidden; margin-bottom: 24px; box-shadow: 0 8px 30px rgba(0,0,0,0.1); }
.hero-bg { position: absolute; inset: 0; background: linear-gradient(135deg, #1a5d3a 0%, #2d8c3c 55%, #3da655 100%); }
.hero-bg::before { content: ''; position: absolute; inset: 0; background: radial-gradient(circle at 30% 70%, rgba(255,255,255,0.1) 0%, transparent 50%), radial-gradient(circle at 70% 30%, rgba(255,255,255,0.06) 0%, transparent 40%); }
.hero-body { position: relative; z-index: 1; display: flex; justify-content: space-between; align-items: center; padding: 40px; color: #fff; }
.hero-left { display: flex; align-items: center; gap: 24px; }
.farmer-avatar-lg { width: 88px; height: 88px; border-radius: 20px; display: flex; align-items: center; justify-content: center; font-size: 40px; font-weight: 700; color: #fff; background: rgba(255,255,255,0.15); border: 2px solid rgba(255,255,255,0.3); backdrop-filter: blur(10px); flex-shrink: 0; }
.hero-badge { display: inline-flex; align-items: center; gap: 5px; background: rgba(255,255,255,0.15); padding: 4px 12px; border-radius: 9999px; font-size: 12px; margin-bottom: 8px; }
.farmer-name-lg { font-size: 32px; font-weight: 800; margin-bottom: 4px; }
.farmer-farm-name { font-size: 16px; opacity: 0.85; margin-bottom: 14px; }
.hero-tags { display: flex; gap: 10px; flex-wrap: wrap; }
.hero-tag { display: inline-flex; align-items: center; gap: 4px; background: rgba(255,255,255,0.12); padding: 5px 14px; border-radius: 9999px; font-size: 13px; }

.hero-right { flex-shrink: 0; }
.mini-map { width: 240px; height: 160px; border-radius: 12px; position: relative; overflow: hidden; background: linear-gradient(160deg, #e8f5e9, #c3dfc6); border: 1px solid rgba(255,255,255,0.3); cursor: pointer; transition: transform 0.3s; }
.mini-map:hover { transform: scale(1.03); }
.mini-map-bg { position: absolute; inset: 0; background-image: linear-gradient(rgba(26,93,58,0.08) 1px, transparent 1px), linear-gradient(90deg, rgba(26,93,58,0.08) 1px, transparent 1px); background-size: 24px 24px; }
.mini-map-pin { position: absolute; transform: translate(-50%, -100%); animation: pinBounce 2s ease-in-out infinite; }
@keyframes pinBounce { 0%, 100% { transform: translate(-50%, -100%); } 50% { transform: translate(-50%, -110%); } }
.mini-map-label { position: absolute; bottom: 8px; left: 50%; transform: translateX(-50%); font-size: 11px; color: rgba(26,93,58,0.7); white-space: nowrap; }

/* Info Cards */
.info-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 32px; }
.info-card { display: flex; align-items: center; gap: 14px; background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.info-icon { width: 44px; height: 44px; border-radius: 10px; background: #e8f5e9; color: #1a5d3a; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.info-label { font-size: 12px; color: #999; margin-bottom: 4px; }
.info-value { font-size: 15px; font-weight: 600; color: #333; }

/* Section Headers */
.section-header { display: flex; align-items: center; gap: 10px; margin-bottom: 18px; }
.section-bar { width: 4px; height: 22px; background: #2d8c3c; border-radius: 9999px; }
.section-title { font-size: 20px; font-weight: 700; color: #1a5d3a; }

/* Description */
.desc-section { background: #fff; border-radius: 16px; padding: 28px 32px; margin-bottom: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.desc-content { font-size: 14px; color: #555; line-height: 2; }

/* Tags */
.tags-section { background: #fff; border-radius: 16px; padding: 28px 32px; margin-bottom: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.tags-list { display: flex; gap: 10px; flex-wrap: wrap; }
.region-tag-item { padding: 6px 16px; background: #e8f5e9; color: #1a5d3a; border-radius: 9999px; font-size: 13px; font-weight: 500; }

/* Products */
.products-section { margin-bottom: 32px; }
.products-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.product-item { border-radius: 12px; overflow: hidden; cursor: pointer; box-shadow: 0 2px 8px rgba(0,0,0,0.06); background: #fff; transition: all 0.3s; }
.product-item:hover { transform: translateY(-4px); box-shadow: 0 8px 20px rgba(0,0,0,0.1); }
.product-img { height: 130px; overflow: hidden; }
.product-img img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.4s; }
.product-item:hover .product-img img { transform: scale(1.08); }
.product-info { padding: 12px 14px; }
.product-info h4 { font-size: 14px; font-weight: 700; color: #333; margin-bottom: 6px; }
.product-price { font-size: 16px; font-weight: 700; color: #1a5d3a; }

/* Back */
.back-action { text-align: center; margin-top: 32px; }

/* Not Found */
.not-found { padding: 80px 0; }

@media (max-width: 768px) {
  .hero-body { flex-direction: column; gap: 24px; padding: 28px 20px; }
  .hero-left { flex-direction: column; text-align: center; }
  .info-cards { grid-template-columns: repeat(2, 1fr); }
  .products-grid { grid-template-columns: repeat(2, 1fr); }
  .desc-section, .tags-section { padding: 20px; }
}
</style>
