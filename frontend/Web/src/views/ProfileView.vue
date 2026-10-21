<template>
  <div>
    <el-card v-loading="loading">
      <el-descriptions :column="2" border title="我的信息">
        <el-descriptions-item label="姓名">{{ patient?.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ patient?.genderText || '-' }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ patient?.age }}岁</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ patient?.birthDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="确诊日期">{{ patient?.diagnosisDate || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ patient?.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="patient?.notes" label="备注" :span="2">{{ patient.notes }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-top:12px">
        <el-button type="success" @click="openEdit">编辑信息</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" style="margin:16px 0">
      <el-col :span="12">
        <el-card>
          <el-statistic title="检查记录" :value="stats.recordCount" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <el-statistic title="使用指标" :value="stats.indicatorCount" />
        </el-card>
      </el-col>
    </el-row>

    <el-card header="系统管理">
      <el-menu :default-active="null" class="inline-menu">
        <el-menu-item index="categories" @click="$router.push('/categories')">
          <el-icon><FolderOpened /></el-icon>
          <span>检查分类管理</span>
        </el-menu-item>
        <el-menu-item index="indicators" @click="$router.push('/indicators')">
          <el-icon><List /></el-icon>
          <span>指标模板管理</span>
        </el-menu-item>
      </el-menu>
    </el-card>

    <el-button type="danger" style="width:100%;margin-top:16px" @click="handleLogout">
      退出登录
    </el-button>

    <el-dialog v-model="showEdit" title="编辑个人信息" width="480px">
      <el-form label-position="top">
        <el-form-item label="姓名" required>
          <el-input v-model="editForm.name" placeholder="您的姓名" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="0">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker v-model="editForm.birthDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="确诊日期">
          <el-date-picker v-model="editForm.diagnosisDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" placeholder="联系电话" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.notes" type="textarea" :rows="3" placeholder="常规分型、分期等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="success" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'
import { myApi } from '@/api/my'


const router = useRouter()
const auth = useAuthStore()
const patient = ref(null)
const showEdit = ref(false)
const loading = ref(true)

const editForm = reactive({ name: '', gender: 1, birthDate: '', diagnosisDate: '', phone: '', notes: '' })
const stats = reactive({ recordCount: 0, indicatorCount: 0 })

function openEdit() {
  if (!patient.value) return
  const p = patient.value
  editForm.name = p.name || ''
  editForm.gender = p.gender ?? 1
  editForm.birthDate = p.birthDate || ''
  editForm.diagnosisDate = p.diagnosisDate || ''
  editForm.phone = p.phone || ''
  editForm.notes = p.notes || ''
  showEdit.value = true
}

async function handleSave() {
  try {
    await myApi.updateMyPatient({ ...editForm })
    showEdit.value = false
    await fetchPatient()
  } catch (e) { alert(e.message) }
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}

async function fetchPatient() {
  try {
    const res = await myApi.getMyPatient()
    patient.value = res.data
  } catch (e) { console.error(e) }
}

onMounted(async () => {
  loading.value = true
  await fetchPatient()
  try {
    const res = await authApi.getStatistics()
    Object.assign(stats, res.data)
  } catch (e) { console.error(e) }
  loading.value = false
})
</script>

<style scoped>
.inline-menu { border-right: none; }
</style>
