<template>
  <div>
    <PageHeader
      title="Agent 调用记录"
      subtitle="每次解读的问题、轮数、工具调用与耗时都留在这里，用来判断「答得好不好」和「为什么慢」"
    >
      <template #actions>
        <el-button :loading="loading" @click="fetchLogs">刷新</el-button>
      </template>
    </PageHeader>

    <el-row :gutter="16" class="stats">
      <el-col :xs="12" :md="6">
        <StatCard label="调用次数" :value="logs.length" unit="次" hint="最近 50 条" />
      </el-col>
      <el-col :xs="12" :md="6">
        <StatCard label="平均轮数" :value="avgTurns" unit="轮" hint="越多说明路径越长" />
      </el-col>
      <el-col :xs="12" :md="6">
        <StatCard label="平均工具调用" :value="avgTools" unit="次" hint="0 = 没取数就作答" />
      </el-col>
      <el-col :xs="12" :md="6">
        <StatCard
          label="降级次数" :value="degradedCount" unit="次"
          :tone="degradedCount ? 'red' : 'green'"
          hint="模型不可用时回落到规则摘要"
        />
      </el-col>
    </el-row>

    <el-card>
      <el-table v-if="logs.length || loading" v-loading="loading" :data="logs" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="trace">
              <div v-if="parsedTrace(row).length" class="trace-list">
                <div v-for="(t, i) in parsedTrace(row)" :key="i" class="trace-item">
                  <span class="trace-step">{{ i + 1 }}</span>
                  <span class="trace-tool">{{ t.tool }}</span>
                  <span class="trace-args">{{ formatArgs(t.args) }}</span>
                  <span class="trace-ms">{{ t.ms }}ms</span>
                  <el-tag v-if="t.ok === false" type="danger" size="small">失败</el-tag>
                </div>
              </div>
              <div v-else class="trace-none">这次调用没有使用工具（证据包模式或降级）</div>
              <div class="trace-summary">{{ row.summary }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">
            <span class="num">{{ (row.createdAt || '').replace('T', ' ').slice(0, 16) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="模式" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.mode === 'agent' ? 'primary' : 'info'">
              {{ modeText(row.mode) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="question" label="问题" min-width="220">
          <template #default="{ row }">{{ row.question || '（页面直接生成，无提问）' }}</template>
        </el-table-column>
        <el-table-column label="轮数" width="80" align="right">
          <template #default="{ row }">{{ row.turns ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="工具" width="80" align="right">
          <template #default="{ row }">{{ row.toolCalls ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="token" width="90" align="right">
          <template #default="{ row }">{{ row.tokens || 0 }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="90" align="right">
          <template #default="{ row }">{{ ((row.elapsedMs || 0) / 1000).toFixed(1) }}s</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.degraded" type="warning" size="small">降级</el-tag>
            <span v-else class="ok">正常</span>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState
        v-if="!loading && !logs.length"
        title="还没有调用记录"
        description="去任意一条检查记录里点「AI 解读」，这里就会出现轨迹"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { insightApi } from '@/api/insight'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const logs = ref([])
const loading = ref(true)

const avgTurns = computed(() => avg('turns'))
const avgTools = computed(() => avg('toolCalls'))
const degradedCount = computed(() => logs.value.filter(l => l.degraded).length)

function avg(field) {
  if (!logs.value.length) return '-'
  const sum = logs.value.reduce((acc, l) => acc + (l[field] || 0), 0)
  return (sum / logs.value.length).toFixed(1)
}

function modeText(mode) {
  if (mode === 'agent') return '深读'
  if (mode === 'simple') return '快速'
  return '规则'
}

function parsedTrace(row) {
  if (!row.traceJson) return []
  try {
    return JSON.parse(row.traceJson) || []
  } catch (e) {
    return []
  }
}

function formatArgs(args) {
  if (!args) return ''
  return Object.entries(args).map(([k, v]) => k + '=' + v).join(' ')
}

async function fetchLogs() {
  loading.value = true
  try {
    const res = await insightApi.logs(50)
    logs.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(fetchLogs)
</script>

<style scoped>
.stats { margin-bottom: 4px; }
.stats :deep(.el-col) { margin-bottom: 16px; }
.ok { color: var(--text-hint); font-size: var(--font-sm); }

.trace { padding: 8px 16px 12px 56px; }
.trace-list { display: flex; flex-direction: column; gap: 6px; }
.trace-item { display: flex; align-items: center; gap: 10px; font-size: var(--font-sm); }
.trace-step {
  width: 20px; height: 20px; border-radius: 50%;
  display: grid; place-items: center;
  background: var(--green-light); color: var(--green-dark);
  font-size: 11px; font-weight: 600;
}
.trace-tool { font-family: ui-monospace, monospace; color: var(--text-primary); }
.trace-args { color: var(--text-hint); font-family: ui-monospace, monospace; }
.trace-ms { color: var(--text-hint); font-size: var(--font-xs); }
.trace-none { color: var(--text-hint); font-size: var(--font-sm); }
.trace-summary {
  margin-top: 10px; padding-top: 10px;
  border-top: 1px dashed var(--border-light);
  color: var(--text-secondary); font-size: var(--font-sm);
}
</style>
