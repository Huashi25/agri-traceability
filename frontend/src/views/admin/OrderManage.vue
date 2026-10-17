<template>
  <div class="order-manage">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button :type="!filterStatus ? 'primary' : ''" @click="filterStatus = null; loadOrders()">全部</el-button>
        <el-button v-for="(label, idx) in statusLabels" :key="idx" :type="filterStatus === idx ? 'primary' : ''" @click="filterStatus = idx; loadOrders()">{{ label }}</el-button>
      </div>
      <el-input v-model="searchKey" placeholder="搜索订单号..." clearable style="width: 220px">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <div class="stats-row">
      <div class="stat-chip"><span class="chip-label">总订单</span><span class="chip-num">{{ stats.total || 0 }}</span></div>
      <div class="stat-chip"><span class="chip-label">待付款</span><span class="chip-num orange">{{ stats.pending || 0 }}</span></div>
      <div class="stat-chip"><span class="chip-label">已付款</span><span class="chip-num blue">{{ stats.paid || 0 }}</span></div>
      <div class="stat-chip"><span class="chip-label">已发货</span><span class="chip-num green">{{ stats.shipped || 0 }}</span></div>
      <div class="stat-chip"><span class="chip-label">已完成</span><span class="chip-num gray">{{ stats.completed || 0 }}</span></div>
      <div class="stat-chip revenue"><span class="chip-label">总销售额</span><span class="chip-num red">￥{{ Number(stats.totalAmount || 0).toFixed(2) }}</span></div>
    </div>

    <div class="table-card">
      <el-table :data="filteredList" v-loading="loading" style="width: 100%">
        <el-table-column label="订单号" width="180">
          <template #default="{ row }"><span class="order-no">{{ row.orderNo }}</span></template>
        </el-table-column>
        <el-table-column label="收货人" width="100" prop="receiverName" />
        <el-table-column label="收货电话" width="130" prop="receiverPhone" />
        <el-table-column label="收货地址" min-width="200" show-overflow-tooltip prop="receiverAddress" />
        <el-table-column label="金额" width="110">
          <template #default="{ row }"><span class="price-text">￥{{ Number(row.totalAmount || 0).toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="dark" size="small">{{ statusLabels[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 1" size="small" type="warning" @click="shipOrder(row)">发货</el-button>
            <el-button v-if="row.status === 0" size="small" type="danger" @click="cancelOrder(row)">取消</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无订单" /></template>
      </el-table>
    </div>

    <el-dialog v-model="detailDialog" title="订单详情" width="600px">
      <div v-if="currentOrder" class="detail-content">
        <div class="detail-row"><span class="label">订单号</span><span>{{ currentOrder.orderNo }}</span></div>
        <div class="detail-row"><span class="label">收货人</span><span>{{ currentOrder.receiverName }}</span></div>
        <div class="detail-row"><span class="label">电话</span><span>{{ currentOrder.receiverPhone }}</span></div>
        <div class="detail-row"><span class="label">地址</span><span>{{ currentOrder.receiverAddress }}</span></div>
        <div class="detail-row"><span class="label">金额</span><span class="price-text">￥{{ Number(currentOrder.totalAmount || 0).toFixed(2) }}</span></div>
        <div class="detail-row"><span class="label">状态</span><el-tag :type="statusTagType(currentOrder.status)" size="small">{{ statusLabels[currentOrder.status] }}</el-tag></div>
        <div class="detail-row"><span class="label">下单时间</span><span>{{ formatTime(currentOrder.createTime) }}</span></div>
        <el-divider>商品明细</el-divider>
        <el-table :data="currentItems" size="small">
          <el-table-column label="商品" prop="productName" />
          <el-table-column label="单价" width="100"><template #default="{ row }">￥{{ Number(row.productPrice || 0).toFixed(2) }}</template></el-table-column>
          <el-table-column label="数量" width="60" prop="quantity" />
          <el-table-column label="小计" width="100"><template #default="{ row }">￥{{ Number(row.subtotal || 0).toFixed(2) }}</template></el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminOrderApi } from '../../api'

const list = ref([])
const stats = ref({})
const loading = ref(false)
const filterStatus = ref(null)
const searchKey = ref('')
const detailDialog = ref(false)
const currentOrder = ref(null)
const currentItems = ref([])

const statusLabels = ['待付款', '已付款', '已发货', '已完成', '已取消']
const statusTagType = (s) => ['warning', 'primary', 'info', 'success', 'danger'][s] || 'info'
const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : ''

const filteredList = computed(() => {
  if (!searchKey.value) return list.value
  return list.value.filter(o => o.orderNo?.includes(searchKey.value))
})

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await adminOrderApi.list(filterStatus.value)
    list.value = res.data
  } finally { loading.value = false }
}

const loadStats = async () => {
  const res = await adminOrderApi.stats()
  stats.value = res.data
}

const viewDetail = async (order) => {
  currentOrder.value = order
  const res = await adminOrderApi.getItems(order.id)
  currentItems.value = res.data
  detailDialog.value = true
}

const shipOrder = async (order) => {
  try {
    await ElMessageBox.confirm('确认发货?', '提示', { type: 'warning' })
    await adminOrderApi.ship(order.id)
    ElMessage.success('已发货')
    loadOrders(); loadStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '发货失败')
  }
}

const cancelOrder = async (order) => {
  try {
    await ElMessageBox.confirm('确认取消该订单? 库存将自动恢复。', '提示', { type: 'warning' })
    await adminOrderApi.cancel(order.id)
    ElMessage.success('已取消')
    loadOrders(); loadStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '取消失败')
  }
}

onMounted(() => { loadOrders(); loadStats() })
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.toolbar-left { display: flex; gap: 6px; }
.stats-row { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-chip { background: white; border-radius: 10px; padding: 12px 20px; display: flex; flex-direction: column; gap: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); flex: 1; }
.stat-chip.revenue { flex: 1.5; }
.chip-label { font-size: 12px; color: #999; }
.chip-num { font-size: 22px; font-weight: 700; color: #333; }
.chip-num.green { color: #67c23a; } .chip-num.blue { color: #409eff; } .chip-num.orange { color: #e6a23c; } .chip-num.gray { color: #909399; } .chip-num.red { color: #f56c6c; }
.table-card { overflow-x: auto; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.order-no { font-family: monospace; font-size: 13px; color: #666; }
.price-text { font-size: 15px; font-weight: 700; color: #f56c6c; }
.detail-content { display: flex; flex-direction: column; gap: 12px; }
.detail-row { display: flex; gap: 16px; }
.detail-row .label { width: 80px; color: #999; font-size: 14px; }
.detail-row span:not(.label) { color: #333; font-size: 14px; }
</style>
