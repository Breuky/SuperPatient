<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的检查记录</span>
          <el-button type="success" @click="$router.push('/records/new')">添加检查记录</el-button>
        </div>
      </template>

      <el-row :gutter="16" style="margin-bottom:16px">
        <el-col :span="8">
          <el-form-item label="分类筛选" style="margin:0">
            <el-select v-model="categoryId" clearable placeholder="全部分类" @change="fetchRecords">
              <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="开始日期" style="margin:0">
            <el-date-picker v-model="startDate" type="date" value-format="YYYY-MM-DD" placeholder="不限" @change="fetchRecords" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="结束日期" style="margin:0">
            <el-date-picker v-model="endDate" type="date" value-format="YYYY-MM-DD" placeholder="不限" @change="fetchRecords" style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-table
        v-loading="loading"
        :data="records"
        stripe
        @row-click="goRecord"
        style="cursor:pointer"
      >
        <el-table-column label="日期" width="160">
          <template #default="{ row }">{{ row.examDate?.replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column prop="categoryName" label="类型" width="100" />
        <el-table-column prop="hospital" label="医院" min-width="140">
          <template #default="{ row }">{{ row.hospital || '-' }}</template>
        </el-table-column>
        <el-table-column label="异常" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.abnormalCount" type="danger" size="small">{{ row.abnormalCount }}项</el-tag>
            <span v-else class="text-green">正常</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !records.length" description="暂无检查记录">
        <el-button type="success" @click="$router.push('/records/new')">添加第一条记录</el-button>
      </el-empty>

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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { recordApi } from '@/api/record'
import { categoryApi } from '@/api/category'

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

function goRecord(row) { router.push(`/records/${row.id}`) }

onMounted(async () => {
  try {
    const catRes = await categoryApi.list()
    categories.value = catRes.data
  } catch (e) { console.error(e) }
  fetchRecords()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 16px; }
</style>
