import api from './index'

export const followUpApi = {
  list() { return api.get('/follow-ups') },
  updateStatus(id, status) { return api.put(`/follow-ups/${id}`, { status }) }
}
