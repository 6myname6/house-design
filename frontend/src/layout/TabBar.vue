<template>
  <aside class="side-nav" aria-label="主导航">
    <router-link to="/projects" class="brand" aria-label="筑梦家 首页">
      <span class="brand-cn">筑梦家</span>
      <span class="brand-en">HOUSE&nbsp;DESIGN&nbsp;ATELIER</span>
    </router-link>

    <hr class="nav-rule" />

    <nav class="nav-list">
      <router-link
        v-for="(t, i) in tabs"
        :key="t.path"
        :to="t.path"
        class="nav-item"
        :class="{ active: isActive(t.path) }"
        :aria-current="isActive(t.path) ? 'page' : undefined"
      >
        <span class="nav-index">{{ String(i + 1).padStart(2, '0') }}</span>
        <span class="nav-label">{{ t.label }}</span>
      </router-link>
    </nav>

    <div class="nav-foot">
      <span class="foot-line" aria-hidden="true"></span>
      <span class="foot-text">室内效果 · 预演未来居所</span>
    </div>
  </aside>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

// 01 首页 / 02 小圈 / 03 问问AI / 04 我的
const tabs = [
  { path: '/projects', label: '首页' },
  { path: '/community', label: '小圈' },
  { path: '/ai-chat', label: '问问AI' },
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
.side-nav {
  width: var(--hd-side-nav-width);
  flex-shrink: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--hd-paper);
  border-right: var(--hd-hairline);
  padding: var(--hd-space-3) var(--hd-space-2) var(--hd-space-2);
}

/* 品牌区：宋体大字 + 等宽英文小注 */
.brand {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 0 var(--hd-space-1);
  text-decoration: none;
}
.brand-cn {
  font-family: var(--hd-font-display);
  font-size: 28px;
  line-height: 1.15;
  color: var(--hd-ink);
  letter-spacing: 0.06em;
}
.brand-en {
  font-family: var(--hd-font-mono);
  font-size: 9px;
  letter-spacing: 0.22em;
  color: var(--hd-neutral-400);
}

.nav-rule {
  border: none;
  border-top: var(--hd-hairline);
  margin: var(--hd-space-3) var(--hd-space-1);
}

.nav-list {
  display: flex;
  flex-direction: column;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: baseline;
  gap: var(--hd-space-1);
  padding: 11px var(--hd-space-1);
  color: var(--hd-neutral-500);
  text-decoration: none;
  font-size: var(--hd-text-body);
  transition: color var(--hd-duration-fast) ease-out;
}
.nav-item::before {
  /* 左侧陶土竖线，默认收起 */
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 2px;
  height: 0;
  background: var(--hd-primary-600);
  transform: translateY(-50%);
  transition: height var(--hd-duration-slow) cubic-bezier(0.22, 0.61, 0.36, 1);
}
.nav-item:hover { color: var(--hd-ink); }
.nav-item:hover .nav-index { color: var(--hd-primary-600); }

.nav-item.active {
  color: var(--hd-ink);
}
.nav-item.active::before {
  height: 20px;
}

.nav-index {
  font-family: var(--hd-font-mono);
  font-size: var(--hd-text-overline);
  letter-spacing: 0.1em;
  color: var(--hd-neutral-400);
  transition: color var(--hd-duration-fast);
}
.nav-label {
  letter-spacing: 0.08em;
}
.nav-item.active .nav-label {
  font-weight: var(--hd-font-weight-medium);
}

/* 底部图注 */
.nav-foot {
  margin-top: auto;
  padding: 0 var(--hd-space-1);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.foot-line {
  width: 32px;
  height: 1px;
  background: var(--hd-neutral-300);
}
.foot-text {
  font-size: var(--hd-text-overline);
  line-height: var(--hd-text-overline-line);
  letter-spacing: 0.1em;
  color: var(--hd-neutral-400);
}
</style>
