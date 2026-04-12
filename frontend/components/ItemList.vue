<template>
  <div>
    <v-row v-if="searched">
      <v-col
        v-for="item of items"
        :key="item.id"
        class="d-flex child-flex"
        cols="12"
        sm="4"
        md="3"
        lg="2"
      >
        <v-img
          :src="$carabassa.getItemThumbnailURL(item)"
          :lazy-src="$carabassa.getItemThumbnailURL(item)"
          :aspect-ratio="1"
          cover
          class="with-pointer grey lighten-2 rounded"
          :title="`${item.id} - ${item.archiveTime}`"
          @click="$emit('select', item)"
        >
          <div
            v-if="selectable"
            class="selection-overlay"
            @click.stop="$emit('toggle-select', item)"
          >
            <v-icon
              :color="isSelected(item) ? 'primary' : 'grey-darken-1'"
              size="20"
            >
              {{ isSelected(item) ? 'mdi-checkbox-marked' : 'mdi-checkbox-blank-outline' }}
            </v-icon>
          </div>

          <div
            class="favorite-overlay"
            :class="{ 'favorite-active': isFavorite(item) }"
            @click.stop="toggleFavoriteWrapper(item)"
          >
            <v-icon
              :color="isFavorite(item) ? 'amber' : 'white'"
              size="20"
            >
              {{ isFavorite(item) ? 'mdi-star' : 'mdi-star-outline' }}
            </v-icon>
          </div>

          <div class="actions-overlay">
            <v-tooltip text="Expand" location="top">
              <template #activator="{ props }">
                <v-btn
                  v-bind="props"
                  icon="mdi-arrow-expand"
                  density="compact"
                  variant="text"
                  color="white"
                  @click.stop="$emit('select', item)"
                />
              </template>
            </v-tooltip>
            <v-tooltip text="View detail" location="top">
              <template #activator="{ props }">
                <v-btn
                  v-bind="props"
                  icon="mdi-magnify"
                  density="compact"
                  variant="text"
                  color="white"
                  @click.stop="navigateDetailed(item)"
                />
              </template>
            </v-tooltip>
            <v-tooltip text="Download" location="top">
              <template #activator="{ props }">
                <v-btn
                  v-bind="props"
                  icon="mdi-download"
                  density="compact"
                  variant="text"
                  color="white"
                  :href="$carabassa.getItemContentURL(item)"
                  target="_blank"
                  @click.stop
                />
              </template>
            </v-tooltip>
            <v-tooltip text="Copy link" location="top">
              <template #activator="{ props }">
                <v-btn
                  v-bind="props"
                  icon="mdi-link"
                  density="compact"
                  variant="text"
                  color="white"
                  @click.stop="copyLink(item)"
                />
              </template>
            </v-tooltip>
            <v-tooltip text="Delete" location="top">
              <template #activator="{ props }">
                <v-btn
                  v-bind="props"
                  icon="mdi-delete"
                  density="compact"
                  variant="text"
                  color="white"
                  @click.stop="$emit('delete', item)"
                />
              </template>
            </v-tooltip>
          </div>

          <div 
            v-if="isVideo(item)"
            class="video-preview-overlay"
          >
            <v-icon
              size="36"
              color="white"
            >
              mdi-play-circle-outline
            </v-icon>
          </div>

          <template #placeholder>
            <div class="d-flex align-center justify-center fill-height">
              <v-progress-circular
                indeterminate
                color="grey-lighten-5"
              />
            </div>
          </template>
        </v-img>
      </v-col>
    </v-row>
    <v-row v-if="loading">
      <v-col class="text-center">
        <v-progress-circular
          size="50"
          width="5"
          indeterminate
          color="primary"
        />
      </v-col>
    </v-row>

    <v-snackbar
      v-model="snackbar"
      :timeout="2000"
      color="primary"
      location="top"
    >
      Link copied to clipboard
    </v-snackbar>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { isVideo } from '~/utils/itemUtils'

const { isFavorite, toggleFavorite, copyItemLink } = useItemActions()
const router = useRouter()
const route = useRoute()
const snackbar = ref(false)

const navigateDetailed = (item) => {
  const datasetName = item.datasetName || route.params.name
  router.push(`/dataset/${datasetName}/item/${item.id}`)
}

const copyLink = async (item) => {
  const ok = await copyItemLink({
    datasetName: route.params.name,
    itemId: item.id
  })
  if (ok) {
    snackbar.value = true
  }
}

const props = defineProps({
  items: {
    type: Array,
    required: true
  },
  searched: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  selectable: {
    type: Boolean,
    default: false
  },
  selectedItems: {
    type: Array,
    default: () => []
  }
})

const toggleFavoriteWrapper = async (item) => {
  await toggleFavorite(item)
  emit('toggle-favorite', item)
}

const isSelected = (item) => props.selectedItems.includes(item.id)

const emit = defineEmits(['select', 'toggle-select', 'delete', 'copy-link', 'toggle-favorite'])
</script>

<style scoped>
.with-pointer {
  cursor: pointer;
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
.selection-overlay {
  position: absolute;
  top: 6px;
  right: 6px;
  z-index: 5;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 3px;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  box-shadow: 0 0 2px rgba(0,0,0,0.2);
}
@media (hover: none) {
  .selection-overlay {
    background: rgba(255, 255, 255, 0.7);
    width: 28px;
    height: 28px;
  }
}
.selection-overlay:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: scale(1.1);
}
.favorite-overlay {
  position: absolute;
  top: 6px;
  left: 6px;
  z-index: 5;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 3px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 0 2px rgba(0,0,0,0.2);
  opacity: 0;
}
.with-pointer:hover .favorite-overlay,
.favorite-active {
  opacity: 1;
  background: rgba(0, 0, 0, 0.4);
}
@media (hover: none) {
  .favorite-overlay {
    opacity: 1;
    background: rgba(0, 0, 0, 0.3);
    width: 32px;
    height: 32px;
  }
}
.favorite-overlay:hover {
  background: rgba(0, 0, 0, 0.6);
  transform: scale(1.15);
}
.actions-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 48px;
  background: linear-gradient(to top, rgba(0,0,0,0.8) 0%, rgba(0,0,0,0) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transform: translateY(100%);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 10;
  padding: 0 4px;
}
.with-pointer:hover .actions-overlay {
  transform: translateY(0);
}
@media (hover: none) {
  .actions-overlay {
    transform: translateY(0);
    height: 42px; /* Slightly more compact on mobile */
    background: linear-gradient(to top, rgba(0,0,0,0.6) 0%, rgba(0,0,0,0) 100%);
  }
  .actions-overlay :deep(.v-btn) {
    width: 32px;
    height: 32px;
  }
}
.actions-overlay :deep(.v-btn) {
  background: rgba(255,255,255,0.1);
  backdrop-filter: blur(4px);
}
.actions-overlay :deep(.v-btn:hover) {
  background: rgba(var(--v-theme-primary), 0.9);
  color: white !important;
}
</style>
