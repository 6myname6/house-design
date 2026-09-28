<template>
  <div class="viewer" @mousemove="onMouseMove">
    <!-- 加载 / 排队 -->
    <div v-if="loading" class="state-box">
      <div class="spinner" aria-hidden="true"></div>
      <p class="state-title">{{ pendingText }}</p>
      <p class="state-sub">AI 正在逐间渲染整套房源，请稍候</p>
    </div>

    <!-- 失败 / 无效 -->
    <div v-else-if="errorText" class="state-box">
      <p class="state-title error">效果加载失败</p>
      <p class="state-sub">{{ errorText }}</p>
      <button class="ink-btn" @click="goBack">返回项目</button>
    </div>

    <!-- v1 旧任务：单图降级，无热点 -->
    <template v-else-if="scene && (!scene.rooms || !scene.rooms.length)">
      <img v-if="gen.panoramaUrl" :src="gen.panoramaUrl" class="legacy-img" alt="装修效果图" />
      <header class="hud hud-top">
        <button class="ghost-btn" @click="goBack">← 返回</button>
        <span class="room-tag">单张效果图</span>
      </header>
    </template>

    <!-- photo-tour v2：多房间照片漫游 -->
    <template v-else-if="scene && currentRoom">
      <!-- 舞台：外层鼠标视差，内层 Ken Burns 缓慢扫视 -->
      <div class="stage">
        <div class="parallax-layer" :style="parallaxStyle">
          <transition name="room-fade">
            <img
              :key="currentRoom.id"
              :src="currentRoom.imageUrl"
              class="room-image"
              :class="`drift-${driftDirection}`"
              alt="房间效果图"
              @error="onImgError"
              draggable="false"
            />
          </transition>
        </div>

        <!-- 渐变压暗，保证 HUD/圆点可读 -->
        <div class="vignette" aria-hidden="true"></div>

        <!-- 房间跳转圆点 -->
        <button
          v-for="h in currentRoom.hotspots"
          :key="h.targetRoomId"
          class="hotspot"
          :style="{ left: h.x * 100 + '%', top: h.y * 100 + '%' }"
          @click="goRoom(h.targetRoomId)"
        >
          <span class="hotspot-ring" aria-hidden="true"></span>
          <span class="hotspot-dot" aria-hidden="true"></span>
          <span class="hotspot-label">{{ h.label }}</span>
        </button>

        <!-- 顶部 HUD：返回 / 房间信息 -->
        <header class="hud hud-top">
          <button class="ghost-btn" @click="goBack">← 返回项目</button>
          <div class="room-meta">
            <span class="room-no">{{ roomIndexText }}</span>
            <strong class="room-name">{{ currentRoom.name }}</strong>
            <span v-if="currentRoom.approxArea" class="room-area">{{ currentRoom.approxArea }}</span>
          </div>
          <button class="ghost-btn icon-only" :title="mapOpen ? '收起户型图' : '查看户型图'" @click="mapOpen = !mapOpen">
            {{ mapOpen ? '▣ 收起' : '▥ 户型' }}
          </button>
        </header>

        <!-- 首次引导：点圆点进入其他房间 -->
        <transition name="hint-fade">
          <p v-if="showHint" class="hint">点击画面中的呼吸圆点，走进其他房间 →</p>
        </transition>

        <!-- 户型小地图 -->
        <transition name="map-slide">
          <aside v-if="mapOpen && scene.floorPlanImageUrl" class="mini-map">
            <p class="map-title">户型导览</p>
            <div class="map-canvas">
              <img :src="scene.floorPlanImageUrl" alt="户型图" @error="mapOpen = false" draggable="false" />
              <div
                v-for="r in scene.rooms"
                :key="'mb-' + r.id"
                class="map-block"
                :class="{ active: r.id === currentRoom.id }"
                :style="bboxStyle(r.bbox)"
              >
                <span v-if="!r.bbox" class="map-block-name">{{ r.name }}</span>
              </div>
            </div>
            <ul class="map-legend">
              <li
                v-for="r in scene.rooms"
                :key="'lg-' + r.id"
                :class="{ active: r.id === currentRoom.id }"
                @click="goRoom(r.id)"
              >
                {{ r.name }}
              </li>
            </ul>
          </aside>
        </transition>

        <!-- 底部房间缩略图条 -->
        <footer class="hud hud-bottom">
          <button
            v-for="(r, i) in scene.rooms"
            :key="'thumb-' + r.id"
            class="thumb"
            :class="{ active: r.id === currentRoom.id }"
            @click="goRoom(r.id)"
          >
            <img :src="r.imageUrl" :alt="r.name" draggable="false" />
            <span class="thumb-name">
              <em>{{ String(i + 1).padStart(2, '0') }}</em>{{ r.name }}
            </span>
          </button>
        </footer>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getGeneration } from '../api/generation'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const gen = ref(null)
const scene = ref(null) // 解析后的 sceneConfig 对象
const currentRoomId = ref(null)
const mapOpen = ref(true)
const showHint = ref(false)
const driftDirection = ref('right')
const imgError = ref(false)
const mouse = ref({ x: 0, y: 0 })

let timer = null
let hintTimer = null

const currentRoom = computed(() => {
  if (!scene.value?.rooms) return null
  return scene.value.rooms.find((r) => r.id === currentRoomId.value) || null
})

const roomIndexText = computed(() => {
  if (!scene.value?.rooms || !currentRoom.value) return ''
  const i = scene.value.rooms.findIndex((r) => r.id === currentRoom.value.id)
  return `${String(i + 1).padStart(2, '0')} / ${String(scene.value.rooms.length).padStart(2, '0')}`
})

const pendingText = computed(() =>
  gen.value?.status === 'PROCESSING' ? 'AI 正在渲染…' : '排队中…'
)

const errorText = computed(() => {
  if (imgError.value) return '效果图加载失败，请返回后重试'
  if (!gen.value) return ''
  if (gen.value.status === 'FAILED') return gen.value.errorMessage || '生成失败，请重新生成'
  if (gen.value.status !== 'SUCCESS') return ''
  if (!scene.value) return '场景数据异常，请重新生成'
  if (scene.value.rooms?.length && !currentRoom.value) return '入口房间数据缺失'
  return ''
})

const parallaxStyle = computed(() => ({
  transform: `translate(${mouse.value.x}px, ${mouse.value.y}px)`
}))

onMounted(async () => {
  await load()
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  clearPolling()
  clearTimeout(hintTimer)
  window.removeEventListener('keydown', onKeydown)
})

async function load() {
  loading.value = true
  try {
    gen.value = await getGeneration(route.params.generationId)
  } catch {
    loading.value = false
    gen.value = null
    return
  }
  loading.value = false
  if (gen.value.status === 'PENDING' || gen.value.status === 'PROCESSING') {
    poll()
    return
  }
  if (gen.value.status === 'SUCCESS') {
    enterScene()
  }
}

// 每 3 秒轮询至终态（支持直接把 /viewer/:id 发给别人/刷新）
function poll() {
  clearPolling()
  timer = setInterval(async () => {
    try {
      gen.value = await getGeneration(route.params.generationId)
      if (gen.value.status === 'PENDING' || gen.value.status === 'PROCESSING') return
      clearPolling()
      if (gen.value.status === 'SUCCESS') enterScene()
    } catch {
      clearPolling()
    }
  }, 3000)
}

function clearPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

// 解析 sceneConfig，定位入口房间
function enterScene() {
  let parsed = null
  try {
    parsed = gen.value.sceneConfig ? JSON.parse(gen.value.sceneConfig) : null
  } catch {
    parsed = null
  }
  scene.value = parsed
  if (parsed?.rooms?.length) {
    const entry = parsed.rooms.some((r) => r.id === parsed.entryRoomId)
      ? parsed.entryRoomId
      : parsed.rooms[0].id
    currentRoomId.value = entry
    showHint.value = true
    clearTimeout(hintTimer)
    hintTimer = setTimeout(() => (showHint.value = false), 6000)
  }
}

// 切换房间：交替扫视方向，重置引导
function goRoom(roomId) {
  if (!roomId || roomId === currentRoomId.value) return
  driftDirection.value = driftDirection.value === 'right' ? 'left' : 'right'
  currentRoomId.value = roomId
  showHint.value = false
}

// 缩略图顺序上的前后切换（键盘 ← →）
function stepRoom(delta) {
  const rooms = scene.value?.rooms
  if (!rooms?.length) return
  const i = rooms.findIndex((r) => r.id === currentRoomId.value)
  const next = rooms[(i + delta + rooms.length) % rooms.length]
  goRoom(next.id)
}

function onKeydown(e) {
  if (e.key === 'Escape') {
    goBack()
  } else if (e.key === 'ArrowRight') {
    stepRoom(1)
  } else if (e.key === 'ArrowLeft') {
    stepRoom(-1)
  }
}

// 轻微鼠标视差（最大 ±14px），让静态照片产生空间纵深
function onMouseMove(e) {
  const x = (e.clientX / window.innerWidth - 0.5) * -14
  const y = (e.clientY / window.innerHeight - 0.5) * -10
  mouse.value = { x: x.toFixed(1), y: y.toFixed(1) }
}

function bboxStyle(bbox) {
  if (!bbox) return { display: 'none' }
  return {
    left: bbox.x * 100 + '%',
    top: bbox.y * 100 + '%',
    width: bbox.w * 100 + '%',
    height: bbox.h * 100 + '%'
  }
}

function onImgError() {
  imgError.value = true
}

function goBack() {
  if (gen.value?.projectId) {
    router.push(`/projects/${gen.value.projectId}`)
  } else {
    router.push('/projects')
  }
}
</script>

<style scoped>
.viewer {
  position: fixed;
  inset: 0;
  background: var(--hd-ink);
  color: var(--hd-on-ink);
  overflow: hidden;
  font-family: var(--hd-font-family);
}

/* ---------- 状态页 ---------- */
.state-box {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--hd-space-1);
  padding: var(--hd-space-3);
  text-align: center;
}
.spinner {
  width: 34px;
  height: 34px;
  border: 2px solid rgba(243, 237, 226, 0.25);
  border-top-color: var(--hd-primary-400);
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
  margin-bottom: var(--hd-space-1);
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.state-title {
  font-family: var(--hd-font-display);
  font-size: var(--hd-text-h2);
  margin: 0;
}
.state-title.error {
  color: #e8a59c;
}
.state-sub {
  margin: 0;
  max-width: 420px;
  font-size: var(--hd-text-caption);
  color: rgba(243, 237, 226, 0.65);
  line-height: var(--hd-text-caption-line);
  word-break: break-word;
}
.ink-btn {
  margin-top: var(--hd-space-2);
  padding: 9px 22px;
  background: var(--hd-on-ink);
  color: var(--hd-ink);
  border: none;
  border-radius: var(--hd-radius-base);
  font-size: var(--hd-text-body);
  cursor: pointer;
  transition: opacity var(--hd-duration-fast) ease-out;
}
.ink-btn:hover {
  opacity: 0.85;
}

/* ---------- 舞台 ---------- */
.stage {
  position: absolute;
  inset: 0;
}
.parallax-layer {
  position: absolute;
  inset: -20px; /* 给视差位移留边，不露黑 */
  transition: transform 0.25s ease-out;
  will-change: transform;
}
.room-image {
  /* 绝对定位叠放：房间切换的过渡瞬间新旧两图共存，
     必须完全重叠交叉淡入；若走普通文档流，新图会被排在旧图下方
     整整一个视口高度（在视口外淡入），肉眼就是一片黑/闪跳 */
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  user-select: none;
  transform-origin: center;
  animation: kenburns 32s ease-in-out infinite alternate;
  will-change: transform;
}
/* 交替方向，每次切房重新起播（:key 变化） */
.room-image.drift-right {
  --kb-from: scale(1.1) translate(-1.5%, 0);
  --kb-to: scale(1.04) translate(1.5%, 0);
}
.room-image.drift-left {
  --kb-from: scale(1.1) translate(1.5%, 0);
  --kb-to: scale(1.04) translate(-1.5%, 0);
}
@keyframes kenburns {
  from {
    transform: var(--kb-from);
  }
  to {
    transform: var(--kb-to);
  }
}
.vignette {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(to bottom, rgba(33, 29, 24, 0.55) 0%, transparent 18%),
    linear-gradient(to top, rgba(33, 29, 24, 0.6) 0%, transparent 22%),
    radial-gradient(ellipse at center, transparent 55%, rgba(33, 29, 24, 0.35) 100%);
}

/* ---------- 房间切换 ---------- */
.room-fade-enter-active,
.room-fade-leave-active {
  transition: opacity 0.5s ease;
}
.room-fade-enter-from,
.room-fade-leave-to {
  opacity: 0;
}

/* ---------- 跳转圆点 ---------- */
.hotspot {
  position: absolute;
  transform: translate(-50%, -50%);
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  z-index: 5;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
}
.hotspot-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--hd-primary-400);
  border: 2px solid rgba(255, 253, 249, 0.92);
  box-shadow: 0 0 0 4px rgba(201, 118, 67, 0.35), 0 2px 10px rgba(0, 0, 0, 0.45);
}
.hotspot-ring {
  position: absolute;
  top: 8px;
  left: 50%;
  width: 16px;
  height: 16px;
  margin-left: -8px;
  border-radius: 50%;
  border: 2px solid rgba(220, 179, 154, 0.9);
  animation: pulse 2.2s ease-out infinite;
  pointer-events: none;
}
@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 0.9;
  }
  70% {
    transform: scale(3.1);
    opacity: 0;
  }
  100% {
    transform: scale(3.1);
    opacity: 0;
  }
}
.hotspot-label {
  padding: 4px 12px;
  background: rgba(33, 29, 24, 0.72);
  border: 1px solid rgba(243, 237, 226, 0.28);
  border-radius: var(--hd-radius-base);
  color: var(--hd-on-ink);
  font-size: var(--hd-text-caption);
  letter-spacing: 0.08em;
  white-space: nowrap;
  backdrop-filter: blur(4px);
  transition: background var(--hd-duration-fast) ease-out;
}
.hotspot:hover .hotspot-label {
  background: var(--hd-primary-600);
  border-color: var(--hd-primary-400);
}
.hotspot:hover .hotspot-dot {
  background: var(--hd-primary-200);
}

/* ---------- HUD ---------- */
.hud {
  position: absolute;
  left: 0;
  right: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  pointer-events: none;
}
.hud > * {
  pointer-events: auto;
}
.hud-top {
  top: 0;
  justify-content: space-between;
  padding: var(--hd-space-2) var(--hd-space-3);
}
.hud-bottom {
  bottom: 0;
  justify-content: center;
  gap: 10px;
  padding: var(--hd-space-2) var(--hd-space-3) var(--hd-space-3);
}
.ghost-btn {
  padding: 8px 16px;
  background: rgba(33, 29, 24, 0.55);
  border: 1px solid rgba(243, 237, 226, 0.3);
  border-radius: var(--hd-radius-base);
  color: var(--hd-on-ink);
  font-size: var(--hd-text-caption);
  letter-spacing: 0.1em;
  cursor: pointer;
  backdrop-filter: blur(6px);
  transition: background var(--hd-duration-fast) ease-out, border-color var(--hd-duration-fast) ease-out;
}
.ghost-btn:hover {
  background: rgba(33, 29, 24, 0.85);
  border-color: var(--hd-primary-400);
}
.icon-only {
  font-family: var(--hd-font-mono);
}
.room-meta {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding: 8px 20px;
  background: rgba(33, 29, 24, 0.5);
  border: 1px solid rgba(243, 237, 226, 0.22);
  border-radius: var(--hd-radius-base);
  backdrop-filter: blur(6px);
}
.room-no {
  font-family: var(--hd-font-mono);
  font-size: var(--hd-text-overline);
  letter-spacing: 0.18em;
  color: var(--hd-primary-200);
}
.room-name {
  font-family: var(--hd-font-display);
  font-size: var(--hd-text-h3);
  font-weight: 500;
}
.room-area {
  font-family: var(--hd-font-mono);
  font-size: var(--hd-text-caption);
  color: rgba(243, 237, 226, 0.7);
}

/* ---------- 引导 ---------- */
.hint {
  position: absolute;
  left: 50%;
  bottom: 132px;
  transform: translateX(-50%);
  z-index: 9;
  margin: 0;
  padding: 8px 18px;
  background: rgba(33, 29, 24, 0.72);
  border: 1px solid rgba(220, 179, 154, 0.5);
  border-radius: var(--hd-radius-base);
  font-size: var(--hd-text-caption);
  letter-spacing: 0.12em;
  white-space: nowrap;
  backdrop-filter: blur(4px);
}
.hint-fade-leave-active {
  transition: opacity 0.6s ease;
}
.hint-fade-leave-to {
  opacity: 0;
}

/* ---------- 户型小地图 ---------- */
.mini-map {
  position: absolute;
  left: var(--hd-space-3);
  bottom: 128px;
  z-index: 10;
  width: 232px;
  padding: 12px;
  background: rgba(33, 29, 24, 0.72);
  border: 1px solid rgba(243, 237, 226, 0.25);
  border-radius: var(--hd-radius-lg);
  backdrop-filter: blur(8px);
}
.map-title {
  margin: 0 0 8px;
  font-family: var(--hd-font-mono);
  font-size: var(--hd-text-overline);
  letter-spacing: 0.22em;
  color: var(--hd-primary-200);
}
.map-canvas {
  position: relative;
  width: 100%;
  border-radius: var(--hd-radius-base);
  overflow: hidden;
  background: var(--hd-on-ink);
}
.map-canvas img {
  display: block;
  width: 100%;
}
.map-block {
  position: absolute;
  border: 1px solid rgba(243, 237, 226, 0.45);
  background: rgba(243, 237, 226, 0.08);
  transition: background var(--hd-duration-fast) ease-out, border-color var(--hd-duration-fast) ease-out;
}
.map-block.active {
  background: rgba(201, 118, 67, 0.55);
  border-color: var(--hd-primary-200);
  box-shadow: 0 0 0 1px rgba(220, 179, 154, 0.6);
}
.map-block-name {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: var(--hd-on-ink);
}
.map-legend {
  list-style: none;
  margin: 8px 0 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.map-legend li {
  padding: 2px 9px;
  font-size: var(--hd-text-overline);
  letter-spacing: 0.08em;
  border: 1px solid rgba(243, 237, 226, 0.25);
  border-radius: var(--hd-radius-base);
  color: rgba(243, 237, 226, 0.75);
  cursor: pointer;
  transition: all var(--hd-duration-fast) ease-out;
}
.map-legend li.active {
  color: var(--hd-on-ink);
  background: var(--hd-primary-600);
  border-color: var(--hd-primary-400);
}
.map-slide-enter-active,
.map-slide-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.map-slide-enter-from,
.map-slide-leave-to {
  opacity: 0;
  transform: translateY(12px);
}

/* ---------- 底部缩略图 ---------- */
.thumb {
  position: relative;
  width: 132px;
  padding: 0;
  background: none;
  border: none;
  cursor: pointer;
  opacity: 0.55;
  transition: opacity var(--hd-duration-fast) ease-out, transform var(--hd-duration-fast) ease-out;
}
.thumb img {
  display: block;
  width: 100%;
  height: 60px;
  object-fit: cover;
  border-radius: var(--hd-radius-base);
  border: 1px solid rgba(243, 237, 226, 0.3);
}
.thumb-name {
  display: block;
  margin-top: 5px;
  font-size: var(--hd-text-overline);
  letter-spacing: 0.1em;
  color: var(--hd-on-ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.thumb-name em {
  font-family: var(--hd-font-mono);
  font-style: normal;
  color: var(--hd-primary-200);
  margin-right: 5px;
}
.thumb:hover {
  opacity: 0.9;
}
.thumb.active {
  opacity: 1;
  transform: translateY(-3px);
}
.thumb.active img {
  border-color: var(--hd-primary-400);
  box-shadow: 0 0 0 1px var(--hd-primary-400), 0 6px 18px rgba(0, 0, 0, 0.4);
}

/* ---------- v1 旧任务单图 ---------- */
.legacy-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
}

/* ---------- 响应式 ---------- */
@media (max-width: 760px) {
  .mini-map {
    width: 180px;
    bottom: 118px;
    left: var(--hd-space-2);
  }
  .thumb {
    width: 92px;
  }
  .thumb img {
    height: 48px;
  }
  .room-area {
    display: none;
  }
  .hud-top {
    padding: var(--hd-space-1) var(--hd-space-2);
  }
}

@media (prefers-reduced-motion: reduce) {
  .room-image,
  .hotspot-ring,
  .spinner {
    animation: none;
  }
}
</style>
