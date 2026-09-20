import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/storage'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true, hideTab: true } // 游客态：无导航
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { public: true, hideTab: true }
  },
  {
    path: '/',
    redirect: '/projects' // Tab 1「首页」为默认落地页
  },
  {
    path: '/projects',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/projects/new',
    name: 'CreateProject',
    component: () => import('../views/CreateProject.vue') // 二级页：显示顶部导航
  },
  {
    path: '/projects/:id',
    name: 'ProjectDetail',
    component: () => import('../views/ProjectDetail.vue') // 二级页：显示顶部导航
  },
  {
    path: '/community',
    name: 'Community',
    component: () => import('../views/Community.vue')
  },
  {
    path: '/ai-chat',
    name: 'AiChat',
    component: () => import('../views/AiChat.vue')
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue')
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/Settings.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录守卫：非公开页面无 token 一律跳登录页
router.beforeEach((to) => {
  if (to.meta.public) {
    return true
  }
  if (!getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router