import api from './index'

export const authApi = {
  login(data) { return api.post('/auth/login', data) },
  register(data) { return api.post('/auth/register', data) },
  getProfile() { return api.get('/auth/profile') },
  updateProfile(data) { return api.put('/auth/profile', data) },
  logout() { return api.post('/auth/logout') },
  getStatistics() { return api.get('/auth/statistics') }
}
