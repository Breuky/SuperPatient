<template>
  <div class="page-header">
    <div class="left">
      <el-button v-if="back" text class="back" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>{{ backText }}</span>
      </el-button>
      <div class="titles">
        <h2>{{ title }}</h2>
        <p v-if="subtitle" class="sub">{{ subtitle }}</p>
      </div>
    </div>
    <div class="actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  back: { type: Boolean, default: false },
  backText: { type: String, default: '返回' },
  /** 返回目标路由，不传则用浏览器后退 */
  backTo: { type: String, default: '' }
})

const router = useRouter()

function goBack() {
  if (props.backTo) router.push(props.backTo)
  else router.back()
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}
.left { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.back { align-self: flex-start; margin-left: -8px; padding: 4px 8px; color: var(--text-hint); }
.back:hover { color: var(--text-primary); }
.titles h2 {
  font-size: var(--font-xl);
  font-weight: 700;
  letter-spacing: -0.015em;
  line-height: 1.25;
  color: var(--text-primary);
}
.sub { font-size: var(--font-sm); color: var(--text-hint); }
.actions { display: flex; align-items: center; gap: 8px; flex: none; }

@media (max-width: 720px) {
  .page-header { flex-direction: column; align-items: stretch; }
  .actions { justify-content: flex-end; }
}
</style>
