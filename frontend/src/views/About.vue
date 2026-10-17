<template>
  <div class="about-page">
    <!-- Banner -->
    <div class="about-banner">
      <div class="banner-bg"></div>
      <div class="banner-content">
        <div class="banner-badge">毕业设计作品</div>
        <h1>鲜源农产品溯源销售平台</h1>
        <p>从田间到餐桌，全程溯源保障食品安全</p>
      </div>
    </div>

    <!-- Intro -->
    <div class="section">
      <h2 class="section-title"><span class="title-bar"></span>系统介绍</h2>
      <div class="intro-card">
        <p>鲜源农产品溯源销售平台是一个集农产品销售与全链路溯源于一体的综合性平台。消费者在购买农产品的同时，可以通过溯源码查询商品从种植、质检到物流的全过程信息，实现"来源可查、去向可追、责任可究"的食品安全保障目标。</p>
        <p>系统采用前后端分离架构，后端基于 SpringBoot + MyBatis-Plus + MySQL，前端基于 Vue3 + Element Plus，界面美观，功能完善。</p>
      </div>
    </div>

    <!-- Tech Stack -->
    <div class="section">
      <h2 class="section-title"><span class="title-bar"></span>技术架构</h2>
      <div class="tech-grid">
        <div class="tech-card" v-for="t in techStack" :key="t.name">
          <div class="tech-icon" :style="{ background: t.gradient }">
            <span class="tech-abbr">{{ t.abbr }}</span>
          </div>
          <h3>{{ t.name }}</h3>
          <p>{{ t.desc }}</p>
        </div>
      </div>
    </div>

    <!-- Features -->
    <div class="section">
      <h2 class="section-title"><span class="title-bar"></span>功能特性</h2>
      <div class="feature-grid">
        <div class="feature-item" v-for="(f, idx) in features" :key="idx">
          <div class="feature-num">{{ String(idx + 1).padStart(2, '0') }}</div>
          <div class="feature-body">
            <h3>{{ f.title }}</h3>
            <p>{{ f.desc }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Stats -->
    <div class="section">
      <h2 class="section-title"><span class="title-bar"></span>系统数据</h2>
      <div class="stats-row" v-loading="loading">
        <div class="stat-card" v-for="s in stats" :key="s.label">
          <div class="stat-icon-box" :style="{ background: s.gradient }">
            <el-icon :size="24" color="white"><component :is="s.icon" /></el-icon>
          </div>
          <div class="stat-num">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <!-- Info -->
    <div class="section">
      <h2 class="section-title"><span class="title-bar"></span>项目信息</h2>
      <div class="info-grid">
        <div class="info-card">
          <div class="info-row"><span class="info-key">项目类型</span><span class="info-val">本科毕业设计</span></div>
          <div class="info-row"><span class="info-key">所属专业</span><span class="info-val">软件工程</span></div>
          <div class="info-row"><span class="info-key">所属院校</span><span class="info-val">哈尔滨金融学院</span></div>
          <div class="info-row"><span class="info-key">开发工具</span><span class="info-val">TRAE</span></div>
        </div>
        <div class="info-card highlight">
          <h3>核心价值</h3>
          <p>通过技术手段解决食品安全信息不对称问题，让消费者买得放心、吃得安心，同时帮助农户建立品牌信任，促进农产品产销对接。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { publicStatsApi } from '../api'

const loading = ref(false)
const stats = ref([
  { label: '商品数量', value: '-', icon: 'Goods', gradient: 'linear-gradient(135deg, #409eff, #337ecc)' },
  { label: '溯源批次', value: '-', icon: 'Connection', gradient: 'linear-gradient(135deg, #67c23a, #529b2e)' },
  { label: '合作农户', value: '-', icon: 'User', gradient: 'linear-gradient(135deg, #e6a23c, #b88230)' },
  { label: '订单数量', value: '-', icon: 'Document', gradient: 'linear-gradient(135deg, #722ed1, #531dab)' }
])

const techStack = [
  { abbr: 'SB', name: 'SpringBoot', desc: '后端框架，提供快速开发能力', gradient: 'linear-gradient(135deg, #6db33f, #5a9e2a)' },
  { abbr: 'MP', name: 'MyBatis-Plus', desc: 'ORM框架，简化数据库操作', gradient: 'linear-gradient(135deg, #409eff, #337ecc)' },
  { abbr: 'M8', name: 'MySQL 8.0', desc: '关系型数据库，存储业务数据', gradient: 'linear-gradient(135deg, #e6a23c, #b88230)' },
  { abbr: 'V3', name: 'Vue3', desc: '前端框架，构建响应式界面', gradient: 'linear-gradient(135deg, #42b883, #35495e)' },
  { abbr: 'EP', name: 'Element Plus', desc: 'UI组件库，提供精美组件', gradient: 'linear-gradient(135deg, #722ed1, #531dab)' },
  { abbr: 'VS', name: 'Vite', desc: '前端构建工具，极速热更新', gradient: 'linear-gradient(135deg, #f56c6c, #c45656)' }
]

const features = [
  { title: '商品销售', desc: '完整的电商流程：浏览、加购、下单、支付、发货、收货' },
  { title: '全链路溯源', desc: '产地信息、质检报告、物流运输三段式溯源，溯源码唯一可查' },
  { title: '后台管理', desc: '商品、订单、溯源、农户、评价、公告、用户十大管理模块' },
  { title: '数据统计', desc: '销售额、订单状态、平台数据多维统计分析' },
  { title: '响应式设计', desc: '适配桌面和移动端，支持汉堡菜单和自适应布局' },
  { title: '用户体验', desc: '专业UI设计，交互流畅，无障碍标签，对比度达标' }
]

const loadStats = async () => {
  loading.value = true
  try {
    const res = await publicStatsApi.summary()
    stats.value[0].value = res.data.productCount || 0
    stats.value[1].value = res.data.traceCount || 0
    stats.value[2].value = res.data.farmerCount || 0
    stats.value[3].value = res.data.orderCount || 0
  } catch {} finally { loading.value = false }
}

onMounted(loadStats)
</script>

<style scoped>
.about-page { max-width: 1100px; margin: 0 auto; }

.about-banner { position: relative; border-radius: 16px; overflow: hidden; margin-bottom: 32px; height: 200px; }
.banner-bg { position: absolute; inset: 0; background: linear-gradient(135deg, #1a5c25, #2d8c3c 50%, #3da655); }
.banner-bg::after { content: ''; position: absolute; inset: 0; background: radial-gradient(circle at 80% 40%, rgba(255,255,255,0.1), transparent 50%); }
.banner-content { position: relative; z-index: 1; padding: 40px; color: white; height: 100%; display: flex; flex-direction: column; justify-content: center; }
.banner-badge { display: inline-block; padding: 4px 12px; background: rgba(255,255,255,0.15); border: 1px solid rgba(255,255,255,0.25); border-radius: 20px; font-size: 12px; width: fit-content; margin-bottom: 12px; backdrop-filter: blur(10px); }
.banner-content h1 { font-size: 32px; margin-bottom: 8px; }
.banner-content p { font-size: 15px; opacity: 0.85; }

.section { margin-bottom: 40px; }
.section-title { font-size: 22px; font-weight: 700; color: #1a1a1a; display: flex; align-items: center; gap: 10px; margin-bottom: 20px; }
.title-bar { width: 4px; height: 22px; background: #2d8c3c; border-radius: 2px; }

.intro-card { background: white; border-radius: 12px; padding: 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.intro-card p { font-size: 15px; color: #555; line-height: 1.8; margin-bottom: 12px; }
.intro-card p:last-child { margin-bottom: 0; }

.tech-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 16px; }
.tech-card { background: white; border-radius: 12px; padding: 24px; text-align: center; box-shadow: 0 1px 3px rgba(0,0,0,0.04); transition: transform 0.2s; }
.tech-card:hover { transform: translateY(-4px); box-shadow: 0 8px 16px rgba(0,0,0,0.06); }
.tech-icon { width: 56px; height: 56px; border-radius: 14px; display: flex; align-items: center; justify-content: center; margin: 0 auto 16px; }
.tech-abbr { color: white; font-size: 18px; font-weight: 700; }
.tech-card h3 { font-size: 16px; color: #333; margin-bottom: 6px; }
.tech-card p { font-size: 13px; color: #999; }

.feature-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.feature-item { display: flex; gap: 16px; background: white; border-radius: 12px; padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); transition: all 0.2s; }
.feature-item:hover { transform: translateY(-2px); box-shadow: 0 8px 16px rgba(0,0,0,0.06); }
.feature-num { font-size: 32px; font-weight: 800; color: #e8f5e9; line-height: 1; flex-shrink: 0; }
.feature-body h3 { font-size: 16px; color: #333; margin-bottom: 6px; }
.feature-body p { font-size: 13px; color: #666; line-height: 1.6; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-card { background: white; border-radius: 12px; padding: 24px; text-align: center; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.stat-icon-box { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; margin: 0 auto 12px; }
.stat-num { font-size: 28px; font-weight: 700; color: #1a1a1a; }
.stat-label { font-size: 13px; color: #999; margin-top: 4px; }

.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.info-card { background: white; border-radius: 12px; padding: 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.info-card.highlight { background: linear-gradient(135deg, #f0f9eb, #e8f5e9); border: 1px solid #c8e6c9; }
.info-row { display: flex; padding: 10px 0; border-bottom: 1px solid #f5f5f5; }
.info-row:last-child { border-bottom: none; }
.info-key { width: 100px; font-size: 14px; color: #999; }
.info-val { font-size: 14px; color: #333; font-weight: 500; }
.info-card.highlight h3 { font-size: 18px; color: #2d8c3c; margin-bottom: 12px; }
.info-card.highlight p { font-size: 14px; color: #555; line-height: 1.8; }

@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr 1fr; }
  .info-grid { grid-template-columns: 1fr; }
}
</style>
