import request from './request'

// 帖子列表（分页；mine=true 只看我的）
export function listPosts(pageNum = 1, pageSize = 20, mine = false) {
  return request.get('/api/posts', { params: { pageNum, pageSize, mine } })
}

// 发表帖子：{ content, images }
export function createPost(data) {
  return request.post('/api/posts', data)
}

// 点赞 / 取消点赞
export function toggleLike(postId) {
  return request.post(`/api/posts/${postId}/like`)
}

// 帖子评论列表（含回复）
export function listComments(postId) {
  return request.get(`/api/posts/${postId}/comments`)
}

// 发表评论：{ content, images }
export function createComment(postId, data) {
  return request.post(`/api/posts/${postId}/comments`, data)
}

// 回复评论
export function replyComment(commentId, data) {
  return request.post(`/api/comments/${commentId}/replies`, data)
}

// 评论点赞 / 取消点赞
export function toggleCommentLike(commentId) {
  return request.post(`/api/comments/${commentId}/like`)
}

// 删除评论
export function deleteComment(commentId) {
  return request.delete(`/api/comments/${commentId}`)
}

// 删除帖子
export function deletePost(id) {
  return request.delete(`/api/posts/${id}`)
}