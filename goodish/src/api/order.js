import request from '@/utils/request'

const O = '/o'

export const orderApi = {
  create: (data) => request.post(`${O}/order/create`, data),
  cancel: (orderId) => request.post(`${O}/order/cancel`, { orderId }),
  requestRefund: (orderId) => request.post(`${O}/order/requestRefund`, { orderId }),
  handleRefund: (orderId, approve) => request.post(`${O}/order/handleRefund`, { orderId, approve: String(approve) }),
  complete: (orderId) => request.post(`${O}/order/complete`, { orderId }),
  myOrders: () => request.get(`${O}/order/myOrders`),
  mySells: () => request.get(`${O}/order/mySells`),
  detail: (orderId) => request.get(`${O}/order/detail`, { params: { orderId } }),
}

export const walletApi = {
  recharge: (amount) => request.post(`${O}/wallet/recharge`, { amount }),
  balance: () => request.get(`${O}/wallet/balance`),
  records: () => request.get(`${O}/wallet/records`),
}
