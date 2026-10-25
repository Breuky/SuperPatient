<template>
  <div>
    <PageHeader
      title="检查详情"
      :subtitle="record ? fmtDate(record.examDate) + ' · ' + (record.categoryName || '') : ''"
      back back-text="返回记录列表" back-to="/records"
    />

    <el-card v-loading="loading">
      <template v-if="record">
        <el-descriptions :column="2" border title="检查信息" style="margin-bottom:20px">
          <el-descriptions-item label="检查日期">{{ fmtDate(record.examDate) }}</el-descriptions-item>
          <el-descriptions-item label="检查类型">{{ record.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="检查医院">{{ record.hospital || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="主治医生">{{ record.doctor || '未填写' }}</el-descriptions-item>
          <el-descriptions-item v-if="record.notes" label="备注" :span="2">{{ record.notes }}</el-descriptions-item>
        </el-descriptions>

        <el-card style="margin-bottom:16px" class="insight-card">
          <template #header>
            <div class="ins-head">
              <span>AI 解读</span>
              <div class="ins-actions">
                <el-radio-group v-model="insightMode" size="small" :disabled="insightLoading">
                  <el-radio-button value="simple">快速</el-radio-button>
                  <el-radio-button value="agent">深读</el-radio-button>
                </el-radio-group>
                <span class="mode-hint">
                  {{ insightMode === 'agent' ? '模型自己决定查哪些数据' : '一次性喂全量数据，便宜但容易答非所问' }}
                </span>
                <el-button size="small" type="primary" :loading="insightLoading" @click="loadInsight">
                  {{ insight ? '重新生成' : '生成解读' }}
                </el-button>
              </div>
            </div>
          </template>

          <div v-if="insight">
            <el-alert
              v-if="insight.degraded"
              type="warning" :closable="false" show-icon style="margin-bottom:12px"
              :title="'已降级为规则摘要：' + (insight.warnings[0] || '模型不可用')"
            />
            <p class="ins-summary">{{ insight.summary }}</p>
            <div v-for="(h, i) in insight.highlights" :key="i" class="ins-item">
              <span class="ins-name">{{ h.indicator }}</span>
              <span class="ins-text">{{ h.text }}</span>
              <span v-if="h.basis" class="ins-basis" :title="h.basis">依据</span>
            </div>
            <div v-if="insight.trace && insight.trace.length" class="ins-trace">
              <span class="ins-trace-label">
                {{ insight.turns }} 轮 · 调用工具 {{ insight.trace.length }} 次
              </span>
              <el-tag v-for="(t, i) in insight.trace" :key="i" size="small" class="ins-tool">
                {{ t.tool }}{{ t.ok ? '' : '(失败)' }}
              </el-tag>
            </div>
            <p class="ins-disclaimer">{{ insight.disclaimer }}</p>
          </div>

          <div v-else class="ins-empty">
            让模型读一遍这次结果。深读模式会自己决定查哪些数据（本次结果、历史趋势、区间规则）。
          </div>
        </el-card>

        <el-card style="margin-bottom:16px">
          <template #header>
            <span>检查结果</span>
          </template>

          <div class="table-hint">
            参考区间优先级：报告标注 &gt; 按性别年龄规则 &gt; 指标默认值
          </div>

          <el-table :data="record.values" stripe size="small">
            <el-table-column label="指标" min-width="170">
              <template #default="{ row }">
                <div>{{ row.indicatorName }}</div>
                <div v-if="hasRawInfo(row)" class="sub">
                  报告原文：{{ row.rawName || row.indicatorName }}
                  <span v-if="row.rawUnit"> · {{ row.rawUnit }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="结果" width="170">
              <template #default="{ row }">
                <span :class="valueClass(row.isAbnormal)">
                  {{ row.valueText || row.valueDecimal }}
                  <el-tag
                    v-if="row.isAbnormal !== 0"
                    :type="row.isAbnormal === 1 ? 'danger' : 'primary'"
                    size="small"
                    style="margin-left:6px"
                  >
                    {{ row.isAbnormal === 1 ? '↑ 偏高' : '↓ 偏低' }}
                  </el-tag>
                </span>
              </template>
            </el-table-column>

            <el-table-column label="参考区间" width="200">
              <template #default="{ row }">
                <div v-if="row.normalMin != null || row.normalMax != null">
                  {{ row.normalMin ?? '-' }} ~ {{ row.normalMax ?? '-' }}
                </div>
                <div v-else class="text-hint">-</div>
                <el-tooltip
                  v-if="row.refText"
                  :content="'报告印刷：' + row.refText"
                  placement="top"
                >
                  <el-tag :type="sourceTagType(row.refSource)" size="small" effect="plain" style="margin-top:2px">
                    {{ sourceText(row.refSource) }}
                  </el-tag>
                </el-tooltip>
                <el-tag
                  v-else
                  :type="sourceTagType(row.refSource)"
                  size="small"
                  effect="plain"
                  style="margin-top:2px"
                >
                  {{ sourceText(row.refSource) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="单位" width="100">
              <template #default="{ row }">{{ row.unit || '-' }}</template>
            </el-table-column>
          </el-table>
        </el-card>

        <div v-if="record.imageUrl" style="margin-bottom:16px">
          <el-card header="报告照片">
            <el-image
              :src="fileUrl(record.imageUrl)"
              :preview-src-list="[fileUrl(record.imageUrl)]"
              fit="cover"
              style="max-height:400px"
            />
          </el-card>
        </div>

        <div style="display:flex;gap:12px">
          <el-button type="success" @click="goEdit">编辑记录</el-button>
          <el-button type="danger" @click="handleDelete">删除此记录</el-button>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { fileUrl } from '@/utils/file'
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { recordApi } from '@/api/record'
import { insightApi } from '@/api/insight'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const insight = ref(null)
const insightLoading = ref(false)
/** simple=证据包单次生成，agent=模型自己调工具 */
const insightMode = ref('agent')

async function loadInsight() {
  if (!record.value) return
  insightLoading.value = true
  try {
    const res = await insightApi.report(record.value.id, insightMode.value,
      '请概括这次检查里需要关注的内容')
    insight.value = res.data
  } catch (e) {
    ElMessage.error('解读失败: ' + e.message)
  } finally {
    insightLoading.value = false
  }
}

const record = ref(null)
const loading = ref(true)

function fmtDate(d) { return d ? d.replace('T', ' ') : '' }

function valueClass(isAbnormal) {
  if (isAbnormal === 1) return 'value-abnormal-high'
  if (isAbnormal === 2) return 'value-abnormal-low'
  return ''
}

/** 只有原文与规范名/单位不一致时才展示，避免噪音 */
function hasRawInfo(row) {
  if (!row.rawName && !row.rawUnit) return false
  const nameDiff = row.rawName && row.rawName !== row.indicatorName
  const unitDiff = row.rawUnit && row.rawUnit !== row.unit
  return Boolean(nameDiff || unitDiff)
}

function sourceText(source) {
  if (source === 'report') return '报告标注'
  if (source === 'rule') return '按性别年龄'
  return '默认值'
}

function sourceTagType(source) {
  if (source === 'report') return 'success'
  if (source === 'rule') return 'warning'
  return 'info'
}

function goEdit() { router.push(`/records/${route.params.id}/edit`) }

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定删除该检查记录？', '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    await recordApi.remove(route.params.id)
    router.push('/records')
  } catch (e) { if (e !== 'cancel') alert(e.message) }
}

onMounted(async () => {
  try {
    const res = await recordApi.detail(route.params.id)
    record.value = res.data
  } catch (e) { console.error(e) }
  loading.value = false
})
</script>

<style scoped>
.ins-head { display: flex; align-items: center; justify-content: space-between; }
.ins-actions { display: flex; align-items: center; gap: 10px; }
.mode-hint { font-size: var(--font-xs); font-weight: 400; color: var(--text-hint); }
.ins-summary { font-size: var(--font-md); line-height: 1.65; color: var(--text-primary); margin-bottom: 10px; }
.ins-item { display: flex; align-items: baseline; gap: 8px; padding: 6px 0; border-top: 1px solid var(--border-light); }
.ins-name { font-weight: 600; white-space: nowrap; }
.ins-text { color: var(--text-secondary); font-size: var(--font-sm); }
.ins-basis {
  font-size: var(--font-xs); color: var(--text-hint);
  border-bottom: 1px dashed var(--border-strong); cursor: help;
}
.ins-trace { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin-top: 12px; }
.ins-trace-label { font-size: var(--font-xs); color: var(--text-hint); margin-right: 4px; }
.ins-tool { font-family: ui-monospace, monospace; }
.ins-disclaimer { margin-top: 12px; font-size: var(--font-xs); color: var(--text-hint); }
.ins-empty { color: var(--text-hint); font-size: var(--font-sm); }
.table-hint {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
</style>
