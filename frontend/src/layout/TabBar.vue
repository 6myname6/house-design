<template>
  <aside class="side-nav" aria-label="主导航">
    <router-link to="/projects" class="brand" aria-label="HouseDesign 首页">
      <img src="/favicon.ico" alt="筑梦家" class="brand-icon" />
    </router-link>

    <nav class="nav-list">
      <router-link
        v-for="t in tabs"
        :key="t.path"
        :to="t.path"
        class="nav-item"
        :class="{ active: isActive(t.path) }"
        :aria-current="isActive(t.path) ? 'page' : undefined"
      >
        {{ t.label }}
      </router-link>
    </nav>
  </aside>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

// 导航 1「首页」/ 导航 2「小圈」/ 导航 3「我的」，底部「设置」
const tabs = [
  { path: '/projects', label: '首页' },
  { path: '/community', label: '小圈' },
  { path: '/profile', label: '我的' }
]

function isActive(path) {
  // 二级页（详情等）保持所属导航高亮
  if (path === '/projects' && route.path.startsWith('/projects')) return true
  if (path === '/community' && route.path.startsWith('/community')) return true
  return route.path === path
}
</script>

<style scoped>
/* 网页端左侧边栏：固定宽、纵向导航、底部设置 */
.side-nav {
  width: 200px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid var(--hd-neutral-200);
  padding: var(--hd-space-2) var(--hd-space-1);
}

.brand {
  display: inline-flex;
  align-items: center;
  padding: var(--hd-space-1);
  margin-bottom: var(--hd-space-2);
}

.brand-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--hd-radius-base);
  object-fit: cover;
}

.nav-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  padding: 10px var(--hd-space-1);
  border-radius: var(--hd-radius-base);
  color: var(--hd-neutral-500);
  text-decoration: none;
  font-size: var(--hd-text-body);
  line-height: var(--hd-text-body-line);
  transition: background-color var(--hd-duration-fast) ease-out, color var(--hd-duration-fast) ease-out;
}

.nav-item:hover {
  color: var(--hd-neutral-800);
  background: var(--hd-neutral-100);
}

.nav-item.active {
  color: var(--hd-primary-700);
  background: var(--hd-primary-50);
  font-weight: var(--hd-font-weight-medium);
}
</style>
