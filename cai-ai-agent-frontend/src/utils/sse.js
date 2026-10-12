import { buildApiUrl } from '@/api/config'

/**
 * 服务端完成标记（Spring AI Flux 通常以 [DONE] 结束）
 */
const DONE_FLAG = '[DONE]'

/**
 * 使用浏览器原生 EventSource 调用 SSE 接口（GET + text/event-stream）
 *
 * 约定：
 * - 只使用 GET，查询参数用 encodeURIComponent 编码（消息里可能有中文 / emoji / & 等字符）
 * - 服务端每个事件推一段内容，前端收到即回调，从而实现打字机效果
 * - 不再需要时务必调用 close()，否则连接会一直挂着
 *
 * @param {object} options
 * @param {string} options.path 接口路径，如 /ai/food_app/chat/sse
 * @param {object} [options.params] 查询参数，会自动 encodeURIComponent
 * @param {(text: string) => void} [options.onMessage] 收到一段内容
 * @param {() => void} [options.onDone] 服务端正常结束
 * @param {(error: { message: string, type: string }) => void} [options.onError] 连接异常
 * @returns {{ close: () => void, aborted: () => boolean }}
 */
export function createSseConnection({ path, params = {}, onMessage, onDone, onError } = {}) {
  const query = Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join('&')

  const url = `${buildApiUrl(path)}${query ? `?${query}` : ''}`

  let abortFlag = false
  let finished = false // 是否已收到完成标记 / 已回调 onDone
  let opened = false // 连接是否成功建立过
  let source = null

  /** 只允许结束一次 */
  const finish = () => {
    if (finished) return
    finished = true
    onDone?.()
  }

  /** 关闭连接 */
  const close = () => {
    if (source) {
      source.close()
      source = null
    }
  }

  /** 完成标记可能在内容中间，需要截断后结束 */
  const handleText = (text) => {
    if (!text) return
    const flagIndex = text.indexOf(DONE_FLAG)
    if (flagIndex === -1) {
      onMessage?.(text)
      return
    }
    if (flagIndex > 0) onMessage?.(text.slice(0, flagIndex))
    close()
    finish()
  }

  try {
    // EventSource 只能发起 GET 请求，且自带断线重连能力
    source = new EventSource(url)
  } catch (error) {
    onError?.({ type: 'init', message: `EventSource 初始化失败：${error?.message || error}` })
    return { close, aborted: () => abortFlag }
  }

  source.onopen = () => {
    opened = true
    console.debug('[sse] 连接已建立', url)
  }

  // 默认事件：服务端以 `data:xxx\n\n` 推送（onmessage 等价于监听 message 事件，不要重复绑定）
  source.onmessage = (event) => {
    if (event.data == null) return
    handleText(event.data)
  }

  // 兼容服务端使用自定义事件名结束（event: done）
  source.addEventListener('done', () => {
    close()
    finish()
  })

  source.onerror = () => {
    if (abortFlag || finished) {
      // 用户主动停止 或 服务端已正常结束，不需要报错
      close()
      return
    }
    close()
    if (opened) {
      // 后端把内容推完直接断开（未发 [DONE]）是很常见的行为，按正常结束处理
      finish()
      return
    }
    onError?.({
      type: 'connect',
      message: '无法连接到后端服务，请确认后端已启动（默认 http://localhost:8123）',
    })
  }

  return {
    close,
    aborted: () => abortFlag,
    // 供外部标记“用户主动停止”
    abort() {
      abortFlag = true
      close()
    },
  }
}
