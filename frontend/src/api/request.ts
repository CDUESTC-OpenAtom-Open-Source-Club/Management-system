import axios from 'axios'
import { message } from 'antd'
import { getToken, clearAuth } from '../utils/auth'

const request = axios.create({
  baseURL: '',
  timeout: 15000,
})

// 请求拦截器：自动注入 JWT Bearer token
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一处理 code 和 HTTP 错误
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 0) {
      return res.data
    }
    message.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        // Token 失效，清除登录状态并跳转到登录页
        clearAuth()
        message.error('登录已过期，请重新登录')
        // 延迟跳转，让 message 显示完毕
        setTimeout(() => {
          window.location.href = '/login'
        }, 1000)
      } else if (status === 403) {
        message.error('权限不足，无法执行该操作')
      } else if (status === 404) {
        message.error('请求的资源不存在')
      } else if (status === 500) {
        message.error('服务器错误，请稍后重试')
      } else {
        message.error(error.response.data?.message || `请求失败 (${status})`)
      }
    } else if (error.code === 'ECONNABORTED') {
      message.error('请求超时，请检查后端服务是否启动')
    } else {
      message.error('网络错误，请检查后端服务是否启动')
    }
    return Promise.reject(error)
  }
)

export default request
