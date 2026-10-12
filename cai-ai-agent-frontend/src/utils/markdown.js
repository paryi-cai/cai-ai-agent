import { marked } from 'marked'

marked.setOptions({
  gfm: true,
  breaks: true,
})

/** 转义 HTML，避免非 Markdown 文本被当成标签解析 */
function escapeHtml(text) {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

/**
 * 把 AI 返回的 Markdown 文本渲染成 HTML（用于 v-html）
 * @param {string} text Markdown 原文
 * @returns {string} HTML 字符串
 */
export function renderMarkdown(text) {
  if (!text) return ''
  try {
    return marked.parse(String(text))
  } catch (error) {
    console.warn('[markdown] 解析失败，按纯文本展示', error)
    return escapeHtml(text).replace(/\n/g, '<br/>')
  }
}

/** 未闭合的代码块 ``` —— 流式输出中先不渲染 Markdown / JSON */
const OPEN_FENCE = /```[\s\S]*$/

/**
 * 流式安全的 Markdown 渲染：
 * - 空内容返回空串，方便调用方判断“还没内容”
 * - 解析失败时退化为纯文本，不会把内容整段丢掉
 * - 代码块未闭合时，先把```去掉，避免闪烁出半截代码样式
 * @param {string} text
 * @returns {string}
 */
export function renderMarkdownSafe(text) {
  const raw = String(text ?? '')
  if (!raw.trim()) return ''
  const withoutFence = OPEN_FENCE.test(raw) ? raw.replace(/```/g, '') : raw
  return renderMarkdown(withoutFence)
}
