import api from './index'

export const fileApi = {
  upload(file, type = 'report') {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('type', type)
    // 图片可能有几 MB，慢网络下超过默认的 15 秒
    return api.post('/files/upload', formData, { timeout: 60000 })
  }
}
