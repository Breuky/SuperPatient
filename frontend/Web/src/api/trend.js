import api from './index'

export const trendApi = {
  get(params) { return api.get('/trends', { params }) },
  getTrendable() { return api.get('/trendable-indicators') }
}
