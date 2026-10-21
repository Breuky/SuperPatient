<template>
  <div>
    <div style="margin-bottom:16px">
      <el-button text @click="$router.push('/records')">
        <el-icon><ArrowLeft /></el-icon> 返回记录列表
      </el-button>
    </div>

    <el-card v-loading="loading">
      <template v-if="record">
        <el-descriptions :column="2" border title="检查信息" style="margin-bottom:20px">
          <el-descriptions-item label="检查日期">{{ fmtDate(record.examDate) }}</el-descriptions-item>
          <el-descriptions-item label="检查类型">{{ record.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="检查医院">{{ record.hospital || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="主治医生">{{ record.doctor || '未填写' }}</el-descriptions-item>
          <el-descriptions-item v-if="record.notes" label="备注" :span="2">{{ record.notes }}</el-descriptions-item>
        </el-descriptions>

        <el-card header="检查结果" style="margin-bottom:16px">
          <el-table :data="record.values" stripe size="small">
            <el-table-column prop="indicatorName" label="指标" />
            <el-table-column label="结果" width="180">
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
            <el-table-column label="正常范围" width="150">
              <template #default="{ row }">
                <span v-if="row.normalMin != null || row.normalMax != null">
                  {{ row.normalMin ?? '-' }} ~ {{ row.normalMax ?? '-' }}
                </span>
                <span v-else class="text-hint">-</span>
              </template>
            </el-table-column>
            <el-table-column label="单位" width="80">
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
