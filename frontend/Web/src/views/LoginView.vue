<template>
  <div class="login-page">
    <el-card class="login-card">
      <div class="logo">
        <div class="logo-icon">SP</div>
        <h1>SuperPatient</h1>
        <p>检查记录系统</p>
      </div>
      <el-form @submit.prevent="handleLogin" label-position="top">
        <el-form-item label="微信登录凭证">
          <el-input v-model="code" placeholder="请输入 wx.login() code" />
        </el-form-item>
        <el-form-item label="昵称（可选）">
          <el-input v-model="nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="handleLogin" :loading="auth.loading" style="width:100%">
            {{ auth.loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>
      <p v-if="error" class="error-msg">{{ error }}</p>
      <p class="hint">开发环境下输入任意 code 即可登录</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const code = ref('dev_test_code')
const nickname = ref('测试医生')
const error = ref('')

async function handleLogin() {
  error.value = ''
  try {
    await auth.login(code.value, nickname.value, null)
    router.push('/home')
  } catch (e) {
    error.value = e.message || '登录失败'
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #07C160 0%, #06AD56 100%);
}
.login-card {
  width: 400px;
}
.logo { text-align: center; margin-bottom: 24px; }
.logo-icon {
  width: 56px; height: 56px; border-radius: 16px;
  background: var(--green); color: #fff; font-size: 20px; font-weight: 700;
  display: inline-flex; align-items: center; justify-content: center; margin-bottom: 12px;
}
.logo h1 { font-size: 20px; font-weight: 700; margin-bottom: 4px; }
.logo p { font-size: 13px; color: #999; }
.error-msg { color: var(--red); font-size: 13px; text-align: center; margin-top: 8px; }
.hint { font-size: 12px; color: #ccc; text-align: center; margin-top: 12px; }
</style>
