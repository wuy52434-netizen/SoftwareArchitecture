import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  (response) => {
    const { data } = response
    if (data.code === 200 || data.code === 0) {
      return data.data
    }
    ElMessage.error(data.message || '请求失败')
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  (error) => {
    const message = error.response?.data?.message || error.response?.data?.error || error.message || '请求失败'
    
    if (error.response?.status === 401) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('currentUser')
      const isKiosk = window.location.pathname.startsWith('/kiosk')
      if (!window.location.pathname.includes('login') && !isKiosk) {
        ElMessage.warning('登录已过期，请重新登录')
        setTimeout(() => {
          window.location.href = '/login'
        }, 1000)
      }
    } else if (error.response?.status === 403) {
      ElMessage.error('权限不足')
    } else if (error.response?.status === 404) {
      ElMessage.error('请求的资源不存在')
    } else if (error.response?.status >= 500) {
      ElMessage.error('服务器错误，请稍后重试')
    } else {
      ElMessage.error(message)
    }
    
    return Promise.reject({ ...error, message })
  }
)

export default api
