import request from '@/utils/request'

const M = '/m'

export const messageApi = {
  list: (params) => request.get(`${M}/message/list`, { params }),
  unreadCount: () => request.get(`${M}/message/unreadCount`),
  read: (messageId) => request.post(`${M}/message/read`, { messageId }),
  readAll: () => request.post(`${M}/message/readAll`),
  chatHistory: (otherId) => request.get(`${M}/chat/history`, { params: { otherId } }),
}
