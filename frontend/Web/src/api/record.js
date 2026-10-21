import api from './index'

export const recordApi = {
  list(params) { return api.get('/records', { params }) },
  detail(id) { return api.get(`/records/${id}`) },
  create(data) { return api.post('/records', data) },
  update(id, data) { return api.put(`/records/${id}`, data) },
  remove(id) { return api.delete(`/records/${id}`) }
}
