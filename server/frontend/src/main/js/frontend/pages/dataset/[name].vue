<template>
  <div>
    <v-container>
      <v-row v-if="showNoDatasetMessage">
        <v-col>
          <v-alert
            type="info"
            variant="tonal"
          >
            Dataset not found.
          </v-alert>
        </v-col>
      </v-row>
      
      <div v-if="hasDataset">
        <ItemSearch
          v-model="searchString"
          v-model:sortField="selectedSortField"
          v-model:sortDirection="selectedSortDirection"
          @search="search"
        />

        <v-row>
          <v-col>
            <div
              v-if="searched"
              class="text-body-2"
            >
              {{ totalItems }} found
            </div>
          </v-col>
        </v-row>

        <ItemList
          :items="items"
          :searched="searched"
          :loading="waitingResults"
          @select="expandItem"
        />

        <v-row>
          <v-col>
            <div
              v-if="leftItems > 0"
              class="text-body-2 text-center mt-4"
            >
              {{ leftItems }} items left
            </div>
          </v-col>
        </v-row>
      </div>

      <div
        v-if="!datasetsLoaded"
        class="text-center mt-12"
      >
        <v-progress-circular
          size="50"
          width="5"
          indeterminate
          color="primary"
        />
      </div>
    </v-container>

    <ItemDetailOverlay
      v-model="overlay"
      :item="selectedItem"
      :has-previous="hasPrevious"
      :has-next="hasNext"
      @previous="previousImage"
      @next="nextImage"
      @delete="confirmDelete"
    />

    <v-dialog
      v-model="deleteDialog"
      max-width="400"
    >
      <v-card>
        <v-card-title>Delete item</v-card-title>
        <v-card-text>Are you sure you want to delete this item?</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            color="orange-darken-2"
            @click="deleteDialog = false"
          >
            Cancel
          </v-btn>
          <v-btn
            color="orange-darken-2"
            @click="deleteItem"
          >
            Delete
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDatasetStore } from '../../stores/dataset'

const route = useRoute()
const router = useRouter()
const datasetStore = useDatasetStore()
const { $carabassa } = useNuxtApp()

const searchString = ref('')
const totalItems = ref(0)
const items = ref([])
const currentPage = ref(0)
const pageSize = ref(24)
const totalPages = ref(0)
const searched = ref(false)
const overlay = ref(false)
const selectedItem = ref(null)
const deleteDialog = ref(false)
const waitingResults = ref(false)
const selectedSortField = ref('archiveTime')
const selectedSortDirection = ref('desc')

const hasDataset = computed(() => !!datasetStore.dataset)
const datasetsLoaded = computed(() => datasetStore.datasetsLoaded)
const showNoDatasetMessage = computed(() => datasetStore.datasetsLoaded && !hasDataset.value)

const leftItems = computed(() => {
  const left = totalItems.value - (currentPage.value + 1) * pageSize.value
  return left > 0 ? left : 0
})

const selectedIndex = computed(() => items.value.indexOf(selectedItem.value))
const hasPrevious = computed(() => selectedIndex.value > 0)
const hasNext = computed(() => selectedIndex.value < items.value.length - 1 || currentPage.value + 1 < totalPages.value)

const combinedSort = computed(() => `${selectedSortField.value},${selectedSortDirection.value}`)

const getItems = async () => {
  if (!hasDataset.value) return
  waitingResults.value = true
  try {
    const data = await $carabassa.getItems(currentPage.value, pageSize.value, searchString.value, combinedSort.value)
    if (data._embedded) {
      totalItems.value = data.page.totalElements
      totalPages.value = data.page.totalPages
      items.value.push(...data._embedded.itemRepresentationList)
    }
    searched.value = true
  } catch (err) {
    console.error(err)
  } finally {
    waitingResults.value = false
  }
}

const reset = () => {
  currentPage.value = 0
  totalItems.value = 0
  totalPages.value = 0
  items.value = []
  searched.value = false
}

const search = () => {
  if (!hasDataset.value) return
  const query = { ...route.query }
  if (searchString.value) query.search = searchString.value
  else delete query.search
  
  query.sort = combinedSort.value
  
  router.push({ query })
  reset()
  getItems()
}

const expandItem = (item) => {
  selectedItem.value = item
  overlay.value = true
  window.addEventListener('keydown', handleKeyDown)
}

const previousImage = () => {
  if (selectedIndex.value > 0) {
    selectedItem.value = items.value[selectedIndex.value - 1]
  }
}

const nextImage = async () => {
  if (selectedIndex.value < items.value.length - 1) {
    selectedItem.value = items.value[selectedIndex.value + 1]
  } else if (currentPage.value + 1 < totalPages.value) {
    currentPage.value++
    await getItems()
    if (items.value.length > selectedIndex.value + 1) {
      selectedItem.value = items.value[selectedIndex.value + 1]
    }
  }
}

const handleKeyDown = (e) => {
  if (!overlay.value) return
  if (e.key === 'ArrowLeft') previousImage()
  if (e.key === 'ArrowRight') nextImage()
  if (e.key === 'Escape') overlay.value = false
}

const confirmDelete = (item) => {
  selectedItem.value = item
  deleteDialog.value = true
}

const deleteItem = async () => {
  try {
    const itemId = selectedItem.value.id
    await $carabassa.deleteItem(itemId)
    deleteDialog.value = false
    overlay.value = false
    items.value = items.value.filter(i => i.id !== itemId)
    totalItems.value--
    selectedItem.value = null
  } catch (err) {
    console.error(err)
  }
}

const enableInfiniteScroll = () => {
  window.onscroll = () => {
    const bottomOfWindow = Math.ceil(document.documentElement.scrollTop + window.innerHeight) >= document.documentElement.offsetHeight
    if (bottomOfWindow && !waitingResults.value && currentPage.value + 1 < totalPages.value) {
      currentPage.value++
      getItems()
    }
  }
}

onMounted(async () => {
  enableInfiniteScroll()
  
  // Wait for datasets to be loaded if they aren't yet
  const unwatch = watch(() => datasetStore.datasetsLoaded, (loaded) => {
    if (loaded) {
      selectDatasetFromRoute()
      unwatch()
    }
  }, { immediate: true })

  // Initial search from query params
  if (route.query.search) searchString.value = route.query.search
  if (route.query.sort) {
    const [field, direction] = route.query.sort.split(',')
    selectedSortField.value = field
    selectedSortDirection.value = direction || 'desc'
  } else {
    // Ensure the UI reflects the default sort direction when no query is provided
    selectedSortDirection.value = 'desc'
  }

  // If dataset is already set and matches the route, trigger search immediately
  if (datasetStore.datasetsLoaded && datasetStore.dataset && datasetStore.dataset.name === route.params.name) {
    getItems()
  }
})

onUnmounted(() => {
  window.onscroll = null
  window.removeEventListener('keydown', handleKeyDown)
})

const selectDatasetFromRoute = async () => {
  const datasetName = route.params.name
  // Only update if the dataset from route is different from the current state
  if (datasetName && (!datasetStore.dataset || datasetStore.dataset.name !== datasetName)) {
    try {
      const data = await $carabassa.getDatasetByName(datasetName)
      datasetStore.dataset = data
    } catch (err) {
      console.error('Error loading dataset from route:', err)
    }
  }
}

watch(() => route.params.name, (newName) => {
  if (newName) selectDatasetFromRoute()
})

watch(overlay, (val) => {
  if (!val) window.removeEventListener('keydown', handleKeyDown)
})

watch(() => datasetStore.dataset, (newVal) => {
  if (newVal) {
    reset()
    getItems()
  }
})
</script>
