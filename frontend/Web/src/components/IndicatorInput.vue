<template>
  <div class="indicator-input-row">
    <div class="left">
      <div class="name">{{ indicator.name }}</div>
      <div class="range" v-if="indicator.valueType === 'number' && rangeText">
        正常范围 {{ rangeText }}
      </div>
      <div class="range" v-else-if="indicator.valueType === 'text'">
        文本描述
      </div>
    </div>
    <div class="input-area">
      <template v-if="indicator.valueType === 'number'">
        <input
          type="number"
          class="num-input"
          :value="modelValue?.valueDecimal ?? ''"
          :placeholder="placeholder"
          @input="onNumberInput"
        />
        <span class="unit" v-if="indicator.unit">{{ indicator.unit }}</span>
      </template>
      <template v-else>
        <input
          type="text"
          class="text-input"
          :value="modelValue?.valueText ?? ''"
          placeholder="输入描述"
          @input="onTextInput"
        />
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  indicator: { type: Object, required: true },
  modelValue: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue'])

const rangeText = computed(() => {
  const min = props.indicator.normalMin
  const max = props.indicator.normalMax
  if (min != null && max != null) return `${min}-${max}`
  if (min != null) return `>${min}`
  if (max != null) return `<${max}`
  return ''
})

const placeholder = computed(() => {
  return rangeText.value ? `正常 ${rangeText.value}` : '输入数值'
})

function onNumberInput(e) {
  const val = e.target.value
  emit('update:modelValue', {
    indicatorId: props.indicator.id,
    valueDecimal: val === '' ? null : parseFloat(val),
    valueText: null
  })
}

function onTextInput(e) {
  emit('update:modelValue', {
    indicatorId: props.indicator.id,
    valueDecimal: null,
    valueText: e.target.value || null
  })
}
</script>

<style scoped>
.indicator-input-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 0.5px solid var(--border-light);
}
.indicator-input-row:last-child { border-bottom: none; }
.left { flex: 1; min-width: 0; margin-right: 12px; }
.name { font-size: var(--font-base); color: var(--text-primary); font-weight: 500; }
.range { font-size: var(--font-xs); color: var(--text-hint); margin-top: 2px; }
.input-area { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.num-input {
  width: 80px;
  height: 32px;
  background: var(--bg-primary);
  border-radius: var(--radius-sm);
  text-align: center;
  font-size: var(--font-base);
  font-weight: 500;
  border: 1px solid transparent;
  transition: border-color 0.2s;
}
.num-input:focus { border-color: var(--green); background: var(--bg-white); }
.text-input {
  width: 140px;
  height: 32px;
  background: var(--bg-primary);
  border-radius: var(--radius-sm);
  padding: 0 10px;
  font-size: var(--font-sm);
  border: 1px solid transparent;
  transition: border-color 0.2s;
}
.text-input:focus { border-color: var(--green); background: var(--bg-white); }
.unit { font-size: var(--font-xs); color: var(--text-hint); min-width: 40px; }
</style>
