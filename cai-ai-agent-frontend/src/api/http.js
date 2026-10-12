import axios from 'axios'
import { API_BASE_URL } from './config'

/**
 * Axios 实例：用于普通（非流式）的 HTTP 请求。
 * 注意：SSE（text/event-stream）必须使用浏览器原生 EventSource，不要用 axios。
 */
const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error?.response?.status
    const message =
      error?.response?.data?.message ||
      error?.response?.data?.error ||
      error?.message ||
      '请求失败，请稍后重试'
    console.error('[http] 请求异常', status, message)
    return Promise.reject(new Error(message))
  },
)

/**
 * 普通 GET 请求示例（当前页面用不到，SSE 走 EventSource）
 * @param {string} url 接口路径
 * @param {object} params 查询参数
 */
export function get(url, params) {
  return http.get(url, { params })
}

/** 健康检查：用于主页提示后端是否已启动 */
export function ping() {
  return http.get('/', { timeout: 5000 })
}

export default http
