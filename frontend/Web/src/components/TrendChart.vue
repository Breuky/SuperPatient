<template>
  <div class="chart-wrap">
    <div ref="chartRef" class="chart" v-show="hasData"></div>
    <div v-if="!hasData" class="empty">暂无趋势数据</div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  indicator: { type: Object, default: null },
  points: { type: Array, default: () => [] }
})

const chartRef = ref(null)
let chart = null

const hasData = computed(() => props.points && props.points.length > 0)

/**
 * 每个点用自己的参考区间（本次报告印刷值），缺失时回落指标默认区间。
 * 同一指标不同医院区间不同，所以不能画一条固定的带。
 */
function refOf(point) {
  const ind = props.indicator || {}
  const min = point?.refMin ?? ind.normalMin
  const max = point?.refMax ?? ind.normalMax
  return [min ?? null, max ?? null]
}

function refLabel(point) {
  const [min, max] = refOf(point)
  if (point?.refText) return point.refText
  if (min != null && max != null) return `${min} ~ ${max}`
  if (min != null) return `> ${min}`
  if (max != null) return `< ${max}`
  return '-'
}

function buildOption() {
  const points = props.points || []
  const dates = points.map(p => p.examDate || '')
  const values = points.map(p => p.value)
  const unit = props.indicator?.unit || ''
  const name = props.indicator?.name || '指标'

  // 参考带你用两个堆叠 series 实现，step 让区间在两次复查之间保持阶梯状
  const bandBase = points.map(p => refOf(p)[0])
  const bandSpan = points.map(p => {
    const [min, max] = refOf(p)
    return min != null && max != null ? Number((max - min).toFixed(4)) : null
  })
  const hasBand = bandBase.some((v, i) => v != null && bandSpan[i] != null)

  const series = []

  if (hasBand) {
    series.push({
      name: '__refBase',
      type: 'line',
      stack: 'refBand',
      step: 'end',
      data: bandBase,
      symbol: 'none',
      lineStyle: { opacity: 0 },
      areaStyle: { opacity: 0 },
      silent: true,
      z: 0,
      tooltip: { show: false }
    })
    series.push({
      name: '__refSpan',
      type: 'line',
      stack: 'refBand',
      step: 'end',
      data: bandSpan,
      symbol: 'none',
      lineStyle: { opacity: 0 },
      areaStyle: { color: 'rgba(7,193,96,0.12)' },
      silent: true,
      z: 0,
      tooltip: { show: false }
    })
  }

  series.push({
    name,
    type: 'line',
    data: values,
    smooth: true,
    symbol: 'circle',
    symbolSize: 8,
    z: 3,
    lineStyle: { color: '#07C160', width: 2 },
    itemStyle: {
      color(p) {
        const pt = points[p.dataIndex]
        if (pt?.isAbnormal === 1) return '#FA5151'
        if (pt?.isAbnormal === 2) return '#1890FF'
        return '#07C160'
      }
    },
    label: {
      show: true,
      position: 'top',
      color: '#333',
      fontSize: 11,
      fontWeight: 500,
      formatter(p) {
        const pt = points[p.dataIndex]
        const val = p.value
        if (pt?.isAbnormal === 1) return `{high|${val}↑}`
        if (pt?.isAbnormal === 2) return `{low|${val}↓}`
        return `{normal|${val}}`
      },
      rich: {
        normal: { color: '#07C160' },
        high: { color: '#FA5151' },
        low: { color: '#1890FF' }
      }
    }
  })

  return {
    grid: { top: 30, right: 20, bottom: 35, left: 60 },
    tooltip: {
      trigger: 'axis',
      formatter(list) {
        const idx = list?.[0]?.dataIndex ?? 0
        const pt = points[idx]
        if (!pt) return ''
        const flag = pt.isAbnormal === 1 ? ' <span style="color:#FA5151">↑ 偏高</span>'
          : pt.isAbnormal === 2 ? ' <span style="color:#1890FF">↓ 偏低</span>' : ''
        return `<div style="color:#999;font-size:12px">${pt.examDate}</div>`
          + `<div style="margin-top:4px">${name}: <b>${pt.value}</b> ${unit}${flag}</div>`
          + `<div style="color:#909399;font-size:12px">参考区间: ${refLabel(pt)}</div>`
      }
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#e0e0e0' } },
      axisLabel: { color: '#999', fontSize: 10 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      name: unit,
      nameLocation: 'end',
      nameRotate: 0,
      nameGap: 12,
      nameTextStyle: { color: '#666', fontSize: 11, fontWeight: 500 },
      axisLabel: { color: '#999', fontSize: 10 },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    series
  }
}

function initChart() {
  if (!chartRef.value || !hasData.value) return
  if (chart) chart.dispose()
  chart = echarts.init(chartRef.value)
  chart.setOption(buildOption())
}

function handleResize() {
  chart?.resize()
}

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

let unwatch = null
unwatch = watch(() => [props.points, props.indicator], () => {
  initChart()
}, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  if (unwatch) unwatch()
})
</script>

<style scoped>
.chart-wrap {
  background: var(--bg-white);
  border-radius: var(--radius-md);
  padding: 8px;
  margin-bottom: 10px;
}
.chart { width: 100%; height: 280px; }
.empty {
  padding: 40px 0;
  text-align: center;
  color: var(--text-hint);
  font-size: var(--font-sm);
}
</style>
