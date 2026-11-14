import api from './index'

export const memoryApi = {
  /** 我记住的偏好与自述事实 */
  list() {
    return api.get('/memory')
  },
  /** kind 只能是 preference / fact */
  save(kind, key, value) {
    return api.post('/memory', { kind, key, value })
  },
  remove(id) {
    return api.delete('/memory/' + id)
  },
  clear() {
    return api.delete('/memory')
  }
}
