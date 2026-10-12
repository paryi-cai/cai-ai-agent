<script setup>
/**
 * 智能体步骤气泡
 *
 * 一个 SSE 响应 = 一个步骤 = 一个气泡：
 * - 工具调用（searchWeb / generatePDF / doTerminate…）→ 结构化卡片：工具名 + 可读结果 + 折叠原始数据
 * - 最后一步的最终答复 → 正常 Markdown 渲染
 */
import { computed, ref } from 'vue'

import { DEFAULT_VISIBLE_RESULTS, parseStep, peekStep, STEP_KIND } from '@/utils/agentStep'
import { renderMarkdownSafe } from '@/utils/markdown'

const props = defineProps({
  /** 消息对象：{ stepNo, content, status, kind, parsed } */
  message: { type: Object, required: true },
  /** 精简模式：只保留工具名与一行摘要 */
  compact: { type: Boolean, default: false },
})

const expanded = ref(false)
const copied = ref(false)
const showAllResults = ref(false)
const textExpanded = ref(false)

const parsed = computed(
  () =>
    props.message.parsed ||
    parseStep(
      props.message.fullText || props.message.content || '',
      props.message.stepNo ?? null,
    ),
)

/** 流式过程中先亮出“正在调用哪个工具”，不用等结果解析完 */
const peek = computed(() =>
  peekStep(props.message.fullText || props.message.content || '', props.message.stepNo ?? null),
)

const toolLabel = computed(() => parsed.value.toolName || peek.value.toolName || '调用工具中')

const isTool = computed(
  () =>
    (props.message.kind === STEP_KIND.TOOL || peek.value.toolFinished) &&
    Boolean(parsed.value.toolName || peek.value.toolName),
)
const isStreaming = computed(() => props.message.status === 'streaming')
const isError = computed(() => props.message.status === 'error')

/** 流式阶段提示：等待中 / 接收中 / 已完成 */
const phase = computed(() => {
  if (isError.value) return '执行出错'
  if (!isStreaming.value) return '已完成'
  return props.message.content ? '接收结果中' : '调用中'
})

const visibleEntries = computed(() =>
  showAllResults.value ? parsed.value.entries : parsed.value.entries.slice(0, DEFAULT_VISIBLE_RESULTS),
)

const hiddenCount = computed(() =>
  Math.max(0, parsed.value.entries.length - visibleEntries.value.length),
)

/** 长文本结果需要单独成块展示 */
const showText = computed(
  () => parsed.value.isText && (parsed.value.text.length > 60 || parsed.value.text.includes('\n')),
)

/** 结果内容是否值得单独成块展示（很短的结果上面摘要已经显示了，不再重复） */
const hasBody = computed(
  () => parsed.value.isSearch || Boolean(parsed.value.filePath) || showText.value,
)

const digestText = computed(() => {
  const digest = parsed.value.digest || ''
  if (props.compact && digest.length > 60) return `${digest.slice(0, 60)}…`
  return digest
})

/** 最终答复的 Markdown 只渲染一次 */
const finalHtml = computed(() => renderMarkdownSafe(props.message.content))

/**
 * 工具返回的纯文本（文件内容 / 报错）也按 Markdown 渲染。
 * 解析用的是完整文本，但显示要跟着打字机的进度走，所以按已显示比例截取。
 */
const visibleText = computed(() => {
  const { text } = parsed.value
  if (!isStreaming.value) return text
  const shown = (props.message.content || '').length
  const total = (props.message.fullText || '').length
  if (!total || shown >= total) return text
  return text.slice(0, Math.max(1, Math.round((text.length * shown) / total)))
})

const resultHtml = computed(() => renderMarkdownSafe(visibleText.value))

async function copyResult() {
  try {
    await navigator.clipboard.writeText(parsed.value.text || parsed.value.raw || '')
    copied.value = true
    setTimeout(() => (copied.value = false), 1500)
  } catch {
    copied.value = false
  }
}
</script>

<template>
  <!-- 最终答复：正常 Markdown 气泡 -->
  <div v-if="!isTool" class="bubble bubble-ai markdown-body" :class="{ 'is-error': isError }">
    <div v-if="finalHtml" v-html="finalHtml"></div>
    <span v-else class="pending">{{ isError ? '（没有返回内容）' : '思考中' }}</span>
  </div>

  <!-- 工具调用步骤：结构化卡片 -->
  <div v-else class="step-card" :class="{ 'is-compact': compact, 'is-error': isError }">
    <header class="step-head">
      <span class="step-badge">第 {{ parsed.stepNo ?? '?' }} 步</span>
      <span class="step-tool">
        <span class="tool-glyph">⚙</span>
        <code>{{ toolLabel }}</code>
      </span>
      <span class="step-state" :class="{ done: !isStreaming }">{{ phase }}</span>
    </header>

    <p v-if="digestText" class="step-digest">{{ digestText }}</p>

    <!-- 结果内容 -->
    <div v-if="!compact && hasBody" class="step-body">
      <template v-if="isStreaming && !parsed.isSearch && !parsed.text">
        <span class="pending">正在接收结果…</span>
      </template>

      <!-- 搜索结果：条目化展示 -->
      <template v-else-if="parsed.isSearch">
        <ol class="result-list">
          <li v-for="entry in visibleEntries" :key="`${entry.position}-${entry.link}`" class="result-item">
            <a
              v-if="entry.link"
              class="result-title"
              :href="entry.link"
              target="_blank"
              rel="noreferrer noopener"
              :title="entry.title"
            >
              {{ entry.title }}
            </a>
            <span v-else class="result-title">{{ entry.title }}</span>

            <div v-if="entry.snippet" class="result-snippet">{{ entry.snippet }}</div>

            <div class="result-meta">
              <span v-if="entry.domain" class="result-domain">{{ entry.domain }}</span>
              <span v-if="entry.date" class="result-date">{{ entry.date }}</span>
              <a v-if="entry.link" class="result-link" :href="entry.link" target="_blank" rel="noreferrer noopener">
                打开原文 ↗
              </a>
            </div>
          </li>
        </ol>
        <button v-if="hiddenCount" class="more-btn" @click="showAllResults = !showAllResults">
          {{ showAllResults ? '收起多余结果' : `展开其余 ${hiddenCount} 条结果` }}
        </button>
      </template>

      <!-- 文件 / 普通文本结果 -->
      <template v-else>
        <p v-if="parsed.filePath" class="file-line">
          <span class="file-icon">📄</span>
          <code>{{ parsed.filePath }}</code>
        </p>
        <template v-else-if="showText">
          <div
            class="text-result markdown-body"
            :class="{ 'is-collapsed': !textExpanded && parsed.isLong }"
            v-html="resultHtml"
          ></div>
          <button v-if="parsed.isLong" class="more-btn" @click="textExpanded = !textExpanded">
            {{ textExpanded ? '收起内容' : '展开全部内容' }}
          </button>
        </template>
        <span v-else-if="isStreaming" class="pending">正在接收结果…</span>
      </template>
    </div>

    <footer v-if="!compact && (parsed.jsonText || parsed.isLong)" class="step-foot">
      <button class="mini-btn" @click="expanded = !expanded">
        {{ expanded ? '收起原始数据' : '查看原始数据' }}
      </button>
      <button class="mini-btn" @click="copyResult">{{ copied ? '已复制' : '复制结果' }}</button>
    </footer>

    <pre v-if="!compact && expanded && parsed.jsonText" class="json-block"><code>{{ parsed.jsonText }}</code></pre>
  </div>
</template>

<style scoped>
/* ==========================================================================
   步骤气泡：与聊天室共用同一套设计令牌（main.css）
   ========================================================================== */

/* ---------------- 最终答复气泡 ---------------- */
.bubble {
  padding: 10px 14px;
  border-radius: var(--r-lg);
  font-size: 14px;
  line-height: 1.65;
  animation: bubble-in var(--t-base);
  word-break: break-word;
}
.bubble-ai {
  background: var(--panel-2);
  color: var(--text);
  border-top-left-radius: 4px;
}
.bubble-ai.is-error {
  background: var(--danger-soft);
  color: #b4232b;
}
.pending {
  display: inline-block;
  color: var(--text-3);
  font-size: 13px;
}
.pending::after {
  content: '▋';
  margin-left: 3px;
  animation: blink 1s steps(2, start) infinite;
}

/* ---------------- 步骤卡片 ---------------- */
.step-card {
  align-self: stretch;
  padding: clamp(10px, 1.6vw, 14px);
  border-radius: var(--r-lg);
  border: 1px solid var(--border);
  border-left: 3px solid #7c8cff;
  background: var(--panel-2);
  box-shadow: var(--shadow-sm);
  animation: bubble-in var(--t-base);
  min-width: 0;
}
.step-card.is-compact {
  padding: 8px 12px;
  background: var(--bg-soft);
}
.step-card.is-error {
  border-left-color: var(--danger);
  background: #fdf7f7;
}

.step-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 0;
}
.step-badge {
  flex: none;
  padding: 2px 10px;
  border-radius: var(--r-full);
  background: var(--brand-gradient);
  color: #fff;
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.2px;
}
.step-tool {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
  color: var(--text-2);
  font-size: 12.5px;
}
.step-tool code {
  padding: 1px 8px;
  border-radius: 6px;
  background: var(--panel);
  border: 1px solid var(--border);
  font-family: var(--font-mono);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 42vw;
}
.tool-glyph {
  flex: none;
  font-size: 12px;
  color: var(--brand);
}
.step-state {
  margin-left: auto;
  flex: none;
  font-size: 11.5px;
  color: var(--text-3);
}
.step-state.done {
  color: var(--success);
}

.step-digest {
  margin: 8px 0 0;
  color: var(--text-2);
  font-size: 13px;
  word-break: break-word;
  overflow-wrap: anywhere;
}
.is-compact .step-digest {
  margin-top: 5px;
  font-size: 12.5px;
}

.step-body {
  margin-top: 10px;
  min-width: 0;
}

/* ---------------- 搜索结果 ---------------- */
.result-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.result-item {
  padding: 9px 11px;
  border-radius: var(--r-sm);
  background: var(--panel);
  border: 1px solid var(--border);
  transition: border-color var(--t-fast), box-shadow var(--t-fast);
}
.result-item:hover {
  border-color: var(--border-strong);
  box-shadow: var(--shadow-sm);
}
.result-title {
  display: block;
  font-weight: 600;
  font-size: 13.5px;
  line-height: 1.5;
  color: var(--link);
  text-decoration: none;
  word-break: break-word;
}
a.result-title:hover {
  text-decoration: underline;
}
.result-snippet {
  margin-top: 5px;
  color: var(--text-2);
  font-size: 12.5px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}
.result-meta {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 10px;
  font-size: 11.5px;
  color: var(--text-3);
}
.result-domain {
  padding: 1px 8px;
  border-radius: var(--r-full);
  background: var(--brand-soft);
  color: var(--brand);
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.result-link {
  margin-left: auto;
  color: var(--brand);
  text-decoration: none;
  white-space: nowrap;
}
.result-link:hover {
  text-decoration: underline;
}

/* ---------------- 次要按钮（展开 / 复制） ---------------- */
.more-btn,
.mini-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border: 1px solid var(--border);
  border-radius: var(--r-full);
  background: var(--panel);
  color: var(--text-2);
  font-size: 12px;
  transition: border-color var(--t-fast), color var(--t-fast), background var(--t-fast);
}
.more-btn {
  margin-top: 10px;
  border-style: dashed;
}
.more-btn:hover,
.mini-btn:hover {
  border-color: var(--brand);
  color: var(--brand);
}
.step-foot {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* ---------------- 文件 / 普通文本结果 ---------------- */
.file-line,
.plain-line {
  margin: 0;
  font-size: 13px;
  color: var(--text);
  word-break: break-word;
  overflow-wrap: anywhere;
}
.file-line {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.file-line code {
  padding: 2px 8px;
  border-radius: 6px;
  background: var(--panel);
  border: 1px solid var(--border);
  font-family: var(--font-mono);
  font-size: 12px;
  word-break: break-all;
}
.file-icon {
  font-size: 14px;
}

/* 文件内容一类的长文本结果 */
.text-result {
  padding: 10px 12px;
  border-radius: var(--r-sm);
  background: var(--panel);
  border: 1px solid var(--border);
  font-size: 13px;
  line-height: 1.7;
  overflow-x: auto;
}
.text-result.is-collapsed {
  max-height: 220px;
  overflow: hidden;
  position: relative;
}
.text-result.is-collapsed::after {
  content: '';
  position: absolute;
  inset: auto 0 0 0;
  height: 56px;
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0), var(--panel));
}

/* 原始 JSON */
.json-block {
  margin: 10px 0 0;
  padding: 10px 12px;
  max-height: 260px;
  overflow: auto;
  border-radius: var(--r-sm);
  background: #1f2430;
  color: #dbe3f5;
  font-size: 12px;
  line-height: 1.6;
}
.json-block code {
  font-family: var(--font-mono);
  white-space: pre;
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
@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

/* ==========================================================================
   响应式
   ========================================================================== */
@media (max-width: 640px) {
  .bubble {
    font-size: 13.5px;
    padding: 9px 12px;
  }
  .step-tool code {
    max-width: 46vw;
    font-size: 11.5px;
  }
  .result-snippet {
    -webkit-line-clamp: 4;
    line-clamp: 4;
  }
  .text-result {
    font-size: 12.5px;
  }
  .text-result.is-collapsed {
    max-height: 168px;
  }
  .json-block {
    max-height: 200px;
    font-size: 11.5px;
  }
}

@media (pointer: coarse) {
  .mini-btn,
  .more-btn {
    min-height: 34px;
    padding: 7px 14px;
  }
  /* 触屏上把“标题 / 打开原文”也做成足够大的点击区域 */
  .result-title,
  .result-link {
    display: inline-flex;
    align-items: center;
    min-height: 32px;
    padding: 4px 0;
  }
  .result-link {
    margin-left: auto;
    justify-content: flex-end;
  }
}
</style>
