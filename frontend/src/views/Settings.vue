<template>
  <div class="page">
    <header class="page-header">
      <h2 class="title">账号设置</h2>
    </header>

    <!-- 联系方式：邮箱（可绑定/换绑） -->
    <section class="contact-card">
      <h3 class="section-title">联系方式</h3>
      <p class="section-note">邮箱用于接收验证码登录、修改密码等账号安全通知。</p>

      <div class="contact-row">
        <div class="contact-info">
          <span class="contact-label">电子邮箱</span>
          <span :class="['contact-value', { muted: !userStore.userInfo?.maskedEmail }]">
            {{ userStore.userInfo?.maskedEmail || '未绑定' }}
          </span>
        </div>
        <el-button plain @click="openBindDialog">
          {{ userStore.userInfo?.maskedEmail ? '更换邮箱' : '绑定邮箱' }}
        </el-button>
      </div>
    </section>

    <!-- 修改密码 -->
    <section class="pwd-card">
      <h3 class="section-title">修改密码</h3>
      <p class="section-note">通过绑定的邮箱接收验证码以验证身份，改密成功后需用新密码重新登录。</p>

      <el-form
        v-if="hasChannel"
        :model="form"
        :rules="rules"
        ref="formRef"
        label-position="top"
        @submit.prevent="onSubmit"
      >
        <el-form-item v-if="userStore.userInfo?.maskedEmail" label="验证方式">
          <span class="current-email">邮箱（{{ userStore.userInfo.maskedEmail }}）</span>
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input
              v-model="form.code"
              placeholder="6 位验证码"
              maxlength="6"
              inputmode="numeric"
            />
            <el-button
              class="code-btn"
              :disabled="countdown > 0 || codeSending"
              :loading="codeSending"
              @click="onSendCode"
            >
              {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="8-32 个字符"
            maxlength="32"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirm">
          <el-input
            v-model="form.confirm"
            type="password"
            placeholder="再次输入新密码"
            maxlength="32"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>

        <el-button type="primary" native-type="submit" class="submit" :loading="submitting">
          确认修改
        </el-button>
      </el-form>

      <el-alert
        v-else
        title="当前账号未绑定邮箱，无法自助修改密码，请先绑定邮箱。"
        type="warning"
        :closable="false"
        show-icon
      />
    </section>

    <!-- 绑定/更换邮箱弹窗 -->
    <el-dialog
      v-model="bindVisible"
      :title="userStore.userInfo?.maskedEmail ? '更换邮箱' : '绑定邮箱'"
      width="min(420px, 90%)"
    >
      <el-form :model="bindForm" :rules="bindRules" ref="bindFormRef" label-position="top" @submit.prevent>
        <el-form-item v-if="userStore.userInfo?.maskedEmail" label="当前邮箱">
          <span class="current-email">{{ userStore.userInfo.maskedEmail }}</span>
        </el-form-item>
        <el-form-item label="新邮箱" prop="email">
          <el-input v-model="bindForm.email" placeholder="请输入新邮箱地址" maxlength="128" autocomplete="email" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input
              v-model="bindForm.code"
              placeholder="6 位验证码"
              maxlength="6"
              inputmode="numeric"
            />
            <el-button
              class="code-btn"
              :disabled="bindCountdown > 0 || bindCodeSending"
              :loading="bindCodeSending"
              @click="onSendBindCode"
            >
              {{ bindCountdown > 0 ? `${bindCountdown}s 后重发` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="binding" @click="onConfirmBind">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import {
  sendPasswordCodeApi,
  changePasswordApi,
  sendEmailBindCodeApi,
  bindEmailApi
} from '../api/auth'

const router = useRouter()
const userStore = useUserStore()

const EMAIL_PATTERN = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

const formRef = ref(null)
const form = reactive({
  code: '',
  newPassword: '',
  confirm: ''
})

const hasChannel = computed(() => !!userStore.userInfo?.maskedEmail)

const rules = {
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入 6 位数字验证码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度 8-32 个字符', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_r, v, cb) => (v === form.newPassword ? cb() : cb(new Error('两次输入的密码不一致'))),
      trigger: 'blur'
    }
  ]
}

// ---- 改密发码倒计时 ----
const countdown = ref(0)
const codeSending = ref(false)
let countdownTimer = null

onMounted(async () => {
  // 进入页面拉一次最新信息（含掩码邮箱），决定改密卡片可用性
  await userStore.fetchMe()
})

async function onSendCode() {
  if (codeSending.value || countdown.value > 0) return
  codeSending.value = true
  try {
    await sendPasswordCodeApi({ channel: 'email' })
    ElMessage.success('验证码已发送，请查收邮箱')
    countdown.value = 60
    countdownTimer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }, 1000)
  } catch {
    // 频控/错误提示由请求拦截器统一弹出
  } finally {
    codeSending.value = false
  }
}

const submitting = ref(false)
async function onSubmit() {
  if (submitting.value) return
  submitting.value = true
  try {
    await formRef.value.validate()
    await changePasswordApi({
      channel: 'email',
      code: form.code,
      newPassword: form.newPassword
    })
    ElMessage.success('密码修改成功，请使用新密码重新登录')
    // 当前 token 已被服务端拉黑：只清本地状态（不再调 /logout），跳登录页
    userStore.clearSession()
    router.push('/login')
  } catch {
    // 校验失败或请求失败：拦截器已统一提示
  } finally {
    submitting.value = false
  }
}

// ---- 绑定/更换邮箱 ----
const bindVisible = ref(false)
const bindFormRef = ref(null)
const bindForm = reactive({ email: '', code: '' })
const bindRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: EMAIL_PATTERN, message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入 6 位数字验证码', trigger: 'blur' }
  ]
}
const bindCountdown = ref(0)
const bindCodeSending = ref(false)
const binding = ref(false)
let bindCountdownTimer = null

function openBindDialog() {
  bindForm.email = ''
  bindForm.code = ''
  bindCountdown.value = 0
  bindVisible.value = true
}

async function onSendBindCode() {
  if (bindCodeSending.value || bindCountdown.value > 0) return
  try {
    await bindFormRef.value.validateField('email')
  } catch {
    return
  }
  bindCodeSending.value = true
  try {
    await sendEmailBindCodeApi({ email: bindForm.email })
    ElMessage.success('验证码已发送，请查收邮箱')
    bindCountdown.value = 60
    bindCountdownTimer = setInterval(() => {
      bindCountdown.value -= 1
      if (bindCountdown.value <= 0) {
        clearInterval(bindCountdownTimer)
        bindCountdownTimer = null
      }
    }, 1000)
  } catch {
    // 占用/频控等错误由拦截器统一提示，不启动倒计时
  } finally {
    bindCodeSending.value = false
  }
}

async function onConfirmBind() {
  if (binding.value) return
  binding.value = true
  try {
    await bindFormRef.value.validate()
    const message = await bindEmailApi({ email: bindForm.email, code: bindForm.code })
    ElMessage.success(message || '操作成功')
    bindVisible.value = false
    // 刷新掩码，改密卡片随之可用
    await userStore.fetchMe()
  } catch {
    // 拦截器已统一提示
  } finally {
    binding.value = false
  }
}

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
  if (bindCountdownTimer) clearInterval(bindCountdownTimer)
})
</script>

<style scoped>
.page {
  max-width: var(--hd-content-max);
  margin: 0 auto;
  padding: var(--hd-space-3) var(--hd-space-2) var(--hd-space-4);
}
.page-header {
  padding: var(--hd-space-2) 0;
}
.title {
  margin: 0;
  font-size: var(--hd-text-h2);
  line-height: var(--hd-text-h2-line);
  font-weight: var(--hd-font-weight-medium);
}

.contact-card,
.pwd-card {
  max-width: 560px;
  margin-top: var(--hd-space-3);
  padding: var(--hd-space-4);
  background: var(--hd-paper);
  border: 1px solid var(--hd-neutral-200);
}
.section-title {
  margin: 0 0 var(--hd-space-1);
  font-size: 18px;
  font-weight: var(--hd-font-weight-medium);
  color: var(--hd-ink);
}
.section-note {
  margin: 0 0 var(--hd-space-3);
  font-size: var(--hd-text-caption);
  line-height: 1.7;
  color: var(--hd-neutral-500);
}

/* 联系方式行 */
.contact-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--hd-space-2);
  padding: var(--hd-space-2) 0;
  border-bottom: var(--hd-hairline);
}
.contact-row:last-child {
  border-bottom: none;
}
.contact-info {
  display: flex;
  align-items: baseline;
  gap: var(--hd-space-3);
  min-width: 0;
}
.contact-label {
  flex-shrink: 0;
  width: 64px;
  font-size: var(--hd-text-caption);
  letter-spacing: 0.1em;
  color: var(--hd-neutral-500);
}
.contact-value {
  font-size: var(--hd-text-body);
  color: var(--hd-ink);
  letter-spacing: 0.03em;
}
.contact-value.muted {
  color: var(--hd-neutral-400);
}
.current-email {
  color: var(--hd-neutral-500);
  font-size: var(--hd-text-caption);
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}
.code-btn {
  flex-shrink: 0;
  min-width: 108px;
}
.submit {
  width: 100%;
  margin-top: var(--hd-space-1);
  background: var(--hd-ink);
  border-color: var(--hd-ink);
  letter-spacing: 0.2em;
}
.submit:hover,
.submit:focus {
  background: var(--hd-primary-700);
  border-color: var(--hd-primary-700);
}
</style>
