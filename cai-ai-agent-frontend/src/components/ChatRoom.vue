<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

import AgentStepBubble from '@/components/AgentStepBubble.vue'
import AppHeader from '@/components/AppHeader.vue'
import { createSseConnection } from '@/utils/sse'
import { createStepSplitter } from '@/utils/agentStepSplitter'
import { renderMarkdownSafe } from '@/utils/markdown'
import { createTypewriter } from '@/hooks/useTypewriter'
import { parseStep } from '@/utils/agentStep'

const props = defineProps({
  /** 应用名 */
  title: { type: String, required: true },
  /** 副标题 */
  subtitle: { type: String, default: '' },
  /** 顶部图标 / emoji */
  emoji: { type: String, default: '🤖' },
  /** SSE 接口路径，如 /ai/love_app/chat/sse */
  endpoint: { type: String, required: true },
  /**
   * 当前会话的聊天室 id。
   * 恋爱大师需要传 chatId；超级智能体没有 chatId，传空字符串即可。
   */
  chatId: { type: String, default: '' },
  /** 输入框占位提示 */
  placeholder: { type: String, default: '输入消息后按 Enter 发送（Shift + Enter 换行）' },
  /** 底部提示文案 */
  hint: { type: String, default: '' },
  /**
   * 智能体模式：每个 SSE 响应（＝智能体的一个步骤）单独渲染成一个气泡，
   * 并额外提供“展开推理过程 / 精简输出”的切换。
   */
  stepMode: { type: Boolean, default: false },
})

const emit = defineEmits(['reset-chat-id'])

/**
 * 标记组件已挂载完成。
 * 页面切换有过渡动画，自动化脚本/第三方集成可以据此判断“可以交互了”，
 * 避免在挂载前写入输入框导致内容被覆盖。
 */
const ready = ref(false)
onMounted(() => {
  ready.value = true
})

/** 消息列表：[{ id, role: 'user' | 'ai', content, fullText, status, stepNo, kind, parsed }] */
const messages = ref([])
const inputText = ref('')
const streaming = ref(false)
const listRef = ref(null)

/** 精简模式：只保留每个步骤的工具名与一行摘要 */
const compact = ref(false)

/** 当前正在流式输出的连接 */
let connection = null

/** 正在进行中的 AI 消息下标（用 ref，computed 才能正确跟踪） */
const activeIndex = ref(-1)

/** 智能体分步骤推送的切分器：一个步骤 = 一个气泡 */
const splitter = createStepSplitter()

/** 智能体模式下已识别到的步骤数（用于判断是否出现新步骤） */
let lastStepCount = 0

/** 当前（或即将开始的）步骤号，用于底部进度提示 */
const currentStepNo = ref(null)

const activeMessage = computed(() =>
  activeIndex.value >= 0 ? messages.value[activeIndex.value] || null : null,
)

const stepsDone = computed(() => messages.value.filter((item) => item.role === 'ai').length)

/**
 * 打字机池：每个气泡一个实例
 * - 智能体模式下多个步骤气泡会同时存在，必须分开，否则会串内容
 * - 普通模式只有一个气泡
 */
const typewriterPool = new Map()

/** 取（或创建）某个气泡的打字机 */
function typewriterOf(message) {
  if (!message) return null
  let typewriter = typewriterPool.get(message.id)
  if (!typewriter) {
    typewriter = createTypewriter((text) => {
      applyText(message, text)
      // 内容变化时保持贴底（用户手动上滑浏览时不打扰）
      if (isNearBottom()) scrollToBottom()
    })
    typewriterPool.set(message.id, typewriter)
  }
  return typewriter
}

/** 关闭并移除某个气泡的打字机 */
function disposeTypewriter(message) {
  if (!message) return
  const typewriter = typewriterPool.get(message.id)
  if (typewriter) {
    typewriter.stop()
    typewriterPool.delete(message.id)
  }
}

/**
 * 把文本写进消息并同步解析结果
 * @param {object} message 消息对象
 * @param {string} text 当前要显示的文本（打字机可能只吐了一部分）
 * @param {string} [parseSource] 用于解析的完整文本；省略时沿用消息已有的 fullText，
 *   这样“结构化结果立刻以卡片出现”和“字符逐个显示”可以同时成立。
 */
function applyText(message, text, parseSource) {
  if (!message) return
  message.content = text
  if (parseSource !== undefined) message.fullText = parseSource
  const parsed = parseStep(message.fullText || text, message.stepNo ?? null)
  message.parsed = parsed
  message.stepNo = parsed.stepNo ?? message.stepNo ?? null
  message.kind = parsed.kind
}

/** 滚动到底部 */
async function scrollToBottom() {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

watch(
  () => messages.value.length,
  () => scrollToBottom(),
)

/** 用户是否停在底部（用于决定新内容是否自动跟随滚动） */
function isNearBottom() {
  const el = listRef.value
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight < 160
}

let messageSeed = 0
const nextId = () => `${Date.now()}-${++messageSeed}`

/** 新建一条 AI 气泡（普通模式一条，智能体模式一个步骤一条） */
function createAiMessage(text = '', stepNo = null) {
  messages.value.push({
    id: nextId(),
    role: 'ai',
    content: '',
    fullText: '',
    status: 'streaming',
    stepNo,
    kind: 'final',
    parsed: null,
  })
  activeIndex.value = messages.value.length - 1

  const message = activeMessage.value
  if (message && text) {
    message.fullText = text
    // 交给该气泡自己的打字机逐字播放（existing 传空串，保证从 0 开始吐）
    typewriterOf(message).push(text, '')
  }
  return message
}

/**
 * 当前步骤该显示第几步
 * - 片段里带编号时直接用；
 * - 只有“确实要开新气泡”时，才用“已识别步骤数 + 1”占位（步骤头可能只打了一半，例如 "Step 9"）；
 * - 其余情况沿用当前气泡的编号，避免因为编号暂时缺失而重复开气泡。
 */
const messageStepNo = (part, fallback) => part?.stepNo ?? fallback ?? lastStepCount + 1

/**
 * 智能体模式：把累积的流式文本按步骤切开
 * 每完成一个步骤就独占一个气泡；最后一段是“进行中”的步骤，继续往后追加
 */
function handleAgentChunk(chunk) {
  const { parts } = splitter.push(chunk)
  const lastIndex = parts.length - 1
  const lastPart = parts[lastIndex]

  const current = activeMessage.value
  const grew = parts.length > lastStepCount

  /*
   * 需要开新气泡的三种情况：
   * 1) 还没有任何气泡；
   * 2) 步骤数变多（上一步已完成）；
   * 3) 当前气泡的步骤号与最新片段不一致——包括
   *    先收到 "Step 9"（还没有编号）建了空气泡，随后编号补全的情形。
   */
  const provisional = !current || grew ? undefined : (current.stepNo ?? undefined)
  const stepNo = messageStepNo(lastPart, provisional)
  const startNewBubble = !current || grew || stepNo !== current.stepNo

  if (grew && current) {
    // 步骤数变多，说明原来“进行中”的那个步骤已经结束
    finalizeMessage(current, parts[lastIndex - 1]?.text ?? current.content)
  }

  const target = startNewBubble ? createAiMessage('', stepNo) : current

  if (target) {
    if (stepNo != null) target.stepNo = stepNo

    /*
     * fullText 记录该步骤的完整文本：
     * - 结构化解析（工具名、搜索结果）直接用完整文本，所以结果能立刻以卡片形式出现，
     *   不必等打字机把整段 JSON 吐完；
     * - 打字机只负责“显示”字符（content），两者互不影响。
     */
    target.fullText = lastPart.text

    /*
     * 只把“当前步骤里新增的部分”交给打字机：
     * - 如果新文本是气泡已有文本的延续，就只吐出多出来的那一截；
     *   否则（新气泡 / 编号补全导致文本重排）整段重放，并重新从 0 开始吐字。
     */
    const shown = target.content || ''
    if (lastPart.text === shown) {
      // 内容没变，无需重放
    } else if (lastPart.text.startsWith(shown)) {
      typewriterOf(target).push(lastPart.text.slice(shown.length), shown)
    } else {
      typewriterOf(target).push(lastPart.text, '')
    }
  }

  // 底部提示：正在执行第几步
  currentStepNo.value = stepNo

  lastStepCount = parts.length
}

/** 智能体模式收尾：把最后一段独立成一个步骤气泡 */
function finalizeAgentStream() {
  const tail = splitter.cut()
  const current = activeMessage.value

  if (!tail.text) {
    if (current) finalizeMessage(current, current.content, current.stepNo ?? lastStepCount)
    scrollToBottom()
    return
  }

  /*
   * 当前气泡是否“就是”最后这一段：
   * - 已经吐完整段（内容一致）；
   * - 或者正在吐（当前内容是它的前缀）——所以不能直接用全等判断，
   *   否则会把同一个步骤又新建一个气泡出来。
   */
  const shown = current?.content || ''
  const isSameStep =
    Boolean(current) && (shown === tail.text || (shown && tail.text.startsWith(shown)))

  if (isSameStep) {
    finalizeMessage(current, tail.text, tail.stepNo ?? current.stepNo ?? lastStepCount)
    scrollToBottom()
    return
  }

  // 最后一步紧贴在上一步之后，需要单独一个气泡
  if (current) finalizeMessage(current, current.content, current.stepNo ?? lastStepCount)
  const last = createAiMessage('')
  finalizeMessage(last, tail.text, tail.stepNo ?? lastStepCount + 1)
  scrollToBottom()
}

/** 步骤彻底结束：把解析结果落定（不再等打字机，直接给完整内容） */
function finalizeMessage(message, text, stepNo = null) {
  if (!message) return
  if (stepNo !== null && stepNo !== undefined) message.stepNo = stepNo
  applyText(message, text)
  message.status = 'done'
  message.parsed = parseStep(text, message.stepNo)
  message.kind = message.parsed.kind
  // 该气泡的内容已经完整，关掉它的打字机（省得继续空转/回吐）
  disposeTypewriter(message)
}

/** 普通模式：整个响应就是一条消息（增量追加） */
function handlePlainChunk(chunk) {
  const current = activeMessage.value
  if (!current) {
    createAiMessage(chunk)
    return
  }
  current.fullText = `${current.fullText || ''}${chunk}`
  typewriterOf(current).push(chunk, current.content)
}

/** 发送消息 */
function handleSend() {
  const text = inputText.value.trim()
  if (!text || streaming.value) return

  messages.value.push({ id: nextId(), role: 'user', content: text, status: 'done' })
  inputText.value = ''

  activeIndex.value = -1
  splitter.clear()
  lastStepCount = 0
  currentStepNo.value = null
  // 清掉上一轮遗留的打字机
  typewriterPool.forEach((typewriter) => typewriter.stop())
  typewriterPool.clear()
  streaming.value = true
  scrollToBottom()

  // 关键：查询参数必须 encodeURIComponent（中文、emoji、& 等都要安全），sse.js 里已统一处理
  const params = { message: text }
  if (props.chatId) params.chatId = props.chatId

  connection = createSseConnection({
    path: props.endpoint,
    params,
    onMessage: (chunk) => (props.stepMode ? handleAgentChunk(chunk) : handlePlainChunk(chunk)),
    onDone: () => finishStreaming(),
    onError: ({ message }) => {
      if (connection?.aborted?.()) {
        finishStreaming()
        return
      }
      const current = activeMessage.value
      if (current) {
        current.status = 'error'
        if (!current.content) applyText(current, `⚠️ ${message}`)
      } else {
        messages.value.push({
          id: nextId(),
          role: 'ai',
          content: `⚠️ ${message}`,
          status: 'error',
          kind: 'final',
          parsed: parseStep(`⚠️ ${message}`),
        })
      }
      streaming.value = false
      connection = null
    },
  })
}

/** 收尾：吐完剩余字符并结束流式状态 */
function finishStreaming() {
  connection = null
  streaming.value = false

  if (props.stepMode) {
    // 智能体模式：把缓冲里最后一段独立成一个步骤气泡
    finalizeAgentStream()
    return
  }

  const message = activeMessage.value
  if (message) {
    const typewriter = typewriterPool.get(message.id)
    if (typewriter) typewriter.flush()
    message.status = 'done'
    if (!message.content) {
      applyText(message, '（本次没有返回内容）')
    } else {
      // 流式结束后按完整 Markdown 重新解析一次，避免半截语法残留
      applyText(message, message.content)
    }
    disposeTypewriter(message)
  }
  scrollToBottom()
}

/** 停止生成 */
function handleStop() {
  if (!connection) return
  connection.abort()
  finishStreaming()
}

/** 清空当前会话 */
function handleClear() {
  handleStop()
  messages.value = []
  activeIndex.value = -1
  splitter.clear()
  lastStepCount = 0
  currentStepNo.value = null
  typewriterPool.forEach((typewriter) => typewriter.stop())
  typewriterPool.clear()
}

/** 新建会话：重新生成聊天室 id（仅恋爱大师这类需要 chatId 的应用） */
function handleNewChat() {
  handleClear()
  emit('reset-chat-id')
}

/** 输入框键盘事件：Enter 发送，Shift + Enter 换行 */
function handleKeydown(event) {
  if (event.key !== 'Enter' || event.isComposing) return
  if (event.shiftKey) return
  event.preventDefault()
  handleSend()
}

onBeforeUnmount(() => {
  connection?.abort?.()
  connection = null
  typewriterPool.forEach((typewriter) => typewriter.stop())
  typewriterPool.clear()
})
</script>

<template>
  <div class="page chat-page" :data-ready="ready ? 'true' : 'false'">
    <div class="page-inner chat-shell">
      <AppHeader :emoji="emoji" :title="title" :subtitle="subtitle" back sticky>
        <template #actions>
          <span v-if="chatId" class="chip chat-id" :title="chatId">
            会话 {{ chatId.slice(-8) }}
          </span>
          <button class="btn" :disabled="streaming" @click="handleNewChat">
            <span class="full">新建会话</span><span class="short">新会话</span>
          </button>
          <button class="btn" :disabled="streaming || !messages.length" @click="handleClear">
            清空
          </button>
        </template>
      </AppHeader>

      <div class="chat-toolbar toolbar">
        <span v-if="stepMode" class="chip">智能体分步执行 · 一步一个气泡</span>
        <label v-if="stepMode" class="switch toolbar-end" :title="compact ? '展开每个步骤的详细结果' : '只看工具名与摘要'">
          <input v-model="compact" type="checkbox" />
          <span class="switch-track"><span class="switch-thumb"></span></span>
          <span class="switch-label">精简输出</span>
        </label>
      </div>

      <main ref="listRef" class="chat-list surface">
        <div v-if="!messages.length" class="empty">
          <div class="empty-emoji">{{ emoji }}</div>
          <p class="empty-title">可以开始聊天了</p>
          <p class="empty-sub">{{ hint || placeholder }}</p>
        </div>

        <div
          v-for="message in messages"
          :key="message.id"
          class="message"
          :class="message.role === 'user' ? 'is-user' : 'is-ai'"
        >
          <div class="bubble-avatar" aria-hidden="true">
            {{ message.role === 'user' ? '我' : emoji }}
          </div>
          <div class="bubble-wrap">
            <!-- 智能体步骤：一个步骤一个气泡（工具调用 → 结构化卡片） -->
            <AgentStepBubble
              v-if="message.role === 'ai' && stepMode"
              :message="message"
              :compact="compact"
            />
            <!-- 用户消息：纯文本 -->
            <div v-else-if="message.role === 'user'" class="bubble bubble-user">
              {{ message.content }}
            </div>
            <!-- 普通 AI 消息：Markdown -->
            <div
              v-else
              class="bubble bubble-ai markdown-body"
              v-html="renderMarkdownSafe(message.content)"
            ></div>

            <span v-if="!stepMode && message.status === 'streaming'" class="cursor">
              <template v-if="!message.content">思考中</template>
            </span>
          </div>
        </div>
      </main>

      <footer class="chat-input surface">
        <textarea
          v-model="inputText"
          data-role="chat-input"
          rows="2"
          :placeholder="placeholder"
          :disabled="streaming"
          @keydown="handleKeydown"
        ></textarea>
        <button
          v-if="streaming"
          class="btn btn-danger btn-lg send-stop"
          data-action="chat-stop"
          @click="handleStop"
        >
          停止
        </button>
        <button
          v-else
          class="btn btn-primary btn-lg send-stop"
          data-action="chat-send"
          :disabled="!inputText.trim()"
          @click="handleSend"
        >
          发送
        </button>
      </footer>

      <p class="chat-hint">
        <template v-if="streaming && stepMode">
          <span class="spinner" aria-hidden="true"></span>
          <span>正在执行第 {{ currentStepNo ?? 1 }} 步…已收到 {{ stepsDone }} 个步骤</span>
        </template>
        <template v-else>{{ hint }}</template>
      </p>
    </div>
  </div>
</template>

<style scoped>
/* ---------------- 页面骨架：头部 + 消息区（可滚动）+ 固定输入区 ---------------- */
.chat-page {
  height: 100vh;
  height: 100dvh;
  min-height: 0;
  padding-bottom: max(clamp(8px, 1.6vw, 14px), env(safe-area-inset-bottom));
}

/*
 * 聊天页与主页同宽同内边距，头部 / 消息区 / 输入区三者左右严格对齐。
 * 注意：这里用 width: auto（而不是 100%），避免把自身的 max-width 撑破。
 */
.chat-shell {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: clamp(8px, 1.4vw, 12px);
}

/* ---------------- 头部右侧 ---------------- */
.chat-id {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.full {
  display: inline;
}
.short {
  display: none;
}

/* ---------------- 工具栏 ---------------- */
.chat-toolbar {
  flex: none;
  min-height: 0;
}

/* ---------------- 消息区 ---------------- */
.chat-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  padding: clamp(12px, 2vw, 20px);
  scroll-behavior: smooth;
}

.empty {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 24px 12px;
  color: var(--text-3);
  text-align: center;
}
.empty-emoji {
  font-size: clamp(36px, 8vw, 46px);
  line-height: 1;
}
.empty-title {
  margin: 6px 0 0;
  font-size: 15px;
  color: var(--text-2);
}
.empty-sub {
  margin: 0;
  max-width: var(--measure);
  font-size: 12.5px;
  line-height: 1.7;
}

.message {
  display: flex;
  gap: var(--gap-sm);
  margin-bottom: clamp(12px, 2vw, 18px);
}
.message.is-user {
  flex-direction: row-reverse;
}
.message:last-child {
  margin-bottom: 0;
}

.bubble-avatar {
  width: 34px;
  height: 34px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 600;
  background: #eef0f6;
  color: var(--text-2);
}
.message.is-ai .bubble-avatar {
  background: var(--brand-gradient);
  color: #fff;
}

/* 消息列宽度统一在此控制，max-width 用 min() 兜底，避免被内容撑出多余留白 */
.bubble-wrap {
  flex: 1;
  min-width: 0;
  max-width: min(80%, 720px);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.message.is-user .bubble-wrap {
  flex: none;
  align-items: flex-end;
}
.bubble,
.step-card {
  max-width: 100%;
}

.bubble {
  padding: 10px 14px;
  border-radius: var(--r-lg);
  font-size: 14px;
  line-height: 1.65;
  animation: bubble-in var(--t-base);
  word-break: break-word;
}

.bubble-user {
  background: var(--brand-gradient);
  color: #fff;
  border-top-right-radius: 4px;
  white-space: pre-wrap;
}

.bubble-ai {
  background: var(--panel-2);
  color: var(--text);
  border-top-left-radius: 4px;
}

@keyframes bubble-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* 流式输出光标（普通模式） */
.cursor {
  display: inline-block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-3);
}
.cursor::after {
  content: '▋';
  margin-left: 2px;
  animation: blink 1s steps(2, start) infinite;
}
@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

/* ---------------- 输入区 ---------------- */
.chat-input {
  flex: none;
  display: flex;
  align-items: flex-end;
  gap: var(--gap-sm);
  padding: clamp(10px, 1.6vw, 14px);
}
.chat-input textarea {
  flex: 1;
  min-width: 0;
  resize: none;
  max-height: 168px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  background: var(--bg-soft);
  outline: none;
  transition: border-color var(--t-fast), box-shadow var(--t-fast), background var(--t-fast);
}
.chat-input textarea:focus {
  background: var(--panel);
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(79, 110, 247, 0.14);
}
.chat-input textarea:disabled {
  background: var(--panel-2);
  color: var(--text-3);
}

.send-stop {
  flex: none;
  min-width: 96px;
}

/* ---------------- 底部提示 ---------------- */
.chat-hint {
  flex: none;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 18px;
  text-align: center;
  font-size: 12px;
  color: var(--text-3);
}

.spinner {
  width: 12px;
  height: 12px;
  flex: none;
  border-radius: 50%;
  border: 2px solid rgba(79, 110, 247, 0.25);
  border-top-color: var(--brand);
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ==========================================================================
   响应式
   ========================================================================== */

/*
 * 宽度策略：
 * - AI 消息（含步骤卡片）占满整列，长文本 / 搜索结果更省空间；
 * - 用户消息保持 70%～82%，左右对比更清晰；
 * - 桌面端整体收窄到 80% / 720px，避免行长过长。
 */
.message.is-ai .bubble-wrap {
  max-width: 100%;
}

/* 平板 / 小笔记本 */
@media (min-width: 901px) {
  .message.is-ai .bubble-wrap {
    max-width: min(80%, 720px);
  }
}

/* 手机：气泡接近满宽、字号略收，隐藏会话号与按钮完整文案 */
@media (max-width: 640px) {
  .chat-id,
  .switch-label {
    display: none;
  }
  .full {
    display: none;
  }
  .short {
    display: inline;
  }
  .message.is-user .bubble-wrap {
    max-width: 82%;
  }
  .bubble-avatar {
    width: 30px;
    height: 30px;
    font-size: 12px;
  }
  .bubble {
    font-size: 13.5px;
    padding: 9px 12px;
  }
  .chat-list {
    scroll-behavior: auto;
  }
  .chat-input textarea {
    max-height: min(30vh, 132px);
  }
}

/* 小屏手机：输入区改为上下排列，避免输入框被压得过窄 */
@media (max-width: 520px) {
  .chat-input {
    flex-direction: column;
    align-items: stretch;
    gap: var(--gap-xs);
  }
  .chat-input textarea {
    width: 100%;
    max-height: min(26vh, 112px);
  }
  .send-stop {
    width: 100%;
    min-width: 0;
    height: 44px;
  }
  .chat-hint {
    font-size: 11.5px;
  }
}

/* 矮屏 / 横屏手机：压缩纵向留白，把高度尽量留给消息区 */
@media (max-height: 520px) {
  .chat-shell {
    gap: 6px;
  }
  .chat-list {
    padding: 10px;
  }
  .message {
    margin-bottom: 10px;
  }
  .chat-input {
    padding: 8px 10px;
  }
  .chat-input textarea {
    max-height: 64px;
  }
  .chat-hint {
    display: none;
  }
  .chat-page :deep(.app-header) {
    padding: 8px 12px;
  }
}
</style>
