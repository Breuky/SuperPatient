import api from './index'

export const parseApi = {
  /**
   * 化验单抽取：只返回候选值，不落库。
   * 模型识别一张单子要 30-60 秒，必须覆盖全局的 15 秒超时，否则请求还没回来就被前端中断。
   */
  report(imageUrl, categoryId) {
    return api.post('/parse/report', { imageUrl, categoryId }, { timeout: 120000 })
  }
}
