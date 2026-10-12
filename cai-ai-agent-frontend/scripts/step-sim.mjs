/**
 * 纯逻辑自测：步骤切分 + 步骤解析（模拟前端在流式过程中的行为）
 * 运行：node scripts/step-sim.mjs
 */
import { readFileSync } from 'node:fs'
import { createStepSplitter } from '../src/utils/agentStepSplitter.js'
import { parseStep } from '../src/utils/agentStep.js'

/** 真实后端抓包的步骤事件（每个事件都以 BOM 开头） */
const REAL_MANUS_EVENTS = JSON.parse(
  readFileSync(new URL('../mock/realEvents.json', import.meta.url), 'utf8'),
).map((event) => `\uFEFF${event}`)

/** 最终答复示例（真实后端也会带 Step 前缀） */
const REAL_FINAL_EVENT = `\uFEFFStep 9: 已为你生成《上海浪漫约会计划》：

## 一、住宿建议
- **静安寺 / 上海火车站商圈**：交通便利，地铁 1/3/4 号线步行可达

> 文件已生成：\`shanghai_romantic_date_plan.txt\``

/** 恋爱大师（普通对话，逐段推送，不应触发步骤切分） */
const REAL_LOVE_CHUNKS = [
  '收到你的消息：',
  '**你好，我喜欢一个同事**',
  '\n\n别着急，',
  '感情里的事我们慢慢聊：\n\n',
  '1. 先说说你们相处多久了？\n',
  '2. 最近一次让你不舒服的具体场景是什么？\n\n',
  '> 记住：先照顾好自己的感受。',
]

console.log(`=== 真实后端采样：${REAL_MANUS_EVENTS.length} 个 SSE 事件（每个事件一个步骤，带 BOM）====`)
const realSplitter = createStepSplitter()
const bubbles = []

REAL_MANUS_EVENTS.forEach((event, index) => {
  const { parts } = realSplitter.push(event)
  const lastIndex = parts.length - 1
  if (parts.length > bubbles.length && bubbles.length > 0) {
    bubbles[bubbles.length - 1].final = true
  }
  while (bubbles.length < parts.length) {
    const part = parts[bubbles.length]
    bubbles.push({ stepNo: part.stepNo, text: part.text, final: false })
  }
  bubbles[lastIndex].text = parts[lastIndex].text
  console.log(`事件${index + 1} -> 步骤数=${parts.length} | 当前步骤号=${parts[lastIndex].stepNo}`)
})

// 追加最终答复
realSplitter.push(REAL_FINAL_EVENT)
console.log('（最终答复单独推一次，下面单独解析）\n')

console.log('=== 每个气泡的解析结果 ===')
bubbles.forEach((bubble, index) => {
  const parsed = parseStep(bubble.text, bubble.stepNo)
  console.log(
    `  气泡${index + 1}: 第${parsed.stepNo}步 | kind=${parsed.kind} | tool=${parsed.toolName || '-'} | 条目=${parsed.entries.length} | 文件=${parsed.filePath || '-'} | 摘要="${parsed.digest.slice(0, 34)}"`,
  )
})

console.log('\n=== 搜索结果条目抽样 ===')
const searchBubble = parseStep(bubbles[1].text, bubbles[1].stepNo)
console.log('条数:', searchBubble.entries.length)
searchBubble.entries.slice(0, 2).forEach((entry) => {
  console.log(
    `  #${entry.position} ${entry.title} | ${entry.domain} | ${entry.date || '无日期'} | ${entry.snippet.slice(0, 30)}…`,
  )
})

console.log('\n=== 最终答复 ===')
const finalParsed = parseStep(REAL_FINAL_EVENT)
console.log(
  `kind=${finalParsed.kind} tool=${finalParsed.toolName || '(无)'} 步骤号=${finalParsed.stepNo} 摘要="${finalParsed.digest}"`,
)

console.log('\n=== 恋爱大师（普通分片，不应触发步骤切分）===')
const loveSplitter = createStepSplitter()
REAL_LOVE_CHUNKS.forEach((chunk) => {
  const result = loveSplitter.push(chunk)
  if (result.parts.length !== 1 || result.parts[0].stepNo !== null) {
    console.log('  ✗ 意外切分:', JSON.stringify(result.parts))
  }
})
console.log('  全部保持在同一条消息内 ✓ 累积文本 =', JSON.stringify(loveSplitter.push('').parts[0].text.slice(0, 40)))

// ---------- 边界：一个响应里塞了多个步骤 ----------
const merged = createStepSplitter()
const one = 'Step 1: 工具 a 完成了它的任务！结果: "x"Step 2: 工具 b 完成了它的任务！结果: "y"'
console.log('\n=== 边界：一个响应里塞了多个步骤 ===')
console.log(
  merged.push(one).parts.map((part) => `第${part.stepNo}步: ${part.text}`),
)

// ---------- 边界：一个步骤被拆成多个响应 ----------
const partial = createStepSplitter()
console.log('\n=== 边界：一个步骤被拆成多个响应 ===')
console.log(partial.push('Step 1: 工具 sea').parts)
console.log(partial.push('rchWeb 完成了它的任务！结果: "ab').parts)
console.log(
  partial
    .push('c"')
    .parts.map((part) => `第${part.stepNo}步 | ${part.text} | tool=${parseStep(part.text, part.stepNo).toolName}`),
)
partial.push('Step 2: 工具 x 完成了它的任务！结果: "y"')
console.log('第二个步骤出现后：', partial.push('').parts.map((part) => `第${part.stepNo}步`))

// ---------- 边界：正文里出现 Step 字样 ----------
const plain = createStepSplitter()
console.log('\n=== 边界：普通对话里提到 Step ===')
const plainChunks = ['你好，', '这里提到 Step 1 ', '但不是步骤开头。']
plainChunks.forEach((chunk) => {
  const result = plain.push(chunk)
  console.log(`  parts=${result.parts.length} newStep=${result.newStep}`)
})

// ---------- 边界：步骤头先到、正文后到 ----------
const headerFirst = createStepSplitter()
console.log('\n=== 边界：先收到 "Step 9"（还没有冒号和正文）===')
console.log('第一次:', headerFirst.push('Step 8: 工具 doTerminate 完成了它的任务！结果: "任务结束"').parts.map((p) => `第${p.stepNo}步(${p.text.length})`))
console.log('再来 "Step 9":', headerFirst.push('Step 9').parts.map((p) => `第${p.stepNo}步(${p.text.length})`))
console.log('补上冒号和正文:', headerFirst.push(': 已为你生成计划').parts.map((p) => `第${p.stepNo}步 ${JSON.stringify(p.text)}`))
console.log('cut() ->', JSON.stringify(headerFirst.cut()))

// ---------- 边界：上一步结果与下一个步骤头紧贴（无换行） ----------
const glued = createStepSplitter()
console.log('\n=== 边界：紧贴 + "Step 9" 分两次到达 ===')
const gluedChunks = [
  'Step 8: 工具 doTerminate 完成了它的任务！结果: "任务结束"',
  'Step 9',
  ': 已为你生成计划',
]
gluedChunks.forEach((chunk) => {
  const { parts } = glued.push(chunk)
  console.log(
    `  收到 ${JSON.stringify(chunk.slice(0, 20))} →`,
    parts.map((p) => `第${p.stepNo}步(${p.text.length}字)`).join(' | '),
  )
})
console.log('  cut() ->', JSON.stringify(glued.cut()))

// ---------- 边界：完整步骤 + 带编号的下一段同时到达 ----------
const both = createStepSplitter()
console.log('\n=== 边界：一次到达 "Step 8 结果 + Step 9 正文" ===')
console.log(
  both
    .push('Step 8: 工具 doTerminate 完成了它的任务！结果: "任务结束"Step 9: 已为你生成计划：\n\n- 内容')
    .parts.map((p) => `第${p.stepNo}步 ${JSON.stringify(p.text.slice(0, 24))}`),
)

// ---------- 解析细节 ----------
console.log('\n=== 解析细节（构造用例）===')
const items = Array.from({ length: 5 }, (_, index) => ({
  position: index + 1,
  title: `结果标题 ${index + 1}`,
  link: `https://example${index + 1}.com/page`,
  displayed_link: `example${index + 1}.com`,
  snippet: `这是第 ${index + 1} 条搜索结果的摘要文本，用来验证折叠与域名解析。`,
  date: '2025年1月1日',
}))
const parsedSearch = parseStep(`Step 1: 工具 searchWeb 完成了它的任务！结果: "${JSON.stringify(items)}"`)
console.log('工具名:', parsedSearch.toolName)
console.log('结果条数:', parsedSearch.entries.length, '默认展示:', parsedSearch.visibleEntries.length, '折叠:', parsedSearch.extraCount)
console.log('第一条:', JSON.stringify({
  position: parsedSearch.visibleEntries[0].position,
  title: parsedSearch.visibleEntries[0].title,
  domain: parsedSearch.visibleEntries[0].domain,
}))
console.log('原始 JSON 可展开:', parsedSearch.jsonText.length > 0, '| 中文摘要:', parsedSearch.digest)

const pdfStep = parseStep(
  'Step 8: 工具 generatePDF 完成了它的任务！结果: "PDF generated successfully to: D:\\\\Desktop\\\\cai-ai-agent/tmp/pdf/Shanghai_Romantic_Date_Plan.pdf"',
)
console.log('PDF 步骤:', pdfStep.toolName, '| 路径:', pdfStep.filePath)

const termStep = parseStep('Step 9: 工具 doTerminate 完成了它的任务！结果: "任务结束"')
console.log('终止步骤:', termStep.toolName, '| kind:', termStep.kind, '| 文本:', termStep.text)

