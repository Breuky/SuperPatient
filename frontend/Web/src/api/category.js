import api from './index'

export const categoryApi = {
  list() { return api.get('/categories') },
  create(data) { return api.post('/categories', data) },
  update(id, data) { return api.put(`/categories/${id}`, data) },
  remove(id) { return api.delete(`/categories/${id}`) }
}
