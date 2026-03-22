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
          v-if="!isVideo(item)"
          :src="$carabassa.getItemThumbnailURL(item.id)"
          :lazy-src="$carabassa.getItemThumbnailURL(item.id)"
          :aspect-ratio="1"
          cover
          class="with-pointer grey lighten-2 rounded"
          :title="`${item.id} - ${item.archiveTime}`"
          @click="$emit('select', item)"
        >
          <div
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
          @click="$emit('select', item)"
        >
          <video
            class="video-preview"
            :src="$carabassa.getItemContentURL(item.id)"
            preload="metadata"
            muted
            playsinline
          />
          <div
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
            class="video-preview-overlay"
          >
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
  </div>
</template>

<script setup>
import { isVideo } from '~/utils/itemUtils'

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

const isSelected = (item) => props.selectedItems.includes(item.id)

defineEmits(['select', 'toggle-select'])
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
  left: 6px;
  z-index: 5;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 3px;
  width: 22px;  /* Icon 20px + 2px */
  height: 22px; /* Icon 20px + 2px */
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  box-shadow: 0 0 2px rgba(0,0,0,0.2);
}
.selection-overlay:hover {
  background: rgba(255, 255, 255, 1);
  transform: scale(1.1);
}
</style>
