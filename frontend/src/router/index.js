import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('../views/Home.vue') },
      { path: 'products', name: 'ProductList', component: () => import('../views/ProductList.vue') },
      { path: 'product/:id', name: 'ProductDetail', component: () => import('../views/ProductDetail.vue') },
      { path: 'trace', name: 'TraceQuery', component: () => import('../views/TraceQuery.vue') },
      { path: 'trace-process', name: 'TraceProcess', component: () => import('../views/TraceProcess.vue') },
      { path: 'farmers', name: 'Farmers', component: () => import('../views/Farmers.vue') },
      { path: 'farmer/:id', name: 'FarmerDetail', component: () => import('../views/FarmerDetail.vue') },
      { path: 'announcements', name: 'Announcements', component: () => import('../views/Announcements.vue') },
      { path: 'about', name: 'About', component: () => import('../views/About.vue') },
      { path: 'seasonal', name: 'Seasonal', component: () => import('../views/Seasonal.vue') },
      { path: 'knowledge', name: 'Knowledge', component: () => import('../views/Knowledge.vue') },
      { path: 'knowledge/:id', name: 'KnowledgeDetail', component: () => import('../views/KnowledgeDetail.vue') },
      { path: 'recipes', name: 'Recipes', component: () => import('../views/Recipes.vue') },
      { path: 'recipe/:id', name: 'RecipeDetail', component: () => import('../views/RecipeDetail.vue') },
      { path: 'origin-map', name: 'OriginMap', component: () => import('../views/OriginMap.vue') },
      { path: 'help', name: 'Help', component: () => import('../views/Help.vue') },
      { path: 'quality', name: 'QualityStandards', component: () => import('../views/QualityStandards.vue') },
      { path: 'cart', name: 'Cart', component: () => import('../views/Cart.vue') },
      { path: 'orders', name: 'Orders', component: () => import('../views/Orders.vue') },
      { path: 'favorites', name: 'Favorites', component: () => import('../views/Favorites.vue') },
      { path: 'notifications', name: 'Notifications', component: () => import('../views/Notifications.vue') },
      { path: 'profile', name: 'Profile', component: () => import('../views/Profile.vue') }
    ]
  },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    children: [
      { path: '', name: 'Dashboard', component: () => import('../views/admin/Dashboard.vue'), meta: { roles: ['ADMIN', 'MERCHANT', 'FARMER'] } },
      { path: 'products', name: 'ProductManage', component: () => import('../views/admin/ProductManage.vue'), meta: { roles: ['ADMIN', 'MERCHANT'] } },
      { path: 'stock-warning', name: 'StockWarning', component: () => import('../views/admin/StockWarning.vue'), meta: { roles: ['ADMIN', 'MERCHANT'] } },
      { path: 'categories', name: 'CategoryManage', component: () => import('../views/admin/CategoryManage.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'orders', name: 'OrderManage', component: () => import('../views/admin/OrderManage.vue'), meta: { roles: ['ADMIN', 'MERCHANT'] } },
      { path: 'shipments', name: 'ShipWorkbench', component: () => import('../views/admin/ShipWorkbench.vue'), meta: { roles: ['ADMIN', 'MERCHANT'] } },
      { path: 'trace', name: 'TraceManage', component: () => import('../views/admin/TraceManage.vue'), meta: { roles: ['ADMIN', 'MERCHANT', 'FARMER'] } },
      { path: 'origins', name: 'OriginManage', component: () => import('../views/admin/OriginManage.vue'), meta: { roles: ['ADMIN', 'FARMER'] } },
      { path: 'farmers', name: 'FarmerManage', component: () => import('../views/admin/FarmerManage.vue'), meta: { roles: ['ADMIN', 'FARMER'] } },
      { path: 'reviews', name: 'ReviewManage', component: () => import('../views/admin/ReviewManage.vue'), meta: { roles: ['ADMIN', 'MERCHANT'] } },
      { path: 'knowledge', name: 'KnowledgeManage', component: () => import('../views/admin/KnowledgeManage.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'recipes', name: 'RecipeManage', component: () => import('../views/admin/RecipeManage.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'faqs', name: 'FaqManage', component: () => import('../views/admin/FaqManage.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'seasonal', name: 'SeasonalManage', component: () => import('../views/admin/SeasonalManage.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'announcements', name: 'AnnouncementManage', component: () => import('../views/admin/AnnouncementManage.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'statistics', name: 'DataStatistics', component: () => import('../views/admin/DataStatistics.vue'), meta: { roles: ['ADMIN', 'MERCHANT'] } },
      { path: 'users', name: 'UserManage', component: () => import('../views/admin/UserManage.vue'), meta: { roles: ['ADMIN'] } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userInfoStr = localStorage.getItem('userInfo')
  const userInfo = userInfoStr ? JSON.parse(userInfoStr) : null
  const publicPaths = ['/trace', '/products', '/trace-process', '/farmers', '/announcements', '/about', '/seasonal', '/knowledge', '/recipes', '/origin-map', '/help', '/quality']
  const isPublic = to.path === '/' || to.path === '/login' || to.name === 'NotFound' || to.path.startsWith('/product/') || to.path.startsWith('/farmer/') || publicPaths.includes(to.path)
  if (isPublic) {
    next()
  } else if (!userInfo) {
    next('/login')
  } else if (to.meta.roles && !to.meta.roles.includes(userInfo.role)) {
    next('/admin')
  } else {
    next()
  }
})

export default router
