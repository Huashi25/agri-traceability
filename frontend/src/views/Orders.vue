<template>
  <div class="orders-page">
    <!-- Success Banner -->
    <div v-if="showSuccessBanner" class="success-banner">
      <div class="success-icon">
        <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22,4 12,14.01 9,11.01"/>
        </svg>
      </div>
      <div class="success-text">
        <span class="success-title">订单创建成功！</span>
        <span class="success-desc">您的订单已生成，请在30分钟内完成支付</span>
      </div>
      <span class="success-close" @click="showSuccessBanner = false">×</span>
    </div>

    <div class="page-header">
      <h1>我的订单</h1>
    </div>

    <!-- Status Filter Tabs -->
    <div class="status-tabs">
      <div class="status-tab" :class="{ active: filterStatus === null }" @click="filterStatus = null">
        全部 <span class="count">{{ orderList.length }}</span>
      </div>
      <div v-for="(label, idx) in statusLabels" :key="idx"
           class="status-tab"
           :class="{ active: filterStatus === idx }"
           @click="filterStatus = idx">
        {{ label }}
        <span class="count" v-if="countByStatus(idx) > 0">{{ countByStatus(idx) }}</span>
      </div>
    </div>

    <div v-if="filteredOrders.length === 0" class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" width="80" height="80" fill="none" stroke="currentColor" stroke-width="1">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/>
        </svg>
      </div>
      <p>暂无订单</p>
      <button class="btn-go-shop" @click="$router.push('/')">去购物</button>
    </div>

    <template v-else>
    <div class="order-list">
      <div v-for="order in pagedOrders" :key="order.id" class="order-card">
        <!-- Order Header -->
        <div class="order-header">
          <div class="order-meta">
            <span class="order-no">订单号: {{ order.orderNo }}</span>
            <span class="order-time">{{ formatTime(order.createTime) }}</span>
          </div>
          <span class="status-badge" :class="'status-' + order.status">
            {{ statusLabels[order.status] || '未知' }}
          </span>
        </div>

        <!-- Order Items -->
        <div class="order-body">
          <div v-if="order.orderItems && order.orderItems.length > 0" class="items-list">
            <div v-for="item in order.orderItems" :key="item.id" class="order-item">
              <div class="item-image">
                <img :src="getProductImage(item.productName)" :alt="item.productName" />
              </div>
              <div class="item-info">
                <div class="item-name">{{ item.productName }}</div>
                <div class="item-price">￥{{ item.productPrice }} × {{ item.quantity }}</div>
              </div>
              <div class="item-subtotal">￥{{ item.subtotal }}</div>
            </div>
          </div>
          <div v-else class="loading-items">
            <span>加载中...</span>
          </div>
        </div>

        <!-- Order Timeline -->
        <div class="order-timeline" v-if="order.status !== 4">
          <div class="timeline-step" :class="{ done: order.status >= 0 }">
            <div class="step-dot"></div>
            <div class="step-info">
              <span class="step-label">下单</span>
              <span class="step-time">{{ formatTime(order.createTime) }}</span>
            </div>
          </div>
          <div class="timeline-line" :class="{ active: order.status >= 1 }"></div>
          <div class="timeline-step" :class="{ done: order.status >= 1 }">
            <div class="step-dot"></div>
            <div class="step-info">
              <span class="step-label">付款</span>
              <span class="step-time">{{ formatTime(order.payTime) }}</span>
            </div>
          </div>
          <div class="timeline-line" :class="{ active: order.status >= 2 }"></div>
          <div class="timeline-step" :class="{ done: order.status >= 2 }">
            <div class="step-dot"></div>
            <div class="step-info">
              <span class="step-label">发货</span>
              <span class="step-time">{{ formatTime(order.shipTime) }}</span>
            </div>
          </div>
          <div class="timeline-line" :class="{ active: order.status >= 3 }"></div>
          <div class="timeline-step" :class="{ done: order.status >= 3 }">
            <div class="step-dot"></div>
            <div class="step-info">
              <span class="step-label">收货</span>
              <span class="step-time">{{ formatTime(order.receiveTime) }}</span>
            </div>
          </div>
        </div>

        <!-- Order Footer -->
        <div class="order-footer">
          <div class="order-address" v-if="order.receiverName">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/>
            </svg>
            <span>{{ order.receiverName }} {{ order.receiverPhone }} · {{ order.receiverAddress }}</span>
          </div>
          <div class="order-pay-method" v-if="order.paymentMethod != null">
            <span class="pay-method-label">支付方式:</span>
            <span class="pay-method-value">{{ paymentLabels[order.paymentMethod] || '微信支付' }}</span>
          </div>
          <div class="order-total">
            共 {{ itemCount(order) }} 件商品，合计
            <span class="total-amount">￥{{ order.totalAmount }}</span>
          </div>
          <div class="order-actions">
            <button v-if="order.status === 0" class="btn-action btn-primary" @click="payOrder(order.id)">
              立即付款
            </button>
            <button v-if="order.status === 1" class="btn-action btn-info" disabled>
              等待发货
            </button>
            <button v-if="order.status === 2" class="btn-action btn-success" @click="confirmReceive(order.id)">
              确认收货
            </button>
            <button v-if="order.status === 0" class="btn-action btn-default" @click="cancelOrder(order.id)">
              取消订单
            </button>
            <button v-if="order.status === 3 || order.status === 4" class="btn-action btn-default" @click="$router.push('/')">
              再次购买
            </button>
          </div>
        </div>
      </div>
    </div>
    <div v-if="filteredOrders.length > orderPage.pageSize" class="pagination-bar">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="orderPage.pageNum"
        :page-size="orderPage.pageSize"
        :total="filteredOrders.length"
        @current-change="handleOrderPageChange"
      />
    </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '../api'

import durianImg from '../assets/images/product-durian.jpg'
import orangeImg from '../assets/images/product-orange.jpg'
import riceImg from '../assets/images/product-rice.jpg'
import scallionImg from '../assets/images/product-scallion.jpg'

const route = useRoute()
const orderList = ref([])
const reminding = reactive({})
const filterStatus = ref(null)
const orderPage = ref({ pageNum: 1, pageSize: 10 })
const loading = reactive({})
const showSuccessBanner = ref(false)

const statusLabels = ['待付款', '已付款', '已发货', '已完成', '已取消']
const paymentLabels = ['微信支付', '支付宝', '银行卡', '货到付款']

const productImages = { '榴莲': durianImg, '脐橙': orangeImg, '大米': riceImg, '葱': scallionImg }
const getProductImage = (name) => {
  if (!name) return durianImg
  for (const key in productImages) { if (name.includes(key)) return productImages[key] }
  return durianImg
}

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : '-'

const filteredOrders = computed(() => {
  if (filterStatus.value === null) return orderList.value
  return orderList.value.filter(o => o.status === filterStatus.value)
})

const pagedOrders = computed(() => {
  const start = (orderPage.value.pageNum - 1) * orderPage.value.pageSize
  return filteredOrders.value.slice(start, start + orderPage.value.pageSize)
})

const handleOrderPageChange = (newPage) => {
  orderPage.value.pageNum = newPage
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

watch(filterStatus, () => { orderPage.value.pageNum = 1 })

watch(filteredOrders, (newList) => {
  const maxPage = Math.ceil(newList.length / orderPage.value.pageSize) || 1
  if (orderPage.value.pageNum > maxPage) {
    orderPage.value.pageNum = 1
  }
})

const countByStatus = (status) => orderList.value.filter(o => o.status === status).length

const itemCount = (order) => {
  if (!order.orderItems || order.orderItems.length === 0) return 0
  return order.orderItems.reduce((sum, item) => sum + item.quantity, 0)
}

const loadOrders = async () => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (!userInfo.id) return
  try {
    const res = await orderApi.list(userInfo.id)
    orderList.value = res.data || []
  } catch (e) {
    ElMessage.error('加载订单失败')
  }
}

const payOrder = async (id) => {
  try {
    await orderApi.pay(id)
    ElMessage.success('付款成功')
    loadOrders()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '付款失败')
  }
}

const confirmReceive = async (id) => {
  try {
    await ElMessageBox.confirm('确认已收到商品?', '提示', { type: 'success' })
    await orderApi.confirm(id)
    ElMessage.success('确认收货成功')
    loadOrders()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '操作失败')
    }
  }
}

const cancelOrder = async (id) => {
  try {
    await ElMessageBox.confirm('确认取消该订单? 取消后库存将自动恢复。', '提示', { type: 'warning' })
    await orderApi.cancel(id)
    ElMessage.success('订单已取消')
    loadOrders()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '取消失败')
    }
  }
}

onMounted(() => {
  if (route.query.new === '1') {
    showSuccessBanner.value = true
  }
  loadOrders()
})
</script>

<style scoped>
.orders-page { max-width: 900px; margin: 0 auto; }

/* Success Banner */
.success-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: linear-gradient(135deg, var(--color-primary-bg), var(--color-primary-bg-light));
  border: 1px solid var(--color-primary-light);
  border-radius: var(--radius-lg);
  margin-bottom: 16px;
  animation: slideDown 0.3s var(--ease-standard);
}
@keyframes slideDown { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: translateY(0); } }
.success-icon {
  width: 36px; height: 36px;
  border-radius: 50%;
  background: var(--color-primary);
  color: white;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.success-text { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.success-title { font-size: 15px; font-weight: 600; color: var(--color-primary-dark); }
.success-desc { font-size: 13px; color: var(--color-text-secondary); }
.success-close { font-size: 22px; color: var(--color-text-hint); cursor: pointer; line-height: 1; padding: 0 4px; }
.success-close:hover { color: var(--color-text); }

.page-header h1 { font-size: 24px; color: var(--color-title); font-family: var(--font-heading); margin-bottom: 20px; }

.status-tabs { display: flex; gap: 8px; margin-bottom: 20px; flex-wrap: wrap; }
.status-tab {
  padding: 8px 16px;
  border-radius: var(--radius-full);
  font-size: 14px;
  color: var(--color-text-secondary);
  background: var(--color-card);
  border: 1px solid var(--color-border-light);
  cursor: pointer;
  transition: all 0.2s var(--ease-standard);
}
.status-tab:hover { border-color: var(--color-primary); color: var(--color-primary); }
.status-tab.active { background: var(--color-primary); color: white; border-color: var(--color-primary); }
.status-tab .count { font-size: 12px; opacity: 0.7; margin-left: 4px; }

.empty-state { text-align: center; padding: 80px 20px; color: var(--color-text-hint); }
.empty-state p { margin: 16px 0 24px; }
.btn-go-shop {
  padding: 10px 28px;
  background: var(--color-primary);
  color: white;
  border: none;
  border-radius: var(--radius-full);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s var(--ease-standard);
}
.btn-go-shop:hover { background: var(--color-primary-dark); transform: translateY(-1px); box-shadow: var(--shadow-md); }

.order-list { display: flex; flex-direction: column; gap: 16px; }
.order-card {
  background: var(--color-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.2s var(--ease-standard);
}
.order-card:hover { box-shadow: var(--shadow-md); }

.order-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; border-bottom: 1px solid var(--color-border-light);
}
.order-meta { display: flex; gap: 16px; align-items: center; }
.order-no { font-size: 14px; color: var(--color-text-secondary); font-family: var(--font-number); }
.order-time { font-size: 13px; color: var(--color-text-hint); }

.status-badge {
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;
}
.status-0 { background: #FFF3E0; color: #E65100; }
.status-1 { background: #E3F2FD; color: #1565C0; }
.status-2 { background: #F3E5F5; color: #7B1FA2; }
.status-3 { background: #E8F5E9; color: #2E7D32; }
.status-4 { background: #FFEBEE; color: #C62828; }

.order-body { padding: 16px 20px; }
.items-list { display: flex; flex-direction: column; gap: 12px; }
.order-item { display: flex; align-items: center; gap: 12px; }
.item-image { width: 56px; height: 56px; border-radius: var(--radius-sm); overflow: hidden; flex-shrink: 0; background: var(--color-bg); }
.item-image img { width: 100%; height: 100%; object-fit: cover; }
.item-info { flex: 1; }
.item-name { font-size: 14px; color: var(--color-title); }
.item-price { font-size: 13px; color: var(--color-text-hint); margin-top: 2px; }
.item-subtotal { font-size: 15px; color: var(--color-danger); font-weight: 600; font-family: var(--font-number); }
.loading-items { text-align: center; color: var(--color-text-hint); font-size: 14px; padding: 12px; }

/* Order Timeline */
.order-timeline {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: var(--color-primary-bg-light);
  border-top: 1px solid var(--color-border-light);
  border-bottom: 1px solid var(--color-border-light);
}
.timeline-step {
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0.4;
  transition: opacity 0.3s var(--ease-standard);
}
.timeline-step.done { opacity: 1; }
.step-dot {
  width: 10px; height: 10px;
  border-radius: 50%;
  background: var(--color-text-hint);
  transition: background 0.3s var(--ease-standard);
}
.timeline-step.done .step-dot { background: var(--color-primary); box-shadow: 0 0 0 3px rgba(26, 93, 58, 0.15); }
.step-info { display: flex; flex-direction: column; }
.step-label { font-size: 12px; font-weight: 500; color: var(--color-text-secondary); }
.step-time { font-size: 11px; color: var(--color-text-hint); }

.timeline-line {
  flex: 1;
  height: 2px;
  background: var(--color-border);
  margin: 0 8px;
  transition: background 0.3s var(--ease-standard);
}
.timeline-line.active { background: var(--color-primary); }

.order-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; background: var(--color-bg); gap: 16px; flex-wrap: wrap;
}
.order-address { font-size: 13px; color: var(--color-text-secondary); display: flex; align-items: center; gap: 4px; flex: 1; min-width: 200px; }
.order-pay-method { font-size: 13px; color: var(--color-text-secondary); display: flex; align-items: center; gap: 4px; }
.pay-method-label { color: var(--color-text-hint, #999); }
.pay-method-value { color: var(--color-primary, #1a5d3a); font-weight: 500; }
.order-total { font-size: 14px; color: var(--color-text-secondary); }
.total-amount { font-size: 20px; font-weight: 700; color: var(--color-danger); font-family: var(--font-number); }
.order-actions { display: flex; gap: 8px; }

.btn-action {
  padding: 8px 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s var(--ease-standard);
  background: var(--color-card);
  color: var(--color-text);
}
.btn-action:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-action:hover:not(:disabled) { box-shadow: var(--shadow-sm); transform: translateY(-1px); }

.btn-primary { background: var(--color-primary); color: white; border-color: var(--color-primary); }
.btn-primary:hover:not(:disabled) { background: var(--color-primary-dark); }

.btn-success { background: var(--color-primary); color: white; border-color: var(--color-primary); }
.btn-success:hover:not(:disabled) { background: var(--color-primary-dark); }

.btn-info { color: var(--color-info); border-color: var(--color-info); }

.btn-default:hover:not(:disabled) { border-color: var(--color-primary); color: var(--color-primary); }

.pagination-bar { display: flex; justify-content: center; margin-top: 20px; }
</style>
