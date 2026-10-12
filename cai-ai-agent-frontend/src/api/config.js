/**
 * 接口地址前缀统一配置
 * - .env 里 VITE_API_BASE_URL 留空 -> 走 vite 代理的 /api
 * - 也可以在 .env 里直接写成 http://localhost:8123/api（需后端开启 CORS）
 */
export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

/** 业务接口路径（与后端约定） */
export const API_PATH = {
  /** 恋爱大师：SSE 流式，GET ?message=xxx&chatId=xxx */
  LOVE_APP_CHAT_SSE: '/ai/love_app/chat/sse',
  /** 超级智能体：SSE 流式，GET ?message=xxx（无 chatId） */
  MANUS_CHAT: '/ai/manus/chat',
}

/**
 * 拼接完整接口地址（EventSource / axios 共用）
 * @param {string} path 以 / 开头的接口路径
 * @returns {string} 完整地址
 */
export function buildApiUrl(path) {
  return `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`
}
