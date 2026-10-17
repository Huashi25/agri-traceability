<template>
  <div class="knowledge-page">
    <!-- Hero Section -->
    <div class="hero-section">
      <div class="hero-content">
        <div class="hero-icon">
          <el-icon size="40"><Reading /></el-icon>
        </div>
        <h1 class="hero-title">农业百科</h1>
        <p class="hero-subtitle">了解农产品知识，做一个聪明的消费者</p>
      </div>
      <div class="hero-decoration">
        <span class="deco-circle deco-1"></span>
        <span class="deco-circle deco-2"></span>
        <span class="deco-circle deco-3"></span>
      </div>
    </div>

    <!-- Category Tabs -->
    <div class="category-bar">
      <div class="category-tabs">
        <div
          v-for="cat in categoryList"
          :key="cat.value || 'all'"
          class="cat-tab"
          :class="{ active: activeCategory === (cat.value || '') }"
          @click="switchCategory(cat.value || '')"
        >
          <el-icon v-if="cat.icon" class="cat-icon"><component :is="cat.icon" /></el-icon>
          {{ cat.label }}
        </div>
      </div>
    </div>

    <!-- Article Grid -->
    <div v-loading="loading" class="article-section">
      <div v-if="articles.length" class="article-grid">
        <div
          v-for="article in articles"
          :key="article.id"
          class="article-card"
          @click="goDetail(article.id)"
        >
          <div class="card-cover">
            <img v-if="article.coverImage" :src="article.coverImage" :alt="article.title" />
            <div v-else class="cover-placeholder">
              <el-icon size="48"><Picture /></el-icon>
              <span>暂无图片</span>
            </div>
            <div class="category-tag">{{ getCategoryLabel(article.category) }}</div>
          </div>
          <div class="card-body">
            <h3 class="card-title">{{ article.title }}</h3>
            <p class="card-summary">{{ article.summary }}</p>
            <div class="card-tags" v-if="getTagList(article.tags).length">
              <span v-for="tag in getTagList(article.tags).slice(0, 3)" :key="tag" class="card-tag">{{ tag }}</span>
            </div>
            <div class="card-footer">
              <span class="view-count">
                <el-icon><View /></el-icon>
                {{ article.viewCount || 0 }} 阅读
              </span>
              <span class="read-link">阅读全文 <el-icon><ArrowRight /></el-icon></span>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="!loading" class="empty-state">
        <el-empty description="暂无百科文章">
          <el-button type="primary" plain @click="switchCategory('')">查看全部</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { knowledgeApi } from '@/api'

const router = useRouter()

const loading = ref(false)
const articles = ref([])
const activeCategory = ref('')

// 分类标签映射
const categoryLabels = {
  SELECTION: '选购指南',
  NUTRITION: '营养价值',
  STORAGE: '储存保鲜',
  USAGE: '食用方法',
  GENERAL: '农业常识'
}

// 分类列表（含"全部"）
const categoryList = ref([
  { value: '', label: '全部' },
  { value: 'SELECTION', label: '选购指南' },
  { value: 'NUTRITION', label: '营养价值' },
  { value: 'STORAGE', label: '储存保鲜' },
  { value: 'USAGE', label: '食用方法' },
  { value: 'GENERAL', label: '农业常识' }
])

const getCategoryLabel = (value) => categoryLabels[value] || '农业百科'

const getTagList = (tags) => {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  return String(tags).split(/[,，、]/).map(t => t.trim()).filter(Boolean)
}

const loadCategories = async () => {
  try {
    const res = await knowledgeApi.categories()
    if (res.data && res.data.length) {
      categoryList.value = [
        { value: '', label: '全部' },
        ...res.data.map((c) => ({ value: c.value, label: c.label }))
      ]
    }
  } catch (e) {
    // 保留默认分类
  }
}

const loadArticles = async () => {
  loading.value = true
  try {
    const res = await knowledgeApi.list(activeCategory.value || undefined)
    articles.value = res.data || []
  } catch (e) {
    ElMessage.error('加载文章列表失败，请稍后重试')
    articles.value = []
  } finally {
    loading.value = false
  }
}

const switchCategory = (value) => {
  activeCategory.value = value
  loadArticles()
}

const goDetail = (id) => {
  router.push(`/knowledge/${id}`)
}

onMounted(() => {
  loadCategories()
  loadArticles()
})
</script>

<style scoped>
.knowledge-page {
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: 40px;
}

/* Hero Section */
.hero-section {
  position: relative;
  background: linear-gradient(135deg, #1a5d3a 0%, #2d8c3c 100%);
  border-radius: 20px;
  padding: 48px 40px;
  margin-bottom: 28px;
  overflow: hidden;
}
.hero-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: #fff;
}
.hero-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 9999px;
  margin-bottom: 16px;
  backdrop-filter: blur(10px);
}
.hero-title {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 10px;
  letter-spacing: 2px;
}
.hero-subtitle {
  font-size: 15px;
  opacity: 0.9;
  letter-spacing: 1px;
}
.hero-decoration {
  position: absolute;
  inset: 0;
  overflow: hidden;
}
.deco-circle {
  position: absolute;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.08);
}
.deco-1 { width: 220px; height: 220px; top: -80px; right: -60px; }
.deco-2 { width: 140px; height: 140px; bottom: -50px; left: 8%; }
.deco-3 { width: 80px; height: 80px; top: 30%; right: 18%; }

/* Category Bar */
.category-bar {
  margin-bottom: 24px;
}
.category-tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.cat-tab {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 9px 22px;
  border-radius: 9999px;
  font-size: 14px;
  color: #666;
  background: #fff;
  border: 1px solid #e8e8e8;
  cursor: pointer;
  transition: all 0.3s ease;
  user-select: none;
}
.cat-tab:hover {
  border-color: #2d8c3c;
  color: #2d8c3c;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.cat-tab.active {
  background: #1a5d3a;
  color: #fff;
  border-color: #1a5d3a;
  box-shadow: 0 4px 12px rgba(26, 93, 58, 0.3);
}
.cat-icon {
  font-size: 15px;
}

/* Article Grid */
.article-section {
  min-height: 300px;
}
.article-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.article-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.35s ease;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
}
.article-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  border-color: #e8f5e9;
}

/* Cover */
.card-cover {
  position: relative;
  height: 200px;
  overflow: hidden;
  background: #f5f5f5;
}
.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}
.article-card:hover .card-cover img {
  transform: scale(1.06);
}
.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  color: #1a5d3a;
}
.cover-placeholder span {
  font-size: 13px;
  opacity: 0.7;
}
.category-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 14px;
  background: rgba(26, 93, 58, 0.9);
  color: #fff;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 500;
  backdrop-filter: blur(4px);
}

/* Card Body */
.card-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  flex: 1;
}
.card-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-summary {
  font-size: 13px;
  color: #888;
  line-height: 1.6;
  margin-bottom: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}
.card-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.card-tag {
  padding: 2px 10px;
  background: #e8f5e9;
  color: #2d8c3c;
  border-radius: 6px;
  font-size: 12px;
}
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 14px;
  border-top: 1px solid #f5f5f5;
}
.view-count {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #aaa;
}
.read-link {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 13px;
  color: #2d8c3c;
  font-weight: 500;
  transition: gap 0.2s ease;
}
.article-card:hover .read-link {
  gap: 6px;
}

/* Empty State */
.empty-state {
  padding: 60px 0;
}

/* Responsive */
@media (max-width: 992px) {
  .article-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 640px) {
  .article-grid {
    grid-template-columns: 1fr;
  }
  .hero-section {
    padding: 32px 20px;
  }
  .hero-title {
    font-size: 26px;
  }
}
</style>
