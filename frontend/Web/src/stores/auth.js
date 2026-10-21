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

  function applyAuth(data) {
    token.value = data.token
    user.value = data.userInfo
    localStorage.setItem('token', token.value)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  async function login(username, password) {
    loading.value = true
    try {
      const res = await authApi.login({ username, password })
      applyAuth(res.data)
      return true
    } finally {
      loading.value = false
    }
  }

  async function register(payload) {
    loading.value = true
    try {
      const res = await authApi.register(payload)
      applyAuth(res.data)
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

  return { token, user, loading, isLoggedIn, restoreSession, login, register, logout }
})
