<template>
  <el-container class="admin-layout">
    <!-- Mobile overlay -->
    <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false"></div>

    <el-aside width="220px" class="sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-logo">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="white" stroke-width="2.5">
            <path d="M12 2L4 7v6c0 5 3.5 8.5 8 10 4.5-1.5 8-5 8-10V7l-8-5z"/>
          </svg>
        </div>
        <span class="logo-text">鲜源溯源管理系统</span>
      </div>
      <el-menu :default-active="activeMenu" router class="sidebar-menu" :collapse="false" @select="onMenuSelect">
        <el-menu-item index="/admin" v-if="canView('dashboard')">
          <el-icon><DataBoard /></el-icon><span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/admin/statistics" v-if="canView('statistics')">
          <el-icon><TrendCharts /></el-icon><span>数据统计</span>
        </el-menu-item>
        <div class="menu-divider"></div>
        <el-menu-item index="/admin/products" v-if="canView('products')">
          <el-icon><Goods /></el-icon><span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/stock-warning" v-if="canView('stockWarning')">
          <el-icon><Warning /></el-icon><span>库存预警</span>
        </el-menu-item>
        <el-menu-item index="/admin/categories" v-if="canView('categories')">
          <el-icon><Folder /></el-icon><span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders" v-if="canView('orders')">
          <el-icon><Document /></el-icon><span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/shipments" v-if="canView('shipments')">
          <el-icon><Van /></el-icon><span>待发货工作台</span>
        </el-menu-item>
        <div class="menu-divider"></div>
        <el-menu-item index="/admin/trace" v-if="canView('trace')">
          <el-icon><Connection /></el-icon><span>溯源管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/origins" v-if="canView('origins')">
          <el-icon><Location /></el-icon><span>产地信息管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/farmers" v-if="canView('farmers')">
          <el-icon><UserFilled /></el-icon><span>农户管理</span>
        </el-menu-item>
        <div class="menu-divider"></div>
        <el-menu-item index="/admin/reviews" v-if="canView('reviews')">
          <el-icon><ChatDotRound /></el-icon><span>评价管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/announcements" v-if="canView('announcements')">
          <el-icon><Bell /></el-icon><span>公告管理</span>
        </el-menu-item>
        <div class="menu-divider"></div>
        <el-menu-item index="/admin/knowledge" v-if="canView('knowledge')">
          <el-icon><Reading /></el-icon><span>百科管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/recipes" v-if="canView('recipes')">
          <el-icon><Food /></el-icon><span>食谱管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/faqs" v-if="canView('faqs')">
          <el-icon><QuestionFilled /></el-icon><span>FAQ管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/seasonal" v-if="canView('seasonal')">
          <el-icon><Calendar /></el-icon><span>时令管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/users" v-if="canView('users')">
          <el-icon><User /></el-icon><span>用户管理</span>
        </el-menu-item>
        <div class="menu-divider"></div>
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon><span>返回前台</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-header">
        <div class="header-left">
          <button class="menu-toggle" @click="sidebarOpen = !sidebarOpen">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 12h18M3 6h18M3 18h18"/>
            </svg>
          </button>
          <div class="header-title">
            <h2>{{ pageTitle }}</h2>
            <p class="header-subtitle">{{ pageSubtitle }}</p>
          </div>
        </div>
        <div class="header-user">
          <!-- 头像区域做成下拉触发：返回前端走左侧菜单，退出登录收进这里 -->
          <el-dropdown trigger="click" @command="onUserCommand">
            <div class="user-trigger">
              <div class="avatar">{{ (userInfo?.username || 'A').charAt(0).toUpperCase() }}</div>
              <div class="user-info-text">
                <div class="user-name">{{ userInfo?.realName || userInfo?.username }}</div>
                <div class="user-role">{{ roleLabel }}</div>
              </div>
              <svg class="trigger-caret" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9"/>
              </svg>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const sidebarOpen = ref(false)
const userInfo = computed(() => userStore.userInfo)
const userRole = computed(() => userInfo.value?.role || '')
const roleLabel = computed(() => {
  const r = userRole.value
  if (r === 'ADMIN') return '系统管理员'
  if (r === 'MERCHANT') return '商家'
  if (r === 'FARMER') return '农户'
  return '客户'
})

const rolePermissions = {
  dashboard: ['ADMIN', 'MERCHANT', 'FARMER'],
  statistics: ['ADMIN', 'MERCHANT'],
  products: ['ADMIN', 'MERCHANT'],
  stockWarning: ['ADMIN', 'MERCHANT'],
  categories: ['ADMIN'],
  orders: ['ADMIN', 'MERCHANT'],
  shipments: ['ADMIN', 'MERCHANT'],
  trace: ['ADMIN', 'MERCHANT', 'FARMER'],
  origins: ['ADMIN', 'FARMER'],
  farmers: ['ADMIN', 'FARMER'],
  reviews: ['ADMIN', 'MERCHANT'],
  announcements: ['ADMIN'],
  knowledge: ['ADMIN'],
  recipes: ['ADMIN'],
  faqs: ['ADMIN'],
  seasonal: ['ADMIN'],
  users: ['ADMIN']
}
const canView = (menu) => rolePermissions[menu]?.includes(userRole.value) || false

const activeMenu = computed(() => route.path)

const onMenuSelect = () => {
  sidebarOpen.value = false
}

/** 头像下拉的指令分发：目前只有退出登录一项 */
const onUserCommand = (command) => {
  if (command === 'logout') logout()
}

/** 退出登录：二次确认后清空登录态并回到登录页 */
const logout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    userStore.logout()
    router.push('/login')
  } catch {}
}

const pageTitle = computed(() => {
  const map = {
    '/admin': '仪表盘',
    '/admin/statistics': '数据统计',
    '/admin/products': '商品管理',
    '/admin/stock-warning': '库存预警',
    '/admin/categories': '分类管理',
    '/admin/orders': '订单管理',
    '/admin/shipments': '待发货工作台',
    '/admin/trace': '溯源管理',
    '/admin/origins': '产地信息管理',
    '/admin/farmers': '农户管理',
    '/admin/reviews': '评价管理',
    '/admin/announcements': '公告管理',
    '/admin/knowledge': '百科管理',
    '/admin/recipes': '食谱管理',
    '/admin/faqs': 'FAQ管理',
    '/admin/seasonal': '时令管理',
    '/admin/users': '用户管理'
  }
  return map[route.path] || '管理后台'
})

const pageSubtitle = computed(() => {
  const map = {
    '/admin': '系统数据总览',
    '/admin/statistics': '销售数据与平台指标分析',
    '/admin/products': '管理商品信息和库存',
    '/admin/stock-warning': '低于预警阈值的商品，及时补货避免断单',
    '/admin/categories': '管理商品分类',
    '/admin/orders': '查看和管理所有订单',
    '/admin/shipments': '集中处理已付款待发货的订单，支持批量发货',
    '/admin/trace': '管理溯源批次和溯源信息',
    '/admin/origins': '维护各溯源批次的产地溯源信息',
    '/admin/farmers': '管理农户和种植信息',
    '/admin/reviews': '管理商品评价',
    '/admin/announcements': '发布和管理平台公告',
    '/admin/knowledge': '管理农业百科文章',
    '/admin/recipes': '管理农产食谱',
    '/admin/faqs': '管理常见问题',
    '/admin/seasonal': '管理商品时令关联',
    '/admin/users': '管理系统用户'
  }
  return map[route.path] || ''
})
</script>

<style scoped>
.admin-layout { min-height: 100vh; }

/* Desktop: sidebar always visible */
.sidebar {
  background: #1f2d3d;
  height: 100vh;
  position: sticky;
  top: 0;
  transition: width 0.3s ease, transform 0.3s ease;
  /* 纵向 flex：logo 固定，菜单区独立滚动。原为 overflow:hidden，
     菜单项增多（含新增「产地信息管理」）后会溢出被裁且无法滚动 */
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.05);
  flex-shrink: 0;   /* logo 不参与压缩，保持固定 */
}
.logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #2d8c3c, #1a5c25);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.logo-text {
  color: white;
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}
.sidebar-menu {
  background: transparent;
  border-right: none;
  padding: 8px;
  /* 占据 logo 之外的剩余高度，内容超出时自身滚动（菜单项多也不裁切） */
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}
/* 滚动条细窄化，避免深色侧栏上出现过宽的白色滚动条 */
.sidebar-menu::-webkit-scrollbar {
  width: 6px;
}
.sidebar-menu::-webkit-scrollbar-thumb {
  background: rgba(255,255,255,0.18);
  border-radius: 3px;
}
.sidebar-menu::-webkit-scrollbar-thumb:hover {
  background: rgba(255,255,255,0.3);
}
.sidebar-menu::-webkit-scrollbar-track {
  background: transparent;
}
.sidebar-menu :deep(.el-menu-item) {
  color: #a0aec0;
  border-radius: 8px;
  margin-bottom: 4px;
  height: 44px;
  line-height: 44px;
}
.sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(255,255,255,0.05);
  color: white;
}
.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #2d8c3c, #1a5c25);
  color: white;
  box-shadow: 0 4px 12px rgba(45,140,60,0.3);
}
.menu-divider {
  height: 1px;
  background: rgba(255,255,255,0.05);
  margin: 12px 16px;
}

/* Mobile overlay */
.sidebar-overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  z-index: 999;
}

/* Menu toggle button */
.menu-toggle {
  display: none;
  background: none;
  border: none;
  cursor: pointer;
  color: #333;
  padding: 6px;
  border-radius: 6px;
  align-items: center;
  justify-content: center;
}
.menu-toggle:hover { background: #f5f5f5; }

.admin-header {
  background: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 72px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-title h2 {
  font-size: 20px;
  color: #1a1a1a;
}
.header-subtitle {
  font-size: 13px;
  color: #999;
  margin-top: 2px;
}
.header-user {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 10px 4px 4px;
  border-radius: 22px;
  cursor: pointer;
  transition: background 0.2s;
  outline: none;
}
.user-trigger:hover { background: #f5f5f5; }
.trigger-caret { color: #999; flex-shrink: 0; }
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2d8c3c, #3da655);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}
.user-info-text { white-space: nowrap; }
.user-name {
  font-size: 14px;
  color: #333;
}
.user-role {
  font-size: 12px;
  color: #999;
}
.content {
  background: #f0f2f5;
  padding: 24px;
  min-height: calc(100vh - 72px);
}

/* Mobile responsive */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    z-index: 1000;
    width: 220px !important;
    transform: translateX(-100%);
  }
  .sidebar.open {
    transform: translateX(0);
  }
  .sidebar-overlay {
    display: block;
  }
  .menu-toggle {
    display: flex;
  }
  .admin-header {
    padding: 0 12px;
    height: 56px;
  }
  .header-title h2 {
    font-size: 16px;
  }
  .header-subtitle {
    display: none;
  }
  .user-info-text {
    display: none;
  }
  .content {
    padding: 12px;
    min-height: calc(100vh - 56px);
  }
}
</style>
