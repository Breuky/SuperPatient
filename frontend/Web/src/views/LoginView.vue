<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <div class="logo">
        <div class="logo-icon">SP</div>
        <h1>SuperPatient</h1>
        <p>长期记录你的检查指标</p>
      </div>

      <el-form @submit.prevent="handleLogin" label-position="top">
        <el-form-item label="用户名">
          <el-input
            v-model="username"
            placeholder="请输入用户名"
            autocomplete="username"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="password"
            type="password"
            show-password
            placeholder="请输入密码"
            autocomplete="current-password"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="handleLogin" :loading="auth.loading" style="width:100%">
            {{ auth.loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <p v-if="error" class="error-msg">{{ error }}</p>

      <div class="foot">
        <router-link to="/register">还没有账号？注册</router-link>
      </div>
      <p class="hint">演示账号：demo / demo1234</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const username = ref('')
const password = ref('')
const error = ref('')

async function handleLogin() {
  error.value = ''
  if (!username.value || !password.value) {
    error.value = '请输入用户名和密码'
    return
  }
  try {
    await auth.login(username.value.trim(), password.value)
    router.push('/records')
  } catch (e) {
    error.value = e.message || '登录失败'
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
}
.auth-card { width: 400px; }
.logo { text-align: center; margin-bottom: 24px; }
.logo-icon {
  width: 56px; height: 56px; border-radius: 16px;
  background: var(--green); color: #fff; font-size: 20px; font-weight: 700;
  display: inline-flex; align-items: center; justify-content: center; margin-bottom: 12px;
}
.logo h1 { font-size: 20px; font-weight: 700; margin-bottom: 4px; }
.logo p { font-size: 13px; color: #999; }
.error-msg { color: var(--red); font-size: 13px; text-align: center; margin-top: 8px; }
.foot { text-align: center; font-size: 13px; margin-top: 4px; }
.foot a { color: var(--green); }
.hint { font-size: 12px; color: #ccc; text-align: center; margin-top: 12px; }
</style>
