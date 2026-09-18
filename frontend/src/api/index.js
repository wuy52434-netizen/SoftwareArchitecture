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
    // 业务错误（HTTP 200 但 code != 200）**只 reject，不弹提示**。
    //
    // 修复 KNWN-UI-01：原先这里 ElMessage.error 弹一次，调用方 catch 里又
    // ElMessage.error(error.message) 弹一次，同一次失败出现两条完全相同的提示（实测「密码错误」×2）。
    // 职责划分：业务错误只有调用方知道上下文（登录页要提示"密码错误"，列表页要提示"加载失败"），
    // 因此交由调用方提示；拦截器只负责与会话相关的全局行为（见下方 401 分支）。
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  (error) => {
    const message = error.response?.data?.message || error.response?.data?.error || error.message || '请求失败'

    if (error.response?.status === 401) {
      // 会话过期是全局事件：清凭据并回登录页。调用方无从得知，因此这里保留提示。
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('currentUser')
      const isKiosk = window.location.pathname.startsWith('/kiosk')
      if (!window.location.pathname.includes('login') && !isKiosk) {
        // grouping: true —— 同一提示短时间内只保留一条，避免并发请求各弹一次
        ElMessage({ type: 'warning', message: '登录已过期，请重新登录', grouping: true })
        setTimeout(() => {
          window.location.href = '/login'
        }, 1000)
      }
    }
    // 其余传输层错误同样不在这里弹：由调用方给出带上下文文案的提示，
    // 避免"服务器错误，请稍后重试"与调用方的"删除失败"叠加成两条。
    return Promise.reject(Object.assign(new Error(message), { response: error.response, status: error.response?.status }))
  }
)

export default api
