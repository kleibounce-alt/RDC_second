import request from '@/utils/request'

const P = '/p'

export const productApi = {
  publish: (data) => request.post(`${P}/publish`, data),
  edit: (data) => request.post(`${P}/edit`, data),
  delete: (productId) => request.post(`${P}/delete`, { productId }),
  offShelf: (productId) => request.post(`${P}/off-shelf`, { productId }),
  myProducts: () => request.get(`${P}/my-products`),
  detail: (productId) => request.get(`${P}/detail`, { params: { productId } }),
  list: (params) => request.get(`${P}/list`, { params }),
  search: (params) => request.get(`${P}/search`, { params }),
  upload: (formData) => request.post(`${P}/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
  tagList: () => request.get(`${P}/tag-list`),
  tagCreate: (name) => request.post(`${P}/tag-create`, { name }),

  userProducts: (userId) => request.get(`${P}/user-products`, { params: { userId } }),
}
