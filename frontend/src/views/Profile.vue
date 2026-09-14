<template>
  <div class="page">
    <header class="page-header">
      <h2 class="title">我的</h2>
      <el-button size="small" plain @click="openEdit">编辑资料</el-button>
    </header>

    <!-- 用户信息 -->
    <div class="profile-card">
      <el-avatar :size="56" :src="displayAvatar" class="avatar">{{ initial }}</el-avatar>
      <div class="profile-info">
        <div class="nickname">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '…' }}</div>
        <ul class="meta-list">
          <li><span class="label">用户名</span>{{ userStore.userInfo?.username }}</li>
          <li><span class="label">昵称</span>{{ userStore.userInfo?.nickname || '-' }}</li>
          <li><span class="label">ID</span>{{ userStore.userInfo?.id }}</li>
        </ul>
      </div>
    </div>

    <!-- 我的发布 -->
    <section class="my-posts">
      <h3 class="section-title">我的发布</h3>

      <div v-if="loading" class="state-box">
        <el-skeleton animated :rows="3" />
      </div>

      <div v-else-if="myPosts.length === 0" class="state-box">
        <el-empty description="还没有发布过动态" :image-size="80" />
      </div>

      <ul v-else class="post-list">
        <li v-for="p in myPosts" :key="p.id" class="post-card">
          <p v-if="p.content" class="post-content">{{ p.content }}</p>
          <div v-if="p.images && p.images.length" class="post-images">
            <img
              v-for="(img, i) in p.images.slice(0, 4)"
              :key="img + i"
              :src="img"
              class="post-img"
              alt=""
              loading="lazy"
            />
          </div>
          <div class="post-meta">
            <time class="time">{{ formatTime(p.createdAt) }}</time>
            <span class="stat">👍 {{ p.likeCount || 0 }}</span>
            <span class="stat">💬 {{ p.commentCount || 0 }}</span>
          </div>
        </li>
      </ul>

      <div v-if="hasMore" class="load-more">
        <el-button plain :loading="loadingMore" @click="loadMore">加载更多</el-button>
      </div>
    </section>

    <!-- 退出登录（页面最下方） -->
    <el-button type="danger" plain class="logout" @click="onLogout">退出登录</el-button>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="editVisible" title="编辑资料" width="min(400px, 90%)">
      <div class="avatar-edit">
        <el-avatar :size="72" :src="editForm.avatar" class="avatar-edit-preview">{{ initial }}</el-avatar>
        <input
          id="edit-avatar-file"
          type="file"
          class="file-input"
          accept=".jpg,.jpeg,.png,.gif,.webp"
          @change="onAvatarChange"
        />
        <label class="avatar-edit-btn" for="edit-avatar-file">更换头像</label>
      </div>
      <el-form label-position="top">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" maxlength="50" show-word-limit placeholder="请输入昵称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { uploadFile } from '../api/file'
import { listPosts } from '../api/post'

const router = useRouter()
const userStore = useUserStore()

const displayAvatar = computed(() => userStore.userInfo?.avatar || '')
const initial = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username
  return name ? name.charAt(0).toUpperCase() : '?'
})

/* ---- 我的发布 ---- */
const myPosts = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const pageNum = ref(1)
const pageSize = 20
const hasMore = ref(false)

async function loadMyPosts(page, append) {
  const res = await listPosts(page, pageSize, true)
  const records = res.records || []
  myPosts.value = append ? [...myPosts.value, ...records] : records
  pageNum.value = page
  hasMore.value = myPosts.value.length < res.total
}

async function loadMore() {
  loadingMore.value = true
  try {
    await loadMyPosts(pageNum.value + 1, true)
  } finally {
    loadingMore.value = false
  }
}

onMounted(async () => {
  userStore.userInfo ?? (await userStore.fetchMe())
  loading.value = true
  try {
    await loadMyPosts(1, false)
  } finally {
    loading.value = false
  }
})

/* ---- 编辑资料 ---- */
const editVisible = ref(false)
const saving = ref(false)
const newAvatarFile = ref(null)
const editForm = reactive({ nickname: '', avatar: '' })

function openEdit() {
  editForm.nickname = userStore.userInfo?.nickname || ''
  editForm.avatar = userStore.userInfo?.avatar || ''
  newAvatarFile.value = null
  editVisible.value = true
}

function onAvatarChange(e) {
  const file = e.target.files?.[0]
  const ALLOWED = ['jpg', 'jpeg', 'png', 'gif', 'webp']
  if (!file) return
  const ext = (file.name.split('.').pop() || '').toLowerCase()
  if (!ALLOWED.includes(ext)) {
    ElMessage.error('仅支持 jpg/jpeg/png/gif/webp 格式')
    e.target.value = ''
    return
  }
  newAvatarFile.value = file
  editForm.avatar = URL.createObjectURL(file)
}

async function onSave() {
  const nickname = editForm.nickname?.trim()
  if (!nickname && !newAvatarFile.value) {
    return ElMessage.warning('请至少修改昵称或头像')
  }
  saving.value = true
  try {
    const payload = {}
    if (nickname && nickname !== userStore.userInfo?.nickname) payload.nickname = nickname
    if (newAvatarFile.value) payload.avatar = await uploadFile(newAvatarFile.value)
    if (Object.keys(payload).length === 0) {
      editVisible.value = false
      return
    }
    await userStore.updateProfile(payload)
    ElMessage.success('资料已更新')
    editVisible.value = false
  } finally {
    saving.value = false
  }
}

/* ---- 退出登录 ---- */
function onLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
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
  padding: var(--hd-space-2) var(--hd-space-2) var(--hd-space-4);
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--hd-space-1) 0 var(--hd-space-2);
}
.title {
  margin: 0;
  font-size: var(--hd-text-h1);
  line-height: var(--hd-text-h1-line);
  font-weight: var(--hd-font-weight-medium);
}

/* 用户信息卡 */
.profile-card {
  display: flex;
  align-items: center;
  gap: var(--hd-space-2);
  padding: var(--hd-space-3);
  background: #fff;
  border: 1px solid var(--hd-neutral-200);
  border-radius: var(--hd-radius-lg);
}
.avatar {
  background: var(--hd-primary-600);
  color: #fff;
  font-size: 24px;
}
.nickname {
  font-size: var(--hd-text-h3);
  line-height: var(--hd-text-h3-line);
  font-weight: var(--hd-font-weight-medium);
}
.meta-list {
  list-style: none;
}
.meta-list li {
  font-size: var(--hd-text-caption);
  line-height: var(--hd-text-caption-line);
  color: var(--hd-neutral-500);
}
.meta-list .label {
  color: var(--hd-neutral-400);
  margin-right: 6px;
}

/* 我的发布 */
.my-posts {
  margin-top: var(--hd-space-2);
}
.section-title {
  margin: 0 0 var(--hd-space-1);
  font-size: var(--hd-text-h3);
  line-height: var(--hd-text-h3-line);
  font-weight: var(--hd-font-weight-medium);
}
.post-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--hd-space-1);
}
.post-card {
  background: #fff;
  border: 1px solid var(--hd-neutral-200);
  border-radius: var(--hd-radius-lg);
  padding: var(--hd-space-2);
}
.post-content {
  white-space: pre-wrap;
  word-break: break-word;
}
.post-images {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: var(--hd-space-1);
}
.post-img {
  width: 96px;
  height: 96px;
  object-fit: cover;
  border-radius: var(--hd-radius-base);
}
.post-meta {
  display: flex;
  align-items: center;
  gap: var(--hd-space-2);
  margin-top: var(--hd-space-1);
}
.time {
  color: var(--hd-neutral-400);
  font-size: var(--hd-text-overline);
}
.stat {
  color: var(--hd-neutral-500);
  font-size: var(--hd-text-overline);
}
.load-more {
  text-align: center;
  margin-top: var(--hd-space-1);
}
.state-box {
  background: #fff;
  border: 1px solid var(--hd-neutral-200);
  border-radius: var(--hd-radius-lg);
  padding: var(--hd-space-2);
  min-height: 120px;
}

/* 退出登录 */
.logout {
  width: 100%;
  margin-top: var(--hd-space-3);
}

/* 编辑弹窗 */
.avatar-edit {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--hd-space-1);
  margin-bottom: var(--hd-space-3);
}
.avatar-edit-preview {
  background: var(--hd-primary-600);
  color: #fff;
  font-size: 28px;
}
.file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  overflow: hidden;
  clip: rect(0 0 0 0);
}
.avatar-edit-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 16px;
  border: 1px solid var(--hd-primary-200);
  border-radius: var(--hd-radius-base);
  background: var(--hd-primary-50);
  color: var(--hd-primary-700);
  font-size: var(--hd-text-caption);
  cursor: pointer;
  transition: background-color var(--hd-duration-fast) ease-out;
}
.avatar-edit-btn:hover {
  background: var(--hd-primary-100);
}
</style>