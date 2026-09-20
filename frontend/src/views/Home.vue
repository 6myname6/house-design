<template>
  <div class="page">
    <!-- 页头 -->
    <header class="page-head hd-rise">
      <div class="head-text">
        <p class="hd-overline">01 — Projects</p>
        <h2 class="head-title">我的项目</h2>
      </div>
      <button class="new-btn" type="button" @click="goNew">
        <span>新建项目</span>
        <span class="new-arrow" aria-hidden="true">→</span>
      </button>
    </header>

    <!-- 加载骨架 -->
    <div v-if="loading" class="entry-list">
      <div v-for="i in 4" :key="i" class="entry skeleton-entry">
        <div class="entry-cover sk-block"></div>
        <div class="entry-info">
          <div class="sk-line sk-w30"></div>
          <div class="sk-line sk-w60"></div>
          <div class="sk-line sk-w40"></div>
        </div>
      </div>
    </div>

    <!-- 错误态 -->
    <div v-else-if="loadError" class="state-box">
      <p class="state-title">项目加载失败</p>
      <button class="text-btn" type="button" @click="loadProjects">重新加载 →</button>
    </div>

    <!-- 空态 -->
    <div v-else-if="projects.length === 0" class="state-box hd-rise">
      <p class="hd-overline">Empty</p>
      <p class="state-title">还没有项目</p>
      <p class="state-desc">上传第一张户型图，创建你的第一个设计方案</p>
      <button class="ink-btn" type="button" @click="goNew">立即创建</button>
    </div>

    <!-- 项目编辑列表 -->
    <div v-else class="entry-list">
      <article
        v-for="(p, i) in projects"
        :key="p.id"
        class="entry hd-rise"
        :style="{ animationDelay: `${0.08 + i * 0.07}s` }"
        role="button"
        tabindex="0"
        @click="goDetail(p.id)"
        @keydown.enter="goDetail(p.id)"
      >
        <div class="entry-cover">
          <img v-if="p.designImageUrl" :src="p.designImageUrl" class="cover-img" alt="" loading="lazy" />
          <div v-else class="cover-empty">
            <span class="cover-empty-mark" aria-hidden="true">⌖</span>
            <span>暂无设计图</span>
          </div>
          <span class="entry-no">{{ String(i + 1).padStart(2, '0') }}</span>
        </div>
        <div class="entry-info">
          <p class="entry-style">{{ p.styleLabel || p.style || '未设置风格' }}</p>
          <h3 class="entry-name">{{ p.name }}</h3>
          <p v-if="p.description" class="entry-desc">{{ p.description }}</p>
          <div class="entry-meta">
            <time class="meta-time">{{ formatTime(p.createdAt) }}</time>
            <span class="meta-go" aria-hidden="true">查看详情 →</span>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { listProjects } from '../api/project'

const router = useRouter()
const userStore = useUserStore()

const projects = ref([])
const loading = ref(false)
const loadError = ref(false)

onMounted(async () => {
  userStore.userInfo ?? (await userStore.fetchMe())
  await loadProjects()
})

async function loadProjects() {
  loading.value = true
  loadError.value = false
  try {
    projects.value = await listProjects()
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function goNew() {
  router.push('/projects/new')
}

function goDetail(id) {
  router.push(`/projects/${id}`)
}

// ISO 时间格式化为 2026.09.14
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
}
.new-arrow {
  transition: transform var(--hd-duration-fast) ease-out;
}
.new-btn:hover { color: var(--hd-primary-700); }
.new-btn:hover .new-arrow { transform: translateX(5px); }

/* ---- 编辑条目 ---- */
.entry-list {
  margin-top: 4px;
}
.entry {
  display: grid;
  grid-template-columns: 248px 1fr;
  gap: var(--hd-space-3);
  padding: var(--hd-space-3) 0;
  border-bottom: var(--hd-hairline);
  cursor: pointer;
  outline: none;
  transition: background-color var(--hd-duration-fast) ease-out;
}
.entry:hover { background-color: rgba(221, 213, 198, 0.18); }
.entry:focus-visible { background-color: rgba(221, 213, 198, 0.28); }

.entry-cover {
  position: relative;
  aspect-ratio: 16 / 10;
  background: var(--hd-neutral-100);
  overflow: hidden;
  border-radius: var(--hd-radius-base);
}
.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.entry:hover .cover-img { transform: scale(1.04); }

.cover-empty {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--hd-neutral-400);
  font-size: var(--hd-text-caption);
  letter-spacing: 0.08em;
}
.cover-empty-mark { font-size: 22px; color: var(--hd-neutral-300); }

/* 图上编号：图纸标注感 */
.entry-no {
  position: absolute;
  top: 8px;
  left: 10px;
  font-family: var(--hd-font-mono);
  font-size: var(--hd-text-overline);
  letter-spacing: 0.15em;
  color: #fff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.55);
}

.entry-info {
  display: flex;
  flex-direction: column;
  padding: 4px 0;
  min-width: 0;
}
.entry-style {
  font-family: var(--hd-font-mono);
  font-size: var(--hd-text-overline);
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--hd-primary-700);
}
.entry-name {
  margin: 6px 0 8px;
  font-size: var(--hd-text-h2);
  line-height: var(--hd-text-h2-line);
  transition: color var(--hd-duration-fast);
}
.entry:hover .entry-name { color: var(--hd-primary-700); }

.entry-desc {
  color: var(--hd-neutral-500);
  font-size: var(--hd-text-caption);
  line-height: var(--hd-text-caption-line);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.entry-meta {
  margin-top: auto;
  padding-top: var(--hd-space-1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--hd-neutral-400);
  font-size: var(--hd-text-overline);
}
.meta-time { font-family: var(--hd-font-mono); letter-spacing: 0.08em; }
.meta-go {
  opacity: 0;
  transform: translateX(-6px);
  transition: opacity var(--hd-duration-fast), transform var(--hd-duration-fast);
  letter-spacing: 0.08em;
}
.entry:hover .meta-go {
  opacity: 1;
  transform: translateX(0);
  color: var(--hd-primary-700);
}

/* ---- 骨架（细线块）---- */
.skeleton-entry { cursor: default; }
.skeleton-entry:hover { background: none; }
.sk-block {
  background: linear-gradient(90deg, var(--hd-neutral-100) 25%, var(--hd-neutral-200) 37%, var(--hd-neutral-100) 63%);
  background-size: 400% 100%;
  animation: sk-shimmer 1.4s ease infinite;
}
.sk-line {
  height: 14px;
  margin-bottom: 12px;
  border-radius: 2px;
}
.sk-w30 { width: 30%; }
.sk-w60 { width: 60%; height: 20px; }
.sk-w40 { width: 40%; }
@keyframes sk-shimmer {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}

/* ---- 状态页 ---- */
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
.state-desc { color: var(--hd-neutral-500); font-size: var(--hd-text-caption); }
.ink-btn {
  margin-top: var(--hd-space-2);
  padding: 11px 28px;
  border: 1px solid var(--hd-ink);
  background: var(--hd-ink);
  color: #f7f4ee;
  font-family: inherit;
  font-size: var(--hd-text-caption);
  letter-spacing: 0.16em;
  cursor: pointer;
  transition: background-color var(--hd-duration-fast), color var(--hd-duration-fast);
}
.ink-btn:hover { background: var(--hd-primary-700); border-color: var(--hd-primary-700); }
.text-btn {
  border: none;
  background: none;
  color: var(--hd-primary-700);
  font-family: inherit;
  font-size: var(--hd-text-caption);
  letter-spacing: 0.08em;
  cursor: pointer;
}

/* ---- 窄屏：条目转纵向 ---- */
@media (max-width: 720px) {
  .page { padding: var(--hd-space-3) var(--hd-space-2) var(--hd-space-5); }
  .entry { grid-template-columns: 1fr; gap: var(--hd-space-1); }
  .entry-cover { aspect-ratio: 16 / 9; }
  .head-title { font-size: 28px; }
}
</style>
