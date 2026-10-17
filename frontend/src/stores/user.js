import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '../api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const isMerchant = computed(() => userInfo.value?.role === 'MERCHANT')
  const isFarmer = computed(() => userInfo.value?.role === 'FARMER')
  const roleLabel = computed(() => {
    const r = userInfo.value?.role
    if (r === 'ADMIN') return '系统管理员'
    if (r === 'MERCHANT') return '商家'
    if (r === 'FARMER') return '农户'
    return '客户'
  })
  const canAccessAdmin = computed(() => ['ADMIN', 'MERCHANT', 'FARMER'].includes(userInfo.value?.role))

  async function login(loginForm) {
    const res = await userApi.login(loginForm)
    token.value = res.data.token
    userInfo.value = res.data.user
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('userInfo', JSON.stringify(res.data.user))
    return res
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  function updateUserInfo(data) {
    userInfo.value = { ...userInfo.value, ...data }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  return { token, userInfo, isLoggedIn, isAdmin, isMerchant, isFarmer, roleLabel, canAccessAdmin, login, logout, updateUserInfo }
})
