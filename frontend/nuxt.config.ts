import { defineNuxtConfig } from 'nuxt/config'
import eslintPlugin from 'vite-plugin-eslint2'

// https://nuxt.com/docs/api/configuration/nuxt-config

export default defineNuxtConfig({
  compatibilityDate: '2026-01-30',
  ssr: false,
  app: {
    head: {
      title: 'Carabassa',
      meta: [
        { name: 'theme-color', content: '#0f6b5a' },
        { name: 'apple-mobile-web-app-capable', content: 'yes' },
        { name: 'apple-mobile-web-app-status-bar-style', content: 'default' }
      ],
      link: [
        { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' },
        { rel: 'manifest', href: '/manifest.webmanifest' },
        { rel: 'apple-touch-icon', href: '/icons/apple-touch-icon.png' }
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
  plugins: [
    '~/plugins/pwa.client',
  ],
  modules: ['@pinia/nuxt', '@vite-pwa/nuxt'],
  pwa: {
    registerType: 'autoUpdate',
    devOptions: {
      enabled: true
    },
    manifest: {
      name: 'Carabassa',
      short_name: 'Carabassa',
      description: 'Carabassa dataset tagging tool',
      theme_color: '#0f6b5a',
      background_color: '#ffffff',
      display: 'standalone',
      start_url: '/',
      scope: '/',
      icons: [
        {
          src: '/icons/icon-192.png',
          sizes: '192x192',
          type: 'image/png',
          purpose: 'any'
        },
        {
          src: '/icons/icon-192-maskable.png',
          sizes: '192x192',
          type: 'image/png',
          purpose: 'maskable'
        },
        {
          src: '/icons/icon-512.png',
          sizes: '512x512',
          type: 'image/png',
          purpose: 'any'
        },
        {
          src: '/icons/icon-512-maskable.png',
          sizes: '512x512',
          type: 'image/png',
          purpose: 'maskable'
        }
      ]
    },
    workbox: {
      navigateFallback: '/'
    }
  },
  vite: {
    define: {
      'process.env.DEBUG': false,
    },
    plugins: [
      eslintPlugin()
    ]
  }
})
