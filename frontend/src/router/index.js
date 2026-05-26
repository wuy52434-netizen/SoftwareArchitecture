import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    redirect: '/portal'
  },
  {
    path: '/portal',
    name: 'Portal',
    component: () => import('@/layouts/PortalLayout.vue'),
    meta: { title: '读者门户', requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/portal/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'books',
        name: 'Books',
        component: () => import('@/views/portal/Books.vue'),
        meta: { title: '图书检索' }
      },
      {
        path: 'my-borrows',
        name: 'MyBorrows',
        component: () => import('@/views/portal/MyBorrows.vue'),
        meta: { title: '我的借阅' }
      }
    ]
  },
  {
    path: '/kiosk',
    name: 'Kiosk',
    component: () => import('@/layouts/KioskLayout.vue'),
    meta: { title: '借书机终端' },
    children: [
      {
        path: '',
        name: 'KioskHome',
        component: () => import('@/views/kiosk/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'borrow',
        name: 'KioskBorrow',
        component: () => import('@/views/kiosk/Borrow.vue'),
        meta: { title: '借阅图书' }
      },
      {
        path: 'return',
        name: 'KioskReturn',
        component: () => import('@/views/kiosk/Return.vue'),
        meta: { title: '归还图书' }
      },
      {
        path: 'search',
        name: 'KioskSearch',
        component: () => import('@/views/kiosk/Search.vue'),
        meta: { title: '查询图书' }
      }
    ]
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true },
    redirect: '/admin/books',
    children: [
      {
        path: 'books',
        name: 'AdminBooks',
        component: () => import('@/views/admin/Books.vue'),
        meta: { title: '图书管理' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'borrows',
        name: 'AdminBorrows',
        component: () => import('@/views/admin/Borrows.vue'),
        meta: { title: '借阅记录' }
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('@/views/admin/Settings.vue'),
        meta: { title: '系统设置' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 图书管理系统` : '图书管理系统'
  
  const authStore = useAuthStore()
  
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    next('/login')
    return
  }
  
  if (to.meta.requiresAdmin && authStore.user?.userType !== 'admin') {
    ElMessage.error('权限不足')
    next('/portal')
    return
  }
  
  next()
})

export default router
