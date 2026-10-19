<template>
  <div class="ship-workbench">
    <!-- 工具栏：批量操作入口 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button :loading="loading" @click="loadData">刷新</el-button>
        <span class="hint">共 {{ rows.length }} 笔待发货，已选 {{ selected.length }} 笔</span>
      </div>
      <el-button
        type="primary"
        :disabled="selected.length === 0"
        :loading="shipping"
        @click="batchShip"
      >
        批量发货（{{ selected.length }}）
      </el-button>
    </div>

    <!-- 汇总 -->
    <div class="stats-row">
      <div class="stat-chip">
        <span class="chip-label">待发货订单</span>
        <span class="chip-num blue">{{ rows.length }}</span>
      </div>
      <div class="stat-chip">
        <span class="chip-label">待发出商品</span>
        <span class="chip-num orange">{{ totalQty }}</span>
      </div>
      <div class="stat-chip">
        <span class="chip-label">待发货金额</span>
        <span class="chip-num red">￥{{ totalAmount }}</span>
      </div>
    </div>

    <!-- 明细 -->
    <div class="table-card">
      <el-table
        :data="rows"
        v-loading="loading"
        :row-key="rowKey"
        style="width: 100%"
        @selection-change="onSelect"
      >
        <el-table-column type="selection" width="48" />

        <el-table-column label="订单号" width="180">
          <template #default="{ row }">
            <span class="order-no">{{ row.order.orderNo }}</span>
          </template>
        </el-table-column>

        <el-table-column label="收货信息" min-width="220">
          <template #default="{ row }">
            <div class="receiver">
              <div class="receiver-name">{{ row.order.receiverName }} {{ row.order.receiverPhone }}</div>
              <div class="receiver-addr">{{ row.order.receiverAddress }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="商品明细" min-width="260">
          <template #default="{ row }">
            <div class="items">
              <span v-for="(it, i) in row.items" :key="i" class="item-tag">
                {{ it.productName }} ×{{ it.quantity }}
              </span>
              <span v-if="!row.items || row.items.length === 0" class="item-empty">无明细</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="金额" width="110">
          <template #default="{ row }">
            <span class="price-text">￥{{ Number(row.order.totalAmount || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="付款时间" width="160">
          <template #default="{ row }">{{ formatTime(row.order.payTime) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="warning"
              :loading="shippingId === row.order.id"
              @click="shipOne(row)"
            >
              发货
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="没有待发货的订单，都发完了" />
        </template>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminOrderApi } from '../../api'

const loading = ref(false)
const shipping = ref(false)
/** 单笔发货中的订单 ID，用于只让那一行转圈 */
const shippingId = ref(null)

/** 行数据形如 { order: {...}, items: [...] } */
const rows = ref([])
const selected = ref([])

/** 待发出的商品总件数 */
const totalQty = computed(() =>
  rows.value.reduce(
    (sum, r) => sum + (r.items || []).reduce((s, it) => s + (it.quantity || 0), 0),
    0
  )
)

/** 待发货订单金额合计 */
const totalAmount = computed(() =>
  rows.value
    .reduce((sum, r) => sum + Number(r.order?.totalAmount || 0), 0)
    .toFixed(2)
)

/** 表格行 key，避免选中态在刷新后错位 */
const rowKey = (row) => row.order?.id

/**
 * 加载待发货订单。
 * 后端已按身份收窄：商家只拿到含自己商品的订单。
 */
const loadData = async () => {
  loading.value = true
  try {
    const res = await adminOrderApi.pendingShip()
    rows.value = res.data || []
    selected.value = []
  } catch (e) {
    console.error('加载待发货订单失败', e)
    ElMessage.error('加载待发货订单失败')
  } finally {
    loading.value = false
  }
}

const onSelect = (val) => {
  selected.value = val
}

/** 时间格式化：后端返回 ISO 字符串，这里只保留到分钟 */
const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '-')

/** 单笔发货 */
const shipOne = async (row) => {
  try {
    await ElMessageBox.confirm(`确认给订单 ${row.order.orderNo} 发货？`, '确认发货', {
      type: 'warning',
      confirmButtonText: '确认发货',
      cancelButtonText: '取消'
    })
  } catch {
    // 用户点了取消，什么都不做
    return
  }
  shippingId.value = row.order.id
  try {
    await adminOrderApi.ship(row.order.id)
    ElMessage.success('已发货')
    await loadData()
  } catch (e) {
    console.error('发货失败', e)
    ElMessage.error(e?.response?.data?.message || '发货失败')
  } finally {
    shippingId.value = null
  }
}

/** 批量发货：后端逐单处理，失败原因原样带回 */
const batchShip = async () => {
  if (selected.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确认批量发货 ${selected.value.length} 笔订单？`,
      '批量发货',
      { type: 'warning', confirmButtonText: '确认发货', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  shipping.value = true
  try {
    const ids = selected.value.map((r) => r.order.id)
    const res = await adminOrderApi.batchShip(ids)
    const { success = 0, failed = [] } = res.data || {}
    if (failed.length > 0) {
      // 部分失败要说清楚是哪几单，不能只提示"成功 N 笔"
      ElMessage.warning(`成功 ${success} 笔，失败 ${failed.length} 笔：${failed.join('；')}`)
    } else {
      ElMessage.success(`已发货 ${success} 笔`)
    }
    await loadData()
  } catch (e) {
    console.error('批量发货失败', e)
    ElMessage.error('批量发货失败')
  } finally {
    shipping.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.ship-workbench { display: flex; flex-direction: column; gap: 16px; }

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: white;
  padding: 14px 16px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.toolbar-left { display: flex; align-items: center; gap: 12px; }
.hint { font-size: 13px; color: #888; }

.stats-row { display: flex; gap: 12px; flex-wrap: wrap; }
.stat-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  background: white;
  padding: 12px 18px;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.chip-label { font-size: 13px; color: #888; }
.chip-num { font-size: 20px; font-weight: 700; color: #1a1a1a; }
.chip-num.red { color: #e53935; }
.chip-num.orange { color: #f59e0b; }
.chip-num.blue { color: #2196f3; }

.table-card {
  background: white;
  border-radius: 12px;
  padding: 8px 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.order-no { font-family: Consolas, Monaco, monospace; font-size: 13px; color: #303133; }
.receiver-name { font-size: 13px; color: #1a1a1a; }
.receiver-addr { font-size: 12px; color: #909399; margin-top: 2px; }

.items { display: flex; flex-wrap: wrap; gap: 6px; }
.item-tag {
  font-size: 12px;
  color: #1a5d3a;
  background: #ecf5ef;
  border-radius: 4px;
  padding: 2px 8px;
}
.item-empty { font-size: 12px; color: #c0c4cc; }

.price-text { color: #e65100; font-weight: 600; }
</style>
