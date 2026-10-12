<script setup>
/**
 * 主页：用于切换不同的应用
 * 响应式：手机单列 → 平板双列 → 桌面双列居中（最大宽 1080px）
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppHeader from '@/components/AppHeader.vue'
import { API_BASE_URL, API_PATH } from '@/api/config'
import http from '@/api/http'

const router = useRouter()

const apps = [
  {
    name: '菜狗探店',
    emoji: '🍜',
    desc: '深圳美食探店博主，推荐各区好店与觅食路线，支持多轮会话上下文。',
    points: ['SSE 流式输出', '实时打字机效果', 'chatId 区分会话'],
    route: '/food',
    gradient: 'linear-gradient(135deg, #f97316, #f43f5e 55%, #facc15)',
  },
  {
    name: 'AI 超级智能体',
    emoji: '🧠',
    desc: '自主规划并分步执行任务，每个步骤实时推送、单独展示。',
    points: ['每一步一个气泡', '工具结果结构化', '可折叠原始数据'],
    route: '/manus',
    gradient: 'linear-gradient(135deg, #4f6ef7, #22d3ee 55%, #14b8a6)',
  },
]

/** 后端连通状态：online / offline / checking */
const backendStatus = ref('checking')
const backendMessage = ref('正在检测后端服务…')

/** 两个应用共用的能力说明（桌面/平板上补齐版面，移动端自动分栏） */
const features = [
  {
    icon: '⚡',
    title: 'SSE 流式输出',
    text: 'EventSource 长连接逐段推送，回复实时呈现，不用等整段生成。',
  },
  {
    icon: '⌨️',
    title: '打字机效果',
    text: '逐字显示，分片推送与整段返回都有平滑的输入观感。',
  },
  {
    icon: '🧭',
    title: '分步可视化',
    text: '智能体每一步独立成气泡，工具结果结构化，可折叠原始数据。',
  },
  {
    icon: '📱',
    title: '自适应布局',
    text: '手机、平板、桌面统一风格：输入区常驻底部，内容区独立滚动。',
  },
]

/** 用 axios 做一次探测，仅作为开发提示 */
async function checkBackend() {
  backendStatus.value = 'checking'
  backendMessage.value = '正在检测后端服务…'
  try {
    // 任意 HTTP 响应都说明服务已启动（404/405 也算连通），只看有没有网络层错误
    await http.get(API_PATH.FOOD_APP_CHAT_SSE, { timeout: 4000, validateStatus: () => true })
    backendStatus.value = 'online'
    backendMessage.value = `后端服务在线 · ${API_BASE_URL}`
  } catch {
    backendStatus.value = 'offline'
    backendMessage.value = `未检测到后端服务（${API_BASE_URL}）`
  }
}

function openApp(route) {
  router.push(route)
}

onMounted(checkBackend)
</script>

<template>
  <div class="page home">
    <div class="glow glow-a" aria-hidden="true"></div>
    <div class="glow glow-b" aria-hidden="true"></div>

    <div class="page-inner">
      <AppHeader emoji="🤖" title="AI 应用中心" subtitle="流式对话 · 分步执行" brand>
        <template #actions>
          <span class="status chip" :class="backendStatus" :title="backendMessage">
            <span class="dot"></span>
            <span class="status-text">{{ backendMessage }}</span>
            <button class="status-retry" title="重新检测后端" @click="checkBackend">检测</button>
          </span>
        </template>
      </AppHeader>

      <section class="hero">
        <h2>选择一个应用开始对话</h2>
        <p>所有回复均为流式实时输出：菜狗探店逐字回复，超级智能体分步执行、一步一个气泡。</p>
      </section>

      <main class="cards">
        <article
          v-for="app in apps"
          :key="app.route"
          class="card surface surface-lg"
          tabindex="0"
          role="link"
          :aria-label="`进入 ${app.name}`"
          @click="openApp(app.route)"
          @keyup.enter="openApp(app.route)"
          @keyup.space.prevent="openApp(app.route)"
        >
          <div class="card-cover" :style="{ background: app.gradient }">
            <span>{{ app.emoji }}</span>
          </div>
          <div class="card-body">
            <h3>{{ app.name }}</h3>
            <p>{{ app.desc }}</p>
            <ul class="points">
              <li v-for="point in app.points" :key="point">{{ point }}</li>
            </ul>
            <div class="enter">进入应用 <i aria-hidden="true">→</i></div>
          </div>
        </article>
      </main>

      <section class="features">
        <h3 class="features-title">两个应用共用的能力</h3>
        <ul class="feature-list">
          <li v-for="feature in features" :key="feature.title" class="feature surface">
            <span class="feature-icon">{{ feature.icon }}</span>
            <div>
              <h4>{{ feature.title }}</h4>
              <p>{{ feature.text }}</p>
            </div>
          </li>
        </ul>
      </section>

      <footer class="foot">
        接口前缀 <code>{{ API_BASE_URL }}</code> · 开发环境由 vite 代理到 <code>http://localhost:8123</code>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.home {
  overflow-x: hidden;
}

/* ---------------- 背景光斑 ---------------- */
.glow {
  position: fixed;
  width: min(520px, 70vw);
  height: min(520px, 70vw);
  border-radius: 50%;
  filter: blur(110px);
  opacity: 0.5;
  pointer-events: none;
  z-index: var(--z-glow);
}
.glow-a {
  top: -220px;
  left: -160px;
  background: var(--glow-a);
}
.glow-b {
  bottom: -240px;
  right: -140px;
  background: var(--glow-b);
}

/* ---------------- 英雄区 ---------------- */
.hero {
  margin: clamp(20px, 4vw, 36px) auto clamp(18px, 3.4vw, 28px);
  max-width: 660px;
  text-align: center;
}
.hero h2 {
  margin: 0 0 10px;
  font-size: clamp(22px, 3.4vw, 32px);
  line-height: 1.25;
  letter-spacing: -0.4px;
  background: linear-gradient(120deg, #4f6ef7, #8b5cf6 50%, #ec4899);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.hero p {
  margin: 0;
  color: var(--text-2);
  font-size: clamp(13px, 1.5vw, 14.5px);
}

/* ---------------- 后端状态 ---------------- */
.status {
  max-width: 320px;
  overflow: hidden;
}
.status.offline {
  background: var(--danger-soft);
  color: #b4232b;
}
.status.online {
  background: var(--success-soft);
  color: #15803d;
}
.status .dot {
  width: 8px;
  height: 8px;
  flex: none;
  border-radius: 50%;
  background: var(--text-3);
}
.status.online .dot {
  background: #22c55e;
  box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.16);
}
.status.offline .dot {
  background: var(--danger);
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.16);
}
.status.checking .dot {
  background: var(--warning);
  animation: pulse 1s infinite;
}
.status-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.status-retry {
  flex: none;
  padding: 4px 10px;
  border: 1px solid currentColor;
  border-radius: var(--r-full);
  background: transparent;
  color: inherit;
  font-size: 11.5px;
  line-height: 1.4;
  opacity: 0.75;
}
.status-retry:hover {
  opacity: 1;
}
@media (pointer: coarse) {
  .status-retry {
    min-height: 32px;
  }
}

@keyframes pulse {
  50% {
    opacity: 0.3;
  }
}

/* ---------------- 应用卡片 ---------------- */
.cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 300px), 1fr));
  gap: clamp(14px, 2.2vw, 22px);
}

.card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  cursor: pointer;
  outline: none;
  transition: transform var(--t-base), box-shadow var(--t-base), border-color var(--t-base);
}
.card:hover,
.card:focus-visible {
  transform: translateY(-6px);
  border-color: rgba(79, 110, 247, 0.4);
  box-shadow: var(--shadow-lg);
}

.card-cover {
  height: clamp(96px, 16vw, 128px);
  display: grid;
  place-items: center;
  font-size: clamp(38px, 7vw, 52px);
}
.card-cover span {
  filter: drop-shadow(0 6px 14px rgba(0, 0, 0, 0.22));
}

.card-body {
  padding: clamp(14px, 2vw, 20px);
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}
.card-body h3 {
  margin: 0;
  font-size: clamp(16px, 2vw, 19px);
}
.card-body p {
  margin: 0;
  color: var(--text-2);
  font-size: 13.5px;
}

.points {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  list-style: none;
  margin: 12px 0 14px;
  padding: 0;
}
.points li {
  padding: 3px 10px;
  border-radius: var(--r-full);
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 12px;
}

.enter {
  margin-top: auto;
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 13.5px;
  color: var(--brand);
}
.enter i {
  font-style: normal;
  transition: transform var(--t-base);
}
.card:hover .enter i,
.card:focus-visible .enter i {
  transform: translateX(4px);
}

/* ---------------- 能力说明 ---------------- */
.features {
  margin-top: clamp(28px, 4.6vw, 48px);
}
.features-title {
  margin: 0 0 clamp(12px, 2vw, 18px);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  color: var(--text-3);
  text-align: center;
}
.feature-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 220px), 1fr));
  gap: clamp(12px, 1.8vw, 18px);
}
.feature {
  display: flex;
  gap: 12px;
  padding: 16px;
  transition: border-color var(--t-fast), box-shadow var(--t-fast), transform var(--t-fast);
}
.feature:hover {
  transform: translateY(-2px);
  border-color: var(--border-strong);
  box-shadow: var(--shadow-sm);
}
.feature-icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  flex: none;
  border-radius: var(--r-md);
  background: var(--brand-soft);
  font-size: 19px;
  line-height: 1;
}
.feature h4 {
  margin: 0 0 2px;
  font-size: 14px;
}
.feature p {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--text-2);
}

/* ---------------- 页脚 ---------------- */
.foot {
  margin: clamp(24px, 4vw, 40px) auto 0;
  text-align: center;
  font-size: 12px;
  color: var(--text-3);
  line-height: 2;
}
.foot code {
  padding: 1px 6px;
  border-radius: 6px;
  background: var(--panel);
  border: 1px solid var(--border);
  font-family: var(--font-mono);
  font-size: 11.5px;
  white-space: nowrap;
}

/* ---------------- 响应式细节 ---------------- */
@media (max-width: 560px) {
  .glow {
    filter: blur(80px);
    opacity: 0.42;
  }
  .status {
    max-width: 200px;
  }
  .status-text {
    font-size: 11.5px;
  }
  .card-cover {
    height: 84px;
  }
}

/* 手机竖屏：状态条独占一行，避免把头部挤变形 */
@media (max-width: 420px) {
  .status {
    max-width: 100%;
    width: 100%;
    justify-content: flex-start;
  }
  /* 竖屏窄屏：图标在左的横排会太挤，改成上下结构 */
  .feature {
    flex-direction: column;
    gap: 8px;
    padding: 14px;
  }
}
</style>
