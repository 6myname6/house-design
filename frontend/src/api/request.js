import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from '../utils/storage'
import router from '../router'

// 后端统一响应：{ code, message, data }，成功 code = 200
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 30000
})

// 请求拦截：自动带 token（格式 Bearer <token>，与后端一致）
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：统一处理后端 Result 与 HTTP 错误
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端业务码：200 成功，其余为业务错误
    if (res.code === 200) {
      return res.data
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    // HTTP 401：token 失效 → 清空并跳登录页
    if (error.response && error.response.status === 401) {
      removeToken()
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
    } else if (error.response && error.response.status === 404) {
      ElMessage.error('资源不存在')
    } else {
      ElMessage.error(error.response?.data?.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request