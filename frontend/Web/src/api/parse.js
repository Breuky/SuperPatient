import api from './index'

export const parseApi = {
  /**
   * 化验单抽取：只返回候选值，不落库。
   * 模型识别一张单子要 30-60 秒，必须覆盖全局的 15 秒超时，否则请求还没回来就被前端中断。
   */
  report(imageUrl, categoryId, doubleRead = false) {
    return api.post(
      '/parse/report',
      { imageUrl, categoryId, doubleRead },
      { timeout: doubleRead ? 240000 : 120000 }
    )
  },

  /**
   * 直接传图片识别，服务端用完即弃：不写磁盘、不建文件记录。
   * 前端不设置 Content-Type，交给 axios 自动带 boundary。
   */
  reportImage(file, categoryId, doubleRead = false) {
    const form = new FormData()
    form.append('file', file)
    form.append('categoryId', categoryId)
    form.append('doubleRead', String(doubleRead))
    return api.post('/parse/report-image', form, {
      timeout: doubleRead ? 240000 : 120000
    })
  }
}
