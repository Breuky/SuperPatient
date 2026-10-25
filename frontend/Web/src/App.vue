<template>
  <div v-if="isLoginPage" class="login-wrapper">
    <router-view />
  </div>

  <el-container v-else class="app-shell">
    <el-header class="app-header">
      <div class="brand">
        <div class="brand-mark">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none"
               stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 12h4l2.2-6 4 12 2.2-6H21" />
          </svg>
        </div>
        <div class="brand-text">
          <span class="brand-name">SuperPatient</span>
          <span class="brand-sub">检查指标追踪</span>
        </div>
      </div>

      <div class="header-right">
        <div class="user-chip">
          <span class="avatar">{{ initial }}</span>
          <span class="user-name">{{ user?.nickname || user?.username || '未登录' }}</span>
        </div>
        <el-button type="danger" text @click="handleLogout">退出</el-button>
      </div>
    </el-header>

    <el-container class="app-body">
      <el-aside width="252px" class="app-aside">
        <el-menu :default-active="activeMenu" router class="side-menu">
          <li class="menu-label">日常</li>
          <el-menu-item index="/records">
            <el-icon><Document /></el-icon>
            <span>我的记录</span>
          </el-menu-item>
          <el-menu-item index="/trends">
            <el-icon><TrendCharts /></el-icon>
            <span>趋势分析</span>
          </el-menu-item>

          <li class="menu-label">AI</li>
          <el-menu-item index="/insight-logs">
            <el-icon><MagicStick /></el-icon>
            <span>调用记录</span>
          </el-menu-item>

          <li class="menu-label">设置</li>
          <el-menu-item index="/indicators">
            <el-icon><List /></el-icon>
            <span>指标管理</span>
          </el-menu-item>
          <el-menu-item index="/categories">
            <el-icon><FolderOpened /></el-icon>
            <span>分类管理</span>
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
const initial = computed(() => {
  const name = auth.user?.nickname || auth.user?.username || '?'
  return name.trim().charAt(0).toUpperCase()
})

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
.login-wrapper { min-height: 100vh; }

.app-header {
  height: 60px;
  padding: 0 24px;
  background: var(--bg-white);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 5;
}

.brand { display: flex; align-items: center; gap: 10px; }
.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(140deg, #0BA379 0%, #047857 100%);
  box-shadow: 0 2px 8px rgba(4, 120, 87, 0.28);
}
.brand-text { display: flex; flex-direction: column; line-height: 1.2; }
.brand-name { font-size: 15px; font-weight: 700; letter-spacing: -0.01em; }
.brand-sub { font-size: 11px; color: var(--text-hint); letter-spacing: 0.02em; }

.header-right { display: flex; align-items: center; gap: 8px; }
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  background: var(--bg-subtle);
  border: 1px solid var(--border-light);
}
.avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--green-dark);
  background: var(--green-light);
}
.user-name { font-size: var(--font-sm); color: var(--text-secondary); }

.app-aside {
  background: var(--bg-white);
  border-right: 1px solid var(--border);
  padding: 14px 12px;
  overflow-x: hidden;
}

.side-menu {
  border-right: none;
  background: transparent;
  --el-menu-item-height: 40px;
  --el-menu-base-level-padding: 12px;
}
.menu-label {
  list-style: none;
  padding: 14px 12px 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--text-hint);
}
.menu-label:first-child { padding-top: 4px; }

.side-menu :deep(.el-menu-item) {
  height: 40px;
  line-height: 40px;
  margin-bottom: 2px;
  border-radius: 9px;
  font-size: var(--font-base);
  color: var(--text-secondary);
}
.side-menu :deep(.el-menu-item:hover) {
  background: var(--bg-subtle);
  color: var(--text-primary);
}
.side-menu :deep(.el-menu-item.is-active) {
  background: var(--green-light);
  color: var(--green-dark);
  font-weight: 600;
}
.side-menu :deep(.el-menu-item .el-icon) { font-size: 17px; margin-right: 8px; }
</style>
