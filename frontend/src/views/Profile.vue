<template>
  <div class="profile-page">
    <!-- Profile Header -->
    <div class="profile-header">
      <div class="avatar" :style="{ background: avatarColor }">
        {{ (userInfo.realName || userInfo.username || '?').charAt(0).toUpperCase() }}
      </div>
      <div class="profile-info">
        <h1 class="profile-name">{{ userInfo.realName || userInfo.username }}</h1>
        <div class="profile-meta">
          <span class="meta-item">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            {{ userInfo.username }}
          </span>
          <span class="meta-item" v-if="userInfo.phone">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
            {{ userInfo.phone }}
          </span>
          <span class="meta-item" v-if="userInfo.email">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
            {{ userInfo.email }}
          </span>
          <span class="role-badge" :class="userInfo.role?.toLowerCase()">{{ roleLabel }}</span>
        </div>
      </div>
      <button class="btn-edit" @click="openEdit">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
        编辑资料
      </button>
    </div>

    <!-- Order Stats -->
    <div class="stats-row">
      <div class="stat-card" @click="$router.push('/orders')">
        <div class="stat-icon icon-blue">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/></svg>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ orderStats.total }}</div>
          <div class="stat-label">全部订单</div>
        </div>
      </div>
      <div class="stat-card" @click="$router.push('/orders')">
        <div class="stat-icon icon-orange">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12,6 12,12 16,14"/></svg>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ orderStats.pending }}</div>
          <div class="stat-label">待付款</div>
        </div>
      </div>
      <div class="stat-card" @click="$router.push('/orders')">
        <div class="stat-icon icon-purple">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ orderStats.paid + orderStats.shipped }}</div>
          <div class="stat-label">处理中</div>
        </div>
      </div>
      <div class="stat-card" @click="$router.push('/orders')">
        <div class="stat-icon icon-green">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22,4 12,14.01 9,11.01"/></svg>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ orderStats.completed }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>
      <div class="stat-card revenue">
        <div class="stat-icon icon-red">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
        </div>
        <div class="stat-body">
          <div class="stat-value">￥{{ orderStats.totalAmount.toFixed(2) }}</div>
          <div class="stat-label">累计消费</div>
        </div>
      </div>
    </div>

    <!-- Recent Orders -->
    <div class="section">
      <div class="section-header">
        <h2>最近订单</h2>
        <span class="view-all" @click="$router.push('/orders')">查看全部 →</span>
      </div>
      <div v-if="recentOrders.length === 0" class="empty-section">
        <p>暂无订单</p>
        <button class="btn-go-shop" @click="$router.push('/')">去购物</button>
      </div>
      <div v-else class="recent-order-list">
        <div v-for="order in recentOrders" :key="order.id" class="recent-order">
          <div class="ro-left">
            <span class="ro-no">{{ order.orderNo }}</span>
            <span class="ro-time">{{ formatTime(order.createTime) }}</span>
          </div>
          <div class="ro-items">
            <span v-if="order.orderItems && order.orderItems.length > 0">
              {{ order.orderItems.map(i => i.productName).join('、') }}
            </span>
            <span v-else>订单商品</span>
          </div>
          <div class="ro-right">
            <span class="ro-amount">￥{{ order.totalAmount }}</span>
            <span class="ro-status" :class="'status-' + order.status">{{ statusLabels[order.status] }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Edit Dialog -->
    <el-dialog v-model="editDialog" title="编辑个人资料" width="420px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input :model-value="userInfo.username" disabled />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="editForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi, orderApi } from '../api'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const userInfo = ref({})
const orders = ref([])
const editDialog = ref(false)
const saving = ref(false)
const editForm = ref({ realName: '', phone: '', email: '' })

const statusLabels = ['待付款', '已付款', '已发货', '已完成', '已取消']

const avatarColors = ['#1a5d3a', '#2196F3', '#9C27B0', '#E65100', '#00695C']
const avatarColor = computed(() => {
  const name = userInfo.value.username || ''
  const idx = name.charCodeAt(0) % avatarColors.length
  return avatarColors[idx]
})

const roleLabel = computed(() => {
  const role = userInfo.value.role
  if (role === 'ADMIN') return '系统管理员'
  if (role === 'MERCHANT') return '商家'
  if (role === 'FARMER') return '农户'
  return '客户'
})

const orderStats = computed(() => {
  const list = orders.value
  return {
    total: list.length,
    pending: list.filter(o => o.status === 0).length,
    paid: list.filter(o => o.status === 1).length,
    shipped: list.filter(o => o.status === 2).length,
    completed: list.filter(o => o.status === 3).length,
    totalAmount: list
      .filter(o => o.status !== 4)
      .reduce((s, o) => s + (o.totalAmount || 0), 0)
  }
})

const recentOrders = computed(() => {
  return [...orders.value].sort((a, b) => {
    const aTime = new Date(a.createTime).getTime() || 0
    const bTime = new Date(b.createTime).getTime() || 0
    return bTime - aTime
  }).slice(0, 5)
})

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : ''

const loadProfile = async () => {
  const local = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (!local.id) return
  userInfo.value = local
  try {
    const res = await userApi.getById(local.id)
    if (res.data) {
      userInfo.value = { ...local, ...res.data }
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    }
  } catch (e) { /* use local data */ }

  try {
    const orderRes = await orderApi.list(local.id)
    orders.value = orderRes.data || []
  } catch (e) { /* ignore */ }
}

/**
 * 打开编辑资料弹窗。
 * 每次打开都从当前 userInfo 重新回填，而不是只在 onMounted 赋一次值 ——
 * loadProfile 是异步的，挂载时 userInfo 还是空对象，早赋值会把空格子带进弹窗。
 * 资料全部非必填，留空即保存为空，不做强制校验。
 */
const openEdit = () => {
  editForm.value = {
    realName: userInfo.value.realName || '',
    phone: userInfo.value.phone || '',
    email: userInfo.value.email || ''
  }
  editDialog.value = true
}

const saveProfile = async () => {
  saving.value = true
  try {
    await userApi.update({
      id: userInfo.value.id,
      realName: editForm.value.realName,
      phone: editForm.value.phone,
      email: editForm.value.email
    })
    userInfo.value = { ...userInfo.value, ...editForm.value }
    userStore.updateUserInfo(editForm.value)
    ElMessage.success('保存成功')
    editDialog.value = false
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.profile-page { max-width: 900px; margin: 0 auto; }

/* Header */
.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
  background: var(--color-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 20px;
}
.avatar {
  width: 72px; height: 72px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 32px; font-weight: 700; color: white;
  flex-shrink: 0;
}
.profile-info { flex: 1; }
.profile-name { font-size: 22px; font-weight: 700; color: var(--color-title); margin-bottom: 8px; font-family: var(--font-heading); }
.profile-meta { display: flex; gap: 16px; flex-wrap: wrap; align-items: center; }
.meta-item { font-size: 13px; color: var(--color-text-secondary); display: flex; align-items: center; gap: 4px; }
.role-badge {
  padding: 2px 10px; border-radius: var(--radius-full); font-size: 12px; font-weight: 500;
}
.role-badge.admin { background: #FFF3E0; color: #E65100; }
.role-badge.merchant { background: #FFF8E1; color: #F57F17; }
.role-badge.farmer { background: #E3F2FD; color: #1565C0; }
.role-badge.consumer { background: #E8F5E9; color: #2E7D32; }
.role-badge.user { background: #E8F5E9; color: #2E7D32; }

.btn-edit {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 16px; border: 1px solid var(--color-border);
  background: var(--color-card); border-radius: var(--radius-full);
  font-size: 13px; color: var(--color-text-secondary);
  cursor: pointer; transition: all 0.2s var(--ease-standard);
}
.btn-edit:hover { border-color: var(--color-primary); color: var(--color-primary); }

/* Stats */
.stats-row { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-bottom: 20px; }
.stat-card {
  background: var(--color-card); border-radius: var(--radius-md); padding: 16px;
  display: flex; align-items: center; gap: 12px; box-shadow: var(--shadow-sm);
  cursor: pointer; transition: transform 0.2s var(--ease-standard), box-shadow 0.2s var(--ease-standard);
}
.stat-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.stat-card.revenue { flex: 1.5; }
.stat-icon { width: 40px; height: 40px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.icon-blue { background: #E3F2FD; color: #1565C0; }
.icon-orange { background: #FFF3E0; color: #E65100; }
.icon-purple { background: #F3E5F5; color: #7B1FA2; }
.icon-green { background: #E8F5E9; color: #2E7D32; }
.icon-red { background: #FFEBEE; color: #C62828; }
.stat-value { font-size: 20px; font-weight: 700; color: var(--color-title); font-family: var(--font-number); }
.stat-label { font-size: 12px; color: var(--color-text-hint); margin-top: 2px; }

/* Section */
.section { background: var(--color-card); border-radius: var(--radius-lg); padding: 20px; box-shadow: var(--shadow-sm); }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--color-border-light); }
.section-header h2 { font-size: 16px; color: var(--color-title); }
.view-all { font-size: 13px; color: var(--color-primary); cursor: pointer; }
.view-all:hover { text-decoration: underline; }

.empty-section { text-align: center; padding: 40px; color: var(--color-text-hint); }
.empty-section p { margin-bottom: 16px; }
.btn-go-shop {
  padding: 8px 24px; background: var(--color-primary); color: white;
  border: none; border-radius: var(--radius-full); font-size: 14px; cursor: pointer;
  transition: all 0.2s var(--ease-standard);
}
.btn-go-shop:hover { background: var(--color-primary-dark); }

/* Recent Orders */
.recent-order-list { display: flex; flex-direction: column; gap: 0; }
.recent-order {
  display: flex; align-items: center; gap: 16px;
  padding: 14px 0; border-bottom: 1px solid var(--color-border-light);
}
.recent-order:last-child { border-bottom: none; }
.ro-left { display: flex; flex-direction: column; gap: 2px; min-width: 160px; }
.ro-no { font-size: 13px; color: var(--color-text-secondary); font-family: var(--font-number); }
.ro-time { font-size: 12px; color: var(--color-text-hint); }
.ro-items { flex: 1; font-size: 14px; color: var(--color-title); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ro-right { display: flex; align-items: center; gap: 12px; }
.ro-amount { font-size: 16px; font-weight: 700; color: var(--color-danger); font-family: var(--font-number); }
.ro-status { font-size: 12px; padding: 2px 10px; border-radius: var(--radius-full); }
.status-0 { background: #FFF3E0; color: #E65100; }
.status-1 { background: #E3F2FD; color: #1565C0; }
.status-2 { background: #F3E5F5; color: #7B1FA2; }
.status-3 { background: #E8F5E9; color: #2E7D32; }
.status-4 { background: #FFEBEE; color: #C62828; }
</style>
