import api from './index'

export const fileApi = {
  upload(file, type = 'report') {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('type', type)
    return api.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
