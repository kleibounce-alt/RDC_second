import axios from 'axios'
import router from '@/router'

const request = axios.create({
  baseURL: '',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code === 'number' && body.code !== 0 && body.code !== 200) {
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return body
  },
  async (err) => {
    if (err.response?.status === 401) {
      const hadToken = !!localStorage.getItem('accessToken')
      if (hadToken) {
        const refreshToken = localStorage.getItem('refreshToken')
        if (refreshToken && !err.config._retry) {
          err.config._retry = true
          try {
            const res = await axios.post('/u/refresh-token', { refreshToken })
            if (res.data?.code === 0 || res.data?.code === 200) {
              const { accessToken, refreshToken: newRefresh } = res.data.data
              localStorage.setItem('accessToken', accessToken)
              if (newRefresh) localStorage.setItem('refreshToken', newRefresh)
              err.config.headers.Authorization = `Bearer ${accessToken}`
              return request(err.config)
            }
          } catch (_) { /* fall through */ }
        }
        localStorage.clear()
        router.replace('/login')
        return Promise.reject(new Error('请重新登录'))
      }
      return Promise.reject(new Error('需要登录'))
    }
    const msg = err.response?.data?.msg || err.message || '网络错误'
    return Promise.reject(new Error(msg))
  },
)

export default request
