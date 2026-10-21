import api from './index'

export const referenceRangeApi = {
  list(indicatorId) { return api.get(`/indicators/${indicatorId}/reference-ranges`) },
  create(data) { return api.post('/reference-ranges', data) },
  update(id, data) { return api.put(`/reference-ranges/${id}`, data) },
  remove(id) { return api.delete(`/reference-ranges/${id}`) }
}
