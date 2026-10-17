<template>
  <div class="help-page">
    <!-- Hero -->
    <section class="hero-banner section-anim">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <span class="hero-badge">
          <el-icon><QuestionFilled /></el-icon>
          全心服务 · 有问必答
        </span>
        <h1 class="hero-title">帮助中心</h1>
        <p class="hero-subtitle">在这里找到关于溯源、订单、支付、配送与售后的常见问题解答</p>
        <div class="hero-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索问题关键词，如：溯源码、退款、配送时间..."
            clearable
            size="large"
            @input="onSearch"
            @clear="onSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
            <template #append>
              <el-button @click="onSearch" :icon="Search">搜索</el-button>
            </template>
          </el-input>
        </div>
      </div>
    </section>

    <div class="help-layout">
      <!-- Category tabs -->
      <div class="category-bar">
        <div
          class="cat-tab"
          :class="{ active: !activeCategory }"
          @click="selectCategory(null)"
        >
          <el-icon><Grid /></el-icon>
          全部
          <span class="cat-count" v-if="allCount">{{ allCount }}</span>
        </div>
        <div
          v-for="c in categories"
          :key="c.value"
          class="cat-tab"
          :class="{ active: activeCategory === c.value }"
          @click="selectCategory(c.value)"
        >
          <el-icon><component :is="resolveCatIcon(c.value)" /></el-icon>
          {{ c.label }}
          <span class="cat-count" v-if="categoryCount[c.value]">{{ categoryCount[c.value] }}</span>
        </div>
      </div>

      <!-- FAQ list -->
      <div class="faq-section" v-loading="loading">
        <div class="faq-header">
          <h2 class="section-title"><span class="title-bar"></span>常见问题</h2>
          <span class="faq-count" v-if="filteredList.length">共 {{ filteredList.length }} 条结果</span>
        </div>

        <div v-if="filteredList.length" class="faq-list">
          <el-collapse v-model="activeNames" accordion>
            <el-collapse-item
              v-for="(item, idx) in filteredList"
              :key="item.id"
              :name="item.id"
              class="faq-item"
            >
              <template #title>
                <div class="faq-q" :style="{ '--i': idx }">
                  <span class="q-index">{{ String(idx + 1).padStart(2, '0') }}</span>
                  <span class="q-text">{{ item.question }}</span>
                  <el-tag size="small" effect="light" class="q-tag" v-if="item.category && catLabel(item.category)">
                    {{ catLabel(item.category) }}
                  </el-tag>
                  <span class="q-views" v-if="item.viewCount != null">
                    <el-icon><View /></el-icon>{{ item.viewCount }}
                  </span>
                </div>
              </template>
              <div class="faq-a">
                <div class="a-icon"><el-icon><ChatLineRound /></el-icon></div>
                <div class="a-body">
                  <p class="a-text">{{ item.answer }}</p>
                  <div class="a-footer">
                    <span class="a-meta">分类：{{ catLabel(item.category) || '未分类' }}</span>
                    <el-button
                      text
                      size="small"
                      :icon="item.id === activeNames ? 'ArrowUp' : 'ArrowDown'"
                      @click.stop="toggleItem(item.id)"
                    >
                      {{ item.id === activeNames ? '收起' : '展开' }}
                    </el-button>
                  </div>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>

        <!-- Empty -->
        <div v-else-if="!loading" class="empty-state">
          <el-empty :description="searchKeyword ? '未找到相关问题' : '暂无常见问题'">
            <template #image>
              <div class="empty-icon">
                <el-icon size="48"><QuestionFilled /></el-icon>
              </div>
            </template>
            <el-button v-if="searchKeyword" type="primary" plain @click="clearSearch">清除搜索</el-button>
          </el-empty>
        </div>

        <!-- Contact card -->
        <div class="contact-card">
          <div class="contact-icon">
            <el-icon><Service /></el-icon>
          </div>
          <div class="contact-body">
            <h3>没有找到您的问题？</h3>
            <p>联系我们的客服团队，我们将尽快为您解答</p>
          </div>
          <div class="contact-actions">
            <el-button type="primary" round @click="goHome">返回首页</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { faqApi } from '@/api'

const router = useRouter()

const searchKeyword = ref('')
const activeCategory = ref(null)
const faqList = ref([])
const categories = ref([])
const loading = ref(false)
const activeNames = ref(null)
const categoryCount = ref({})

const catIconMap = {
  TRACE: 'Search',
  ORDER: 'Document',
  PAY: 'Wallet',
  DELIVERY: 'Van',
  AFTER_SALE: 'Service',
  ACCOUNT: 'User'
}
const resolveCatIcon = (val) => (val && catIconMap[val]) || 'QuestionFilled'

const catLabel = (val) => {
  const c = categories.value.find((i) => i.value === val)
  return c ? c.label : ''
}

const filteredList = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return faqList.value
  return faqList.value.filter(
    (i) =>
      (i.question && i.question.toLowerCase().includes(kw)) ||
      (i.answer && i.answer.toLowerCase().includes(kw))
  )
})

const allCount = computed(() => faqList.value.length)

const loadCategories = async () => {
  try {
    const res = await faqApi.categories()
    categories.value = res.data || []
    buildCategoryCount()
  } catch (e) {
    categories.value = []
  }
}

const buildCategoryCount = () => {
  const map = {}
  faqList.value.forEach((i) => {
    if (i.category) map[i.category] = (map[i.category] || 0) + 1
  })
  categoryCount.value = map
}

const loadList = async (category) => {
  loading.value = true
  try {
    const res = await faqApi.list(category || undefined)
    faqList.value = res.data || []
    buildCategoryCount()
  } catch (e) {
    faqList.value = []
  } finally {
    loading.value = false
  }
}

const selectCategory = (val) => {
  activeCategory.value = val
  activeNames.value = null
  loadList(val)
}

const onSearch = () => {
  activeNames.value = null
}

const clearSearch = () => {
  searchKeyword.value = ''
  onSearch()
}

const toggleItem = (id) => {
  activeNames.value = activeNames.value === id ? null : id
}

const goHome = () => router.push('/')

onMounted(async () => {
  await loadCategories()
  await loadList()
})
</script>

<style scoped>
.help-page { max-width: 1100px; margin: 0 auto; }

/* Hero */
.hero-banner {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  margin-bottom: 24px;
}
.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1a5d3a 0%, #2d8c3c 60%, #3da655 100%);
}
.hero-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 80% 20%, rgba(255,255,255,0.16), transparent 45%),
    radial-gradient(circle at 20% 90%, rgba(232,245,233,0.2), transparent 50%);
}
.hero-content {
  position: relative;
  z-index: 1;
  padding: 44px 44px 36px;
  color: #fff;
  text-align: center;
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
.hero-title { font-size: 36px; font-weight: 800; margin-bottom: 10px; letter-spacing: 1px; }
.hero-subtitle { font-size: 15px; opacity: 0.9; margin-bottom: 24px; }
.hero-search { max-width: 560px; margin: 0 auto; }
.hero-search :deep(.el-input__wrapper) {
  border-radius: 9999px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.16);
}
.hero-search :deep(.el-input-group__append) {
  border-radius: 0 9999px 9999px 0;
  background: #1a5d3a;
  border: none;
  color: #fff;
}
.hero-search :deep(.el-input-group__append .el-button) { color: #fff; }
.hero-search :deep(.el-input-group__append .el-button:hover) { background: rgba(255,255,255,0.15); }

/* Layout */
.help-layout { display: flex; flex-direction: column; gap: 20px; }

/* Category bar */
.category-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  background: #fff;
  border-radius: 12px;
  padding: 14px 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.cat-tab {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 16px;
  border-radius: 9999px;
  font-size: 14px;
  color: #555;
  background: #f7faf7;
  border: 1px solid #eef2ee;
  cursor: pointer;
  transition: all 0.25s ease;
}
.cat-tab .el-icon { font-size: 15px; }
.cat-tab:hover {
  border-color: #2d8c3c;
  color: #2d8c3c;
  transform: translateY(-2px);
}
.cat-tab.active {
  background: linear-gradient(135deg, #1a5d3a, #2d8c3c);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 6px 16px rgba(45,140,60,0.32);
}
.cat-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: rgba(0,0,0,0.08);
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 600;
}
.cat-tab.active .cat-count { background: rgba(255,255,255,0.25); }

/* FAQ section */
.faq-section { background: #fff; border-radius: 12px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.faq-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.section-title { font-size: 20px; font-weight: 700; color: #1a1a1a; display: flex; align-items: center; gap: 10px; }
.title-bar { width: 4px; height: 20px; background: #2d8c3c; border-radius: 2px; }
.faq-count { font-size: 13px; color: #999; }

.faq-list :deep(.el-collapse) { border: none; }
.faq-list :deep(.el-collapse-item__header) {
  border: 1px solid #f0f3f0;
  border-radius: 12px;
  margin-bottom: 12px;
  padding: 0 16px;
  height: auto;
  min-height: 60px;
  background: #fbfcfb;
  transition: all 0.25s ease;
}
.faq-list :deep(.el-collapse-item__header:hover) {
  border-color: #c8e6c9;
  background: #f3faf4;
}
.faq-list :deep(.el-collapse-item.is-active .el-collapse-item__header) {
  border-color: #2d8c3c;
  background: #e8f5e9;
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}
.faq-list :deep(.el-collapse-item__wrap) { border: 1px solid #2d8c3c; border-top: none; border-radius: 0 0 12px 12px; margin-bottom: 12px; }
.faq-list :deep(.el-collapse-item__content) { padding: 0; }

.faq-q {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 8px 0;
  animation: fadeInUp 0.4s ease both;
  animation-delay: calc(var(--i, 0) * 0.04s);
}
.q-index {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: linear-gradient(135deg, #1a5d3a, #2d8c3c);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.q-text {
  flex: 1;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}
.q-tag { margin-right: 4px; color: #2d8c3c; border-color: #c8e6c9; background: #e8f5e9; }
.q-views {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: #bbb;
  flex-shrink: 0;
}
.q-views .el-icon { font-size: 13px; }

.faq-a {
  display: flex;
  gap: 14px;
  padding: 20px 24px;
  background: #fbfdfb;
}
.a-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #e8f5e9, #d7ecd9);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2d8c3c;
}
.a-body { flex: 1; }
.a-text { font-size: 14px; color: #555; line-height: 1.8; margin-bottom: 14px; }
.a-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px dashed #eef2ee;
}
.a-meta { font-size: 12px; color: #aaa; }
.a-footer :deep(.el-button) { color: #2d8c3c; }

/* Empty */
.empty-state { padding: 50px 0; }
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

/* Contact */
.contact-card {
  margin-top: 28px;
  display: flex;
  align-items: center;
  gap: 20px;
  background: linear-gradient(135deg, #f0f9eb, #e8f5e9);
  border: 1px solid #c8e6c9;
  border-radius: 12px;
  padding: 24px 28px;
}
.contact-icon {
  flex-shrink: 0;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: linear-gradient(135deg, #1a5d3a, #2d8c3c);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
}
.contact-body { flex: 1; }
.contact-body h3 { font-size: 16px; color: #1a5d3a; margin-bottom: 4px; }
.contact-body p { font-size: 13px; color: #666; }

.section-anim { animation: fadeInUp 0.6s ease both; }
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .hero-content { padding: 32px 20px 28px; }
  .hero-title { font-size: 26px; }
  .faq-q { flex-wrap: wrap; }
  .q-views { display: none; }
  .contact-card { flex-direction: column; text-align: center; }
}
</style>
