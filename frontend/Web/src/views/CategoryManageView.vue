<template>
  <div>
    <PageHeader title="分类管理" subtitle="分类决定录入页和抽取候选池的分组">
      <template #actions>
        <el-button type="primary" @click="openAdd">新增分类</el-button>
      </template>
    </PageHeader>

    <el-card>
      <el-table v-loading="loading" :data="categories" stripe>
        <el-table-column prop="name" label="名称" />
        <el-table-column label="指标数" width="120">
          <template #default="{ row }">{{ row.indicators?.length || 0 }}个</template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="editCat(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteCat(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="showForm"
      :title="editingCat ? '编辑分类' : '新增分类'"
      width="450px"
      @close="resetForm"
    >
      <el-form label-position="top">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如：抽血、CT、MRI" />
        </el-form-item>
        <el-form-item label="图标标识">
          <el-input v-model="form.icon" placeholder="如：blood、ct、mri" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" placeholder="数字越小越靠前" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { categoryApi } from '@/api/category'
import PageHeader from '@/components/PageHeader.vue'

const categories = ref([])
const loading = ref(true)
const showForm = ref(false)
const editingCat = ref(null)

const form = reactive({ name: '', icon: '', sortOrder: 0 })

function resetForm() {
  editingCat.value = null
  form.name = ''; form.icon = ''; form.sortOrder = 0
}

async function fetchCategories() {
  loading.value = true
  try {
    const res = await categoryApi.list()
    categories.value = res.data
  } catch (e) { console.error(e) }
  loading.value = false
}

function openAdd() { resetForm(); showForm.value = true }

function editCat(c) {
  editingCat.value = c
  form.name = c.name; form.icon = c.icon || ''; form.sortOrder = c.sortOrder || 0
  showForm.value = true
}

async function handleSave() {
  if (!form.name) return
  try {
    if (editingCat.value) {
      await categoryApi.update(editingCat.value.id, { ...form })
    } else {
      await categoryApi.create({ ...form })
    }
    showForm.value = false
    await fetchCategories()
  } catch (e) { alert(e.message) }
}

async function deleteCat(c) {
  try {
    await ElMessageBox.confirm(`确定删除分类"${c.name}"？这将级联删除该分类下的所有指标和检查数据！`, '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    await categoryApi.remove(c.id)
    await fetchCategories()
  } catch (e) { if (e !== 'cancel') alert(e.message) }
}

onMounted(fetchCategories)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
