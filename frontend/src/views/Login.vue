<template>
  <div class="login-page">
    <div class="login-left">
      <div class="brand-area">
        <div class="brand-icon">
          <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="white" stroke-width="2">
            <path d="M12 2L4 7v6c0 5 3.5 8.5 8 10 4.5-1.5 8-5 8-10V7l-8-5z"/>
            <path d="M9 12l2 2 4-4" stroke-width="2"/>
          </svg>
        </div>
        <h1>鲜源农产品溯源销售平台</h1>
        <p>从田间到餐桌，全程可追溯</p>
        <div class="features">
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>全链路溯源，产地质检物流一目了然</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>扫码查询，来源信息透明可信</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>在线商城，新鲜农产品直达</span>
          </div>
        </div>
      </div>
    </div>
    <div class="login-right">
      <div class="login-box">
        <h2>{{ activeTab === 'login' ? '欢迎回来' : '创建账号' }}</h2>
        <p class="subtitle">{{ activeTab === 'login' ? '请登录您的账号' : '注册一个新账号开始购物' }}</p>
        <el-tabs v-model="activeTab" class="login-tabs">
          <el-tab-pane label="登录" name="login">
            <el-form :model="loginForm" label-position="top">
              <el-form-item label="用户名">
                <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" size="large" @keyup.enter="handleLogin" />
              </el-form-item>
              <el-form-item label="密码">
                <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password prefix-icon="Lock" size="large" @keyup.enter="handleLogin" />
              </el-form-item>
              <el-button type="primary" @click="handleLogin" size="large" style="width: 100%; margin-top: 10px; border-radius: 8px; height: 48px; font-size: 16px;">登 录</el-button>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="注册" name="register">
            <el-form :model="registerForm" label-position="top">
              <el-form-item label="用户名">
                <el-input v-model="registerForm.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
              </el-form-item>
              <el-form-item label="密码">
                <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password prefix-icon="Lock" size="large" />
              </el-form-item>
              <el-form-item label="真实姓名">
                <el-input v-model="registerForm.realName" placeholder="请输入真实姓名" size="large" />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="registerForm.phone" placeholder="请输入手机号" size="large" />
              </el-form-item>
              <el-form-item label="注册身份">
                <el-radio-group v-model="registerForm.role">
                  <el-radio value="CONSUMER">客户</el-radio>
                  <el-radio value="FARMER">农户</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-button type="primary" @click="handleRegister" size="large" style="width: 100%; margin-top: 10px; border-radius: 8px; height: 48px; font-size: 16px;">注 册</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
        <div class="demo-tip">
          <div>管理员: admin / admin123</div>
          <div>商家: merchant / 123456</div>
          <div>农户: farmer01 / 123456</div>
          <div>客户: consumer / 123456</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '../api'
import { useUserStore } from '../stores/user'
import { useCartStore } from '../stores/cart'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const activeTab = ref('login')

const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', password: '', realName: '', phone: '', role: 'CONSUMER' })

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  try {
    const res = await userStore.login(loginForm.value)
    ElMessage.success('登录成功')
    await cartStore.fetchCart()
    if (['ADMIN', 'MERCHANT', 'FARMER'].includes(res.data.user.role)) {
      router.push('/admin')
    } else {
      router.push('/')
    }
  } catch (e) { /* handled by interceptor */ }
}

const handleRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  try {
    await userApi.register(registerForm.value)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.value.username = registerForm.value.username
    loginForm.value.password = registerForm.value.password
  } catch (e) { /* handled by interceptor */ }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
}
.login-left {
  flex: 1;
  background: linear-gradient(135deg, #1a5c25 0%, #2d8c3c 50%, #3da655 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
.login-left::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -30%;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: rgba(255,255,255,0.05);
}
.login-left::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -20%;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: rgba(255,255,255,0.03);
}
.brand-area {
  position: relative;
  z-index: 1;
  color: white;
  max-width: 400px;
  padding: 40px;
}
.brand-icon {
  width: 80px;
  height: 80px;
  background: rgba(255,255,255,0.15);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.2);
}
.brand-area h1 {
  font-size: 32px;
  margin-bottom: 8px;
  font-weight: 700;
}
.brand-area > p {
  font-size: 16px;
  opacity: 0.85;
  margin-bottom: 40px;
}
.features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  opacity: 0.9;
}
.feature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #a8f0b8;
  flex-shrink: 0;
  box-shadow: 0 0 12px rgba(168,240,184,0.6);
}
.login-right {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
}
.login-box {
  width: 100%;
  max-width: 360px;
  padding: 40px;
}
.login-box h2 {
  font-size: 26px;
  color: #1a1a1a;
  margin-bottom: 8px;
}
.subtitle {
  color: #999;
  font-size: 14px;
  margin-bottom: 24px;
}
.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}
.login-tabs :deep(.el-tabs__item) {
  font-size: 15px;
}
.login-tabs :deep(.el-form-item__label) {
  font-weight: 500;
  color: #555;
}
.demo-tip {
  margin-top: 20px;
  padding: 12px;
  background: #f0f9eb;
  border-radius: 8px;
  font-size: 12px;
  color: #67c23a;
  line-height: 1.8;
}
@media (max-width: 768px) {
  .login-left { display: none; }
  .login-right { width: 100%; }
}
</style>
