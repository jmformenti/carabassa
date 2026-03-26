<template>
  <v-container>
    <v-card>
      <v-card-title class="text-h6">
        PWA Debug
      </v-card-title>
      <v-card-subtitle>
        Installation and Service Worker diagnostics
      </v-card-subtitle>
      <v-card-text class="text-body-2">
        <div>android: {{ debug.isAndroid }}</div>
        <div>secure: {{ debug.isSecureContext }}</div>
        <div>display-mode: {{ debug.displayMode }}</div>
        <div>standalone: {{ debug.isStandalone }}</div>
        <div>sw supported: {{ debug.swSupported }}</div>
        <div>sw registered: {{ debug.swRegistered }}</div>
        <div>sw controlling: {{ debug.swControlling }}</div>
        <div>sw error: {{ debug.swError || 'none' }}</div>
        <div>register called: {{ debug.registerCalled }}</div>
        <div>register scope: {{ debug.registerScope || 'n/a' }}</div>
        <div>manifest status: {{ debug.manifestStatus || 'n/a' }}</div>
        <div>sw fetch: {{ debug.swFetchStatus || 'n/a' }}</div>
        <div>beforeinstallprompt: {{ debug.beforeInstallPromptFired }}</div>
        <div>appinstalled: {{ debug.appInstalled }}</div>
      </v-card-text>
      <v-card-actions>
        <v-btn
          color="primary"
          @click="refresh"
        >
          Refresh
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive } from 'vue'

const debug = reactive({
  isAndroid: false,
  isSecureContext: false,
  displayMode: 'unknown',
  isStandalone: false,
  swSupported: false,
  swRegistered: false,
  swControlling: false,
  swError: '',
  registerCalled: false,
  registerScope: '',
  manifestStatus: '',
  swFetchStatus: '',
  beforeInstallPromptFired: false,
  appInstalled: false,
})

const formatError = (error) => {
  if (!error) return ''
  if (typeof error === 'string') return error
  if (error.message) return error.message
  try {
    return JSON.stringify(error)
  } catch {
    return String(error)
  }
}

const getDisplayMode = () => {
  if (window.matchMedia('(display-mode: standalone)').matches) {
    return 'standalone'
  }
  if (window.matchMedia('(display-mode: fullscreen)').matches) {
    return 'fullscreen'
  }
  if (window.matchMedia('(display-mode: minimal-ui)').matches) {
    return 'minimal-ui'
  }
  return 'browser'
}

const log = (source) => {
  // eslint-disable-next-line no-console
  console.log(`[pwa-debug:${source}]`, { ...debug })
}

const handleBeforeInstallPrompt = () => {
  debug.beforeInstallPromptFired = true
  log('beforeinstallprompt')
}

const handleAppInstalled = () => {
  debug.appInstalled = true
  log('appinstalled')
}

const handleSwError = (event) => {
  debug.swError = formatError(event?.detail)
  log('sw-error-event')
}

const handleSwRegisterCalled = () => {
  debug.registerCalled = true
  log('sw-register-called')
}

const handleSwRegistered = (event) => {
  debug.registerScope = event?.detail?.scope || ''
  debug.swRegistered = true
  log('sw-registered-event')
}

const handleDisplayModeChange = () => {
  debug.displayMode = getDisplayMode()
  debug.isStandalone = debug.displayMode === 'standalone'
  log('display-mode')
}

const refresh = async () => {
  debug.displayMode = getDisplayMode()
  debug.isStandalone = debug.displayMode === 'standalone'
  debug.swSupported = 'serviceWorker' in navigator
  debug.isSecureContext = window.isSecureContext

  if (debug.swSupported) {
    try {
      const reg = await navigator.serviceWorker.getRegistration()
      debug.swRegistered = !!reg
      debug.swControlling = !!navigator.serviceWorker.controller
    } catch (error) {
      debug.swRegistered = false
      debug.swError = formatError(error)
    }
  }

  try {
    const manifestRes = await fetch('/manifest.webmanifest', { cache: 'no-store' })
    debug.manifestStatus = `${manifestRes.status} ${manifestRes.ok ? 'ok' : 'fail'}`
  } catch (error) {
    debug.manifestStatus = `error: ${formatError(error)}`
  }

  const swCandidates = ['/sw.js', '/dev-sw.js', '/sw.js?dev-sw', '/dev-sw.js?dev-sw']
  const results = []
  for (const url of swCandidates) {
    try {
      const res = await fetch(url, { cache: 'no-store' })
      results.push(`${url}:${res.status}`)
    } catch {
      results.push(`${url}:error`)
    }
  }
  debug.swFetchStatus = results.join(' | ')
  log('refresh')
}

onMounted(() => {
  const ua = navigator.userAgent || ''
  debug.isAndroid = /Android/i.test(ua)
  refresh()

  window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt)
  window.addEventListener('appinstalled', handleAppInstalled)
  window.addEventListener('pwa-sw-error', handleSwError)
  window.addEventListener('pwa-sw-register-called', handleSwRegisterCalled)
  window.addEventListener('pwa-sw-registered', handleSwRegistered)
  window.matchMedia('(display-mode: standalone)').addEventListener('change', handleDisplayModeChange)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt)
  window.removeEventListener('appinstalled', handleAppInstalled)
  window.removeEventListener('pwa-sw-error', handleSwError)
  window.removeEventListener('pwa-sw-register-called', handleSwRegisterCalled)
  window.removeEventListener('pwa-sw-registered', handleSwRegistered)
  window.matchMedia('(display-mode: standalone)').removeEventListener('change', handleDisplayModeChange)
})
</script>
