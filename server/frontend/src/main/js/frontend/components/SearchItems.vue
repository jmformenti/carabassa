<template>
  <div>
    <v-container>
      <v-row v-if="showNoDatasetMessage">
        <v-col>
          <v-alert
            type="info"
            variant="tonal"
          >
            No dataset created.
          </v-alert>
        </v-col>
      </v-row>
      <v-row
        v-if="hasDataset"
        justify="center" 
        align="center"
      >
        <v-col
          cols="12"
          sm="8"
          md="6"
        >
          <v-text-field
            v-model="searchString"
            label="Search"
            variant="underlined"
            clear-icon="mdi-close-circle"
            clearable
            @keyup.enter="search"
          >
            <template #prepend>
              <v-tooltip location="bottom">
                <template #activator="{ props }">
                  <v-icon
                    small
                    class="with-pointer no-opacity"
                    color="orange-darken-2"
                    v-bind="props"
                  >
                    mdi-help-circle
                  </v-icon>
                </template>
                <span>
                  Cheatsheet for searching:<br>
                  <b>from:</b> YYYY-MM-DD<br>
                  <b>to:</b> YYYY-MM-DD<br>
                  <b>on:</b> YYYY-MM-DD
                </span>
              </v-tooltip>
            </template>

            <template #append>
              <v-icon
                class="no-opacity"
                color="orange-darken-2"
                @click="search()"
              >
                mdi-send
              </v-icon>
            </template>
          </v-text-field>
        </v-col>
        <v-col
          cols="12"
          sm="4"
          md="3"
        >
          <v-select
            v-model="selectedSortField"
            :items="sortOptions"
            item-title="text"
            item-value="value"
            label="Order by"
            variant="underlined"
            @update:modelValue="search"
          >
            <template #append>
              <v-btn
                :icon="selectedSortDirection === 'asc' ? 'mdi-sort-ascending' : 'mdi-sort-descending'"
                variant="text"
                density="compact"
                color="orange-darken-2"
                class="mt-1"
                @click="toggleSortDirection"
              />
            </template>
          </v-select>
        </v-col>
      </v-row>
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
      <v-row v-if="searched">
        <v-col
          v-for="item of items"
          :key="item.id"
          class="d-flex child-flex"
          cols="2"
        >
          <v-img
            v-if="!isVideo(item)"
            :src="$carabassa.getItemThumbnailURL(item.id)"
            :lazy-src="$carabassa.getItemThumbnailURL(item.id)"
            :aspect-ratio="1"
            cover
            class="with-pointer grey lighten-2"
            :title="`${item.id} - ${item.archiveTime}`"
            @click="expandItem(item)"
          >
            <template #placeholder>
              <div class="d-flex align-center justify-center fill-height">
                <v-progress-circular
                  indeterminate
                  color="grey-lighten-5"
                />
              </div>
            </template>
          </v-img>
          <div
            v-else
            class="media-preview with-pointer"
            :title="`${item.id} - ${item.archiveTime}`"
            @click="expandItem(item)"
          >
            <video
              class="video-preview"
              :src="$carabassa.getItemContentURL(item.id)"
              preload="metadata"
              muted
              playsinline
            />
            <div class="video-preview-overlay">
              <v-icon
                size="36"
                color="white"
              >
                mdi-play-circle-outline
              </v-icon>
            </div>
          </div>
        </v-col>
      </v-row>
      <v-row>
        <v-col>
          <div
            v-if="leftItems > 0"
            class="text-body-2 text-center"
          >
            {{ leftItems }} items left
          </div>
        </v-col>
      </v-row>
    </v-container>
    <v-overlay
      v-model="overlay"
      class="align-center justify-center"
    >
      <v-card
        v-if="selectedItem"
        class="navigation-card"
      >
        <v-btn
          v-if="hasPrevious"
          icon="mdi-chevron-left"
          variant="elevated"
          class="nav-btn prev-btn"
          size="large"
          @click.stop="previousImage"
        />
        <v-btn
          v-if="hasNext || currentPage + 1 < totalPages"
          icon="mdi-chevron-right"
          variant="elevated"
          class="nav-btn next-btn"
          size="large"
          @click.stop="nextImage"
        />
        <v-card-item>
          <v-card-title>{{ selectedItem.filename }}</v-card-title>
          <v-card-subtitle>{{ new Date(selectedItem.archiveTime).toLocaleDateString(undefined, dateFormatOptions) }}</v-card-subtitle>
          <template #append>
            <v-btn
              icon="mdi-close"
              variant="text"
              size="small"
              @click="overlay = false"
            />
          </template>
        </v-card-item>
        <v-card-text>
          <video
            v-if="isVideo(selectedItem)"
            class="overlay-video"
            :src="$carabassa.getItemContentURL(selectedItem.id)"
            controls
            autoplay
          />
          <v-img
            v-else
            width="500"
            max-height="500"
            :src="$carabassa.getItemContentURL(selectedItem.id)"
            @click="overlay = false" 
          />
        </v-card-text>
        <v-card-actions>
          <v-tooltip text="Enter detail" location="top">
            <template #activator="{ props }">
              <v-btn
                v-bind="props"
                icon="mdi-magnify"
                color="orange-darken-2"
                :to="`/item/${selectedItem.id}`"
              />
            </template>
          </v-tooltip>
          <v-tooltip text="Download" location="top">
            <template #activator="{ props }">
              <v-btn
                v-bind="props"
                icon="mdi-download"
                color="orange-darken-2"
                :href="$carabassa.getItemContentURL(selectedItem.id)"
              />
            </template>
          </v-tooltip>
          <v-tooltip text="Delete" location="top">
            <template #activator="{ props }">
              <v-btn
                v-bind="props"
                icon="mdi-delete"
                color="orange-darken-2"
                @click="confirmDelete"
              />
            </template>
          </v-tooltip>
        </v-card-actions>
      </v-card>
    </v-overlay>
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
    <div
      v-if="waitingResults"
      class="text-center"
    >
      <v-progress-circular
        size="50"
        width="5"
        indeterminate
        color="primary"
      />
    </div>
  </div>
</template>

<script>
export default {
  setup () {
    const datasetStore = useDatasetStore()
    return { datasetStore }
  },

  data () {
    return {
      apiBaseURL: null,
      searchString: '',
      totalItems: 0,
      items: [],
      currentPage: 0,
      pageSize: 24,
      totalPages: 0,
      searched: false,
      overlay: false,
      selectedItem: null,
      deleteDialog: false,
      dateFormatOptions: {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      },
      autoSearchOnLoad: false,
      waitingResults: false,
      selectedSortField: 'archiveTime',
      selectedSortDirection: 'desc',
      sortOptions: [
        { text: 'date', value: 'archiveTime' },
        { text: 'size', value: 'size' },
        { text: 'duplicated', value: 'duplicated.group' }
      ]
    }
  },

  computed: {
    leftItems () {
      const leftItems = this.totalItems - this.currentPage * this.pageSize
      if (leftItems > this.pageSize) {
        return leftItems
      } else {
        return 0
      }
    },

    selectedDataset () {
      return this.datasetStore.dataset
    },

    hasDataset () {
      return Boolean(this.datasetStore.dataset && this.datasetStore.dataset.id)
    },

    showNoDatasetMessage () {
      return this.datasetStore.datasetsLoaded && !this.hasDataset
    },

    selectedIndex () {
      return this.items.indexOf(this.selectedItem)
    },

    hasPrevious () {
      return this.selectedIndex > 0
    },

    hasNext () {
      return this.selectedIndex < this.items.length - 1
    },
    
    combinedSort () {
      return `${this.selectedSortField},${this.selectedSortDirection}`
    }
  },

  watch: {
    selectedDataset () {
      this.reset()
      if (this.autoSearchOnLoad && this.hasDataset) {
        this.autoSearchOnLoad = false
        this.getItems()
      }
    },
    overlay (val) {
      if (!val) {
        window.removeEventListener('keydown', this.handleKeyDown)
      }
    }
  },

  mounted () {
    this.enableInfiniteScroll()
    const searchStringByQuery = this.$route.query.search
    const sortByQuery = this.$route.query.sort
    if (searchStringByQuery || sortByQuery) {
      if (searchStringByQuery) {
        this.searchString = searchStringByQuery
      }
      if (sortByQuery) {
        const [field, direction] = sortByQuery.split(',')
        this.selectedSortField = field
        this.selectedSortDirection = direction || 'desc'
      }
      this.autoSearchOnLoad = true
      if (this.hasDataset) {
        this.autoSearchOnLoad = false
        this.getItems()
      }
    }
  },

  methods: {
    async getItems () {
      if (!this.hasDataset) {
        this.waitingResults = false
        return
      }
      this.waitingResults = true
      await this.$carabassa.getItems(this.currentPage, this.pageSize, this.searchString, this.combinedSort)
      .then((data) => {
        let items = []
        if (data._embedded) {
          this.totalItems = data.page.totalElements
          this.totalPages = data.page.totalPages
          items = data._embedded.itemRepresentationList
        }
        this.searched = true
        this.items.push(...items)
      })
      .catch((err) => {
        this.$notification.alert(err)
      })
      this.waitingResults = false
    },

    reset () {
      this.currentPage = 0
      this.totalItems = 0
      this.totalPages = 0
      this.items = []
      this.searched = false
    },

    search () {
      if (!this.hasDataset) {
        return
      }
      const query = { ...this.$route.query }
      if (this.searchString) {
        query.search = this.searchString
      } else {
        delete query.search
      }
      if (this.combinedSort) {
        query.sort = this.combinedSort
      } else {
        delete query.sort
      }
      this.$router.push({ query })
      this.reset()
      this.getItems()
    },

    toggleSortDirection () {
      this.selectedSortDirection = this.selectedSortDirection === 'asc' ? 'desc' : 'asc'
      this.search()
    },

    enableInfiniteScroll () {
      window.onscroll = () => {
        const bottomOfWindow = document.documentElement.scrollTop + window.innerHeight === document.documentElement.offsetHeight
        if (bottomOfWindow) {
          this.currentPage++
          if (this.currentPage < this.totalPages) {
            this.getItems()
          }
        }
      }
    },

    isVideo (item) {
      return item && item.type && item.type.toLowerCase() === 'video'
    },

    expandItem (item) {
      this.overlay = true
      this.selectedItem = item
      window.addEventListener('keydown', this.handleKeyDown)
    },

    previousImage () {
      if (this.hasPrevious) {
        this.selectedItem = this.items[this.selectedIndex - 1]
      }
    },

    nextImage () {
      if (this.hasNext) {
        this.selectedItem = this.items[this.selectedIndex + 1]
      } else if (this.currentPage + 1 < this.totalPages) {
        this.currentPage++
        this.getItems().then(() => {
          if (this.items.length > this.selectedIndex + 1) {
            this.selectedItem = this.items[this.selectedIndex + 1]
          }
        })
      }
    },

    handleKeyDown (e) {
      if (!this.overlay) return
      if (e.key === 'ArrowLeft') this.previousImage()
      if (e.key === 'ArrowRight') this.nextImage()
      if (e.key === 'Escape') this.overlay = false
    },

    confirmDelete () {
      this.deleteDialog = true
    },

    async deleteItem () {
      try {
        const itemId = this.selectedItem.id
        await this.$carabassa.deleteItem(itemId)
        this.deleteDialog = false
        this.overlay = false
        this.selectedItem = null
        this.items = this.items.filter(i => i.id !== itemId)
        this.totalItems--
      } catch (err) {
        this.deleteDialog = false
        this.$notification.alert(err)
      }
    }
  }
}
</script>

<style scoped>
.with-pointer {
  cursor: pointer;
}
.no-opacity {
  opacity: 1;
}
.media-preview {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  overflow: hidden;
  border-radius: 4px;
  background: #111;
}
.video-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.video-preview-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.2);
}
.overlay-video {
  width: 100%;
  max-width: 900px;
  max-height: 70vh;
  display: block;
}
.navigation-card {
  overflow: visible !important;
}
.nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  background-color: rgba(var(--v-theme-surface), 0.5) !important;
  border-radius: 50%;
  backdrop-filter: blur(4px);
  transition: all 0.3s ease;
}
.nav-btn:hover {
  background-color: rgba(var(--v-theme-surface), 0.8) !important;
  transform: translateY(-50%) scale(1.1);
}
.prev-btn {
  left: -80px;
}
.next-btn {
  right: -80px;
}
@media (max-width: 800px) {
  .prev-btn {
    left: 8px;
  }
  .next-btn {
    right: 8px;
  }
  .nav-btn {
    background-color: rgba(var(--v-theme-surface), 0.8) !important;
  }
}
</style>
