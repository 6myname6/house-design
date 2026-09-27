import { defineStore } from 'pinia'
import { loginApi, registerApi, getMe, updateMe, smsLoginApi } from '../api/auth'
import { getToken, setToken, removeToken, getUserIdFromToken } from '../utils/storage'
import { useAiChatStore } from './aiChat'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: null // { id, username, nickname, avatar }
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    // 登录：拿 token 存 localStorage + state
    async login(username, password) {
      const token = await loginApi({ username, password })
      this.token = token
      setToken(token)
      // 切换 AI 会话库到新登录用户，防止看到上一个账号的本地会话
      useAiChatStore().bindUser(getUserIdFromToken())
      // 登录成功后拉一次用户信息回显
      await this.fetchMe()
    },
    // 手机验证码登录：拿 token 存 localStorage + state（新手机号后端自动建档）
    async loginByPhone(phone, code) {
      const token = await smsLoginApi({ phone, code })
      this.token = token
      setToken(token)
      useAiChatStore().bindUser(getUserIdFromToken())
      // 登录成功后拉一次用户信息回显
      await this.fetchMe()
    },
    // 注册：成功后自动登录
    async register(username, password) {
      await registerApi({ username, password })
      await this.login(username, password)
    },
    // 拉取当前用户信息
    async fetchMe() {
      this.userInfo = await getMe()
    },
    // 更新昵称/头像并刷新本地信息
    async updateProfile(data) {
      this.userInfo = await updateMe(data)
    },
    // 登出：清 token + 用户信息 + 内存中的 AI 会话（各人数据仍保留在各自 localStorage 键中）
    logout() {
      this.token = ''
      this.userInfo = null
      removeToken()
      useAiChatStore().reset()
    }
  }
})