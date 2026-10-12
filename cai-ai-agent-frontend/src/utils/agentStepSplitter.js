/**
 * 步骤起始标记：Step 1: / Step 2： / [Step 3] / 第 4 步
 *
 * 前缀允许三种情况：
 * 1) 文本开头；
 * 2) 换行之后（后端用 \n 分隔多个步骤）；
 * 3) 紧跟在一个引号 / 右括号之后——智能体工具结果常以 `结果: "..."` 结尾，
 *    下一个步骤紧贴着拼接过来（`..."Step 2: ...`、`...）Step 2: ...`）时也能识别。
 */
export const STEP_START = /(?:^|(?<=[\n"'”’」）)\]]))\s*(?:\[?Step\s*\d+\]?|第\s*\d+\s*步)\s*[:：.、]?\s*/

/** 只用来抓步骤号，不消费正文 */
const STEP_NUMBER = /(?:\[?Step\s*(\d+)\]?|第\s*(\d+)\s*步)\s*[:：.、]?/gi

/**
 * 缓冲结尾是否正在写一个新的步骤头（可能还没打出冒号，例如 "Step 9"）
 * 这种情况下也要开新气泡，否则最后一段内容会被并进上一步。
 */
const TRAILING_STEP_HEADER = /(?:^|[\n"'”’」）)\]])\s*\[?(?:Step\s*\d*|第\s*\d*\s*步?)\s*[:：.、]?$/i

/**
 * 按“智能体步骤”切分 SSE 响应
 *
 * 超级智能体每推一步就是一条“Step N: ...”响应，用它把流式文本拆成一个个独立步骤，
 * 前端就可以一步一个气泡地展示。
 *
 * 说明：
 * - 只有当缓冲区开头就是步骤标记时才启用切分（正文里夹带 "Step 1" 字样不会被误切）；
 * - 每次 push 都基于完整缓冲重新切分，因此对“一个响应一个步骤”“一个响应多个步骤”
 *   以及“一个步骤被拆成多个响应”三种情况都成立；
 * - 返回的每一项都带上步骤号，UI 直接用它做“第 N 步”徽标。
 */
export function createStepSplitter() {
  let buffer = ''

  const clear = () => {
    buffer = ''
  }

  /**
   * 清理不可见前缀：
   * - BOM（后端有时会带上 \uFEFF，会让 "Step 1:" 匹配不上）
   * - 事件开头多余的换行
   */
  const sanitize = (text) => text.replace(/^[\uFEFF\u200B\s]+/, '')

  /** 第 target 个步骤标记的位置（跳过空匹配），用于判断缓冲结尾是否是新步骤 */
  const nthStepIndex = (text, target) => {
    const matcher = new RegExp(STEP_START.source, 'g')
    let matched
    let count = 0
    while ((matched = matcher.exec(text)) !== null) {
      if (matched[0].length === 0) {
        matcher.lastIndex += 1
        continue
      }
      if (++count === target) return matched.index
    }
    return -1
  }

  /** 取出所有步骤号（与 split 出来的片段一一对应） */
  const collectStepNumbers = (text) => {
    const numbers = []
    const matcher = new RegExp(STEP_NUMBER.source, 'gi')
    let matched
    while ((matched = matcher.exec(text)) !== null) {
      numbers.push(Number(matched[1] ?? matched[2]) || null)
    }
    return numbers
  }

  /**
   * 把整段缓冲切成步骤片段
   *
   * @param {string} text
   * @returns {{ parts: Array<{ stepNo: number|null, text: string }>, newStep: boolean }}
   */
  const splitParts = (text) => {
    const numbers = collectStepNumbers(text)

    // split 在“开头就是标记”时会多出一个空串，去掉它保证下标与步骤号对齐
    const texts = text.split(new RegExp(STEP_START.source))
    if (texts[0] === '') texts.shift()

    const parts = texts.map((partText, index) => ({
      stepNo: numbers[index] ?? null,
      text: partText,
    }))
    if (!parts.length) {
      parts.push({ stepNo: numbers[0] ?? null, text: '' })
    }

    const suffix = text.slice(nthStepIndex(text, parts.length))
    const newStep = new RegExp(`^${STEP_START.source}`).test(suffix)

    // 正在写步骤头但还没写完（例如只剩 "Step 9"）：
    // 先把这一步作为空片段拆出来，UI 立刻开新气泡，避免最后一段被并进上一个步骤。
    if (!newStep && TRAILING_STEP_HEADER.test(text)) {
      parts.push({ stepNo: null, text: '' })
    }

    return { parts, newStep }
  }

  /**
   * 追加一段流式文本
   * @param {string} chunk
   * @returns {{
   *   parts: Array<{ stepNo: number|null, text: string }>,
   *   buffer: string, newStep: boolean, unchanged: boolean
   * }}
   *   parts：拆分后的步骤（最后一段可能还不完整）；没有步骤标记时只有一段、stepNo 为 null
   */
  const push = (chunk) => {
    const prev = buffer
    buffer = sanitize(buffer + (chunk ?? ''))

    const startsWithStep = new RegExp(`^${STEP_START.source}`).test(buffer)
    if (!startsWithStep) {
      return {
        parts: [{ stepNo: null, text: buffer }],
        buffer,
        newStep: false,
        unchanged: buffer === prev,
      }
    }

    const { parts, newStep } = splitParts(buffer)

    return {
      parts,
      buffer,
      // 缓冲末尾就是一个新步骤的开头 -> 上一步已完成，可以生成新气泡
      newStep,
      unchanged: buffer === prev,
    }
  }

  /**
   * 流结束：强制切开最后一个步骤
   *
   * 后端每一步是一次响应，但两个响应的文本可能紧挨着（没有换行/空格），
   * 只有在下一条响应到来时才能确定上一步已结束。
   * 所以流结束时调用它，把缓冲里最后一段取出来作为最后一步。
   * @returns {{ stepNo: number|null, text: string }}
   */
  const cut = () => {
    const text = buffer
    buffer = ''

    if (!new RegExp(`^${STEP_START.source}`).test(text)) return { stepNo: null, text }

    const { parts } = splitParts(text)
    const last = parts[parts.length - 1]

    return { stepNo: last?.stepNo ?? null, text: last?.text ?? '' }
  }

  return { push, cut, clear, size: () => buffer.length }
}
