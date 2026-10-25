<template>
  <div class="auth-page">
    <aside class="auth-brand">
      <div class="brand-top">
        <div class="brand-mark">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none"
               stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 12h4l2.2-6 4 12 2.2-6H21" />
          </svg>
        </div>
        <span>SuperPatient</span>
      </div>

      <div>
        <h1>把化验单照片<br />变成看得懂的趋势</h1>
        <p class="tagline">同一个指标在不同医院、不同年份的参考区间都不一样。这里把它们对齐，画成一条能对比的线。</p>
        <div class="brand-points">
          <div class="brand-point"><i>1</i><span>拍照识别，逐项校对后入库，模型不直接写数据</span></div>
          <div class="brand-point"><i>2</i><span>参考区间随每次报告分段保存，异常值一眼看到</span></div>
          <div class="brand-point"><i>3</i><span>记录只属于你，报告照片按账号隔离</span></div>
        </div>
      </div>

      <div class="brand-foot">演示环境 · 不含真实个人健康信息</div>
    </aside>

    <main class="auth-panel">
      <div class="auth-card">
        <h2>欢迎回来</h2>
        <p class="sub">登录后继续记录你的检查指标</p>

        <el-form @submit.prevent="handleLogin" label-position="top">
          <el-form-item label="用户名">
            <el-input
              v-model="username"
              size="large"
              placeholder="请输入用户名"
              autocomplete="username"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="password"
              size="large"
              type="password"
              show-password
              placeholder="请输入密码"
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" @click="handleLogin" :loading="auth.loading" style="width:100%">
              {{ auth.loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <p v-if="error" class="error-msg">{{ error }}</p>

        <div class="foot">
          还没有账号？<router-link to="/register">创建一个</router-link>
        </div>
        <p class="demo-hint">演示账号：demo / demo1234</p>
      </div>
    </main>
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
.brand-foot { font-size: var(--font-xs); color: rgba(255, 255, 255, 0.62); letter-spacing: 0.02em; }
.demo-hint { margin-top: 12px; font-size: var(--font-xs); color: var(--text-disabled); text-align: center; }
</style>
