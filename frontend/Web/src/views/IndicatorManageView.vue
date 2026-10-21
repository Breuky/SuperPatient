<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>指标管理</span>
          <el-button type="success" @click="openAdd">新增指标</el-button>
        </div>
      </template>

      <el-tabs v-model="activeCatId" @tab-change="onTabChange">
        <el-tab-pane label="全部" :name="null" />
        <el-tab-pane
          v-for="c in categories" :key="c.id"
          :label="c.name" :name="c.id"
        />
      </el-tabs>

      <el-table v-loading="loading" :data="filteredIndicators" stripe>
        <el-table-column prop="name" label="名称">
          <template #default="{ row }">
            {{ row.name }}
            <el-tag v-if="row.status === 0" type="info" size="small" style="margin-left:8px">已禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ row.valueType === 'number' ? '数值型' : '文本型' }}</template>
        </el-table-column>
        <el-table-column label="单位" width="100">
          <template #default="{ row }">{{ row.unit || '-' }}</template>
        </el-table-column>
        <el-table-column label="正常范围" width="160">
          <template #default="{ row }">
            <span v-if="row.valueType === 'number' && (row.normalMin != null || row.normalMax != null)">
              {{ row.normalMin ?? '-' }} ~ {{ row.normalMax ?? '-' }}
            </span>
            <span v-else class="text-hint">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" @click="editInd(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteInd(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="showForm"
      :title="editing ? '编辑指标' : '新增指标'"
      width="500px"
      @close="resetForm"
    >
      <el-form label-position="top">
        <el-form-item label="所属分类" required>
          <el-select v-model="form.categoryId" style="width:100%" placeholder="选择分类">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="指标名称" required>
          <el-input v-model="form.name" placeholder="如：血红蛋白" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" placeholder="如：g/L" />
        </el-form-item>
        <el-form-item label="值类型">
          <el-select v-model="form.valueType" style="width:100%">
            <el-option label="数值型 (number)" value="number" />
            <el-option label="文本型 (text)" value="text" />
          </el-select>
        </el-form-item>
        <template v-if="form.valueType === 'number'">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="正常下限">
                <el-input-number v-model="form.normalMin" :step="0.1" style="width:100%" placeholder="如：120" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="正常上限">
                <el-input-number v-model="form.normalMax" :step="0.1" style="width:100%" placeholder="如：160" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="success" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { categoryApi } from '@/api/category'
import { indicatorApi } from '@/api/indicator'

const categories = ref([])
const indicators = ref([])
const loading = ref(true)
const activeCatId = ref(null)
const showForm = ref(false)
const editing = ref(null)

const form = reactive({
  categoryId: null, name: '', unit: '', valueType: 'number',
  normalMin: null, normalMax: null, sortOrder: 0
})

const filteredIndicators = computed(() => {
  if (!activeCatId.value) return indicators.value
  return indicators.value.filter(i => i.categoryId === activeCatId.value)
})

function resetForm() {
  editing.value = null
  form.categoryId = categories.value[0]?.id || null
  form.name = ''; form.unit = ''; form.valueType = 'number'
  form.normalMin = null; form.normalMax = null; form.sortOrder = 0
}

function onTabChange() {}

async function fetchData() {
  loading.value = true
  try {
    const [catRes, indRes] = await Promise.all([
      categoryApi.list(),
      indicatorApi.list()
    ])
    categories.value = catRes.data
    indicators.value = indRes.data
  } catch (e) { console.error(e) }
  loading.value = false
}

function openAdd() { resetForm(); showForm.value = true }

function editInd(ind) {
  editing.value = ind
  form.categoryId = ind.categoryId
  form.name = ind.name; form.unit = ind.unit || ''
  form.valueType = ind.valueType || 'number'
  form.normalMin = ind.normalMin; form.normalMax = ind.normalMax
  form.sortOrder = ind.sortOrder || 0
  showForm.value = true
}

async function handleSave() {
  if (!form.name || !form.categoryId) return
  try {
    const data = { ...form }
    if (data.valueType === 'text') { data.normalMin = null; data.normalMax = null }
    if (editing.value) {
      await indicatorApi.update(editing.value.id, data)
    } else {
      await indicatorApi.create(data)
    }
    showForm.value = false
    await fetchData()
  } catch (e) { alert(e.message) }
}

async function toggleStatus(ind) {
  const newStatus = ind.status === 1 ? 0 : 1
  try {
    await indicatorApi.updateStatus(ind.id, newStatus)
    await fetchData()
  } catch (e) { alert(e.message) }
}

async function deleteInd(ind) {
  try {
    await ElMessageBox.confirm(`确定删除指标"${ind.name}"？`, '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    await indicatorApi.remove(ind.id)
    await fetchData()
  } catch (e) { if (e !== 'cancel') alert(e.message) }
}

onMounted(fetchData)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
