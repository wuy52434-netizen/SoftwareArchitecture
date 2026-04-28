<template>
  <div class="portal-layout">
    <nav class="navbar">
      <div class="nav-container">
        <div class="nav-left">
          <div class="logo-section" @click="goHome">
            <div class="logo-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
              </svg>
            </div>
            <span class="logo-text">图书自动借书机</span>
          </div>
          <div class="nav-menu">
            <router-link 
              v-for="item in menuItems" 
              :key="item.path"
              :to="item.path"
              class="nav-item"
              :class="{ active: isActiveRoute(item.path) }"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              {{ item.name }}
            </router-link>
          </div>
        </div>
        <div class="nav-right">
          <div class="user-profile">
            <el-avatar :size="32" class="user-avatar">
              <el-icon><User /></el-icon>
            </el-avatar>
            <span class="username">{{ currentUser?.name || currentUser?.username || '用户' }}</span>
          </div>
          <el-button text @click="handleLogout" class="logout-btn">
            <el-icon><SwitchButton /></el-icon>
          </el-button>
        </div>
      </div>
    </nav>

    <main class="main-container">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  HomeFilled, 
  Search, 
  Document, 
  User, 
  SwitchButton 
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const menuItems = [
  { path: '/portal', name: '首页', icon: 'HomeFilled' },
  { path: '/portal/books', name: '图书检索', icon: 'Search' },
  { path: '/portal/my-borrows', name: '我的借阅', icon: 'Document' }
]

const currentUser = computed(() => authStore.user)

onMounted(() => {
  authStore.restoreFromSession()
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
  }
})

function isActiveRoute(path) {
  if (path === '/portal') {
    return route.path === '/portal' || route.path === '/portal/'
  }
  return route.path.startsWith(path)
}

function goHome() {
  router.push('/portal')
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.portal-layout {
  min-height: 100vh;
  background: var(--bg-secondary);
}

.navbar {
  background: var(--navy-dark);
  color: white;
  box-shadow: var(--shadow-md);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 48px;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: white;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-blue);
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.nav-menu {
  display: flex;
  gap: 8px;
}

.nav-item {
  padding: 8px 16px;
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-item:hover {
  color: white;
  background: rgba(255, 255, 255, 0.1);
}

.nav-item.active {
  color: white;
  background: rgba(91, 126, 246, 0.3);
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.2s;
}

.user-profile:hover {
  background: rgba(255, 255, 255, 0.1);
}

.user-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.username {
  font-size: 14px;
  font-weight: 500;
}

.logout-btn {
  color: rgba(255, 255, 255, 0.8);
  padding: 8px;
}

.logout-btn:hover {
  color: white;
  background: rgba(255, 255, 255, 0.1);
}

.main-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px 24px;
}
</style>
