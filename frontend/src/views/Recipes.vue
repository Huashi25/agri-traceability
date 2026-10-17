<template>
  <div class="recipes-page">
    <!-- Hero Section -->
    <div class="hero-section">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <div class="hero-label">FRESH RECIPES</div>
        <h1 class="hero-title">农产食谱</h1>
        <div class="hero-divider"></div>
        <p class="hero-subtitle">用溯源好食材，做健康好味道</p>
      </div>
    </div>

    <!-- Category Tabs -->
    <div class="category-tabs">
      <div class="cat-tab" :class="{ active: activeCategory === null }" @click="switchCategory(null)">全部</div>
      <div
        v-for="c in categories"
        :key="c.value"
        class="cat-tab"
        :class="{ active: activeCategory === c.value }"
        @click="switchCategory(c.value)"
      >
        {{ c.label }}
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-grid">
      <div v-for="n in 6" :key="n" class="skeleton-card">
        <div class="skeleton-img"></div>
        <div class="skeleton-body">
          <div class="skeleton-line w60"></div>
          <div class="skeleton-line w90"></div>
          <div class="skeleton-line w40"></div>
        </div>
      </div>
    </div>

    <!-- Recipe Grid -->
    <div v-else-if="recipes.length > 0" class="recipe-grid">
      <div
        v-for="r in recipes"
        :key="r.id"
        class="recipe-card"
        @click="goDetail(r.id)"
      >
        <!-- Cover Image -->
        <div class="card-cover">
          <img v-if="r.coverImage" :src="r.coverImage" :alt="r.title" class="cover-img" />
          <div v-else class="cover-placeholder">
            <el-icon size="48"><Food /></el-icon>
            <span>暂无图片</span>
          </div>
          <div class="card-category-tag">{{ getCategoryLabel(r.category) }}</div>
        </div>

        <!-- Card Body -->
        <div class="card-body">
          <h3 class="card-title">{{ r.title }}</h3>
          <p class="card-desc">{{ r.description || '美味食谱，待您探索' }}</p>

          <div class="card-info-row">
            <div class="info-item">
              <el-icon><Clock /></el-icon>
              <span>{{ r.cookTime || 30 }}分钟</span>
            </div>
            <div class="info-item">
              <el-icon><User /></el-icon>
              <span>{{ r.servings || 2 }}人份</span>
            </div>
            <div class="info-item">
              <el-icon><Histogram /></el-icon>
              <span :class="['difficulty-text', difficultyClass(r.difficulty)]">
                {{ difficultyLabel(r.difficulty) }}
              </span>
            </div>
          </div>

          <div class="card-footer">
            <div class="view-count">
              <el-icon><View /></el-icon>
              <span>{{ r.viewCount || 0 }} 浏览</span>
            </div>
            <div class="view-btn">
              查看食谱
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <el-icon size="64" color="#c0c4cc"><Bowl /></el-icon>
      <p class="empty-title">暂无食谱</p>
      <p class="empty-desc">该分类下还没有食谱，敬请期待</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { recipeApi } from '@/api'

const router = useRouter()

const recipes = ref([])
const categories = ref([])
const activeCategory = ref(null)
const loading = ref(true)

const difficultyMap = {
  SIMPLE: { label: '简单', class: 'diff-simple' },
  EASY: { label: '入门', class: 'diff-easy' },
  NORMAL: { label: '普通', class: 'diff-normal' },
  HARD: { label: '挑战', class: 'diff-hard' }
}

const difficultyLabel = (d) => difficultyMap[d]?.label || '普通'
const difficultyClass = (d) => difficultyMap[d]?.class || 'diff-normal'

const getCategoryLabel = (value) => {
  if (!value) return '食谱'
  const c = categories.value.find(item => item.value === value)
  return c ? c.label : '食谱'
}

const switchCategory = async (cat) => {
  activeCategory.value = cat
  await loadRecipes()
}

const loadCategories = async () => {
  try {
    const res = await recipeApi.categories()
    categories.value = res.data
  } catch (e) {
    categories.value = [
      { value: 'DESSERT', label: '甜品' },
      { value: 'STAPLE', label: '主食' },
      { value: 'SOUP', label: '汤品' },
      { value: 'SIDEDISH', label: '小菜' },
      { value: 'JUICE', label: '果汁' }
    ]
  }
}

const loadRecipes = async () => {
  loading.value = true
  try {
    const res = await recipeApi.list(activeCategory.value)
    recipes.value = res.data
  } catch (e) {
    recipes.value = []
  } finally {
    loading.value = false
  }
}

const goDetail = (id) => router.push(`/recipe/${id}`)

onMounted(async () => {
  await loadCategories()
  await loadRecipes()
})
</script>

<style scoped>
.recipes-page { max-width: 1200px; margin: 0 auto; padding-bottom: 40px; }

/* Hero */
.hero-section {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  margin-bottom: 28px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}
.hero-bg {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, #1a5d3a 0%, #2d8c3c 60%, #3da655 100%);
}
.hero-bg::before {
  content: ''; position: absolute; inset: 0;
  background: radial-gradient(circle at 20% 80%, rgba(255,255,255,0.12) 0%, transparent 50%),
              radial-gradient(circle at 80% 20%, rgba(255,255,255,0.08) 0%, transparent 40%);
}
.hero-content { position: relative; z-index: 1; padding: 48px 40px; text-align: center; color: #fff; }
.hero-label { font-size: 12px; letter-spacing: 3px; opacity: 0.7; font-weight: 600; margin-bottom: 8px; }
.hero-title { font-size: 36px; font-weight: 800; margin-bottom: 12px; letter-spacing: 2px; }
.hero-divider { width: 60px; height: 3px; background: rgba(255,255,255,0.5); border-radius: 9999px; margin: 0 auto 16px; }
.hero-subtitle { font-size: 16px; opacity: 0.9; }

/* Category Tabs */
.category-tabs { display: flex; gap: 10px; margin-bottom: 28px; flex-wrap: wrap; }
.cat-tab {
  padding: 10px 22px; border-radius: 9999px; font-size: 14px; font-weight: 500;
  color: #666; background: #fff; border: 1px solid #e8e8e8; cursor: pointer;
  transition: all 0.3s ease; box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.cat-tab:hover { border-color: #2d8c3c; color: #2d8c3c; transform: translateY(-1px); }
.cat-tab.active { background: #1a5d3a; color: #fff; border-color: #1a5d3a; box-shadow: 0 4px 14px rgba(26,93,58,0.3); }

/* Recipe Grid */
.recipe-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px;
}
@media (max-width: 992px) { .recipe-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 600px) { .recipe-grid { grid-template-columns: 1fr; } }

.recipe-card {
  background: #fff; border-radius: 12px; overflow: hidden; cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06); transition: all 0.35s ease;
  border: 1px solid #f0f0f0;
}
.recipe-card:hover {
  transform: translateY(-6px); box-shadow: 0 8px 30px rgba(0,0,0,0.12);
  border-color: #e8f5e9;
}

/* Cover */
.card-cover { position: relative; height: 200px; overflow: hidden; background: #f5f5f5; }
.cover-img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.4s ease; }
.recipe-card:hover .cover-img { transform: scale(1.08); }
.cover-placeholder {
  width: 100%; height: 100%;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px;
  background: linear-gradient(135deg, #e8f5e9, #c8e6c9); color: #1a5d3a;
}
.cover-placeholder span { font-size: 13px; opacity: 0.7; }
.card-category-tag {
  position: absolute; top: 12px; left: 12px;
  padding: 5px 14px; background: rgba(26,93,58,0.9); color: #fff;
  border-radius: 9999px; font-size: 12px; font-weight: 600; backdrop-filter: blur(8px);
}

/* Card Body */
.card-body { padding: 18px 20px 20px; }
.card-title {
  font-size: 17px; font-weight: 700; color: #1a5d3a; margin-bottom: 8px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.card-desc {
  font-size: 13px; color: #999; line-height: 1.6; margin-bottom: 16px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden; min-height: 42px;
}

.card-info-row { display: flex; align-items: center; gap: 16px; margin-bottom: 14px; padding-bottom: 14px; border-bottom: 1px solid #f5f5f5; }
.info-item { display: flex; align-items: center; gap: 5px; font-size: 13px; color: #666; }
.info-item .el-icon { font-size: 15px; color: #2d8c3c; }

.difficulty-text { font-weight: 600; }
.diff-simple { color: #67c23a; }
.diff-easy { color: #409eff; }
.diff-normal { color: #e6a23c; }
.diff-hard { color: #f56c6c; }

.card-footer { display: flex; align-items: center; justify-content: space-between; }
.view-count { display: flex; align-items: center; gap: 5px; font-size: 12px; color: #bbb; }
.view-count .el-icon { font-size: 14px; }
.view-btn {
  display: flex; align-items: center; gap: 4px;
  font-size: 13px; font-weight: 600; color: #2d8c3c; transition: gap 0.3s ease;
}
.recipe-card:hover .view-btn { gap: 8px; }

/* Loading Skeleton */
.loading-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px; }
@media (max-width: 992px) { .loading-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 600px) { .loading-grid { grid-template-columns: 1fr; } }
.skeleton-card { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.skeleton-img { height: 200px; background: linear-gradient(90deg, #f5f5f5 25%, #ececec 50%, #f5f5f5 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; }
.skeleton-body { padding: 18px 20px; }
.skeleton-line { height: 14px; border-radius: 6px; margin-bottom: 10px; background: linear-gradient(90deg, #f5f5f5 25%, #ececec 50%, #f5f5f5 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; }
.skeleton-line.w60 { width: 60%; } .skeleton-line.w90 { width: 90%; } .skeleton-line.w40 { width: 40%; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

/* Empty State */
.empty-state { text-align: center; padding: 80px 20px; }
.empty-state .el-icon { margin-bottom: 16px; }
.empty-title { font-size: 18px; font-weight: 600; color: #666; margin-bottom: 8px; }
.empty-desc { font-size: 14px; color: #bbb; }
</style>
