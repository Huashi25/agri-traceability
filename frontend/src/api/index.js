import request from './request'

// 用户接口
export const userApi = {
  login: (data) => request.post('/user/login', data),
  register: (data) => request.post('/user/register', data),
  getById: (id) => request.get(`/user/${id}`),
  list: () => request.get('/user/list'),
  save: (data) => request.post('/user', data),
  update: (data) => request.put('/user', data),
  delete: (id) => request.delete(`/user/${id}`)
}

// 商品分类接口
export const categoryApi = {
  list: () => request.get('/category/list'),
  save: (data) => request.post('/category', data),
  delete: (id) => request.delete(`/category/${id}`)
}

// 商品接口
export const productApi = {
  list: (params) => request.get('/product/list', { params }),
  getById: (id) => request.get(`/product/${id}`),
  save: (data) => request.post('/product', data),
  update: (data) => request.put('/product', data),
  delete: (id) => request.delete(`/product/${id}`)
}

// 购物车接口
export const cartApi = {
  list: (userId) => request.get('/cart/list', { params: { userId } }),
  add: (data) => request.post('/cart', data),
  update: (data) => request.put('/cart', data),
  delete: (id) => request.delete(`/cart/${id}`)
}

// 订单接口
export const orderApi = {
  list: (userId) => request.get('/order/list', { params: { userId } }),
  getById: (id) => request.get(`/order/${id}`),
  getItems: (orderId) => request.get(`/order/items/${orderId}`),
  create: (orderData, items) => request.post('/order', { order: orderData, items }),
  checkout: (data) => request.post('/order/checkout', data),
  pay: (id) => request.put(`/order/pay/${id}`),
  ship: (id) => request.put(`/order/ship/${id}`),
  confirm: (id) => request.put(`/order/confirm/${id}`),
  cancel: (id) => request.put(`/order/cancel/${id}`),
  update: (data) => request.put('/order', data)
}

// 溯源批次接口
export const traceBatchApi = {
  list: (productId) => request.get('/trace/batch/list', { params: { productId } }),
  getById: (id) => request.get(`/trace/batch/${id}`),
  create: (data) => request.post('/trace/batch', data),
  update: (data) => request.put('/trace/batch', data),
  delete: (id) => request.delete(`/trace/batch/${id}`)
}

// 溯源查询接口
export const traceQueryApi = {
  query: (traceCode) => request.get(`/trace/query/${traceCode}`)
}

// 产地信息接口
export const originApi = {
  list: (batchId) => request.get('/trace/origin/list', { params: { batchId } }),
  save: (data) => request.post('/trace/origin', data),
  update: (data) => request.put('/trace/origin', data),
  delete: (id) => request.delete(`/trace/origin/${id}`)
}

// 质检信息接口
export const inspectionApi = {
  list: (batchId) => request.get('/trace/inspection/list', { params: { batchId } }),
  save: (data) => request.post('/trace/inspection', data),
  update: (data) => request.put('/trace/inspection', data),
  delete: (id) => request.delete(`/trace/inspection/${id}`)
}

// 物流信息接口
export const logisticsApi = {
  list: (batchId) => request.get('/trace/logistics/list', { params: { batchId } }),
  save: (data) => request.post('/trace/logistics', data),
  update: (data) => request.put('/trace/logistics', data),
  delete: (id) => request.delete(`/trace/logistics/${id}`)
}

// 管理端订单接口
export const adminOrderApi = {
  list: (status) => request.get('/admin/order/list', { params: { status } }),
  getById: (id) => request.get(`/admin/order/${id}`),
  getItems: (orderId) => request.get(`/admin/order/items/${orderId}`),
  ship: (id) => request.put(`/admin/order/ship/${id}`),
  cancel: (id) => request.put(`/admin/order/cancel/${id}`),
  update: (data) => request.put('/admin/order', data),
  stats: () => request.get('/admin/order/stats')
}

// 数据统计接口
export const statsApi = {
  overview: () => request.get('/admin/stats/overview'),
  hotProducts: () => request.get('/admin/stats/hot-products')
}

// 农户接口
export const farmerApi = {
  list: (name) => request.get('/farmer/list', { params: { name } }),
  save: (data) => request.post('/farmer', data),
  update: (data) => request.put('/farmer', data),
  delete: (id) => request.delete(`/farmer/${id}`)
}

// 评价接口
export const reviewApi = {
  list: (productId, pagination) => request.get('/review/list', { params: { productId, ...pagination } }),
  save: (data) => request.post('/review', data),
  update: (data) => request.put('/review', data),
  delete: (id) => request.delete(`/review/${id}`)
}

// 公告接口
export const announcementApi = {
  list: () => request.get('/announcement/list'),
  save: (data) => request.post('/announcement', data),
  update: (data) => request.put('/announcement', data),
  delete: (id) => request.delete(`/announcement/${id}`)
}

// 文件上传接口
export const uploadApi = {
  image: (formData) => request.post('/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 用户地址接口
export const addressApi = {
  list: () => request.get('/address/list'),
  save: (data) => request.post('/address', data),
  update: (data) => request.put('/address', data),
  delete: (id) => request.delete(`/address/${id}`),
  setDefault: (id) => request.put(`/address/default/${id}`)
}

// 商品收藏接口
export const favoriteApi = {
  toggle: (productId) => request.post(`/favorite/toggle/${productId}`),
  list: () => request.get('/favorite/list'),
  check: (productId) => request.get(`/favorite/check/${productId}`),
  ids: () => request.get('/favorite/ids')
}

// 时令接口
export const seasonalApi = {
  current: () => request.get('/seasonal/current'),
  byMonth: (month) => request.get(`/seasonal/month/${month}`),
  byProduct: (productId) => request.get(`/seasonal/product/${productId}`),
  adminList: () => request.get('/seasonal/admin/list'),
  save: (data) => request.post('/seasonal', data),
  delete: (id) => request.delete(`/seasonal/${id}`)
}

// 农业百科接口
export const knowledgeApi = {
  list: (category) => request.get('/knowledge/list', { params: { category } }),
  getById: (id) => request.get(`/knowledge/${id}`),
  categories: () => request.get('/knowledge/categories'),
  adminList: () => request.get('/knowledge/admin/list'),
  save: (data) => request.post('/knowledge', data),
  update: (data) => request.put('/knowledge', data),
  delete: (id) => request.delete(`/knowledge/${id}`)
}

// 食谱接口
export const recipeApi = {
  list: (category) => request.get('/recipe/list', { params: { category } }),
  getById: (id) => request.get(`/recipe/${id}`),
  categories: () => request.get('/recipe/categories'),
  getProducts: (id) => request.get(`/recipe/${id}/products`),
  saveProducts: (id, products) => request.post(`/recipe/${id}/products`, products),
  save: (data) => request.post('/recipe', data),
  update: (data) => request.put('/recipe', data),
  delete: (id) => request.delete(`/recipe/${id}`)
}

// FAQ接口
export const faqApi = {
  list: (category) => request.get('/faq/list', { params: { category } }),
  categories: () => request.get('/faq/categories'),
  save: (data) => request.post('/faq', data),
  update: (data) => request.put('/faq', data),
  delete: (id) => request.delete(`/faq/${id}`)
}

// 产地地图接口
export const originMapApi = {
  mapData: () => request.get('/origin/map'),
  list: () => request.get('/origin/list'),
  getById: (id) => request.get(`/origin/${id}`)
}

// 公共统计接口
export const publicStatsApi = {
  summary: () => request.get('/stats/summary'),
  hotProducts: () => request.get('/stats/hot-products')
}

// 品质标准接口
export const qualityApi = {
  standards: () => request.get('/quality/standards'),
  traceProcess: () => request.get('/quality/trace-process')
}

// 通知接口
export const notificationApi = {
  list: () => request.get('/notification/list'),
  unreadCount: () => request.get('/notification/unread-count'),
  markRead: (id) => request.put(`/notification/read/${id}`),
  markAllRead: () => request.put('/notification/read-all'),
  delete: (id) => request.delete(`/notification/${id}`),
  create: (data) => request.post('/notification', data)
}
