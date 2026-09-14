<template>
  <div class="page">
    <header class="page-header">
      <el-page-header @back="goBack">
        <template #content>
          <span class="title">{{ project?.name || '项目详情' }}</span>
        </template>
        <template #extra>
          <el-button
            v-if="project"
            type="primary"
            :loading="generating"
            :disabled="isGenerating"
            @click="onGenerate"
          >
            {{ isGenerating ? '生成中…' : '生成 3D 效果' }}
          </el-button>
          <el-button v-if="project" type="danger" plain :loading="deleting" @click="onDelete">
            删除项目
          </el-button>
        </template>
      </el-page-header>
    </header>

    <div v-if="project" class="layout">
      <!-- 左：详情 + 设计图 -->
      <section class="main-col">
        <div class="card">
          <h3 class="card-title">项目信息</h3>
          <ul class="info-list">
            <li><span class="label">项目名称</span>{{ project.name }}</li>
            <li><span class="label">项目描述</span>{{ project.description || '未填写' }}</li>
            <li><span class="label">装修风格</span>{{ project.styleLabel || project.style || '未设置' }}</li>
            <li><span class="label">创建时间</span>{{ formatTime(project.createdAt) }}</li>
            <li><span class="label">更新时间</span>{{ formatTime(project.updatedAt) }}</li>
          </ul>
        </div>

        <div class="card">
          <h3 class="card-title">原始户型图</h3>
          <img v-if="project.designImageUrl" :src="project.designImageUrl" class="design-img" alt="户型图" />
          <el-empty v-else description="该项目暂无设计图" />
        </div>
      </section>

      <!-- 右：3D 生成区 -->
      <aside class="side-col">
        <div class="card">
          <h3 class="card-title">3D 装修效果</h3>

          <!-- 生成中：加载状态 -->
          <div v-if="isGenerating" class="gen-state">
            <el-skeleton animated :rows="4" />
            <p class="gen-tip">
              {{ lastGen?.status === 'PROCESSING' ? 'AI 正在渲染，请稍候…' : '任务排队中，请稍候…' }}
            </p>
          </div>

          <!-- 生成失败 -->
          <div v-else-if="lastGen?.status === 'FAILED'" class="gen-state error">
            <p class="gen-error">生成失败：{{ lastGen.errorMessage || '未知错误' }}</p>
            <el-button type="primary" plain :loading="generating" @click="onGenerate">重新生成</el-button>
          </div>

          <!-- 生成成功 -->
          <div v-else-if="lastGen?.status === 'SUCCESS'" class="gen-state">
            <img :src="resultImg" class="gen-img" alt="装修效果图" />
            <div class="gen-actions">
              <a
                v-if="lastGen.modelUrl"
                :href="lastGen.modelUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="el-button el-button--primary el-button--small"
              >
                查看 3D 模型
              </a>
              <el-button
                v-if="lastGen.panoramaUrl"
                size="small"
                plain
                @click="panoramaVisible = true"
              >
                查看全景效果
              </el-button>
            </div>
          </div>

          <!-- 从未生成 -->
          <div v-else class="gen-state empty">
            <el-empty description="上传户型图后可一键生成 3D 装修效果" :image-size="80" />
          </div>
        </div>

        <!-- 生成历史 -->
        <div v-if="history.length" class="card">
          <h3 class="card-title">生成历史</h3>
          <ul class="history-list">
            <li v-for="g in history" :key="g.id" class="history-item" @click="viewGen(g)">
              <img
                v-if="g.previewImageUrl || g.panoramaUrl"
                :src="g.previewImageUrl || g.panoramaUrl"
                class="history-img"
                alt=""
              />
              <div v-else class="history-img placeholder">{{ statusText(g.status) }}</div>
              <div class="history-info">
                <span class="status" :class="statusClass(g.status)">{{ statusText(g.status) }}</span>
                <time class="time">{{ formatTime(g.createdAt) }}</time>
                <span v-if="g.status === 'FAILED'" class="err">{{ g.errorMessage }}</span>
              </div>
            </li>
          </ul>
        </div>
      </aside>
    </div>

    <!-- 全景效果大图 -->
    <el-dialog v-model="panoramaVisible" title="全景效果" width="min(720px, 92%)">
      <img v-if="lastGen?.panoramaUrl" :src="lastGen.panoramaUrl" class="panorama-img" alt="全景效果图" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import { getProject, deleteProject } from '../api/project'
import { generate, getGeneration, listProjectGenerations } from '../api/generation'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const project = ref(null)
const deleting = ref(false)
const generating = ref(false) // 发起请求中
const lastGen = ref(null) // 最近一次生成任务（当前查看对象）
const history = ref([])
const panoramaVisible = ref(false)
let timer = null

const isGenerating = computed(
  () => lastGen.value?.status === 'PENDING' || lastGen.value?.status === 'PROCESSING'
)

const resultImg = computed(
  () => lastGen.value?.panoramaUrl || lastGen.value?.previewImageUrl || ''
)

onMounted(async () => {
  userStore.userInfo ?? (await userStore.fetchMe())
  try {
    project.value = await getProject(route.params.id)
  } catch {
    router.replace('/')
    return
  }
  await loadHistory()
})

onBeforeUnmount(() => clearPolling())

/* ---- 3D 生成 ---- */
async function loadHistory() {
  history.value = await listProjectGenerations(project.value.id)
  // 默认展示最新一条
  if (history.value.length) {
    lastGen.value = history.value[0]
  }
}

async function onGenerate() {
  generating.value = true
  try {
    const gen = await generate(project.value.id)
    lastGen.value = gen
    await poll(gen.id)
    await loadHistory()
  } catch (e) {
    // 接口已弹错误提示，仅兜底
  } finally {
    generating.value = false
  }
}

// 每 3 秒轮询一次，直到终态
async function poll(id) {
  clearPolling()
  await new Promise((resolve) => {
    timer = setInterval(async () => {
      try {
        const gen = await getGeneration(id)
        lastGen.value = gen
        if (gen.status === 'SUCCESS' || gen.status === 'FAILED') {
          clearPolling()
          if (gen.status === 'SUCCESS') ElMessage.success('3D 效果生成完成')
          resolve()
        }
      } catch {
        // 单次查询失败不中断，继续轮询
      }
    }, 3000)
  })
}

function clearPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function viewGen(g) {
  lastGen.value = g
}

function statusText(s) {
  return { PENDING: '排队中', PROCESSING: '生成中', SUCCESS: '已完成', FAILED: '失败' }[s] || s
}

function statusClass(s) {
  return { SUCCESS: 'ok', FAILED: 'fail', PENDING: 'wait', PROCESSING: 'run' }[s] || ''
}

/* ---- 删除项目 ---- */
async function onDelete() {
  try {
    await ElMessageBox.confirm('确定删除该项目吗？删除后不可恢复。', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  deleting.value = true
  try {
    await deleteProject(route.params.id)
    ElMessage.success('项目已删除')
    router.push('/projects')
  } finally {
    deleting.value = false
  }
}

function goBack() {
  router.push('/projects')
}

function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.page {
  max-width: var(--hd-container-max);
  margin: 0 auto;
  padding: var(--hd-space-2);
}
.page-header {
  padding: var(--hd-space-1) 0 var(--hd-space-2);
}
.title {
  font-size: var(--hd-text-h3);
  font-weight: var(--hd-font-weight-medium);
}
.layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: var(--hd-space-2);
  align-items: start;
}
.main-col,
.side-col {
  display: flex;
  flex-direction: column;
  gap: var(--hd-space-2);
}
.card {
  background: #fff;
  border: 1px solid var(--hd-neutral-200);
  border-radius: var(--hd-radius-lg);
  padding: var(--hd-space-2);
}
.card-title {
  margin: 0 0 var(--hd-space-1);
  font-size: var(--hd-text-h3);
  line-height: var(--hd-text-h3-line);
  font-weight: var(--hd-font-weight-medium);
}
.info-list {
  list-style: none;
}
.info-list li {
  display: flex;
  gap: var(--hd-space-1);
  padding: 6px 0;
  font-size: var(--hd-text-body);
  line-height: var(--hd-text-body-line);
}
.info-list .label {
  flex-shrink: 0;
  width: 64px;
  color: var(--hd-neutral-500);
}
.design-img {
  width: 100%;
  max-height: 480px;
  object-fit: contain;
  border-radius: var(--hd-radius-base);
  background: var(--hd-neutral-100);
}
.gen-state {
  min-height: 200px;
}
.gen-state.empty {
  display: flex;
  align-items: center;
  justify-content: center;
}
.gen-state.error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--hd-space-1);
  justify-content: center;
}
.gen-tip {
  margin-top: var(--hd-space-1);
  color: var(--hd-neutral-500);
  text-align: center;
  font-size: var(--hd-text-caption);
}
.gen-error {
  color: var(--hd-danger);
  word-break: break-word;
}
.gen-img {
  width: 100%;
  border-radius: var(--hd-radius-base);
}
.gen-actions {
  display: flex;
  gap: 8px;
  margin-top: var(--hd-space-1);
}
.history-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.history-item {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 6px;
  border: 1px solid var(--hd-neutral-100);
  border-radius: var(--hd-radius-base);
  cursor: pointer;
  transition: border-color var(--hd-duration-fast) ease-out;
}
.history-item:hover {
  border-color: var(--hd-primary-200);
}
.history-img {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: var(--hd-radius-base);
  background: var(--hd-neutral-100);
  flex-shrink: 0;
}
.history-img.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--hd-neutral-400);
  font-size: var(--hd-text-overline);
}
.history-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.status {
  font-size: var(--hd-text-caption);
  font-weight: var(--hd-font-weight-medium);
}
.status.ok {
  color: var(--hd-success);
}
.status.fail {
  color: var(--hd-danger);
}
.status.wait,
.status.run {
  color: var(--hd-warning);
}
.time {
  color: var(--hd-neutral-400);
  font-size: var(--hd-text-overline);
}
.err {
  color: var(--hd-danger);
  font-size: var(--hd-text-overline);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.panorama-img {
  width: 100%;
  border-radius: var(--hd-radius-base);
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>