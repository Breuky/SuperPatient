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
        <h1>从第一次检查<br />开始建立档案</h1>
        <p class="tagline">注册后可以拍照录入、按指标看趋势，并把长期关心的项标星置顶。</p>
        <div class="brand-points">
          <div class="brand-point"><i>1</i><span>填性别和出生日期，参考区间会自动匹配</span></div>
          <div class="brand-point"><i>2</i><span>公共指标库开箱可用，缺的项自己补</span></div>
          <div class="brand-point"><i>3</i><span>数据只存在这套部署里，不上传第三方</span></div>
        </div>
      </div>

      <div class="brand-foot">演示环境 · 不含真实个人健康信息</div>
    </aside>

    <main class="auth-panel">
      <div class="auth-card">
        <h2>创建账号</h2>
        <p class="sub">几十秒就能开始记第一条</p>

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
            <el-button type="primary" size="large" @click="handleRegister" :loading="auth.loading" style="width:100%">
              {{ auth.loading ? '注册中...' : '注册并登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <p v-if="error" class="error-msg">{{ error }}</p>

        <div class="foot">
          已有账号？<router-link to="/login">去登录</router-link>
        </div>
      </div>
    </main>
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
.field-tip { font-size: var(--font-xs); color: var(--text-hint); margin-top: 4px; }
.brand-foot { font-size: var(--font-xs); color: rgba(255, 255, 255, 0.62); letter-spacing: 0.02em; }
</style>
