import { defineNuxtPlugin } from "nuxt/app"
import AWN from "awesome-notifications"

export default defineNuxtPlugin(nuxtApp => {
  const globalOptions = {}
  
  nuxtApp.provide('notification', new AWN(globalOptions))
})