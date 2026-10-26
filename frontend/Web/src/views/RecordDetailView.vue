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

        <AgentChat v-if="record" :record-id="record.id" style="margin-bottom:16px" />

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
import AgentChat from '@/components/AgentChat.vue'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

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
    restoreChat()
  } catch (e) { console.error(e) }
  loading.value = false
})
</script>

<style scoped>
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
