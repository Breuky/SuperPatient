import api from './index'

export const statisticsApi = {
  /** 当前用户全部记录的宏观统计，用于助手页展示「范围」 */
  overview() {
    return api.get('/statistics/overview')
  }
}
