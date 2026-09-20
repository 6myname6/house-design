import { marked } from 'marked'
import DOMPurify from 'dompurify'

// GFM + 换行即 <br>，与多数聊天产品的阅读习惯一致
marked.setOptions({ gfm: true, breaks: true })

/**
 * 把 AI 返回的 Markdown 渲染为消毒后的安全 HTML。
 * AI 输出属于不可信内容，必须经 DOMPurify 过滤后才能 v-html，防止 XSS。
 */
export function renderMarkdown(text = '') {
  const rawHtml = marked.parse(text, { async: false })
  return DOMPurify.sanitize(rawHtml, { USE_PROFILES: { html: true } })
}
