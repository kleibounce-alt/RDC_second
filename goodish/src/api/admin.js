import request from '@/utils/request'

const A = '/a'

export const adminApi = {
  ban: (data) => request.post(`${A}/admin/ban`, data),
  unban: (data) => request.post(`${A}/admin/unban`, data),
  forceOffShelf: (productId) => request.post(`${A}/admin/forceOffShelf`, { productId }),
  relist: (productId) => request.post(`${A}/admin/relist`, { productId }),
  deleteComment: (commentId) => request.post(`${A}/admin/deleteComment`, { commentId }),
  deleteProduct: (productId) => request.post(`${A}/admin/deleteProduct`, { productId }),
  setExposure: (productId, level) => request.post(`${A}/admin/setExposure`, { productId, level: String(level) }),
  listSensitive: () => request.get(`${A}/admin/listSensitive`),
  addSensitive: (word) => request.post(`${A}/admin/addSensitive`, { word }),
  deleteSensitive: (id) => request.post(`${A}/admin/deleteSensitive`, { id }),

  auditPending: () => request.get(`${A}/admin/audit/pending`),
  auditApprove: (productId) => request.post(`${A}/admin/audit/approve`, { productId }),
  auditReject: (data) => request.post(`${A}/admin/audit/reject`, data),
  auditLogs: (productId) => request.get(`${A}/admin/audit/logs`, { params: { productId } }),

  offShelved: () => request.get(`${A}/admin/off-shelved`),
}
