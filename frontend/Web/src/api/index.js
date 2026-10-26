import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000
  // 不设全局 Content-Type：普通对象由 axios 自动用 application/json，
  // FormData 必须让它自动带 boundary，手写 multipart/form-data 反而会坏
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.token = token
  }
  return config
})

api.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data.code === 0) {
      return Promise.reject(new Error(data.msg || '请求失败'))
    }
    return data
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(error.response.data?.msg || `服务器错误 (${status})`))
    }
    return Promise.reject(error)
  }
)

export default api
