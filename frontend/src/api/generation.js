import request from './request'

// 发起 3D 生成
export function generate(projectId) {
  return request.post(`/api/projects/${projectId}/generate`)
}

// 查询单个生成任务
export function getGeneration(id) {
  return request.get(`/api/generations/${id}`)
}

// 项目下的生成记录
export function listProjectGenerations(projectId) {
  return request.get(`/api/projects/${projectId}/generations`)
}