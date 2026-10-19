<template>
  <div class="dashboard">
    <!-- Stat Cards -->
    <div class="stat-row">
      <div class="stat-card" v-for="card in statCards" :key="card.title" :style="{ '--card-color': card.color }">
        <div class="stat-icon" :style="{ background: card.gradient }">
          <el-icon :size="24" color="white"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-title">{{ card.title }}</div>
        </div>
      </div>
    </div>

    <!-- Revenue + Order Status -->
    <div class="dashboard-grid" v-if="showSalesSection">
      <div class="dash-card revenue-card">
        <div class="dash-card-header">
          <h3>营收概览</h3>
          <span class="revenue-amount">￥{{ Number(overview.totalRevenue || 0).toFixed(2) }}</span>
        </div>
        <div class="order-status-grid">
          <div class="status-item" v-for="s in orderStatusItems" :key="s.key">
            <div class="status-dot" :style="{ background: s.color }"></div>
            <div class="status-info">
              <span class="status-label">{{ s.label }}</span>
              <span class="status-count">{{ s.value }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="dash-card">
        <div class="dash-card-header">
          <h3>热销商品 Top 5</h3>
        </div>
        <div class="hot-products" v-if="hotProducts.length > 0">
          <div class="hot-item" v-for="(p, idx) in hotProducts" :key="p.productId">
            <div class="hot-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</div>
            <div class="hot-info">
              <div class="hot-name">{{ p.productName || '未知商品' }}</div>
              <div class="hot-meta">
                <span class="hot-price">￥{{ Number(p.price || 0).toFixed(2) }}</span>
                <span class="hot-sales">已售 {{ p.salesCount }} 件</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-hot">
          <span>暂无销售数据</span>
        </div>
      </div>
    </div>

    <!-- Content Module Stats -->
    <div class="content-stats-row" v-if="showContentStats">
      <div class="content-stat-card" v-for="c in contentStats" :key="c.title" :style="{ '--c-color': c.color }">
        <div class="content-stat-icon" :style="{ background: c.color + '15', color: c.color }">
          <el-icon :size="18"><component :is="c.icon" /></el-icon>
        </div>
        <div class="content-stat-body">
          <div class="content-stat-value">{{ c.value }}</div>
          <div class="content-stat-title">{{ c.title }}</div>
        </div>
        <router-link :to="c.link" class="content-stat-link">
          管理 <el-icon><ArrowRight /></el-icon>
        </router-link>
      </div>
    </div>

    <!-- System Features + Traceability Flow -->
    <div class="dashboard-grid" :class="{ 'single-col': !showSystemFeatures }">
      <div class="dash-card" v-if="showSystemFeatures">
        <div class="dash-card-header">
          <h3>系统功能</h3>
        </div>
        <div class="feature-list">
          <div class="feature-row" v-for="f in features" :key="f.title">
            <div class="feature-icon" :style="{ background: f.color + '15', color: f.color }">
              <el-icon :size="20"><component :is="f.icon" /></el-icon>
            </div>
            <div class="feature-text">
              <div class="feature-title">{{ f.title }}</div>
              <div class="feature-desc">{{ f.desc }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="dash-card">
        <div class="dash-card-header">
          <h3>溯源流程</h3>
        </div>
        <div class="flow-list">
          <div class="flow-step" v-for="(step, i) in flowSteps" :key="i">
            <div class="step-num">{{ i + 1 }}</div>
            <div class="step-content">
              <div class="step-title">{{ step.title }}</div>
              <div class="step-desc">{{ step.desc }}</div>
            </div>
            <div class="step-line" v-if="i < flowSteps.length - 1"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { statsApi } from '../../api'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const userRole = computed(() => userStore.userInfo?.role || '')

const overview = ref({})
const hotProducts = ref([])

const allStatCards = [
  { title: '商品数量', value: () => overview.value.productCount || 0, icon: 'Goods', gradient: 'linear-gradient(135deg, #1a5d3a, #0f3d24)', color: '#1a5d3a', roles: ['ADMIN', 'MERCHANT', 'FARMER'] },
  { title: '订单总数', value: () => overview.value.orderCount || 0, icon: 'List', gradient: 'linear-gradient(135deg, #2196F3, #1565C0)', color: '#2196F3', roles: ['ADMIN', 'MERCHANT'] },
  { title: '总销售额', value: () => '￥' + Number(overview.value.totalRevenue || 0).toFixed(2), icon: 'Money', gradient: 'linear-gradient(135deg, #F59E0B, #E65100)', color: '#F59E0B', roles: ['ADMIN', 'MERCHANT'] },
  { title: '用户数量', value: () => overview.value.userCount || 0, icon: 'User', gradient: 'linear-gradient(135deg, #9C27B0, #6A1B9A)', color: '#9C27B0', roles: ['ADMIN'] },
  { title: '溯源批次', value: () => overview.value.traceCount || 0, icon: 'Connection', gradient: 'linear-gradient(135deg, #E53935, #C62828)', color: '#E53935', roles: ['ADMIN', 'MERCHANT', 'FARMER'] },
  // 农户数量属于平台口径，农户账号只需要看自己的批次，不给这张卡
  { title: '农户数量', value: () => overview.value.farmerCount || 0, icon: 'OfficeBuilding', gradient: 'linear-gradient(135deg, #009688, #00695C)', color: '#009688', roles: ['ADMIN'] }
]

const statCards = computed(() => allStatCards
  .filter(c => c.roles.includes(userRole.value))
  .map(c => ({ ...c, value: c.value() }))
)

const showSalesSection = computed(() => ['ADMIN', 'MERCHANT'].includes(userRole.value))
// 内容类统计（百科/食谱/FAQ/时令/公告）已经收紧为管理员专属功能，
// 商家看到的入口点进去只会被踢回仪表盘，所以整块不再对商家展示
const showContentStats = computed(() => userRole.value === 'ADMIN')
const showSystemFeatures = computed(() => userRole.value === 'ADMIN')

const orderStatusItems = computed(() => {
  const s = overview.value.orderStatus || {}
  return [
    { key: 'pending', label: '待付款', value: s.pending || 0, color: '#E65100' },
    { key: 'paid', label: '已付款', value: s.paid || 0, color: '#1565C0' },
    { key: 'shipped', label: '已发货', value: s.shipped || 0, color: '#7B1FA2' },
    { key: 'completed', label: '已完成', value: s.completed || 0, color: '#2E7D32' },
    { key: 'cancelled', label: '已取消', value: s.cancelled || 0, color: '#C62828' }
  ]
})

const features = [
  { title: '商品管理', desc: '商品信息维护、库存管理、上下架', icon: 'Goods', color: '#1a5d3a' },
  { title: '订单管理', desc: '订单发货、取消、状态流转', icon: 'List', color: '#2196F3' },
  { title: '溯源管理', desc: '创建批次、录入产地/质检/物流', icon: 'Connection', color: '#E53935' },
  { title: '农户管理', desc: '农户信息、产地地图、区域标签', icon: 'OfficeBuilding', color: '#009688' },
  { title: '百科管理', desc: '农业知识文章发布与维护', icon: 'Reading', color: '#7B1FA2' },
  { title: '食谱管理', desc: '农产食谱、食材关联管理', icon: 'Food', color: '#F57C00' },
  { title: 'FAQ管理', desc: '常见问题维护与分类', icon: 'QuestionFilled', color: '#5C6BC0' },
  { title: '时令管理', desc: '商品时令关联与旺季标记', icon: 'Calendar', color: '#26A69A' }
]

const contentStats = computed(() => [
  { title: '百科文章', value: overview.value.knowledgeCount || 0, icon: 'Reading', color: '#7B1FA2', link: '/admin/knowledge' },
  { title: '食谱数量', value: overview.value.recipeCount || 0, icon: 'Food', color: '#F57C00', link: '/admin/recipes' },
  { title: 'FAQ条目', value: overview.value.faqCount || 0, icon: 'QuestionFilled', color: '#5C6BC0', link: '/admin/faqs' },
  { title: '时令关联', value: overview.value.seasonalCount || 0, icon: 'Calendar', color: '#26A69A', link: '/admin/seasonal' },
  { title: '公告数量', value: overview.value.announcementCount || 0, icon: 'Bell', color: '#FF7043', link: '/admin/announcements' }
])

const flowSteps = [
  { title: '创建溯源批次', desc: '选择商品，自动生成溯源码' },
  { title: '录入产地信息', desc: '农场、位置、种植/采收日期' },
  { title: '录入质检信息', desc: '质检员、检测项目、合格状态' },
  { title: '录入物流信息', desc: '发货、运输公司、冷链温度' },
  { title: '消费者扫码查询', desc: '输入溯源码，查看全链路信息' }
]

onMounted(async () => {
  try {
    const [overviewRes, hotRes] = await Promise.all([
      statsApi.overview(),
      statsApi.hotProducts()
    ])
    overview.value = overviewRes.data || {}
    hotProducts.value = hotRes.data || []
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
})
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 20px; }

/* Stat Cards */
.stat-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}
.stat-card {
  background: var(--color-card);
  border-radius: var(--radius-lg);
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: var(--shadow-sm);
  transition: transform 0.2s var(--ease-standard), box-shadow 0.2s var(--ease-standard);
  border-top: 3px solid var(--card-color, var(--color-primary));
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-title);
  font-family: var(--font-number);
}
.stat-title {
  font-size: 12px;
  color: var(--color-text-hint);
  margin-top: 2px;
}

/* Grid */
.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.dashboard-grid.single-col {
  grid-template-columns: 1fr;
}
.dash-card {
  background: var(--color-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
}
.dash-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border-light);
}
.dash-card-header h3 {
  font-size: 16px;
  color: var(--color-title);
}

/* Revenue Card */
.revenue-amount {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-danger);
  font-family: var(--font-number);
}
.order-status-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}
.status-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 4px;
  background: var(--color-bg);
  border-radius: var(--radius-md);
  text-align: center;
}
.status-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
}
.status-info { display: flex; flex-direction: column; gap: 2px; }
.status-label { font-size: 11px; color: var(--color-text-hint); }
.status-count { font-size: 20px; font-weight: 700; color: var(--color-title); font-family: var(--font-number); }

/* Hot Products */
.hot-products { display: flex; flex-direction: column; gap: 12px; }
.hot-item { display: flex; align-items: center; gap: 12px; }
.hot-rank {
  width: 28px; height: 28px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700;
  color: white;
  flex-shrink: 0;
}
.rank-1 { background: #E53935; }
.rank-2 { background: #FB8C00; }
.rank-3 { background: #FDD835; color: #333; }
.rank-4 { background: #81C784; }
.rank-5 { background: #90CAF9; }
.hot-info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.hot-name { font-size: 14px; font-weight: 500; color: var(--color-title); }
.hot-meta { display: flex; gap: 12px; font-size: 13px; }
.hot-price { color: var(--color-danger); font-weight: 600; }
.hot-sales { color: var(--color-text-hint); }
.empty-hot { text-align: center; padding: 40px; color: var(--color-text-hint); font-size: 14px; }

/* Feature List */
.feature-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.feature-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.feature-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.feature-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-title);
}
.feature-desc {
  font-size: 12px;
  color: var(--color-text-hint);
  margin-top: 2px;
}

/* Flow List */
.flow-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.flow-step {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  position: relative;
  padding-bottom: 20px;
}
.step-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--color-primary);
  color: white;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.step-content {
  padding-top: 3px;
}
.step-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-title);
}
.step-desc {
  font-size: 12px;
  color: var(--color-text-hint);
  margin-top: 2px;
}
.step-line {
  position: absolute;
  left: 14px;
  top: 28px;
  width: 1px;
  height: 20px;
  background: var(--color-border);
}

/* Content Stats Row */
.content-stats-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}
.content-stat-card {
  background: var(--color-card);
  border-radius: var(--radius-lg);
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: var(--shadow-sm);
  transition: transform 0.2s var(--ease-standard), box-shadow 0.2s var(--ease-standard);
  border-left: 3px solid var(--c-color, var(--color-primary));
}
.content-stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
.content-stat-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.content-stat-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.content-stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-title);
  font-family: var(--font-number);
}
.content-stat-title {
  font-size: 11px;
  color: var(--color-text-hint);
}
.content-stat-link {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: var(--c-color, var(--color-primary));
  text-decoration: none;
  white-space: nowrap;
  opacity: 0.7;
  transition: opacity 0.2s;
}
.content-stat-link:hover {
  opacity: 1;
}
</style>
