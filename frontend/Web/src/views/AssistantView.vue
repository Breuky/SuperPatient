<template>
  <div>
    <PageHeader
      title="AI 助手"
      subtitle="范围是你的全部检查记录；问「这次」默认指最近一次，回答里会带日期"
    />

    <div v-if="overview" class="scope-bar">
      <template v-if="overview.totalRecords">
        <span class="scope-label">当前范围</span>
        <span>全部 {{ overview.totalRecords }} 次记录</span>
        <span class="dot">·</span>
        <span>{{ overview.firstDate }} ~ {{ overview.lastDate }}</span>
        <span class="dot">·</span>
        <span>{{ overview.totalValues }} 项指标值</span>
      </template>
      <template v-else>
        <span class="scope-label">还没有记录</span>
        <span>先去添加一条检查记录，助手才有数据可查</span>
      </template>
    </div>

    <AgentChat title="AI 助手" empty-hint="可以问全部记录：一共查过几次、哪项指标一直偏高、某个指标这几年怎么变。" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import AgentChat from '@/components/AgentChat.vue'
import { statisticsApi } from '@/api/statistics'

const overview = ref(null)

onMounted(async () => {
  try {
    const res = await statisticsApi.overview()
    overview.value = res.data || null
  } catch (e) { console.error(e) }
})
</script>

<style scoped>
.scope-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
  padding: 8px 12px;
  border-radius: var(--radius-md);
  background: var(--bg-hover, #f7f8fa);
  font-size: var(--font-xs);
  color: var(--text-secondary);
}
.scope-label { color: var(--text-hint); margin-right: 2px; }
.dot { color: var(--text-hint); }
</style>
