import api from './index'

export const insightApi = {
  /** mode: simple（证据包单次生成）| agent（模型自己调工具取数） */
  report(recordId, mode = 'agent', question = '') {
    return api.post('/insights/report', { recordId, mode, question }, { timeout: 90000 })
  }
}
