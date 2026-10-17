<template>
  <div class="knowledge-detail-page" v-loading="loading">
    <!-- Back Button -->
    <div class="back-bar">
      <el-button class="back-btn" text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回百科列表
      </el-button>
    </div>

    <template v-if="article">
      <!-- Article Card -->
      <div class="article-container">
        <!-- Header -->
        <div class="article-header">
          <div class="category-badge">{{ getCategoryLabel(article.category) }}</div>
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="article-meta">
            <span class="meta-item">
              <el-icon><View /></el-icon>
              {{ article.viewCount || 0 }} 阅读
            </span>
            <span class="meta-divider">|</span>
            <span class="meta-item">
              <el-icon><Clock /></el-icon>
              {{ formatTime(article.createTime) }}
            </span>
          </div>
        </div>

        <!-- Cover Image -->
        <div v-if="article.coverImage" class="article-cover">
          <img :src="article.coverImage" :alt="article.title" />
        </div>

        <!-- Summary -->
        <div v-if="article.summary" class="article-summary">
          <el-icon class="summary-icon"><InfoFilled /></el-icon>
          <span>{{ article.summary }}</span>
        </div>

        <!-- Content -->
        <div class="article-content" v-html="renderedContent"></div>

        <!-- Tags -->
        <div v-if="tagList.length" class="article-tags">
          <span class="tags-label">
            <el-icon><PriceTag /></el-icon>
            标签：
          </span>
          <span v-for="tag in tagList" :key="tag" class="article-tag">{{ tag }}</span>
        </div>

        <!-- Bottom Actions -->
        <div class="article-actions">
          <el-button @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回列表
          </el-button>
          <el-button type="primary" plain @click="scrollTop">
            <el-icon><Top /></el-icon>
            回到顶部
          </el-button>
        </div>
      </div>

      <!-- Related Articles -->
      <div v-if="relatedArticles.length" class="related-section">
        <div class="section-header">
          <span class="section-bar"></span>
          <h2 class="section-title">相关推荐</h2>
          <span class="section-hint">同分类下的其他文章</span>
        </div>
        <div class="related-grid">
          <div
            v-for="item in relatedArticles"
            :key="item.id"
            class="related-card"
            @click="goDetail(item.id)"
          >
            <div class="related-cover">
              <img v-if="item.coverImage" :src="item.coverImage" :alt="item.title" />
              <div v-else class="related-placeholder">
                <el-icon size="28"><Picture /></el-icon>
              </div>
            </div>
            <div class="related-body">
              <h4 class="related-title">{{ item.title }}</h4>
              <span class="related-views">
                <el-icon><View /></el-icon>
                {{ item.viewCount || 0 }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Not Found State -->
    <div v-else-if="!loading" class="not-found">
      <el-empty description="文章不存在或已删除">
        <el-button type="primary" @click="goBack">返回百科列表</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { knowledgeApi } from '@/api'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const article = ref(null)
const allArticles = ref([])

const categoryLabels = {
  SELECTION: '选购指南',
  NUTRITION: '营养价值',
  STORAGE: '储存保鲜',
  USAGE: '食用方法',
  GENERAL: '农业常识'
}

const getCategoryLabel = (value) => categoryLabels[value] || '农业百科'

const tagList = computed(() => {
  if (!article.value?.tags) return []
  if (Array.isArray(article.value.tags)) return article.value.tags
  return String(article.value.tags).split(/[,，、]/).map(t => t.trim()).filter(Boolean)
})

const formatTime = (time) => {
  if (!time) return '未知时间'
  const d = new Date(time)
  if (isNaN(d.getTime())) return time
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/**
 * 简易 Markdown 转 HTML
 * 支持: ## 标题, - 列表, 段落, **加粗**
 */
const renderedContent = computed(() => {
  if (!article.value || !article.value.content) return '<p class="empty-content">暂无内容</p>'
  return parseMarkdown(article.value.content)
})

const parseMarkdown = (text) => {
  if (!text) return ''
  // 转义 HTML 特殊字符防止 XSS
  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // 按行分割处理
  const lines = html.split('\n')
  const result = []
  let inList = false
  let paragraphs = []

  const flushParagraphs = () => {
    if (paragraphs.length) {
      result.push(`<p>${paragraphs.join('<br>')}</p>`)
      paragraphs = []
    }
  }
  const closeList = () => {
    if (inList) {
      result.push('</ul>')
      inList = false
    }
  }

  for (const line of lines) {
    const trimmed = line.trim()

    if (!trimmed) {
      // 空行：结束当前段落/列表
      flushParagraphs()
      closeList()
      continue
    }

    // ## 标题 → h3
    if (/^##\s+/.test(trimmed)) {
      flushParagraphs()
      closeList()
      const heading = trimmed.replace(/^##\s+/, '')
      result.push(`<h3>${heading}</h3>`)
      continue
    }

    // ### 标题 → h4
    if (/^###\s+/.test(trimmed)) {
      flushParagraphs()
      closeList()
      const heading = trimmed.replace(/^###\s+/, '')
      result.push(`<h4>${heading}</h4>`)
      continue
    }

    // - 或 * 列表项
    if (/^[-*]\s+/.test(trimmed)) {
      flushParagraphs()
      if (!inList) {
        result.push('<ul>')
        inList = true
      }
      const item = trimmed.replace(/^[-*]\s+/, '')
      result.push(`<li>${item}</li>`)
      continue
    }

    // 普通文本行：先处理 **加粗**
    const formatted = trimmed.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    paragraphs.push(formatted)
  }

  flushParagraphs()
  closeList()

  return result.join('\n')
}

// 相关文章（同分类，排除当前）
const relatedArticles = computed(() => {
  if (!article.value) return []
  return allArticles.value
    .filter((a) => a.id !== article.value.id && a.category === article.value.category)
    .slice(0, 4)
})

const loadArticle = async (id) => {
  loading.value = true
  article.value = null
  try {
    const res = await knowledgeApi.getById(id)
    article.value = res.data
    if (article.value) {
      // 加载同分类相关文章
      loadRelated(article.value.category)
    }
  } catch (e) {
    ElMessage.error('加载文章详情失败')
  } finally {
    loading.value = false
  }
}

const loadRelated = async (category) => {
  try {
    const res = await knowledgeApi.list(category)
    allArticles.value = res.data || []
  } catch (e) {
    allArticles.value = []
  }
}

const goBack = () => {
  router.push('/knowledge')
}

const goDetail = (id) => {
  router.push(`/knowledge/${id}`)
  // 切换文章后滚动到顶部
  nextTick(() => scrollTop())
}

const scrollTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 监听路由参数变化（相关文章跳转）
watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      loadArticle(newId)
      scrollTop()
    }
  }
)

onMounted(() => {
  const id = route.params.id
  if (id) {
    loadArticle(id)
  }
})
</script>

<style scoped>
.knowledge-detail-page {
  max-width: 860px;
  margin: 0 auto;
  padding-bottom: 60px;
}

/* Back Bar */
.back-bar {
  margin-bottom: 20px;
}
.back-btn {
  color: #2d8c3c;
  font-size: 14px;
  font-weight: 500;
  padding: 8px 4px;
}
.back-btn:hover {
  color: #1a5d3a;
}

/* Article Container */
.article-container {
  background: #fff;
  border-radius: 20px;
  padding: 40px 48px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-bottom: 32px;
}

/* Header */
.article-header {
  margin-bottom: 28px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}
.category-badge {
  display: inline-block;
  padding: 5px 16px;
  background: #e8f5e9;
  color: #1a5d3a;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 16px;
}
.article-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a5d3a;
  line-height: 1.4;
  margin-bottom: 16px;
}
.article-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #999;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
}
.meta-divider {
  color: #ddd;
}

/* Cover Image */
.article-cover {
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 28px;
  max-height: 400px;
}
.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* Summary */
.article-summary {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 16px 20px;
  background: #e8f5e9;
  border-radius: 12px;
  margin-bottom: 32px;
  font-size: 14px;
  color: #1a5d3a;
  line-height: 1.7;
  border-left: 4px solid #2d8c3c;
}
.summary-icon {
  color: #2d8c3c;
  font-size: 18px;
  flex-shrink: 0;
  margin-top: 2px;
}

/* Content */
.article-content {
  font-size: 15px;
  line-height: 1.9;
  color: #444;
  margin-bottom: 36px;
}
.article-content :deep(h3) {
  font-size: 20px;
  font-weight: 600;
  color: #1a5d3a;
  margin: 28px 0 14px;
  padding-left: 12px;
  border-left: 4px solid #2d8c3c;
}
.article-content :deep(h4) {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin: 24px 0 12px;
}
.article-content :deep(p) {
  margin: 0 0 16px;
}
.article-content :deep(ul) {
  margin: 0 0 16px;
  padding-left: 24px;
}
.article-content :deep(li) {
  margin-bottom: 8px;
  color: #555;
}
.article-content :deep(strong) {
  color: #1a5d3a;
  font-weight: 600;
}
.article-content :deep(.empty-content) {
  color: #bbb;
  text-align: center;
  padding: 40px 0;
}

/* Tags */
.article-tags {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 20px 0;
  margin-bottom: 24px;
  border-top: 1px solid #f0f0f0;
}
.tags-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #999;
}
.article-tag {
  padding: 5px 14px;
  background: #e8f5e9;
  color: #2d8c3c;
  border-radius: 9999px;
  font-size: 13px;
  transition: all 0.25s ease;
  cursor: default;
}
.article-tag:hover {
  background: #1a5d3a;
  color: #fff;
}

/* Actions */
.article-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}
.article-actions .el-button {
  border-radius: 9999px;
  padding: 10px 24px;
}

/* Related Section */
.related-section {
  background: #fff;
  border-radius: 20px;
  padding: 32px 40px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}
.section-bar {
  width: 4px;
  height: 22px;
  background: #2d8c3c;
  border-radius: 9999px;
}
.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a5d3a;
}
.section-hint {
  font-size: 13px;
  color: #aaa;
}
.related-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.related-card {
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
}
.related-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  border-color: #e8f5e9;
}
.related-cover {
  height: 110px;
  overflow: hidden;
  background: #f5f5f5;
}
.related-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}
.related-card:hover .related-cover img {
  transform: scale(1.05);
}
.related-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f5e9, #c8e6c9);
  color: #1a5d3a;
}
.related-body {
  padding: 12px;
}
.related-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  line-height: 1.4;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 39px;
}
.related-views {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #aaa;
}

/* Not Found */
.not-found {
  padding: 80px 0;
}

/* Responsive */
@media (max-width: 768px) {
  .article-container {
    padding: 28px 20px;
  }
  .article-title {
    font-size: 22px;
  }
  .related-section {
    padding: 24px 20px;
  }
  .related-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
