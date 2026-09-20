<template>
  <div class="auth">
    <!-- 左：编辑宣言区 -->
    <section class="auth-stage">
      <div class="stage-inner">
        <p class="hd-overline hd-rise" style="animation-delay: 0.05s">Est. 2026 · Interior Preview</p>
        <h1 class="stage-title hd-rise" style="animation-delay: 0.15s">
          筑梦家
        </h1>
        <div class="stage-rule hd-rise" style="animation-delay: 0.25s"></div>
        <p class="stage-lead hd-rise" style="animation-delay: 0.32s">
          上传一张户型图，<br />让未来的家先于施工到来。
        </p>
        <p class="stage-note hd-rise" style="animation-delay: 0.42s">
          AI 写实效果图 · 五种主流风格 · 设计助手在线答疑
        </p>
      </div>
      <span class="stage-index" aria-hidden="true">01</span>
    </section>

    <!-- 右：表单区 -->
    <section class="auth-panel">
      <div class="panel-inner hd-rise" style="animation-delay: 0.2s">
        <p class="hd-overline">Sign in</p>
        <h2 class="panel-title">登 录</h2>

        <el-form :model="form" :rules="rules" ref="formRef" size="large" class="auth-form" @submit.prevent="onSubmit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" maxlength="32" autocomplete="username" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              maxlength="32"
              show-password
              autocomplete="current-password"
              @keyup.enter="onSubmit"
            />
          </el-form-item>
          <el-button type="primary" native-type="submit" class="submit" :loading="loading" @click="onSubmit">
            进 入
          </el-button>
        </el-form>

        <p class="switch-line">
          还没有账号？
          <router-link to="/register" class="link">前往注册 →</router-link>
        </p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const formRef = ref(null)

const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/projects')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.15fr 1fr;
}

/* ---- 左：宣言舞台 ---- */
.auth-stage {
  position: relative;
  background: var(--hd-ink);
  color: #f3ede2;
  padding: var(--hd-space-6);
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}
/* 纸张般的细微纹理（纯 CSS，零图片） */
.auth-stage::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(rgba(243, 237, 226, 0.05) 1px, transparent 1px);
  background-size: 22px 22px;
  pointer-events: none;
}
.stage-inner {
  position: relative;
  z-index: 1;
  max-width: 460px;
}
.auth-stage .hd-overline { color: rgba(243, 237, 226, 0.55); }

.stage-title {
  margin-top: var(--hd-space-2);
  font-size: clamp(56px, 8vw, 96px);
  line-height: 1.05;
  letter-spacing: 0.12em;
  font-weight: 500;
}
.stage-rule {
  width: 64px;
  height: 2px;
  margin: var(--hd-space-3) 0;
  background: var(--hd-primary-400);
  transform-origin: left;
  animation: hd-line 0.6s 0.4s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}
.stage-lead {
  font-family: var(--hd-font-display);
  font-size: 22px;
  line-height: 1.7;
  letter-spacing: 0.04em;
  color: rgba(243, 237, 226, 0.92);
}
.stage-note {
  margin-top: var(--hd-space-2);
  font-size: var(--hd-text-caption);
  letter-spacing: 0.08em;
  color: rgba(243, 237, 226, 0.5);
}
.stage-index {
  position: absolute;
  right: var(--hd-space-5);
  bottom: var(--hd-space-4);
  font-family: var(--hd-font-mono);
  font-size: 13px;
  letter-spacing: 0.3em;
  color: rgba(243, 237, 226, 0.35);
}

/* ---- 右：表单 ---- */
.auth-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--hd-space-4);
  background: var(--hd-paper);
}
.panel-inner {
  width: 100%;
  max-width: 360px;
}
.panel-title {
  margin: 10px 0 var(--hd-space-4);
  font-size: 32px;
  letter-spacing: 0.2em;
  color: var(--hd-ink);
}
.auth-form :deep(.el-form-item) { margin-bottom: 22px; }

.submit {
  width: 100%;
  margin-top: 6px;
  height: 48px;
  font-size: 15px;
  letter-spacing: 0.3em;
  background: var(--hd-ink);
  border-color: var(--hd-ink);
}
.submit:hover,
.submit:focus {
  background: var(--hd-primary-700);
  border-color: var(--hd-primary-700);
}

.switch-line {
  margin-top: var(--hd-space-4);
  padding-top: var(--hd-space-2);
  border-top: var(--hd-hairline);
  font-size: var(--hd-text-caption);
  color: var(--hd-neutral-500);
  text-align: center;
}
.link {
  color: var(--hd-primary-700);
  text-decoration: none;
  font-weight: var(--hd-font-weight-medium);
  letter-spacing: 0.05em;
}
.link:hover { text-decoration: underline; text-underline-offset: 4px; }

/* ---- 窄屏：隐藏舞台，只留表单 ---- */
@media (max-width: 860px) {
  .auth { grid-template-columns: 1fr; }
  .auth-stage { display: none; }
}
</style>
