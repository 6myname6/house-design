<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="brand">筑梦家</h1>
      <p class="slogan">注册新账号，开始你的家装之旅</p>

      <el-form :model="form" :rules="rules" ref="formRef" size="large" @submit.prevent="onSubmit">
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
          注 册
        </el-button>
      </el-form>

      <p class="switch-line">
        已有账号？
        <router-link to="/login" class="link">去登录</router-link>
      </p>
    </div>
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
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--hd-space-3);
  background: linear-gradient(160deg, var(--hd-primary-100) 0%, var(--hd-primary-50) 55%, var(--hd-neutral-50) 100%);
}

.auth-card {
  width: 100%;
  max-width: 400px;
  padding: var(--hd-space-5) var(--hd-space-4);
  background: #fff;
  border-radius: var(--hd-radius-lg);
  box-shadow: 0 8px 24px rgba(92, 42, 14, 0.08);
}

.brand {
  margin: 0;
  text-align: center;
  font-size: var(--hd-text-display);
  line-height: var(--hd-text-display-line);
  font-weight: var(--hd-font-weight-medium);
  color: var(--hd-primary-700);
}

.slogan {
  margin: var(--hd-space-1) 0 var(--hd-space-4);
  text-align: center;
  color: var(--hd-neutral-500);
  font-size: var(--hd-text-caption);
}

.submit {
  width: 100%;
  margin-top: var(--hd-space-1);
}

.switch-line {
  margin-top: var(--hd-space-3);
  text-align: center;
  color: var(--hd-neutral-500);
  font-size: var(--hd-text-caption);
}

.link {
  color: var(--hd-primary-700);
  text-decoration: none;
  font-weight: var(--hd-font-weight-medium);
}
</style>