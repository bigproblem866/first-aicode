<template>
  <el-drawer
    v-model="visible"
    class="ai-assistant-drawer"
    direction="rtl"
    size="380px"
    :with-header="false"
    append-to-body
  >
    <div class="ai-panel">
      <header class="ai-header">
        <div class="ai-header-title">
          <span class="ai-dot" />
          <span>AI 助手</span>
        </div>
        <el-icon class="ai-close" @click="visible = false"><Close /></el-icon>
      </header>

      <div ref="bodyRef" class="ai-body">
        <div class="ai-msg ai-msg-bot">
          <p class="ai-msg-text">你好，我是 AI 助手。</p>
          <p class="ai-msg-note">
            当前只是界面骨架：后端尚未提供对话接口，所以这里不会给出任何"分析结果"。
          </p>
        </div>

        <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['ai-msg', msg.role === 'user' ? 'ai-msg-user' : 'ai-msg-bot']"
        >
          <p class="ai-msg-text">{{ msg.content }}</p>
        </div>

        <div v-if="!messages.length" class="ai-suggests">
          <div
            v-for="item in suggestions"
            :key="item"
            class="ai-suggest"
            @click="send(item)"
          >
            {{ item }}
          </div>
        </div>
      </div>

      <footer class="ai-footer">
        <el-input
          v-model="input"
          placeholder="输入问题，回车发送"
          @keyup.enter="send()"
        />
        <el-button type="primary" :disabled="!input.trim()" @click="send()">
          发送
        </el-button>
      </footer>
    </div>
  </el-drawer>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { Close } from '@element-plus/icons-vue'

/**
 * AI 助手侧边面板。
 *
 * 交互形态参考金蝶 AI 套件：业务页右上角一个入口，点击从右侧滑出对话面板，
 * 不打断当前页面（不用跳转、不用弹窗遮挡表格）。
 *
 * ⚠ 这里**刻意不伪造 AI 回答**。后端还没有对话接口，任何"看起来像分析结果"的
 *   内容都会让人误以为功能已上线。所以发送后统一返回一句说明。
 *   真正接入时，把 send() 里的 setTimeout 换成 POST /api/ai/chat 即可，
 *   UI 与状态管理都不用动。
 */
const visible = defineModel({ type: Boolean, default: false })

const input = ref('')
const messages = ref([])
const bodyRef = ref()

const suggestions = [
  '本月采购入库金额是多少？',
  '哪些物料低于安全库存？',
  '查看今天待审核的单据'
]

function scrollToBottom() {
  const el = bodyRef.value
  if (el) el.scrollTop = el.scrollHeight
}

function send(text) {
  const content = (text == null ? input.value : text).trim()
  if (!content) return

  messages.value.push({ role: 'user', content })
  input.value = ''
  nextTick(scrollToBottom)

  // 模拟一次网络往返，让消息出现的节奏正常；接入后端后删掉这段
  window.setTimeout(() => {
    messages.value.push({
      role: 'assistant',
      content:
        'AI 服务尚未接入。接入后这里会调用后端对话接口，结合当前单据与库存数据作答。'
    })
    nextTick(scrollToBottom)
  }, 300)
}
</script>

<style scoped>
.ai-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.ai-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-color);
}

.ai-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.ai-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--primary-color);
}

.ai-close {
  font-size: 16px;
  color: #8a94a6;
  cursor: pointer;

  &:hover {
    color: var(--primary-color);
  }
}

.ai-body {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background-color: #f7f8fa;
}

.ai-msg {
  max-width: 88%;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.6;
  border-radius: 8px;
  word-break: break-word;
}

.ai-msg-bot {
  margin-bottom: 10px;
  color: #303133;
  background-color: #ffffff;
  border: 1px solid var(--border-color);
}

.ai-msg-user {
  margin: 0 0 10px auto;
  color: #ffffff;
  background-color: var(--primary-color);
}

.ai-msg-text {
  margin: 0;
}

.ai-msg-note {
  margin: 6px 0 0;
  font-size: 12px;
  color: #8a94a6;
}

.ai-suggests {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 14px;
}

.ai-suggest {
  padding: 9px 12px;
  font-size: 12px;
  color: #4b5563;
  background-color: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;

  &:hover {
    color: var(--primary-color);
    border-color: var(--el-color-primary-light-7);
    background-color: #fdf4f5;
  }
}

.ai-footer {
  flex-shrink: 0;
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--border-color);
}
</style>
