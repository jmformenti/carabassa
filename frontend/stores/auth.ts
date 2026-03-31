import { defineStore } from 'pinia'
import { useRuntimeConfig } from '#app'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: null,
    username: null,
    role: null,
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    isAdmin: (state) => state.role === 'ADMIN',
    authHeaders: (state) => (state.token ? { Authorization: `Bearer ${state.token}` } : {}),
  },
  actions: {
    async login(username: string, password: string) {
      const config = useRuntimeConfig()
      const data: any = await $fetch(`${config.public.apiBaseUrl}/api/auth/login`, {
        method: 'POST',
        body: { username, password },
      })
      this.token = data.token
      this.username = data.username
      this.role = data.role
      if (import.meta.client) {
        localStorage.setItem(
          'carabassa_auth',
          JSON.stringify({
            token: this.token,
            username: this.username,
            role: this.role,
          })
        )
      }
    },
    logout() {
      this.token = null
      this.username = null
      this.role = null
      if (import.meta.client) {
        localStorage.removeItem('carabassa_auth')
      }
    },
    loadFromStorage() {
      if (import.meta.client) {
        const saved = localStorage.getItem('carabassa_auth')
        if (saved) {
          try {
            const data = JSON.parse(saved)
            this.token = data.token
            this.username = data.username
            this.role = data.role
          } catch (e) {
            // ignore
          }
        }
      }
    },
  },
})
