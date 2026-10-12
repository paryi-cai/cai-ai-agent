/**
 * 端到端 UI 自测（可选依赖：playwright，需自行安装）
 *
 * 覆盖：
 * 1) 智能体页面：每个 SSE 响应（一个步骤）是否独立成一个气泡；
 * 2) 工具结果是否结构化渲染（搜索结果条目、文件路径、长文本 Markdown）；
 * 3) 折叠 / 展开 / 精简模式；
 * 4) 恋爱大师页面普通流式对话的回归验证。
 *
 * 安装与运行：
 *   pnpm add -D playwright && npx playwright install chromium
 *   pnpm build
 *   PORT=4173 BACKEND=http://localhost:8123 node mock/static-server.mjs
 *   BASE_URL=http://127.0.0.1:4173 node scripts/e2e-check.mjs
 *
 * 环境变量：BASE_URL / OUT_DIR / TASK / TIMEOUT / PW_CHANNEL（默认用系统 Chrome）
 */
import { chromium } from 'playwright'
import fs from 'node:fs'

const BASE = process.env.BASE_URL || 'http://127.0.0.1:4173'
const OUT = process.env.OUT_DIR || 'screenshots'
const TASK = process.env.TASK || '帮我制定一个上海的浪漫约会计划'
const TIMEOUT = Number(process.env.TIMEOUT || 420000)
/** 聊天输入区按钮的稳定选择器（样式重构后不再依赖具体类名） */
const SEND = '[data-action="chat-send"]'
const STOP = '[data-action="chat-stop"]'
/**
 * 组件挂载完成标记。
 * 路由切换带过渡动画，必须等它出现后再操作输入框，
 * 否则写入的内容会被组件首次渲染覆盖。
 */
const READY = '.chat-page[data-ready="true"]'

/** 等待页面就绪并发送一条消息 */
async function openAndSend(hash, text) {
  await page.goto(`${BASE}/${hash}`, { waitUntil: 'domcontentloaded' })
  await page.waitForSelector(READY, { timeout: 20000 })
  await page.waitForTimeout(200)
  await page.fill('textarea', text)
  await page.waitForSelector(`${SEND}:not([disabled])`, { timeout: 20000 })
  await page.click(`${SEND}:not([disabled])`)
}

fs.mkdirSync(OUT, { recursive: true })

const browser = await chromium.launch({ channel: process.env.PW_CHANNEL || 'chrome' })
const page = await browser.newPage({ viewport: { width: 1280, height: 900 } })

const errors = []
page.on('console', (msg) => {
  if (msg.type() === 'error') errors.push(`[console.error] ${msg.text()}`)
})
page.on('pageerror', (err) => errors.push(`[pageerror] ${err.message}`))

const shot = async (name) => {
  await page.screenshot({ path: `${OUT}/${name}.png` })
  console.log(`  screenshot -> ${OUT}/${name}.png`)
}

// ---------- 超级智能体 ----------
console.log('=== 智能体页面：发送任务 ===')
await openAndSend('#/manus', TASK)

const seen = new Set()
const startedAt = Date.now()
let midShot = false

while (Date.now() - startedAt < TIMEOUT) {
  const snapshot = await page.evaluate(() =>
    Array.from(document.querySelectorAll('.step-card')).map((card) => ({
      badge: card.querySelector('.step-badge')?.innerText ?? '',
      tool: card.querySelector('.step-tool code')?.innerText ?? '',
      digest: (card.querySelector('.step-digest')?.innerText ?? '').slice(0, 40),
      results: card.querySelectorAll('.result-item').length,
    })),
  )

  snapshot.forEach((card, index) => {
    const key = `${index}|${card.badge}|${card.tool}|${card.results}`
    if (seen.has(key)) return
    seen.add(key)
    console.log(
      `  +${((Date.now() - startedAt) / 1000).toFixed(1)}s 第${index + 1}个气泡 → ${card.badge} ${card.tool} | 摘要="${card.digest}" | 结果条目=${card.results}`,
    )
  })

  if (!midShot && snapshot.length >= 2) {
    midShot = true
    await shot('21-manus-streaming')
  }

  if ((await page.locator(SEND).count()) > 0) break
  await page.waitForTimeout(1500)
}

await page.waitForTimeout(800)
console.log('\n=== 完成状态 ===')
console.log('步骤气泡数:', await page.locator('.step-card').count())
console.log('徽标:', (await page.locator('.step-badge').allInnerTexts()).join(' / '))
console.log('工具:', (await page.locator('.step-tool code').allInnerTexts()).join(' / '))
console.log('最终答复气泡数:', await page.locator('.bubble-ai').count())
console.log('结果条目总数:', await page.locator('.result-item').count())
console.log('折叠按钮:', (await page.locator('.more-btn').allInnerTexts()).join(' | ') || '(无)')
console.log('文件路径:', (await page.locator('.file-line code').allInnerTexts()).join(' | ') || '(无)')
await shot('22-manus-done')

const jsonBtn = page.locator('.mini-btn', { hasText: '查看原始数据' }).first()
if (await jsonBtn.count()) {
  await jsonBtn.click()
  await page.waitForTimeout(400)
  console.log('展开后 JSON 块数量:', await page.locator('.json-block').count())
  await shot('23-manus-json')
}

const moreBtn = page.locator('.more-btn', { hasText: '展开其余' }).first()
if (await moreBtn.count()) {
  const before = await page.locator('.result-item').count()
  await moreBtn.click()
  await page.waitForTimeout(400)
  console.log(`展开其余结果：${before} -> ${await page.locator('.result-item').count()} 条`)
  await shot('24-manus-results-all')
}

await page.locator('.switch').click()
await page.waitForTimeout(400)
console.log(
  '精简模式：结果条目 =',
  await page.locator('.result-item').count(),
  '| 徽标仍在 =',
  (await page.locator('.step-badge').count()) > 0,
)
await shot('25-manus-compact')
await page.locator('.switch').click()
await page.waitForTimeout(300)

await page.evaluate(() => {
  const el = document.querySelector('.chat-list')
  if (el) el.scrollTop = 0
})
await page.waitForTimeout(300)
await shot('26-manus-top')

// ---------- 恋爱大师回归 ----------
console.log('\n=== 恋爱大师页面（普通模式回归）===')
await openAndSend('#/love', '你好，我喜欢一个同事，但不敢表白')
await page.waitForTimeout(1000)
await shot('27-love-streaming')
await page.waitForSelector(SEND, { timeout: 60000 })
await page.waitForTimeout(500)
console.log('AI 气泡数（普通模式应为 1）:', await page.locator('.bubble-ai').count())
console.log('内容长度:', (await page.locator('.bubble-ai').last().innerText()).length)
console.log('是否误用步骤卡片:', (await page.locator('.step-card').count()) === 0 ? '否 ✓' : '是 ✗')
await shot('28-love-done')

console.log('\n=== 页面错误 ===')
console.log(errors.length ? errors.join('\n') : '无')

await browser.close()
