<template>
  <div class="layout">
    <div class="scroll-progress" :style="{ width: scrollProgress + '%' }"></div>

    <!-- Unified Site Header -->
    <header class="site-header" :class="{ scrolled: isScrolled }">
      <!-- Tier 1: Brand Row -->
      <div class="header-brand-row">
        <div class="header-brand-inner">
          <span class="header-utility-left">全链路溯源 · 食品安全</span>
          <span class="header-logo" @click="$router.push('/')">鲜源溯源农品</span>
          <div class="header-utility-right desktop-only">
            <el-dropdown trigger="click">
              <div class="user-box" tabindex="0">
                <span class="username">{{ userInfo?.realName || userInfo?.username || '未登录' }}</span>
                <el-icon size="10"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/profile')">
                    <el-icon><User /></el-icon> 个人中心
                  </el-dropdown-item>
                  <el-dropdown-item v-if="['ADMIN', 'MERCHANT', 'FARMER'].includes(userInfo?.role)" @click="$router.push('/admin')">
                    <el-icon><Setting /></el-icon> 管理后台
                  </el-dropdown-item>
                  <el-dropdown-item @click="logout">
                    <el-icon><SwitchButton /></el-icon> 退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="hamburger mobile-only" @click="drawerOpen = true" tabindex="0">
            <el-icon size="22"><Menu /></el-icon>
          </div>
        </div>
        <div class="header-rule"></div>
      </div>

      <!-- Tier 2: Navigation -->
      <nav class="header-nav desktop-only">
        <div class="header-nav-inner">
          <div class="primary-nav">
            <router-link to="/" class="nav-link" :class="{ active: isActive('/') }">首页</router-link>
            <router-link to="/products" class="nav-link" :class="{ active: isActive('/products') }">全部商品</router-link>
            <router-link to="/seasonal" class="nav-link" :class="{ active: isActive('/seasonal') }">当季时令</router-link>
            <router-link to="/knowledge" class="nav-link" :class="{ active: isActive('/knowledge') }">农业百科</router-link>
            <router-link to="/recipes" class="nav-link" :class="{ active: isActive('/recipes') }">农产食谱</router-link>
            <router-link to="/trace" class="nav-link" :class="{ active: isActive('/trace') }">溯源查询</router-link>
            <router-link to="/origin-map" class="nav-link" :class="{ active: isActive('/origin-map') }">产地地图</router-link>
            <router-link to="/help" class="nav-link" :class="{ active: isActive('/help') }">帮助中心</router-link>
          </div>
          <div class="nav-actions">
            <div class="nav-notification-wrapper" v-if="userInfo">
              <div class="nav-icon-link notification-bell" @click="toggleNotifications" title="消息通知">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
                <span v-if="unreadCount > 0" class="notif-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
              </div>
              <transition name="notif-dropdown">
                <div v-if="showNotifications" class="notif-dropdown">
                  <div class="notif-dropdown-header">
                    <span class="notif-dropdown-title">消息通知</span>
                    <span class="notif-mark-all" @click="markAllRead" v-if="unreadCount > 0">全部已读</span>
                  </div>
                  <div class="notif-dropdown-body">
                    <div v-if="notifications.length === 0" class="notif-empty">暂无通知</div>
                    <div v-for="n in notifications.slice(0, 5)" :key="n.id"
                      class="notif-item" :class="{ unread: n.isRead === 0 }"
                      @click="handleNotifClick(n)">
                      <div class="notif-item-icon" :class="'type-' + n.type">
                        <span v-if="n.type === 0">📢</span>
                        <span v-else-if="n.type === 1">📦</span>
                        <span v-else-if="n.type === 2">🔍</span>
                        <span v-else>🎁</span>
                      </div>
                      <div class="notif-item-content">
                        <div class="notif-item-title">{{ n.title }}</div>
                        <div class="notif-item-desc">{{ n.content }}</div>
                        <div class="notif-item-time">{{ formatNotifTime(n.createTime) }}</div>
                      </div>
                      <div class="notif-unread-dot" v-if="n.isRead === 0"></div>
                    </div>
                  </div>
                  <div class="notif-dropdown-footer" @click="showNotifications = false; $router.push('/notifications')">
                    查看全部通知
                  </div>
                </div>
              </transition>
            </div>
            <router-link to="/favorites" class="nav-icon-link" :class="{ active: isActive('/favorites') }" title="我的收藏">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
            </router-link>
            <router-link to="/orders" class="nav-icon-link" :class="{ active: isActive('/orders') }" title="我的订单">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
            </router-link>
            <router-link to="/cart" class="nav-icon-link cart-icon" :class="{ active: isActive('/cart') }" title="购物车">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
              <span v-if="cartCount > 0" class="cart-count">{{ cartCount }}</span>
            </router-link>
          </div>
        </div>
      </nav>
    </header>

    <el-drawer v-model="drawerOpen" direction="rtl" size="280px" :show-close="false" title="导航菜单">
      <div class="drawer-nav">
        <router-link to="/" class="drawer-item" :class="{ active: isActive('/') }" @click="drawerOpen = false">首页</router-link>
        <router-link to="/products" class="drawer-item" :class="{ active: isActive('/products') }" @click="drawerOpen = false">全部商品</router-link>
        <router-link to="/seasonal" class="drawer-item" :class="{ active: isActive('/seasonal') }" @click="drawerOpen = false">当季时令</router-link>
        <router-link to="/knowledge" class="drawer-item" :class="{ active: isActive('/knowledge') }" @click="drawerOpen = false">农业百科</router-link>
        <router-link to="/recipes" class="drawer-item" :class="{ active: isActive('/recipes') }" @click="drawerOpen = false">农产食谱</router-link>
        <router-link to="/trace" class="drawer-item" :class="{ active: isActive('/trace') }" @click="drawerOpen = false">溯源查询</router-link>
        <router-link to="/trace-process" class="drawer-item" :class="{ active: isActive('/trace-process') }" @click="drawerOpen = false">溯源流程</router-link>
        <router-link to="/origin-map" class="drawer-item" :class="{ active: isActive('/origin-map') }" @click="drawerOpen = false">产地地图</router-link>
        <router-link to="/farmers" class="drawer-item" :class="{ active: isActive('/farmers') }" @click="drawerOpen = false">产地直供</router-link>
        <router-link to="/quality" class="drawer-item" :class="{ active: isActive('/quality') }" @click="drawerOpen = false">品质标准</router-link>
        <router-link to="/announcements" class="drawer-item" :class="{ active: isActive('/announcements') }" @click="drawerOpen = false">公告中心</router-link>
        <router-link to="/help" class="drawer-item" :class="{ active: isActive('/help') }" @click="drawerOpen = false">帮助中心</router-link>
        <router-link to="/about" class="drawer-item" :class="{ active: isActive('/about') }" @click="drawerOpen = false">关于系统</router-link>
        <div class="drawer-divider"></div>
        <router-link to="/cart" class="drawer-item" :class="{ active: isActive('/cart') }" @click="drawerOpen = false">
          购物车 <span v-if="cartCount > 0" class="cart-count">{{ cartCount }}</span>
        </router-link>
        <router-link to="/orders" class="drawer-item" :class="{ active: isActive('/orders') }" @click="drawerOpen = false">我的订单</router-link>
        <router-link to="/favorites" class="drawer-item" :class="{ active: isActive('/favorites') }" @click="drawerOpen = false">我的收藏</router-link>
        <router-link to="/notifications" class="drawer-item" :class="{ active: isActive('/notifications') }" @click="drawerOpen = false">
          消息通知 <span v-if="unreadCount > 0" class="cart-count">{{ unreadCount }}</span>
        </router-link>
      </div>
    </el-drawer>

    <main class="main" :class="{ 'main-scrolled': isScrolled }">
      <router-view />
    </main>

    <!-- Footer -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-top">
          <div class="footer-brand-col">
            <h3 class="footer-logo">鲜源溯源农品</h3>
            <p class="footer-slogan">从田间到餐桌，全程可追溯。让每一口农产品都安全、透明、放心。</p>
            <div class="footer-social">
              <a href="#" class="social-link" aria-label="微信"><svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M8.69 4C4.99 4 2 6.46 2 9.5c0 1.74.99 3.29 2.55 4.31L4 16l2.27-1.17c.72.2 1.49.31 2.3.34-.2-.62-.31-1.28-.31-1.97 0-2.8 2.74-5.07 6.12-5.07.17 0 .33.01.5.02C14.4 5.63 11.83 4 8.69 4zm-2.4 3.2c.45 0 .82.36.82.82s-.36.82-.82.82-.82-.36-.82-.82.37-.82.82-.82zm4.8 0c.45 0 .82.36.82.82s-.36.82-.82.82-.82-.36-.82-.82.37-.82.82-.82z"/><path d="M22 13.2c0-2.64-2.49-4.78-5.56-4.78s-5.56 2.14-5.56 4.78 2.49 4.78 5.56 4.78c.65 0 1.27-.1 1.85-.27L20 18.8l-.65-1.4c1.58-.84 2.65-2.28 2.65-3.92zm-7.3-1.52c.34 0 .62.28.62.62s-.28.62-.62.62-.62-.28-.62-.62.28-.62.62-.62zm3.48 0c.34 0 .62.28.62.62s-.28.62-.62.62-.62-.28-.62-.62.28-.62.62-.62z"/></svg></a>
              <a href="#" class="social-link" aria-label="微博"><svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M9.62 8.46l.84.21-.21.84-.84-.21.21-.84zM10.62 11.86c.36.09.58.46.49.82l-.21.84c-.09.36-.46.58-.82.49l-.21-.02c-.36-.09-.58-.46-.49-.82l.21-.84c.09-.36.46-.58.82-.49l.21.02zm-3.4 1.26c1.32 0 2.38.52 3.16 1.56.78 1.04 1.17 2.32 1.17 3.84 0 1.52-.39 2.8-1.17 3.84-.78 1.04-1.84 1.56-3.16 1.56s-2.38-.52-3.16-1.56C3.39 19.32 3 18.04 3 16.52c0-1.52.39-2.8 1.17-3.84.78-1.04 1.84-1.56 3.16-1.56zm0 6.48c.7 0 1.26-.27 1.68-.82.42-.55.63-1.27.63-2.16s-.21-1.61-.63-2.16c-.42-.55-.98-.82-1.68-.82s-1.26.27-1.68.82c-.42.55-.63 1.27-.63 2.16s.21 1.61.63 2.16c.42.55.98.82 1.68.82zM20.4 6.32c-.33-.33-.79-.49-1.34-.49-.55 0-1.01.16-1.34.49-.33.33-.49.79-.49 1.34 0 .55.16 1.01.49 1.34.33.33.79.49 1.34.49.55 0 1.01-.16 1.34-.49.33-.33.49-.79.49-1.34 0-.55-.16-1.01-.49-1.34z"/><path d="M18.56 12.48c-.42 0-.77.34-.77.77v.15c0 .42.34.77.77.77h.15c.42 0 .77-.34.77-.77v-.15c0-.42-.34-.77-.77-.77h-.15z"/></svg></a>
              <a href="#" class="social-link" aria-label="QQ"><svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 2c2.3 0 4.17 1.87 4.17 4.17 0 .3-.03.59-.09.87.96.36 1.84.98 2.5 1.83 1.42 1.81 1.62 4.35.62 6.79.34.48.54 1.06.54 1.68 0 1.66-1.34 3-3 3-.46 0-.9-.1-1.29-.29-.62.27-1.32.42-2.05.42h-2.8c-.73 0-1.43-.15-2.05-.42-.39.19-.83.29-1.29.29-1.66 0-3-1.34-3-3 0-.62.2-1.2.54-1.68-1-2.44-.8-4.98.62-6.79.66-.85 1.54-1.47 2.5-1.83-.06-.28-.09-.57-.09-.87C7.83 3.87 9.7 2 12 2z"/></svg></a>
            </div>
          </div>
          <div class="footer-links">
            <div class="footer-col">
              <h4>快速导航</h4>
              <router-link to="/">首页</router-link>
              <router-link to="/products">全部商品</router-link>
              <router-link to="/seasonal">当季时令</router-link>
              <router-link to="/trace">溯源查询</router-link>
            </div>
            <div class="footer-col">
              <h4>知识与帮助</h4>
              <router-link to="/knowledge">农业百科</router-link>
              <router-link to="/recipes">农产食谱</router-link>
              <router-link to="/quality">品质标准</router-link>
              <router-link to="/help">帮助中心</router-link>
            </div>
            <div class="footer-col">
              <h4>关于我们</h4>
              <router-link to="/origin-map">产地地图</router-link>
              <router-link to="/farmers">产地直供</router-link>
              <router-link to="/announcements">公告中心</router-link>
              <router-link to="/about">关于系统</router-link>
            </div>
            <div class="footer-col">
              <h4>购物服务</h4>
              <router-link to="/cart">购物车</router-link>
              <router-link to="/orders">我的订单</router-link>
              <router-link to="/favorites">我的收藏</router-link>
            </div>
            <div class="footer-col">
              <h4>联系方式</h4>
              <p><svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.5" style="vertical-align:-2px"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></svg> 400-888-6666</p>
              <p><svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.5" style="vertical-align:-2px"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg> service@traceagri.com</p>
              <p><svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.5" style="vertical-align:-2px"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg> 哈尔滨金融学院</p>
            </div>
          </div>
        </div>
        <div class="footer-divider"></div>
        <div class="footer-bottom">
          <p>鲜源农产品溯源销售平台 &copy; 2026 哈尔滨金融学院 · 毕业设计</p>
          <p class="footer-tech">Powered by Vue3 + SpringBoot · JWT认证 · 全链路溯源</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useCartStore } from '../stores/cart'
import { notificationApi } from '../api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const drawerOpen = ref(false)
const isScrolled = ref(false)
const scrollProgress = ref(0)

const userInfo = computed(() => userStore.userInfo)
const cartCount = computed(() => cartStore.totalCount)

const unreadCount = ref(0)
const notifications = ref([])
const showNotifications = ref(false)
let pollTimer = null

const fetchUnreadCount = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const res = await notificationApi.unreadCount()
    unreadCount.value = res.data?.count || 0
  } catch {}
}

const fetchNotifications = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const res = await notificationApi.list()
    notifications.value = res.data || []
  } catch {}
}

const toggleNotifications = () => {
  showNotifications.value = !showNotifications.value
  if (showNotifications.value && notifications.value.length === 0) {
    fetchNotifications()
  }
}

const handleNotifClick = async (n) => {
  if (n.isRead === 0) {
    try {
      await notificationApi.markRead(n.id)
      n.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch {}
  }
  showNotifications.value = false
  if (n.linkUrl) router.push(n.linkUrl)
}

const markAllRead = async () => {
  try {
    await notificationApi.markAllRead()
    notifications.value.forEach(n => n.isRead = 1)
    unreadCount.value = 0
  } catch {}
}

const formatNotifTime = (time) => {
  if (!time) return ''
  const d = new Date(time.replace(/-/g, '/'))
  const now = new Date()
  const diff = (now - d) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  if (diff < 2592000) return Math.floor(diff / 86400) + '天前'
  return time.substring(0, 10)
}

const closeNotifications = (e) => {
  if (showNotifications.value && !e.target.closest('.nav-notification-wrapper')) {
    showNotifications.value = false
  }
}

const isActive = (path) => {
  if (path === '/') return route.path === '/' || route.path.startsWith('/product/')
  return route.path === path || route.path.startsWith(path + '/')
}

const handleScroll = () => {
  const scrollTop = window.scrollY
  if (scrollTop > 40) isScrolled.value = true
  else if (scrollTop < 10) isScrolled.value = false
  const scrollHeight = document.documentElement.scrollHeight - window.innerHeight
  scrollProgress.value = scrollHeight > 0 ? (scrollTop / scrollHeight) * 100 : 0
}

const logout = () => {
  userStore.logout()
  cartStore.clearCart()
  router.push('/login')
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    cartStore.fetchCart()
    fetchUnreadCount()
    pollTimer = setInterval(fetchUnreadCount, 30000)
  }
  window.addEventListener('scroll', handleScroll, { passive: true })
  document.addEventListener('click', closeNotifications)
  handleScroll()
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  document.removeEventListener('click', closeNotifications)
  if (pollTimer) clearInterval(pollTimer)
})

watch(() => route.path, () => {
  if (userStore.isLoggedIn) fetchUnreadCount()
})
</script>

<style scoped>
.layout { min-height: 100vh; display: flex; flex-direction: column; }

/* === Unified Site Header === */
.site-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 200;
  background: var(--bg-card);
  transition: box-shadow var(--duration-fast) var(--ease-out);
}
.site-header.scrolled {
  box-shadow: var(--shadow-sm);
}
.site-header.scrolled .header-brand-row {
  max-height: 0;
  opacity: 0;
}
.site-header.scrolled .header-rule {
  opacity: 0;
  margin: 0;
}

/* Tier 1: Brand Row */
.header-brand-row {
  max-height: 120px;
  overflow: hidden;
  transition: max-height 0.3s var(--ease-out), opacity 0.3s var(--ease-out);
}
.header-brand-inner {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28px 24px 18px;
  gap: 16px;
}
.header-utility-left,
.header-utility-right {
  flex: 1;
  font-size: 11px;
  letter-spacing: 0.15em;
  color: var(--text-tertiary, #aaa);
  font-weight: 400;
}
.header-utility-left { text-align: left; }
.header-utility-right { text-align: right; display: flex; justify-content: flex-end; align-items: center; }
.header-logo {
  font-family: var(--font-serif, Georgia, serif);
  font-size: 36px;
  font-weight: 600;
  color: var(--brand-primary, #1a5d3a);
  cursor: pointer;
  letter-spacing: 0.04em;
  white-space: nowrap;
  transition: font-size 0.3s var(--ease-out);
}
.header-rule {
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--border-color, #ddd) 20%, var(--border-color, #ddd) 80%, transparent);
  margin: 0 24px;
  transition: opacity 0.3s var(--ease-out);
}

/* Tier 2: Navigation */
.header-nav {
  border-bottom: 1px solid var(--border-light, #eee);
}
.header-nav-inner {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24px;
  height: 46px;
}
.primary-nav {
  display: flex;
  align-items: center;
  gap: 22px;
}
.nav-link {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--text-primary, #333);
  text-decoration: none;
  padding: 14px 0;
  position: relative;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  transition: color var(--duration-fast);
}
.nav-link:hover { color: var(--brand-primary, #1a5d3a); }
.nav-link.active { color: var(--brand-primary, #1a5d3a); }
.nav-link.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--brand-primary, #1a5d3a);
}
.more-link { cursor: pointer; }

.nav-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-left: 36px;
  padding-left: 36px;
  border-left: 1px solid var(--border-light, #e8e8e8);
}
.nav-icon-link {
  position: relative;
  color: var(--text-secondary, #666);
  text-decoration: none;
  display: flex;
  align-items: center;
  transition: color var(--duration-fast);
}
.nav-icon-link:hover { color: var(--brand-primary, #1a5d3a); }
.nav-icon-link.active { color: var(--brand-primary, #1a5d3a); }
.notification-bell { cursor: pointer; }
/* Notification Dropdown */
.nav-notification-wrapper { position: relative; }
.notif-badge {
  position: absolute; top: -4px; right: -4px;
  background: #e74c3c; color: white; font-size: 10px; font-weight: 700;
  min-width: 16px; height: 16px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  padding: 0 4px; line-height: 1;
}
.notif-dropdown {
  position: absolute; top: calc(100% + 8px); right: 0;
  width: 360px; background: #fff; border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.12); z-index: 1000;
  overflow: hidden; border: 1px solid #f0f0f0;
}
.notif-dropdown-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 16px; border-bottom: 1px solid #f5f5f5;
}
.notif-dropdown-title { font-size: 15px; font-weight: 700; color: #1a5d3a; }
.notif-mark-all { font-size: 12px; color: #2d8c3c; cursor: pointer; }
.notif-mark-all:hover { text-decoration: underline; }
.notif-dropdown-body { max-height: 360px; overflow-y: auto; }
.notif-empty { padding: 40px 0; text-align: center; color: #999; font-size: 13px; }
.notif-item {
  display: flex; gap: 10px; padding: 12px 16px;
  border-bottom: 1px solid #f8f8f8; cursor: pointer;
  transition: background 0.15s; position: relative;
}
.notif-item:hover { background: #f9fbf9; }
.notif-item.unread { background: #f0f7f0; }
.notif-item.unread:hover { background: #e8f3e8; }
.notif-item-icon {
  width: 32px; height: 32px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; flex-shrink: 0;
}
.notif-item-icon.type-0 { background: #e3f2fd; }
.notif-item-icon.type-1 { background: #fff3e0; }
.notif-item-icon.type-2 { background: #e8f5e9; }
.notif-item-icon.type-3 { background: #fce4ec; }
.notif-item-content { flex: 1; min-width: 0; }
.notif-item-title { font-size: 13px; font-weight: 600; color: #333; margin-bottom: 2px; }
.notif-item-desc { font-size: 12px; color: #888; line-height: 1.5;
  overflow: hidden; text-overflow: ellipsis; display: -webkit-box;
  -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.notif-item-time { font-size: 11px; color: #bbb; margin-top: 4px; }
.notif-unread-dot { position: absolute; top: 50%; right: 12px; transform: translateY(-50%);
  width: 8px; height: 8px; border-radius: 50%; background: #e74c3c; }
.notif-dropdown-footer {
  padding: 12px; text-align: center; font-size: 13px;
  color: #2d8c3c; cursor: pointer; border-top: 1px solid #f5f5f5;
  transition: background 0.15s;
}
.notif-dropdown-footer:hover { background: #f9fbf9; }
.notif-dropdown-enter-active, .notif-dropdown-leave-active { transition: all 0.2s; }
.notif-dropdown-enter-from, .notif-dropdown-leave-to { opacity: 0; transform: translateY(-8px); }
.cart-count {
  position: absolute;
  top: -8px;
  right: -10px;
  background: var(--brand-primary, #1a5d3a);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  min-width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  border-radius: 9999px;
}
.user-box {
  display: flex; align-items: center; gap: 6px;
  cursor: pointer;
  font-size: 11px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-tertiary, #aaa);
  font-weight: 500;
}
.username { white-space: nowrap; }
.hamburger { cursor: pointer; padding: 4px; }

/* Drawer */
.drawer-nav { display: flex; flex-direction: column; gap: 2px; }
.drawer-item {
  font-size: 13px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-primary);
  text-decoration: none;
  padding: 14px 16px;
  transition: all var(--duration-fast);
}
.drawer-item:hover { color: var(--brand-primary); }
.drawer-item.active { color: var(--brand-primary); }
.drawer-divider { height: 1px; background: var(--border-light); margin: 8px 0; }

/* Main */
.main { flex: 1; max-width: 1280px; width: 100%; margin: 0 auto; padding: 140px 24px 48px; transition: padding-top 0.3s var(--ease-out); }
.main.main-scrolled { padding-top: 60px; }

/* Footer */
.footer {
  background: var(--cream-100);
  border-top: 1px solid var(--border-color);
  margin-top: 64px;
}
.footer-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 64px 24px 32px;
}
.footer-top {
  display: flex;
  gap: 64px;
  justify-content: space-between;
  margin-bottom: 48px;
}
.footer-brand-col {
  max-width: 320px;
}
.footer-logo {
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: 12px;
}
.footer-slogan {
  font-size: 14px;
  line-height: 1.8;
  color: var(--color-text-secondary);
  margin-bottom: 20px;
}
.footer-social {
  display: flex;
  gap: 12px;
}
.social-link {
  width: 36px; height: 36px;
  border-radius: 50%;
  background: var(--color-primary-bg);
  color: var(--color-primary);
  display: flex; align-items: center; justify-content: center;
  text-decoration: none;
  transition: all 0.25s var(--ease-out);
}
.social-link:hover {
  background: var(--color-primary);
  color: white;
  transform: translateY(-2px);
}
.footer-links {
  display: flex;
  gap: 48px;
}
.footer-col h4 {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-title);
  margin-bottom: 16px;
}
.footer-col a, .footer-col p {
  display: block;
  font-size: 13px;
  color: var(--color-text-secondary);
  text-decoration: none;
  padding: 4px 0;
  transition: color 0.25s;
  line-height: 1.6;
}
.footer-col a:hover {
  color: var(--color-primary);
}
.footer-divider {
  height: 1px;
  background: var(--border-color);
  margin-bottom: 24px;
}
.footer-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.footer-bottom p {
  font-size: 12px;
  color: var(--text-tertiary);
  letter-spacing: 0.03em;
}
.footer-tech {
  font-size: 11px !important;
  letter-spacing: 0.1em;
}

.desktop-only { display: flex; }
.mobile-only { display: none; }

@media (max-width: 768px) {
  .desktop-only { display: none !important; }
  .mobile-only { display: flex !important; }
  .main { padding: 80px 16px 24px; }
  .main.main-scrolled { padding-top: 80px; }
  .site-header.scrolled .header-brand-row { max-height: 120px; opacity: 1; }
  .header-brand-inner { padding: 20px 16px 14px; }
  .header-logo { font-size: 28px; }
  .header-utility-left { font-size: 10px; }
  .header-nav-inner { padding: 0 16px; }
  .footer-top { flex-direction: column; gap: 40px; }
  .footer-links { flex-wrap: wrap; gap: 32px; }
  .footer-bottom { flex-direction: column; gap: 8px; text-align: center; }
}
</style>
