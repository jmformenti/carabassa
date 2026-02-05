import { defineNuxtConfig } from 'nuxt/config'
import eslintPlugin from 'vite-plugin-eslint2'

// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2026-01-30',
  ssr: false,
  runtimeConfig: {
    public: {
      // to override, export NUXT_PUBLIC_API_BASE_URL=http://192.168.1.2:8080
      apiBaseURL: 'http://localhost:8080'
    }
  },
  css: [
    'vuetify/styles',
    '@mdi/font/css/materialdesignicons.min.css',
    'awesome-notifications/dist/style.css',
    '@fortawesome/fontawesome-free/css/all.min.css'
  ],
  build: {
    transpile: ['vuetify'],
  },
  imports: {
    dirs: ['stores'],
  },
  modules: ['@pinia/nuxt'],
  vite: {
    define: {
      'process.env.DEBUG': false,
    },
    plugins: [
      eslintPlugin()
    ]
  }
})
