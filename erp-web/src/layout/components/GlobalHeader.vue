<template>
  <header class="global-header">
    <!-- 左：品牌 + 账套/组织 -->
    <div class="gh-left">
      <div class="gh-logo">
        <!-- 菜单入口：悬停/点击展开浮层菜单，不占左侧布局 -->
        <MenuLauncher />
        <span class="gh-logo-mark">E</span>
        <span class="gh-logo-text">ERP 管理系统</span>
      </div>
      <span class="gh-divider" />
      <span class="gh-org">演示账套 · 默认组织</span>
    </div>

    <!-- 右：搜索 / AI / 通知 / 帮助 / 反馈 / 用户 -->
    <div class="gh-right">
      <GlobalSearch />

      <el-tooltip content="AI 助手" placement="bottom">
        <el-icon class="gh-icon gh-ai" @click="aiVisible = true"><MagicStick /></el-icon>
      </el-tooltip>
      <el-tooltip content="消息通知（待接入）" placement="bottom">
        <el-icon class="gh-icon"><Bell /></el-icon>
      </el-tooltip>
      <el-tooltip content="帮助（待接入）" placement="bottom">
        <el-icon class="gh-icon"><QuestionFilled /></el-icon>
      </el-tooltip>

      <button class="gh-feedback" @click="handleFeedback">建议与反馈</button>

      <el-dropdown trigger="click" @command="handleCommand">
        <span class="gh-user">
          <el-avatar :size="26" :src="userStore.avatar">{{ shortName }}</el-avatar>
          <span class="gh-user-name">{{ userStore.nickName }}</span>
          <el-icon><CaretBottom /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="home">
              <el-icon><HomeFilled /></el-icon>
              首页
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <AiAssistant v-model="aiVisible" />
  </header>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell,
  CaretBottom,
  HomeFilled,
  MagicStick,
  QuestionFilled,
  SwitchButton
} from '@element-plus/icons-vue'
import AiAssistant from './AiAssistant.vue'
import GlobalSearch from './GlobalSearch.vue'
import MenuLauncher from './MenuLauncher.vue'
import { useUserStore } from '@/store/modules/user'
import { usePermissionStore } from '@/store/modules/permission'
import { useTagsViewStore } from '@/store/modules/tagsView'
import { useDictStore } from '@/store/modules/dict'

/**
 * 全局顶栏。
 *
 * 布局参考金蝶 AI 套件：白色通栏，左边菜单入口 / 品牌 / 账套组织，
 * 右边搜索框、AI、通知、帮助、建议反馈、用户。
 *
 * 配色是红白主题：白底 + 深色文字，红色只出现在品牌标记（E）、AI 入口和
 * 「建议与反馈」按钮上。不要改回大面积深色底 —— 那会让顶栏和下方白底
 * 侧栏/搜索栏割裂。
 */
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const tagsViewStore = useTagsViewStore()

const aiVisible = ref(false)

const shortName = computed(() => (userStore.nickName || '').slice(0, 1))

function handleFeedback() {
  ElMessage.info('建议与反馈功能尚未接入')
}

async function handleCommand(command) {
  if (command === 'home') {
    router.push('/')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch (e) {
      return
    }
    await userStore.logout()
    // 清掉与「上一个登录用户」相关的所有前端状态，避免换账号后残留
    permissionStore.resetRoutes()
    tagsViewStore.delAllViews()
    useDictStore().clearDict()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.global-header {
  flex-shrink: 0;
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 0 20px;
  color: var(--header-text);
  background-color: var(--header-bg);
  /* 白底顶栏与下方白色搜索栏之间需要一条分界线，否则会糊成一整块 */
  border-bottom: 1px solid var(--border-color);
}

.gh-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.gh-logo {
  display: flex;
  align-items: center;
  gap: 8px;
}

.gh-logo-mark {
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #ffffff;
  background-color: var(--primary-color);
  border-radius: 6px;
}

.gh-logo-text {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}

.gh-divider {
  width: 1px;
  height: 16px;
  background-color: var(--border-color);
}

.gh-org {
  font-size: 13px;
  color: var(--header-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.gh-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.gh-icon {
  font-size: 17px;
  color: var(--header-text-muted);
  cursor: pointer;

  &:hover {
    color: var(--primary-color);
  }
}

/* AI 入口常亮，和通知/帮助区分开 */
.gh-ai {
  color: var(--primary-color);
}

.gh-feedback {
  height: 28px;
  padding: 0 12px;
  font-size: 12px;
  color: var(--primary-color);
  background-color: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-8);
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;

  &:hover {
    background-color: var(--el-color-primary-light-8);
  }
}

.gh-user {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--header-text);
  cursor: pointer;
  outline: none;

  &:hover {
    color: var(--primary-color);
  }
}

.gh-user-name {
  white-space: nowrap;
}
</style>
