<template>
  <div>
    <PageHeader
      title="AI 助手"
      subtitle="范围是你的全部检查记录；问「这次」默认指最近一次，回答里会带日期"
    />
    <AgentChat empty-hint="可以问全部记录：一共查过几次、哪项指标一直偏高、某个指标这几年怎么变。" />
  </div>
</template>

<script setup>
import PageHeader from '@/components/PageHeader.vue'
import AgentChat from '@/components/AgentChat.vue'
</script>
