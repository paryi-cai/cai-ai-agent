/**
 * 生产构建的本地校验服务器（仅自测用，不参与构建）
 * 静态托管 dist/，并把 /api 反向代理到后端，方便验证打包产物。
 *
 * 运行：node mock/static-server.mjs   （默认 4173，后端默认 8124）
 */
import http from 'node:http'
import { readFile } from 'node:fs/promises'
import { extname, join, normalize } from 'node:path'
import { fileURLToPath } from 'node:url'

const PORT = Number(process.env.PORT || 4173)
const BACKEND = process.env.BACKEND || 'http://localhost:8124'
const DIST = fileURLToPath(new URL('../dist/', import.meta.url))

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`)

  // 代理接口（含 SSE）
  if (url.pathname.startsWith('/api')) {
    const target = new URL(url.pathname + url.search, BACKEND)
    const proxyReq = http.request(
      target,
      { method: req.method, headers: { ...req.headers, host: target.host } },
      (proxyRes) => {
        res.writeHead(proxyRes.statusCode ?? 502, proxyRes.headers)
        proxyRes.pipe(res)
      },
    )
    proxyReq.on('error', (error) => {
      res.writeHead(502, { 'Content-Type': 'text/plain; charset=utf-8' })
      res.end(`proxy error: ${error.message}`)
    })
    req.pipe(proxyReq)
    return
  }

  // 静态资源
  const filePath = join(DIST, normalize(url.pathname === '/' ? '/index.html' : url.pathname))
  try {
    const content = await readFile(filePath)
    res.writeHead(200, { 'Content-Type': MIME[extname(filePath)] || 'application/octet-stream' })
    res.end(content)
  } catch {
    const fallback = await readFile(join(DIST, 'index.html'))
    res.writeHead(200, { 'Content-Type': MIME['.html'] })
    res.end(fallback)
  }
})

server.listen(PORT, () => {
  console.log(`[static] dist 已托管：http://127.0.0.1:${PORT}/  （/api -> ${BACKEND}）`)
})
