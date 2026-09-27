import { defineStore } from 'pinia'
import { getUserIdFromToken } from '../utils/storage'

// 本地持久化键按 userId 隔离：不同账号各存一份，杜绝同机换账号看到他人会话
const storageKeyFor = (userId) => `hd:ai-chat:sessions:${userId}`
// 旧版本使用的无用户隔离键，初始化时清理一次
const LEGACY_STORAGE_KEY = 'hd:ai-chat:sessions'
const SAVE_DEBOUNCE_MS = 300
const TITLE_MAX_LEN = 20
const MAX_SESSIONS = 50 // 本地最多保留会话数，超出按 updatedAt 淘汰最旧

let saveTimer = null

function createSession() {
  const now = Date.now()
  return {
    id: `s-${now}-${Math.random().toString(36).slice(2, 8)}`,
    title: '', // 取首条用户消息前若干字，供将来历史会话列表展示
    messages: [], // { id, role, content, images, pending, error }
    serverConversationId: '', // 后端 Redis 记忆会话 id，仅纯文本多轮对话使用
    createdAt: now,
    updatedAt: now
  }
}

// 从 localStorage 恢复指定用户的数据；数据损坏时静默降级为空，不影响页面渲染
function restore(userId) {
  const empty = { sessions: [], currentSessionId: null }
  if (!userId) return empty
  try {
    const raw = localStorage.getItem(storageKeyFor(userId))
    if (!raw) return empty
    const parsed = JSON.parse(raw)
    const sessions = Array.isArray(parsed?.sessions) ? parsed.sessions : []
    if (sessions.length === 0) return empty
    // 当前指针失效（旧数据/被淘汰）时，回退到最近更新的会话
    const currentSessionId = sessions.some((s) => s.id === parsed.currentSessionId)
      ? parsed.currentSessionId
      : [...sessions].sort((a, b) => b.updatedAt - a.updatedAt)[0].id
    return { sessions, currentSessionId }
  } catch (e) {
    console.warn('[ai-chat] 本地会话恢复失败，已忽略', e)
    return empty
  }
}

// 序列化快照：发送中的消息转为可重试错误态，避免刷新/重开后永久转圈
function toSnapshot(state) {
  return {
    currentSessionId: state.currentSessionId,
    sessions: state.sessions.map((s) => ({
      ...s,
      messages: s.messages.map((m) =>
        m.pending ? { ...m, pending: false, error: m.error || '请求已中断，请重试' } : m
      )
    }))
  }
}

// 写 localStorage；图片为 base64 dataURL 体积大，超配额时逐级降级：
// 1) 剥离全部图片只留文字 → 2) 仅保留最近一个会话的文字
function writeStorage(userId, state) {
  const write = (snapshot) => localStorage.setItem(storageKeyFor(userId), JSON.stringify(snapshot))
  try {
    write(toSnapshot(state))
    return
  } catch (e) {
    if (!String(e?.name || '').includes('Quota') && e?.code !== 22) {
      console.warn('[ai-chat] 本地会话持久化失败', e)
      return
    }
  }
  try {
    const textOnly = toSnapshot(state)
    textOnly.sessions.forEach((s) => s.messages.forEach((m) => { m.images = [] }))
    write(textOnly)
  } catch (_) {
    try {
      const latest = [...state.sessions].sort((a, b) => b.updatedAt - a.updatedAt)[0]
      if (!latest) {
        localStorage.removeItem(storageKeyFor(userId))
        return
      }
      write({
        currentSessionId: latest.id,
        sessions: [
          {
            ...latest,
            messages: latest.messages.map((m) => ({ ...m, images: [] }))
          }
        ]
      })
    } catch (err) {
      console.warn('[ai-chat] 本地会话持久化彻底失败', err)
    }
  }
}

export const useAiChatStore = defineStore('ai-chat', {
  state: () => {
    // 清理旧版本无用户隔离的残留键（key 不存在时无副作用）
    localStorage.removeItem(LEGACY_STORAGE_KEY)
    const ownerUserId = getUserIdFromToken()
    return { ownerUserId, ...restore(ownerUserId) }
  },
  getters: {
    currentSession: (state) =>
      state.sessions.find((s) => s.id === state.currentSessionId) || null
  },
  actions: {
    // 登录后/进入对话页时绑定当前用户：用户变化则丢弃内存数据并加载该用户的历史
    bindUser(userId) {
      if (!userId) return
      if (userId === this.ownerUserId) return
      if (saveTimer) {
        clearTimeout(saveTimer)
        saveTimer = null
      }
      this.ownerUserId = userId
      Object.assign(this, restore(userId))
    },
    // 退出登录：清空内存中的会话（各人的数据仍留在各自的 localStorage key 中）
    reset() {
      if (saveTimer) {
        clearTimeout(saveTimer)
        saveTimer = null
      }
      this.ownerUserId = null
      this.sessions = []
      this.currentSessionId = null
    },
    // 确保存在当前会话（进入空状态发首条消息时调用）
    ensureSession() {
      // 防御：理论上路由守卫保证已有登录态，这里兜底补绑一次
      if (!this.ownerUserId) this.bindUser(getUserIdFromToken())
      const existing = this.currentSession
      if (existing) return existing
      const session = createSession()
      this.sessions.push(session)
      this.currentSessionId = session.id
      return session
    },
    // 开新会话：当前会话有消息才新建，避免连续产生空会话
    newSession() {
      const current = this.currentSession
      if (!current || current.messages.length > 0) {
        const session = createSession()
        this.sessions.push(session)
        this.currentSessionId = session.id
      }
      this.gc()
    },
    // 切换到指定历史会话（供将来历史会话列表使用）
    switchSession(id) {
      if (this.sessions.some((s) => s.id === id)) {
        this.currentSessionId = id
      }
    },
    // 用首条用户消息生成标题并刷新更新时间
    touch(titleSeed) {
      const current = this.currentSession
      if (!current) return
      current.updatedAt = Date.now()
      if (!current.title && titleSeed) {
        current.title = String(titleSeed).slice(0, TITLE_MAX_LEN)
      }
    },
    // 超出本地会话上限时淘汰最旧会话（不淘汰当前会话）
    gc() {
      if (this.sessions.length <= MAX_SESSIONS) return
      const oldestIds = new Set(
        [...this.sessions]
          .sort((a, b) => a.updatedAt - b.updatedAt)
          .slice(0, this.sessions.length - MAX_SESSIONS)
          .map((s) => s.id)
      )
      oldestIds.delete(this.currentSessionId)
      if (oldestIds.size > 0) {
        this.sessions = this.sessions.filter((s) => !oldestIds.has(s.id))
      }
    },
    // 防抖落盘，供组件在会话数据变更后调用；未绑定用户（已登出）时禁止写盘
    scheduleSave() {
      if (!this.ownerUserId) return
      if (saveTimer) clearTimeout(saveTimer)
      saveTimer = setTimeout(() => writeStorage(this.ownerUserId, this), SAVE_DEBOUNCE_MS)
    }
  }
})
