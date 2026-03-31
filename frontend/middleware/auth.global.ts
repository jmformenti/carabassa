import { defineNuxtRouteMiddleware, navigateTo, useRuntimeConfig } from '#app'
import { useAuthStore } from '~/stores/auth'

export default defineNuxtRouteMiddleware((to) => {
  const config = useRuntimeConfig()
  
  // If auth is disabled globally, do nothing
  if (!config.public.authEnabled) {
    return
  }

  // Prevent infinite redirects
  if (to.path === '/login') {
    return
  }

  const authStore = useAuthStore()
  
  // Try to load auth token from local storage on client side
  if (!authStore.isAuthenticated) {
    authStore.loadFromStorage()
  }

  if (!authStore.isAuthenticated) {
    return navigateTo('/login')
  }

  // Only ADMIN can access /admin route
  if (to.path.startsWith('/admin') && !authStore.isAdmin) {
    return navigateTo('/')
  }
})
