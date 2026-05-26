import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('accessToken') || null)

  const isLoggedIn = computed(() => !!user.value)
  const isAdmin = computed(() => user.value?.userType === 'admin')

  async function login(username, password) {
    const response = await authApi.login(username, password)
    user.value = response.user
    token.value = response.accessToken
    localStorage.setItem('accessToken', response.accessToken)
    if (response.refreshToken) {
      localStorage.setItem('refreshToken', response.refreshToken)
    }
    localStorage.setItem('currentUser', JSON.stringify(response.user))
    return response
  }

  async function logout() {
    user.value = null
    token.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('currentUser')
  }

  function restoreFromSession() {
    const savedUser = localStorage.getItem('currentUser')
    if (savedUser) {
      try {
        user.value = JSON.parse(savedUser)
      } catch (e) {
        console.error('解析用户信息失败:', e)
        localStorage.removeItem('currentUser')
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
