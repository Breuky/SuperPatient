import api from './index'

export const myApi = {
  getMyPatient() { return api.get('/auth/my-patient') },
  updateMyPatient(data) { return api.put('/auth/my-patient', data) }
}
