<template>
  <div class="cart-page">
    <div class="page-header">
      <h1>购物车</h1>
      <p>{{ cartList.length }} 件商品</p>
    </div>

    <div v-if="cartList.length === 0" class="empty-cart">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" width="80" height="80" fill="none" stroke="currentColor" stroke-width="1">
          <circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/>
          <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
        </svg>
      </div>
      <p>溯源好货等你发现</p>
      <button class="btn-go-shop" @click="$router.push('/')">去溯源选购</button>
    </div>

    <div v-else class="cart-content">
      <div class="cart-items">
        <!-- Select All Bar -->
        <div class="select-all-bar">
          <el-checkbox v-model="allSelected" @change="toggleSelectAll">全选</el-checkbox>
          <span class="selected-info">已选 {{ selectedCount }} 件</span>
        </div>

        <div v-for="item in cartList" :key="item.cart.id" class="cart-item" :class="{ selected: item.cart._selected }">
          <div class="item-check">
            <el-checkbox v-model="item.cart._selected" />
          </div>
          <div class="item-image">
            <img :src="getProductImage(item.product)" :alt="item.product?.name" />
          </div>
          <div class="item-info">
            <h3 class="item-name">{{ item.product?.name || '未知商品' }}</h3>
            <p class="item-origin" v-if="item.product?.origin">{{ item.product.origin }}</p>
            <p class="item-desc">{{ item.product?.description || '' }}</p>
            <div class="item-stock-hint" v-if="item.product?.stock != null && item.product.stock < 50">
              <span class="stock-warn">仅剩 {{ item.product.stock }} 件</span>
            </div>
          </div>
          <div class="item-price">
            <span class="symbol">￥</span>
            <span class="amount">{{ item.product?.price || 0 }}</span>
          </div>
          <div class="item-quantity">
            <el-input-number v-model="item.cart.quantity" :min="1" :max="item.product?.stock || 99" size="small"
              @change="updateQuantity(item.cart)" />
          </div>
          <div class="item-subtotal">
            <span class="symbol">￥</span>
            <span class="amount">{{ ((item.product?.price || 0) * item.cart.quantity).toFixed(2) }}</span>
          </div>
          <div class="item-action">
            <button class="btn-remove" @click="removeItem(item.cart.id)">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <div class="cart-summary">
        <h3 class="summary-title">订单摘要</h3>
        <div class="summary-row">
          <span>商品数量</span>
          <span>{{ selectedCount }} 件</span>
        </div>
        <div class="summary-row">
          <span>商品总额</span>
          <span class="total-price">￥{{ selectedPrice.toFixed(2) }}</span>
        </div>
        <div class="summary-row">
          <span>运费</span>
          <span class="free-shipping">包邮</span>
        </div>
        <div class="summary-divider"></div>
        <div class="summary-row total">
          <span>应付金额</span>
          <span class="pay-price">￥{{ selectedPrice.toFixed(2) }}</span>
        </div>
        <button class="btn-checkout" :disabled="selectedCount === 0" @click="checkout">
          <span v-if="selectedCount > 0">结算 ({{ selectedCount }})</span>
          <span v-else>请选择商品</span>
        </button>
      </div>
    </div>

    <!-- Checkout Dialog -->
    <el-dialog v-model="checkoutDialog" title="确认订单" width="520px" :close-on-click-modal="false">
      <div class="checkout-items">
        <div v-for="item in selectedItems" :key="item.cart.id" class="checkout-item">
          <img :src="getProductImage(item.product)" :alt="item.product?.name" class="ck-img" />
          <div class="ck-info">
            <div class="ck-name">{{ item.product?.name }}</div>
            <div class="ck-detail">￥{{ item.product?.price }} × {{ item.cart.quantity }}</div>
          </div>
          <div class="ck-subtotal">￥{{ ((item.product?.price || 0) * item.cart.quantity).toFixed(2) }}</div>
        </div>
      </div>

      <el-divider />

      <el-form :model="orderForm" label-width="80px" class="checkout-form">
        <el-form-item label="收货地址" v-if="savedAddresses.length > 0">
          <el-select v-model="selectedAddressId" placeholder="选择已保存地址" style="width: 100%" @change="onAddressChange">
            <el-option
              v-for="a in savedAddresses"
              :key="a.id"
              :label="`${a.receiverName} ${a.receiverPhone} ${a.receiverAddress}`"
              :value="a.id"
            >
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span>{{ a.receiverName }} {{ a.receiverPhone }}</span>
                <el-tag v-if="a.isDefault" size="small" type="success">默认</el-tag>
              </div>
              <div style="font-size: 12px; color: #999;">{{ a.receiverAddress }}</div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="收货人" required>
          <el-input v-model="orderForm.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="orderForm.receiverPhone" placeholder="请输入收货人手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="详细地址" required>
          <el-input v-model="orderForm.receiverAddress" type="textarea" :rows="2" placeholder="请输入详细收货地址" />
        </el-form-item>
        <el-form-item label="">
          <el-checkbox v-model="saveAddressFlag">保存为常用地址</el-checkbox>
        </el-form-item>
        <el-form-item label="支付方式" required>
          <el-radio-group v-model="orderForm.paymentMethod">
            <el-radio :label="0"><span class="pay-icon wechat">微信支付</span></el-radio>
            <el-radio :label="1"><span class="pay-icon alipay">支付宝</span></el-radio>
            <el-radio :label="2"><span class="pay-icon bank">银行卡</span></el-radio>
            <el-radio :label="3"><span class="pay-icon cod">货到付款</span></el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <div class="checkout-summary-bar">
        <span>共 {{ selectedCount }} 件商品，合计</span>
        <span class="pay-price">￥{{ selectedPrice.toFixed(2) }}</span>
      </div>

      <template #footer>
        <el-button @click="checkoutDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOrder">提交订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cartApi, orderApi, addressApi } from '../api'
import { useCartStore } from '../stores/cart'
import { useUserStore } from '../stores/user'

import durianImg from '../assets/images/product-durian.jpg'
import orangeImg from '../assets/images/product-orange.jpg'
import riceImg from '../assets/images/product-rice.jpg'
import scallionImg from '../assets/images/product-scallion.jpg'

const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()
const cartList = ref([])
const loading = ref(false)
const checkoutDialog = ref(false)
const submitting = ref(false)
const allSelected = ref(false)
const orderForm = ref({ receiverName: '', receiverPhone: '', receiverAddress: '', paymentMethod: 0 })
const savedAddresses = ref([])
const selectedAddressId = ref(null)
const saveAddressFlag = ref(false)

const productImages = { '榴莲': durianImg, '脐橙': orangeImg, '大米': riceImg, '葱': scallionImg }
const getProductImage = (p) => {
  if (!p) return durianImg
  if (p.image) return p.image
  for (const key in productImages) { if (p.name?.includes(key)) return productImages[key] }
  return durianImg
}

const selectedItems = computed(() => cartList.value.filter(i => i.cart._selected))
const selectedCount = computed(() => selectedItems.value.reduce((s, i) => s + i.cart.quantity, 0))
const selectedPrice = computed(() => selectedItems.value.reduce((s, i) => s + (i.product?.price || 0) * i.cart.quantity, 0))

const totalQuantity = computed(() => cartList.value.reduce((s, i) => s + i.cart.quantity, 0))
const totalPrice = computed(() => cartList.value.reduce((s, i) => s + (i.product?.price || 0) * i.cart.quantity, 0))

const toggleSelectAll = (val) => {
  cartList.value.forEach(i => { i.cart._selected = val })
}

const loadCart = async () => {
  if (!userStore.isLoggedIn) return
  loading.value = true
  try {
    const res = await cartApi.list(userStore.userInfo?.id)
    cartList.value = (res.data || []).map(item => ({
      ...item,
      cart: { ...item.cart, _selected: true }
    }))
    allSelected.value = true
  } catch (e) {
    ElMessage.error('加载购物车失败')
  } finally { loading.value = false }
}

const updateQuantity = async (cart) => {
  try {
    await cartApi.update({ id: cart.id, quantity: cart.quantity, productId: cart.productId, userId: cart.userId })
    cartStore.fetchCart()
  } catch (e) {
    ElMessage.error('更新数量失败')
  }
}

const removeItem = async (id) => {
  try {
    await cartApi.delete(id)
    ElMessage.success('已删除')
    loadCart()
    cartStore.fetchCart()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const loadAddresses = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const res = await addressApi.list()
    savedAddresses.value = res.data || []
    const defaultAddr = savedAddresses.value.find(a => a.isDefault === 1)
    if (defaultAddr) {
      selectedAddressId.value = defaultAddr.id
      orderForm.value.receiverName = defaultAddr.receiverName
      orderForm.value.receiverPhone = defaultAddr.receiverPhone
      orderForm.value.receiverAddress = defaultAddr.receiverAddress
    }
  } catch {}
}

const onAddressChange = (id) => {
  const addr = savedAddresses.value.find(a => a.id === id)
  if (addr) {
    orderForm.value.receiverName = addr.receiverName
    orderForm.value.receiverPhone = addr.receiverPhone
    orderForm.value.receiverAddress = addr.receiverAddress
  }
}

const checkout = () => {
  if (selectedCount.value === 0) {
    ElMessage.warning('请选择要结算的商品')
    return
  }
  orderForm.value = { receiverName: '', receiverPhone: '', receiverAddress: '', paymentMethod: 0 }
  selectedAddressId.value = null
  saveAddressFlag.value = false
  checkoutDialog.value = true
  loadAddresses()
}

const submitOrder = async () => {
  if (!orderForm.value.receiverName?.trim()) {
    ElMessage.warning('请填写收货人姓名')
    return
  }
  if (!orderForm.value.receiverPhone?.trim() || orderForm.value.receiverPhone.length !== 11) {
    ElMessage.warning('请填写正确的手机号')
    return
  }
  if (!orderForm.value.receiverAddress?.trim()) {
    ElMessage.warning('请填写收货地址')
    return
  }

  if (saveAddressFlag.value) {
    try {
      await addressApi.save({
        receiverName: orderForm.value.receiverName,
        receiverPhone: orderForm.value.receiverPhone,
        receiverAddress: orderForm.value.receiverAddress,
        isDefault: savedAddresses.value.length === 0 ? 1 : 0
      })
    } catch {}
  }

  submitting.value = true
  try {
    const res = await orderApi.checkout({
      userId: userStore.userInfo?.id,
      cartIds: selectedItems.value.map(i => i.cart.id),
      receiverName: orderForm.value.receiverName,
      receiverPhone: orderForm.value.receiverPhone,
      receiverAddress: orderForm.value.receiverAddress,
      paymentMethod: orderForm.value.paymentMethod
    })

    if (res.code === 200) {
      ElMessage.success('下单成功！')
      checkoutDialog.value = false
      cartStore.fetchCart()
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

onMounted(loadCart)
</script>

<style scoped>
.cart-page { max-width: 1100px; margin: 0 auto; }
.page-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 20px; }
.page-header h1 { font-size: 24px; color: var(--color-title); font-family: var(--font-heading); }
.page-header p { font-size: 14px; color: var(--color-text-hint); }

.empty-cart { text-align: center; padding: 80px 20px; color: var(--color-text-hint); }
.empty-cart p { margin: 16px 0 24px; }
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

.cart-content { display: flex; gap: 20px; align-items: flex-start; }
.cart-items { flex: 1; display: flex; flex-direction: column; gap: 0; background: var(--color-card); border-radius: var(--radius-lg); overflow: hidden; box-shadow: var(--shadow-sm); }

.select-all-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  background: var(--color-primary-bg-light);
  border-bottom: 1px solid var(--color-border-light);
}
.selected-info { font-size: 13px; color: var(--color-text-secondary); }

.cart-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-light);
  transition: background 0.2s var(--ease-standard);
}
.cart-item:last-child { border-bottom: none; }
.cart-item.selected { background: var(--color-primary-bg-light); }
.cart-item:hover { background: var(--color-primary-bg-light); }

.item-check { flex-shrink: 0; }
.item-image { width: 80px; height: 80px; border-radius: var(--radius-md); overflow: hidden; flex-shrink: 0; background: var(--color-bg); }
.item-image img { width: 100%; height: 100%; object-fit: cover; }

.item-info { flex: 1; min-width: 0; }
.item-name { font-size: 15px; font-weight: 600; color: var(--color-title); margin-bottom: 4px; }
.item-origin { font-size: 12px; color: var(--color-primary); margin-bottom: 2px; }
.item-desc { font-size: 12px; color: var(--color-text-hint); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-stock-hint { margin-top: 4px; }
.stock-warn { font-size: 12px; color: var(--color-danger); }

.item-price { width: 100px; text-align: right; color: var(--color-text-secondary); font-size: 13px; }
.item-price .symbol { font-size: 12px; }
.item-price .amount { font-size: 15px; }

.item-quantity { width: 130px; }

.item-subtotal { width: 100px; text-align: right; color: var(--color-danger); }
.item-subtotal .symbol { font-size: 12px; }
.item-subtotal .amount { font-size: 17px; font-weight: 700; }

.item-action { flex-shrink: 0; }
.btn-remove {
  width: 32px; height: 32px;
  border: none; background: transparent;
  color: var(--color-text-hint);
  border-radius: var(--radius-full);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s var(--ease-standard);
}
.btn-remove:hover { background: rgba(229, 57, 53, 0.08); color: var(--color-danger); }

.cart-summary {
  width: 280px;
  flex-shrink: 0;
  background: var(--color-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 80px;
}
.summary-title { font-size: 16px; font-weight: 600; color: var(--color-title); margin-bottom: 16px; }
.summary-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; font-size: 14px; color: var(--color-text-secondary); }
.summary-row .total-price { color: var(--color-text); font-weight: 600; }
.free-shipping { color: var(--color-primary); font-size: 13px; }
.summary-divider { height: 1px; background: var(--color-border-light); margin: 12px 0; }
.summary-row.total { font-size: 16px; font-weight: 600; color: var(--color-title); }
.pay-price { color: var(--color-danger); font-size: 22px; font-weight: 700; font-family: var(--font-number); }

.btn-checkout {
  width: 100%;
  margin-top: 16px;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: white;
  cursor: pointer;
  transition: all 0.2s var(--ease-standard);
}
.btn-checkout:hover:not(:disabled) { background: var(--color-primary-dark); box-shadow: var(--shadow-md); transform: translateY(-1px); }
.btn-checkout:disabled { background: var(--color-text-hint); cursor: not-allowed; }

/* Checkout Dialog */
.checkout-items { max-height: 200px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; }
.checkout-item { display: flex; align-items: center; gap: 12px; padding: 8px 0; }
.ck-img { width: 48px; height: 48px; border-radius: var(--radius-sm); object-fit: cover; }
.ck-info { flex: 1; }
.ck-name { font-size: 14px; color: var(--color-title); }
.ck-detail { font-size: 13px; color: var(--color-text-hint); margin-top: 2px; }
.ck-subtotal { font-size: 14px; font-weight: 600; color: var(--color-danger); }

.checkout-form { margin-top: 8px; }

.checkout-summary-bar {
  display: flex; justify-content: flex-end; align-items: center; gap: 8px;
  padding: 12px 16px; background: var(--color-primary-bg-light); border-radius: var(--radius-md);
  margin-top: 8px;
}
.checkout-summary-bar .pay-price { font-size: 20px; }
</style>
