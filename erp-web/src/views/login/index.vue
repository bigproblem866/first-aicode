<template>
  <div class="login-page">
    <!-- 左：品牌区 -->
    <aside class="login-brand">
      <div class="brand-inner">
        <div class="brand-logo">爱</div>
        <p class="brand-eyebrow">AIYONG ERP</p>
        <h1 class="brand-title">爱用</h1>
        <div class="brand-rule"></div>
        <h2 class="brand-desc">一套系统管到底</h2>
      </div>
      <p class="brand-foot">© 2026 爱用 · ERP 管理系统</p>
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
  position: relative;
  flex: 0 0 46%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  overflow: hidden;
  background-color: #b30016;
  background-image:
    radial-gradient(circle at 18% 22%, rgba(255, 255, 255, 0.14), transparent 42%),
    radial-gradient(circle at 84% 80%, rgba(255, 255, 255, 0.1), transparent 46%),
    linear-gradient(155deg, #7a0010 0%, #b30016 55%, #d9001b 100%);
}

/* 装饰圆环：填补大面积留白，同时不抢文字注意力 */
.login-brand::before,
.login-brand::after {
  content: '';
  position: absolute;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 50%;
  pointer-events: none;
}

.login-brand::before {
  top: -190px;
  right: -170px;
  width: 520px;
  height: 520px;
}

.login-brand::after {
  bottom: -140px;
  left: -110px;
  width: 340px;
  height: 340px;
  border-color: rgba(255, 255, 255, 0.09);
}

.brand-inner {
  position: relative;
  z-index: 1;
  max-width: 400px;
  color: #ffffff;
  /* 各行字数不同，居中对齐比强行左对齐更自然 */
  text-align: center;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin: 0 auto 32px;
  font-size: 24px;
  font-weight: 700;
  color: #d9001b;
  background-color: #ffffff;
  border-radius: 14px;
  box-shadow: 0 12px 28px rgba(60, 0, 8, 0.28);
}

.brand-eyebrow {
  margin: 0 0 10px;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.3em;
  color: rgba(255, 255, 255, 0.58);
}

.brand-title {
  margin: 0;
  font-size: 44px;
  font-weight: 600;
  line-height: 1.15;
  letter-spacing: 0.12em;
}

.brand-rule {
  width: 40px;
  height: 2px;
  margin: 22px auto 16px;
  background-color: rgba(255, 255, 255, 0.85);
  border-radius: 2px;
}

.brand-desc {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  letter-spacing: 0.06em;
  color: rgba(255, 255, 255, 0.78);
}

.brand-foot {
  position: absolute;
  bottom: 32px;
  left: 50%;
  z-index: 1;
  transform: translateX(-50%);
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: rgba(255, 255, 255, 0.45);
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
