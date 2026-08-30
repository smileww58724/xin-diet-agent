import axios from 'axios'
import { ElMessage } from 'element-plus'

// 使用相对路径走 Vite 的 /api 代理（见 vite.config.js），避免跨域（CORS）问题：
// 之前硬编码 http://localhost:8080/api，当页面以 http://127.0.0.1:5173 打开时
// 会被后端 CORS 白名单拦截，所有请求直接显示"请求失败"。
const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  response => response.data,
  error => {
    const status = error.response?.status
    const message = error.response?.data?.message

    // 401/403：token 缺失、过期或无效，清理本地登录态并回到登录页
    if (status === 401 || status === 403) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('userId')
      if (window.location.pathname !== '/login') {
        ElMessage.error(message || '登录已过期，请重新登录')
        window.location.href = '/login'
      } else {
        ElMessage.error(message || '登录失败，请检查用户名和密码')
      }
      return Promise.reject(error)
    }

    if (message) {
      ElMessage.error(message)
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else if (!error.response) {
      ElMessage.error('网络异常，请确认后端服务已启动')
    } else {
      ElMessage.error(`请求失败（${status || '未知错误'}）`)
    }
    return Promise.reject(error)
  }
)

export const authAPI = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
}

export const dietAPI = {
  getRecords: (date) => api.get('/diet/records', { params: { date } }),
  addRecord: (data) => api.post('/diet/records', data),
  updateRecord: (id, data) => api.put(`/diet/records/${id}`, data),
  deleteRecord: (id) => api.delete(`/diet/records/${id}`),
}

export const analysisAPI = {
  getDaily: (date) => api.get('/analysis/daily', { params: { date } }),
  getWeekly: (startDate) => api.get('/analysis/weekly', { params: { startDate } }),
}

export const goalsAPI = {
  getDaily: (date) => api.get('/goals/daily', { params: { date } }),
  setDaily: (data) => api.post('/goals/daily', data),
  getHistory: (startDate, endDate) => api.get('/goals/history', { params: { startDate, endDate } }),
  getProfile: () => api.get('/goals/profile'),
  updateProfile: (data) => api.put('/goals/profile', data),
}

export const agentAPI = {
  chat: (message) => api.post('/agent/chat', { message }),
  chatStream: (message, signal) => {
    const token = localStorage.getItem('token')
    // 走同源 /api 代理，避免跨域问题
    // userId 不再随请求体传递，后端统一从 Authorization token 解析，防止越权冒用他人身份
    return fetch('/api/agent/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify({ message }),
      signal,
    })
  },
  stopGeneration: () => api.post('/agent/chat/stop'),
  clearMemory: () => api.delete('/agent/memory'),
  history: () => api.get('/agent/memory'),
  usage: () => api.get('/agent/usage'),
}

export default api
