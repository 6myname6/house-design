<template>
  <div class="page">
    <header class="page-header">
      <h2 class="title">装修小圈</h2>
      <el-button type="primary" @click="openPublish">发表动态</el-button>
    </header>

    <!-- 加载骨架屏 -->
    <div v-if="loading" class="post-list">
      <div v-for="i in 3" :key="i" class="post-card">
        <el-skeleton animated>
          <template #template>
            <el-skeleton-item variant="circle" class="sk-avatar" />
            <div class="sk-main">
              <el-skeleton-item variant="h3" class="sk-line short" />
              <el-skeleton-item variant="text" class="sk-line" />
              <el-skeleton-item variant="text" class="sk-line" />
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <!-- 加载失败 -->
    <div v-else-if="loadError" class="state-box">
      <el-empty description="小圈加载失败">
        <el-button type="primary" plain @click="load()">重新加载</el-button>
      </el-empty>
    </div>

    <!-- 空态 -->
    <div v-else-if="posts.length === 0" class="state-box">
      <el-empty description="还没有动态，来发第一条吧">
        <el-button type="primary" @click="openPublish">发表动态</el-button>
      </el-empty>
    </div>

    <!-- 帖子流 -->
    <div v-else class="post-list">
      <article v-for="p in posts" :key="p.id" class="post-card">
        <header class="post-head">
          <el-avatar :size="40" :src="p.authorAvatar || ''" class="post-avatar">
            {{ (p.authorName || '?').charAt(0) }}
          </el-avatar>
          <div class="post-author">
            <div class="name">{{ p.authorName }}</div>
            <time class="time">{{ formatTime(p.createdAt) }}</time>
          </div>
          <button
            v-if="p.userId === userStore.userInfo?.id"
            type="button"
            class="post-del"
            title="删除帖子"
            @click="onDeletePost(p)"
          >
            删除
          </button>
        </header>

        <p v-if="p.content" class="post-content">{{ p.content }}</p>

        <div v-if="visibleImages(p.images).length" class="post-images" :class="{ single: visibleImages(p.images).length === 1 }">
          <img
            v-for="(img, i) in p.images"
            :key="img + i"
            :src="img"
            v-show="!brokenImages.has(img)"
            class="post-img"
            alt=""
            loading="lazy"
            @error="brokenImages.add(img)"
          />
        </div>

        <footer class="post-actions">
          <button
            type="button"
            class="action-btn"
            :class="{ liked: p.likedByMe }"
            @click="onLike(p)"
          >
            <span aria-hidden="true">{{ p.likedByMe ? '❤️' : '🤍' }}</span>
            <span>觉得赞</span>
            <span class="count">{{ p.likeCount || 0 }}</span>
          </button>
          <button type="button" class="action-btn" @click="toggleComments(p)">
            <span aria-hidden="true">💬</span>
            <span>评论 {{ p.commentCount || 0 }}</span>
          </button>
        </footer>

        <!-- 评论区 -->
        <div v-if="expandedId === p.id" class="comment-panel">
          <div v-if="commentLoading" class="comment-loading">评论加载中…</div>
          <template v-else>
            <div v-if="topComments(p).length === 0" class="comment-loading">还没有评论，来抢沙发～</div>
            <div v-for="c in topComments(p)" :key="c.id" class="comment-item">
              <el-avatar :size="28" :src="c.authorAvatar || ''" class="comment-avatar">
                {{ (c.authorName || '?').charAt(0) }}
              </el-avatar>
              <div class="comment-body">
                <div class="comment-meta">
                  <span class="comment-author">{{ c.authorName }}</span>
                  <time class="comment-time">{{ formatTime(c.createdAt) }}</time>
                </div>
                <div class="comment-text">{{ c.content }}</div>
                <div v-if="visibleImages(c.images).length" class="comment-images">
                  <img v-for="(img, i) in c.images" :key="img + i" :src="img" v-show="!brokenImages.has(img)" class="comment-img" alt="" loading="lazy" @error="brokenImages.add(img)" />
                </div>
                <div class="comment-ops">
                  <button type="button" class="op-btn" :class="{ liked: c.likedByMe }" @click="onCommentLike(c)">
                    赞 {{ c.likeCount || 0 }}
                  </button>
                  <button type="button" class="op-btn" @click="startReply(c)">回复</button>
                  <button
                    v-if="c.userId === userStore.userInfo?.id"
                    type="button"
                    class="op-btn danger"
                    @click="onDeleteComment(c, p)"
                  >
                    删除
                  </button>
                </div>
              </div>
            </div>

            <!-- 回复（缩进展示） -->
            <div v-for="r in repliesOf(p.id)" :key="r.id" class="comment-item reply">
              <el-avatar :size="28" :src="r.authorAvatar || ''" class="comment-avatar">
                {{ (r.authorName || '?').charAt(0) }}
              </el-avatar>
              <div class="comment-body">
                <div class="comment-meta">
                  <span class="comment-author">{{ r.authorName }}</span>
                  <span class="comment-reply-to">回复</span>
                  <time class="comment-time">{{ formatTime(r.createdAt) }}</time>
                </div>
                <div class="comment-text">{{ r.content }}</div>
                <div v-if="visibleImages(r.images).length" class="comment-images">
                  <img v-for="(img, i) in r.images" :key="img + i" :src="img" v-show="!brokenImages.has(img)" class="comment-img" alt="" loading="lazy" @error="brokenImages.add(img)" />
                </div>
                <div class="comment-ops">
                  <button type="button" class="op-btn" :class="{ liked: r.likedByMe }" @click="onCommentLike(r)">
                    赞 {{ r.likeCount || 0 }}
                  </button>
                  <button type="button" class="op-btn" @click="startReply(r)">回复</button>
                  <button
                    v-if="r.userId === userStore.userInfo?.id"
                    type="button"
                    class="op-btn danger"
                    @click="onDeleteComment(r, p)"
                  >
                    删除
                  </button>
                </div>
              </div>
            </div>
          </template>

          <!-- 评论输入 -->
          <div class="comment-input">
            <div class="comment-input-main">
              <div v-if="commentImages.length" class="comment-sel-imgs">
                <div v-for="(img, i) in commentImages" :key="img + i" class="comment-sel-img">
                  <img :src="img" alt="" />
                  <button type="button" class="del" aria-label="移除图片" @click="commentImages.splice(i, 1)">×</button>
                </div>
              </div>
              <div class="comment-input-row">
                <label class="img-btn" for="comment-image-file" title="添加图片">
                  <span aria-hidden="true">🖼️</span>
                </label>
                <input
                  id="comment-image-file"
                  type="file"
                  class="file-hide"
                  accept=".jpg,.jpeg,.png,.gif,.webp"
                  multiple
                  @change="onAddCommentImages"
                />
                <el-input
                  v-model="commentText"
                  :placeholder="replyTarget ? `回复 ${replyTarget.authorName}：` : '写下你的评论…'"
                  @keyup.enter="submitComment(p)"
                />
                <el-button v-if="replyTarget" @click="cancelReply">取消回复</el-button>
                <el-button type="primary" :loading="commentSending" @click="submitComment(p)">发送</el-button>
              </div>
            </div>
          </div>
        </div>
      </article>

      <!-- 加载更多 -->
      <div v-if="hasMore" class="load-more">
        <el-button plain :loading="loadingMore" @click="loadMore">加载更多</el-button>
      </div>
    </div>

    <!-- 发表动态弹窗 -->
    <el-dialog v-model="publishVisible" title="发表动态" width="min(480px, 92%)">
      <el-input
        v-model="publishForm.content"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="分享你的装修灵感…"
      />
      <div class="publish-images">
        <label v-for="(img, i) in publishForm.images" :key="i" class="publish-img" :for="`publish-file-${i}`">
          <img :src="img" alt="" />
        </label>
        <label v-if="publishForm.images.length < 9" class="publish-add" for="publish-file">
          <span aria-hidden="true">＋</span>
        </label>
        <p v-if="publishForm.images.length" class="publish-imgs-tip">点击图片可替换，最多 9 张</p>
      </div>
      <input
        v-for="(img, i) in publishForm.images"
        :key="i"
        :id="`publish-file-${i}`"
        type="file"
        class="file-hide"
        accept=".jpg,.jpeg,.png,.gif,.webp"
        @change="onReplace(i, $event)"
      />
      <input id="publish-file" type="file" class="file-hide" accept=".jpg,.jpeg,.png,.gif,.webp" multiple @change="onAddFiles" />
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" :disabled="!canPublish" @click="publish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import {
  listPosts,
  createPost,
  toggleLike,
  listComments,
  createComment,
  replyComment,
  toggleCommentLike,
  deleteComment,
  deletePost
} from '../api/post'
import { uploadFile } from '../api/file'

const userStore = useUserStore()

const posts = ref([])
const loading = ref(false)
// 加载失败的图片 URL 集合（裂图自动隐藏；刷新列表时清空，给 URL 恢复的机会）
const brokenImages = ref(new Set())
function visibleImages(images) {
  if (!images || !images.length) return []
  return images.filter((u) => u && !brokenImages.value.has(u))
}
const loadError = ref(false)
const pageNum = ref(1)
const pageSize = 20
const hasMore = ref(false)
const loadingMore = ref(false)

const publishVisible = ref(false)
const publishing = ref(false)
const publishForm = reactive({ content: '', images: [] })

const canPublish = computed(() => publishForm.content.trim() || publishForm.images.length)

onMounted(async () => {
  userStore.userInfo ?? (await userStore.fetchMe())
  await load()
})

async function load() {
  loading.value = true
  loadError.value = false
  brokenImages.value = new Set() // 重新加载时重置裂图标记
  try {
    await fetchPage(1, false)
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loadingMore.value = true
  try {
    await fetchPage(pageNum.value + 1, true)
  } finally {
    loadingMore.value = false
  }
}

async function fetchPage(page, append) {
  const res = await listPosts(page, pageSize)
  const records = res.records || []
  posts.value = append ? [...posts.value, ...records] : records
  pageNum.value = page
  hasMore.value = posts.value.length < res.total
}

function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

/* ---- 点赞 ---- */
async function onLike(p) {
  // 请求进行中忽略连点，防止乐观更新连续翻转造成计数错乱
  if (p._liking) return
  const prevLiked = !!p.likedByMe
  const prevCount = Number.isInteger(p.likeCount) ? p.likeCount : 0
  const nextLiked = !prevLiked
  p._liking = true
  // 乐观更新
  p.likedByMe = nextLiked
  p.likeCount = Math.max(0, prevCount + (nextLiked ? 1 : -1))
  try {
    const res = await toggleLike(p.id)
    // 服务端返回为唯一真相；但字段异常（缺字段/类型错）时不盲目覆盖，保留乐观结果
    if (res && typeof res.liked === 'boolean') {
      p.likedByMe = res.liked
      p.likeCount = Number.isInteger(res.likeCount)
        ? res.likeCount
        : Math.max(0, prevCount + (res.liked ? 1 : -1))
    }
  } catch {
    p.likedByMe = prevLiked
    p.likeCount = prevCount
  } finally {
    p._liking = false
  }
}

/* ---- 删除帖子 ---- */
async function onDeletePost(p) {
  try {
    await ElMessageBox.confirm('确定删除这条动态吗？删除后不可恢复。', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deletePost(p.id)
  ElMessage.success('已删除')
  posts.value = posts.value.filter((x) => x.id !== p.id)
}

/* ---- 评论 ---- */
const expandedId = ref(null) // 当前展开评论区的帖子 ID
const commentLoading = ref(false)
const commentSending = ref(false)
const commentText = ref('')
const replyTarget = ref(null) // 正在回复的评论（null = 发表新评论）
const commentImages = ref([]) // 评论待上传图片 URL
const commentsMap = reactive({}) // postId -> 评论列表

// 顶层评论（无 parentId）
function topComments(p) {
  return (commentsMap[p.id] || []).filter((c) => !c.parentId)
}

// 某顶层评论下的回复
function repliesOf(postId) {
  const tops = commentsMap[postId] || []
  const parentIds = new Set(tops.filter((c) => !c.parentId).map((c) => c.id))
  return tops.filter((c) => c.parentId && parentIds.has(c.parentId))
}

async function toggleComments(p) {
  if (expandedId.value === p.id) {
    expandedId.value = null
    replyTarget.value = null
    return
  }
  expandedId.value = p.id
  replyTarget.value = null
  if (commentsMap[p.id]) return
  commentLoading.value = true
  try {
    commentsMap[p.id] = await listComments(p.id)
  } finally {
    commentLoading.value = false
  }
}

function startReply(c) {
  replyTarget.value = c
}

function cancelReply() {
  replyTarget.value = null
}

// 评论附件图片：选择文件 → 直接上传拿 URL
async function onAddCommentImages(e) {
  const files = Array.from(e.target.files || [])
  const ALLOWED = ['jpg', 'jpeg', 'png', 'gif', 'webp']
  const need = 9 - commentImages.value.length
  let left = files.slice(0, need)
  const bad = left.some((f) => !ALLOWED.includes((f.name.split('.').pop() || '').toLowerCase()))
  if (bad) {
    ElMessage.error('仅支持 jpg/jpeg/png/gif/webp 格式')
  }
  left = left.filter((f) => ALLOWED.includes((f.name.split('.').pop() || '').toLowerCase()))
  for (const f of left) {
    const url = await uploadFile(f)
    commentImages.value.push(url)
  }
  e.target.value = ''
}

async function submitComment(p) {
  const content = commentText.value.trim()
  if (!content && commentImages.value.length === 0) return ElMessage.warning('请输入评论内容')
  commentSending.value = true
  try {
    const data = { content, images: [...commentImages.value] }
    if (replyTarget.value) {
      await replyComment(replyTarget.value.id, data)
    } else {
      await createComment(p.id, data)
    }
    commentsMap[p.id] = await listComments(p.id)
    p.commentCount = (p.commentCount || 0) + 1
    commentText.value = ''
    commentImages.value = []
    replyTarget.value = null
  } finally {
    commentSending.value = false
  }
}

async function onCommentLike(c) {
  // 同帖子点赞：防连点 + 乐观更新 + 服务端字段校验 + 失败回滚
  if (c._liking) return
  const prevLiked = !!c.likedByMe
  const prevCount = Number.isInteger(c.likeCount) ? c.likeCount : 0
  const nextLiked = !prevLiked
  c._liking = true
  c.likedByMe = nextLiked
  c.likeCount = Math.max(0, prevCount + (nextLiked ? 1 : -1))
  try {
    const res = await toggleCommentLike(c.id)
    if (res && typeof res.liked === 'boolean') {
      c.likedByMe = res.liked
      c.likeCount = Number.isInteger(res.likeCount)
        ? res.likeCount
        : Math.max(0, prevCount + (res.liked ? 1 : -1))
    }
  } catch {
    c.likedByMe = prevLiked
    c.likeCount = prevCount
  } finally {
    c._liking = false
  }
}

async function onDeleteComment(c, p) {
  try {
    await deleteComment(c.id)
    commentsMap[p.id] = await listComments(p.id)
    p.commentCount = Math.max(0, (p.commentCount || 0) - 1)
  } catch {}
}

/* ---- 发表动态 ---- */
function openPublish() {
  publishForm.content = ''
  publishForm.images = []
  publishVisible.value = true
}

async function onAddFiles(e) {
  const files = Array.from(e.target.files || [])
  const ALLOWED = ['jpg', 'jpeg', 'png', 'gif', 'webp']
  const need = 9 - publishForm.images.length
  let left = files.slice(0, need)
  const bad = left.some((f) => !ALLOWED.includes((f.name.split('.').pop() || '').toLowerCase()))
  if (bad) {
    ElMessage.error('仅支持 jpg/jpeg/png/gif/webp 格式')
  }
  left = left.filter((f) => ALLOWED.includes((f.name.split('.').pop() || '').toLowerCase()))
  for (const f of left) {
    const url = await uploadFile(f)
    publishForm.images.push(url)
  }
  e.target.value = ''
}

async function onReplace(i, e) {
  const file = e.target.files?.[0]
  const ALLOWED = ['jpg', 'jpeg', 'png', 'gif', 'webp']
  if (!file) return
  const ext = (file.name.split('.').pop() || '').toLowerCase()
  if (!ALLOWED.includes(ext)) {
    ElMessage.error('仅支持 jpg/jpeg/png/gif/webp 格式')
    e.target.value = ''
    return
  }
  const url = await uploadFile(file)
  publishForm.images[i] = url
  e.target.value = ''
}

async function publish() {
  publishing.value = true
  try {
    await createPost({
      content: publishForm.content.trim(),
      images: publishForm.images
    })
    ElMessage.success('发布成功')
    publishVisible.value = false
    load()
  } finally {
    publishing.value = false
  }
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
.post-list {
  display: flex;
  flex-direction: column;
  gap: var(--hd-space-2);
}
.post-card {
  background: #fff;
  border: 1px solid var(--hd-neutral-200);
  border-radius: var(--hd-radius-lg);
  padding: var(--hd-space-2);
}
.post-head {
  display: flex;
  align-items: center;
  gap: var(--hd-space-1);
}
.post-avatar {
  background: var(--hd-primary-600);
  color: #fff;
}
.name {
  font-weight: var(--hd-font-weight-medium);
}
.time {
  color: var(--hd-neutral-400);
  font-size: var(--hd-text-overline);
}
.post-del {
  margin-left: auto;
  border: none;
  background: none;
  padding: 4px 8px;
  border-radius: var(--hd-radius-base);
  color: var(--hd-neutral-400);
  font-size: var(--hd-text-overline);
  cursor: pointer;
}
.post-del:hover {
  color: var(--hd-danger);
  background: var(--hd-neutral-100);
}
.post-content {
  margin: var(--hd-space-1) 0 var(--hd-space-1);
  white-space: pre-wrap;
  word-break: break-word;
}
.post-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  margin-bottom: var(--hd-space-1);
}
.post-images.single {
  grid-template-columns: minmax(0, 240px);
}
.post-img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: var(--hd-radius-base);
}
.post-actions {
  display: flex;
  gap: var(--hd-space-2);
  border-top: 1px solid var(--hd-neutral-100);
  padding-top: var(--hd-space-1);
}
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: none;
  padding: 4px 8px;
  border-radius: var(--hd-radius-base);
  color: var(--hd-neutral-500);
  font-size: var(--hd-text-caption);
  cursor: pointer;
  transition: background-color var(--hd-duration-fast) ease-out, color var(--hd-duration-fast) ease-out;
}
.action-btn:hover {
  background: var(--hd-neutral-100);
}
.action-btn.liked {
  color: var(--hd-danger);
}
.count {
  font-variant-numeric: tabular-nums;
}

/* 评论区 */
.comment-panel {
  margin-top: var(--hd-space-1);
  padding-top: var(--hd-space-1);
  border-top: 1px solid var(--hd-neutral-100);
  background: var(--hd-neutral-50);
  border-radius: 0 0 var(--hd-radius-lg) var(--hd-radius-lg);
}
.comment-loading {
  padding: var(--hd-space-2);
  text-align: center;
  color: var(--hd-neutral-400);
  font-size: var(--hd-text-caption);
}
.comment-item {
  display: flex;
  gap: 8px;
  padding: var(--hd-space-1) var(--hd-space-2);
}
.comment-item.reply {
  padding-left: var(--hd-space-5);
}
.comment-avatar {
  background: var(--hd-primary-600);
  color: #fff;
  flex-shrink: 0;
}
.comment-body {
  flex: 1;
  min-width: 0;
}
.comment-meta {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.comment-author {
  font-weight: var(--hd-font-weight-medium);
  font-size: var(--hd-text-caption);
}
.comment-reply-to {
  color: var(--hd-neutral-400);
  font-size: var(--hd-text-overline);
}
.comment-time {
  color: var(--hd-neutral-400);
  font-size: var(--hd-text-overline);
}
.comment-text {
  word-break: break-word;
  white-space: pre-wrap;
}
/* 评论图片 */
.comment-images {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}
.comment-img {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: var(--hd-radius-base);
}
.comment-ops {
  display: flex;
  gap: var(--hd-space-2);
  margin-top: 2px;
}
.op-btn {
  border: none;
  background: none;
  padding: 0;
  color: var(--hd-neutral-500);
  font-size: var(--hd-text-overline);
  cursor: pointer;
  user-select: none;
}
.op-btn:hover {
  color: var(--hd-primary-700);
}
.op-btn.liked {
  color: var(--hd-danger);
}
.op-btn.danger {
  color: var(--hd-danger);
}
.comment-input {
  padding: var(--hd-space-1) var(--hd-space-2) var(--hd-space-2);
}
.comment-input-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.comment-sel-imgs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.comment-sel-img {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: var(--hd-radius-base);
  overflow: hidden;
}
.comment-sel-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.comment-sel-img .del {
  position: absolute;
  top: 0;
  right: 0;
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 0 0 0 var(--hd-radius-base);
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
}
.comment-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.comment-input-row .el-input {
  flex: 1;
}
.img-btn {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--hd-neutral-200);
  border-radius: var(--hd-radius-base);
  background: #fff;
  cursor: pointer;
  font-size: 16px;
}
.img-btn:hover {
  border-color: var(--hd-primary-200);
  background: var(--hd-primary-50);
}
.load-more {
  text-align: center;
}
.state-box {
  padding: var(--hd-space-4) 0;
}

/* 骨架屏 */
.sk-avatar {
  float: left;
  margin-right: 12px;
  width: 40px;
  height: 40px;
}
.sk-main {
  overflow: hidden;
}
.sk-line {
  margin-top: 8px;
}
.sk-line.short {
  width: 40%;
}

/* 发帖弹窗 */
.publish-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: var(--hd-space-2);
}
.publish-img,
.publish-add {
  width: 80px;
  height: 80px;
  border-radius: var(--hd-radius-base);
  overflow: hidden;
  border: 1px dashed var(--hd-neutral-300);
}
.publish-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.publish-add {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--hd-neutral-100);
  color: var(--hd-neutral-400);
  font-size: 24px;
  cursor: pointer;
}
.publish-imgs-tip {
  width: 100%;
  color: var(--hd-neutral-400);
  font-size: var(--hd-text-overline);
}
.file-hide {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  overflow: hidden;
  clip: rect(0 0 0 0);
}
</style>