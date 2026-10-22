<template>
  <div>
    <el-card v-loading="loading">
      <div style="margin-bottom:16px">
        <el-form-item label="选择指标" style="margin:0">
          <el-select
            v-model="selectedId"
            style="width:400px"
            placeholder="选择指标"
            @change="loadTrend"
            :disabled="!trendable.length"
          >
            <el-option
              v-for="item in visibleTrendable" :key="item.indicatorId"
              :label="`${item.indicatorName} (${item.unit} · ${item.recordCount}次)`"
              :value="item.indicatorId"
            />
          </el-select>
          <el-checkbox v-model="onlyFocused" style="margin-left:16px" @change="onFocusFilterChange">
            只看关注指标
          </el-checkbox>
          <span v-if="onlyFocused && !visibleTrendable.length" class="focus-empty">
            还没有关注的指标，去录入页点亮 ★
          </span>
        </el-form-item>
      </div>

      <el-empty v-if="!trendable.length" description="暂无可分析的指标数据" />

      <template v-if="trendData">
        <TrendChart :indicator="trendData.indicator" :points="trendData.points" />
        <div class="chart-hint">
          绿色区域为该次报告印刷的参考区间；不同医院/不同时期的区间可能不同，因此区间会呈阶梯变化。
        </div>

        <el-card v-if="trendData.points.length" header="历史数据" style="margin-top:16px" body-style="padding: 0">
          <el-table :data="trendData.points" stripe>
            <el-table-column prop="examDate" label="日期" width="140" align="center" />
            <el-table-column label="结果" min-width="180" align="center">
              <template #default="{ row }">
                <span :class="valueClass(row.isAbnormal)" style="font-size:15px;font-weight:500">
                  {{ row.value }} <small style="color:#999;font-weight:400;font-size:12px">{{ trendData.indicator.unit }}</small>
                </span>
                <el-tag v-if="row.isAbnormal === 1" type="danger" style="margin-left:8px">↑ 偏高</el-tag>
                <el-tag v-else-if="row.isAbnormal === 2" type="primary" style="margin-left:8px">↓ 偏低</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="参考区间" min-width="200" align="center">
              <template #default="{ row }">
                <span style="color:#909399">{{ rangeText(row) }}</span>
                <el-tag
                  v-if="row.refMin != null || row.refMax != null"
                  size="small"
                  type="info"
                  style="margin-left:6px"
                >本次报告</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { trendApi } from '@/api/trend'
import { focusApi } from '@/api/focus'
import TrendChart from '@/components/TrendChart.vue'

const trendable = ref([])
const trendData = ref(null)
const selectedId = ref(null)
const loading = ref(true)
const onlyFocused = ref(false)
const focusIds = ref(new Set())

const visibleTrendable = computed(() =>
  onlyFocused.value
    ? trendable.value.filter(t => focusIds.value.has(t.indicatorId))
    : trendable.value
)

function onFocusFilterChange() {
  const list = visibleTrendable.value
  if (!list.some(t => t.indicatorId === selectedId.value)) {
    selectedId.value = list.length ? list[0].indicatorId : null
    trendData.value = null
    if (selectedId.value) loadTrend()
  }
}

function rangeText(row) {
  const ind = trendData.value?.indicator || {}
  const min = row.refMin ?? ind.normalMin
  const max = row.refMax ?? ind.normalMax
  if (min == null && max == null) return '-'
  return `${min ?? '-'} ~ ${max ?? '-'}`
}

function valueClass(isAbnormal) {
  if (isAbnormal === 1) return 'value-abnormal-high'
  if (isAbnormal === 2) return 'value-abnormal-low'
  return ''
}

async function loadTrend() {
  if (!selectedId.value) return
  try {
    const res = await trendApi.get({ indicatorId: selectedId.value })
    trendData.value = res.data
  } catch (e) { console.error(e) }
}

onMounted(async () => {
  try {
    const focusRes = await focusApi.list()
    focusIds.value = new Set((focusRes.data || []).map(i => i.id))
  } catch (e) { console.error(e) }
  try {
    const res = await trendApi.getTrendable()
    trendable.value = res.data
    if (res.data.length) {
      selectedId.value = res.data[0].indicatorId
      loadTrend()
    }
  } catch (e) { console.error(e) }
  loading.value = false
})
</script>

<style scoped>
.focus-empty { font-size: 12px; color: #909399; margin-left: 8px; }
.chart-hint {
  color: var(--text-hint, #999);
  font-size: 12px;
  margin: -4px 0 12px 4px;
}
</style>
