/**
 * 聊天室会话 ID 工具
 * 要求：进入页面后自动生成一个聊天室 id 用于区分会话。
 */

/** 生成一个会话 ID，例如 love_1699999999999_a1b2c3d4 */
export function generateChatId(prefix = 'chat') {
  const random = Math.random().toString(16).slice(2, 10)
  return `${prefix}_${Date.now()}_${random}`
}

/**
 * 优先使用 sessionStorage 复用（同一次会话内刷新页面不丢上下文），
 * 关闭标签页后自动失效；取不到时重新生成。
 * @param {string} key 存储 key
 * @param {string} prefix ID 前缀
 */
export function getChatId(key, prefix = 'chat') {
  try {
    const cached = window.sessionStorage.getItem(key)
    if (cached) return cached
    const created = generateChatId(prefix)
    window.sessionStorage.setItem(key, created)
    return created
  } catch {
    return generateChatId(prefix)
  }
}

/** 强制刷新一个新的会话 ID */
export function resetChatId(key, prefix = 'chat') {
  const created = generateChatId(prefix)
  try {
    window.sessionStorage.setItem(key, created)
  } catch {
    /* 忽略隐私模式下的存储异常 */
  }
  return created
}
