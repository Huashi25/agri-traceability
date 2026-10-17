<template>
  <div class="seasonal-page">
    <!-- Hero -->
    <section class="hero-banner section-anim">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <span class="hero-badge">
          <el-icon><Calendar /></el-icon>
          顺时而食 · 应季而食
        </span>
        <h1 class="hero-title">{{ selectedMonth }}月时令</h1>
        <p class="hero-subtitle">{{ seasonTip }} · 当月精选 {{ seasonalList.length }} 款时令农产品，从产地直达餐桌</p>
        <div class="hero-meta" v-if="selectedMonth === currentMonth">
          <span class="now-tag"><el-icon><Sunny /></el-icon> 当前月份</span>
        </div>
      </div>
    </section>

    <!-- Month selector -->
    <div class="month-selector">
      <div class="month-tabs">
        <div
          v-for="m in 12"
          :key="m"
          class="month-tab"
          :class="{ active: selectedMonth === m, current: m === currentMonth }"
          @click="selectMonth(m)"
        >
          <span class="m-num">{{ m }}月</span>
          <span class="m-dot" v-if="m === currentMonth"></span>
        </div>
      </div>
    </div>

    <!-- Product grid -->
    <div class="seasonal-content" v-loading="loading">
      <div class="result-bar">
        <span class="result-text">
          <el-icon><Goods /></el-icon>
          共 {{ seasonalList.length }} 款时令产品
        </span>
        <span class="result-hint" v-if="seasonalList.length">点击卡片查看商品详情</span>
      </div>

      <div class="product-grid" v-if="seasonalList.length">
        <div
          v-for="(item, idx) in seasonalList"
          :key="item.product?.id || idx"
          class="product-card"
          :style="{ '--i': idx }"
          @click="goDetail(item.product?.id)"
        >
          <div class="card-image">
            <img
              v-if="item.product?.image || item.product?.imageUrl"
              :src="item.product?.image || item.product?.imageUrl"
              :alt="item.product?.name"
            />
            <div v-else class="img-placeholder">
              <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.6">
                <path d="M12 2C7 7 5 11 5 14a7 7 0 0 0 14 0c0-3-2-7-7-12z" />
                <path d="M12 22V8" />
              </svg>
              <span class="ph-text">{{ item.product?.name?.charAt(0) || '鲜' }}</span>
            </div>
            <div class="peak-badge" v-if="item.isPeak === 1">
              <el-icon><Star /></el-icon> 正当时
            </div>
          </div>
          <div class="card-body">
            <h3 class="card-title">{{ item.product?.name }}</h3>
            <div class="card-origin">
              <el-icon><Location /></el-icon>
              <span>{{ item.product?.origin || '产地直供' }}</span>
            </div>
            <p class="card-desc" :title="item.seasonalDesc">{{ item.seasonalDesc || '当季时令好食材' }}</p>
            <div class="card-footer">
              <div class="price-box">
                <span class="price-symbol">￥</span>
                <span class="price-num">{{ item.product?.price ?? '--' }}</span>
                <span class="price-unit" v-if="item.product?.unit">/{{ item.product.unit }}</span>
              </div>
              <el-button type="primary" plain circle size="small" @click.stop="goDetail(item.product?.id)" aria-label="查看详情">
                <el-icon><Right /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else-if="!loading" class="empty-state">
        <el-empty description="本月暂无时令产品">
          <template #image>
            <div class="empty-icon">
              <el-icon size="48"><Cherry /></el-icon>
            </div>
          </template>
          <el-button type="primary" plain @click="selectMonth(currentMonth)">查看本月时令</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { seasonalApi } from '@/api'

const router = useRouter()

const currentMonth = new Date().getMonth() + 1
const selectedMonth = ref(currentMonth)
const seasonalList = ref([])
const loading = ref(false)

const seasonMap = {
  春: [3, 4, 5],
  夏: [6, 7, 8],
  秋: [9, 10, 11],
  冬: [12, 1, 2]
}
const seasonTips = {
  春: '春暖花开，嫩芽初生，正是尝鲜好时节',
  夏: '夏日炎炎，瓜果飘香，清凉解暑正当时',
  秋: '秋高气爽，五谷丰登，丰收季的醇厚滋味',
  冬: '寒冬腊月，根茎蓄力，温补养身的应季之选'
}

const currentSeason = computed(() => {
  for (const [name, months] of Object.entries(seasonMap)) {
    if (months.includes(selectedMonth.value)) return name
  }
  return '春'
})

const seasonTip = computed(() => seasonTips[currentSeason.value])

const loadCurrent = async () => {
  loading.value = true
  try {
    const res = await seasonalApi.current()
    seasonalList.value = res.data || []
  } catch (e) {
    seasonalList.value = []
  } finally {
    loading.value = false
  }
}

const loadByMonth = async (month) => {
  loading.value = true
  try {
    const res = await seasonalApi.byMonth(month)
    seasonalList.value = res.data || []
  } catch (e) {
    seasonalList.value = []
  } finally {
    loading.value = false
  }
}

const selectMonth = (month) => {
  if (month === selectedMonth.value && seasonalList.value.length) return
  selectedMonth.value = month
  if (month === currentMonth) {
    loadCurrent()
  } else {
    loadByMonth(month)
  }
}

const goDetail = (id) => {
  if (!id) return
  router.push(`/product/${id}`)
}

onMounted(() => {
  loadCurrent()
})
</script>

<style scoped>
.seasonal-page { max-width: 1200px; margin: 0 auto; }

/* Hero */
.hero-banner {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  margin-bottom: 24px;
  min-height: 220px;
  display: flex;
  align-items: center;
}
.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1a5d3a 0%, #2d8c3c 55%, #46a85a 100%);
}
.hero-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 85% 30%, rgba(255,255,255,0.16), transparent 45%),
    radial-gradient(circle at 15% 80%, rgba(232,245,233,0.18), transparent 50%);
}
.hero-content {
  position: relative;
  z-index: 1;
  padding: 40px 44px;
  color: #fff;
  width: 100%;
}
.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  background: rgba(255,255,255,0.16);
  border: 1px solid rgba(255,255,255,0.28);
  border-radius: 9999px;
  font-size: 13px;
  backdrop-filter: blur(8px);
  margin-bottom: 16px;
}
.hero-title {
  font-size: 40px;
  font-weight: 800;
  margin-bottom: 10px;
  letter-spacing: 1px;
}
.hero-subtitle {
  font-size: 15px;
  opacity: 0.9;
  line-height: 1.6;
}
.hero-meta { margin-top: 16px; }
.now-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 12px;
  background: rgba(255,255,255,0.22);
  border-radius: 9999px;
  font-size: 12px;
}

/* Month selector */
.month-selector {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  margin-bottom: 20px;
}
.month-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 2px;
}
.month-tabs::-webkit-scrollbar { height: 4px; }
.month-tabs::-webkit-scrollbar-thumb { background: #c8e6c9; border-radius: 9999px; }
.month-tab {
  position: relative;
  flex: 1 0 auto;
  min-width: 64px;
  text-align: center;
  padding: 10px 4px;
  border-radius: 12px;
  font-size: 14px;
  color: #666;
  background: #f7faf7;
  border: 1px solid #eef2ee;
  cursor: pointer;
  transition: all 0.28s ease;
}
.month-tab:hover {
  border-color: #2d8c3c;
  color: #2d8c3c;
  transform: translateY(-2px);
}
.month-tab.active {
  background: linear-gradient(135deg, #1a5d3a, #2d8c3c);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 6px 16px rgba(45,140,60,0.32);
}
.m-num { font-weight: 600; }
.m-dot {
  display: block;
  width: 5px;
  height: 5px;
  background: #ffd54f;
  border-radius: 9999px;
  margin: 4px auto 0;
}
.month-tab.active .m-dot { background: #fff; }

/* Content */
.seasonal-content { min-height: 320px; }
.result-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f3f0;
}
.result-text {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #333;
  font-weight: 600;
}
.result-text .el-icon { color: #2d8c3c; }
.result-hint { font-size: 13px; color: #aaa; }

/* Product grid */
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.product-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #f0f3f0;
  transition: all 0.3s ease;
  animation: cardIn 0.5s ease both;
  animation-delay: calc(var(--i, 0) * 0.06s);
}
@keyframes cardIn {
  from { opacity: 0; transform: translateY(18px); }
  to { opacity: 1; transform: translateY(0); }
}
.product-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 8px 30px rgba(0,0,0,0.12);
  border-color: #c8e6c9;
}
.card-image {
  position: relative;
  height: 170px;
  overflow: hidden;
  background: linear-gradient(135deg, #e8f5e9, #d7ecd9);
}
.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}
.product-card:hover .card-image img { transform: scale(1.06); }
.img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #2d8c3c;
}
.ph-text {
  font-size: 22px;
  font-weight: 700;
  color: #1a5d3a;
  opacity: 0.55;
}
.peak-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: linear-gradient(135deg, #ff9800, #f57c00);
  color: #fff;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(245,124,0,0.35);
}
.card-body { padding: 14px 16px 16px; }
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-origin {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #2d8c3c;
  margin-bottom: 8px;
}
.card-origin .el-icon { font-size: 13px; }
.card-desc {
  font-size: 13px;
  color: #999;
  line-height: 1.5;
  margin-bottom: 14px;
  height: 38px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.price-box { display: flex; align-items: baseline; }
.price-symbol { font-size: 13px; color: #f56c6c; }
.price-num { font-size: 22px; font-weight: 700; color: #f56c6c; }
.price-unit { font-size: 12px; color: #bbb; margin-left: 2px; }

/* Empty */
.empty-state {
  padding: 60px 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.empty-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 16px;
  border-radius: 20px;
  background: linear-gradient(135deg, #e8f5e9, #d7ecd9);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2d8c3c;
}

.section-anim { animation: fadeInUp 0.6s ease both; }
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 992px) {
  .product-grid { grid-template-columns: repeat(2, 1fr); }
  .hero-title { font-size: 30px; }
}
@media (max-width: 576px) {
  .product-grid { grid-template-columns: 1fr; }
  .hero-content { padding: 28px 24px; }
  .hero-title { font-size: 26px; }
  .month-tab { min-width: 52px; }
}
</style>
