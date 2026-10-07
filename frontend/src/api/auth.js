import request from './request'

// 注册：返回 userId
export function registerApi(data) {
  return request.post('/api/auth/register', data)
}

// 登录：返回 token 字符串
export function loginApi(data) {
  return request.post('/api/auth/login', data)
}

// 发送短信验证码（手机登录）：data = { phone }，无返回数据
export function sendSmsCodeApi(data) {
  return request.post('/api/auth/sms/code', data)
}

// 手机号验证码登录：data = { phone, code }，返回 token 字符串
export function smsLoginApi(data) {
  return request.post('/api/auth/sms/login', data)
}

// 退出登录：无请求体，后端把当前 token 写入 Redis 黑名单
export function logoutApi() {
  return request.post('/api/auth/logout')
}

// 获取当前用户信息
export function getMe() {
  return request.get('/api/auth/me')
}

// 更新个人信息（昵称/头像）
export function updateMe(data) {
  return request.put('/api/auth/me', data)
}