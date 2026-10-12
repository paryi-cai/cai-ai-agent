<script setup>
/**
 * 页面 2：AI 超级智能体（Manus）
 * 与页面 1 同样的聊天室风格，调用 /ai/manus/chat（无 chatId）。
 * 后端每次 SSE 响应 = 智能体的一个步骤 → 单独一个气泡展示，
 * 收到什么就展示什么（工具调用做成可读的结果卡片，最后一步是最终答复）。
 */
import ChatRoom from '@/components/ChatRoom.vue'
import { API_PATH } from '@/api/config'
</script>

<template>
  <ChatRoom
    title="AI 超级智能体"
    subtitle="自主规划并分步执行任务：每个步骤一个气泡，执行过程实时推送"
    emoji="🧠"
    :endpoint="API_PATH.MANUS_CHAT"
    step-mode
    placeholder="描述你的任务，例如：帮我搜索并总结今天的 AI 资讯"
    hint="SSE 接口：GET /ai/manus/chat?message=xxx（每一步一次响应，一步一个气泡）"
  />
</template>
