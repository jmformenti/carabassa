<template>
  <ItemCollection 
    key="favorites-page"
    title="Favorites"
    :allow-search="false"
    :allow-upload="false"
    :favorites="true"
  />
</template>

<script setup>
import ItemCollection from '~/components/ItemCollection.vue'
import { useDatasetStore } from '~/stores/dataset'
import { onMounted } from 'vue'

const datasetStore = useDatasetStore()

onMounted(() => {
  // Ensure we are in a clean global state for favorites
  datasetStore.dataset = null
})
</script>
