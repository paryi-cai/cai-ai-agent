/** 端到端验证：用真实抓包数据（mock/realEvents.json）跑步骤解析 */
import { readFileSync } from 'node:fs'
import { parseStep } from '../src/utils/agentStep.js'
import { createStepSplitter } from '../src/utils/agentStepSplitter.js'

/** 真实后端每个事件都以 BOM 开头 */
const raw = JSON.parse(readFileSync(new URL('../mock/realEvents.json', import.meta.url), 'utf8'))
const events = raw.map((event) => `\uFEFF${event}`)

console.log('=== 真实事件逐个解析（模拟前端行为）===')
const splitter = createStepSplitter()
let bubbles = []

events.forEach((event, index) => {
  const { parts } = splitter.push(event)
  const lastIndex = parts.length - 1

  if (parts.length > bubbles.length && bubbles.length > 0) {
    bubbles[bubbles.length - 1].done = true
  }
  while (bubbles.length < parts.length) {
    const part = parts[bubbles.length]
    bubbles.push({ stepNo: part.stepNo, text: part.text, done: false })
  }
  bubbles[lastIndex].text = parts[lastIndex].text

  console.log(
    `事件${index + 1}: 收到「${event.replace(/^\uFEFF/, '').slice(0, 26)}…」→ 当前气泡数=${parts.length}，进行中的步骤号=${parts[lastIndex].stepNo}`,
  )
})

// 流结束：跟前端一样调用 cut() 把最后一段独立出来
const tail = splitter.cut()
if (tail.text) {
  const last = bubbles[bubbles.length - 1]
  if (last && last.text === tail.text) {
    last.done = true
    console.log(`\n流结束 cut() → 与当前气泡一致（第 ${last.stepNo} 步），直接落定`)
  } else {
    if (last) last.done = true
    bubbles.push({ stepNo: tail.stepNo ?? bubbles.length + 1, text: tail.text, done: true })
    console.log(
      `\n流结束 cut() → 最后一段独立成第 ${bubbles.length} 步：${JSON.stringify(tail.text.slice(0, 40))}`,
    )
  }
}

console.log('\n=== 气泡解析结果（前端渲染依据）===')
let problems = 0
bubbles.forEach((bubble, index) => {
  const parsed = parseStep(bubble.text, bubble.stepNo)
  const ok = parsed.kind === 'tool' && Boolean(parsed.toolName)
  if (!ok) problems += 1
  console.log(
    `  气泡${index + 1} | 第${parsed.stepNo}步 | ${parsed.toolName} | 结构化条目=${parsed.entries.length} | 摘要=${JSON.stringify(parsed.digest.slice(0, 30))} | ${ok ? '✓' : '✗'}`,
  )
})

console.log('\n=== 搜索结果条目（第 2 个气泡）===')
const searchBubble = parseStep(bubbles[1].text, bubbles[1].stepNo)
console.log('条数:', searchBubble.entries.length, '| 默认展示:', searchBubble.visibleEntries.length, '| 折叠:', searchBubble.extraCount)
searchBubble.entries.forEach((entry) => {
  console.log(`  #${entry.position} [${entry.domain}] ${entry.title.slice(0, 34)} | ${entry.date || '无日期'}`)
})

console.log('\n=== 第 3、4 个气泡是否为结构化搜索结果 ===')
;[2, 3].forEach((index) => {
  const parsed = parseStep(bubbles[index].text, bubbles[index].stepNo)
  console.log(`  气泡${index + 1}: 条目=${parsed.entries.length} 首条=${parsed.entries[0]?.title?.slice(0, 30) ?? '(无)'}`)
})

console.log('\n=== 失败结果 / 文件结果 / 终止结果 ===')
;[4, 5, 6].forEach((index) => {
  const parsed = parseStep(bubbles[index].text, bubbles[index].stepNo)
  console.log(
    `  气泡${index + 1}: tool=${parsed.toolName} | 文件=${parsed.filePath || '-'} | 纯文本=${parsed.isText} | 正文=${JSON.stringify(parsed.text.slice(0, 46))}`,
  )
})

const readFileBubble = parseStep(bubbles[5].text, bubbles[5].stepNo)
console.log('\nreadFile 结果换行是否还原:', readFileBubble.text.includes('\n'))
console.log('readFile 结果前 80 字:', JSON.stringify(readFileBubble.text.slice(0, 80)))

console.log(`\n结论：${problems === 0 ? '全部步骤都被正确识别为工具步骤 ✓' : `有 ${problems} 个步骤未识别 ✗`}`)
