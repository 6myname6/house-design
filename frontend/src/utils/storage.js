// localStorage 封装：统一管理与 token 有关的键
const TOKEN_KEY = 'house_design_token'

// 时钟漂移/请求在途缓冲：提前 30s 视为过期，避免令牌在请求飞行途中失效
const EXP_SKEW_MS = 30 * 1000

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 解析 JWT 的 payload（仅 base64 解码，不验签，任何鉴权一律以后端校验为准）。
 * @param {string} [token] 缺省取当前存储的 token
 * @returns {object|null} claims 对象；无 token 或解析失败返回 null
 */
function parseClaims(token = getToken()) {
  if (!token) return null
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    // JWT 使用 base64url，需先转回标准 base64
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(normalized))
  } catch {
    return null
  }
}

/**
 * 判断 token 本地是否仍有效：存在、可解析且 exp 未过期。
 * 不做签名校验（前端无密钥），仅用于路由守卫/请求层的前置拦截；最终鉴权以后端为准。
 * @param {string} [token] 缺省取当前存储的 token
 * @returns {boolean}
 */
export function isTokenValid(token = getToken()) {
  if (!token) return false
  const claims = parseClaims(token)
  // 无法解析或缺少 exp 声明，保守视为无效
  if (!claims || typeof claims.exp !== 'number') return false
  return claims.exp * 1000 > Date.now() + EXP_SKEW_MS
}

/**
 * 从 JWT 的 payload 中解析 userId（sub 声明）。
 * 仅做 base64 解码，用于前端本地数据按用户隔离；不做验签，任何鉴权一律以后端校验为准。
 * @returns {string|null} userId 字符串；无 token 或解析失败返回 null
 */
export function getUserIdFromToken() {
  const claims = parseClaims()
  return claims?.sub != null ? String(claims.sub) : null
}