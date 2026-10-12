import { createRouter, createWebHashHistory } from 'vue-router'

import HomeView from '@/views/HomeView.vue'
import FoodAppView from '@/views/FoodAppView.vue'
import ManusAppView from '@/views/ManusAppView.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
    meta: { title: 'AI 应用中心' },
  },
  {
    // 页面 1：菜狗探店（AI 美食探店博主）
    path: '/food',
    name: 'food',
    component: FoodAppView,
    meta: { title: '菜狗探店' },
  },
  {
    // 页面 2：AI 超级智能体
    path: '/manus',
    name: 'manus',
    component: ManusAppView,
    meta: { title: 'AI 超级智能体' },
  },
  // 兜底：未知路径回主页
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const SITE_NAME = 'AI 应用中心'

const router = createRouter({
  // 使用 hash 模式，打包后无需后端做 history 回退配置
  history: createWebHashHistory(),
  routes,
})

router.afterEach((to) => {
  const title = to.meta?.title
  document.title = !title || title === SITE_NAME ? SITE_NAME : `${title} · ${SITE_NAME}`
})

export default router
