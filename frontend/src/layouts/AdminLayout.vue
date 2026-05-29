<template>
  <el-container class="admin-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="admin-aside">
      <div class="logo-section">
        <div class="logo-icon">
          <el-icon><Reading /></el-icon>
        </div>
        <div class="logo-text" v-show="!isCollapse">
          <h1>图书管理系统</h1>
          <p>管理后台</p>
        </div>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        class="admin-menu"
        router
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>统计看板</template>
        </el-menu-item>
        <el-menu-item index="/admin/books">
          <el-icon><Collection /></el-icon>
          <template #title>图书管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/borrows">
          <el-icon><Document /></el-icon>
          <template #title>借阅记录</template>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/settings">
          <el-icon><Setting /></el-icon>
          <template #title>系统设置</template>
        </el-menu-item>
      </el-menu>

      <div class="collapse-btn" @click="isCollapse = !isCollapse">
        <el-icon><ArrowLeft v-if="!isCollapse" /><ArrowRight v-else /></el-icon>
      </div>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <div class="quick-actions">
            <el-tooltip content="刷新数据" placement="bottom">
              <el-button type="primary" text @click="handleRefresh">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="系统通知" placement="bottom">
              <el-badge :value="3" class="notification-badge">
                <el-button type="primary" text>
                  <el-icon><Bell /></el-icon>
                </el-button>
              </el-badge>
            </el-tooltip>
          </div>
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-profile">
              <el-avatar :size="36" class="user-avatar">
                <el-icon><UserFilled /></el-icon>
              </el-avatar>
              <div class="user-info" v-show="!isCollapse">
                <span class="user-name">{{ userStore.user?.name || userStore.user?.username || '管理员' }}</span>
                <span class="user-role">{{ userStore.isAdmin ? '超级管理员' : '管理员' }}</span>
              </div>
              <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="kiosk">
                  <el-icon><HomeFilled /></el-icon>
                  进入借书机
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Reading,
  Collection,
  DataAnalysis,
  Document,
  User,
  Setting,
  ArrowLeft,
  ArrowRight,
  Refresh,
  Bell,
  UserFilled,
  ArrowDown,
  HomeFilled,
  SwitchButton
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const userStore = useAuthStore()

const isCollapse = ref(false)

const activeMenu = computed(() => route.path)

const currentPageTitle = computed(() => {
  const path = route.path
  if (path.includes('dashboard')) return '统计看板'
  if (path.includes('books')) return '图书管理'
  if (path.includes('borrows')) return '借阅记录'
  if (path.includes('users')) return '用户管理'
  if (path.includes('settings')) return '系统设置'
  return '管理后台'
})

function handleRefresh() {
  router.go(0)
}

async function handleCommand(command) {
  switch (command) {
    case 'profile':
      ElMessageBox.alert(
        `用户名：${userStore.user?.username || '-'}\n姓名：${userStore.user?.realName || userStore.user?.name || '-'}\n角色：${userStore.isAdmin ? '超级管理员' : '管理员'}`,
        '个人中心',
        { confirmButtonText: '确定' }
      )
      break
    case 'kiosk':
      router.push('/')
      break
    case 'logout':
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      userStore.logout()
      router.push('/login')
      break
  }
}
</script>

<style scoped>
.admin-container {
  min-height: 100vh;
}

.admin-aside {
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%);
  transition: width 0.3s ease;
  display: flex;
  flex-direction: column;
  position: relative;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-icon {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  flex-shrink: 0;
}

.logo-text h1 {
  font-size: 16px;
  font-weight: 700;
  color: white;
  margin: 0;
  line-height: 1.2;
}

.logo-text p {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin: 4px 0 0 0;
}

.admin-menu {
  border-right: none;
  background: transparent;
  flex: 1;
  margin-top: 20px;
}

.admin-menu :deep(.el-menu-item) {
  margin: 4px 12px;
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.7);
  height: 48px;
  line-height: 48px;
}

.admin-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
}

.admin-menu :deep(.el-menu-item .el-icon) {
  font-size: 20px;
}

.collapse-btn {
  padding: 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.6);
  transition: all 0.3s;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.collapse-btn:hover {
  color: white;
  background: rgba(255, 255, 255, 0.05);
}

.collapse-btn .el-icon {
  font-size: 20px;
}

.admin-header {
  background: white;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.quick-actions {
  display: flex;
  gap: 8px;
  border-right: 1px solid var(--border-color);
  padding-right: 16px;
}

.notification-badge {
  display: inline-flex;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.3s;
}

.user-profile:hover {
  background: var(--bg-secondary);
}

.user-avatar {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1d4ed8;
}

.user-info {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.user-role {
  font-size: 12px;
  color: var(--text-secondary);
}

.dropdown-icon {
  font-size: 12px;
  color: var(--text-secondary);
}

.admin-main {
  background: var(--bg-primary);
  padding: 24px;
}
</style>
