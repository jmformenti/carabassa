import { registerSW } from 'virtual:pwa-register'

export default defineNuxtPlugin(() => {
  if (!('serviceWorker' in navigator)) {
    // eslint-disable-next-line no-console
    console.warn('[pwa] Service workers not supported in this browser')
    return
  }

  window.dispatchEvent(new CustomEvent('pwa-sw-register-called'))
  registerSW({
    immediate: true,
    onRegistered (reg) {
      // eslint-disable-next-line no-console
      console.log('[pwa] Service worker registered', reg)
      window.dispatchEvent(new CustomEvent('pwa-sw-registered', { detail: { scope: reg?.scope } }))
    },
    onRegisterError (error) {
      // eslint-disable-next-line no-console
      console.error('[pwa] Service worker registration failed', error)
      window.dispatchEvent(new CustomEvent('pwa-sw-error', { detail: error }))
    },
  })
})
