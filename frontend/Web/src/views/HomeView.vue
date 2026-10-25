<template>
  <div>
    <PageHeader title="我的记录" :subtitle="subtitle">
      <template #actions>
        <el-button type="primary" @click="$router.push('/records/new')">添加检查记录</el-button>
      </template>
    </PageHeader>

    <el-row :gutter="16" class="stats">
      <el-col :xs="12" :sm="12" :md="6">
        <StatCard label="检查记录" :value="total" unit="次" hint="按当前筛选统计" />
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <StatCard label="覆盖指标" :value="indicatorCount" unit="项" hint="有数值记录的指标" />
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <StatCard label="关注指标" :value="focusCount" unit="项" hint="录入页默认展开" />
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <StatCard
          label="最近一次异常"
          :value="latestAbnormal"
          unit="项"
          :hint="latest ? latest.examDate?.slice(0, 10) + ' · ' + latest.categoryName : '还没有记录'"
          :tone="latestAbnormal > 0 ? 'red' : 'green'"
        />
      </el-col>
    </el-row>

    <el-card>
      <div class="filter-bar">
        <el-select v-model="categoryId" clearable placeholder="全部分类" @change="fetchRecords" class="filter-item">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-date-picker
          v-model="startDate" type="date" value-format="YYYY-MM-DD"
          placeholder="开始日期" @change="fetchRecords" class="filter-item"
        />
        <el-date-picker
          v-model="endDate" type="date" value-format="YYYY-MM-DD"
          placeholder="结束日期" @change="fetchRecords" class="filter-item"
        />
        <el-button text @click="resetFilter" v-if="hasFilter">清空筛选</el-button>
      </div>

      <el-table
        v-if="records.length || loading"
        v-loading="loading"
        :data="records"
        stripe
        @row-click="goRecord"
      >
        <el-table-column label="日期" width="150">
          <template #default="{ row }">
            <span class="num">{{ row.examDate?.replace('T', ' ') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="类型" width="110" />
        <el-table-column prop="hospital" label="医院" min-width="160">
          <template #default="{ row }">{{ row.hospital || '—' }}</template>
        </el-table-column>
        <el-table-column label="指标数" width="90" align="right">
          <template #default="{ row }">{{ row.totalIndicators ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="异常" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.abnormalCount" type="danger" size="small">{{ row.abnormalCount }} 项</el-tag>
            <span v-else class="ok">正常</span>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState
        v-if="!loading && !records.length"
        icon="file"
        :title="hasFilter ? '这个筛选下没有记录' : '还没有检查记录'"
        :description="hasFilter ? '换个分类或日期范围试试' : '录一条过去做过的检查，趋势就有起点了'"
      >
        <el-button v-if="!hasFilter" type="primary" @click="$router.push('/records/new')">添加第一条记录</el-button>
      </EmptyState>

      <div class="pagination-wrap" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchRecords"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { recordApi } from '@/api/record'
import { categoryApi } from '@/api/category'
import { trendApi } from '@/api/trend'
import { focusApi } from '@/api/focus'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const records = ref([])
const categories = ref([])
const loading = ref(true)
const categoryId = ref(null)
const startDate = ref('')
const endDate = ref('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const indicatorCount = ref('-')
const focusCount = ref('-')

const hasFilter = computed(() => !!(categoryId.value || startDate.value || endDate.value))
const latest = computed(() => records.value[0] || null)
const latestAbnormal = computed(() => latest.value?.abnormalCount ?? 0)
const subtitle = computed(() => {
  if (!latest.value) return '还没有记录'
  return '共 ' + total.value + ' 条 · 最近一次 ' + latest.value.examDate?.slice(0, 10)
})

async function fetchRecords() {
  loading.value = true
  try {
    const res = await recordApi.list({
      categoryId: categoryId.value || undefined,
      startDate: startDate.value || undefined,
      endDate: endDate.value || undefined,
      page: page.value,
      pageSize: pageSize.value
    })
    records.value = res.data.rows
    total.value = res.data.total
  } catch (e) { console.error(e) }
  loading.value = false
}

function resetFilter() {
  categoryId.value = null
  startDate.value = ''
  endDate.value = ''
  page.value = 1
  fetchRecords()
}

function goRecord(row) { router.push('/records/' + row.id) }

async function loadSummary() {
  try {
    const [t, f] = await Promise.all([trendApi.getTrendable(), focusApi.list()])
    indicatorCount.value = (t.data || []).length
    focusCount.value = (f.data || []).length
  } catch (e) { console.error(e) }
}

onMounted(async () => {
  try {
    const catRes = await categoryApi.list()
    categories.value = catRes.data
  } catch (e) { console.error(e) }
  fetchRecords()
  loadSummary()
})
</script>

<style scoped>
.stats { margin-bottom: 4px; }
.stats :deep(.el-col) { margin-bottom: 16px; }

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
}
.filter-item { width: 180px; }

.ok { color: var(--text-hint); font-size: var(--font-sm); }
.pagination-wrap { display: flex; justify-content: center; margin-top: 18px; }

:deep(.el-table__row) { cursor: pointer; }
</style>
