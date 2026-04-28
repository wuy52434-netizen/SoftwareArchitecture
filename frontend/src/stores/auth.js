import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const token = ref(null)

  const isLoggedIn = computed(() => !!user.value)
  const isAdmin = computed(() => user.value?.role === 'admin')

  async function login(username, password) {
    const response = await authApi.login(username, password)
    if (response.ok) {
      user.value = response.user
      token.value = response.token || null
      sessionStorage.setItem('currentUser', JSON.stringify(response.user))
    }
    return response
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      user.value = null
      token.value = null
      sessionStorage.removeItem('currentUser')
    }
  }

  function restoreFromSession() {
    const savedUser = sessionStorage.getItem('currentUser')
    if (savedUser) {
      try {
        user.value = JSON.parse(savedUser)
      } catch (e) {
        console.error('解析用户信息失败:', e)
        sessionStorage.removeItem('currentUser')
      }
    }
  }

  function getCurrentUser() {
    return user.value
  }

  return {
    user,
    token,
    isLoggedIn,
    isAdmin,
    login,
    logout,
    restoreFromSession,
    getCurrentUser
  }
})
