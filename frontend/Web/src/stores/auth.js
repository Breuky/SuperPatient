import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const loading = ref(false)

  const isLoggedIn = computed(() => !!token.value)

  function restoreSession() {
    const t = localStorage.getItem('token')
    const u = localStorage.getItem('user')
    if (t) token.value = t
    if (u) {
      try { user.value = JSON.parse(u) } catch { user.value = null }
    }
  }

  async function login(code, nickname, avatarUrl) {
    loading.value = true
    try {
      const res = await authApi.login({ code, nickname, avatarUrl })
      token.value = res.data.token
      user.value = res.data.userInfo
      localStorage.setItem('token', token.value)
      localStorage.setItem('user', JSON.stringify(user.value))
      return true
    } finally {
      loading.value = false
    }
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, loading, isLoggedIn, restoreSession, login, logout }
})
