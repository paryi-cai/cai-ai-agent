import { onBeforeUnmount, ref } from 'vue'

/**
 * 打字机实例（逐字吐出）
 *
 * 为什么需要它：
 * 1) 后端按 token 一小段一小段推送时，直接 append 就已经是打字机效果；
 * 2) 但很多实现（或经过网关缓冲后）会把一整段内容一次性推过来，
 *    这时用本实例逐字展示，保证任何情况下都有平滑的打字机观感。
 *
 * 设计要点（重要）：
 * 状态只以「完整文本 source + 已吐字数 revealed」为准。
 * 调用方每次把**完整文本**交给 updateSource() 即可，不需要（也不应该）自己算增量、
 * 更不要传"当前已显示的文本"当基准——一旦基准与界面漂移，就会切错字符串、吞掉内容。
 */
export function createTypewriter(onReveal, intervalMs = 16) {
  /** 是否正在吐字（用于显示光标） */
  const isTyping = ref(false)

  /** 完整文本（唯一状态源） */
  let source = ''
  /** 已吐出的字符数（只往前走，不依赖外部传入） */
  let revealed = 0
  let timer = null

  /** 根据积压量动态调整速度，避免内容太多时“追不上” */
  const resolveSpeed = (backlog) => {
    if (backlog > 600) return 6
    if (backlog > 240) return 4
    if (backlog > 80) return 2
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
    const backlog = source.length - revealed
    if (backlog <= 0) {
      stop()
      return
    }
    revealed = Math.min(source.length, revealed + resolveSpeed(backlog))
    onReveal?.(source.slice(0, revealed))
    if (source.length - revealed <= 0) stop()
  }

  const start = () => {
    if (timer !== null) return
    isTyping.value = true
    timer = setInterval(tick, intervalMs)
  }

  /**
   * 更新完整文本（SSE 每收到一段就调用一次）
   *
   * - 正常流式：文本变长 → 从上次吐到的位置继续吐（已显示的字符不重复吐）
   * - 文本被替换（换气泡/重放）：已吐字数超过新长度时回退到新长度
   *
   * @param {string} text 该气泡当前的完整文本
   */
  const updateSource = (text) => {
    source = String(text ?? '')
    if (revealed > source.length) revealed = source.length
    if (source.length - revealed > 0) start()
    else stop()
  }

  /** 立刻显示全部内容并停止动画（收尾用） */
  const revealAll = (text) => {
    if (text !== undefined) source = String(text ?? '')
    revealed = source.length
    stop()
    onReveal?.(source)
  }

  /** 重置，准备下一轮 */
  const reset = () => {
    source = ''
    revealed = 0
    stop()
  }

  return {
    updateSource,
    revealAll,
    reset,
    stop,
    isTyping,
    /** 当前已吐出的文本 */
    renderedText: () => source.slice(0, revealed),
    hasPending: () => source.length - revealed > 0,
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
    updateSource: (text) => typewriter.updateSource(text),
    revealAll: (text) => typewriter.revealAll(text),
    reset: () => typewriter.reset(),
    stop: () => typewriter.stop(),
    isTyping: typewriter.isTyping,
    renderedText: typewriter.renderedText,
    hasPending: typewriter.hasPending,
  }
}
