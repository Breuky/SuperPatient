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
        <el-table-column label="名称" min-width="160">
          <template #default="{ row }">
            {{ row.name }}
            <el-tag v-if="row.status === 0" type="info" size="small" style="margin-left:8px">已禁用</el-tag>
            <div v-if="row.aliases" class="alias-hint">别名：{{ row.aliases }}</div>
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
        <el-table-column label="操作" width="300">
          <template #default="{ row }">
            <el-button size="small" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" @click="openRules(row)" :disabled="row.valueType !== 'number'">区间规则</el-button>
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
        <el-form-item label="别名">
          <el-input v-model="form.aliases" placeholder="报告上的其他写法，逗号分隔，如：HGB,Hb,HB" />
          <div class="field-tip">抽取时按别名把报告原文归到本指标，多个用逗号分隔</div>
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

    <el-dialog v-model="showRules" :title="'参考区间规则 · ' + (rulesIndicator?.name || '')" width="760px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="优先级：报告照片上的印刷区间 > 这里的规则 > 指标默认值。只有报告没写区间时才会用到规则。"
      />

      <el-table :data="rules" size="small" border style="margin-top:12px" empty-text="还没有规则，命中时将使用指标默认区间">
        <el-table-column label="性别" width="70" align="center">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column label="年龄" width="110" align="center">
          <template #default="{ row }">{{ ageText(row) }}</template>
        </el-table-column>
        <el-table-column label="参考区间" width="150" align="center">
          <template #default="{ row }">{{ row.refMin ?? '-' }} ~ {{ row.refMax ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="source" label="来源" min-width="160" />
        <el-table-column prop="priority" label="优先级" width="80" align="center" />
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="removeRule(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-divider content-position="left">新增规则</el-divider>
      <el-form label-position="top">
        <el-row :gutter="12">
          <el-col :span="6">
            <el-form-item label="性别">
              <el-select v-model="ruleForm.gender" style="width:100%">
                <el-option label="不限" :value="null" />
                <el-option label="女" :value="0" />
                <el-option label="男" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="年龄下限">
              <el-input-number v-model="ruleForm.ageMin" :min="0" :max="150" style="width:100%" placeholder="不限" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="年龄上限">
              <el-input-number v-model="ruleForm.ageMax" :min="0" :max="150" style="width:100%" placeholder="不限" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="优先级">
              <el-input-number v-model="ruleForm.priority" :min="0" :max="100" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="参考下限">
              <el-input-number v-model="ruleForm.refMin" :step="0.1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="参考上限">
              <el-input-number v-model="ruleForm.refMax" :step="0.1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源说明">
              <el-input v-model="ruleForm.source" placeholder="如：成人男性参考区间" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="showRules = false">关闭</el-button>
        <el-button type="success" @click="addRule">添加规则</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi } from '@/api/category'
import { indicatorApi } from '@/api/indicator'
import { referenceRangeApi } from '@/api/referenceRange'

const categories = ref([])
const indicators = ref([])
const loading = ref(true)
const activeCatId = ref(null)
const showForm = ref(false)
const editing = ref(null)

const showRules = ref(false)
const rulesIndicator = ref(null)
const rules = ref([])
const ruleForm = reactive({
  gender: null, ageMin: null, ageMax: null,
  refMin: null, refMax: null, source: '', priority: 0
})

const form = reactive({
  categoryId: null, name: '', aliases: '', unit: '', valueType: 'number',
  normalMin: null, normalMax: null, sortOrder: 0
})

const filteredIndicators = computed(() => {
  if (!activeCatId.value) return indicators.value
  return indicators.value.filter(i => i.categoryId === activeCatId.value)
})

function resetForm() {
  editing.value = null
  form.categoryId = categories.value[0]?.id || null
  form.name = ''; form.aliases = ''; form.unit = ''; form.valueType = 'number'
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
  form.name = ind.name; form.aliases = ind.aliases || ''; form.unit = ind.unit || ''
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

function genderText(gender) {
  if (gender === 0) return '女'
  if (gender === 1) return '男'
  return '不限'
}

function ageText(rule) {
  const { ageMin, ageMax } = rule
  if (ageMin == null && ageMax == null) return '不限'
  if (ageMin == null) return `≤ ${ageMax} 岁`
  if (ageMax == null) return `≥ ${ageMin} 岁`
  return `${ageMin}-${ageMax} 岁`
}

async function openRules(ind) {
  rulesIndicator.value = ind
  showRules.value = true
  ruleForm.gender = null
  ruleForm.ageMin = null
  ruleForm.ageMax = null
  ruleForm.refMin = ind.normalMin ?? null
  ruleForm.refMax = ind.normalMax ?? null
  ruleForm.source = ''
  ruleForm.priority = 0
  await loadRules()
}

async function loadRules() {
  if (!rulesIndicator.value) return
  try {
    const res = await referenceRangeApi.list(rulesIndicator.value.id)
    rules.value = res.data || []
  } catch (e) { ElMessage.error(e.message) }
}

async function addRule() {
  if (ruleForm.refMin == null && ruleForm.refMax == null) {
    return ElMessage.warning('至少填写参考下限或上限')
  }
  try {
    await referenceRangeApi.create({
      indicatorId: rulesIndicator.value.id,
      gender: ruleForm.gender,
      ageMin: ruleForm.ageMin,
      ageMax: ruleForm.ageMax,
      refMin: ruleForm.refMin,
      refMax: ruleForm.refMax,
      refText: `${ruleForm.refMin ?? ''}-${ruleForm.refMax ?? ''}`,
      source: ruleForm.source || '自定义规则',
      priority: ruleForm.priority ?? 0
    })
    ElMessage.success('已添加')
    await loadRules()
  } catch (e) { ElMessage.error(e.message) }
}

async function removeRule(rule) {
  try {
    await ElMessageBox.confirm('确定删除这条参考区间规则？', '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    await referenceRangeApi.remove(rule.id)
    await loadRules()
  } catch (e) { if (e !== 'cancel') ElMessage.error(e.message) }
}

onMounted(fetchData)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.alias-hint { font-size: 12px; color: #909399; margin-top: 2px; }
.field-tip { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
