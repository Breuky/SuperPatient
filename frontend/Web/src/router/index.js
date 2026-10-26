import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { guest: true, title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { guest: true, title: '注册' }
  },
  {
    path: '/',
    redirect: '/records'
  },
  {
    path: '/records',
    name: 'Records',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '我的检查记录' }
  },
  {
    path: '/records/new',
    name: 'RecordNew',
    component: () => import('@/views/RecordFormView.vue'),
    meta: { title: '新增检查' }
  },
  {
    path: '/records/:id/edit',
    name: 'RecordEdit',
    component: () => import('@/views/RecordFormView.vue'),
    meta: { title: '编辑检查' }
  },
  {
    path: '/records/:id',
    name: 'RecordDetail',
    component: () => import('@/views/RecordDetailView.vue'),
    meta: { title: '检查详情' }
  },
  {
    path: '/assistant',
    name: 'Assistant',
    component: () => import('@/views/AssistantView.vue'),
    meta: { title: 'AI 助手' }
  },
  {
    path: '/trends',
    name: 'Trend',
    component: () => import('@/views/TrendView.vue'),
    meta: { title: '趋势分析' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { title: '个人信息' }
  },
  {
    path: '/categories',
    name: 'CategoryManage',
    component: () => import('@/views/CategoryManageView.vue'),
    meta: { title: '分类管理' }
  },
  {
    path: '/indicators',
    name: 'IndicatorManage',
    component: () => import('@/views/IndicatorManageView.vue'),
    meta: { title: '指标管理' }
  },
  {
    path: '/insight-logs',
    name: 'InsightLogs',
    component: () => import('@/views/InsightLogsView.vue'),
    meta: { title: 'Agent 调试' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.meta.guest) {
    if (auth.isLoggedIn) return next('/records')
    return next()
  }
  if (!auth.isLoggedIn) {
    return next('/login')
  }
  next()
})

export default router
