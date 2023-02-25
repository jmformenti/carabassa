import { defineNuxtPlugin } from "nuxt/app"
import { CarabassaService } from '~/service/carabassa.service'
import { useDatasetStore } from "~~/stores/dataset"

export default defineNuxtPlugin(nuxtApp => {
  const datasetStore = useDatasetStore()
  const carabassaService = new CarabassaService(datasetStore)
  
  nuxtApp.provide('carabassa', carabassaService)
})
