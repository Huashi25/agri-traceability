<template>
  <div class="home">
    <!-- Hero Banner with visual area -->
    <section class="hero-banner section-anim">
      <div class="hero-content">
        <span class="hero-badge">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L4 7v6c0 5 3.5 8.5 8 10 4.5-1.5 8-5 8-10V7l-8-5z"/></svg>
          全链路溯源 · 食品安全
        </span>
        <h1 class="hero-title">新鲜农产品<br>从田间直达您的餐桌</h1>
        <p class="hero-subtitle">每一件商品都带有独立溯源码，产地、质检、物流全程透明可查</p>
        <div class="hero-actions">
          <button class="btn-primary" @click="scrollToProducts">浏览商品</button>
          <button class="btn-secondary" @click="$router.push('/trace')">溯源查询</button>
        </div>
      </div>
      <div class="hero-visual">
        <el-carousel :interval="4000" arrow="hover" height="320px" class="hero-carousel">
          <el-carousel-item v-for="(img, idx) in heroImages" :key="idx">
            <img :src="img.src" :alt="img.alt" class="carousel-img" />
          </el-carousel-item>
        </el-carousel>
        <div class="floating-card card-1">
          <div class="float-icon"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L4 7v6c0 5 3.5 8.5 8 10 4.5-1.5 8-5 8-10V7l-8-5z"/></svg></div>
          <div class="float-text"><div class="float-title">产地认证</div><div class="float-sub">100%可追溯</div></div>
        </div>
        <div class="floating-card card-2">
          <div class="float-icon"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><rect x="1" y="3" width="15" height="13" rx="2"/><path d="M16 8h4l3 3v5h-7V8z"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg></div>
          <div class="float-text"><div class="float-title">冷链直送</div><div class="float-sub">24h新鲜送达</div></div>
        </div>
      </div>
    </section>

    <!-- Stats Grid with icons + count animation -->
    <section class="stats-grid section-anim">
      <div class="stat-card" v-for="s in stats" :key="s.label">
        <div class="stat-icon" v-html="s.icon"></div>
        <div class="stat-number">{{ s.display }}<span class="stat-unit" v-if="s.unit">{{ s.unit }}</span></div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </section>

    <!-- Seasonal Entry -->
    <div class="section-block section-anim" v-if="seasonalProducts.length > 0">
      <div class="section-header">
        <div class="section-eyebrow">IN SEASON</div>
        <h2 class="section-title">{{ monthNames[currentMonth - 1] }}时令</h2>
        <div class="editorial-divider"></div>
        <span class="section-more" @click="$router.push('/seasonal')">查看全年时令 →</span>
      </div>
      <div class="seasonal-grid">
        <div v-for="item in seasonalProducts.slice(0, 4)" :key="item.product.id" class="seasonal-card" @click="goDetail(item.product.id)">
          <div class="seasonal-img"><img :src="getProductImage(item.product)" :alt="item.product.name" /></div>
          <div class="seasonal-peak" v-if="item.isPeak">正当时</div>
          <div class="seasonal-info">
            <h4>{{ item.product.name }}</h4>
            <p class="seasonal-desc-text">{{ item.seasonalDesc || '应季好物' }}</p>
            <div class="seasonal-price">￥{{ item.product.price }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Category & Search -->
    <div class="filter-section" id="products">
      <div class="category-tabs">
        <div class="cat-tab" :class="{ active: !filter.categoryId }" @click="filter.categoryId = null; resetPageAndLoad()">全部</div>
        <div v-for="c in categories" :key="c.id" class="cat-tab" :class="{ active: filter.categoryId === c.id }" @click="filter.categoryId = c.id; resetPageAndLoad()">{{ c.name }}</div>
      </div>
      <el-input v-model="filter.name" placeholder="搜索商品..." clearable @clear="resetPageAndLoad" @keyup.enter="resetPageAndLoad" class="search-input" aria-label="搜索商品">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <!-- Product Grid - 2 column enhanced cards -->
    <div class="product-grid">
      <div v-for="(p, idx) in products" :key="p.id" class="product-card" :style="{ '--i': idx }" @click="goDetail(p.id)">
        <div class="product-image">
          <img :src="getProductImage(p)" :alt="p.name" />
          <div class="product-origin-tag" v-if="p.origin">{{ p.origin }}</div>
          <div class="product-badge" v-if="getProductBadge(p)">{{ getProductBadge(p) }}</div>
          <div class="product-trace-badge" v-if="traceableIds.includes(p.id)" @click.stop="goTrace">
            <el-icon size="12"><CircleCheck /></el-icon> 溯源可查
          </div>
        </div>
        <div class="product-info">
          <div class="product-category">{{ getCategoryName(p.categoryId) }}</div>
          <h3 class="product-name">{{ p.name }}</h3>
          <p class="product-desc">{{ p.description || '优质农产品，从田间直采' }}</p>
          <div class="product-meta">
            <el-rate :model-value="getProductRating(p)" disabled size="small" />
            <span class="product-sales">已售 {{ getProductSales(p) }} 件</span>
          </div>
          <div class="product-price-row">
            <div class="price-info">
              <span class="price-current">￥{{ p.price }}</span>
              <span class="price-original" v-if="getProductOriginalPrice(p)">￥{{ getProductOriginalPrice(p) }}</span>
            </div>
            <button class="btn-cart" @click.stop="addToCart(p)">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
            </button>
          </div>
          <div class="stock-hint" v-if="p.stock && p.stock < 50">仅剩 {{ p.stock }} 件</div>
        </div>
      </div>
    </div>
    <el-empty v-if="products.length === 0" description="暂无商品" />

    <!-- Pagination -->
    <div v-if="productPage.total > productPage.pageSize" class="pagination-bar">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="productPage.pageNum"
        :page-size="productPage.pageSize"
        :total="productPage.total"
        @current-change="handleProductPageChange"
      />
    </div>

    <!-- Recipe Recommendation -->
    <div class="section-block section-anim" v-if="featuredRecipes.length > 0">
      <div class="section-header">
        <div class="section-eyebrow">RECIPES</div>
        <h2 class="section-title">农产食谱</h2>
        <div class="editorial-divider"></div>
        <span class="section-more" @click="$router.push('/recipes')">更多食谱 →</span>
      </div>
      <div class="recipe-grid">
        <div v-for="r in featuredRecipes.slice(0, 3)" :key="r.id" class="recipe-card" @click="$router.push(`/recipe/${r.id}`)">
          <div class="recipe-cover">
            <img v-if="r.coverImage" :src="r.coverImage" :alt="r.title" class="recipe-cover-img" />
            <div v-else class="recipe-cover-placeholder">
              <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M5 13a4 4 0 0 1 4-4h6a4 4 0 0 1 4 4v6H5v-6z"/><path d="M9 9V7a3 3 0 0 1 6 0v2"/></svg>
            </div>
          </div>
          <div class="recipe-info">
            <h4>{{ r.title }}</h4>
            <p class="recipe-desc-text">{{ r.description }}</p>
            <div class="recipe-meta">
              <span v-if="r.cookTime"><el-icon><Clock /></el-icon> {{ r.cookTime }}分钟</span>
              <span v-if="r.servings"><el-icon><User /></el-icon> {{ r.servings }}人份</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Hot Ranking -->
    <div class="section-block section-anim" v-if="hotProducts.length > 0">
      <div class="section-header">
        <div class="section-eyebrow">HOT RANKING</div>
        <h2 class="section-title">热销排行</h2>
        <div class="editorial-divider"></div>
      </div>
      <div class="hot-ranking">
        <div v-for="(h, idx) in hotProducts" :key="h.productId" class="hot-card" @click="goDetail(h.productId)">
          <div class="hot-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</div>
          <div class="hot-product-img"><img :src="getProductImage({ name: h.productName, image: h.image })" :alt="h.productName" /></div>
          <div class="hot-info">
            <h3 class="hot-name">{{ h.productName }}</h3>
            <div class="hot-meta">
              <span class="hot-sales">已售 {{ h.salesCount }} 件</span>
              <span v-if="h.origin" class="hot-origin">{{ h.origin }}</span>
            </div>
          </div>
          <div class="hot-price-box"><span class="hot-price">￥{{ h.price }}</span></div>
        </div>
      </div>
    </div>

    <!-- Map Entry + Farmer Showcase (merged) -->
    <div class="section-block section-anim" v-if="mapFarmers.length > 0 || farmers.length > 0">
      <div class="section-header">
        <div class="section-eyebrow">DIRECT FROM FARM</div>
        <h2 class="section-title">产地直供</h2>
        <div class="editorial-divider"></div>
        <span class="section-more" @click="$router.push('/origin-map')">查看完整地图 →</span>
      </div>
      <div class="map-entry" v-if="mapFarmers.length > 0">
        <div class="map-preview">
          <svg class="map-china-svg" viewBox="0 0 100 80" preserveAspectRatio="none">
            <path d="M 85,10 L 72,8 L 55,6 L 38,7 L 20,10 L 8,14 L 3,20 L 5,30 L 2,38 L 7,45 L 4,52 L 10,56 L 18,55 L 25,60 L 20,68 L 28,72 L 38,70 L 48,74 L 58,72 L 68,70 L 75,65 L 80,60 L 88,55 L 92,48 L 88,40 L 82,35 L 88,28 L 92,22 L 88,16 Z" fill="rgba(45,140,60,0.12)" stroke="rgba(26,93,58,0.3)" stroke-width="0.3"/>
            <path d="M 15,30 Q 25,28 35,32 Q 45,35 55,33 Q 65,31 72,35" fill="none" stroke="rgba(123,179,212,0.4)" stroke-width="0.5"/>
            <path d="M 10,50 Q 20,48 30,52 Q 40,55 50,53 Q 60,51 70,50 Q 78,48 85,52" fill="none" stroke="rgba(123,179,212,0.4)" stroke-width="0.5"/>
          </svg>
          <div class="map-dot" v-for="f in mapFarmers" :key="f.id"
            :style="{ left: ((f.longitude - 73) / (135 - 73) * 100) + '%', top: ((53 - f.latitude) / (53 - 18) * 100) + '%' }"
            :title="f.farmName" @click="$router.push(`/farmer/${f.id}`)">
            <div class="map-dot-inner"></div>
            <div class="map-tooltip">{{ f.name }} · {{ f.location }}</div>
          </div>
        </div>
        <div class="map-farmer-list">
          <div v-for="f in mapFarmers.slice(0, 3)" :key="f.id" class="map-farmer-item" @click="$router.push(`/farmer/${f.id}`)">
            <div class="map-farmer-avatar" :style="{ background: getFarmerColor(f.id) }">{{ (f.name || '?').charAt(0) }}</div>
            <div class="map-farmer-info">
              <h5>{{ f.farmName }}</h5>
              <span>{{ f.location }} · {{ f.mainCrop }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="farmer-showcase">
        <div v-for="f in farmers.slice(0, 4)" :key="f.id" class="farmer-card" @click="$router.push(`/farmer/${f.id}`)">
          <div class="farmer-image">
            <div class="farmer-avatar" :style="{ background: getFarmerColor(f.id) }">{{ (f.name || '?').charAt(0) }}</div>
            <div class="farmer-cert"><el-icon size="12" color="#fff"><CircleCheck /></el-icon></div>
          </div>
          <div class="farmer-info">
            <div class="farmer-header">
              <h4 class="farmer-name">{{ f.name }}</h4>
              <span class="farmer-product-tag">{{ f.mainCrop }}</span>
            </div>
            <div class="farmer-meta">
              <div class="meta-item"><svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg><span>{{ f.location }}</span></div>
              <div class="meta-item"><svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg><span>{{ f.farmName }}</span></div>
              <div class="meta-item"><svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M3 3v18h18"/><path d="M7 14l4-4 4 4 6-6"/></svg><span>{{ f.farmArea }} 亩</span></div>
            </div>
            <p class="farmer-desc">{{ f.description }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Reviews -->
    <div class="section-block section-anim" v-if="reviews.length > 0">
      <div class="section-header">
        <div class="section-eyebrow">USER REVIEWS</div>
        <h2 class="section-title">用户评价</h2>
        <div class="editorial-divider"></div>
      </div>
      <div class="review-showcase">
        <div v-for="r in reviews.slice(0, 3)" :key="r.id" class="review-card" @click="r.productId && $router.push(`/product/${r.productId}`)">
          <div class="review-header">
            <div class="review-avatar">{{ (r.userName || '?').charAt(0).toUpperCase() }}</div>
            <div class="review-meta"><div class="review-user">{{ r.userName }}</div><el-rate v-model="r.rating" disabled size="small" /></div>
          </div>
          <div class="review-content">"{{ r.content }}"</div>
          <div class="review-date">{{ r.createTime?.replace('T', ' ').substring(0, 10) }}</div>
        </div>
      </div>
    </div>

    <!-- Guarantee Section -->
    <section class="guarantee-section section-anim">
      <div class="guarantee-header">
        <h2 class="guarantee-title">溯源保障</h2>
        <p class="guarantee-subtitle">从田间到餐桌，每一步都透明可查</p>
      </div>
      <div class="guarantee-grid">
        <div class="guarantee-card" v-for="(g, i) in guarantees" :key="i">
          <div class="guarantee-number">{{ String(i+1).padStart(2,'0') }}</div>
          <div class="guarantee-icon" v-html="g.icon"></div>
          <h3 class="guarantee-item-title">{{ g.title }}</h3>
          <p class="guarantee-desc">{{ g.desc }}</p>
        </div>
      </div>
      <button class="btn-guarantee-cta" @click="$router.push('/trace')">
        立即体验溯源查询
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" style="margin-left:8px"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
      </button>
    </section>

    <!-- Back to top -->
    <transition name="fade">
      <div v-if="showBackTop" class="back-top" @click="scrollToTop" aria-label="回到顶部">
        <el-icon size="20"><Top /></el-icon>
      </div>
    </transition>

    <!-- Announcement Dialog -->
    <el-dialog v-model="announceDialog" :title="currentAnnounce?.title" width="500px">
      <div class="announce-detail">
        <span class="announce-type">{{ currentAnnounce?.type === 0 ? '系统公告' : currentAnnounce?.type === 1 ? '活动' : '通知' }}</span>
        <p class="announce-detail-content">{{ currentAnnounce?.content }}</p>
        <p class="announce-detail-time">{{ currentAnnounce?.createTime?.replace('T', ' ').substring(0, 16) }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productApi, categoryApi, traceBatchApi, announcementApi, farmerApi, reviewApi, seasonalApi, recipeApi, publicStatsApi, originMapApi } from '../api'
import { useCartStore } from '../stores/cart'

import durianImg from '../assets/images/product-durian.jpg'
import orangeImg from '../assets/images/product-orange.jpg'
import riceImg from '../assets/images/product-rice.jpg'
import scallionImg from '../assets/images/product-scallion.jpg'

const router = useRouter()
const cartStore = useCartStore()

const productImages = { '榴莲': durianImg, '脐橙': orangeImg, '大米': riceImg, '葱': scallionImg }

const heroImages = [
  { src: durianImg, alt: '农场实景' },
  { src: orangeImg, alt: '新鲜蔬果' },
  { src: riceImg, alt: '田园风光' },
  { src: scallionImg, alt: '产地直采' }
]

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
const productPage = ref({ pageNum: 1, pageSize: 8, total: 0 })
const showBackTop = ref(false)
const announcements = ref([])
const farmers = ref([])
const reviews = ref([])
const hotProducts = ref([])
const announceDialog = ref(false)
const currentAnnounce = ref(null)
const seasonalProducts = ref([])
const newArrivals = ref([])
const featuredRecipes = ref([])
const publicStats = ref({})
const mapFarmers = ref([])

const currentMonth = new Date().getMonth() + 1
const monthNames = ['一月','二月','三月','四月','五月','六月','七月','八月','九月','十月','十一月','十二月']

const stats = ref([
  { icon: '<svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>', display: 0, unit: '', label: '严选好品', target: 0 },
  { icon: '<svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2L4 7v6c0 5 3.5 8.5 8 10 4.5-1.5 8-5 8-10V7l-8-5z"/></svg>', display: 100, unit: '%', label: '可溯源', target: 100 },
  { icon: '<svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>', display: 24, unit: 'h', label: '新鲜直达', target: 24 },
  { icon: '<svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>', display: 4, unit: '', label: '覆盖产区', target: 4 },
])

const processSteps = [
  { name: '种植产地', desc: '记录农场位置、农户信息、种植日期', icon: '<svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2v20M2 12h20M12 2l-3 3M12 2l3 3"/></svg>' },
  { name: '质量检测', desc: '农残检测、质量分级、合格认证', icon: '<svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M9 11l3 3L22 4M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>' },
  { name: '物流运输', desc: '冷链配送、温度监控、全程追踪', icon: '<svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="1" y="3" width="15" height="13" rx="2"/><path d="M16 8h4l3 3v5h-7V8z"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>' },
  { name: '扫码溯源', desc: '消费者扫码查看全程信息', icon: '<svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><path d="M14 14h7v7h-7z"/></svg>' },
]

const guarantees = [
  { title: '产地透明', desc: '每件商品均可查询种植基地、农户姓名、种植面积等产地详细信息', icon: '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>' },
  { title: '质检认证', desc: '所有商品经过专业质检，农残检测报告可查，不合格商品不上架', icon: '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M9 11l3 3L22 4M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>' },
  { title: '冷链直送', desc: '全程冷链运输，温度实时监控，确保农产品新鲜度与品质', icon: '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="1" y="3" width="15" height="13" rx="2"/><path d="M16 8h4l3 3v5h-7V8z"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>' },
  { title: '全程溯源', desc: '从种植到配送的每个环节都有记录，溯源码唯一可查不可篡改', icon: '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><path d="M14 14h7v7h-7z"/></svg>' },
]

const farmerColors = ['#1a5d3a', '#2d8e5a', '#4a9060', '#0f3d24']
const getFarmerColor = (id) => farmerColors[id % farmerColors.length]

const getProductRating = (p) => p.rating ? Number(p.rating) : 5
const getProductSales = (p) => p.salesCount || 0
const getProductOriginalPrice = (p) => p.originalPrice ? Number(p.originalPrice) : null
const getProductBadge = (p) => {
  if (p.salesCount && p.salesCount >= 50) return '热销推荐'
  if (p.originalPrice && Number(p.originalPrice) > Number(p.price)) return '限时特惠'
  if (p.origin) return '产地直发'
  return null
}

const loadProducts = async () => {
  const res = await productApi.list({
    name: filter.value.name,
    categoryId: filter.value.categoryId,
    page: productPage.value.pageNum,
    pageSize: productPage.value.pageSize
  })
  products.value = res.data.list
  productPage.value.total = res.data.total
  stats.value[0].target = res.data.total
  stats.value[0].display = res.data.total
}
const resetPageAndLoad = () => {
  productPage.value.pageNum = 1
  loadProducts()
}
const handleProductPageChange = (newPage) => {
  productPage.value.pageNum = newPage
  loadProducts()
  document.getElementById('products')?.scrollIntoView({ behavior: 'smooth' })
}
const loadCategories = async () => { const res = await categoryApi.list(); categories.value = res.data }
const loadTraceableProducts = async () => { try { const res = await traceBatchApi.list(); traceableIds.value = [...new Set(res.data.map(b => b.productId))] } catch {} }
const loadAnnouncements = async () => { try { const res = await announcementApi.list(); announcements.value = res.data.filter(a => a.status === 1).slice(0, 5) } catch {} }
const loadFarmers = async () => { try { const res = await farmerApi.list(); farmers.value = res.data.filter(f => f.status === 1) } catch {} }
const loadReviews = async () => { try { const res = await reviewApi.list(); reviews.value = res.data.filter(r => r.status === 1) } catch {} }
const loadHotProducts = async () => { try { const res = await publicStatsApi.hotProducts(); hotProducts.value = res.data } catch {} }
const loadSeasonalProducts = async () => { try { const res = await seasonalApi.current(); seasonalProducts.value = res.data || [] } catch {} }
const loadNewArrivals = async () => { try { const res = await productApi.list({ page: 1, pageSize: 4 }); newArrivals.value = (res.data.list || []).slice(0, 4) } catch {} }
const loadFeaturedRecipes = async () => { try { const res = await recipeApi.list(); featuredRecipes.value = (res.data || []).slice(0, 3) } catch {} }
const loadPublicStats = async () => { try { const res = await publicStatsApi.summary(); publicStats.value = res.data || {} } catch {} }
const loadMapFarmers = async () => { try { const res = await originMapApi.mapData(); mapFarmers.value = (res.data || []).slice(0, 5) } catch {} }

const getCategoryName = (id) => categories.value.find(c => c.id === id)?.name || '农产品'
const showAnnouncement = (a) => { currentAnnounce.value = a; announceDialog.value = true }
const addToCart = async (product) => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (!userInfo.id) { ElMessage.warning('请先登录'); router.push('/login'); return }
  const ok = await cartStore.addToCart(product.id, 1)
  if (ok) ElMessage.success(`${product.name} 已加入购物车`)
}
const goDetail = (id) => router.push(`/product/${id}`)
const goTrace = () => router.push('/trace')
const scrollToProducts = () => document.getElementById('products')?.scrollIntoView({ behavior: 'smooth' })
const scrollToTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })
const handleScroll = () => { showBackTop.value = window.scrollY > 500 }

onMounted(() => {
  loadProducts(); loadCategories(); loadTraceableProducts(); loadFarmers(); loadReviews(); loadHotProducts()
  loadSeasonalProducts(); loadFeaturedRecipes(); loadMapFarmers()
  window.addEventListener('scroll', handleScroll)
})
onUnmounted(() => window.removeEventListener('scroll', handleScroll))
</script>

<style scoped>
.home { max-width: 1280px; margin: 0 auto; }

/* === Hero Banner === */
.hero-banner {
  display: flex; align-items: center; justify-content: space-between;
  background: linear-gradient(135deg, #0f3d24 0%, #1a5d3a 50%, #2d8e5a 100%);
  border-radius: var(--radius-lg); overflow: hidden; position: relative;
  margin-bottom: var(--space-2xl);
}
.hero-banner::before {
  content: ''; position: absolute; right: -100px; top: -100px;
  width: 400px; height: 400px; border-radius: 50%;
  background: rgba(255,255,255,0.05);
}
.hero-banner::after {
  content: ''; position: absolute; right: 50px; bottom: -150px;
  width: 300px; height: 300px; border-radius: 50%;
  background: rgba(255,255,255,0.03);
}
.hero-content { padding: var(--space-2xl); position: relative; z-index: 2; flex: 1; }
.hero-badge {
  display: inline-flex; align-items: center; gap: 6px;
  background: rgba(255,255,255,0.15); backdrop-filter: blur(10px);
  color: white; font-size: 12px; padding: 6px 14px;
  border-radius: var(--radius-full); margin-bottom: var(--space-lg);
}
.hero-title {
  font-family: var(--font-heading); font-size: 42px; font-weight: 700;
  color: white; line-height: 1.3; margin-bottom: var(--space-md);
}
.hero-subtitle {
  font-size: 15px; color: rgba(255,255,255,0.85);
  max-width: 420px; line-height: 1.7; margin-bottom: var(--space-xl);
}
.hero-actions { display: flex; gap: var(--space-md); }
.btn-primary {
  background: #fff; color: var(--color-primary-dark); border: none;
  padding: 12px 32px; font-size: 14px; font-weight: 600;
  border-radius: var(--radius-full); cursor: pointer;
  transition: all 0.25s var(--ease-out);
}
.btn-primary:hover { transform: translateY(-2px); box-shadow: var(--shadow-lg); }
.btn-secondary {
  background: transparent; color: white; border: 1px solid rgba(255,255,255,0.4);
  padding: 12px 32px; font-size: 14px; font-weight: 500;
  border-radius: var(--radius-full); cursor: pointer;
  transition: all 0.25s var(--ease-out);
}
.btn-secondary:hover { background: rgba(255,255,255,0.1); border-color: rgba(255,255,255,0.8); }

/* Hero visual area */
.hero-visual { position: relative; width: 45%; padding: var(--space-lg); }
.hero-carousel { border-radius: var(--radius-md); overflow: hidden; box-shadow: var(--shadow-lg); }
.carousel-img { width: 100%; height: 320px; object-fit: cover; }

.floating-card {
  position: absolute; display: flex; align-items: center; gap: 10px;
  background: rgba(255,255,255,0.95); backdrop-filter: blur(10px);
  border-radius: var(--radius-md); padding: 10px 16px;
  box-shadow: var(--shadow-lg); animation: floatUp 3s ease-in-out infinite alternate;
}
.card-1 { top: 20px; right: 12px; }
.card-2 { bottom: 40px; left: 12px; animation-delay: 1.5s; }
.float-icon { width: 36px; height: 36px; border-radius: 50%; background: var(--color-primary-bg); color: var(--color-primary); display: flex; align-items: center; justify-content: center; }
.float-title { font-size: 13px; font-weight: 600; color: var(--color-title); }
.float-sub { font-size: 11px; color: var(--color-text-secondary); }

/* === Stats Grid === */
.stats-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--space-md);
  margin-bottom: var(--space-2xl);
}
.stat-card {
  text-align: center; background: var(--color-card);
  border-radius: var(--radius-md); padding: var(--space-lg) var(--space-sm);
  box-shadow: var(--shadow-sm); transition: all 0.3s var(--ease-out);
}
.stat-card:hover { transform: translateY(-4px); box-shadow: var(--shadow-hover); }
.stat-icon {
  width: 48px; height: 48px; margin: 0 auto var(--space-sm);
  color: var(--color-primary); display: flex; align-items: center; justify-content: center;
  background: var(--color-primary-bg); border-radius: 50%;
}
.stat-number {
  font-size: 32px; font-weight: 800; color: var(--color-primary-dark);
  font-family: var(--font-number); line-height: 1;
}
.stat-unit { font-size: 18px; font-weight: 600; }
.stat-label { font-size: 13px; color: var(--color-text-secondary); margin-top: 4px; }

/* === Filter === */
.filter-section { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--space-lg); gap: var(--space-md); }
.category-tabs { display: flex; gap: 0; flex-wrap: wrap; }
.cat-tab {
  padding: 8px 16px; font-size: 13px; font-weight: 500;
  color: var(--color-text-secondary); cursor: pointer;
  border-bottom: 2px solid transparent; transition: all 0.25s var(--ease-out);
}
.cat-tab:hover { color: var(--color-primary); }
.cat-tab.active { color: var(--color-primary); border-bottom-color: var(--color-primary); }
.search-input { width: 240px; }

/* === Product Grid (2-column) === */
.product-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: var(--space-lg); }
.product-card {
  background: var(--color-card); border-radius: var(--radius-md);
  overflow: hidden; cursor: pointer; box-shadow: var(--shadow-sm);
  transition: all 0.3s var(--ease-out);
  animation: cardEnter 0.5s ease both; animation-delay: calc(var(--i) * 0.08s);
}
.product-card:hover { transform: translateY(-4px); box-shadow: var(--shadow-hover); }

.product-image { position: relative; aspect-ratio: 4/3; overflow: hidden; }
.product-image img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.4s ease; }
.product-card:hover .product-image img { transform: scale(1.08); }

.product-origin-tag {
  position: absolute; top: 8px; left: 8px;
  background: rgba(255,255,255,0.95); backdrop-filter: blur(8px);
  color: var(--color-primary-dark); font-size: 12px; padding: 4px 12px;
  border-radius: var(--radius-full);
}
.product-badge {
  position: absolute; top: 8px; right: 8px;
  background: var(--color-accent); color: white;
  font-size: 11px; font-weight: 600; padding: 3px 10px;
  border-radius: var(--radius-sm);
}
.product-trace-badge {
  position: absolute; bottom: 8px; right: 8px;
  background: rgba(26,93,58,0.9); backdrop-filter: blur(8px);
  color: white; font-size: 11px; padding: 4px 10px;
  border-radius: var(--radius-full); display: flex; align-items: center; gap: 4px; cursor: pointer;
}

.product-info { padding: var(--space-md); }
.product-category { font-size: 11px; color: var(--color-text-hint); letter-spacing: 0.1em; text-transform: uppercase; margin-bottom: 4px; }
.product-name { font-family: var(--font-heading); font-size: 18px; font-weight: 700; color: var(--color-title); margin-bottom: 4px; }
.product-desc { font-size: 13px; color: var(--color-text-secondary); margin-bottom: var(--space-sm); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-meta { display: flex; align-items: center; gap: var(--space-sm); margin-bottom: var(--space-sm); }
.product-sales { font-size: 12px; color: var(--color-text-hint); }

.product-price-row { display: flex; align-items: flex-end; justify-content: space-between; }
.price-info { display: flex; align-items: baseline; gap: 6px; }
.price-current { font-size: 22px; font-weight: 700; color: var(--color-danger); font-family: var(--font-number); }
.price-original { font-size: 13px; color: var(--color-text-hint); text-decoration: line-through; }

.btn-cart {
  width: 40px; height: 40px; border-radius: 50%;
  background: var(--color-primary); color: white; border: none;
  display: flex; align-items: center; justify-content: center;
  box-shadow: var(--shadow-sm); cursor: pointer; transition: all 0.25s var(--ease-out);
}
.btn-cart:hover { background: var(--color-primary-dark); transform: scale(1.1); }
.btn-cart:active { transform: scale(0.95); }

.stock-hint { margin-top: var(--space-sm); font-size: 11px; color: var(--color-accent); font-weight: 500; }

/* === Pagination === */
.pagination-bar { display: flex; justify-content: center; margin-top: var(--space-xl); margin-bottom: var(--space-sm); }

/* === Section Blocks === */
.section-block { margin-top: var(--space-3xl); }
.section-header { text-align: center; margin-bottom: var(--space-xl); }
.section-eyebrow {
  display: block; font-size: 11px; font-weight: 500;
  letter-spacing: 0.25em; text-transform: uppercase;
  color: var(--color-primary); margin-bottom: 8px;
}
.section-title { font-family: var(--font-heading); font-size: 28px; font-weight: 700; color: var(--color-title); }
.editorial-divider { width: 40px; height: 2px; background: var(--color-primary); margin: 12px auto 0; border-radius: 1px; }
.section-more {
  display: inline-block; font-size: 13px; color: var(--color-primary);
  margin-left: 12px; cursor: pointer; transition: opacity 0.2s;
  vertical-align: middle;
}
.section-more:hover { opacity: 0.7; }

/* === Seasonal Entry === */
.seasonal-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
.seasonal-card {
  position: relative; border-radius: var(--radius-md); overflow: hidden;
  cursor: pointer; box-shadow: var(--shadow-sm); transition: transform 0.3s, box-shadow 0.3s;
}
.seasonal-card:hover { transform: translateY(-4px); box-shadow: var(--shadow-lg); }
.seasonal-img { height: 160px; overflow: hidden; }
.seasonal-img img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.4s; }
.seasonal-card:hover .seasonal-img img { transform: scale(1.08); }
.seasonal-peak {
  position: absolute; top: 10px; left: 10px;
  background: #e74c3c; color: white; font-size: 11px; font-weight: 600;
  padding: 3px 10px; border-radius: var(--radius-full); z-index: 2;
}
.seasonal-info { padding: 12px 14px; }
.seasonal-info h4 { font-size: 15px; font-weight: 700; color: var(--color-title); margin-bottom: 4px; }
.seasonal-desc-text { font-size: 12px; color: var(--color-text-secondary); margin-bottom: 8px; line-height: 1.5; }
.seasonal-price { font-family: var(--font-number); font-size: 18px; font-weight: 700; color: var(--color-primary); }

/* === New Arrivals === */
.new-arrivals-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
.arrival-card {
  border-radius: var(--radius-md); overflow: hidden; cursor: pointer;
  box-shadow: var(--shadow-sm); transition: transform 0.3s, box-shadow 0.3s;
  background: white;
}
.arrival-card:hover { transform: translateY(-4px); box-shadow: var(--shadow-md); }
.arrival-img { height: 140px; overflow: hidden; }
.arrival-img img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.4s; }
.arrival-card:hover .arrival-img img { transform: scale(1.08); }
.arrival-info { padding: 12px 14px; }
.arrival-origin { font-size: 11px; color: var(--color-text-hint); display: block; margin-bottom: 4px; }
.arrival-info h4 { font-size: 14px; font-weight: 700; color: var(--color-title); margin-bottom: 6px; }
.arrival-price { font-family: var(--font-number); font-size: 18px; font-weight: 700; color: var(--color-primary); }

/* === Recipe Recommendation === */
.recipe-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px; }
.recipe-card {
  border-radius: var(--radius-md); overflow: hidden; cursor: pointer;
  box-shadow: var(--shadow-sm); transition: transform 0.3s, box-shadow 0.3s;
  background: white;
}
.recipe-card:hover { transform: translateY(-4px); box-shadow: var(--shadow-md); }
.recipe-cover { height: 160px; overflow: hidden; background: linear-gradient(135deg, #e8f5e9, #c8e6c9); display: flex; align-items: center; justify-content: center; color: var(--color-primary); }
.recipe-cover-img { width: 100%; height: 100%; object-fit: cover; }
.recipe-cover-placeholder svg { opacity: 0.6; }
.recipe-info { padding: 14px 16px; }
.recipe-info h4 { font-size: 16px; font-weight: 700; color: var(--color-title); margin-bottom: 6px; }
.recipe-desc-text { font-size: 13px; color: var(--color-text-secondary); line-height: 1.5; margin-bottom: 8px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.recipe-meta { display: flex; gap: 16px; font-size: 12px; color: var(--color-text-hint); }
.recipe-meta span { display: inline-flex; align-items: center; gap: 4px; }

/* === Trace Big Data === */
.bigdata-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
.bigdata-card {
  text-align: center; padding: 32px 16px; border-radius: var(--radius-md);
  background: linear-gradient(135deg, #1a5d3a 0%, #2d8c3c 100%); color: white;
  box-shadow: var(--shadow-sm); transition: transform 0.3s;
}
.bigdata-card:hover { transform: translateY(-4px); }
.bigdata-num { font-family: var(--font-number); font-size: 42px; font-weight: 700; margin-bottom: 8px; }
.bigdata-label { font-size: 13px; opacity: 0.85; letter-spacing: 0.1em; }

/* === Map Entry === */
.map-entry { display: flex; gap: 20px; }
.map-preview {
  flex: 1; height: 280px; border-radius: var(--radius-md); position: relative; overflow: hidden;
  background: linear-gradient(135deg, #e8f5e9 0%, #f1f8e9 50%, #e0f2f1 100%);
  border: 1px solid var(--color-border-light);
}
.map-china-svg { position: absolute; inset: 0; width: 100%; height: 100%; z-index: 1; }
.map-preview::before {
  content: ''; position: absolute; inset: 0;
  background-image: linear-gradient(rgba(26,93,58,0.06) 1px, transparent 1px),
                    linear-gradient(90deg, rgba(26,93,58,0.06) 1px, transparent 1px);
  background-size: 30px 30px;
}
.map-dot {
  position: absolute; width: 14px; height: 14px; transform: translate(-50%, -50%);
  cursor: pointer; z-index: 2;
}
.map-dot:hover .map-dot-inner { transform: scale(1.4); }
.map-dot-inner {
  width: 12px; height: 12px; border-radius: 50%; background: #1a5d3a;
  border: 2px solid white; box-shadow: 0 0 0 2px rgba(26,93,58,0.3);
  transition: transform 0.2s;
}
.map-dot:hover .map-dot-inner { transform: scale(1.4); }
.map-tooltip {
  position: absolute; bottom: 18px; left: 50%; transform: translateX(-50%);
  background: rgba(26,93,58,0.95); color: white; font-size: 11px;
  padding: 4px 10px; border-radius: var(--radius-sm); white-space: nowrap;
  opacity: 0; pointer-events: none; transition: opacity 0.2s;
}
.map-dot:hover .map-tooltip { opacity: 1; }
.map-farmer-list { width: 240px; display: flex; flex-direction: column; gap: 10px; }
.map-farmer-item {
  display: flex; align-items: center; gap: 10px; padding: 10px 12px;
  border-radius: var(--radius-sm); background: white; box-shadow: var(--shadow-sm);
  transition: box-shadow 0.2s; cursor: pointer;
}
.map-farmer-item:hover { box-shadow: var(--shadow-md); }
.map-farmer-avatar {
  width: 36px; height: 36px; border-radius: 50%; color: white;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; font-weight: 700; flex-shrink: 0;
}
.map-farmer-info h5 { font-size: 13px; font-weight: 700; color: var(--color-title); margin-bottom: 2px; }
.map-farmer-info span { font-size: 11px; color: var(--color-text-hint); }

@media (max-width: 768px) {
  .seasonal-grid, .new-arrivals-grid, .bigdata-grid { grid-template-columns: repeat(2, 1fr); }
  .recipe-grid { grid-template-columns: 1fr; }
  .map-entry { flex-direction: column; }
  .map-farmer-list { width: 100%; }
}

/* === Hot Ranking === */
.hot-ranking { display: flex; flex-direction: column; gap: 0; }
.hot-card { display: flex; align-items: center; gap: 20px; padding: 16px 0; cursor: pointer; border-bottom: 1px solid var(--color-border-light); transition: padding 0.25s; }
.hot-card:hover { padding-left: 8px; }
.hot-card:last-child { border-bottom: none; }
.hot-rank { font-family: var(--font-number); font-size: 28px; font-weight: 700; color: var(--color-border); width: 40px; }
.hot-rank.rank-1 { color: var(--color-danger); }
.hot-rank.rank-2 { color: var(--color-accent); }
.hot-rank.rank-3 { color: var(--color-primary-light); }
.hot-product-img { width: 56px; height: 56px; border-radius: var(--radius-sm); overflow: hidden; flex-shrink: 0; }
.hot-product-img img { width: 100%; height: 100%; object-fit: cover; }
.hot-info { flex: 1; }
.hot-name { font-family: var(--font-heading); font-size: 16px; font-weight: 700; color: var(--color-title); margin-bottom: 4px; }
.hot-meta { display: flex; align-items: center; gap: 12px; }
.hot-sales { font-size: 12px; color: var(--color-text-secondary); }
.hot-origin { font-size: 11px; color: var(--color-text-hint); }
.hot-price { font-family: var(--font-number); font-size: 20px; font-weight: 700; color: var(--color-danger); }

/* === Announcements === */
.announcement-bar { border-top: 1px solid var(--color-border); border-bottom: 1px solid var(--color-border); }
.announce-item { display: flex; align-items: center; gap: 16px; padding: 12px 0; cursor: pointer; border-bottom: 1px solid var(--color-border-light); transition: padding 0.25s; }
.announce-item:last-child { border-bottom: none; }
.announce-item:hover { padding-left: 8px; }
.announce-type { font-size: 11px; font-weight: 600; color: var(--color-primary); border: 1px solid var(--color-primary); padding: 2px 8px; border-radius: var(--radius-sm); flex-shrink: 0; }
.announce-text { flex: 1; font-size: 14px; color: var(--color-text); }
.announce-date { font-size: 12px; color: var(--color-text-hint); }

/* === Process Flow === */
.process-flow { display: flex; align-items: flex-start; justify-content: space-between; }
.process-step { display: flex; flex-direction: column; align-items: center; text-align: center; flex: 1; padding: 0 16px; }
.step-icon { width: 64px; height: 64px; border-radius: 50%; background: var(--color-primary-bg); color: var(--color-primary); display: flex; align-items: center; justify-content: center; margin-bottom: var(--space-md); transition: all 0.3s var(--ease-out); }
.process-step:hover .step-icon { background: var(--color-primary); color: white; transform: scale(1.1); }
.step-name { font-family: var(--font-heading); font-size: 16px; font-weight: 700; color: var(--color-title); margin-bottom: 6px; }
.step-desc { font-size: 13px; color: var(--color-text-secondary); line-height: 1.6; max-width: 160px; }

/* === Farmer Showcase === */
.farmer-showcase { display: grid; grid-template-columns: repeat(2, 1fr); gap: var(--space-md); }
.farmer-card { display: flex; gap: var(--space-md); background: var(--color-card); border-radius: var(--radius-md); padding: var(--space-md); box-shadow: var(--shadow-sm); transition: all 0.3s var(--ease-out); cursor: pointer; }
.farmer-card:hover { box-shadow: var(--shadow-hover); transform: translateY(-2px); }
.farmer-image { position: relative; flex-shrink: 0; }
.farmer-avatar { width: 80px; height: 80px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; font-size: 32px; font-weight: 700; color: white; font-family: var(--font-heading); }
.farmer-cert { position: absolute; bottom: 4px; right: 4px; width: 24px; height: 24px; background: var(--color-primary); border-radius: 50%; display: flex; align-items: center; justify-content: center; border: 2px solid white; }
.farmer-info { flex: 1; }
.farmer-header { display: flex; align-items: center; gap: var(--space-sm); margin-bottom: var(--space-sm); }
.farmer-name { font-family: var(--font-heading); font-size: 16px; font-weight: 700; color: var(--color-title); }
.farmer-product-tag { font-size: 11px; color: var(--color-primary); background: var(--color-primary-bg); padding: 2px 8px; border-radius: var(--radius-sm); }
.farmer-meta { display: flex; flex-direction: column; gap: 6px; margin-bottom: var(--space-sm); }
.meta-item { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--color-text-secondary); }
.meta-item svg { color: var(--color-primary); flex-shrink: 0; }
.farmer-desc { font-size: 13px; color: var(--color-text-secondary); line-height: 1.7; }

/* === Reviews === */
.review-showcase { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-md); }
.review-card { background: var(--color-card); border-radius: var(--radius-md); padding: var(--space-lg); box-shadow: var(--shadow-sm); cursor: pointer; transition: all 0.3s var(--ease-out); }
.review-card:hover { box-shadow: var(--shadow-hover); transform: translateY(-2px); }
.review-header { display: flex; align-items: center; gap: 12px; margin-bottom: var(--space-md); }
.review-avatar { width: 40px; height: 40px; border-radius: 50%; background: var(--color-primary); color: white; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 18px; }
.review-meta { flex: 1; }
.review-user { font-size: 14px; font-weight: 600; color: var(--color-title); margin-bottom: 2px; }
.review-content { font-family: var(--font-heading); font-size: 15px; font-style: italic; color: var(--color-text); line-height: 1.8; margin-bottom: var(--space-sm); }
.review-date { font-size: 11px; color: var(--color-text-hint); }

/* === Guarantee Section === */
.guarantee-section {
  background: linear-gradient(135deg, #0f3d24, #1a5d3a);
  border-radius: var(--radius-lg); padding: var(--space-2xl);
  color: white; position: relative; overflow: hidden;
  margin-top: var(--space-3xl); text-align: center;
}
.guarantee-section::before {
  content: ''; position: absolute; inset: 0;
  background-image:
    radial-gradient(circle at 20% 30%, rgba(255,255,255,0.05) 0%, transparent 40%),
    radial-gradient(circle at 80% 70%, rgba(255,255,255,0.03) 0%, transparent 40%);
}
.guarantee-header { position: relative; z-index: 1; margin-bottom: var(--space-xl); }
.guarantee-title { font-family: var(--font-heading); font-size: 32px; font-weight: 700; color: white; margin-bottom: var(--space-sm); }
.guarantee-subtitle { font-size: 14px; color: rgba(255,255,255,0.8); }
.guarantee-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--space-md); margin-bottom: var(--space-xl); position: relative; z-index: 1; }
.guarantee-card { text-align: center; position: relative; }
.guarantee-number { font-size: 40px; font-weight: 900; color: rgba(255,255,255,0.15); font-family: var(--font-number); }
.guarantee-icon {
  width: 56px; height: 56px; margin: -20px auto var(--space-sm);
  background: rgba(255,255,255,0.1); border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: white; backdrop-filter: blur(8px);
}
.guarantee-item-title { font-family: var(--font-heading); font-size: 18px; font-weight: 700; color: white; margin-bottom: 6px; }
.guarantee-desc { font-size: 13px; color: rgba(255,255,255,0.7); line-height: 1.7; }
.btn-guarantee-cta {
  background: white; color: var(--color-primary-dark); border: none;
  border-radius: var(--radius-full); padding: 14px 36px;
  font-size: 15px; font-weight: 600; cursor: pointer;
  display: inline-flex; align-items: center;
  transition: all 0.25s var(--ease-out);
  position: relative; z-index: 1;
}
.btn-guarantee-cta:hover { transform: translateY(-2px); box-shadow: var(--shadow-lg); }

/* === Back to top === */
.back-top {
  position: fixed; bottom: 40px; right: 40px;
  width: 48px; height: 48px; border-radius: 50%;
  background: var(--color-primary); color: white;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; z-index: 200; box-shadow: var(--shadow-lg);
  transition: all 0.3s var(--ease-out);
}
.back-top:hover { background: var(--color-primary-dark); transform: scale(1.1); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(10px); }

/* === Announcement Dialog === */
.announce-detail { padding: 8px; }
.announce-detail-content { margin-top: 16px; font-size: 14px; color: var(--color-text); line-height: 1.8; }
.announce-detail-time { font-size: 12px; color: var(--color-text-hint); margin-top: 16px; }

/* === Responsive === */
@media (max-width: 768px) {
  .hero-banner { flex-direction: column; }
  .hero-visual { width: 100%; padding: var(--space-md); }
  .hero-content { padding: var(--space-lg); }
  .hero-title { font-size: 28px; }
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .product-grid { grid-template-columns: 1fr; }
  .process-flow { flex-direction: column; gap: var(--space-lg); }
  .guarantee-grid { grid-template-columns: 1fr 1fr; }
  .farmer-showcase { grid-template-columns: 1fr; }
  .review-showcase { grid-template-columns: 1fr; }
}
</style>
