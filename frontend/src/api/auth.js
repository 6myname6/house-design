import request from './request'

// 注册：返回 userId
export function registerApi(data) {
  return request.post('/api/auth/register', data)
}

// 登录：返回 token 字符串
export function loginApi(data) {
  return request.post('/api/auth/login', data)
}

// 获取当前用户信息
export function getMe() {
  return request.get('/api/auth/me')
}

// 更新个人信息（昵称/头像）
export function updateMe(data) {
  return request.put('/api/auth/me', data)
}