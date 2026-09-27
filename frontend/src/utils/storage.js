// localStorage 封装：统一管理与 token 有关的键
const TOKEN_KEY = 'house_design_token'

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
 * 从 JWT 的 payload 中解析 userId（sub 声明）。
 * 仅做 base64 解码，用于前端本地数据按用户隔离；不做验签，任何鉴权一律以后端校验为准。
 * @returns {string|null} userId 字符串；无 token 或解析失败返回 null
 */
export function getUserIdFromToken() {
  const token = getToken()
  if (!token) return null
  try {
    const payload = token.split('.')[1]
    // JWT 使用 base64url，需先转回标准 base64
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const claims = JSON.parse(atob(normalized))
    return claims.sub != null ? String(claims.sub) : null
  } catch {
    return null
  }
}