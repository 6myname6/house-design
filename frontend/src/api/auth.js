import request from './request'

// 注册：返回 userId
export function registerApi(data) {
  return request.post('/api/auth/register', data)
}

// 登录：返回 token 字符串
export function loginApi(data) {
  return request.post('/api/auth/login', data)
}

// 发送短信验证码：data = { phone, scene }，scene=login（已注册）/register（未注册），无返回数据
export function sendSmsCodeApi(data) {
  return request.post('/api/auth/sms/code', data)
}

// 手机号验证码登录：data = { phone, code }，返回 token 字符串
export function smsLoginApi(data) {
  return request.post('/api/auth/sms/login', data)
}

// 发送邮箱验证码：data = { email, scene }，scene=login（已注册）/register（未注册），无返回数据
export function sendEmailCodeApi(data) {
  return request.post('/api/auth/email/code', data)
}

// 邮箱验证码登录：data = { email, code }，返回 token 字符串
export function emailLoginApi(data) {
  return request.post('/api/auth/email/login', data)
}

// 发送修改密码验证码（需登录）：data = { channel: 'sms' | 'email' }，收件地址后端决定
export function sendPasswordCodeApi(data) {
  return request.post('/api/auth/password/code', data)
}

// 修改密码（需登录）：data = { channel, code, newPassword }，成功后当前 token 失效
export function changePasswordApi(data) {
  return request.post('/api/auth/password/change', data)
}

// 发送绑定/换绑邮箱验证码（需登录）：data = { email }，新邮箱必须未被占用
export function sendEmailBindCodeApi(data) {
  return request.post('/api/auth/bind/email/code', data)
}

// 确认绑定/换绑邮箱（需登录）：data = { email, code }，返回成功提示语
export function bindEmailApi(data) {
  return request.post('/api/auth/bind/email', data)
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