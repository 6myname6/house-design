import { defineStore } from 'pinia'
import { loginApi, registerApi, getMe, updateMe, smsLoginApi, emailLoginApi, logoutApi } from '../api/auth'
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
    // 手机验证码登录：手机号必须已注册
    async loginByPhone(phone, code) {
      const token = await smsLoginApi({ phone, code })
      this.token = token
      setToken(token)
      useAiChatStore().bindUser(getUserIdFromToken())
      // 登录成功后拉一次用户信息回显
      await this.fetchMe()
    },
    // 邮箱验证码登录：邮箱必须已注册（A-11）
    async loginByEmail(email, code) {
      const token = await emailLoginApi({ email, code })
      this.token = token
      setToken(token)
      useAiChatStore().bindUser(getUserIdFromToken())
      await this.fetchMe()
    },
    // 注册：用户名 + 密码 + 验证码 + 手机/邮箱（二选一）；成功后自动登录
    async register({ username, password, code, phone, email }) {
      await registerApi({ username, password, code, phone, email })
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
    // 仅清理本地登录态（改密成功后 token 已被服务端拉黑，不能再调 /logout 接口）
    clearSession() {
      this.token = ''
      this.userInfo = null
      removeToken()
      useAiChatStore().reset()
    },
    // 登出：先请求后端把 token 拉黑（失败也吞掉），无论成败都清本地状态，保证退出闭环
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 接口失败不阻塞本地清理（如 token 已过期，本地同样要退出）
      }
      this.token = ''
      this.userInfo = null
      removeToken()
      useAiChatStore().reset()
    }
  }
})