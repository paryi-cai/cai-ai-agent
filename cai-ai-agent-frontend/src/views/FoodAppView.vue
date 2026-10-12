<script setup>
/**
 * 页面 1：菜狗探店（AI 美食探店博主）
 * 聊天室风格 + SSE 打字机效果，进入页面自动生成聊天室 id（chatId）
 * 支持按区域过滤知识库检索（全部 / 南山 / 福田 / 罗湖 / 宝安）
 */
import { ref } from 'vue'

import ChatRoom from '@/components/ChatRoom.vue'
import { API_PATH } from '@/api/config'
import { getChatId, resetChatId } from '@/utils/chatId'

const CHAT_ID_KEY = 'food_app_chat_id'

// 进入页面即生成（刷新页面复用同一个，避免会话上下文丢失）
const chatId = ref(getChatId(CHAT_ID_KEY, 'food'))

/** 检索区域：'' = 全库；选择具体区域则只检索该区域的探店知识 */
const region = ref('')

/** 区域选项（与知识库的区域元数据保持一致） */
const regionOptions = [
  { label: '全部', value: '' },
  { label: '南山', value: '南山' },
  { label: '福田', value: '福田' },
  { label: '罗湖', value: '罗湖' },
  { label: '宝安', value: '宝安' },
]

/** “新建会话”时换一个新的聊天室 id */
function handleResetChatId() {
  chatId.value = resetChatId(CHAT_ID_KEY, 'food')
}
</script>

<template>
  <ChatRoom
    title="菜狗探店"
    subtitle="深圳美食探店博主，懂吃懂玩懂避雷，回复实时逐字输出"
    emoji="🍜"
    :endpoint="API_PATH.FOOD_APP_CHAT_SSE"
    :chat-id="chatId"
    v-model:region="region"
    :region-options="regionOptions"
    placeholder="说说你想吃什么、在哪个区、什么场景～（Enter 发送，Shift + Enter 换行）"
    hint="SSE 接口：GET /ai/food_app/chat/sse?message=xxx&chatId=xxx&region=南山（可选）"
    @reset-chat-id="handleResetChatId"
  />
</template>
