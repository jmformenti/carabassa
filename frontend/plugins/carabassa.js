import { defineNuxtPlugin } from "nuxt/app"
import { CarabassaService } from '~/service/carabassa.service'
import { useDatasetStore } from "~~/stores/dataset"
import { useAuthStore } from "~~/stores/auth"

export default defineNuxtPlugin(nuxtApp => {
  const datasetStore = useDatasetStore()
  const authStore = useAuthStore()
  const carabassaService = new CarabassaService(datasetStore, authStore)
  
  nuxtApp.provide('carabassa', carabassaService)
})
