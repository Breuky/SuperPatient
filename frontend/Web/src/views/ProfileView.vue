<template>
  <div>
    <el-card v-loading="loading">
      <el-descriptions :column="2" border title="我的信息">
        <el-descriptions-item label="昵称">{{ profile?.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ profile?.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ profile?.genderText || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ profile?.age != null ? profile.age + ' 岁' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ profile?.birthDate || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ profile?.phone || '未填写' }}</el-descriptions-item>
        <el-descriptions-item v-if="profile?.notes" label="备注" :span="2">{{ profile.notes }}</el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="!profile?.gender || !profile?.birthDate"
        type="warning"
        :closable="false"
        show-icon
        style="margin-top:12px"
        title="补全性别和出生日期后，参考区间会按你的情况匹配，异常判定更准确"
      />

      <el-alert
        v-if="loadError"
        type="error"
        :closable="false"
        show-icon
        style="margin-top:12px"
        :title="'个人信息加载失败：' + loadError"
      />

      <div style="margin-top:12px">
        <el-button type="success" :disabled="!profile" @click="openEdit">编辑信息</el-button>
        <el-button v-if="loadError" @click="reload">重新加载</el-button>
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
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="0">女</el-radio>
          </el-radio-group>
          <div class="field-tip">参考区间按性别匹配，填错会让异常判定偏掉</div>
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker v-model="editForm.birthDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" placeholder="联系电话" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.notes" type="textarea" :rows="3" placeholder="既往病史、过敏史等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="success" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'

const router = useRouter()
const auth = useAuthStore()
const profile = ref(null)
const showEdit = ref(false)
const loading = ref(true)
const saving = ref(false)
const loadError = ref('')

const editForm = reactive({ nickname: '', gender: null, birthDate: '', phone: '', notes: '' })
const stats = reactive({ recordCount: 0, indicatorCount: 0 })

function openEdit() {
  if (!profile.value) {
    loadError.value = '个人信息还没加载出来，请点「重新加载」'
    return
  }
  const p = profile.value
  editForm.nickname = p.nickname || ''
  editForm.gender = p.gender ?? null
  editForm.birthDate = p.birthDate || ''
  editForm.phone = p.phone || ''
  editForm.notes = p.notes || ''
  showEdit.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const res = await authApi.updateProfile({ ...editForm, birthDate: editForm.birthDate || null })
    profile.value = res.data
    auth.user = res.data
    localStorage.setItem('user', JSON.stringify(res.data))
    showEdit.value = false
  } catch (e) {
    alert(e.message)
  } finally {
    saving.value = false
  }
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}

async function fetchProfile() {
  loadError.value = ''
  try {
    const res = await authApi.getProfile()
    if (!res.data) {
      loadError.value = '接口没有返回用户信息'
      return
    }
    profile.value = res.data
  } catch (e) {
    loadError.value = e.message || '网络异常'
  }
}

async function reload() {
  loading.value = true
  try {
    await fetchProfile()
    const res = await authApi.getStatistics()
    Object.assign(stats, res.data)
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>

<style scoped>
.inline-menu { border-right: none; }
.field-tip { font-size: 12px; color: #999; margin-top: 4px; }
</style>
