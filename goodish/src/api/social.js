import request from '@/utils/request'

const P = '/p'

export const favoriteApi = {
  toggle: (productId) => request.post(`${P}/favorite/toggle`, { productId }),
  list: (params) => request.get(`${P}/favorite/list`, { params }),
}

export const followApi = {
  toggle: (followUserId) => request.post(`${P}/follow/toggle`, { followUserId }),
  follows: (params) => request.get(`${P}/follow/follows`, { params }),
  fans: (params) => request.get(`${P}/follow/fans`, { params }),
}

export const commentApi = {
  add: (data) => request.post(`${P}/comment/add`, data),
  reply: (data) => request.post(`${P}/comment/reply`, data),
  delete: (commentId) => request.post(`${P}/comment/delete`, { commentId }),
  like: (commentId) => request.post(`${P}/comment/like`, { commentId }),
  list: (productId) => request.get(`${P}/comment/list`, { params: { productId } }),
}
