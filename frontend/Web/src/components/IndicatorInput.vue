<template>
  <div class="indicator-input-row" :class="{ expanded: showRange }">
    <div class="left">
      <div class="name">{{ indicator.name }}</div>
      <div class="range" v-if="indicator.valueType === 'number'">
        <span>{{ customRange ? '本次区间 ' + customRange : (rangeText ? '正常范围 ' + rangeText : '未设置区间') }}</span>
        <button type="button" class="range-toggle" @click="showRange = !showRange">
          {{ showRange ? '收起' : '填报告区间' }}
        </button>
      </div>
      <div class="range" v-else-if="indicator.valueType === 'text'">文本描述</div>

      <div v-if="showRange && indicator.valueType === 'number'" class="range-edit">
        <input
          type="number" class="range-input" placeholder="下限"
          :value="modelValue?.refMin ?? ''" @input="onRangeInput('min', $event)"
        />
        <span class="dash">~</span>
        <input
          type="number" class="range-input" placeholder="上限"
          :value="modelValue?.refMax ?? ''" @input="onRangeInput('max', $event)"
        />
        <button v-if="customRange" type="button" class="range-clear" @click="clearRange">恢复默认</button>
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
import { computed, ref } from 'vue'

const props = defineProps({
  indicator: { type: Object, required: true },
  modelValue: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue'])

const showRange = ref(false)

const rangeText = computed(() => {
  const min = props.indicator.normalMin
  const max = props.indicator.normalMax
  if (min != null && max != null) return `${min}-${max}`
  if (min != null) return `>${min}`
  if (max != null) return `<${max}`
  return ''
})

/** 用户自己填的区间（来自报告），有值就说明这条不走规则 */
const customRange = computed(() => {
  const min = props.modelValue?.refMin
  const max = props.modelValue?.refMax
  if (min == null && max == null) return ''
  if (min != null && max != null) return `${min}-${max}`
  return min != null ? `>${min}` : `<${max}`
})

const placeholder = computed(() => {
  return rangeText.value ? `正常 ${rangeText.value}` : '输入数值'
})

function base() {
  return { ...props.modelValue, indicatorId: props.indicator.id }
}

function onNumberInput(e) {
  const val = e.target.value
  // 保留 refMin/refMax/refSource 等字段，否则手改一次数值就把报告区间丢了
  emit('update:modelValue', {
    ...base(),
    valueDecimal: val === '' ? null : parseFloat(val),
    valueText: null
  })
}

function onTextInput(e) {
  emit('update:modelValue', {
    ...base(),
    valueDecimal: null,
    valueText: e.target.value || null
  })
}

/** 手填区间：视为「报告上印的」，来源写成 report；两个都清空则回落到规则 */
function onRangeInput(which, e) {
  const raw = e.target.value
  const num = raw === '' ? null : parseFloat(raw)
  const next = { ...base() }
  if (which === 'min') next.refMin = num
  else next.refMax = num

  const filled = next.refMin != null || next.refMax != null
  next.refSource = filled ? 'report' : null
  if (!filled) next.refText = null
  emit('update:modelValue', next)
}

function clearRange() {
  emit('update:modelValue', { ...base(), refMin: null, refMax: null, refText: null, refSource: null })
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
.indicator-input-row.expanded { align-items: flex-start; }
.left { flex: 1; min-width: 0; margin-right: 12px; }
.name { font-size: var(--font-base); color: var(--text-primary); font-weight: 500; }
.range { font-size: var(--font-xs); color: var(--text-hint); margin-top: 2px; display: flex; align-items: center; gap: 8px; }
.range-toggle {
  border: none; background: none; padding: 0; cursor: pointer;
  font-size: var(--font-xs); color: var(--green-dark, #0a8f4c);
  text-decoration: underline;
}
.range-edit { display: flex; align-items: center; gap: 6px; margin-top: 6px; }
.range-input {
  width: 76px; height: 26px; background: var(--bg-primary);
  border: 1px solid var(--border); border-radius: var(--radius-sm);
  text-align: center; font-size: var(--font-xs);
}
.range-input:focus { border-color: var(--green); background: var(--bg-white); }
.dash { color: var(--text-hint); font-size: var(--font-xs); }
.range-clear {
  border: none; background: none; cursor: pointer; padding: 0;
  font-size: var(--font-xs); color: var(--text-hint); text-decoration: underline;
}
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
