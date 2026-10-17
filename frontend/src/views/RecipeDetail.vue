<template>
  <div class="recipe-detail-page" v-loading="loading">
    <!-- Back Button -->
    <div class="back-bar">
      <div class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回食谱列表</span>
      </div>
    </div>

    <template v-if="recipe">
      <!-- Recipe Header -->
      <div class="recipe-header">
        <div class="header-cover">
          <img v-if="recipe.coverImage" :src="recipe.coverImage" :alt="recipe.title" class="cover-img" />
          <div v-else class="cover-placeholder">
            <el-icon size="60"><Food /></el-icon>
            <span>暂无图片</span>
          </div>
        </div>
        <div class="header-info">
          <div class="header-category-badge" v-if="recipe.category">
            {{ getCategoryLabel(recipe.category) }}
          </div>
          <h1 class="header-title">{{ recipe.title }}</h1>
          <p class="header-desc">{{ recipe.description || '美味食谱，用心烹饪每一道菜' }}</p>
          <div class="header-view-count">
            <el-icon><View /></el-icon>
            <span>{{ recipe.viewCount || 0 }} 人浏览</span>
          </div>
        </div>
      </div>

      <!-- Recipe Meta Cards -->
      <div class="meta-cards">
        <div class="meta-card">
          <div class="meta-icon cook-time">
            <el-icon><Timer /></el-icon>
          </div>
          <div class="meta-text">
            <div class="meta-label">烹饪时间</div>
            <div class="meta-value">{{ recipe.cookTime || 30 }} 分钟</div>
          </div>
        </div>
        <div class="meta-card">
          <div class="meta-icon servings">
            <el-icon><User /></el-icon>
          </div>
          <div class="meta-text">
            <div class="meta-label">适合人数</div>
            <div class="meta-value">{{ recipe.servings || 2 }} 人份</div>
          </div>
        </div>
        <div class="meta-card">
          <div class="meta-icon difficulty">
            <el-icon><Histogram /></el-icon>
          </div>
          <div class="meta-text">
            <div class="meta-label">烹饪难度</div>
            <div class="meta-value">{{ difficultyLabel(recipe.difficulty) }}</div>
          </div>
        </div>
      </div>

      <!-- Content Layout -->
      <div class="content-layout">
        <!-- Ingredients Section -->
        <div class="section ingredients-section">
          <div class="section-header">
            <div class="section-icon"><el-icon><Goods /></el-icon></div>
            <h2 class="section-title">所需食材</h2>
          </div>
          <div class="ingredients-list" v-if="ingredients.length > 0">
            <div
              v-for="(ing, idx) in ingredients"
              :key="idx"
              class="ingredient-item"
              @click="goProduct(ing.product?.id)"
            >
              <div class="ingredient-img">
                <img v-if="ing.product?.image" :src="ing.product.image" :alt="ing.product.name" />
                <el-icon v-else size="24"><Picture /></el-icon>
              </div>
              <div class="ingredient-info">
                <div class="ingredient-name-row">
                  <span class="ingredient-name">{{ ing.product?.name || '食材' }}</span>
                  <span v-if="ing.isMain === 1" class="main-badge">主料</span>
                </div>
                <div class="ingredient-meta">
                  <span class="ingredient-amount">{{ ing.amount }}</span>
                  <span v-if="ing.product?.origin" class="ingredient-origin">
                    <el-icon size="12"><LocationFilled /></el-icon>
                    {{ ing.product.origin }}
                  </span>
                </div>
              </div>
              <div class="ingredient-arrow">
                <el-icon><ArrowRight /></el-icon>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无食材信息" :image-size="60" />
        </div>

        <!-- Steps Section -->
        <div class="section steps-section">
          <div class="section-header">
            <div class="section-icon"><el-icon><List /></el-icon></div>
            <h2 class="section-title">烹饪步骤</h2>
          </div>
          <div class="steps-list" v-if="steps.length > 0">
            <div v-for="(step, idx) in steps" :key="idx" class="step-card">
              <div class="step-number-circle">{{ step.step || idx + 1 }}</div>
              <div class="step-content">
                <div class="step-label">步骤 {{ step.step || idx + 1 }}</div>
                <p class="step-desc">{{ step.desc }}</p>
              </div>
              <div class="step-connector" v-if="idx < steps.length - 1"></div>
            </div>
          </div>
          <el-empty v-else description="暂无步骤信息" :image-size="60" />
        </div>
      </div>
    </template>

    <!-- Error State -->
    <div v-else-if="!loading" class="error-state">
      <el-icon size="64" color="#c0c4cc"><WarningFilled /></el-icon>
      <p class="error-title">食谱未找到</p>
      <p class="error-desc">该食谱可能已被删除或不存在</p>
      <div class="error-btn" @click="goBack">返回食谱列表</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { recipeApi } from '@/api'

const router = useRouter()
const route = useRoute()

const recipe = ref(null)
const ingredients = ref([])
const loading = ref(true)

const difficultyMap = {
  SIMPLE: '简单',
  EASY: '入门',
  NORMAL: '普通',
  HARD: '挑战'
}

const categoryMap = {
  DESSERT: '甜品',
  STAPLE: '主食',
  SOUP: '汤品',
  SIDEDISH: '小菜',
  JUICE: '果汁'
}

const difficultyLabel = (d) => difficultyMap[d] || '普通'
const getCategoryLabel = (c) => categoryMap[c] || '食谱'

const steps = computed(() => {
  if (!recipe.value?.steps) return []
  try {
    const parsed = typeof recipe.value.steps === 'string'
      ? JSON.parse(recipe.value.steps)
      : recipe.value.steps
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    return []
  }
})

const loadRecipe = async () => {
  loading.value = true
  try {
    const id = route.params.id
    const res = await recipeApi.getById(id)
    const data = res.data
    recipe.value = data.recipe || data
    ingredients.value = data.ingredients || []
  } catch (e) {
    recipe.value = null
    ingredients.value = []
  } finally {
    loading.value = false
  }
}

const goBack = () => router.push('/recipes')
const goProduct = (id) => {
  if (id) router.push(`/product/${id}`)
}

onMounted(loadRecipe)
</script>

<style scoped>
.recipe-detail-page { max-width: 1000px; margin: 0 auto; padding-bottom: 40px; }

/* Back Bar */
.back-bar { margin-bottom: 20px; }
.back-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 16px; border-radius: 9999px; font-size: 14px; font-weight: 500;
  color: #1a5d3a; background: #e8f5e9; cursor: pointer;
  transition: all 0.3s ease;
}
.back-btn:hover { background: #1a5d3a; color: #fff; box-shadow: 0 4px 14px rgba(26,93,58,0.25); }
.back-btn .el-icon { font-size: 16px; }

/* Recipe Header */
.recipe-header {
  display: flex; gap: 32px; margin-bottom: 28px;
  background: #fff; border-radius: 20px; overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.header-cover { width: 380px; height: 280px; flex-shrink: 0; overflow: hidden; background: #f5f5f5; }
.cover-img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder {
  width: 100%; height: 100%; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 12px;
  background: linear-gradient(135deg, #e8f5e9, #c8e6c9); color: #1a5d3a;
}
.cover-placeholder span { font-size: 14px; opacity: 0.7; }

.header-info { flex: 1; padding: 32px 32px 32px 0; display: flex; flex-direction: column; justify-content: center; }
.header-category-badge {
  display: inline-block; align-self: flex-start;
  padding: 5px 16px; background: #e8f5e9; color: #1a5d3a;
  border-radius: 9999px; font-size: 13px; font-weight: 600; margin-bottom: 14px;
}
.header-title { font-size: 30px; font-weight: 800; color: #1a5d3a; margin-bottom: 12px; line-height: 1.3; }
.header-desc { font-size: 15px; color: #888; line-height: 1.7; margin-bottom: 18px; }
.header-view-count { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #bbb; }
.header-view-count .el-icon { font-size: 16px; }

/* Meta Cards */
.meta-cards { display: flex; gap: 16px; margin-bottom: 32px; }
.meta-card {
  flex: 1; display: flex; align-items: center; gap: 16px;
  background: #fff; border-radius: 12px; padding: 22px 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06); transition: all 0.3s ease;
  border: 1px solid #f5f5f5;
}
.meta-card:hover { transform: translateY(-3px); box-shadow: 0 8px 20px rgba(0,0,0,0.08); border-color: #e8f5e9; }
.meta-icon {
  width: 52px; height: 52px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; flex-shrink: 0;
}
.meta-icon.cook-time { background: #e8f5e9; color: #2d8c3c; }
.meta-icon.servings { background: #fff3e0; color: #e6a23c; }
.meta-icon.difficulty { background: #ede7f6; color: #722ed1; }
.meta-label { font-size: 12px; color: #999; margin-bottom: 4px; }
.meta-value { font-size: 18px; font-weight: 700; color: #333; }

/* Content Layout */
.content-layout { display: flex; flex-direction: column; gap: 28px; }

.section { background: #fff; border-radius: 20px; padding: 28px 30px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.section-header { display: flex; align-items: center; gap: 10px; margin-bottom: 24px; }
.section-icon {
  width: 36px; height: 36px; border-radius: 12px; background: #1a5d3a; color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 18px;
}
.section-title { font-size: 20px; font-weight: 700; color: #1a5d3a; }

/* Ingredients */
.ingredients-list { display: flex; flex-direction: column; gap: 12px; }
.ingredient-item {
  display: flex; align-items: center; gap: 16px;
  padding: 14px 16px; border-radius: 12px; background: #fafafa;
  cursor: pointer; transition: all 0.3s ease; border: 1px solid transparent;
}
.ingredient-item:hover { background: #e8f5e9; border-color: #c8e6c9; transform: translateX(4px); }
.ingredient-img {
  width: 48px; height: 48px; border-radius: 12px; overflow: hidden;
  background: #f0f0f0; display: flex; align-items: center; justify-content: center;
  color: #ccc; flex-shrink: 0;
}
.ingredient-img img { width: 100%; height: 100%; object-fit: cover; }
.ingredient-info { flex: 1; min-width: 0; }
.ingredient-name-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.ingredient-name { font-size: 15px; font-weight: 600; color: #333; }
.main-badge {
  padding: 2px 8px; background: #1a5d3a; color: #fff;
  border-radius: 9999px; font-size: 11px; font-weight: 600;
}
.ingredient-meta { display: flex; align-items: center; gap: 14px; font-size: 13px; color: #999; }
.ingredient-amount { font-weight: 500; color: #2d8c3c; }
.ingredient-origin { display: flex; align-items: center; gap: 3px; }
.ingredient-arrow { color: #ccc; flex-shrink: 0; transition: color 0.3s ease; }
.ingredient-item:hover .ingredient-arrow { color: #2d8c3c; }

/* Steps */
.steps-list { display: flex; flex-direction: column; gap: 8px; }
.step-card {
  display: flex; align-items: flex-start; gap: 20px;
  padding: 20px 22px; border-radius: 12px; background: #fafafa;
  transition: all 0.3s ease; position: relative; border: 1px solid transparent;
}
.step-card:hover { background: #e8f5e9; border-color: #c8e6c9; }
.step-number-circle {
  width: 40px; height: 40px; border-radius: 9999px;
  background: linear-gradient(135deg, #1a5d3a, #2d8c3c); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 800; flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(26,93,58,0.3);
}
.step-content { flex: 1; }
.step-label { font-size: 13px; font-weight: 600; color: #2d8c3c; margin-bottom: 6px; }
.step-desc { font-size: 15px; color: #555; line-height: 1.7; }

/* Error State */
.error-state { text-align: center; padding: 80px 20px; }
.error-state .el-icon { margin-bottom: 16px; }
.error-title { font-size: 20px; font-weight: 700; color: #666; margin-bottom: 8px; }
.error-desc { font-size: 14px; color: #bbb; margin-bottom: 24px; }
.error-btn {
  display: inline-block; padding: 10px 28px; border-radius: 9999px;
  background: #1a5d3a; color: #fff; font-size: 14px; font-weight: 600;
  cursor: pointer; transition: all 0.3s ease;
}
.error-btn:hover { background: #2d8c3c; box-shadow: 0 4px 14px rgba(26,93,58,0.3); transform: translateY(-2px); }

/* Responsive */
@media (max-width: 768px) {
  .recipe-header { flex-direction: column; }
  .header-cover { width: 100%; height: 220px; }
  .header-info { padding: 24px; }
  .meta-cards { flex-direction: column; }
}
</style>
