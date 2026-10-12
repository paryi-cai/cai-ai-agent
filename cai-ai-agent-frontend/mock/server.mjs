/**
 * 本地联调用的假后端（仅用于开发自测，不参与构建）
 *
 * 模拟 Spring Boot 的两个 SSE 接口：
 *   - GET /api/ai/food_app/chat/sse?message=xxx&chatId=xxx[&region=南山]   菜狗探店（本地知识库问答）
 *   - GET /api/ai/manus/chat?message=xxx                                   超级智能体（分步推送）
 * 其中 manus 用真实抓包数据回放：每次 SSE 响应 = 一个步骤（Step N: ...，带 BOM），流结束无 [DONE]。
 * 数据来源：mock/realEvents.json（从真实后端 /api/ai/manus/chat 抓取）
 *
 * 运行：node mock/server.mjs   （默认 8123 端口，可用 PORT 覆盖）
 * 参数：DELAY=800 控制 manus 每个步骤之间的间隔毫秒数
 */
import { readFileSync } from 'node:fs'
import http from 'node:http'

const PORT = Number(process.env.PORT || 8123)
const STEP_DELAY = Number(process.env.DELAY || 800)

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

/** 把长文本切成小块，模拟 token 流 */
function chunk(text, size = 4) {
  const parts = []
  for (let i = 0; i < text.length; i += size) parts.push(text.slice(i, i + size))
  return parts
}

/** 真实抓包的智能体步骤：一个元素 = 一次 SSE 响应 = 一个步骤 */
const MANUS_STEPS = JSON.parse(
  readFileSync(new URL('./realEvents.json', import.meta.url), 'utf8'),
).map((event) => `\uFEFF${event}`)

/** 追加一段最终答复（真实后端最后一步也会带 Step 前缀），方便验证 Markdown 气泡 */
const FINAL_ANSWER = `\uFEFFStep 9: 已为你生成《上海浪漫约会计划》：

## 一、住宿建议
- **静安寺 / 上海火车站商圈**：交通便利，地铁 1/3/4 号线步行可达

## 二、白天行程
1. **甜爱路**（虹口区）—— 上海最浪漫的马路，路口有爱心邮筒
2. **静安寺 & 静安公园** —— 城市中心的千年古刹

## 三、夜晚安排
- 思南公馆：夏夜最浪漫的漫步地
- 静安大悦城：150+ 品牌的夜间经济地标

> 文件已生成：\`shanghai_romantic_date_plan.txt\``

/** 菜狗探店：按区域给出"像知识库"的回复，方便验证区域过滤参数是否生效 */
const REGION_REPLY = {
  南山:
    '海岸城和万象天地适合逛街顺路觅食，蛇口海上世界傍晚的海景餐厅氛围最好。\n\n' +
    '科技园上班族午餐推荐错峰（11:30 前或 13:15 后）。',
  福田:
    '福田早茶去 CBD 老牌茶楼，车公庙和华强北适合工作日快餐，晚上可以逛福田夜市。',
  罗湖:
    '东门老街老字号密集，乐园路海鲜街的经典玩法是"市场买 + 加工店做"。',
  宝安:
    '宝安中心区（壹方城）适合逛吃，西乡和固戍藏着不少平价小店，宵夜去大排档。',
  通用:
    '先明确三件事：在哪个区、想吃什么菜系、人均预算多少，我再给你更精准的推荐。',
}

/** 菜狗探店：未指定区域时的通用回复 */
const FOOD_REPLY =
  '想吃点啥？告诉我你在深圳哪个区、想吃什么菜系、什么场景（约会/聚餐/一人食），我来帮你挑店。'

async function stream(res, parts, { delay = 45 } = {}) {
  res.writeHead(200, {
    'Content-Type': 'text/event-stream; charset=utf-8',
    'Cache-Control': 'no-cache, no-transform',
    Connection: 'keep-alive',
    'Access-Control-Allow-Origin': '*',
  })

  for (const part of parts) {
    // 一个 part = 一次 SSE 响应（data 内部换行需要拆成多行 data:）
    const payload = String(part)
      .split('\n')
      .map((line) => `data:${line}`)
      .join('\n')
    res.write(`${payload}\n\n`)
    await sleep(delay)
  }
  res.write('data:[DONE]\n\n')
  res.end()
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`)
  const message = url.searchParams.get('message') || ''
  const chatId = url.searchParams.get('chatId') || '(无)'
  const region = url.searchParams.get('region') || '(全库)'

  console.log(
    `[mock] ${req.method} ${url.pathname} message=${JSON.stringify(message)} chatId=${chatId} region=${region}`,
  )

  // 菜狗探店（SSE 流式）
  if (url.pathname === '/api/ai/food_app/chat/sse') {
    const body = REGION_REPLY[region] || FOOD_REPLY
    const reply = `收到：**${message}**\n检索范围：${region}\n\n${body}`
    return stream(res, chunk(reply, 4), { delay: 45 })
  }

  // 超级智能体（分步推送）
  if (url.pathname === '/api/ai/manus/chat') {
    return stream(res, [...MANUS_STEPS, FINAL_ANSWER], { delay: STEP_DELAY })
  }

  res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' })
  res.end('not found')
})

server.listen(PORT, () => {
  console.log(`[mock] 假后端已启动：http://localhost:${PORT}/api`)
  console.log(`[mock] 菜狗探店：/api/ai/food_app/chat/sse（支持 region=南山/福田/罗湖/宝安）`)
  console.log(
    `[mock] 智能体：/api/ai/manus/chat 回放 ${MANUS_STEPS.length} 个真实步骤 + 1 段最终答复，每步间隔 ${STEP_DELAY}ms`,
  )
})
