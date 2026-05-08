import request from '@/utils/request'

const U = '/u'

export const userApi = {
  login: (data) => request.post(`${U}/login`, data),
  register: (data) => request.post(`${U}/register`, data),
  adminRegister: (data) => request.post(`${U}/admin-register`, data),
  logout: () => request.post(`${U}/logout`),
  refreshToken: (data) => request.post(`${U}/refresh-token`, data),

  profile: () => request.get(`${U}/profile`),
  updateProfile: (data) => request.post(`${U}/update-profile`, data),
  updatePassword: (data) => request.post(`${U}/update-password`, data),
  bindEmail: (data) => request.post(`${U}/bind-email`, data),
  searchUser: (keyword) => request.get(`${U}/search-user`, { params: { keyword } }),

  forgotPassword: (data) => request.post(`${U}/forgot-password`, data),
  resetPassword: (data) => request.post(`${U}/reset-password`, data),

  vipOrder: (data) => request.post(`${U}/vip/order`, data),
  vipPay: (data) => request.post(`${U}/vip/pay`, data),

  uploadAvatar: (formData) => request.post(`${U}/upload-avatar`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),

  userInfo: (userId) => request.get(`${U}/user-info`, { params: { userId } }),
}
