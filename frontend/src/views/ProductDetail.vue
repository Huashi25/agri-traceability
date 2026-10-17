<template>
  <div class="detail-page" v-loading="loading">
    <div class="breadcrumb">
      <span class="crumb-link" @click="$router.push('/')">首页</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">商品详情</span>
    </div>

    <div v-if="product" class="detail-content">
      <!-- Left: Image -->
      <div class="detail-left">
        <div class="main-image">
          <img :src="getProductImage(product)" :alt="product.name" />
          <div class="trace-badge" v-if="hasTrace">
            <el-icon><CircleCheck /></el-icon> 溯源可查
          </div>
        </div>
      </div>

      <!-- Right: Info -->
      <div class="detail-right">
        <div class="product-label">FRESH PRODUCE</div>
        <h1 class="product-title">{{ product.name }}</h1>
        <div class="product-tags">
          <span class="tag-item" v-if="product.origin">产地: {{ product.origin }}</span>
          <span class="tag-item" v-if="hasTrace">全程溯源</span>
          <span class="tag-item" v-if="product.stock > 0">有货</span>
          <span class="tag-item out" v-else>缺货</span>
        </div>

        <div class="price-section">
          <span class="price-label">售价</span>
          <span class="price-symbol">￥</span>
          <span class="price-amount">{{ product.price }}</span>
          <span class="price-original-detail" v-if="product.originalPrice && Number(product.originalPrice) > Number(product.price)">￥{{ product.originalPrice }}</span>
          <span class="stock-info" v-if="product.stock">库存 {{ product.stock }} 件</span>
          <span class="sales-info" v-if="product.salesCount">已售 {{ product.salesCount }} 件</span>
        </div>

        <!-- Tabs -->
        <div class="detail-tabs">
          <div class="tab-headers">
            <div class="tab-header" :class="{ active: activeTab === 'detail' }" @click="activeTab = 'detail'">
              商品详情
            </div>
            <div class="tab-header" :class="{ active: activeTab === 'spec' }" @click="activeTab = 'spec'">
              规格参数
            </div>
            <div class="tab-header" :class="{ active: activeTab === 'review' }" @click="activeTab = 'review'">
              用户评价 <span v-if="reviewPage.total > 0" class="tab-count">{{ product.reviewCount || reviewPage.total }}</span>
            </div>
          </div>

          <div v-show="activeTab === 'detail'" class="tab-content">
            <div class="detail-desc-block">
              <h3>商品简介</h3>
              <p>{{ product.description || '优质农产品，从田间直采，新鲜送达。本产品支持全程溯源，扫码即可查看产地、质检、物流全链路信息。' }}</p>
            </div>
            <div class="detail-desc-block">
              <h3>品质保障</h3>
              <div class="quality-list">
                <div class="quality-item"><el-icon color="#1a5d3a"><CircleCheck /></el-icon> 原产地直采，中间无环节</div>
                <div class="quality-item"><el-icon color="#1a5d3a"><CircleCheck /></el-icon> 通过农药残留检测，安全放心</div>
                <div class="quality-item"><el-icon color="#1a5d3a"><CircleCheck /></el-icon> 全程冷链运输，保障新鲜度</div>
                <div class="quality-item"><el-icon color="#1a5d3a"><CircleCheck /></el-icon> 溯源码可查，信息透明可信</div>
              </div>
            </div>
          </div>

          <div v-show="activeTab === 'spec'" class="tab-content">
            <table class="spec-table">
              <tbody>
                <tr><td class="spec-key">商品名称</td><td class="spec-val">{{ product.name }}</td></tr>
                <tr><td class="spec-key">产地</td><td class="spec-val">{{ product.origin || '-' }}</td></tr>
                <tr><td class="spec-key">分类</td><td class="spec-val">{{ getCategoryName(product.categoryId) }}</td></tr>
                <tr><td class="spec-key">规格</td><td class="spec-val">{{ product.spec || '按实际重量' }}</td></tr>
                <tr><td class="spec-key">保质期</td><td class="spec-val">{{ product.shelfLife || '新鲜采摘，建议7天内食用' }}</td></tr>
                <tr><td class="spec-key">储存方式</td><td class="spec-val">{{ product.storage || '常温/冷藏保存' }}</td></tr>
                <tr><td class="spec-key">包装方式</td><td class="spec-val">{{ product.packing || '泡沫箱+冰袋冷链包装' }}</td></tr>
                <tr><td class="spec-key">配送方式</td><td class="spec-val">顺丰冷链，24小时内发出</td></tr>
                <tr><td class="spec-key">库存</td><td class="spec-val">{{ product.stock || 0 }} 件</td></tr>
                <tr><td class="spec-key">溯源</td><td class="spec-val">{{ hasTrace ? '支持全程溯源' : '暂无溯源信息' }}</td></tr>
              </tbody>
            </table>
          </div>

          <div v-show="activeTab === 'review'" class="tab-content">
            <div class="review-overview">
              <div class="overview-score">
                <div class="score-num">{{ avgRating.toFixed(1) }}</div>
                <div class="score-label">综合评分</div>
                <el-rate v-model="avgRating" disabled show-score="false" size="small" />
              </div>
              <div class="overview-stats">
                <div class="stat-row">
                  <span class="stat-label">好评率</span>
                  <span class="stat-value">{{ goodRate }}%</span>
                </div>
                <div class="stat-row">
                  <span class="stat-label">评价总数</span>
                  <span class="stat-value">{{ product.reviewCount || reviewPage.total }} 条</span>
                </div>
                <div class="stat-row">
                  <span class="stat-label">销量</span>
                  <span class="stat-value">{{ product.salesCount || 0 }} 件</span>
                </div>
              </div>
            </div>

            <div class="review-form-section" v-if="isLoggedIn">
              <h4 class="review-form-title">写下您的评价</h4>
              <div class="review-form-rating">
                <span class="form-rating-label">评分</span>
                <el-rate v-model="reviewForm.rating" :max="5" :allow-half="false" size="large" />
              </div>
              <el-input
                v-model="reviewForm.content"
                type="textarea"
                :rows="4"
                placeholder="分享您对这款商品的真实体验..."
                maxlength="500"
                show-word-limit
              />
              <div class="review-form-actions">
                <el-button type="primary" :loading="submittingReview" @click="submitReview">提交评价</el-button>
              </div>
            </div>
            <div class="review-login-hint" v-else>
              <el-text type="info">请先登录后发表评价</el-text>
              <el-button type="primary" link @click="$router.push('/login')">去登录</el-button>
            </div>

            <div v-if="reviewPage.total === 0" class="no-reviews">
              <el-empty description="暂无评价，购买后快来分享你的体验吧" :image-size="80" />
            </div>
            <div v-else class="review-list-detail">
              <div v-for="r in reviews" :key="r.id" class="review-item-detail">
                <div class="review-avatar-detail">
                  {{ (r.userName || '?').charAt(0).toUpperCase() }}
                </div>
                <div class="review-body-detail">
                  <div class="review-header-detail">
                    <span class="review-user-detail">{{ r.userName }}</span>
                    <el-rate v-model="r.rating" disabled size="small" />
                    <span class="review-date-detail">{{ r.createTime?.replace('T',' ').substring(0, 10) }}</span>
                  </div>
                  <p class="review-text-detail">{{ r.content }}</p>
                </div>
              </div>
            </div>
            <div v-if="reviewPage.total > reviewPage.pageSize" class="review-pagination">
              <el-pagination
                background
                layout="prev, pager, next"
                :current-page="reviewPage.pageNum"
                :page-size="reviewPage.pageSize"
                :total="reviewPage.total"
                @current-change="handleReviewPageChange"
              />
            </div>
          </div>
        </div>

        <div class="purchase-bar">
          <div class="purchase-left">
            <div class="qty-box">
              <span class="qty-label">数量</span>
              <el-input-number v-model="quantity" :min="1" :max="product.stock || 99" size="default" />
            </div>
            <div class="btn-favorite" :class="{ favorited: isFavorited }" @click="toggleFavorite">
              <svg viewBox="0 0 24 24" width="20" height="20" :fill="isFavorited ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
              </svg>
              <span>{{ isFavorited ? '已收藏' : '收藏' }}</span>
            </div>
          </div>
          <div class="purchase-right">
            <button class="btn-add-cart" @click="addToCart" :disabled="product.status !== 1">
              加入购物车
            </button>
            <button class="btn-buy-now" @click="buyNow" :disabled="product.status !== 1">
              立即购买
            </button>
          </div>
        </div>

        <div v-if="traceCode" class="trace-preview">
          <div class="trace-preview-header">
            <h3>溯源信息预览</h3>
            <span class="trace-link" @click="goTrace">查看完整溯源 →</span>
          </div>
          <div class="trace-code-box">
            <span class="trace-code-label">溯源码</span>
            <code class="trace-code-text">{{ traceCode }}</code>
            <span class="trace-copy" @click="copyCode(traceCode)"><el-icon><CopyDocument /></el-icon></span>
          </div>
          <div class="trace-qr-section">
            <div class="trace-qr-box">
              <QrCode :text="traceUrl" :size="160" />
            </div>
            <div class="trace-qr-desc">
              <span class="qr-hint">扫码溯源</span>
              <span class="qr-sub">手机扫码查看完整溯源信息</span>
            </div>
          </div>
          <div class="trace-steps">
            <div class="trace-step-mini" :class="{ done: traceInfo?.origin }">
              <div class="step-dot"></div>
              <div class="step-text">
                <div class="step-title">产地</div>
                <div class="step-detail">{{ traceInfo?.origin?.farmName || '数据采集中' }}</div>
              </div>
            </div>
            <div class="step-line"></div>
            <div class="trace-step-mini" :class="{ done: traceInfo?.inspection }">
              <div class="step-dot"></div>
              <div class="step-text">
                <div class="step-title">质检</div>
                <div class="step-detail">{{ traceInfo?.inspection?.qualified === 1 ? '合格' : '数据采集中' }}</div>
              </div>
            </div>
            <div class="step-line"></div>
            <div class="trace-step-mini" :class="{ done: traceInfo?.logistics }">
              <div class="step-dot"></div>
              <div class="step-text">
                <div class="step-title">物流</div>
                <div class="step-detail">{{ traceInfo?.logistics?.transportCompany || '数据采集中' }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Related Products -->
    <div v-if="relatedProducts.length > 0" class="related-section">
      <div class="section-eyebrow">YOU MAY ALSO LIKE</div>
      <h2 class="related-title">相关推荐</h2>
      <div class="editorial-divider"></div>
      <div class="related-grid">
        <div v-for="p in relatedProducts" :key="p.id" class="related-card" @click="goProduct(p.id)">
          <div class="related-image">
            <img :src="getProductImage(p)" :alt="p.name" />
          </div>
          <div class="related-info">
            <div class="related-name">{{ p.name }}</div>
            <div class="related-price">￥{{ p.price }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Buy Now Checkout Dialog -->
    <el-dialog v-model="buyNowDialog" title="确认订单" width="480px" :close-on-click-modal="false">
      <div class="buynow-item" v-if="product">
        <img :src="getProductImage(product)" :alt="product.name" class="bn-img" />
        <div class="bn-info">
          <div class="bn-name">{{ product.name }}</div>
          <div class="bn-detail">￥{{ product.price }} × {{ quantity }}</div>
        </div>
        <div class="bn-subtotal">￥{{ (product.price * quantity).toFixed(2) }}</div>
      </div>

      <el-divider />

      <el-form :model="buyNowForm" label-width="80px">
        <el-form-item label="收货地址" v-if="savedAddresses.length > 0">
          <el-select v-model="selectedAddressId" placeholder="选择已保存地址" style="width: 100%" @change="onAddressChange">
            <el-option
              v-for="a in savedAddresses"
              :key="a.id"
              :label="`${a.receiverName} ${a.receiverPhone}`"
              :value="a.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="收货人" required>
          <el-input v-model="buyNowForm.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="buyNowForm.receiverPhone" placeholder="请输入收货人手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="收货地址" required>
          <el-input v-model="buyNowForm.receiverAddress" type="textarea" :rows="2" placeholder="请输入详细收货地址" />
        </el-form-item>
        <el-form-item label="">
          <el-checkbox v-model="saveAddressFlag">保存为常用地址</el-checkbox>
        </el-form-item>
        <el-form-item label="支付方式" required>
          <el-radio-group v-model="buyNowForm.paymentMethod">
            <el-radio :label="0">微信支付</el-radio>
            <el-radio :label="1">支付宝</el-radio>
            <el-radio :label="2">银行卡</el-radio>
            <el-radio :label="3">货到付款</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <div class="buynow-summary-bar">
        <span>共 {{ quantity }} 件商品，合计</span>
        <span class="bn-pay-price">￥{{ product ? (product.price * quantity).toFixed(2) : 0 }}</span>
      </div>

      <template #footer>
        <el-button @click="buyNowDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBuyNow">提交订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productApi, categoryApi, traceBatchApi, traceQueryApi, reviewApi, orderApi, addressApi, favoriteApi } from '../api'
import { useCartStore } from '../stores/cart'
import { useUserStore } from '../stores/user'
import QrCode from '../components/QrCode.vue'

import durianImg from '../assets/images/product-durian.jpg'
import orangeImg from '../assets/images/product-orange.jpg'
import riceImg from '../assets/images/product-rice.jpg'
import scallionImg from '../assets/images/product-scallion.jpg'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)

const product = ref(null)
const categories = ref([])
const relatedProducts = ref([])
const loading = ref(false)
const quantity = ref(1)
const traceCode = ref('')
const traceInfo = ref(null)
const activeTab = ref('detail')
const reviews = ref([])
const reviewPage = ref({ pageNum: 1, pageSize: 5, total: 0 })
const reviewForm = ref({ rating: 5, content: '' })
const submittingReview = ref(false)
const buyNowDialog = ref(false)
const submitting = ref(false)
const buyNowForm = ref({ receiverName: '', receiverPhone: '', receiverAddress: '', paymentMethod: 0 })
const savedAddresses = ref([])
const selectedAddressId = ref(null)
const saveAddressFlag = ref(false)
const isFavorited = ref(false)

const productImages = { '榴莲': durianImg, '脐橙': orangeImg, '大米': riceImg, '葱': scallionImg }
const getProductImage = (p) => {
  if (!p) return durianImg
  if (p.image) return p.image
  for (const key in productImages) { if (p.name?.includes(key)) return productImages[key] }
  return durianImg
}

const hasTrace = computed(() => !!traceCode.value)
const traceUrl = computed(() => `${window.location.origin}/trace?code=${traceCode.value}`)

const avgRating = computed(() => {
  if (product.value?.rating) return Number(product.value.rating)
  return 5
})

const goodRate = computed(() => {
  if (!product.value?.rating) return 100
  const rating = Number(product.value.rating)
  if (rating >= 4.8) return 98
  if (rating >= 4.5) return 95
  if (rating >= 4.0) return 85
  if (rating >= 3.0) return 60
  return 40
})

const loadProduct = async () => {
  loading.value = true
  try {
    const res = await productApi.getById(route.params.id)
    product.value = res.data
    loadRelated()
    loadTraceInfo()
    loadReviews()
    checkFavorite()
  } finally { loading.value = false }
}

const checkFavorite = async () => {
  if (!userStore.isLoggedIn) { isFavorited.value = false; return }
  try {
    const res = await favoriteApi.check(product.value.id)
    isFavorited.value = res.data
  } catch {}
}

const toggleFavorite = async () => {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); router.push('/login'); return }
  try {
    const res = await favoriteApi.toggle(product.value.id)
    isFavorited.value = res.data
    ElMessage.success(res.data ? '已收藏' : '已取消收藏')
  } catch {
    ElMessage.error('操作失败')
  }
}

const loadRelated = async () => {
  const res = await productApi.list({ categoryId: product.value.categoryId })
  relatedProducts.value = res.data.filter(p => p.id !== product.value.id).slice(0, 4)
  if (relatedProducts.value.length < 4) {
    const all = await productApi.list({})
    const more = all.data.filter(p => p.id !== product.value.id && !relatedProducts.value.find(r => r.id === p.id))
    relatedProducts.value = [...relatedProducts.value, ...more].slice(0, 4)
  }
}

const loadTraceInfo = async () => {
  try {
    const batchRes = await traceBatchApi.list(product.value.id)
    if (batchRes.data && batchRes.data.length > 0) {
      const batch = batchRes.data[0]
      traceCode.value = batch.traceCode
      const queryRes = await traceQueryApi.query(batch.traceCode)
      traceInfo.value = queryRes.data
    } else {
      const pid = String(product.value.id).padStart(4, '0')
      const ts = Date.now().toString(36).toUpperCase().slice(-4)
      traceCode.value = `AGRI${pid}${ts}`
    }
  } catch {
    const pid = String(product.value.id).padStart(4, '0')
    const ts = Date.now().toString(36).toUpperCase().slice(-4)
    traceCode.value = `AGRI${pid}${ts}`
  }
}

const loadCategories = async () => {
  const res = await categoryApi.list()
  categories.value = res.data
}

const loadReviews = async () => {
  try {
    const res = await reviewApi.list(product.value.id, {
      page: reviewPage.value.pageNum,
      pageSize: reviewPage.value.pageSize
    })
    reviews.value = res.data.list
    reviewPage.value.total = res.data.total
  } catch {}
}

const handleReviewPageChange = (newPage) => {
  reviewPage.value.pageNum = newPage
  loadReviews()
}

const submitReview = async () => {
  if (!reviewForm.value.content?.trim()) {
    ElMessage.warning('请输入评价内容')
    return
  }
  submittingReview.value = true
  try {
    const res = await reviewApi.save({
      productId: product.value.id,
      rating: reviewForm.value.rating,
      content: reviewForm.value.content.trim()
    })
    if (res.code === 200) {
      ElMessage.success('评价提交成功')
      reviewForm.value = { rating: 5, content: '' }
      reviewPage.value.pageNum = 1
      loadReviews()
      loadProduct()
    } else {
      ElMessage.error(res.message || '评价失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '评价失败，请重试')
  } finally {
    submittingReview.value = false
  }
}

const getCategoryName = (id) => categories.value.find(c => c.id === id)?.name || '-'

const loadSavedAddresses = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const res = await addressApi.list()
    savedAddresses.value = res.data || []
    const defaultAddr = savedAddresses.value.find(a => a.isDefault === 1)
    if (defaultAddr) {
      selectedAddressId.value = defaultAddr.id
      buyNowForm.value.receiverName = defaultAddr.receiverName
      buyNowForm.value.receiverPhone = defaultAddr.receiverPhone
      buyNowForm.value.receiverAddress = defaultAddr.receiverAddress
    }
  } catch {}
}

const onAddressChange = (id) => {
  const addr = savedAddresses.value.find(a => a.id === id)
  if (addr) {
    buyNowForm.value.receiverName = addr.receiverName
    buyNowForm.value.receiverPhone = addr.receiverPhone
    buyNowForm.value.receiverAddress = addr.receiverAddress
  }
}

const addToCart = async () => {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); router.push('/login'); return }
  if (product.value.status !== 1) { ElMessage.warning('商品已下架'); return }
  if (product.value.stock != null && quantity.value > product.value.stock) {
    ElMessage.warning('库存不足'); return
  }
  const ok = await cartStore.addToCart(product.value.id, quantity.value)
  if (ok) ElMessage.success(`${product.value.name} 已加入购物车`)
}

const buyNow = () => {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); router.push('/login'); return }
  if (product.value.status !== 1) { ElMessage.warning('商品已下架'); return }
  if (product.value.stock != null && quantity.value > product.value.stock) {
    ElMessage.warning('库存不足'); return
  }
  buyNowForm.value = { receiverName: '', receiverPhone: '', receiverAddress: '', paymentMethod: 0 }
  selectedAddressId.value = null
  saveAddressFlag.value = false
  buyNowDialog.value = true
  loadSavedAddresses()
}

const submitBuyNow = async () => {
  if (!buyNowForm.value.receiverName?.trim()) { ElMessage.warning('请填写收货人姓名'); return }
  if (!buyNowForm.value.receiverPhone?.trim() || buyNowForm.value.receiverPhone.length !== 11) {
    ElMessage.warning('请填写正确的手机号'); return
  }
  if (!buyNowForm.value.receiverAddress?.trim()) { ElMessage.warning('请填写收货地址'); return }

  if (saveAddressFlag.value) {
    try {
      await addressApi.save({
        receiverName: buyNowForm.value.receiverName,
        receiverPhone: buyNowForm.value.receiverPhone,
        receiverAddress: buyNowForm.value.receiverAddress,
        isDefault: savedAddresses.value.length === 0 ? 1 : 0
      })
    } catch {}
  }

  submitting.value = true
  try {
    const orderData = {
      order: {
        userId: userStore.userInfo?.id,
        receiverName: buyNowForm.value.receiverName,
        receiverPhone: buyNowForm.value.receiverPhone,
        receiverAddress: buyNowForm.value.receiverAddress,
        paymentMethod: buyNowForm.value.paymentMethod
      },
      items: [{
        productId: product.value.id,
        quantity: quantity.value
      }]
    }
    const res = await orderApi.create(orderData)
    if (res.code === 200) {
      ElMessage.success('订单创建成功，请完成支付')
      buyNowDialog.value = false
      router.push('/orders?new=1')
    } else {
      ElMessage.error(res.message || '下单失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '下单失败，请重试')
  } finally {
    submitting.value = false
  }
}

const goProduct = (id) => {
  router.push(`/product/${id}`)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const goTrace = () => {
  router.push('/trace')
  setTimeout(() => {
    const input = document.querySelector('.trace-page input')
    if (input) {
      input.value = traceCode.value
      input.dispatchEvent(new Event('input'))
    }
  }, 500)
}

const copyCode = (code) => {
  navigator.clipboard.writeText(code)
  ElMessage.success('溯源码已复制')
}

watch(() => route.params.id, () => {
  if (route.params.id) {
    quantity.value = 1
    traceCode.value = ''
    traceInfo.value = null
    loadProduct()
  }
})

onMounted(() => {
  loadCategories()
  loadProduct()
})
</script>

<style scoped>
.detail-page { max-width: 1200px; margin: 0 auto; padding-bottom: 80px; }

.breadcrumb { margin-bottom: 32px; }
.crumb-link { font-size: 11px; letter-spacing: 0.15em; text-transform: uppercase; color: var(--text-secondary); cursor: pointer; }
.crumb-link:hover { color: var(--brand-primary); }
.crumb-sep { margin: 0 8px; color: var(--gray-300); }
.crumb-current { font-size: 11px; letter-spacing: 0.15em; text-transform: uppercase; color: var(--brand-primary); }

.detail-content { display: flex; gap: 48px; }

/* Detail Image */
.detail-left { width: 440px; flex-shrink: 0; }
.main-image {
  position: relative;
  overflow: hidden;
  background: var(--cream-200);
}
.main-image img {
  width: 100%; height: 440px;
  object-fit: cover;
  transition: transform 0.6s var(--ease-out);
}
.main-image:hover img { transform: scale(1.03); }
.trace-badge {
  position: absolute; top: 0; right: 0;
  background: var(--brand-primary);
  color: white;
  padding: 6px 12px;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  display: flex; align-items: center; gap: 4px;
}

.detail-right { flex: 1; }
.product-label {
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.25em;
  text-transform: uppercase;
  color: var(--text-tertiary);
  margin-bottom: 8px;
}
.product-title {
  font-family: var(--font-serif);
  font-size: 36px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 16px;
  letter-spacing: 0.01em;
}
.product-tags { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 24px; }
.tag-item {
  font-size: 11px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--brand-primary);
  border: 1px solid var(--brand-primary);
  padding: 4px 10px;
}
.tag-item.out { color: var(--gray-400); border-color: var(--gray-300); }

/* Price Section */
.price-section {
  display: flex;
  align-items: baseline;
  gap: 4px;
  padding: 20px 0;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 24px;
}
.price-label { font-size: 11px; letter-spacing: 0.15em; text-transform: uppercase; color: var(--text-secondary); margin-right: 12px; }
.price-symbol { font-size: 18px; color: var(--brand-primary); font-weight: 500; }
.price-amount { font-family: var(--font-serif); font-size: 40px; font-weight: 500; color: var(--brand-primary); }
.stock-info { font-size: 12px; color: var(--text-tertiary); margin-left: 16px; letter-spacing: 0.05em; }
.price-original-detail { font-size: 14px; color: var(--text-tertiary); text-decoration: line-through; margin-left: 12px; }
.sales-info { font-size: 12px; color: var(--brand-primary); margin-left: 16px; letter-spacing: 0.05em; }

/* Tabs */
.detail-tabs { margin: 0 0 24px; }
.tab-headers { display: flex; border-bottom: 1px solid var(--border-color); }
.tab-header {
  flex: 1; padding: 14px 0;
  text-align: center; font-size: 11px; font-weight: 500;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color var(--duration-fast);
  position: relative;
}
.tab-header:hover { color: var(--brand-primary); }
.tab-header.active { color: var(--brand-primary); }
.tab-header.active::after {
  content: '';
  position: absolute;
  bottom: -1px; left: 0; right: 0;
  height: 2px;
  background: var(--brand-primary);
}
.tab-count { font-size: 10px; color: var(--brand-primary); margin-left: 2px; }
.tab-content { padding: 24px 0; min-height: 200px; }
.detail-desc-block { margin-bottom: 24px; }
.detail-desc-block h3 { font-family: var(--font-serif); font-size: 18px; font-weight: 500; color: var(--text-primary); margin-bottom: 10px; }
.detail-desc-block p { font-size: 14px; color: var(--text-secondary); line-height: 1.8; }
.quality-list { display: flex; flex-direction: column; gap: 10px; }
.quality-item { display: flex; align-items: center; gap: 8px; font-size: 14px; color: var(--text-secondary); }
.spec-table { width: 100%; border-collapse: collapse; }
.spec-table td { padding: 12px 16px; border-bottom: 1px solid var(--border-light); font-size: 13px; }
.spec-key { width: 120px; color: var(--text-tertiary); letter-spacing: 0.1em; text-transform: uppercase; font-size: 11px; }
.spec-val { color: var(--text-primary); }

/* Review */
.review-overview {
  display: flex;
  gap: 32px;
  padding: 24px 0;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 24px;
  align-items: center;
}
.overview-score { text-align: center; padding-right: 32px; border-right: 1px solid var(--border-color); }
.score-num { font-family: var(--font-serif); font-size: 40px; font-weight: 500; color: var(--brand-primary); line-height: 1; }
.score-label { font-size: 11px; letter-spacing: 0.15em; text-transform: uppercase; color: var(--text-secondary); margin: 8px 0; }
.overview-stats { flex: 1; display: flex; flex-direction: column; gap: 10px; }
.stat-row { display: flex; justify-content: space-between; font-size: 13px; }
.stat-label { color: var(--text-secondary); }
.stat-value { color: var(--text-primary); font-weight: 500; }
.review-list-detail { display: flex; flex-direction: column; gap: 0; }
.review-item-detail { display: flex; gap: 12px; padding: 20px 0; border-bottom: 1px solid var(--border-light); }
.review-item-detail:last-child { border-bottom: none; }
.review-avatar-detail {
  width: 36px; height: 36px;
  background: var(--brand-primary);
  color: white;
  display: flex; align-items: center; justify-content: center;
  font-family: var(--font-serif);
  font-size: 16px; font-weight: 500;
  flex-shrink: 0;
}
.review-body-detail { flex: 1; }
.review-header-detail { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.review-user-detail { font-size: 13px; font-weight: 500; color: var(--text-primary); }
.review-date-detail { font-size: 11px; color: var(--text-tertiary); margin-left: auto; letter-spacing: 0.05em; }
.review-text-detail { font-family: var(--font-serif); font-size: 15px; font-style: italic; color: var(--text-primary); line-height: 1.7; }
.no-reviews { padding: 32px; text-align: center; }
.review-pagination { display: flex; justify-content: center; padding: 16px 0; }

.review-form-section { padding: 24px; border: 1px solid var(--border-color); border-radius: var(--radius-md, 12px); margin-bottom: 24px; background: var(--cream-100, #fafafa); }
.review-form-title { font-family: var(--font-serif); font-size: 16px; font-weight: 500; color: var(--text-primary); margin-bottom: 16px; }
.review-form-rating { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.form-rating-label { font-size: 14px; color: var(--text-secondary); }
.review-form-actions { display: flex; justify-content: flex-end; margin-top: 12px; }
.review-login-hint { display: flex; align-items: center; gap: 8px; padding: 24px; border: 1px dashed var(--border-color); border-radius: var(--radius-md, 12px); margin-bottom: 24px; justify-content: center; }

/* Purchase Bar */
.purchase-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--bg-card);
  border-top: 1px solid var(--border-color);
  padding: 14px 24px;
  padding-bottom: calc(14px + env(safe-area-inset-bottom));
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1200px;
  margin: 0 auto;
}
.purchase-left { display: flex; align-items: center; gap: 12px; }
.qty-box { display: flex; align-items: center; gap: 10px; }
.qty-label { font-size: 11px; letter-spacing: 0.15em; text-transform: uppercase; color: var(--text-secondary); }
.btn-favorite {
  display: flex; align-items: center; gap: 6px;
  padding: 0 16px; height: 44px;
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  font-size: 11px; font-weight: 500;
  letter-spacing: 0.15em; text-transform: uppercase;
  cursor: pointer; transition: all 0.3s var(--ease-out);
}
.btn-favorite:hover { border-color: var(--brand-primary); color: var(--brand-primary); }
.btn-favorite.favorited { color: #e53935; border-color: #e53935; }
.purchase-right { display: flex; gap: 12px; }
.btn-add-cart {
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid var(--brand-primary);
  color: var(--brand-primary);
  padding: 0 24px;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.3s var(--ease-out);
}
.btn-add-cart:hover { background: var(--brand-primary); color: #fff; }
.btn-add-cart:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-buy-now {
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--brand-primary);
  border: none;
  color: white;
  padding: 0 28px;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.3s var(--ease-out);
}
.btn-buy-now:hover { background: var(--brand-primary-dark); }
.btn-buy-now:disabled { opacity: 0.4; cursor: not-allowed; }

/* Trace Preview */
.trace-preview {
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  padding: 24px 0;
  margin-top: 24px;
}
.trace-preview-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.trace-preview-header h3 { font-family: var(--font-serif); font-size: 18px; font-weight: 500; color: var(--text-primary); }
.trace-link { font-size: 11px; letter-spacing: 0.15em; text-transform: uppercase; color: var(--brand-primary); cursor: pointer; }
.trace-link:hover { color: var(--brand-primary-dark); }
.trace-code-box { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.trace-code-label { font-size: 11px; letter-spacing: 0.15em; text-transform: uppercase; color: var(--text-secondary); }
.trace-code-text { font-family: monospace; font-size: 14px; color: var(--brand-primary); border: 1px solid var(--border-color); padding: 6px 12px; }
.trace-copy { cursor: pointer; color: var(--text-tertiary); display: flex; align-items: center; }
.trace-copy:hover { color: var(--brand-primary); }

.trace-qr-section { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; padding: 16px; background: var(--cream-100, #fafafa); border-radius: var(--radius-md, 12px); }
.trace-qr-box { flex-shrink: 0; border: 1px solid var(--border-color); padding: 8px; background: white; border-radius: 8px; }
.trace-qr-desc { display: flex; flex-direction: column; gap: 4px; }
.qr-hint { font-size: 14px; font-weight: 500; color: var(--brand-primary); }
.qr-sub { font-size: 12px; color: var(--text-tertiary); }

.trace-steps { display: flex; align-items: center; gap: 0; }
.trace-step-mini { display: flex; align-items: center; gap: 10px; }
.step-dot { width: 10px; height: 10px; background: var(--gray-300); flex-shrink: 0; }
.trace-step-mini.done .step-dot { background: var(--brand-primary); }
.step-text .step-title { font-size: 11px; letter-spacing: 0.1em; text-transform: uppercase; color: var(--text-primary); }
.step-text .step-detail { font-size: 12px; color: var(--text-secondary); }
.step-line { width: 40px; height: 1px; background: var(--border-color); margin: 0 12px; }

/* Related Products */
.related-section { margin-top: 64px; text-align: center; }
.section-eyebrow {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.25em;
  text-transform: uppercase;
  color: var(--brand-primary);
  margin-bottom: 8px;
}
.related-title {
  font-family: var(--font-serif);
  font-size: 28px;
  font-weight: 500;
  color: var(--text-primary);
}
.editorial-divider {
  width: 40px;
  height: 1px;
  background: var(--brand-primary);
  margin: 16px auto 32px;
}
.related-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px; }
.related-card {
  cursor: pointer;
  transition: opacity var(--duration-fast);
}
.related-card:hover { opacity: 0.8; }
.related-image { height: 140px; overflow: hidden; background: var(--cream-200); margin-bottom: 12px; }
.related-image img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.6s var(--ease-out); }
.related-card:hover .related-image img { transform: scale(1.05); }
.related-info { }
.related-name { font-family: var(--font-serif); font-size: 15px; color: var(--text-primary); font-weight: 500; margin-bottom: 4px; }
.related-price { font-size: 16px; color: var(--brand-primary); font-weight: 500; }

@media (max-width: 768px) {
  .detail-content { flex-direction: column; gap: 24px; }
  .detail-left { width: 100%; }
  .related-grid { grid-template-columns: repeat(2, 1fr); }
  .product-title { font-size: 28px; }
  .price-amount { font-size: 32px; }
  .purchase-bar { flex-direction: column; gap: 10px; }
  .purchase-left { width: 100%; justify-content: center; }
  .purchase-right { width: 100%; }
  .btn-add-cart, .btn-buy-now { flex: 1; }
}

/* Buy Now Dialog */
.buynow-item { display: flex; align-items: center; gap: 12px; padding: 8px 0; }
.bn-img { width: 56px; height: 56px; border-radius: 6px; object-fit: cover; }
.bn-info { flex: 1; }
.bn-name { font-size: 14px; color: #333; }
.bn-detail { font-size: 13px; color: #999; margin-top: 2px; }
.bn-subtotal { font-size: 15px; font-weight: 600; color: #f56c6c; }
.buynow-summary-bar {
  display: flex; justify-content: flex-end; align-items: center; gap: 8px;
  padding: 12px 16px; background: #f0f9eb; border-radius: 8px; margin-top: 8px;
}
.bn-pay-price { font-size: 20px; font-weight: 700; color: #f56c6c; }
</style>
