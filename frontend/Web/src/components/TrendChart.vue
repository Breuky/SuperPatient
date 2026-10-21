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

function buildOption() {
  const dates = props.points.map(p => p.examDate || '')
  const values = props.points.map(p => p.value)
  const min = props.indicator?.normalMin
  const max = props.indicator?.normalMax
  const unit = props.indicator?.unit || ''
  const name = props.indicator?.name || '指标'

  const markArea = (min != null && max != null) ? {
    silent: true,
    data: [[{ yAxis: min, itemStyle: { color: 'rgba(7,193,96,0.06)' } },
             { yAxis: max }]]
  } : null

  return {
    grid: { top: 30, right: 20, bottom: 35, left: 60 },
    tooltip: {
      trigger: 'axis',
      formatter(p) {
        const pt = p[0]
        return `${pt.axisValue}<br/>${name}: <b>${pt.data}</b> ${unit}`
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
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series: [{
      type: 'line',
      data: values,
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { color: '#07C160', width: 2 },
      itemStyle: {
        color(p) {
          const pt = props.points[p.dataIndex]
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
          const pt = props.points[p.dataIndex]
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
      },
      markArea,
      markLine: (max != null || min != null) ? {
        silent: true,
        symbol: 'none',
        lineStyle: { type: 'dashed', color: '#07C160', width: 1 },
        label: { fontSize: 10, color: '#07C160' },
        data: [
          ...(max != null ? [{ yAxis: max, label: { formatter: '上限 ' + max } }] : []),
          ...(min != null ? [{ yAxis: min, label: { formatter: '下限 ' + min } }] : [])
        ]
      } : null
    }]
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
