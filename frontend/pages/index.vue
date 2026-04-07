<template>
  <v-container v-if="datasetsLoaded && !hasDataset">
    <v-alert
      type="info"
      variant="tonal"
    >
      No dataset found.
    </v-alert>
  </v-container>
</template>

<script setup>
import { computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useDatasetStore } from '../stores/dataset'

const datasetStore = useDatasetStore()
const router = useRouter()
const { $carabassa } = useNuxtApp()

const hasDataset = computed(() => !!datasetStore.dataset)
const datasetsLoaded = computed(() => datasetStore.datasetsLoaded)

// Redirect to the dataset page as soon as one is selected/available
watch(() => datasetStore.dataset, (newDataset) => {
  if (newDataset) {
    router.replace(`/dataset/${newDataset.name}`)
  }
}, { immediate: true })

onMounted(async () => {
  if (datasetsLoaded.value && !hasDataset.value) {
    try {
      const datasets = await $carabassa.getDatasets()
      if (datasets && datasets.length > 0) {
        datasetStore.dataset = datasets[0]
      }
    } catch (e) {
      console.error('Error fetching datasets on index page:', e)
    }
  }
})
</script>
