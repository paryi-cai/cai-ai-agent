import { onBeforeUnmount, ref } from 'vue'

/**
 * 打字机实例（逐字吐出）
 *
 * 为什么需要它：
 * 1) 后端如果按 token 一小段一小段推送，直接 append 就已经是打字机效果；
 * 2) 但很多实现（或经过网关缓冲后）会把一整段内容一次性推过来，
 *    这时用本实例逐字展示，保证任何情况下都有平滑的打字机观感。
 *
 * 每个气泡一个实例：push 时用 existing 告诉它该气泡已有的文本，
 * 这样多个步骤气泡之间不会串内容。
 *
 * @param {(text: string) => void} onReveal 每次吐字后回调“当前已吐出的文本”
 * @param {number} [intervalMs] 吐字间隔（毫秒）
 */
export function createTypewriter(onReveal, intervalMs = 16) {
  /** 是否正在吐字（用于显示光标） */
  const isTyping = ref(false)

  let pending = '' // 待吐出的内容
  let rendered = '' // 已吐出的内容
  let timer = null

  /** 根据积压量动态调整速度，避免内容太多时“追不上” */
  const resolveSpeed = () => {
    if (pending.length > 600) return 6
    if (pending.length > 240) return 4
    if (pending.length > 80) return 2
    return 1
  }

  const stop = () => {
    if (timer !== null) {
      clearInterval(timer)
      timer = null
    }
    isTyping.value = false
  }

  const tick = () => {
    if (!pending) {
      stop()
      return
    }
    const speed = resolveSpeed()
    rendered += pending.slice(0, speed)
    pending = pending.slice(speed)
    onReveal?.(rendered)

    if (!pending) {
      // 吐完当前积压先停下，等下一段推送（省电、无空转）
      stop()
    }
  }

  const start = () => {
    if (timer !== null) return
    isTyping.value = true
    timer = setInterval(tick, intervalMs)
  }

  /**
   * 追加待吐出的文本
   * @param {string} text 本次新增的文本
   * @param {string} [existing] 该气泡已展示的文本（新气泡传空串，避免重复吐出旧内容）
   */
  const push = (text, existing = '') => {
    if (!text) return
    rendered = existing
    pending = text
    start()
  }

  /** 立刻吐完队列里的剩余内容 */
  const flush = () => {
    if (pending) {
      rendered += pending
      pending = ''
      onReveal?.(rendered)
    }
    stop()
  }

  /** 直接落定整段文本（收尾用） */
  const finish = (text) => {
    stop()
    pending = ''
    rendered = text ?? ''
    onReveal?.(rendered)
  }

  /** 重置，准备下一轮 */
  const reset = () => {
    pending = ''
    rendered = ''
    stop()
  }

  return {
    push,
    flush,
    finish,
    reset,
    stop,
    isTyping,
    hasPending: () => pending.length > 0,
  }
}

/**
 * 组件内使用的打字机（自动在卸载时清理定时器）
 * @param {(fullText: string) => void} onReveal
 * @param {number} [intervalMs]
 */
export function useTypewriter(onReveal, intervalMs = 16) {
  const typewriter = createTypewriter(onReveal, intervalMs)
  onBeforeUnmount(() => typewriter.stop())

  return {
    push: (text, existing = '') => typewriter.push(text, existing),
    flush: () => typewriter.flush(),
    finish: (text) => typewriter.finish(text),
    reset: () => typewriter.reset(),
    stop: () => typewriter.stop(),
    isTyping: typewriter.isTyping,
    hasPending: typewriter.hasPending,
  }
}
