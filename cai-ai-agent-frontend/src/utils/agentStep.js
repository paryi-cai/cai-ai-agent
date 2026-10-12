/**
 * 智能体步骤解析
 *
 * 超级智能体每一步的推送是一整段文本，形如：
 *   Step 1: 工具 searchWeb 完成了它的任务！结果: "[{...搜索结果 JSON...}]"
 *   Step 8: 工具 generatePDF 完成了它的任务！结果: "PDF generated successfully to: D:\...\xx.pdf"
 *   Step 9: 工具 doTerminate 完成了它的任务！结果: "任务结束"
 *   最后一步：普通 Markdown 正文（最终答复）
 *
 * 这里把这些原始文本解析成结构化数据（工具名、搜索条目、文件路径、纯文本），
 * 交给气泡组件做成“可读的文档”，避免把一坨 JSON 直接糊在页面上。
 */

/** 步骤头部：Step 1: / 第 1 步： */
const STEP_HEADER = /^\s*(?:\[?Step\s*(\d+)\]?|第\s*(\d+)\s*步)\s*[:：.、]?\s*/i

/** 工具调用结果：中英文两种写法都兼容 */
const TOOL_RESULT =
  /^(?:工具|工具调用|tool|Tool|ToolCall)?\s*[「"']?([A-Za-z_][\w-]*)[」"']?\s*(?:完成了它的任务|执行完成|已完成|finished|completed|done)\s*[!！。]?\s*(?:结果|result|输出|output)?\s*[:：]?\s*([\s\S]*)$/i

const JSON_START = /^\s*[[{]/

export const STEP_KIND = {
  /** 工具调用步骤 */
  TOOL: 'tool',
  /** 最终答复 */
  FINAL: 'final',
  /** 其他普通内容 */
  TEXT: 'text',
}

/** 单个搜索结果最多展示多少条（其余折叠起来） */
export const DEFAULT_VISIBLE_RESULTS = 3
/** 摘要最多展示多少个字 */
export const SNIPPET_MAX = 220

/** 去掉包裹在外层的引号，以及 BOM / 零宽字符等不可见前缀 */
function stripQuotes(text) {
  let value = (text ?? '').replace(/^[\uFEFF\u200B\s]+/, '').trim()
  if (value.length >= 2) {
    const first = value[0]
    const last = value[value.length - 1]
    if ((first === '"' && last === '"') || (first === "'" && last === "'")) {
      value = value.slice(1, -1)
    }
  }
  return value.trim()
}

/** 反转义：智能体工具结果里的引号通常是被转义过的 */
function unescapeJsonText(text) {
  return text
    .replace(/\\"/g, '"')
    .replace(/\\n/g, '\n')
    .replace(/\\r/g, '')
    .replace(/\\t/g, '  ')
    .replace(/\\\\/g, '\\')
}

/** 扫描：把 `{...},{...}` 这种“裸对象串”切成单个对象的 JSON 文本 */
function scanJsonObjects(text) {
  const chunks = []
  let depth = 0
  let start = -1
  let inString = false
  let escaped = false

  for (let index = 0; index < text.length; index += 1) {
    const char = text[index]

    if (inString) {
      if (escaped) escaped = false
      else if (char === '\\') escaped = true
      else if (char === '"') inString = false
      continue
    }

    if (char === '"') {
      inString = true
      continue
    }
    if (char === '{' || char === '[') {
      if (depth === 0) start = index
      depth += 1
      continue
    }
    if (char === '}' || char === ']') {
      depth -= 1
      if (depth === 0 && start >= 0) {
        chunks.push(text.slice(start, index + 1))
        start = -1
      }
      if (depth < 0) depth = 0
    }
  }

  return chunks
}

/**
 * 尝试解析 JSON，兼容三种真实情况：
 * 1) 标准 JSON 数组 / 对象；
 * 2) 整体被转义过的 JSON 字符串；
 * 3) 多个对象用逗号拼接、没有外层方括号的“裸对象串”（百度搜索结果就是这种）。
 */
function tryParseJson(text) {
  const raw = (text ?? '').trim()
  if (!JSON_START.test(raw)) return null

  const candidates = [raw]
  const unescaped = unescapeJsonText(raw)
  if (unescaped !== raw) candidates.push(unescaped)

  for (const candidate of candidates) {
    try {
      return JSON.parse(candidate)
    } catch {
      /* 继续尝试下一种 */
    }
  }

  // 裸对象串：拼成数组再解析
  for (const candidate of candidates) {
    const objects = scanJsonObjects(candidate)
    if (objects.length < 1) continue
    try {
      // 单个对象也可以直接包成数组（后端返回单条搜索结果时就是这样）
      return JSON.parse(`[${objects.join(',')}]`)
    } catch {
      /* 忽略 */
    }
  }

  return null
}

/** 从 URL 中取出域名，用作条目的来源标注 */
export function extractDomain(link) {
  if (!link) return ''
  try {
    return new URL(link).hostname.replace(/^www\./, '')
  } catch {
    return link.slice(0, 40)
  }
}

/** 把任意值转成适合展示的文本 */
export function stringifyValue(value) {
  if (value === null || value === undefined) return ''
  if (typeof value === 'string') return value
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  try {
    const inline = JSON.stringify(value)
    if (inline.length <= 120) return inline
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

/** 把一条原始结果规范成展示用的条目 */
function normalizeEntry(item, index) {
  if (item === null || item === undefined) {
    return { position: index + 1, title: '(空)', snippet: '', link: '', domain: '', date: '' }
  }
  if (typeof item !== 'object') {
    return {
      position: index + 1,
      title: String(item),
      snippet: '',
      link: '',
      domain: '',
      date: '',
    }
  }

  const link = item.link || item.url || item.href || ''
  const snippet = item.snippet || item.summary || item.description || item.content || ''
  const title = item.title || item.name || item.displayed_link || link || `第 ${index + 1} 条`

  return {
    position: Number(item.position) || index + 1,
    title: stringifyValue(title),
    snippet: stringifyValue(snippet),
    link: stringifyValue(link),
    domain: extractDomain(link) || stringifyValue(item.displayed_link || ''),
    displayedLink: stringifyValue(item.displayed_link || ''),
    date: stringifyValue(item.date || item.publish_time || item.time || ''),
    extra: item.thumbnail ? { thumbnail: item.thumbnail } : null,
  }
}

/** 提取数组里的搜索条目（只要有一条像搜索结果就按搜索结果渲染） */
function pickSearchEntries(list) {
  const entries = list.filter((item) => item && typeof item === 'object')
  if (!entries.length) return null
  const looksLikeSearch = entries.some(
    (item) => item.title || item.link || item.snippet || item.position,
  )
  return looksLikeSearch ? entries.map(normalizeEntry) : null
}

/** 文件路径 */
function extractFilePath(text) {
  const matched = String(text).match(/[A-Za-z]:\\[^\s"']+|\/[\w./-]+\.(?:pdf|docx?|xlsx?|md|txt|csv|png|jpe?g|zip)/i)
  return matched ? matched[0] : ''
}

/** 一行摘要：把结果压成一句话 */
function buildDigest(text) {
  if (!text) return ''
  const firstLine = String(text)
    .split('\n')
    .map((line) => line.trim())
    .find((line) => line.length > 0)
  if (!firstLine) return ''
  return firstLine.length > 80 ? `${firstLine.slice(0, 80)}…` : firstLine
}

/**
 * 解析一段步骤文本
 * @param {string} raw 原始步骤文本（含 "Step N:" 前缀）
 * @param {number} [activeStepNo] 流式输出中已知的步骤号（文本还没解析完整时用）
 * @returns {{
 *   stepNo: number|null, kind: string, toolName: string, status: string,
 *   digest: string, entries: Array, visibleEntries: Array, extraCount: number,
 *   jsonText: string, text: string, raw: string,
 *   filePath: string, isLong: boolean, isSearch: boolean
 * }}
 */
export function parseStep(raw, activeStepNo = null) {
  const original = String(raw ?? '').replace(/^[\uFEFF\u200B]+/, '')

  // 1) 去掉步骤标记，取出步骤号
  const headerMatch = original.match(STEP_HEADER)
  const stepNo = headerMatch
    ? Number(headerMatch[1] || headerMatch[2]) || activeStepNo
    : activeStepNo

  let body = headerMatch ? original.slice(headerMatch[0].length) : original

  // 2) 是否包裹着外层引号
  const wasQuoted = /^\s*"/.test(body) && /"\s*$/.test(body)
  const inner = stripQuotes(body)

  // 3) 工具调用结果
  const toolMatch = inner.match(TOOL_RESULT)
  if (toolMatch) {
    const toolName = toolMatch[1]
    const resultText = stripQuotes(toolMatch[2])
    const json = tryParseJson(resultText)
    const entries = Array.isArray(json)
      ? pickSearchEntries(json)
      : json && typeof json === 'object'
        ? pickSearchEntries(
            ['results', 'data', 'items', 'list'].map((key) => json[key]).find(Array.isArray) || [],
          )
        : null
    const filePath = extractFilePath(resultText)

    // 纯文本结果里的 \n 是转义过的（例如 readFile 读到的文件内容），还原成真正的换行
    const plainText = entries ? '' : unescapeJsonText(resultText)
    const text = entries ? `${entries.length} 条结果` : plainText

    return {
      stepNo,
      kind: STEP_KIND.TOOL,
      toolName,
      status: 'done',
      digest: buildDigest(text) || `${toolName} 执行完成`,
      entries: entries || [],
      visibleEntries: entries ? entries.slice(0, DEFAULT_VISIBLE_RESULTS) : [],
      extraCount: entries ? Math.max(0, entries.length - DEFAULT_VISIBLE_RESULTS) : 0,
      jsonText: json ? JSON.stringify(json, null, 2) : '',
      text,
      raw: original,
      filePath,
      isSearch: Boolean(entries),
      /** 纯文本结果（文件内容、报错等）→ 按 Markdown 渲染，换行/列表更好看 */
      isText: !entries && plainText.length > 0,
      isLong: resultText.length > 400,
    }
  }

  // 4) 普通正文（含最终答复）
  const text = wasQuoted ? unescapeJsonText(inner) : body.trim() || original.trim()

  return {
    stepNo,
    kind: STEP_KIND.FINAL,
    toolName: '',
    status: 'done',
    digest: buildDigest(text),
    entries: [],
    visibleEntries: [],
    extraCount: 0,
    jsonText: '',
    text,
    raw: original,
    filePath: extractFilePath(text),
    isSearch: false,
    isText: text.length > 0,
    isLong: text.length > 900,
  }
}

/**
 * 流式输出中：文本还只解析到一半时，先给出轻量信息（工具名等）
 * 用于“正在调用某个工具”这类即时提示
 */
export function peekStep(raw, activeStepNo = null) {
  const original = String(raw ?? '')
  const headerMatch = original.match(STEP_HEADER)
  const stepNo = headerMatch
    ? Number(headerMatch[1] || headerMatch[2]) || activeStepNo
    : activeStepNo
  const toolMatch = original.match(TOOL_RESULT)
  const inner = stripQuotes(original)
  const resultText = toolMatch ? stripQuotes(toolMatch[2]) : ''

  return {
    stepNo,
    toolName: toolMatch ? toolMatch[1] : '',
    resultLength: resultText.length,
    /** 结果看起来是结构化数据（JSON/搜索结果） */
    structured: JSON_START.test(resultText),
    toolFinished: Boolean(toolMatch),
  }
}
