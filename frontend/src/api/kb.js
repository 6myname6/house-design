import request from './request'

/**
 * 创建知识库文档（文件与文本二选一）。
 *
 * @param {Object} payload
 * @param {File}   [payload.file]       上传的文档文件，与 text 二选一
 * @param {string} [payload.text]       直接录入的正文，与 file 二选一
 * @param {string} [payload.title]      标题，不传由后端按文件名/默认值补齐
 * @param {string} [payload.visibility] PUBLIC（默认）/ PRIVATE
 * @returns {Promise<{documentId:number, status:string}>} 创建结果，入库为异步
 */
export function createKbDocument({ file, text, title, visibility }) {
  const fd = new FormData()
  if (file) fd.append('file', file)
  if (text) fd.append('text', text)
  if (title) fd.append('title', title)
  if (visibility) fd.append('visibility', visibility)
  return request.post('/api/kb/documents', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
    // 文档上传+解析链路较长，单独放宽到 60s，避免全局 30s 提前中断
    timeout: 60000
  })
}

/**
 * 分页查询当前用户可见的文档（PUBLIC 全部 + 自己的 PRIVATE）。
 *
 * @param {Object} params
 * @param {number} [params.pageNum]  页码，从 1 开始
 * @param {number} [params.pageSize] 每页条数，最大 50
 * @param {string} [params.status]   状态过滤：PENDING/PARSING/INDEXING/READY/FAILED
 * @returns {Promise<{total:number, records:Array}>}
 */
export function listKbDocuments({ pageNum = 1, pageSize = 10, status } = {}) {
  return request.get('/api/kb/documents', { params: { pageNum, pageSize, status } })
}

/**
 * 查询单个文档详情（用于轮询入库状态）。
 *
 * @param {number} id 文档 ID
 * @returns {Promise<Object>} 文档详情
 */
export function getKbDocument(id) {
  return request.get(`/api/kb/documents/${id}`)
}

/**
 * 重建索引：版本号 +1 后重新解析/切块/向量化，失败文档也用它重试。
 *
 * @param {number} id 文档 ID
 * @returns {Promise<Object>} 重建后的文档（status 立即变为 PENDING）
 */
export function reindexKbDocument(id) {
  return request.post(`/api/kb/documents/${id}/reindex`)
}

/**
 * 删除文档：同步清理分块与向量，仅上传者本人可操作。
 *
 * @param {number} id 文档 ID
 * @returns {Promise<null>}
 */
export function deleteKbDocument(id) {
  return request.delete(`/api/kb/documents/${id}`)
}
