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
                <el-input v-model="registerForm.password" type="password" placeholder="请输入 6-20 位密码" show-password prefix-icon="Lock" size="large" minlength="6" maxlength="20" />
              </el-form-item>
              <!-- 确认密码：只在注册时做一致性校验，不提交给后端 -->
              <el-form-item label="确认密码">
                <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password prefix-icon="Lock" size="large" minlength="6" maxlength="20" @keyup.enter="handleRegister" />
              </el-form-item>
              <el-form-item label="注册身份">
                <el-radio-group v-model="registerForm.role">
                  <el-radio value="CONSUMER">客户</el-radio>
                  <el-radio value="FARMER">农户</el-radio>
                  <el-radio value="MERCHANT">商家</el-radio>
                </el-radio-group>
                <div v-if="registerForm.role === 'MERCHANT'" class="role-hint">
                  商家入驻需管理员审核，提交后请等待审核通过再登录
                </div>
              </el-form-item>
              <!-- 资料非必填说明：注册只收账号信息，资料留到个人中心按需补充 -->
              <div class="optional-hint">真实姓名、手机号等资料可在注册后到「个人中心 → 编辑资料」按需补充，不填也能正常使用。</div>
              <el-button type="primary" @click="handleRegister" size="large" style="width: 100%; margin-top: 10px; border-radius: 8px; height: 48px; font-size: 16px;">注 册</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi } from '../api'
import { useUserStore } from '../stores/user'
import { useCartStore } from '../stores/cart'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const activeTab = ref('login')

const loginForm = ref({ username: '', password: '' })
// 注册表单：只收账号必需信息（用户名 + 密码 + 确认密码 + 身份）
// 真实姓名/手机号不再在注册时收集，统一放到个人中心按需补充
const registerForm = ref({ username: '', password: '', confirmPassword: '', role: 'CONSUMER' })

/** 获取一份干净的注册表单初值（重置用，避免各地手写对象漏字段） */
const emptyRegisterForm = () => ({ username: '', password: '', confirmPassword: '', role: 'CONSUMER' })

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
  // 前端先拦一道长度，避免无效请求；后端 SysUserServiceImpl 仍有同样校验，绕过前端也无效
  const pwdLen = registerForm.value.password.length
  if (pwdLen < 6 || pwdLen > 20) {
    ElMessage.warning('密码长度必须为 6-20 位')
    return
  }
  // 两次密码必须一致：只在前端比对，confirmPassword 不发送给后端
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  // 显式构造提交体，避免把 confirmPassword 这类纯前端字段带到接口上
  const payload = {
    username: registerForm.value.username,
    password: registerForm.value.password,
    role: registerForm.value.role
  }
  try {
    await userApi.register(payload)
    if (registerForm.value.role === 'MERCHANT') {
      // 商家注册后状态是待审核（status=0），此时登录会被后端拒绝，因此不自动跳到登录页
      ElMessageBox.alert(
        '入驻申请已提交，请等待管理员审核。审核通过后即可用该账号登录经营。',
        '提交成功',
        { confirmButtonText: '知道了', type: 'success' }
      )
      registerForm.value = emptyRegisterForm()
      return
    }
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.value.username = registerForm.value.username
    loginForm.value.password = registerForm.value.password
    registerForm.value = emptyRegisterForm()
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
/* 商家入驻提示：只在选中「商家」时出现 */
.role-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.5;
}
/* 资料可选项说明：弱化处理，不抢主流程 */
.optional-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #999;
  line-height: 1.6;
}
@media (max-width: 768px) {
  .login-left { display: none; }
  .login-right { width: 100%; }
}
</style>
