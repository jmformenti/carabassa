<template>
  <v-container v-if="item">
    <v-row>
      <v-col>
        <div class="d-flex align-center mb-4">
          <v-btn icon="mdi-arrow-left" variant="text" class="mr-4" @click="$router.back()" />
          <h2 class="text-h4">{{ item.filename }}</h2>
        </div>
      </v-col>
    </v-row>

    <v-row>
      <!-- Left Column: Media Preview -->
      <v-col cols="12" md="8">
        <v-card class="media-container pa-4" flat color="transparent">
          <video
            v-if="isVideo"
            class="media-content"
            :src="$carabassa.getItemContentURL(item.id)"
            controls
            autoplay
          />
          <v-img
            v-else
            class="media-content"
            :src="$carabassa.getItemContentURL(item.id)"
          />
        </v-card>
      </v-col>

      <!-- Right Column: Metadata and Tags -->
      <v-col cols="12" md="4">
        <!-- Basic Data -->
        <v-card class="mb-4" flat color="grey-lighten-4">
          <v-card-title>Information</v-card-title>
          <v-list lines="one" bg-color="transparent">
            <v-list-item>
              <template #prepend>
                <v-icon icon="mdi-calendar" class="mr-3" />
              </template>
              <v-list-item-title>Archive Date</v-list-item-title>
              <v-list-item-subtitle>{{ formattedDate }}</v-list-item-subtitle>
            </v-list-item>
            <v-list-item>
              <template #prepend>
                <v-icon icon="mdi-file" class="mr-3" />
              </template>
              <v-list-item-title>Type</v-list-item-title>
              <v-list-item-subtitle>{{ item.type }}</v-list-item-subtitle>
            </v-list-item>
            <v-list-item>
              <template #prepend>
                <v-icon icon="mdi-information-outline" class="mr-3" />
              </template>
              <v-list-item-title>Format</v-list-item-title>
              <v-list-item-subtitle>{{ item.format }}</v-list-item-subtitle>
            </v-list-item>
            <v-list-item>
              <template #prepend>
                <v-icon icon="mdi-harddisk" class="mr-3" />
              </template>
              <v-list-item-title>Size</v-list-item-title>
              <v-list-item-subtitle>{{ formattedSize }}</v-list-item-subtitle>
            </v-list-item>
          </v-list>
        </v-card>

        <!-- Tags -->
        <v-card flat color="grey-lighten-4">
          <v-card-title>Tags</v-card-title>
          <v-card-text>
            <div v-if="filteredTags.length > 0" class="d-flex flex-wrap gap-2 mt-2">
              <v-chip
                v-for="tag in filteredTags"
                :key="tag.id"
                color="primary"
                variant="flat"
                size="small"
                class="mr-2 mb-2"
              >
                {{ tag.name }}: {{ tag.value }}
              </v-chip>
            </div>
            <div v-else class="text-body-2 text-medium-emphasis mt-2">
              No tags for this item.
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>

  <div v-else class="d-flex justify-center align-center fill-height" style="min-height: 50vh;">
    <v-progress-circular indeterminate color="primary" size="64" />
  </div>
</template>

<script>
export default {
  setup() {
    const datasetStore = useDatasetStore()
    return { datasetStore }
  },
  data() {
    return {
      item: null,
      error: null
    }
  },
  computed: {
    isVideo() {
      return this.item && this.item.type && this.item.type.toLowerCase() === 'video'
    },
    formattedDate() {
      if (!this.item || !this.item.archiveTime) return ''
      return new Date(this.item.archiveTime).toLocaleDateString(undefined, {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      })
    },
    formattedSize() {
      if (!this.item || this.item.size == null) return ''
      const bytes = this.item.size
      if (bytes === 0) return '0 Bytes'
      const k = 1024
      const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },
    filteredTags() {
      if (!this.item || !this.item.tags) return []
      return this.item.tags.filter(tag => !tag.name.startsWith('meta.'))
    }
  },
  watch: {
    'datasetStore.dataset': {
      handler(newVal) {
        if (newVal && !this.item) {
          this.fetchItem()
        }
      },
      immediate: true
    }
  },
  methods: {
    async fetchItem() {
      try {
        const itemId = this.$route.params.id
        this.item = await this.$carabassa.getItem(itemId)
      } catch (err) {
        console.error('Error fetching item details:', err)
        this.$notification.alert('Failed to load item details: ' + (err.message || 'Unknown error'))
      }
    }
  }
}
</script>

<style scoped>
.media-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}
.media-content {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}
</style>
