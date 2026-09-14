import request from './request'

// 项目列表（仅当前用户）
export function listProjects() {
  return request.get('/api/projects')
}

// 项目详情
export function getProject(id) {
  return request.get(`/api/projects/${id}`)
}

// 创建项目：multipart/form-data（含设计图文件）
// formData 字段：name、description(可选)、style(可选)、styleLabel(可选)、designImage(文件)
export function createProject(formData) {
  return request.post('/api/projects', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 删除项目
export function deleteProject(id) {
  return request.delete(`/api/projects/${id}`)
}