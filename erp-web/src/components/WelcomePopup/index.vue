<template>
  <Transition name="welcome-slide">
    <div
      v-if="visible"
      class="welcome-popup"
      role="status"
      @mouseenter="pause"
      @mouseleave="resume"
    >
      <button class="welcome-close" type="button" aria-label="关闭" @click="close">×</button>

      <div class="welcome-head">
        <span class="welcome-wave" aria-hidden="true">👋</span>
        <span class="welcome-greeting">{{ greeting }}，{{ userStore.nickName }}</span>
      </div>

      <p class="welcome-quote">{{ quote }}</p>

      <div class="welcome-brand">
        <span class="welcome-mark">爱</span>
        <span class="welcome-name">爱用 ERP</span>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useUserStore } from '@/store/modules/user'

/**
 * 登录欢迎弹窗（右下角）。
 *
 * 触发规则：每次登录后弹一次。
 *   sessionStorage 记 `erp_welcome_shown_<用户ID>` —— 同一标签页内刷新不会重复，
 *   关闭标签页或退出登录后标记被清掉，下次登录会再次出现。
 *
 * 弹窗不阻塞页面：3 秒后自动消失，不需要用户操作。
 * 鼠标悬停时暂停计时（正在看的时候不会突然消失），× 只是可选的提前关闭。
 */
const AUTO_CLOSE_MS = 3000

const QUOTES = [
  '每一个认真对待的今天，都是未来的底气。',
  '先把眼前这一件事做好，答案会自己出现。',
  '慢一点没关系，方向对就好。',
  '今天的进度哪怕只有 1%，也是在向前。',
  '把复杂的事做简单，把简单的事做扎实。',
  '先完成，再完美。',
  '你今天的努力，会在某个时刻派上用场。',
  '好的开始，是成功的一半。'
]

const userStore = useUserStore()
const visible = ref(false)
let timer = null

/** 按当前时间给问候语 */
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

/** 用「一年中的第几天」做种子：同一天固定同一条，刷新不会来回跳 */
const quote = computed(() => {
  const now = new Date()
  const start = new Date(now.getFullYear(), 0, 0)
  const dayOfYear = Math.floor((now - start) / 86400000)
  return QUOTES[dayOfYear % QUOTES.length]
})

function storageKey(userId) {
  return `erp_welcome_shown_${userId}`
}

function startTimer() {
  clearTimeout(timer)
  timer = setTimeout(close, AUTO_CLOSE_MS)
}

function pause() {
  clearTimeout(timer)
}

function resume() {
  startTimer()
}

function close() {
  clearTimeout(timer)
  visible.value = false
}

/**
 * 等 userInfo 到位再判断。
 * 正常情况下 permission.js 已经先调过 getInfo，这里只是兜底。
 */
watch(
  () => userStore.userInfo && userStore.userInfo.id,
  (userId) => {
    if (!userId) return
    try {
      if (sessionStorage.getItem(storageKey(userId))) return
      // 弹过就记下：同一会话内刷新或再次进入首页都不再打扰
      sessionStorage.setItem(storageKey(userId), '1')
    } catch (e) {
      // 浏览器禁用 sessionStorage 时照常弹出，不影响主流程
    }
    visible.value = true
    startTimer()
  },
  { immediate: true }
)

onBeforeUnmount(() => clearTimeout(timer))
</script>

<style scoped>
.welcome-popup {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 2000;
  width: 320px;
  max-width: calc(100vw - 32px);
  padding: 18px 20px 16px;
  overflow: hidden;
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow:
    0 12px 32px rgba(15, 23, 42, 0.16),
    0 2px 8px rgba(15, 23, 42, 0.08);
}

/* 顶部红色渐变条，和全站红白主题呼应 */
.welcome-popup::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 3px;
  background: linear-gradient(90deg, #ae0016 0%, #d9001b 55%, #f4566a 100%);
}

.welcome-close {
  position: absolute;
  top: 8px;
  right: 10px;
  padding: 2px 4px;
  font-size: 18px;
  line-height: 1;
  color: #b0b6c0;
  cursor: pointer;
  background: none;
  border: none;
  transition: color 0.2s;
}

.welcome-close:hover {
  color: #5c6270;
}

.welcome-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-right: 18px;
}

.welcome-wave {
  font-size: 18px;
  line-height: 1;
}

.welcome-greeting {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.welcome-quote {
  margin: 12px 0 16px;
  font-size: 13px;
  line-height: 1.8;
  color: #5c6270;
}

.welcome-brand {
  display: flex;
  align-items: center;
  gap: 6px;
}

.welcome-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  font-size: 11px;
  font-weight: 700;
  color: #ffffff;
  background-color: #d9001b;
  border-radius: 5px;
}

.welcome-name {
  font-size: 11px;
  letter-spacing: 0.06em;
  color: #b0b6c0;
}

/* 从右下角滑入 */
.welcome-slide-enter-active,
.welcome-slide-leave-active {
  transition:
    opacity 0.28s ease,
    transform 0.28s ease;
}

.welcome-slide-enter-from,
.welcome-slide-leave-to {
  opacity: 0;
  transform: translateY(16px);
}
</style>
