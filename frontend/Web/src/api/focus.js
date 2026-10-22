import api from './index'

export const focusApi = {
  list() { return api.get('/focus-indicators') },
  add(indicatorId) { return api.post(`/focus-indicators/${indicatorId}`) },
  remove(indicatorId) { return api.delete(`/focus-indicators/${indicatorId}`) }
}
