<template>
  <div class="stats-page" v-loading="loading">
    <!-- Revenue Cards -->
    <div class="revenue-cards">
      <div class="rev-card" v-for="card in revCards" :key="card.title">
        <div class="rev-icon" :style="{ background: card.gradient }">
          <el-icon :size="24" color="white"><component :is="card.icon" /></el-icon>
        </div>
        <div class="rev-body">
          <div class="rev-value">{{ card.value }}</div>
          <div class="rev-title">{{ card.title }}</div>
        </div>
      </div>
    </div>

    <!-- Charts Row -->
    <div class="charts-grid">
      <div class="chart-card">
        <div class="chart-header"><h3>订单状态分布</h3></div>
        <div class="pie-chart">
          <div v-for="(label, idx) in statusLabels" :key="idx" class="pie-item">
            <div class="pie-bar-wrapper">
              <div class="pie-label">{{ label }}</div>
              <div class="pie-bar-bg">
                <div class="pie-bar-fill" :style="{ width: getPercent(idx) + '%', background: statusColors[idx] }"></div>
              </div>
              <div class="pie-count">{{ statusData[idx] || 0 }} 单</div>
            </div>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <div class="chart-header"><h3>平台数据概览</h3></div>
        <div class="overview-grid">
          <div class="ov-item" v-for="item in overviewItems" :key="item.label">
            <div class="ov-icon" :style="{ color: item.color, background: item.color + '15' }">
              <el-icon :size="20"><component :is="item.icon" /></el-icon>
            </div>
            <div class="ov-num" :style="{ color: item.color }">{{ item.value }}</div>
            <div class="ov-label">{{ item.label }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent Orders -->
    <div class="chart-card">
      <div class="chart-header"><h3>最近订单</h3></div>
      <el-table :data="recentOrders" size="small">
        <el-table-column label="订单号" prop="orderNo" width="180">
          <template #default="{ row }"><span style="font-family:monospace;font-size:12px;color:#666">{{ row.orderNo }}</span></template>
        </el-table-column>
        <el-table-column label="收货人" prop="receiverName" width="100" />
        <el-table-column label="金额" width="100">
          <template #default="{ row }"><span style="color:#f56c6c;font-weight:600">￥{{ Number(row.totalAmount || 0).toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="dark">{{ statusLabels[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ row.createTime?.replace('T',' ').substring(0,16) }}</template>
        </el-table-column>
        <template #empty><el-empty description="暂无订单" :image-size="60" /></template>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { statsApi, adminOrderApi } from '../../api'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const userRole = computed(() => userStore.userInfo?.role || '')

const loading = ref(false)
const data = ref({})
const recentOrders = ref([])

const statusLabels = ['待付款', '已付款', '已发货', '已完成', '已取消']
const statusColors = ['#e6a23c', '#409eff', '#909399', '#67c23a', '#f56c6c']
const statusTagType = (s) => ['warning', 'primary', 'info', 'success', 'danger'][s] || 'info'

// 平台级指标（用户数）只对管理员有意义，商家看到的是 0，索性不展示
const revCards = computed(() => {
  const cards = [
    { title: '总销售额', value: '￥' + Number(data.value.totalRevenue || 0).toFixed(2), icon: 'Money', gradient: 'linear-gradient(135deg,#f56c6c,#c45656)' },
    { title: '总订单数', value: data.value.orderCount || 0, icon: 'Document', gradient: 'linear-gradient(135deg,#409eff,#337ecc)' },
    { title: '商品数量', value: data.value.productCount || 0, icon: 'Goods', gradient: 'linear-gradient(135deg,#67c23a,#529b2e)' },
    { title: '溯源批次', value: data.value.traceCount || 0, icon: 'Connection', gradient: 'linear-gradient(135deg,#722ed1,#531dab)' }
  ]
  if (userRole.value === 'ADMIN') {
    cards.splice(3, 0, { title: '用户数量', value: data.value.userCount || 0, icon: 'User', gradient: 'linear-gradient(135deg,#e6a23c,#b88230)' })
  }
  return cards
})

const statusData = computed(() => {
  const os = data.value.orderStatus || {}
  return [os.pending, os.paid, os.shipped, os.completed, os.cancelled]
})

const totalOrders = computed(() => statusData.value.reduce((s, n) => s + (n || 0), 0) || 1)

const getPercent = (idx) => {
  const count = statusData.value[idx] || 0
  return Math.round((count / totalOrders.value) * 100)
}

const overviewItems = computed(() => {
  const items = [
    { label: '商品', value: data.value.productCount || 0, icon: 'Goods', color: '#409eff' },
    { label: '订单', value: data.value.orderCount || 0, icon: 'Document', color: '#67c23a' },
    { label: '农户', value: data.value.farmerCount || 0, icon: 'UserFilled', color: '#e6a23c', adminOnly: true },
    { label: '评价', value: data.value.reviewCount || 0, icon: 'ChatDotRound', color: '#f56c6c' },
    { label: '溯源', value: data.value.traceCount || 0, icon: 'Connection', color: '#722ed1' },
    { label: '用户', value: data.value.userCount || 0, icon: 'User', color: '#909399', adminOnly: true }
  ]
  // 农户数与用户数是平台口径，商家看到的是 0，不展示
  return items.filter((i) => !i.adminOnly || userRole.value === 'ADMIN')
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await statsApi.overview()
    data.value = res.data
    const orderRes = await adminOrderApi.list()
    recentOrders.value = orderRes.data.slice(0, 8)
  } finally { loading.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.revenue-cards { display: grid; grid-template-columns: repeat(5, 1fr); gap: 16px; margin-bottom: 20px; }
.rev-card { background: white; border-radius: 12px; padding: 20px; display: flex; align-items: center; gap: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); transition: transform 0.2s; }
.rev-card:hover { transform: translateY(-2px); box-shadow: 0 8px 16px rgba(0,0,0,0.06); }
.rev-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.rev-value { font-size: 24px; font-weight: 700; color: #1a1a1a; }
.rev-title { font-size: 13px; color: #999; margin-top: 2px; }

.charts-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
.chart-card { background: white; border-radius: 12px; padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); margin-bottom: 20px; }
.chart-header { margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #f5f5f5; }
.chart-header h3 { font-size: 16px; color: #333; }

.pie-chart { display: flex; flex-direction: column; gap: 14px; }
.pie-bar-wrapper { display: flex; align-items: center; gap: 12px; }
.pie-label { width: 60px; font-size: 13px; color: #666; }
.pie-bar-bg { flex: 1; height: 20px; background: #f5f5f5; border-radius: 10px; overflow: hidden; }
.pie-bar-fill { height: 100%; border-radius: 10px; transition: width 0.8s ease; }
.pie-count { width: 60px; font-size: 13px; color: #999; text-align: right; }

.overview-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.ov-item { text-align: center; padding: 16px; background: #fafafa; border-radius: 10px; }
.ov-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; margin: 0 auto 8px; }
.ov-num { font-size: 24px; font-weight: 700; }
.ov-label { font-size: 12px; color: #999; margin-top: 4px; }
</style>
