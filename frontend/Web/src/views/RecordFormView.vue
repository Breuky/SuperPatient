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
        <el-input v-model="form.notes" type="textarea" :rows="2" placeholder="如：空腹、近期用药等" />
      </el-form-item>
    </el-card>

    <!-- Indicators -->
    <el-card v-if="activeIndicators.length" style="margin-top:16px">
      <template #header>
        <div class="ind-header">
          <span>检查指标</span>
          <span class="ind-hint">点亮 ★ 长期关注，下次默认展开</span>
        </div>
      </template>

      <template v-if="focusedIndicators.length">
        <div class="group-title">我关注的</div>
        <div v-for="ind in focusedIndicators" :key="ind.id" class="ind-row">
          <span class="star on" title="取消关注" @click="toggleFocus(ind.id)">★</span>
          <IndicatorInput
            class="grow"
            :indicator="ind"
            :model-value="valueMap[ind.id]"
            @update:model-value="v => valueMap[ind.id] = v"
          />
        </div>
      </template>

      <div v-if="otherIndicators.length" class="group-title clickable" @click="showOthers = !showOthers">
        其他指标 ({{ otherIndicators.length }}) {{ showOthers ? '收起 ▲' : '展开 ▼' }}
      </div>

      <template v-if="showOthers">
        <div v-for="ind in otherIndicators" :key="ind.id" class="ind-row">
          <span class="star" title="加入关注" @click="toggleFocus(ind.id)">☆</span>
          <IndicatorInput
            class="grow"
            :indicator="ind"
            :model-value="valueMap[ind.id]"
            @update:model-value="v => valueMap[ind.id] = v"
          />
        </div>
      </template>
    </el-card>

    <!-- Upload -->
    <el-card v-loading="uploading" element-loading-text="上传中..." style="margin-top:16px">
      <template #header>
        <div style="display:flex;align-items:center;justify-content:space-between">
          <span>报告照片 (可选)</span>
          <el-button
            v-if="uploadedUrl"
            type="primary"
            plain
            size="small"
            :loading="parsing"
            :disabled="uploading"
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
import { focusApi } from '@/api/focus'
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
const uploading = ref(false)
const focusIds = ref(new Set())
const showOthers = ref(false)

const focusedIndicators = computed(() => activeIndicators.value.filter(i => focusIds.value.has(i.id)))
const otherIndicators = computed(() => activeIndicators.value.filter(i => !focusIds.value.has(i.id)))
const reviewVisible = ref(false)
const extractResult = ref(null)

const activeIndicators = computed(() => {
  if (!form.categoryId) return []
  const cat = categories.value.find(c => c.id === form.categoryId)
  return cat?.indicators || []
})

async function onFileChange(file) {
  uploading.value = true
  try {
    const res = await fileApi.upload(file.raw)
    uploadedUrl.value = res.data.url
    ElMessage.success('上传成功，可以点右上角「AI 识别化验单」')
  } catch (err) {
    uploadedUrl.value = ''
    ElMessage.error('上传失败: ' + err.message)
  } finally {
    uploading.value = false
  }
}

async function handleParse() {
  if (!form.categoryId) return alert('请先选择检查类型')
  if (!uploadedUrl.value) return alert('请先上传报告照片')

  parsing.value = true
  try {
    const res = await parseApi.report(uploadedUrl.value, form.categoryId)
    const data = res.data || {}
    const n = data.candidates?.length || 0
    const m = data.unmatched?.length || 0
    if (!n && !m) {
      ElMessage.warning('没有识别到指标，请检查图片是否清晰')
      return
    }
    // 先落数据再开窗，避免弹窗首次渲染读不到 result
    extractResult.value = data
    reviewVisible.value = true
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

async function loadFocus() {
  try {
    const res = await focusApi.list()
    focusIds.value = new Set((res.data || []).map(i => i.id))
  } catch (e) { console.error(e) }
}

async function toggleFocus(indicatorId) {
  const has = focusIds.value.has(indicatorId)
  try {
    await (has ? focusApi.remove(indicatorId) : focusApi.add(indicatorId))
    const next = new Set(focusIds.value)
    if (has) next.delete(indicatorId)
    else next.add(indicatorId)
    focusIds.value = next
    if (!has) showOthers.value = false
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
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
  loadFocus()
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

<style scoped>
.ind-header { display: flex; align-items: center; justify-content: space-between; }
.ind-hint { font-size: 12px; color: #909399; font-weight: 400; }
.group-title { font-size: 13px; color: #606266; font-weight: 600; margin: 8px 0 4px; }
.group-title.clickable { cursor: pointer; color: #909399; font-weight: 400; }
.ind-row { display: flex; align-items: center; gap: 6px; }
.grow { flex: 1; min-width: 0; }
.star { cursor: pointer; font-size: 16px; color: #c0c4cc; width: 20px; text-align: center; user-select: none; }
.star.on { color: #f5a623; }
</style>
