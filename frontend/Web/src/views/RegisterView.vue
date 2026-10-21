<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <div class="logo">
        <div class="logo-icon">SP</div>
        <h1>创建账号</h1>
        <p>注册后即可开始记录</p>
      </div>

      <el-form @submit.prevent="handleRegister" label-position="top">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" placeholder="3-32 位字母、数字、下划线或中文" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="确认密码" required>
          <el-input v-model="form.confirm" type="password" show-password placeholder="再输入一次" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="不填则与用户名相同" />
        </el-form-item>
        <el-form-item label="性别" required>
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="0">女</el-radio>
          </el-radio-group>
          <div class="field-tip">用于匹配参考区间，注册后可在个人信息里修改</div>
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="handleRegister" :loading="auth.loading" style="width:100%">
            {{ auth.loading ? '注册中...' : '注册并登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <p v-if="error" class="error-msg">{{ error }}</p>

      <div class="foot">
        <router-link to="/login">已有账号？去登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const error = ref('')

const form = reactive({
  username: '',
  password: '',
  confirm: '',
  nickname: '',
  gender: 1
})

async function handleRegister() {
  error.value = ''
  if (!form.username || !form.password) {
    error.value = '请填写用户名和密码'
    return
  }
  if (form.password !== form.confirm) {
    error.value = '两次输入的密码不一致'
    return
  }
  try {
    await auth.register({
      username: form.username.trim(),
      password: form.password,
      nickname: form.nickname.trim() || undefined,
      gender: form.gender
    })
    router.push('/records')
  } catch (e) {
    error.value = e.message || '注册失败'
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #07C160 0%, #06AD56 100%);
  padding: 24px 0;
}
.auth-card { width: 420px; }
.logo { text-align: center; margin-bottom: 20px; }
.logo-icon {
  width: 52px; height: 52px; border-radius: 16px;
  background: var(--green); color: #fff; font-size: 18px; font-weight: 700;
  display: inline-flex; align-items: center; justify-content: center; margin-bottom: 12px;
}
.logo h1 { font-size: 19px; font-weight: 700; margin-bottom: 4px; }
.logo p { font-size: 13px; color: #999; }
.field-tip { font-size: 12px; color: #999; margin-top: 4px; }
.error-msg { color: var(--red); font-size: 13px; text-align: center; margin-top: 8px; }
.foot { text-align: center; font-size: 13px; }
.foot a { color: var(--green); }
</style>
