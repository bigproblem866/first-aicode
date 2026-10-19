<template>
  <div class="login-page">
    <!-- 左：品牌区 -->
    <aside class="login-brand">
      <div class="brand-inner">
        <div class="brand-logo">E</div>
        <h1 class="brand-title">ERP 管理系统</h1>
        <p class="brand-desc">主数据 · 库存 · 采购，一套系统管到底</p>
      </div>
    </aside>

    <!-- 右：登录表单 -->
    <main class="login-main">
      <div class="login-box">
        <h2 class="login-title">登录</h2>
        <p class="login-subtitle">请使用系统账号登录</p>

        <el-form
          ref="formRef"
          :model="loginForm"
          :rules="rules"
          size="large"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input v-model="loginForm.username" placeholder="用户名" :prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="login-button" :loading="loading" @click="handleLogin">
              登 录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/modules/user'

/**
 * 登录页。
 *
 * 表单校验通过后调 store.login（内部走 POST /api/auth/login），
 * 成功即 router.replace 到 redirect 指的页面 —— 这里刻意不自己去 addRoute，
 * 动态路由统一交给 permission.js 的全局守卫处理，避免两条路径各写一份。
 *
 * 布局是左右分栏：左边品牌区（红底），右边表单区（白底）。
 * 原来的做法是「深色渐变背景 + 居中卡片」，应用一多会显得像通用模板。
 */
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)

const loginForm = reactive({
  // 种子数据里的默认管理员账号（admin / 123456），开发时省得每次手敲
  username: 'admin',
  password: '123456'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  loading.value = true
  try {
    await userStore.login(loginForm)
    const redirect = route.query.redirect
    const target = redirect ? decodeURIComponent(redirect) : '/'
    await router.replace(target)
  } catch (error) {
    // 账号密码错误等业务失败：提示已由 axios 响应拦截器统一弹出（ElMessage.error(res.msg)）
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  height: 100%;
  background-color: #ffffff;
}

/* ---------- 左侧品牌区 ---------- */
.login-brand {
  flex: 0 0 46%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: linear-gradient(160deg, #7a0010 0%, #b30016 55%, #d9001b 100%);
}

.brand-inner {
  max-width: 380px;
  color: #ffffff;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  margin-bottom: 28px;
  font-size: 26px;
  font-weight: 700;
  color: #d9001b;
  background-color: #ffffff;
  border-radius: 12px;
}

.brand-title {
  margin: 0 0 12px;
  font-size: 30px;
  font-weight: 600;
  line-height: 1.3;
}

.brand-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.78);
}

/* ---------- 右侧表单区 ---------- */
.login-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.login-box {
  width: 360px;
  max-width: 100%;
}

.login-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
}

.login-subtitle {
  margin: 8px 0 28px;
  font-size: 13px;
  color: #8a94a6;
}

.login-button {
  width: 100%;
}

/* 窄屏时隐藏品牌区，避免表单被挤到不可用 */
@media (max-width: 900px) {
  .login-brand {
    display: none;
  }
}
</style>
