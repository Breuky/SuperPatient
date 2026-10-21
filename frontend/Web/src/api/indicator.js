import api from './index'

export const indicatorApi = {
  list(params) { return api.get('/indicators', { params }) },
  create(data) { return api.post('/indicators', data) },
  update(id, data) { return api.put(`/indicators/${id}`, data) },
  updateStatus(id, status) { return api.put(`/indicators/${id}/status`, { status }) },
  remove(id) { return api.delete(`/indicators/${id}`) }
}
