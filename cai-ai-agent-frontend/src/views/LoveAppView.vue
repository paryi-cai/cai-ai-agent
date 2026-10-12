<script setup>
/**
 * 页面 1：AI 恋爱大师
 * 聊天室风格 + SSE 打字机效果，进入页面自动生成聊天室 id（chatId）
 */
import { ref } from 'vue'

import ChatRoom from '@/components/ChatRoom.vue'
import { API_PATH } from '@/api/config'
import { getChatId, resetChatId } from '@/utils/chatId'

const CHAT_ID_KEY = 'love_app_chat_id'

// 进入页面即生成（刷新页面复用同一个，避免会话上下文丢失）
const chatId = ref(getChatId(CHAT_ID_KEY, 'love'))

/** “新建会话”时换一个新的聊天室 id */
function handleResetChatId() {
  chatId.value = resetChatId(CHAT_ID_KEY, 'love')
}
</script>

<template>
  <ChatRoom
    title="AI 恋爱大师"
    subtitle="知心大姐姐在线陪你聊感情问题，回复实时逐字输出"
    emoji="💕"
    :endpoint="API_PATH.LOVE_APP_CHAT_SSE"
    :chat-id="chatId"
    placeholder="说说你遇到的感情困惑吧～（Enter 发送，Shift + Enter 换行）"
    hint="SSE 接口：GET /ai/love_app/chat/sse?message=xxx&chatId=xxx"
    @reset-chat-id="handleResetChatId"
  />
</template>
