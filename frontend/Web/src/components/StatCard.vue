<template>
  <div class="stat-card" :class="tone">
    <div class="label">{{ label }}</div>
    <div class="value-row">
      <span class="value">{{ value }}</span>
      <span v-if="unit" class="unit">{{ unit }}</span>
    </div>
    <div v-if="hint" class="hint">{{ hint }}</div>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: '-' },
  unit: { type: String, default: '' },
  hint: { type: String, default: '' },
  /** default | green | red | blue */
  tone: { type: String, default: 'default' }
})
</script>

<style scoped>
.stat-card {
  padding: 16px 18px;
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  height: 100%;
}
.label { font-size: var(--font-sm); color: var(--text-hint); }
.value-row { display: flex; align-items: baseline; gap: 4px; margin-top: 6px; }
.value {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.15;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}
.unit { font-size: var(--font-sm); color: var(--text-hint); }
.hint { margin-top: 6px; font-size: var(--font-xs); color: var(--text-hint); }

.stat-card.green .value { color: var(--green-dark); }
.stat-card.red .value { color: var(--red); }
.stat-card.blue .value { color: var(--blue); }
</style>
