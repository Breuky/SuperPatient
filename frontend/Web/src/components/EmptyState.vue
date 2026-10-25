<template>
  <div class="empty-state">
    <div class="icon">
      <el-icon><Files v-if="icon === 'file'" /><TrendCharts v-else-if="icon === 'trend'" /><Search v-else /></el-icon>
    </div>
    <p class="title">{{ title }}</p>
    <p v-if="description" class="desc">{{ description }}</p>
    <div v-if="$slots.default" class="actions"><slot /></div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: '还没有数据' },
  description: { type: String, default: '' },
  /** file | trend | default */
  icon: { type: String, default: 'default' }
})
</script>

<style scoped>
.empty-state {
  padding: 56px 24px;
  text-align: center;
}
.icon {
  width: 52px;
  height: 52px;
  margin: 0 auto 14px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: var(--green-light);
  color: var(--green);
  font-size: 24px;
}
.title { font-size: var(--font-md); font-weight: 600; color: var(--text-secondary); }
.desc { margin-top: 6px; font-size: var(--font-sm); color: var(--text-hint); }
.actions { margin-top: 18px; }
</style>
