import request from './request'

// 上传文件，返回可访问的 URL 字符串
export function uploadFile(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/api/files/upload', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}