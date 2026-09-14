<template>
  <div class="page">
    <!-- 顶栏：标题居左，新建按钮居右 -->
    <header class="page-header">
      <h2 class="title">我的项目</h2>
      <button class="new-project-btn" type="button" @click="goNew">
        <span aria-hidden="true">＋</span> 新建项目
      </button>
    </header>

    <!-- 加载骨架屏 -->
    <div v-if="loading" class="grid">
      <div v-for="i in 6" :key="i" class="card">
        <el-skeleton animated>
          <template #template>
            <el-skeleton-item variant="image" class="skeleton-img" />
            <el-skeleton-item variant="h3" class="skeleton-line" />
            <el-skeleton-item variant="text" class="skeleton-line short" />
          </template>
        </el-skeleton>
      </div>
    </div>

    <!-- 加载失败：错误态 + 重试 -->
    <div v-else-if="loadError" class="state-box">
      <el-empty description="项目加载失败">
        <el-button type="primary" plain @click="loadProjects">重新加载</el-button>
      </el-empty>
    </div>

    <!-- 空态 -->
    <div v-else-if="projects.length === 0" class="state-box">
      <el-empty description="还没有项目，去创建第一个吧">
        <el-button type="primary" @click="goNew">立即创建</el-button>
      </el-empty>
    </div>

    <!-- 项目卡片流 -->
    <div v-else class="grid">
      <article
        v-for="p in projects"
        :key="p.id"
        class="card"
        role="button"
        tabindex="0"
        @click="goDetail(p.id)"
        @keydown.enter="goDetail(p.id)"
      >
        <div class="cover-wrap">
          <img v-if="p.designImageUrl" :src="p.designImageUrl" class="cover" alt="" />
          <div v-else class="cover placeholder">暂无设计图</div>
        </div>
        <div class="info">
          <div class="name">{{ p.name }}</div>
          <div v-if="p.description" class="desc">{{ p.description }}</div>
          <div class="meta">
            <span class="label">{{ p.styleLabel || p.style || '未设置风格' }}</span>
            <time class="time">{{ formatTime(p.createdAt) }}</time>
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

// ISO 时间格式化为 2026-09-14 14:28
function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.page {
  max-width: var(--hd-container-max);
  margin: 0 auto;
  padding: var(--hd-space-2) var(--hd-space-2) var(--hd-space-4);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--hd-space-2);
  padding: var(--hd-space-1) 0 var(--hd-space-2);
}
.title {
  margin: 0;
  font-size: var(--hd-text-h1);
  line-height: var(--hd-text-h1-line);
  font-weight: var(--hd-font-weight-medium);
}

/* 新建项目按钮：实心橙色，居页头右侧 */
.new-project-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border: none;
  border-radius: var(--hd-radius-base);
  background: var(--hd-primary-600);
  color: #fff;
  font-size: var(--hd-text-caption);
  line-height: var(--hd-text-caption-line);
  cursor: pointer;
  transition: background-color var(--hd-duration-fast) ease-out, transform var(--hd-duration-fast) ease-out;
}
.new-project-btn:hover {
  background: var(--hd-primary-700);
}
.new-project-btn:active {
  transform: scale(0.98);
}

/* 卡片网格：免媒体查询自动换列 */
.grid {
  margin-top: var(--hd-space-2);
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: var(--hd-space-2);
}
.card {
  background: #fff;
  border-radius: var(--hd-radius-lg);
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--hd-neutral-200);
  transition: transform var(--hd-duration-fast) ease-out, box-shadow var(--hd-duration-fast) ease-out;
}
.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(92, 42, 14, 0.08);
}
.cover-wrap {
  aspect-ratio: 16 / 10;
  background: var(--hd-neutral-100);
}
.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--hd-neutral-400);
  font-size: var(--hd-text-caption);
}
.skeleton-img {
  width: 100%;
  aspect-ratio: 16 / 10;
}
.skeleton-line {
  margin: var(--hd-space-2);
}
.skeleton-line.short {
  width: 60%;
}

.info {
  padding: var(--hd-space-2);
}
.name {
  font-size: var(--hd-text-h3);
  line-height: var(--hd-text-h3-line);
  font-weight: var(--hd-font-weight-medium);
}
.desc {
  margin-top: 2px;
  color: var(--hd-neutral-500);
  font-size: var(--hd-text-caption);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.meta {
  margin-top: var(--hd-space-1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--hd-space-1);
}
.label {
  color: var(--hd-primary-700);
  font-size: var(--hd-text-overline);
  background: var(--hd-primary-50);
  padding: 2px 8px;
  border-radius: 999px;
}
.time {
  color: var(--hd-neutral-400);
  font-size: var(--hd-text-overline);
}

.state-box {
  padding: var(--hd-space-6) 0;
}

/* sm ≥576px：内容限宽居中 */
@media (min-width: 576px) {
  .page {
    max-width: var(--hd-content-max);
  }
}
/* lg ≥992px：容器限宽 1200，三列网格 */
@media (min-width: 992px) {
  .page {
    max-width: var(--hd-container-max);
  }
}
</style>