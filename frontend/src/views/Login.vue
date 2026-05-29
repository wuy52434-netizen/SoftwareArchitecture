<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <div class="logo">
          <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
          </svg>
        </div>
        <h1>图书管理系统</h1>
        <p class="subtitle">Library Management System</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="divider">
        <span>或</span>
      </div>

      <div class="quick-login">
        <p>快速登录</p>
        <div class="quick-login-options">
          <el-button type="primary" plain @click="quickLogin('admin')">
            <el-icon><User /></el-icon>
            管理员
          </el-button>
          <el-button type="success" plain @click="quickLogin('user')">
            <el-icon><UserFilled /></el-icon>
            普通用户
          </el-button>
        </div>
      </div>

      <div class="divider">
        <span>或</span>
      </div>

      <div class="biometric-login">
        <p>借书机终端模式</p>
        <div class="biometric-options">
          <el-button type="info" plain @click="goToKiosk">
            <el-icon><Monitor /></el-icon>
            进入借书机
          </el-button>
        </div>
      </div>

      <div class="login-footer">
        <p>&copy; 2024 图书管理系统. All rights reserved.</p>
      </div>
    </div>

    <div class="decoration decoration-1"></div>
    <div class="decoration decoration-2"></div>
    <div class="decoration decoration-3"></div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { User, UserFilled, Monitor } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

onMounted(() => {
  authStore.restoreFromSession()
  if (authStore.isLoggedIn) {
    redirectToDashboard()
  }
})

function redirectToDashboard() {
  if (authStore.isAdmin) {
    router.push('/admin')
  } else {
    router.push('/')
  }
}

async function handleLogin() {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await authStore.login(loginForm.username, loginForm.password)
      ElMessage.success('登录成功')
      redirectToDashboard()
    } catch (error) {
      ElMessage.error(error.message || '登录失败，请检查用户名和密码')
    } finally {
      loading.value = false
    }
  })
}

function quickLogin(type) {
  if (type === 'admin') {
    loginForm.username = 'admin'
    loginForm.password = 'admin123'
  } else {
    loginForm.username = 'user1'
    loginForm.password = '123456'
  }
}

function goToKiosk() {
  router.push('/')
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.login-box {
  background: white;
  border-radius: 24px;
  padding: 48px;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  z-index: 10;
  position: relative;
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.login-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 14px;
  color: #64748b;
}

.login-form {
  margin-bottom: 24px;
}

.login-btn {
  width: 100%;
  font-weight: 600;
  font-size: 16px;
  height: 48px;
}

.divider {
  display: flex;
  align-items: center;
  margin: 24px 0;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e2e8f0;
}

.divider span {
  padding: 0 16px;
  color: #94a3b8;
  font-size: 12px;
}

.quick-login,
.biometric-login {
  text-align: center;
}

.quick-login p,
.biometric-login p {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 12px;
}

.quick-login-options {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.biometric-options {
  display: flex;
  justify-content: center;
}

.login-footer {
  text-align: center;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e2e8f0;
}

.login-footer p {
  font-size: 12px;
  color: #94a3b8;
}

.decoration {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.decoration-1 {
  width: 400px;
  height: 400px;
  top: -100px;
  left: -100px;
}

.decoration-2 {
  width: 300px;
  height: 300px;
  bottom: -50px;
  right: -50px;
}

.decoration-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  right: 10%;
}
</style>
