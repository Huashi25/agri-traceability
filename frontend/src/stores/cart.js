import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { cartApi } from '../api'
import { useUserStore } from './user'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const loading = ref(false)

  const totalCount = computed(() =>
    items.value.reduce((sum, item) => sum + (item.cart?.quantity || 0), 0)
  )

  async function fetchCart() {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) return
    loading.value = true
    try {
      const res = await cartApi.list(userStore.userInfo.id)
      items.value = res.data || []
    } catch (e) {
      console.error('Failed to load cart:', e)
    } finally {
      loading.value = false
    }
  }

  async function addToCart(productId, quantity = 1) {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) {
      return false
    }
    try {
      await cartApi.add({
        userId: userStore.userInfo.id,
        productId,
        quantity
      })
      await fetchCart()
      return true
    } catch (e) {
      ElMessage.error('加入购物车失败')
      return false
    }
  }

  function clearCart() {
    items.value = []
  }

  return { items, loading, totalCount, fetchCart, addToCart, clearCart }
})
