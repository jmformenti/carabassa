<template>
  <div>
    <v-container>
      <h2 class="text-h5 text-orange-darken-4 mb-4">
        Ask
      </h2>

      <v-row v-if="!hasDataset">
        <v-col>
          <v-alert
            type="info"
            variant="tonal"
          >
            Select a dataset first.
          </v-alert>
        </v-col>
      </v-row>

      <div v-else>
        <v-row>
          <v-col>
            <v-textarea
              v-model="question"
              label="Ask in your own words"
              placeholder="e.g. Photos of Maria in Barcelona last summer"
              rows="2"
              auto-grow
              variant="outlined"
              color="grey-darken-2"
              :disabled="loading"
              @keydown.enter.exact.prevent="submit"
            />
          </v-col>
        </v-row>

        <v-row class="mt-n4">
          <v-col class="d-flex">
            <v-btn
              color="orange-darken-2"
              :loading="loading"
              :disabled="!question.trim() || loading"
              prepend-icon="mdi-comment-question-outline"
              @click="submit"
            >
              Ask
            </v-btn>
          </v-col>
        </v-row>

        <v-row v-if="askError">
          <v-col>
            <v-alert
              type="error"
              variant="tonal"
              closable
              @click:close="askError = null"
            >
              {{ askError }}
            </v-alert>
          </v-col>
        </v-row>

        <v-row v-if="summary">
          <v-col>
            <v-card variant="tonal" color="orange-darken-2">
              <v-card-text>
                <div class="text-body-1">
                  {{ summary }}
                </div>
                <div
                  v-if="search"
                  class="text-caption text-grey-darken-1 mt-2"
                >
                  search: <code>{{ search }}</code>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <v-row v-if="searched">
          <v-col class="d-flex align-center">
            <div class="text-body-2">
              {{ totalItems }} found
            </div>
          </v-col>
        </v-row>

        <ItemList
          :items="items"
          :searched="searched"
          :loading="loading || paginating"
          @select="expandItem"
          @toggle-favorite="onToggleFavorite"
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
    </v-container>

    <ItemDetailOverlay
      v-model="overlay"
      :item="selectedItem"
      :has-previous="hasPrevious"
      :has-next="hasNext"
      @previous="previousImage"
      @next="nextImage"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import ItemList from '~/components/ItemList.vue'
import ItemDetailOverlay from '~/components/ItemDetailOverlay.vue'
import { useDatasetStore } from '~/stores/dataset'

const datasetStore = useDatasetStore()
const { $carabassa } = useNuxtApp()

const question = ref('')
const summary = ref('')
const search = ref('')
const items = ref([])
const totalItems = ref(0)
const totalPages = ref(0)
const currentPage = ref(0)
const pageSize = ref(24)
const searched = ref(false)
const loading = ref(false)
const paginating = ref(false)
const askError = ref(null)
const overlay = ref(false)
const selectedItem = ref(null)

const hasDataset = computed(() => !!datasetStore.dataset)

const leftItems = computed(() => {
  const left = totalItems.value - (currentPage.value + 1) * pageSize.value
  return left > 0 ? left : 0
})

const selectedIndex = computed(() => items.value.indexOf(selectedItem.value))
const hasPrevious = computed(() => selectedIndex.value > 0)
const hasNext = computed(() => selectedIndex.value < items.value.length - 1 || currentPage.value + 1 < totalPages.value)

const submit = async () => {
  if (!question.value.trim() || loading.value || !hasDataset.value) return
  loading.value = true
  askError.value = null
  summary.value = ''
  search.value = ''
  items.value = []
  totalItems.value = 0
  totalPages.value = 0
  currentPage.value = 0
  searched.value = false
  try {
    const data = await $carabassa.askItems(question.value.trim())
    summary.value = data.summary || ''
    search.value = data.search || ''
    items.value = data.items || []
    totalItems.value = data.totalItems || 0
    totalPages.value = items.value.length < totalItems.value ? Math.ceil(totalItems.value / pageSize.value) : 1
    searched.value = true
  } catch (err) {
    const status = err?.response?.status ?? err?.status
    const message = err?.data?.message ?? err?.response?._data?.message
    if (status === 503) {
      askError.value = 'Natural language search is not configured. Set CARABASSA_LLM_URL and CARABASSA_LLM_MODEL on the backend to enable it.'
    } else if (message) {
      askError.value = message
    } else {
      askError.value = 'The natural language search failed.'
    }
    console.error(err)
  } finally {
    loading.value = false
  }
}

const loadNextPage = async () => {
  if (paginating.value) return
  if (!search.value) return
  if (currentPage.value + 1 >= totalPages.value) return
  paginating.value = true
  try {
    const next = currentPage.value + 1
    const data = await $carabassa.getItems(next, pageSize.value, search.value, 'archiveTime,desc')
    if (data._embedded) {
      const incoming = data._embedded.itemRepresentationList || []
      const seen = new Set(items.value.map(i => i.id))
      for (const item of incoming) {
        if (!seen.has(item.id)) items.value.push(item)
      }
      currentPage.value = next
      totalPages.value = data.page.totalPages
      totalItems.value = data.page.totalElements
    }
  } catch (err) {
    console.error('Pagination error', err)
  } finally {
    paginating.value = false
  }
}

const expandItem = (item) => {
  selectedItem.value = item
  overlay.value = true
}

watch(overlay, (open) => {
  if (open) {
    window.addEventListener('keydown', handleKeyDown)
  } else {
    window.removeEventListener('keydown', handleKeyDown)
  }
})

const previousImage = () => {
  if (selectedIndex.value > 0) {
    selectedItem.value = items.value[selectedIndex.value - 1]
  }
}

const nextImage = async () => {
  if (selectedIndex.value < items.value.length - 1) {
    selectedItem.value = items.value[selectedIndex.value + 1]
  } else if (currentPage.value + 1 < totalPages.value) {
    await loadNextPage()
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

const onToggleFavorite = () => {
  // Favorite toggling is handled by ItemList; nothing to do here.
}

const handleScroll = () => {
  const bottom = Math.ceil(document.documentElement.scrollTop + window.innerHeight) >= document.documentElement.offsetHeight
  if (bottom && !loading.value && !paginating.value && currentPage.value + 1 < totalPages.value) {
    loadNextPage()
  }
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
:deep(.v-field--focused) .v-field__input::placeholder {
  color: rgb(66, 66, 66);
}
</style>
