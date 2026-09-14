<template>
  <div v-if="showTab" class="app-shell">
    <TabBar />
    <main class="app-main">
      <router-view />
    </main>
  </div>
  <router-view v-else />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import TabBar from './layout/TabBar.vue'

const route = useRoute()

// 登录/注册页为游客态：无侧边栏
const showTab = computed(() => !route.meta.hideTab)
</script>

<style scoped>
/* 网页端布局：左侧边栏 + 右侧内容区（内容区独立滚动） */
.app-shell {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.app-main {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
}
</style>
