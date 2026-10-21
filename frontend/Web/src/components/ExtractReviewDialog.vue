<template>
  <el-dialog v-model="open" title="识别结果校对" width="920px" append-to-body>
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="模型只负责抽取，保存前请逐项核对。取消勾选的项不会入库。"
      style="margin-bottom:12px"
    />

    <template v-if="rows.length">
      <div class="section-title">已匹配 {{ rows.length }} 项</div>
      <el-table :data="rows" size="small" border>
        <el-table-column width="46" align="center">
          <template #default="{ row }"><el-checkbox v-model="row.use" /></template>
        </el-table-column>
        <el-table-column label="指标" min-width="150">
          <template #default="{ row }">
            <div>{{ row.indicatorName }}</div>
            <div class="raw" v-if="row.rawName && row.rawName !== row.indicatorName">
              原文：{{ row.rawName }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="120">
          <template #default="{ row }">
            <el-input v-model="row.valueInput" size="small" placeholder="结果" />
          </template>
        </el-table-column>
        <el-table-column label="单位" width="90">
          <template #default="{ row }"><span class="raw">{{ row.unit || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="参考区间" width="180">
          <template #default="{ row }">
            <div class="range">
              <el-input v-model="row.refMin" size="small" placeholder="下限" />
              <span>~</span>
              <el-input v-model="row.refMax" size="small" placeholder="上限" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="判定" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="abnormalOf(row) === 1" type="danger" size="small">偏高</el-tag>
            <el-tag v-else-if="abnormalOf(row) === 2" type="primary" size="small">偏低</el-tag>
            <el-tag v-else type="info" size="small">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提示" min-width="150">
          <template #default="{ row }">
            <span v-if="row.warning" class="warn">{{ row.warning }}</span>
            <span v-else-if="row.matchType !== 'exact'" class="raw">
              别名匹配 {{ Math.round((row.confidence || 0) * 100) }}%
            </span>
            <span v-else class="raw">-</span>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <template v-if="extras.length">
      <div class="section-title">未匹配 {{ extras.length }} 项（指标库里没有，可手动指定）</div>
      <el-table :data="extras" size="small" border>
        <el-table-column width="46" align="center">
          <template #default="{ row }"><el-checkbox v-model="row.use" /></template>
        </el-table-column>
        <el-table-column prop="rawName" label="报告原文" width="170" />
        <el-table-column label="对应指标" min-width="180">
          <template #default="{ row }">
            <el-select v-model="row.indicatorId" size="small" clearable filterable placeholder="选择指标">
              <el-option v-for="i in indicators" :key="i.id" :label="i.name" :value="i.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="110">
          <template #default="{ row }">
            <el-input v-model="row.valueInput" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="rawUnit" label="单位" width="80" />
        <el-table-column prop="refText" label="参考区间" width="120" />
      </el-table>
    </template>

    <template #footer>
      <span class="summary">
        识别到：{{ result?.examDate || '无日期' }} · {{ result?.hospital || '无医院' }}（应用时一并填入）
      </span>
      <el-button @click="open = false">取消</el-button>
      <el-button type="primary" @click="handleApply">应用到表单</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  result: { type: Object, default: null },
  indicators: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:visible', 'apply'])

const open = computed({
  get: () => props.visible,
  set: (v) => emit('update:visible', v)
})

const rows = ref([])
const extras = ref([])

watch(() => [props.visible, props.result], () => {
  if (!props.visible) return
  const r = props.result || {}
  rows.value = (r.candidates || []).map(c => ({
    ...c,
    use: true,
    valueInput: c.valueDecimal != null ? String(c.valueDecimal) : (c.valueText || ''),
    refMin: c.refMin != null ? String(c.refMin) : '',
    refMax: c.refMax != null ? String(c.refMax) : ''
  }))
  extras.value = (r.unmatched || []).map(u => ({
    ...u,
    use: false,
    indicatorId: null,
    valueInput: u.rawValue || ''
  }))
}, { immediate: true })

function num(v) {
  if (v === '' || v === null || v === undefined) return null
  const n = Number(v)
  return Number.isNaN(n) ? null : n
}

function abnormalOf(row) {
  const v = num(row.valueInput)
  const min = num(row.refMin)
  const max = num(row.refMax)
  if (v === null) return 0
  if (max !== null && v > max) return 1
  if (min !== null && v < min) return 2
  return 0
}

function handleApply() {
  const items = []
  const push = (row) => {
    if (!row.indicatorId) return
    const item = {
      indicatorId: row.indicatorId,
      rawName: row.rawName || null,
      rawUnit: row.rawUnit || null,
      refText: row.refText || null
    }
    const min = num(row.refMin)
    const max = num(row.refMax)
    if (min !== null) item.refMin = min
    if (max !== null) item.refMax = max

    const value = num(row.valueInput)
    if (value !== null) item.valueDecimal = value
    else if (row.valueInput) item.valueText = row.valueInput
    else return

    items.push(item)
  }

  rows.value.filter(r => r.use).forEach(push)
  extras.value.filter(r => r.use && r.indicatorId).forEach(push)

  emit('apply', items)
  open.value = false
}
</script>

<style scoped>
.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin: 14px 0 8px;
}
.raw { color: #909399; font-size: 12px; }
.warn { color: #e6a23c; font-size: 12px; }
.range { display: flex; align-items: center; gap: 4px; }
.summary { float: left; color: #909399; font-size: 12px; line-height: 32px; }
</style>
