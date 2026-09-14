<template>
  <div class="create-page">
    <el-page-header class="page-header" @back="goBack">
      <template #content>
        <span class="title">新建项目</span>
      </template>
    </el-page-header>

    <el-steps :active="step" align-center class="steps">
      <el-step title="设计图" />
      <el-step title="选风格" />
      <el-step title="信息" />
    </el-steps>

    <!-- 步骤 1：上传设计图 -->
    <section v-show="step === 0" class="step-body">
      <input
        ref="fileInputRef"
        id="upload-file"
        type="file"
        class="file-input"
        accept=".jpg,.jpeg,.png,.gif,.webp"
        @change="onFileChange"
      />
      <label v-if="!previewUrl" class="upload-zone" for="upload-file">
        <span class="upload-icon" aria-hidden="true">🖼️</span>
        <span class="upload-main">点击上传设计图</span>
        <span class="upload-sub">支持户型图 / CAD / 手绘，jpg、png 等格式</span>
      </label>
      <div v-else class="preview-wrap">
        <img :src="previewUrl" class="preview" alt="设计图预览" />
        <label class="re-upload" for="upload-file">
          重新选择
        </label>
      </div>
      <el-button type="primary" class="next-btn" :disabled="!form.designFile" @click="next">
        下一步
      </el-button>
    </section>

    <!-- 步骤 2：选风格 -->
    <section v-show="step === 1" class="step-body">
      <div class="style-grid" role="radiogroup" aria-label="选择风格">
        <button
          v-for="(s, i) in styles"
          :key="s.code"
          type="button"
          class="style-card"
          :class="{ selected: form.styleLabel === s.code }"
          :style="{ background: STYLE_GRADIENTS[i % STYLE_GRADIENTS.length] }"
          role="radio"
          :aria-checked="form.styleLabel === s.code"
          @click="selectStyle(s.code)"
        >
          <span class="check" aria-hidden="true">✓</span>
          <span class="style-label">{{ s.label }}</span>
        </button>
      </div>

      <button type="button" class="custom-toggle" @click="customOpen = !customOpen">
        <span>或填写自定义风格要求</span>
        <span class="arrow" :class="{ open: customOpen }" aria-hidden="true">▾</span>
      </button>
      <el-input
        v-show="customOpen"
        v-model="form.style"
        type="textarea"
        :rows="3"
        maxlength="200"
        show-word-limit
        placeholder="如：原木色家具、温馨、多绿植"
        class="custom-input"
      />

      <p class="hint" :class="{ ok: isValidStyle }">
        {{ hintText }}
      </p>

      <div class="btn-row">
        <el-button @click="step = 0">上一步</el-button>
        <el-button type="primary" :disabled="!isValidStyle" @click="next">下一步</el-button>
      </div>
    </section>

    <!-- 步骤 3：名称/描述 + 提交 -->
    <section v-show="step === 2" class="step-body">
      <el-form :model="form" label-position="top">
        <el-form-item label="项目名称" required>
          <el-input v-model="form.name" placeholder="如：我的温馨小家" maxlength="50" />
        </el-form-item>
        <el-form-item label="项目描述（可选）">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="如：朝阳 120㎡ 三居室" />
        </el-form-item>
      </el-form>

      <div class="btn-row">
        <el-button @click="step = 1">上一步</el-button>
        <el-button type="primary" :loading="creating" :disabled="!form.name.trim()" @click="onSubmit">
          {{ creating ? '上传中…' : '提交' }}
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createProject } from '../api/project'
import { fetchStyles } from '../api/style'

const router = useRouter()

const step = ref(0)
const customOpen = ref(false)
const creating = ref(false)
const styles = ref([])
const previewUrl = ref(null)

const form = reactive({
  name: '',
  description: '',
  styleLabel: '',
  style: '',
  designFile: null
})

// 5 个预设风格的主题色渐变（与 DesignStyle 顺序对应，纯视觉小样）
const STYLE_GRADIENTS = [
  'linear-gradient(135deg, #E3E0D9 0%, #C9C5BB 100%)',
  'linear-gradient(135deg, #FAE0D3 0%, #F3BFA4 100%)',
  'linear-gradient(135deg, #EDE6D8 0%, #D6C7A8 100%)',
  'linear-gradient(135deg, #DCE0E0 0%, #A8B4B4 100%)',
  'linear-gradient(135deg, #F1EFEA 0%, #C9A878 100%)'
]

// 风格校验：预设标签或自定义描述至少填一个
const isValidStyle = computed(() => form.styleLabel || form.style.trim())
const hintText = computed(() =>
  isValidStyle.value ? '风格已设置，可进入下一步' : '请至少选择一种风格，或填写自定义要求'
)

onMounted(async () => {
  step.value = 0
  try {
    styles.value = await fetchStyles()
  } catch {
    // 风格拉取失败不阻塞步骤 2，用户仍可填自定义要求
  }
})

function onFileChange(e) {
  const file = e.target.files?.[0]
  const ALLOWED = ['jpg', 'jpeg', 'png', 'gif', 'webp']
  if (!file) return
  const ext = (file.name.split('.').pop() || '').toLowerCase()
  if (!ALLOWED.includes(ext)) {
    ElMessage.error('仅支持 jpg/jpeg/png/gif/webp 格式')
    e.target.value = ''
    return
  }
  form.designFile = file
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = URL.createObjectURL(file)
}

function selectStyle(code) {
  // 再次点击同一张卡 = 取消选择
  form.styleLabel = form.styleLabel === code ? '' : code
}

function next() {
  if (step.value === 0 && !form.designFile) {
    return ElMessage.warning('请先上传设计图')
  }
  if (step.value === 1 && !isValidStyle.value) {
    return ElMessage.warning(hintText.value)
  }
  step.value += 1
}

async function onSubmit() {
  if (!form.name.trim()) {
    return ElMessage.warning('请填写项目名称')
  }
  creating.value = true
  try {
    const fd = new FormData()
    fd.append('name', form.name.trim())
    fd.append('designImage', form.designFile)
    if (form.description.trim()) fd.append('description', form.description.trim())
    if (form.styleLabel) fd.append('styleLabel', form.styleLabel)
    if (form.style.trim()) fd.append('style', form.style.trim())
    await createProject(fd)
    ElMessage.success('项目创建成功')
    router.push('/projects')
  } finally {
    creating.value = false
  }
}

function goBack() {
  router.push('/projects')
}
</script>

<style scoped>
.create-page {
  max-width: var(--hd-content-max);
  margin: 0 auto;
  padding: var(--hd-space-2) var(--hd-space-2) calc(var(--hd-space-6));
}
.page-header {
  margin-bottom: var(--hd-space-2);
}
.title {
  font-size: var(--hd-text-h3);
  font-weight: var(--hd-font-weight-medium);
}
.steps {
  margin-bottom: var(--hd-space-3);
}

.step-body {
  min-height: 320px;
}

/* 步骤 1：上传区 */
.file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  overflow: hidden;
}
.upload-zone,
.preview-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--hd-space-1);
  min-height: 260px;
  border: 2px dashed var(--hd-primary-200);
  border-radius: var(--hd-radius-lg);
  background: var(--hd-primary-50);
  cursor: pointer;
  text-align: center;
  padding: var(--hd-space-3);
}
.upload-icon {
  font-size: 40px;
  line-height: 1;
}
.upload-main {
  font-size: var(--hd-text-h3);
  font-weight: var(--hd-font-weight-medium);
  color: var(--hd-primary-700);
}
.upload-sub {
  font-size: var(--hd-text-caption);
  color: var(--hd-neutral-500);
}
.preview {
  width: 100%;
  max-height: 320px;
  object-fit: contain;
  border-radius: var(--hd-radius-base);
}
.re-upload {
  margin-top: var(--hd-space-1);
  font-size: var(--hd-text-caption);
  color: var(--hd-primary-700);
  text-decoration: underline;
  cursor: pointer;
}
.next-btn {
  width: 100%;
  margin-top: var(--hd-space-3);
}

/* 步骤 2：风格卡片 */
.style-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: var(--hd-space-2);
}
.style-card {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 72px;
  border: 2px solid transparent;
  border-radius: var(--hd-radius-lg);
  cursor: pointer;
  transition: transform var(--hd-duration-fast) ease-out, border-color var(--hd-duration-fast) ease-out;
}
.style-card:hover {
  transform: scale(1.03);
}
.style-card.selected {
  border-color: var(--hd-primary-700);
  box-shadow: 0 0 0 2px var(--hd-primary-50);
}
.style-card .check {
  position: absolute;
  top: 6px;
  right: 8px;
  color: var(--hd-primary-700);
  font-weight: var(--hd-font-weight-medium);
  opacity: 0;
}
.style-card.selected .check {
  opacity: 1;
}
.style-label {
  font-size: var(--hd-text-caption);
  color: var(--hd-neutral-800);
}

.custom-toggle {
  margin-top: var(--hd-space-3);
  display: flex;
  align-items: center;
  gap: var(--hd-space-1);
  border: none;
  background: none;
  color: var(--hd-primary-700);
  font-size: var(--hd-text-caption);
  cursor: pointer;
  min-height: 44px;
}
.arrow {
  transition: transform var(--hd-duration-fast) ease-out;
}
.arrow.open {
  transform: rotate(180deg);
}
.custom-input {
  margin-top: var(--hd-space-1);
}

.hint {
  margin-top: var(--hd-space-2);
  font-size: var(--hd-text-caption);
  color: var(--hd-warning);
}
.hint.ok {
  color: var(--hd-success);
}

.btn-row {
  margin-top: var(--hd-space-4);
  display: flex;
  gap: var(--hd-space-2);
}
.btn-row .el-button {
  flex: 1;
}
</style>