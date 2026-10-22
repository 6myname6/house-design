<template>
  <div class="ai-chat">
    <!-- 顶部标题栏 -->
    <header class="chat-header">
      <div class="header-inner">
        <div class="header-text">
          <p class="header-kicker">03 — AI Atelier</p>
          <h2 class="header-title">问问设计师</h2>
          <p class="header-sub">风格识别 · 配色材质 · 预算建议，支持发图提问</p>
        </div>
        <div class="header-actions">
          <!-- 历史对话下拉 -->
          <div ref="historyWrapRef" class="history-wrap">
            <button
              class="history-btn"
              type="button"
              :class="{ 'is-open': historyOpen }"
              @click="toggleHistory"
            >
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 12a9 9 0 1 0 3-6.7"/>
                <path d="M3 4v5h5"/>
                <path d="M12 8v4l3 2"/>
              </svg>
              历史
            </button>
            <Transition name="history-drop">
              <div v-if="historyOpen" class="history-panel">
                <p class="history-panel-title">历史会话 / Archive</p>
                <div v-if="historyList.length === 0" class="history-empty">暂无历史对话</div>
                <button
                  v-for="s in historyList"
                  :key="s.id"
                  type="button"
                  class="history-item"
                  :class="{ active: s.id === aiStore.currentSessionId }"
                  @click="openSession(s.id)"
                >
                  <span class="history-item-title">{{ s.title || '图片对话' }}</span>
                  <span class="history-item-meta">{{ s.messages.length }} 条 · {{ formatTime(s.updatedAt) }}</span>
                </button>
              </div>
            </Transition>
          </div>
          <button class="new-chat-btn" type="button" @click="newConversation">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 5v14"/>
              <path d="M5 12h14"/>
            </svg>
            新会话
          </button>
        </div>
      </div>
    </header>

    <!-- 消息区 -->
    <div ref="scrollRef" class="chat-scroll">
      <!-- 空状态：开场建议 -->
      <div v-if="messages.length === 0" class="welcome">
        <p class="welcome-kicker hd-rise" style="animation-delay:.05s">Your Design Atelier</p>
        <h3 class="welcome-title hd-rise" style="animation-delay:.12s">想聊点什么？</h3>
        <p class="welcome-rule" aria-hidden="true"></p>
        <p class="welcome-desc hd-rise" style="animation-delay:.2s">发一张效果图让我识别风格，或直接问装修难题</p>
        <div class="suggest-list hd-rise" style="animation-delay:.28s">
          <button
            v-for="(s, idx) in suggestions"
            :key="s"
            type="button"
            class="suggest-item"
            @click="sendText(s)"
          >
            <span class="suggest-no">Q.0{{ idx + 1 }}</span>
            <span class="suggest-text">{{ s }}</span>
            <span class="suggest-arrow" aria-hidden="true">→</span>
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
        <img :src="img.previewUrl" alt="待发送图片" />
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
          <span v-if="uploading">上传中…</span>
          <span v-else-if="!sending">发送</span>
          <span v-else class="send-loading" aria-hidden="true"></span>
        </button>
      </div>
      <p class="input-hint">Enter 发送，Shift+Enter 换行；图片将先上传再随消息发送</p>
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
import { ref, reactive, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { chatWithAi } from '../api/ai'
import { uploadFile } from '../api/file'
import { renderMarkdown } from '../utils/markdown'
import { useAiChatStore } from '../stores/aiChat'
import { getUserIdFromToken } from '../utils/storage'

// keep-alive 按组件名缓存本页，切换侧边栏时休眠而非卸载
defineOptions({ name: 'AiChat' })

const aiStore = useAiChatStore()
// 兜底：进入对话页时确保加载的是当前登录用户的会话（刷新/SPA 内跳转场景）
aiStore.bindUser(getUserIdFromToken())

const suggestions = [
  '现代简约风格怎么搭配主色调？',
  '小户型显大，灯光怎么设计？',
  '预算有限，客厅瓷砖选平替还是实木地板？'
]

let seq = 0
const nextId = () => `m${++seq}-${Date.now()}`

// 消息与后端会话标识提升至 Pinia（并持久化到 localStorage），切换页面/刷新都不丢
const messages = computed(() => aiStore.currentSession?.messages ?? [])
const conversationId = computed(() => aiStore.currentSession?.serverConversationId ?? '')
// 会话数据任意变更（内容、错误态、当前会话指针切换）都防抖落盘
watch(
  [() => aiStore.sessions, () => aiStore.currentSessionId],
  () => aiStore.scheduleSave(),
  { deep: true }
)

const draft = ref('')
const pendingImages = ref([])   // { file: File, previewUrl: blobURL }
const sending = ref(false)
const uploading = ref(false)    // 图片上传到文件服务阶段
const MAX_IMAGES = 4
const MAX_IMAGE_SIZE = 8 * 1024 * 1024
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
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

// 释放待发图片的本地预览 URL，防止 blob 引用泄漏
function disposePending(list) {
  list.forEach((x) => x.previewUrl && URL.revokeObjectURL(x.previewUrl))
}

// 选图 -> 只生成本地 blob 预览；真正的上传推迟到点击发送时
function onPickFiles(e) {
  const files = Array.from(e.target.files || [])
  files.forEach((file) => {
    if (pendingImages.value.length >= MAX_IMAGES) {
      ElMessage.warning(`最多上传 ${MAX_IMAGES} 张图片`)
      return
    }
    if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
      ElMessage.warning(`「${file.name}」仅支持 jpg/png/gif/webp 格式`)
      return
    }
    if (file.size > MAX_IMAGE_SIZE) {
      ElMessage.warning(`「${file.name}」超过 8MB，请压缩后再发`)
      return
    }
    pendingImages.value.push({ file, previewUrl: URL.createObjectURL(file) })
  })
  e.target.value = ''
}

function removeImage(i) {
  const [removed] = pendingImages.value.splice(i, 1)
  if (removed) URL.revokeObjectURL(removed.previewUrl)
}

async function onSend() {
  if (!canSend.value) return
  const question = draft.value.trim()
  const pending = pendingImages.value

  // 1.先把图片上传到文件服务，拿到可访问 URL；失败则保留草稿与待发图片，不产生消息
  let imageUrls = []
  if (pending.length) {
    sending.value = true
    uploading.value = true
    try {
      imageUrls = await Promise.all(pending.map((x) => uploadFile(x.file)))
    } catch {
      // request 拦截器已弹错误提示，这里只复位状态；草稿与待发图片保留以便重试
      sending.value = false
      uploading.value = false
      return
    } finally {
      uploading.value = false
    }
  }

  // 2.用 URL 组装消息并请求 AI（请求体仅几百字节，不再携带 base64）
  const userMsg = { id: nextId(), role: 'user', content: question, images: imageUrls, pending: false }
  // aiMsg 必须用 reactive 包裹：push 进 store 后，requestAi 里对它的 content/pending 赋值
  // 要能触发响应式更新与 deep watch 落盘——裸对象赋值会绕过代理，导致回答已到达但界面冻结在加载态
  const aiMsg = reactive({ id: nextId(), role: 'assistant', content: '', images: [], pending: true, error: '' })
  aiStore.ensureSession()
  messages.value.push(userMsg, aiMsg)
  aiStore.touch(question) // 首条用户消息作为会话标题，并刷新 updatedAt
  draft.value = ''
  disposePending(pending)
  pendingImages.value = []
  sending.value = true
  scrollToBottom()

  await requestAi(question, imageUrls, aiMsg)
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
    // 所有链路统一携带会话 id（图文也走记忆），响应统一为 {conversationId, answer}
    const data = await chatWithAi({ question, images, conversationId: conversationId.value })
    // 保存后端会话 id 到当前会话，供后续轮次回传（watch 自动持久化）
    if (aiStore.currentSession) aiStore.currentSession.serverConversationId = data.conversationId
    aiMsg.content = data.answer
  } catch (err) {
    aiMsg.error = err?.response?.data?.message || 'AI 暂时开小差了，请稍后重试'
  } finally {
    aiMsg.pending = false
    sending.value = false
    scrollToBottom()
  }
}

// 开新会话：旧会话归档保留（将来可在历史列表查看），切换到空白新会话
function newConversation() {
  aiStore.newSession()
  draft.value = ''
  disposePending(pendingImages.value)
  pendingImages.value = []
  // 旧会话可能停留在底部滚动位置，空态欢迎内容在顶部，需瞬时复位
  nextTick(() => {
    const box = scrollRef.value
    if (box) box.scrollTo({ top: 0 })
  })
}

// ---------- 历史会话下拉 ----------
const historyOpen = ref(false)
const historyWrapRef = ref(null)
// 只有产生过消息的会话才进历史，按最后更新时间倒序
const historyList = computed(() =>
  [...aiStore.sessions]
    .filter((s) => s.messages.length > 0)
    .sort((a, b) => b.updatedAt - a.updatedAt)
)

function toggleHistory() {
  historyOpen.value = !historyOpen.value
}

// 点击历史项：切换会话，messages 是指向 store 的 computed 会自动更新
async function openSession(id) {
  historyOpen.value = false
  if (id === aiStore.currentSessionId) return
  aiStore.switchSession(id)
  await nextTick()
  scrollToBottom()
}

// 相对时间：今天显示时分，昨天显式标注，更早显示月-日
function formatTime(ts) {
  const d = new Date(ts)
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  const hm = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  if (d.toDateString() === now.toDateString()) return `今天 ${hm}`
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) return `昨天 ${hm}`
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

// 点击面板外部或按 Esc 关闭
function onDocClick(e) {
  if (historyOpen.value && historyWrapRef.value && !historyWrapRef.value.contains(e.target)) {
    historyOpen.value = false
  }
}
function onKeydown(e) {
  if (e.key === 'Escape') historyOpen.value = false
}
onMounted(() => {
  document.addEventListener('click', onDocClick)
  document.addEventListener('keydown', onKeydown)
})
onUnmounted(() => {
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onKeydown)
  // 组件销毁时兜底释放未发送的本地预览 URL（keep-alive 休眠不触发，待发图片保留符合预期）
  disposePending(pendingImages.value)
})
</script>

<style scoped>
.ai-chat {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--hd-paper);
}

/* ---------- 顶部 ---------- */
.chat-header {
  flex-shrink: 0;
  padding: var(--hd-space-3) var(--hd-space-3) var(--hd-space-2);
  background: var(--hd-paper);
  border-bottom: var(--hd-hairline-strong);
}
.header-inner {
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--hd-space-3);
}
.header-kicker {
  margin: 0 0 8px;
  font-family: var(--hd-font-mono);
  font-size: var(--hd-text-overline);
  line-height: var(--hd-text-overline-line);
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--hd-primary-700);
}
.header-title {
  margin: 0;
  font-size: 28px;
  line-height: 1.25;
  letter-spacing: 0.05em;
  color: var(--hd-ink);
}
.header-sub {
  margin: 7px 0 0;
  font-size: var(--hd-text-caption);
  line-height: var(--hd-text-caption-line);
  letter-spacing: 0.03em;
  color: var(--hd-neutral-500);
}

/* ---------- 顶部按钮组 + 历史下拉 ---------- */
/* 历史 / 新会话：同一对等宽文字按钮，细发线分隔，hover 转陶土 */
.history-btn,
.new-chat-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 4px;
  font-family: var(--hd-font-mono);
  font-size: var(--hd-text-caption);
  letter-spacing: 0.14em;
  color: var(--hd-neutral-500);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color var(--hd-duration-fast) ease-out;
}
.history-btn:hover,
.history-btn.is-open,
.new-chat-btn:hover {
  color: var(--hd-primary-700);
}
.new-chat-btn {
  padding-left: var(--hd-space-2);
  border-left: var(--hd-hairline);
}
.history-btn svg,
.new-chat-btn svg {
  transition: transform var(--hd-duration-slow) cubic-bezier(0.22, 0.61, 0.36, 1);
}
.history-btn.is-open svg {
  transform: rotate(-30deg);
}
.new-chat-btn:hover svg {
  transform: scale(1.18);
}
.header-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}
.history-wrap {
  position: relative;
}
.history-panel {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  z-index: 20;
  width: 324px;
  max-height: 380px;
  overflow-y: auto;
  padding: 2px 14px 8px;
  background: var(--hd-paper);
  border: var(--hd-hairline-strong);
  border-radius: var(--hd-radius-base);
  box-shadow: 0 18px 40px -18px rgba(33, 29, 24, 0.28);
}
.history-panel-title {
  margin: 0;
  padding: 12px 2px 10px;
  border-bottom: var(--hd-hairline);
  font-family: var(--hd-font-mono);
  font-size: var(--hd-text-overline);
  letter-spacing: 0.16em;
  color: var(--hd-neutral-400);
}
.history-empty {
  padding: 20px 2px 24px;
  text-align: center;
  font-size: var(--hd-text-caption);
  letter-spacing: 0.06em;
  color: var(--hd-neutral-400);
}
.history-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  width: 100%;
  padding: 11px 2px;
  background: transparent;
  border: none;
  border-bottom: var(--hd-hairline);
  cursor: pointer;
  text-align: left;
}
.history-item:last-child {
  border-bottom: none;
}
.history-item-title {
  width: 100%;
  font-size: var(--hd-text-body);
  color: var(--hd-neutral-700);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color var(--hd-duration-fast) ease-out;
}
.history-item:hover .history-item-title {
  color: var(--hd-primary-700);
}
.history-item.active::before {
  /* 当前会话：陶土竖线，与侧边导航语言一致 */
  content: '';
  position: absolute;
  left: -14px;
  top: 12px;
  bottom: 12px;
  width: 2px;
  background: var(--hd-primary-600);
}
.history-item.active .history-item-title {
  color: var(--hd-primary-700);
  font-weight: var(--hd-font-weight-medium);
}
.history-item-meta {
  font-family: var(--hd-font-mono);
  font-size: var(--hd-text-overline);
  letter-spacing: 0.08em;
  color: var(--hd-neutral-400);
}

/* 下拉进场动效 */
.history-drop-enter-active {
  transition: opacity 0.18s ease-out, transform 0.18s ease-out;
}
.history-drop-leave-active {
  transition: opacity 0.12s ease-in, transform 0.12s ease-in;
}
.history-drop-enter-from,
.history-drop-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ---------- 消息滚动区（草图纸点阵） ---------- */
.chat-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  /* 滚动位置完全由代码管理（发消息滚底/新会话回顶），关闭浏览器滚动锚定避免它偷改 scrollTop */
  overflow-anchor: none;
  padding: var(--hd-space-4) var(--hd-space-3);
  background-color: var(--hd-paper);
  background-image: radial-gradient(rgba(69, 62, 51, 0.06) 1px, transparent 1.2px);
  background-size: 26px 26px;
}
.msg-list {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* ---------- 空状态：杂志目录式开场 ---------- */
.welcome {
  max-width: 620px;
  margin: 0 auto;
  padding: clamp(24px, 6vh, 72px) 0 0;
}
.welcome-kicker {
  margin: 0 0 10px;
  font-family: var(--hd-font-mono);
  font-size: var(--hd-text-overline);
  line-height: var(--hd-text-overline-line);
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: var(--hd-primary-700);
}
.welcome-title {
  margin: 0;
  font-size: clamp(28px, 4vw, 38px);
  line-height: 1.25;
  letter-spacing: 0.06em;
  color: var(--hd-ink);
}
.welcome-rule {
  width: 40px;
  height: 1px;
  margin: 14px 0;
  background: var(--hd-primary-600);
  transform-origin: left center;
  animation: hd-line 0.6s 0.18s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}
.welcome-desc {
  margin: 0 0 var(--hd-space-2);
  font-size: var(--hd-text-body);
  letter-spacing: 0.02em;
  color: var(--hd-neutral-500);
}
.suggest-list {
  border-top: var(--hd-hairline);
}
.suggest-item {
  display: flex;
  align-items: baseline;
  gap: var(--hd-space-2);
  width: 100%;
  padding: 15px 4px;
  background: transparent;
  border: none;
  border-bottom: var(--hd-hairline);
  color: var(--hd-neutral-700);
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}
.suggest-no {
  flex-shrink: 0;
  width: 44px;
  font-family: var(--hd-font-mono);
  font-size: var(--hd-text-overline);
  letter-spacing: 0.12em;
  color: var(--hd-neutral-400);
  transition: color var(--hd-duration-fast) ease-out;
}
.suggest-text {
  flex: 1;
  min-width: 0;
  font-size: var(--hd-text-body);
  letter-spacing: 0.02em;
  transition: color var(--hd-duration-fast) ease-out;
}
.suggest-arrow {
  flex-shrink: 0;
  color: var(--hd-primary-600);
  opacity: 0;
  transform: translateX(-8px);
  transition: opacity var(--hd-duration-fast) ease-out,
              transform var(--hd-duration-slow) cubic-bezier(0.22, 0.61, 0.36, 1);
}
.suggest-item:hover .suggest-no,
.suggest-item:hover .suggest-text {
  color: var(--hd-primary-700);
}
.suggest-item:hover .suggest-arrow {
  opacity: 1;
  transform: translateX(0);
}

/* ---------- 消息：稿件往还 ---------- */
.msg-row { display: flex; }
.msg-row.user { justify-content: flex-end; }
.msg-row.assistant { justify-content: flex-start; }

/* 双方对话框同色同形：米白纸卡片 + 细发线，仅以陶土竖线的左右位置区分发言方 */
.bubble {
  position: relative;
  max-width: 78%;
  padding: 20px 24px;
  border-radius: var(--hd-radius-base);
  font-size: var(--hd-text-body);
  line-height: var(--hd-text-body-line);
  word-break: break-word;
  background: #fffdf9;
  color: var(--hd-ink);
  border: 1px solid var(--hd-neutral-200);
  box-shadow: 0 1px 2px rgba(33, 29, 24, 0.05);
}
/* AI：陶土竖线在左 */
.msg-row.assistant .bubble {
  border-left: 2px solid var(--hd-primary-600);
}
/* 用户：陶土竖线在右 */
.msg-row.user .bubble {
  border-right: 2px solid var(--hd-primary-600);
}
.bubble-text { margin: 0; }
.bubble-text:empty { display: none; }

/* AI 气泡内的 Markdown 排版 */
.markdown-body :first-child { margin-top: 0; }
.markdown-body :last-child { margin-bottom: 0; }
.markdown-body p { margin: 0 0 10px; line-height: var(--hd-text-body-line); }
.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4 {
  margin: 16px 0 8px;
  line-height: 1.4;
  color: inherit;
  font-weight: var(--hd-font-weight-medium);
}
.markdown-body h1 { font-size: 19px; }
.markdown-body h2 { font-size: 18px; }
.markdown-body h3 { font-size: 17px; }
.markdown-body h4 { font-size: var(--hd-text-body); }
.markdown-body ul,
.markdown-body ol { margin: 8px 0; padding-left: 22px; }
.markdown-body li { margin: 4px 0; }
.markdown-body li::marker { color: var(--hd-primary-600); }
.markdown-body strong { font-weight: var(--hd-font-weight-medium); color: var(--hd-primary-900); }
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
  margin-bottom: 10px;
}
.bubble-images:last-child { margin-bottom: 0; }
.bubble-img {
  width: 136px;
  height: 136px;
  object-fit: cover;
  border-radius: var(--hd-radius-base);
  cursor: zoom-in;
  border: 1px solid var(--hd-neutral-200);
}

/* AI 正在输入三点：陶土 */
.typing { display: inline-flex; gap: 5px; padding: 4px 0; }
.typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--hd-primary-400);
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
  background: var(--hd-ink);
  color: var(--hd-on-primary);
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color var(--hd-duration-fast) ease-out;
}
.thumb-remove:hover {
  background: var(--hd-primary-700);
}

/* ---------- 输入区 ---------- */
.chat-input-bar {
  flex-shrink: 0;
  padding: var(--hd-space-2) var(--hd-space-3) var(--hd-space-2);
  background: var(--hd-paper);
  border-top: var(--hd-hairline-strong);
}
.input-inner {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  align-items: flex-end;
  gap: var(--hd-space-1);
  background: #fffdf9;
  border: 1px solid var(--hd-neutral-300);
  border-radius: var(--hd-radius-base);
  padding: 6px 8px;
  transition: border-color var(--hd-duration-fast) ease-out,
              box-shadow var(--hd-duration-fast) ease-out;
}
.input-inner:focus-within {
  border-color: var(--hd-primary-400);
  box-shadow: 0 0 0 3px rgba(201, 118, 67, 0.12);
}

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
  border: 1px solid var(--hd-ink);
  border-radius: var(--hd-radius-base);
  padding: 8px 22px;
  background: var(--hd-ink);
  color: var(--hd-on-primary);
  font-size: var(--hd-text-caption);
  font-family: inherit;
  font-weight: var(--hd-font-weight-medium);
  letter-spacing: 0.18em;
  cursor: pointer;
  transition: background-color var(--hd-duration-fast),
              border-color var(--hd-duration-fast),
              opacity var(--hd-duration-fast);
}
.send-btn:hover:not(:disabled) {
  background: var(--hd-primary-700);
  border-color: var(--hd-primary-700);
}
.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }

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
  margin: 8px auto 0;
  font-family: var(--hd-font-mono);
  font-size: var(--hd-text-overline);
  letter-spacing: 0.08em;
  color: var(--hd-neutral-400);
}

/* ---------- 动效 ---------- */
@keyframes spin { to { transform: rotate(360deg); } }

@keyframes typing-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.45; }
  30% { transform: translateY(-5px); opacity: 1; }
}

/* 消息进场（TransitionGroup），曲线与全站 hd-rise 一致 */
.msg-enter-active { transition: opacity 0.32s cubic-bezier(0.22, 0.61, 0.36, 1),
                             transform 0.32s cubic-bezier(0.22, 0.61, 0.36, 1); }
.msg-enter-from { opacity: 0; transform: translateY(14px); }

/* ---------- 窄屏 ---------- */
@media (max-width: 720px) {
  .chat-header { padding: var(--hd-space-2) var(--hd-space-2) var(--hd-space-2); }
  .header-inner { flex-direction: column; align-items: stretch; gap: var(--hd-space-1); }
  .header-actions { justify-content: space-between; }
  .header-title { font-size: 24px; }
  .chat-scroll { padding: var(--hd-space-3) var(--hd-space-2); }
  .bubble { max-width: 88%; padding: 16px 18px; }
  .welcome { padding-top: var(--hd-space-4); }
  .history-panel { width: min(300px, 78vw); }
  .chat-input-bar { padding: var(--hd-space-1) var(--hd-space-2) var(--hd-space-2); }
  .pending-bar { padding-left: var(--hd-space-2); padding-right: var(--hd-space-2); }
  .input-hint { display: none; }
}
</style>
