<template>
  <div v-if="isLoginPage" class="login-wrapper">
    <router-view />
  </div>

  <el-container v-else class="app-container">
    <el-header class="app-header">
      <div class="header-left">
        <span class="logo-text">SuperPatient</span>
        <span class="subtitle">检查记录系统</span>
      </div>
      <div class="header-right">
        <span class="user-name">{{ user?.nickname || '未登录' }}</span>
        <el-button type="danger" text @click="handleLogout">退出</el-button>
      </div>
    </el-header>

    <el-container>
      <el-aside width="200px" class="app-aside">
        <el-menu
          :default-active="activeMenu"
          router
          :collapse="false"
          class="side-menu"
        >
          <el-menu-item index="/records">
            <el-icon><Document /></el-icon>
            <span>我的记录</span>
          </el-menu-item>
          <el-menu-item index="/trends">
            <el-icon><TrendCharts /></el-icon>
            <span>趋势分析</span>
          </el-menu-item>
          <el-menu-item index="/categories">
            <el-icon><FolderOpened /></el-icon>
            <span>分类管理</span>
          </el-menu-item>
          <el-menu-item index="/indicators">
            <el-icon><List /></el-icon>
            <span>指标管理</span>
          </el-menu-item>
          <el-menu-item index="/profile">
            <el-icon><UserFilled /></el-icon>
            <span>个人信息</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const isLoginPage = computed(() => route.path === '/login')
const user = computed(() => auth.user)

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/records')) return '/records'
  return path
})

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-container {
  height: 100vh;
}

.app-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 56px;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--green);
}

.subtitle {
  font-size: 13px;
  color: #909399;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 14px;
  color: #606266;
}

.app-aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
}

.side-menu {
  border-right: none;
}

.app-main {
  background: var(--bg-primary);
  min-height: calc(100vh - 56px);
  padding: 20px;
}

.login-wrapper {
  min-height: 100vh;
}
</style>
