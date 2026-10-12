<script setup>
/**
 * 全站统一的应用头部
 * - 主页：品牌标识 + 名称 + 后端状态
 * - 聊天页：返回按钮 + 应用标识 + 名称/副标题 + 右侧操作
 * 样式类（.app-header / .icon-chip / .btn）在 main.css 里统一定义。
 */
import { useRouter } from 'vue-router'

const props = defineProps({
  /** 应用图标（emoji） */
  emoji: { type: String, default: '🤖' },
  /** 主标题 */
  title: { type: String, required: true },
  /** 副标题 */
  subtitle: { type: String, default: '' },
  /** 是否显示返回主页按钮 */
  back: { type: Boolean, default: false },
  /** 头部是否吸顶（聊天页用） */
  sticky: { type: Boolean, default: false },
  /** 品牌模式：标题使用渐变文字（主页用） */
  brand: { type: Boolean, default: false },
})

const router = useRouter()

function goBack() {
  router.push('/')
}
</script>

<template>
  <header class="app-header surface" :class="{ 'is-sticky': props.sticky }">
    <button v-if="props.back" class="btn btn-icon" title="返回主页" aria-label="返回主页" @click="goBack">
      ←
    </button>

    <span class="icon-chip" aria-hidden="true">{{ props.emoji }}</span>

    <div class="app-header-titles">
      <h1 :class="props.brand ? 'app-title is-brand' : ''">{{ props.title }}</h1>
      <p v-if="props.subtitle">{{ props.subtitle }}</p>
    </div>

    <div class="app-header-actions toolbar">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.is-sticky {
  position: sticky;
  top: max(8px, env(safe-area-inset-top));
  z-index: var(--z-header);
  backdrop-filter: saturate(180%) blur(8px);
  background: rgba(255, 255, 255, 0.92);
}

.is-brand {
  background: linear-gradient(120deg, #4f6ef7, #8b5cf6 50%, #ec4899);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.app-header-actions {
  flex: none;
}

@media (max-width: 560px) {
  .icon-chip {
    width: 36px;
    height: 36px;
    font-size: 18px;
  }
  .app-header-actions {
    gap: 6px;
  }
  /* 窄屏优先保证标题与操作区完整，副标题让位（应用页内部还有同样的说明） */
  .app-header-titles p {
    display: none;
  }
}
</style>
