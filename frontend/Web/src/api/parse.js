import api from './index'

export const parseApi = {
  /** 化验单抽取：只返回候选值，不落库 */
  report(imageUrl, categoryId) {
    return api.post('/parse/report', { imageUrl, categoryId })
  }
}
