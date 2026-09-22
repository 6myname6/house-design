<template>
  <div class="auth">
    <!-- 左：编辑宣言区 -->
    <section class="auth-stage">
      <div class="stage-inner">
        <p class="hd-overline hd-rise" style="animation-delay: 0.05s">Est. 2026 · Join the Studio</p>
        <h1 class="stage-title hd-rise" style="animation-delay: 0.15s">
          筑梦家
        </h1>
        <div class="stage-rule hd-rise" style="animation-delay: 0.25s"></div>
        <p class="stage-lead hd-rise" style="animation-delay: 0.32s">
          从一张户型图开始，<br />把新家提前请进生活。
        </p>
        <p class="stage-note hd-rise" style="animation-delay: 0.42s">
          AI 写实效果图 · 五种主流风格 · 设计助手在线答疑
        </p>
      </div>
      <span class="stage-index" aria-hidden="true">02</span>
    </section>

    <!-- 右：表单区 -->
    <section class="auth-panel">
      <div class="panel-inner hd-rise" style="animation-delay: 0.2s">
        <p class="hd-overline">Sign up</p>
        <h2 class="panel-title">注 册</h2>

        <el-form :model="form" :rules="rules" ref="formRef" size="large" class="auth-form" @submit.prevent="onSubmit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名（3-32 个字符）" maxlength="32" autocomplete="username" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码（8-32 个字符）"
              maxlength="32"
              show-password
              autocomplete="new-password"
            />
          </el-form-item>
          <el-form-item prop="confirm">
            <el-input
              v-model="form.confirm"
              type="password"
              placeholder="确认密码"
              maxlength="32"
              show-password
              autocomplete="new-password"
            />
          </el-form-item>
          <el-button type="primary" native-type="submit" class="submit" :loading="loading" @click="onSubmit">
            开 始
          </el-button>
        </el-form>

        <p class="switch-line">
          已有账号？
          <router-link to="/login" class="link">前往登录 →</router-link>
        </p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const formRef = ref(null)

const form = reactive({ username: '', password: '', confirm: '' })

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度 3-32 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度 8-32 个字符', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_r, v, cb) => (v === form.password ? cb() : cb(new Error('两次输入的密码不一致'))),
      trigger: 'blur'
    }
  ]
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await userStore.register(form.username, form.password)
    ElMessage.success('注册成功，已自动登录')
    router.push('/projects')
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
  color: var(--hd-on-ink);
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
    radial-gradient(color-mix(in srgb, var(--hd-on-ink) 5%, transparent) 1px, transparent 1px);
  background-size: 22px 22px;
  pointer-events: none;
}
.stage-inner {
  position: relative;
  z-index: 1;
  max-width: 460px;
}
.auth-stage .hd-overline { color: color-mix(in srgb, var(--hd-on-ink) 55%, transparent); }

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
  color: color-mix(in srgb, var(--hd-on-ink) 92%, transparent);
}
.stage-note {
  margin-top: var(--hd-space-2);
  font-size: var(--hd-text-caption);
  letter-spacing: 0.08em;
  color: color-mix(in srgb, var(--hd-on-ink) 50%, transparent);
}
.stage-index {
  position: absolute;
  right: var(--hd-space-5);
  bottom: var(--hd-space-4);
  font-family: var(--hd-font-mono);
  font-size: 13px;
  letter-spacing: 0.3em;
  color: color-mix(in srgb, var(--hd-on-ink) 35%, transparent);
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
