import request from './request'

// 风格列表：返回 [{ code, label }]
export function fetchStyles() {
  return request.get('/api/styles')
}