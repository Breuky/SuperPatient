import api from './index'

export const insightApi = {
  /** mode: simple（证据包单次生成）| agent（模型自己调工具取数） */
  report(recordId, mode = 'agent', question = '') {
    return api.post('/insights/report', { recordId, mode, question }, { timeout: 90000 })
  },
  /** 最近的调用留痕，用于观察 Agent 行为 */
  logs(limit = 50) {
    return api.get('/insights/logs', { params: { limit } })
  },
  /** 按题型的聚合统计：哪类问题最多、最贵、最容易降级 */
  stats() {
    return api.get('/insights/stats')
  }
}
