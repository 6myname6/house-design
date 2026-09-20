<template>
  <div class="ai-chat">
    <!-- 顶部标题栏 -->
    <header class="chat-header">
      <div class="header-text">
        <h2 class="header-title">AI 设计助手</h2>
        <p class="header-sub">资深装修设计师在线，风格识别 · 配色材质 · 预算建议，支持发图提问</p>
      </div>
    </header>

    <!-- 消息区 -->
    <div ref="scrollRef" class="chat-scroll">
      <!-- 空状态：开场建议 -->
      <div v-if="messages.length === 0" class="welcome">
        <div class="welcome-mark" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 20l4-1 10.5-10.5a2.12 2.12 0 0 0-3-3L4 16l-1 4z"/>
            <path d="M13.5 6.5l3 3"/>
          </svg>
        </div>
        <h3 class="welcome-title">想聊点什么？</h3>
        <p class="welcome-desc">发一张效果图让我识别风格，或直接问装修难题</p>
        <div class="suggest-list">
          <button
            v-for="s in suggestions"
            :key="s"
            type="button"
            class="suggest-item"
            @click="sendText(s)"
          >
            {{ s }}
          </button>
        </div>
      </div>

      <!-- 消息气泡流 -->
      <TransitionGroup v-else name="msg" tag="div" class="msg-list">
        <div
          v-for="(m, i) in messages"
          :key="m.id"
          class="msg-row"
          :class="m.role"
        >
          <div class="bubble">
            <div v-if="m.images && m.images.length" class="bubble-images">
              <img
                v-for="(img, idx) in m.images"
                :key="idx"
                :src="img"
                class="bubble-img"
                alt="对话图片"
                @click="previewList = m.images; previewIndex = idx"
              />
            </div>
            <!-- AI 内容是 Markdown，经 DOMPurify 消毒后渲染；用户消息仍走纯文本 -->
            <div
              v-if="m.content"
              class="bubble-text markdown-body"
              v-html="renderMarkdown(m.content)"
            ></div>
            <!-- AI 正在输入 -->
            <div v-if="m.pending" class="typing" aria-label="AI 正在输入">
              <span></span><span></span><span></span>
            </div>
            <!-- 失败重试 -->
            <div v-if="m.error" class="bubble-error">
              <span>{{ m.error }}</span>
              <button type="button" class="retry-btn" @click="retry(i)">重试</button>
            </div>
          </div>
        </div>
      </TransitionGroup>
    </div>

    <!-- 待发送图片预览条 -->
    <div v-if="pendingImages.length" class="pending-bar">
      <div v-for="(img, i) in pendingImages" :key="i" class="pending-thumb">
        <img :src="img.dataUrl" alt="待发送图片" />
        <button type="button" class="thumb-remove" title="移除" @click="removeImage(i)">×</button>
      </div>
    </div>

    <!-- 输入区 -->
    <footer class="chat-input-bar">
      <div class="input-inner">
        <input
          ref="fileInputRef"
          type="file"
          accept="image/jpeg,image/png,image/gif,image/webp"
          multiple
          hidden
          @change="onPickFiles"
        />
        <button
          type="button"
          class="icon-btn"
          :disabled="sending"
          title="添加图片"
          @click="fileInputRef?.click()"
        >
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="18" height="18" rx="2"/>
            <circle cx="9" cy="9" r="1.6"/>
            <path d="M21 15l-5-5L5 21"/>
          </svg>
        </button>
        <textarea
          v-model="draft"
          class="draft-input"
          rows="1"
          placeholder="描述你的装修问题，或直接发图…"
          @keydown.enter.exact.prevent="onSend"
          @input="autoResize"
        ></textarea>
        <button
          type="button"
          class="send-btn"
          :disabled="!canSend"
          @click="onSend"
        >
          <span v-if="!sending">发送</span>
          <span v-else class="send-loading" aria-hidden="true"></span>
        </button>
      </div>
      <p class="input-hint">Enter 发送，Shift+Enter 换行；图片将随消息直接提交</p>
    </footer>

    <!-- 大图预览 -->
    <el-image-viewer
      v-if="previewList.length"
      :url-list="previewList"
      :initial-index="previewIndex"
      teleported
      @close="previewList = []"
    />
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { chatWithAi } from '../api/ai'
import { renderMarkdown } from '../utils/markdown'

const suggestions = [
  '现代简约风格怎么搭配主色调？',
  '小户型显大，灯光怎么设计？',
  '预算有限，客厅瓷砖选平替还是实木地板？'
]

let seq = 0
const nextId = () => `m${++seq}-${Date.now()}`

const messages = ref([])        // { id, role: 'user'|'assistant', content, images: [dataUrl], pending, error }
const draft = ref('')
const pendingImages = ref([])   // { dataUrl }
const sending = ref(false)
const scrollRef = ref(null)
const fileInputRef = ref(null)
const previewList = ref([])
const previewIndex = ref(0)

const canSend = computed(() =>
  !sending.value && (draft.value.trim().length > 0 || pendingImages.value.length > 0)
)

function autoResize(e) {
  const el = e.target
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

function scrollToBottom() {
  nextTick(() => {
    const box = scrollRef.value
    if (box) box.scrollTo({ top: box.scrollHeight, behavior: 'smooth' })
  })
}

// 空状态建议直接发问
function sendText(text) {
  draft.value = text
  onSend()
}

// 选图 -> FileReader 转 DataURL（不经服务器中转，直接随 JSON 提交）
function onPickFiles(e) {
  const files = Array.from(e.target.files || [])
  files.forEach((file) => {
    if (file.size > 8 * 1024 * 1024) {
      ElMessage.warning(`「${file.name}」超过 8MB，请压缩后再发`)
      return
    }
    const reader = new FileReader()
    reader.onload = () => pendingImages.value.push({ dataUrl: reader.result })
    reader.readAsDataURL(file)
  })
  e.target.value = ''
}

function removeImage(i) {
  pendingImages.value.splice(i, 1)
}

async function onSend() {
  if (!canSend.value) return
  const question = draft.value.trim()
  const images = pendingImages.value.map((x) => x.dataUrl)

  const userMsg = { id: nextId(), role: 'user', content: question, images, pending: false }
  const aiMsg = { id: nextId(), role: 'assistant', content: '', images: [], pending: true, error: '' }
  messages.value.push(userMsg, aiMsg)
  draft.value = ''
  pendingImages.value = []
  sending.value = true
  scrollToBottom()

  await requestAi(question, images, aiMsg)
}

// 失败重试：保留原问题与图片，只重新请求并更新同一条 AI 消息
async function retry(index) {
  const aiMsg = messages.value[index]
  // 找上一条用户消息取回原始提问
  const userMsg = [...messages.value].slice(0, index).reverse().find((m) => m.role === 'user')
  if (!userMsg) return
  aiMsg.error = ''
  aiMsg.pending = true
  aiMsg.content = ''
  sending.value = true
  await requestAi(userMsg.content || '', userMsg.images || [], aiMsg)
}

async function requestAi(question, images, aiMsg) {
  try {
    const answer = await chatWithAi({ question, images })
    aiMsg.content = answer
  } catch (err) {
    aiMsg.error = err?.response?.data?.message || 'AI 暂时开小差了，请稍后重试'
  } finally {
    aiMsg.pending = false
    sending.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.ai-chat {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--hd-neutral-50);
}

/* ---------- 顶部 ---------- */
.chat-header {
  flex-shrink: 0;
  padding: var(--hd-space-2) var(--hd-space-3);
  background: #fff;
  border-bottom: 1px solid var(--hd-neutral-200);
}
.header-title {
  margin: 0;
  font-size: var(--hd-text-h2);
  line-height: var(--hd-text-h2-line);
  color: var(--hd-neutral-800);
}
.header-sub {
  margin: 2px 0 0;
  font-size: var(--hd-text-caption);
  line-height: var(--hd-text-caption-line);
  color: var(--hd-neutral-500);
}

/* ---------- 消息滚动区 ---------- */
.chat-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: var(--hd-space-3);
}
.msg-list {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: var(--hd-space-2);
}

/* ---------- 空状态 ---------- */
.welcome {
  max-width: 560px;
  margin: 8vh auto 0;
  text-align: center;
  padding: 0 var(--hd-space-2);
  animation: welcome-in 0.5s ease-out both;
}
.welcome-mark {
  width: 64px;
  height: 64px;
  margin: 0 auto var(--hd-space-2);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: var(--hd-primary-700);
  background: var(--hd-primary-50);
  border: 1px solid var(--hd-primary-100);
}
.welcome-title {
  margin: 0 0 var(--hd-space-1);
  font-size: var(--hd-text-h2);
  color: var(--hd-neutral-800);
}
.welcome-desc {
  margin: 0 0 var(--hd-space-3);
  font-size: var(--hd-text-body);
  color: var(--hd-neutral-500);
}
.suggest-list {
  display: flex;
  flex-direction: column;
  gap: var(--hd-space-1);
}
.suggest-item {
  padding: 12px var(--hd-space-2);
  border: 1px solid var(--hd-neutral-200);
  border-radius: var(--hd-radius-lg);
  background: #fff;
  color: var(--hd-neutral-700);
  font-size: var(--hd-text-body);
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color var(--hd-duration-fast) ease-out,
              color var(--hd-duration-fast) ease-out,
              transform var(--hd-duration-fast) ease-out;
}
.suggest-item:hover {
  border-color: var(--hd-primary-200);
  color: var(--hd-primary-700);
  transform: translateY(-1px);
}

/* ---------- 气泡 ---------- */
.msg-row { display: flex; }
.msg-row.user { justify-content: flex-end; }
.msg-row.assistant { justify-content: flex-start; }

.bubble {
  max-width: 76%;
  padding: 10px 14px;
  border-radius: var(--hd-radius-lg);
  font-size: var(--hd-text-body);
  line-height: var(--hd-text-body-line);
  word-break: break-word;
}
.msg-row.user .bubble {
  background: var(--hd-primary-700);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.msg-row.assistant .bubble {
  background: #fff;
  color: var(--hd-neutral-800);
  border: 1px solid var(--hd-neutral-200);
  border-bottom-left-radius: 4px;
}
.bubble-text { margin: 0; }
.bubble-text:empty { display: none; }

/* AI 气泡内的 Markdown 排版 */
.markdown-body :first-child { margin-top: 0; }
.markdown-body :last-child { margin-bottom: 0; }
.markdown-body p { margin: 0 0 8px; line-height: var(--hd-text-body-line); }
.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4 {
  margin: 14px 0 6px;
  line-height: 1.4;
  color: inherit;
  font-weight: var(--hd-font-weight-medium);
}
.markdown-body h1 { font-size: 19px; }
.markdown-body h2 { font-size: 18px; }
.markdown-body h3 { font-size: 17px; }
.markdown-body h4 { font-size: var(--hd-text-body); }
.markdown-body ul,
.markdown-body ol { margin: 6px 0; padding-left: 22px; }
.markdown-body li { margin: 3px 0; }
.markdown-body li::marker { color: var(--hd-primary-600); }
.markdown-body strong { font-weight: var(--hd-font-weight-medium); color: var(--hd-primary-900); }
.msg-row.user .markdown-body strong { color: #fff; }
.markdown-body code {
  font-family: var(--hd-font-mono);
  font-size: 0.9em;
  background: var(--hd-neutral-100);
  padding: 1px 5px;
  border-radius: 4px;
}
.markdown-body pre {
  background: var(--hd-neutral-100);
  padding: var(--hd-space-1);
  border-radius: var(--hd-radius-base);
  overflow-x: auto;
  margin: 8px 0;
}
.markdown-body pre code { background: none; padding: 0; }
.markdown-body blockquote {
  margin: 8px 0;
  padding-left: var(--hd-space-1);
  border-left: 3px solid var(--hd-primary-200);
  color: var(--hd-neutral-500);
}
.markdown-body a { color: var(--hd-primary-700); text-decoration: underline; }

.bubble-images {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}
.bubble-images:last-child { margin-bottom: 0; }
.bubble-img {
  width: 140px;
  height: 140px;
  object-fit: cover;
  border-radius: var(--hd-radius-base);
  cursor: zoom-in;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

/* AI 正在输入三点 */
.typing { display: inline-flex; gap: 4px; padding: 4px 0; }
.typing span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--hd-neutral-400);
  animation: typing-bounce 1.2s infinite ease-in-out both;
}
.typing span:nth-child(2) { animation-delay: 0.15s; }
.typing span:nth-child(3) { animation-delay: 0.3s; }

.bubble-error {
  display: flex;
  align-items: center;
  gap: var(--hd-space-1);
  color: var(--hd-danger);
  font-size: var(--hd-text-caption);
}
.retry-btn {
  border: 1px solid currentColor;
  background: transparent;
  color: inherit;
  border-radius: var(--hd-radius-base);
  padding: 2px 10px;
  font-size: var(--hd-text-caption);
  font-family: inherit;
  cursor: pointer;
}
.retry-btn:hover { background: var(--hd-danger); color: #fff; }

/* ---------- 待发图片条 ---------- */
.pending-bar {
  flex-shrink: 0;
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
  padding: var(--hd-space-1) var(--hd-space-3) 0;
  display: flex;
  gap: var(--hd-space-1);
  flex-wrap: wrap;
}
.pending-thumb { position: relative; }
.pending-thumb img {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: var(--hd-radius-base);
  border: 1px solid var(--hd-neutral-200);
}
.thumb-remove {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: var(--hd-neutral-700);
  color: #fff;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ---------- 输入区 ---------- */
.chat-input-bar {
  flex-shrink: 0;
  padding: var(--hd-space-1) var(--hd-space-3) var(--hd-space-2);
  background: var(--hd-neutral-50);
  border-top: 1px solid var(--hd-neutral-200);
}
.input-inner {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  align-items: flex-end;
  gap: var(--hd-space-1);
  background: #fff;
  border: 1px solid var(--hd-neutral-200);
  border-radius: var(--hd-radius-lg);
  padding: 6px 8px;
  transition: border-color var(--hd-duration-fast) ease-out;
}
.input-inner:focus-within { border-color: var(--hd-primary-400); }

.icon-btn {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: var(--hd-radius-base);
  background: transparent;
  color: var(--hd-neutral-500);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color var(--hd-duration-fast), color var(--hd-duration-fast);
}
.icon-btn:hover:not(:disabled) { background: var(--hd-neutral-100); color: var(--hd-primary-700); }
.icon-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.draft-input {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  background: transparent;
  font-family: inherit;
  font-size: var(--hd-text-body);
  line-height: var(--hd-text-body-line);
  color: var(--hd-neutral-800);
  padding: 7px 0;
  max-height: 120px;
}
.draft-input::placeholder { color: var(--hd-neutral-400); }

.send-btn {
  flex-shrink: 0;
  border: none;
  border-radius: var(--hd-radius-base);
  padding: 8px 18px;
  background: var(--hd-primary-700);
  color: #fff;
  font-size: var(--hd-text-caption);
  font-family: inherit;
  font-weight: var(--hd-font-weight-medium);
  cursor: pointer;
  transition: background-color var(--hd-duration-fast), opacity var(--hd-duration-fast);
}
.send-btn:hover:not(:disabled) { background: var(--hd-primary-600); }
.send-btn:disabled { opacity: 0.45; cursor: not-allowed; }

.send-loading {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  display: inline-block;
  animation: spin 0.7s linear infinite;
}

.input-hint {
  max-width: 800px;
  margin: 6px auto 0;
  font-size: var(--hd-text-overline);
  color: var(--hd-neutral-400);
}

/* ---------- 动效 ---------- */
@keyframes spin { to { transform: rotate(360deg); } }

@keyframes typing-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
  30% { transform: translateY(-5px); opacity: 1; }
}

@keyframes welcome-in {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 气泡进场（TransitionGroup） */
.msg-enter-active { transition: all 0.28s ease-out; }
.msg-enter-from { opacity: 0; transform: translateY(10px); }
</style>
