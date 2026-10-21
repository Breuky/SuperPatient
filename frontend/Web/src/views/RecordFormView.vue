<template>
  <div>
    <div style="margin-bottom:16px">
      <el-button text @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <span style="font-size:18px;font-weight:600;margin-left:8px">
        {{ isEdit ? '编辑检查' : '新增检查' }}
      </span>
    </div>

    <el-card v-loading="loading">
      <!-- Category selector -->
      <el-form-item label="检查类型" required>
        <el-radio-group v-model="form.categoryId">
          <el-radio-button
            v-for="c in categories" :key="c.id"
            :value="c.id"
          >{{ c.name }}</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- Basic info -->
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="检查日期" required>
            <el-date-picker
              v-model="form.examDate"
              type="datetime"
              placeholder="选择日期"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm"
              style="width:100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="检查医院">
            <el-input v-model="form.hospital" placeholder="如：市第一人民医院" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="主治医生">
            <el-input v-model="form.doctor" placeholder="医生姓名" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="备注">
        <el-input v-model="form.notes" type="textarea" :rows="2" placeholder="随访周期、特殊情况等" />
      </el-form-item>
    </el-card>

    <!-- Indicators -->
    <el-card v-if="activeIndicators.length" header="检查指标" style="margin-top:16px">
      <IndicatorInput
        v-for="ind in activeIndicators" :key="ind.id"
        :indicator="ind"
        :model-value="valueMap[ind.id]"
        @update:model-value="v => valueMap[ind.id] = v"
      />
    </el-card>

    <!-- Upload -->
    <el-card style="margin-top:16px">
      <template #header>
        <div style="display:flex;align-items:center;justify-content:space-between">
          <span>报告照片 (可选)</span>
          <el-button
            v-if="uploadedUrl"
            type="primary"
            plain
            size="small"
            :loading="parsing"
            @click="handleParse"
          >
            <el-icon><MagicStick /></el-icon>
            {{ parsing ? '识别中...' : 'AI 识别化验单' }}
          </el-button>
        </div>
      </template>
      <el-upload
        :auto-upload="false"
        :limit="1"
        :on-change="onFileChange"
        :file-list="fileList"
        list-type="picture"
        accept="image/jpeg,image/png"
      >
        <el-button type="primary">
          <el-icon><Upload /></el-icon> 点击上传
        </el-button>
        <template #tip>
          <div class="el-upload__tip">支持 jpg/png 格式</div>
        </template>
      </el-upload>
    </el-card>

    <div style="margin-top:16px;display:flex;gap:12px">
      <el-button type="success" size="large" @click="handleSave">保存</el-button>
      <el-button size="large" @click="$router.back()">取消</el-button>
    </div>

    <ExtractReviewDialog
      v-model:visible="reviewVisible"
      :result="extractResult"
      :indicators="activeIndicators"
      @apply="applyExtraction"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { categoryApi } from '@/api/category'
import { recordApi } from '@/api/record'
import { fileApi } from '@/api/file'
import { parseApi } from '@/api/parse'
import { ElMessage } from 'element-plus'
import IndicatorInput from '@/components/IndicatorInput.vue'
import ExtractReviewDialog from '@/components/ExtractReviewDialog.vue'
import { now } from '@/utils/date'
import { fileUrl } from '@/utils/file'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => route.name === 'RecordEdit')
const loading = ref(true)
const categories = ref([])

const form = reactive({
  categoryId: null,
  examDate: now(),
  hospital: '',
  doctor: '',
  notes: '',
  imageUrl: ''
})

const valueMap = reactive({})
const uploadedUrl = ref('')
const fileList = ref([])
const parsing = ref(false)
const reviewVisible = ref(false)
const extractResult = ref(null)

const activeIndicators = computed(() => {
  if (!form.categoryId) return []
  const cat = categories.value.find(c => c.id === form.categoryId)
  return cat?.indicators || []
})

async function onFileChange(file) {
  try {
    const res = await fileApi.upload(file.raw)
    uploadedUrl.value = res.data.url
  } catch (err) { alert('上传失败: ' + err.message) }
}

async function handleParse() {
  if (!form.categoryId) return alert('请先选择检查类型')
  if (!uploadedUrl.value) return alert('请先上传报告照片')

  parsing.value = true
  try {
    const res = await parseApi.report(uploadedUrl.value, form.categoryId)
    extractResult.value = res.data
    reviewVisible.value = true
    const n = res.data.candidates?.length || 0
    const m = res.data.unmatched?.length || 0
    if (!n && !m) ElMessage.warning('没有识别到指标，请检查图片是否清晰')
  } catch (e) {
    ElMessage.error('识别失败: ' + e.message)
  } finally {
    parsing.value = false
  }
}

function applyExtraction(items) {
  if (!items.length) return ElMessage.warning('没有选择可入库的项')

  for (const item of items) {
    valueMap[item.indicatorId] = { ...item }
  }

  const r = extractResult.value || {}
  // 新增时以报告上的日期为准；编辑时不动用户已有的日期
  if (r.examDate && !isEdit.value) form.examDate = toDateTime(r.examDate)
  if (r.hospital && !form.hospital) form.hospital = r.hospital

  ElMessage.success(`已填入 ${items.length} 项，请核对后保存`)
}

function toDateTime(date) {
  return /^\d{4}-\d{2}-\d{2}$/.test(date) ? `${date}T00:00` : date
}

async function handleSave() {
  if (!form.categoryId) return alert('请选择检查类型')
  try {
    const values = Object.values(valueMap).filter(v => v && (v.valueDecimal != null || v.valueText))
    const data = {
      ...form,
      imageUrl: uploadedUrl.value || null,
      values
    }

    if (isEdit.value) {
      await recordApi.update(route.params.id, { ...data, categoryId: undefined })
    } else {
      await recordApi.create(data)
    }
    router.back()
  } catch (e) { alert('保存失败: ' + e.message) }
}

onMounted(async () => {
  try {
    const res = await categoryApi.list()
    categories.value = res.data
    if (res.data.length) form.categoryId = res.data[0].id

    if (isEdit.value) {
      const recordRes = await recordApi.detail(route.params.id)
      const r = recordRes.data
      form.categoryId = r.categoryId
      form.examDate = r.examDate
      form.hospital = r.hospital || ''
      form.doctor = r.doctor || ''
      form.notes = r.notes || ''
      if (r.imageUrl) {
        uploadedUrl.value = r.imageUrl
        fileList.value = [{ name: '报告照片', url: fileUrl(r.imageUrl) }]
      }
      if (r.values) {
        for (const v of r.values) {
          valueMap[v.indicatorId] = {
            indicatorId: v.indicatorId,
            valueDecimal: v.valueDecimal,
            valueText: v.valueText,
            // 保留报告级参考区间，避免编辑一次就把快照丢了
            refMin: v.refMin,
            refMax: v.refMax,
            refText: v.refText,
            rawName: v.rawName,
            rawUnit: v.rawUnit
          }
        }
      }
    }
  } catch (e) { console.error(e) }
  loading.value = false
})
</script>
