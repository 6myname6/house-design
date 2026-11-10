<template>
  <div class="page">
    <!-- 页头 -->
    <header class="page-head hd-rise">
      <div class="head-text">
        <p class="hd-overline">04 — Knowledge</p>
        <h2 class="head-title">知识库</h2>
        <p class="head-desc">上传资料后自动解析、切块与向量化，状态变为「可检索」即可被 AI 问答引用</p>
      </div>
      <button class="new-btn" type="button" @click="toggleForm">
        <span>{{ formOpen ? '收起录入' : '新增文档' }}</span>
        <span class="new-arrow" aria-hidden="true">→</span>
      </button>
    </header>

    <!-- 录入区：文件与文本二选一，与后端校验口径一致 -->
    <section v-if="formOpen" class="compose hd-rise">
      <div class="compose-tabs" role="tablist" aria-label="录入方式">
        <button
          type="button"
          class="compose-tab"
          :class="{ active: mode === 'file' }"
          role="tab"
          :aria-selected="mode === 'file'"
          @click="mode = 'file'"
        >
          文件上传
        </button>
        <button
          type="button"
          class="compose-tab"
          :class="{ active: mode === 'text' }"
          role="tab"
          :aria-selected="mode === 'text'"
          @click="mode = 'text'"
        >
          文本录入
        </button>
      </div>

      <div class="compose-body">
        <!-- 文件方式 -->
        <div
          v-if="mode === 'file'"
          class="drop"
          role="button"
          tabindex="0"
          @click="pickFile"
          @keydown.enter="pickFile"
          @dragover.prevent
          @drop.prevent="onDrop"
        >
          <input
            ref="fileInput"
            class="file-input"
            type="file"
            accept=".pdf,.doc,.docx,.md,.markdown,.txt,.html,.htm"
            @change="onFileChange"
          />
          <template v-if="file">
            <p class="drop-title">{{ file.name }}</p>
            <p class="drop-meta">{{ fileSizeText }} — 点击重新选择</p>
          </template>
          <template v-else>
            <p class="drop-title">点击或拖拽文档到此处</p>
            <p class="drop-meta">尚未选择文件</p>
          </template>
          <p class="drop-hint">支持 PDF / Word / Markdown / TXT / HTML，单个不超过 20MB</p>
        </div>

        <!-- 文本方式 -->
        <textarea
          v-else
          v-model="text"
          class="compose-text"
          maxlength="100000"
          placeholder="粘贴需要入库的正文，最长 10 万字符…"
        ></textarea>

        <div class="compose-fields">
          <label class="field">
            <span class="field-label">标题</span>
            <input v-model="title" class="field-input" maxlength="255" placeholder="不填则由文件名或默认值补齐" />
          </label>
          <label class="field">
            <span class="field-label">可见范围</span>
            <select v-model="visibility" class="field-input">
              <option value="PUBLIC">公开 — 所有用户可检索</option>
              <option value="PRIVATE">私有 — 仅自己可检索</option>
            </select>
          </label>
        </div>

        <div class="compose-foot">
          <p class="compose-tip">提交为异步入库：排队 → 解析 → 向量化 → 可检索，失败原因会显示在列表中</p>
          <button class="ink-btn" type="button" :disabled="submitting" @click="submit">
            {{ submitting ? '提交中…' : '开始入库' }}
          </button>
        </div>
      </div>
    </section>

    <!-- 筛选与计数 -->
    <div class="toolbar">
      <div class="filters">
        <button
          v-for="f in statusFilters"
          :key="f.value"
          type="button"
          class="chip"
          :class="{ active: statusFilter === f.value }"
          @click="setFilter(f.value)"
        >
          {{ f.label }}
        </button>
      </div>
      <p class="toolbar-count">共 <span class="mono">{{ total }}</span> 篇</p>
    </div>

    <!-- 加载骨架 -->
    <div v-if="loading" class="doc-list">
      <div v-for="i in 3" :key="i" class="doc skeleton-doc">
        <div class="doc-main">
          <div class="sk-line sk-w30"></div>
          <div class="sk-line sk-w60"></div>
        </div>
      </div>
    </div>

    <!-- 错误态 -->
    <div v-else-if="loadError" class="state-box">
      <p class="state-title">知识库加载失败</p>
      <button class="text-btn" type="button" @click="loadList()">重新加载 →</button>
    </div>

    <!-- 空态 -->
    <div v-else-if="documents.length === 0" class="state-box hd-rise">
      <p class="hd-overline">Empty</p>
      <p class="state-title">还没有文档</p>
      <p class="state-desc">上传装修资料或直接录入文本，AI 就能基于它回答并给出出处</p>
      <button class="ink-btn" type="button" @click="formOpen = true">录入第一篇</button>
    </div>

    <!-- 文档列表 -->
    <div v-else class="doc-list">
      <article
        v-for="(d, i) in documents"
        :key="d.id"
        class="doc hd-rise"
        :style="{ animationDelay: `${0.05 + i * 0.05}s` }"
      >
        <span class="doc-no mono">{{ String((pageNum - 1) * pageSize + i + 1).padStart(2, '0') }}</span>

        <div class="doc-main">
          <h3 class="doc-title">{{ d.title }}</h3>
          <p class="doc-meta">
            <span>{{ sourceLabel(d.sourceType) }}</span>
            <span v-if="d.fileName" class="mono ellipsis">{{ d.fileName }}</span>
            <span>{{ d.visibility === 'PRIVATE' ? '私有' : '公开' }}</span>
            <span class="mono">v{{ d.version }}</span>
            <span class="mono">{{ d.chunkCount }} 块</span>
            <time class="mono">{{ formatTime(d.createdAt) }}</time>
          </p>
          <p v-if="d.status === 'FAILED' && d.errorMessage" class="doc-error">{{ d.errorMessage }}</p>
        </div>

        <div class="doc-side">
          <span class="status" :class="statusClass(d.status)">{{ statusText(d.status) }}</span>
          <div class="doc-actions">
            <button
              class="text-btn"
              type="button"
              :disabled="isIngesting(d.status) || actingId === d.id"
              @click="onReindex(d)"
            >
              重建
            </button>
            <button
              class="text-btn danger"
              type="button"
              :disabled="isIngesting(d.status) || actingId === d.id"
              @click="onDelete(d)"
            >
              删除
            </button>
          </div>
        </div>
      </article>
    </div>

    <!-- 分页 -->
    <nav v-if="!loading && totalPages > 1" class="pager" aria-label="分页">
      <button class="text-btn" type="button" :disabled="pageNum <= 1" @click="goPage(pageNum - 1)">← 上一页</button>
      <span class="pager-no mono">{{ pageNum }} / {{ totalPages }}</span>
      <button class="text-btn" type="button" :disabled="pageNum >= totalPages" @click="goPage(pageNum + 1)">
        下一页 →
      </button>
    </nav>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createKbDocument,
  deleteKbDocument,
  listKbDocuments,
  reindexKbDocument
} from '../api/kb'

/** 单文件大小上限，与后端 MAX_FILE_BYTES 一致 */
const MAX_FILE_BYTES = 20 * 1024 * 1024
/** 入库中间态：这些状态会触发列表轮询 */
const ACTIVE_STATUSES = ['PENDING', 'PARSING', 'INDEXING']
/** 轮询间隔 */
const POLL_INTERVAL_MS = 3000
/** 轮询硬封顶：避免任务异常时无限请求 */
const POLL_MAX_MS = 10 * 60 * 1000

/** 状态筛选项 */
const statusFilters = [
  { label: '全部', value: '' },
  { label: '可检索', value: 'READY' },
  { label: '入库中', value: 'PENDING' },
  { label: '失败', value: 'FAILED' }
]

/* ---- 列表状态 ---- */
const documents = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const statusFilter = ref('')
const loading = ref(false)
const loadError = ref(false)
const actingId = ref(null)

/* ---- 录入表单 ---- */
const formOpen = ref(false)
const mode = ref('file')
const fileInput = ref(null)
const file = ref(null)
const text = ref('')
const title = ref('')
const visibility = ref('PUBLIC')
const submitting = ref(false)

/* ---- 轮询 ---- */
let timer = null
let pollStartedAt = 0

/** 当前页是否存在入库中的文档 */
const hasActive = computed(() => documents.value.some((d) => ACTIVE_STATUSES.includes(d.status)))
/** 总页数，至少 1 页 */
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
/** 所选文件大小文案 */
const fileSizeText = computed(() => {
  if (!file.value) return ''
  const kb = file.value.size / 1024
  return kb >= 1024 ? `${(kb / 1024).toFixed(1)} MB` : `${Math.round(kb)} KB`
})

onMounted(() => loadList())

// 离开页面必须停表，否则后台会一直轮询
onUnmounted(clearPolling)

// 有入库中的文档就开轮询，全部到终态自动停止
watch(hasActive, (active) => {
  if (active) {
    startPolling()
  } else {
    clearPolling()
  }
})

/**
 * 拉取文档列表。
 *
 * @param {boolean} silent 静默刷新（轮询用，不显示骨架屏）
 */
async function loadList(silent = false) {
  if (!silent) {
    loading.value = true
    loadError.value = false
  }
  try {
    const data = await listKbDocuments({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: statusFilter.value || undefined
    })
    documents.value = data.records || []
    total.value = data.total || 0
    loadError.value = false
  } catch {
    // 轮询中的偶发失败不打断已有列表，只有首次加载失败才展示错误态
    if (!silent) loadError.value = true
  } finally {
    if (!silent) loading.value = false
  }
}

/** 启动轮询：已有定时器则不重复启动 */
function startPolling() {
  if (timer) return
  pollStartedAt = Date.now()
  timer = setInterval(async () => {
    if (Date.now() - pollStartedAt >= POLL_MAX_MS) {
      clearPolling()
      ElMessage.warning('入库耗时较长，请稍后刷新查看')
      return
    }
    await loadList(true)
    if (!hasActive.value) clearPolling()
  }, POLL_INTERVAL_MS)
}

/** 停止轮询并清空计时器 */
function clearPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

/** 切换状态筛选：重置到第一页后重新查询 */
function setFilter(value) {
  if (statusFilter.value === value) return
  statusFilter.value = value
  pageNum.value = 1
  loadList()
}

/** 翻页 */
function goPage(target) {
  const next = Math.min(Math.max(1, target), totalPages.value)
  if (next === pageNum.value) return
  pageNum.value = next
  loadList()
}

/** 展开/收起录入区 */
function toggleForm() {
  formOpen.value = !formOpen.value
  if (formOpen.value) return
  resetForm()
}

/** 清空表单，避免上次选择残留 */
function resetForm() {
  file.value = null
  text.value = ''
  title.value = ''
  visibility.value = 'PUBLIC'
  if (fileInput.value) fileInput.value.value = ''
}

/** 触发隐藏的文件选择框 */
function pickFile() {
  fileInput.value?.click()
}

/** 选择文件后校验大小 */
function onFileChange(e) {
  applyFile(e.target.files?.[0])
}

/** 拖拽上传 */
function onDrop(e) {
  applyFile(e.dataTransfer?.files?.[0])
}

/**
 * 校验并记录待上传文件。
 *
 * @param {File} picked 用户选择或拖入的文件
 */
function applyFile(picked) {
  if (!picked) return
  if (picked.size > MAX_FILE_BYTES) {
    ElMessage.error('文件大小不能超过 20MB')
    return
  }
  if (picked.size === 0) {
    ElMessage.error('上传文件内容为空')
    return
  }
  file.value = picked
}

/** 提交入库：文件与文本必须且只能提供一个 */
async function submit() {
  const hasFile = Boolean(file.value)
  const hasText = Boolean(text.value.trim())
  if (!hasFile && !hasText) {
    ElMessage.warning('请上传文件或填写文本内容')
    return
  }
  if (hasFile && hasText) {
    ElMessage.warning('文件与文本只能选择一种录入方式')
    return
  }
  submitting.value = true
  try {
    await createKbDocument({
      file: hasFile ? file.value : null,
      text: hasText ? text.value.trim() : '',
      title: title.value.trim(),
      visibility: visibility.value
    })
    ElMessage.success('已提交入库，完成后状态会变为「可检索」')
    pageNum.value = 1
    resetForm()
    formOpen.value = false
    await loadList()
  } catch {
    // 错误提示由响应拦截器统一处理，这里保留表单内容方便重试
  } finally {
    submitting.value = false
  }
}

/** 重建索引：版本号 +1 重新解析，失败文档也用它重试 */
async function onReindex(doc) {
  actingId.value = doc.id
  try {
    await reindexKbDocument(doc.id)
    ElMessage.success('已重新提交入库')
    await loadList()
  } catch {
    // 403（系统内置语料）/409（入库中）由拦截器提示
  } finally {
    actingId.value = null
  }
}

/** 删除文档：同步清理分块与向量，不可恢复 */
async function onDelete(doc) {
  try {
    await ElMessageBox.confirm(`确定删除《${doc.title}》吗？分块与向量会一并清理，不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  actingId.value = doc.id
  try {
    await deleteKbDocument(doc.id)
    ElMessage.success('文档已删除')
    // 删掉当前页最后一条时回退一页，避免停在空页
    if (documents.value.length === 1 && pageNum.value > 1) pageNum.value -= 1
    await loadList()
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    actingId.value = null
  }
}

/** 状态是否属于入库中间态 */
function isIngesting(status) {
  return ACTIVE_STATUSES.includes(status)
}

/** 状态文案 */
function statusText(status) {
  return {
    PENDING: '排队中',
    PARSING: '解析中',
    INDEXING: '向量化中',
    READY: '可检索',
    FAILED: '失败'
  }[status] || status
}

/** 状态样式类 */
function statusClass(status) {
  if (status === 'READY') return 'ok'
  if (status === 'FAILED') return 'fail'
  if (status === 'PENDING') return 'wait'
  return 'run'
}

/** 来源类型文案 */
function sourceLabel(type) {
  return { FILE: '文件', TEXT: '文本', URL: '链接' }[type] || type
}

/** ISO 时间格式化为 2026.11.10 */
function formatTime(t) {
  if (!t) return ''
  return String(t).slice(0, 10).replaceAll('-', '.')
}
</script>

<style scoped>
.page {
  max-width: var(--hd-container-max);
  margin: 0 auto;
  padding: var(--hd-space-4) var(--hd-space-4) var(--hd-space-6);
}

/* ---- 页头 ---- */
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--hd-space-3);
  padding-bottom: var(--hd-space-3);
  border-bottom: var(--hd-hairline-strong);
}
.head-title {
  margin-top: 8px;
  font-size: 36px;
  line-height: 1.2;
  letter-spacing: 0.04em;
}
.head-desc {
  margin-top: 8px;
  max-width: 560px;
  color: var(--hd-neutral-500);
  font-size: var(--hd-text-caption);
  line-height: var(--hd-text-caption-line);
}
.new-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border: none;
  background: none;
  color: var(--hd-ink);
  font-family: inherit;
  font-size: var(--hd-text-caption);
  letter-spacing: 0.12em;
  cursor: pointer;
  white-space: nowrap;
}
.new-arrow {
  transition: transform var(--hd-duration-fast) ease-out;
}
.new-btn:hover { color: var(--hd-primary-700); }
.new-btn:hover .new-arrow { transform: translateX(5px); }

/* ---- 录入区 ---- */
.compose {
  margin-top: var(--hd-space-3);
  border: var(--hd-hairline);
  border-radius: var(--hd-radius-base);
  background: var(--hd-neutral-100);
}
.compose-tabs {
  display: flex;
  gap: var(--hd-space-3);
  padding: var(--hd-space-1) var(--hd-space-2);
  border-bottom: var(--hd-hairline);
}
.compose-tab {
  padding: 8px 0;
  border: none;
  border-bottom: 2px solid transparent;
  background: none;
  color: var(--hd-neutral-500);
  font-family: inherit;
  font-size: var(--hd-text-caption);
  letter-spacing: 0.1em;
  cursor: pointer;
  transition: color var(--hd-duration-fast), border-color var(--hd-duration-fast);
}
.compose-tab:hover { color: var(--hd-ink); }
.compose-tab.active {
  color: var(--hd-primary-700);
  border-bottom-color: var(--hd-primary-600);
}

.compose-body {
  padding: var(--hd-space-2);
  display: flex;
  flex-direction: column;
  gap: var(--hd-space-2);
}

/* 文件投放区：虚线框 + 纸白底 */
.drop {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 132px;
  padding: var(--hd-space-2);
  border: 1px dashed var(--hd-neutral-300);
  border-radius: var(--hd-radius-base);
  background: var(--hd-paper);
  cursor: pointer;
  outline: none;
  transition: border-color var(--hd-duration-fast), background-color var(--hd-duration-fast);
}
.drop:hover,
.drop:focus-visible {
  border-color: var(--hd-primary-400);
  background: var(--hd-primary-50);
}
.file-input { display: none; }
.drop-title {
  color: var(--hd-ink);
  font-size: var(--hd-text-body);
  letter-spacing: 0.04em;
  word-break: break-all;
  text-align: center;
}
.drop-meta {
  color: var(--hd-neutral-500);
  font-size: var(--hd-text-overline);
  letter-spacing: 0.08em;
}
.drop-hint {
  margin-top: 4px;
  color: var(--hd-neutral-400);
  font-size: var(--hd-text-overline);
  letter-spacing: 0.06em;
}

.compose-text {
  min-height: 132px;
  padding: var(--hd-space-2);
  border: 1px solid var(--hd-neutral-300);
  border-radius: var(--hd-radius-base);
  background: var(--hd-paper);
  color: var(--hd-ink);
  font-family: inherit;
  font-size: var(--hd-text-body);
  line-height: var(--hd-text-body-line);
  resize: vertical;
  outline: none;
  transition: border-color var(--hd-duration-fast);
}
.compose-text:focus { border-color: var(--hd-primary-400); }

.compose-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--hd-space-2);
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field-label {
  font-family: var(--hd-font-mono);
  font-size: var(--hd-text-overline);
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--hd-neutral-500);
}
.field-input {
  height: 38px;
  padding: 0 10px;
  border: 1px solid var(--hd-neutral-300);
  border-radius: var(--hd-radius-base);
  background: var(--hd-paper);
  color: var(--hd-ink);
  font-family: inherit;
  font-size: var(--hd-text-caption);
  outline: none;
  transition: border-color var(--hd-duration-fast);
}
.field-input:focus { border-color: var(--hd-primary-400); }

.compose-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--hd-space-2);
}
.compose-tip {
  color: var(--hd-neutral-500);
  font-size: var(--hd-text-overline);
  line-height: var(--hd-text-overline-line);
  letter-spacing: 0.04em;
}

/* ---- 工具栏 ---- */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--hd-space-2);
  margin-top: var(--hd-space-3);
  padding-bottom: var(--hd-space-1);
  border-bottom: var(--hd-hairline);
}
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: var(--hd-space-1);
}
.chip {
  padding: 5px 12px;
  border: 1px solid var(--hd-neutral-200);
  border-radius: var(--hd-radius-base);
  background: transparent;
  color: var(--hd-neutral-500);
  font-family: inherit;
  font-size: var(--hd-text-overline);
  letter-spacing: 0.1em;
  cursor: pointer;
  transition: color var(--hd-duration-fast), border-color var(--hd-duration-fast), background-color var(--hd-duration-fast);
}
.chip:hover { color: var(--hd-ink); border-color: var(--hd-neutral-400); }
.chip.active {
  color: var(--hd-primary-700);
  border-color: var(--hd-primary-400);
  background: var(--hd-primary-50);
}
.toolbar-count {
  color: var(--hd-neutral-500);
  font-size: var(--hd-text-caption);
  letter-spacing: 0.06em;
  white-space: nowrap;
}

/* ---- 文档条目 ---- */
.doc-list { margin-top: 4px; }
.doc {
  display: grid;
  grid-template-columns: 36px 1fr auto;
  gap: var(--hd-space-2);
  align-items: start;
  padding: var(--hd-space-2) 0;
  border-bottom: var(--hd-hairline);
  transition: background-color var(--hd-duration-fast) ease-out;
}
.doc:hover { background-color: rgba(221, 213, 198, 0.18); }
.doc-no {
  padding-top: 4px;
  color: var(--hd-neutral-400);
  font-size: var(--hd-text-overline);
  letter-spacing: 0.12em;
}
.doc-main { min-width: 0; }
.doc-title {
  font-size: var(--hd-text-h3);
  line-height: var(--hd-text-h3-line);
  color: var(--hd-ink);
  word-break: break-all;
}
.doc-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 12px;
  margin-top: 6px;
  color: var(--hd-neutral-500);
  font-size: var(--hd-text-overline);
  letter-spacing: 0.04em;
}
.doc-meta > span,
.doc-meta > time {
  display: inline-flex;
  align-items: center;
}
.doc-meta > span:not(:first-child)::before {
  content: '';
  width: 1px;
  height: 9px;
  margin-right: 12px;
  background: var(--hd-neutral-300);
}
.ellipsis {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc-error {
  margin-top: 6px;
  color: var(--hd-danger);
  font-size: var(--hd-text-overline);
  line-height: var(--hd-text-overline-line);
  word-break: break-all;
}

.doc-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  min-width: 108px;
}
.status {
  font-family: var(--hd-font-mono);
  font-size: var(--hd-text-overline);
  letter-spacing: 0.14em;
  text-transform: uppercase;
  padding: 3px 8px;
  border: 1px solid currentColor;
  border-radius: var(--hd-radius-base);
}
.status.ok { color: var(--hd-success); }
.status.fail { color: var(--hd-danger); }
.status.run { color: var(--hd-primary-700); }
.status.wait { color: var(--hd-neutral-500); }

.doc-actions {
  display: flex;
  gap: var(--hd-space-1);
}

/* ---- 通用控件 ---- */
.mono { font-family: var(--hd-font-mono); }
.text-btn {
  border: none;
  background: none;
  color: var(--hd-primary-700);
  font-family: inherit;
  font-size: var(--hd-text-caption);
  letter-spacing: 0.08em;
  cursor: pointer;
}
.text-btn:hover { color: var(--hd-primary-900); }
.text-btn:disabled {
  color: var(--hd-neutral-400);
  cursor: not-allowed;
}
.text-btn.danger { color: var(--hd-danger); }
.text-btn.danger:hover { color: var(--hd-primary-900); }

.ink-btn {
  padding: 10px 26px;
  border: 1px solid var(--hd-ink);
  border-radius: var(--hd-radius-base);
  background: var(--hd-ink);
  color: var(--hd-on-ink);
  font-family: inherit;
  font-size: var(--hd-text-caption);
  letter-spacing: 0.14em;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color var(--hd-duration-fast), border-color var(--hd-duration-fast);
}
.ink-btn:hover:not(:disabled) {
  background: var(--hd-primary-700);
  border-color: var(--hd-primary-700);
}
.ink-btn:disabled {
  background: var(--hd-neutral-300);
  border-color: var(--hd-neutral-300);
  cursor: not-allowed;
}

/* ---- 骨架与状态页 ---- */
.skeleton-doc { cursor: default; }
.skeleton-doc:hover { background: none; }
.sk-line {
  height: 12px;
  margin-bottom: 10px;
  border-radius: 2px;
  background: linear-gradient(90deg, var(--hd-neutral-100) 25%, var(--hd-neutral-200) 37%, var(--hd-neutral-100) 63%);
  background-size: 400% 100%;
  animation: sk-shimmer 1.4s ease infinite;
}
.sk-w30 { width: 30%; }
.sk-w60 { width: 60%; height: 18px; }
@keyframes sk-shimmer {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}

.state-box {
  padding: var(--hd-space-6) 0;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--hd-space-1);
}
.state-title {
  font-family: var(--hd-font-display);
  font-size: var(--hd-text-h2);
  color: var(--hd-ink);
}
.state-desc {
  color: var(--hd-neutral-500);
  font-size: var(--hd-text-caption);
  max-width: 420px;
}
.state-box .ink-btn { margin-top: var(--hd-space-2); }

/* ---- 分页 ---- */
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--hd-space-3);
  padding-top: var(--hd-space-3);
}
.pager-no {
  color: var(--hd-neutral-500);
  font-size: var(--hd-text-overline);
  letter-spacing: 0.12em;
}

/* ---- 窄屏 ---- */
@media (max-width: 720px) {
  .page { padding: var(--hd-space-3) var(--hd-space-2) var(--hd-space-5); }
  .head-title { font-size: 28px; }
  .compose-fields { grid-template-columns: 1fr; }
  .compose-foot { flex-direction: column; align-items: flex-start; }
  .doc { grid-template-columns: 28px 1fr; }
  .doc-side {
    grid-column: 2;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    min-width: 0;
  }
  .ellipsis { max-width: 140px; }
}
</style>
