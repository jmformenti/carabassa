import { defineNuxtConfig } from 'nuxt/config'
import eslintPlugin from 'vite-plugin-eslint2'

// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2026-01-30',
  ssr: false,
  app: {
    head: {
      title: 'Carabassa',
      link: [
        { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' }
      ]
    }
  },
  runtimeConfig: {
    public: {
      // to override, export NUXT_PUBLIC_API_BASE_URL
      apiBaseUrl: 'http://localhost:8080',
      appVersion: ''
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
