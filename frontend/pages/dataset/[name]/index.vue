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

        <v-row v-if="searchError">
          <v-col>
            <v-alert
              type="error"
              variant="tonal"
              closable
              @click:close="searchError = null"
            >
              {{ searchError }}
            </v-alert>
          </v-col>
        </v-row>

        <v-row v-if="searched">
          <v-col class="d-flex align-center">
            <div class="text-body-2">
              {{ totalItems }} found
            </div>
            <v-spacer />
            <v-btn
              v-if="selectedIds.length > 0"
              color="orange-darken-2"
              variant="tonal"
              prepend-icon="mdi-trash-can-outline"
              class="ml-2"
              @click="confirmBulkDelete"
            >
              Delete ({{ selectedIds.length }})
            </v-btn>
          </v-col>
        </v-row>

        <ItemList
          :items="items"
          :searched="searched"
          :loading="waitingResults"
          :selectable="true"
          :selected-items="selectedIds"
          @select="expandItem"
          @toggle-select="toggleSelect"
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

    <v-dialog
      v-model="bulkDeleteDialog"
      max-width="400"
    >
      <v-card>
        <v-card-title>Delete items</v-card-title>
        <v-card-text>Are you sure you want to delete {{ selectedIds.length }} selected items?</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            color="orange-darken-2"
            @click="bulkDeleteDialog = false"
          >
            Cancel
          </v-btn>
          <v-btn
            color="orange-darken-2"
            :loading="deletingBulk"
            @click="deleteSelectedItems"
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
import { useDatasetStore } from '../../../stores/dataset'

const route = useRoute()
const router = useRouter()
const datasetStore = useDatasetStore()
const { $carabassa } = useNuxtApp()

const initialState = datasetStore.listState.datasetName === route.params.name
  ? datasetStore.listState
  : {
    datasetName: route.params.name,
    items: [],
    currentPage: 0,
    totalItems: 0,
    totalPages: 0,
    searchString: route.query.search || '',
    sortField: 'archiveTime',
    sortDirection: 'desc',
    scrollPosition: 0,
    searched: false
  }

const searchString = ref(initialState.searchString)
const totalItems = ref(initialState.totalItems)
const items = ref([...initialState.items])
const currentPage = ref(initialState.currentPage)
const pageSize = ref(24)
const totalPages = ref(initialState.totalPages)
const searched = ref(initialState.searched)
const overlay = ref(false)
const selectedItem = ref(null)
const deleteDialog = ref(false)
const waitingResults = ref(false)
const selectedSortField = ref(initialState.sortField)
const selectedSortDirection = ref(initialState.sortDirection)
const searchError = ref(null)

const selectedIds = ref([])
const bulkDeleteDialog = ref(false)
const deletingBulk = ref(false)

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
  if (!hasDataset.value || waitingResults.value) return
  const requestedPage = currentPage.value
  waitingResults.value = true
  searchError.value = null
  try {
    const data = await $carabassa.getItems(requestedPage, pageSize.value, searchString.value, combinedSort.value)
    if (data._embedded) {
      totalItems.value = data.page.totalElements
      totalPages.value = data.page.totalPages
      const incoming = data._embedded.itemRepresentationList
      if (requestedPage === 0) {
        items.value = incoming
      } else {
        items.value.push(...incoming)
      }
      const seenIds = new Set()
      items.value = items.value.filter((item) => {
        if (seenIds.has(item.id)) return false
        seenIds.add(item.id)
        return true
      })
      
      datasetStore.setListState({
        items: [...items.value],
        totalItems: totalItems.value,
        totalPages: totalPages.value,
        searched: true,
        currentPage: requestedPage
      })
    }
    searched.value = true
  } catch (err) {
    if (err.data && err.data.message) {
      searchError.value = err.data.message
    } else {
      searchError.value = 'An error occurred while searching.'
    }
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
  datasetStore.resetListState()
  datasetStore.setListState({ datasetName: route.params.name })
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
    datasetStore.setListState({
      items: [...items.value],
      totalItems: totalItems.value
    })
  } catch (err) {
    console.error(err)
  }
}

const toggleSelect = (item) => {
  const index = selectedIds.value.indexOf(item.id)
  if (index === -1) {
    selectedIds.value.push(item.id)
  } else {
    selectedIds.value.splice(index, 1)
  }
}

const confirmBulkDelete = () => {
  bulkDeleteDialog.value = true
}

const deleteSelectedItems = async () => {
  deletingBulk.value = true
  try {
    for (const id of selectedIds.value) {
      await $carabassa.deleteItem(id)
    }
    items.value = items.value.filter(item => !selectedIds.value.includes(item.id))
    totalItems.value -= selectedIds.value.length
    selectedIds.value = []
    bulkDeleteDialog.value = false
    datasetStore.setListState({
      items: [...items.value],
      totalItems: totalItems.value
    })
  } catch (err) {
    console.error('Error deleting items:', err)
  } finally {
    deletingBulk.value = false
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

const applySortFromRoute = () => {
  if (route.query.sort) {
    const [field, direction] = route.query.sort.split(',')
    selectedSortField.value = field
    selectedSortDirection.value = direction || 'desc'
  } else {
    selectedSortField.value = 'archiveTime'
    selectedSortDirection.value = 'desc'
  }
  datasetStore.setListState({
    sortField: selectedSortField.value,
    sortDirection: selectedSortDirection.value
  })
}

onMounted(async () => {
  enableInfiniteScroll()
  
  watch(() => datasetStore.datasetsLoaded, (loaded) => {
    if (loaded) {
      selectDatasetFromRoute()
    }
  }, { immediate: true })

  applySortFromRoute()

  if (datasetStore.datasetsLoaded && datasetStore.dataset && datasetStore.dataset.name === route.params.name) {
    if (items.value.length === 0) {
      getItems()
    } else if (datasetStore.listState.scrollPosition > 0) {
      setTimeout(() => {
        window.scrollTo(0, datasetStore.listState.scrollPosition)
      }, 100)
    }
  }
})

onUnmounted(() => {
  datasetStore.setListState({
    scrollPosition: window.pageYOffset || document.documentElement.scrollTop,
    searchString: searchString.value,
    datasetName: route.params.name
  })
  window.onscroll = null
  window.removeEventListener('keydown', handleKeyDown)
})

const selectDatasetFromRoute = async () => {
  const datasetName = route.params.name
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

watch(() => route.query.sort, () => {
  applySortFromRoute()
}, { immediate: true })

watch(() => route.query.search, (newSearch) => {
  if (newSearch) searchString.value = newSearch
  else searchString.value = ''
  reset()
  getItems()
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
