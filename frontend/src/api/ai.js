import request from './request'

// AI 设计助手对话
// question: 文本提问（纯图场景传空串）
// images: DataURL 字符串数组（纯文本场景传空数组或不传）
// 二者至少传一个；多模态调用含服务端重试，单独放宽到 120s，覆盖全局 30s 超时
export function chatWithAi({ question, images }) {
  return request.post('/api/ai/chat', { question, images }, { timeout: 120000 })
}
