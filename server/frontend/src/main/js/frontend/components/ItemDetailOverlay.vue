<template>
  <v-overlay
    :model-value="modelValue"
    class="align-center justify-center"
    @update:modelValue="$emit('update:modelValue', $event)"
  >
    <v-card
      v-if="item"
      class="navigation-card"
    >
      <v-btn
        v-if="hasPrevious"
        icon="mdi-chevron-left"
        variant="elevated"
        class="nav-btn prev-btn"
        size="large"
        @click.stop="$emit('previous')"
      />
      <v-btn
        v-if="hasNext"
        icon="mdi-chevron-right"
        variant="elevated"
        class="nav-btn next-btn"
        size="large"
        @click.stop="$emit('next')"
      />
      <v-card-item>
        <v-card-title>{{ item.filename }}</v-card-title>
        <v-card-subtitle>{{ formatDate(item.archiveTime) }}</v-card-subtitle>
        <template #append>
          <v-btn
            icon="mdi-close"
            variant="text"
            size="small"
            @click="$emit('update:modelValue', false)"
          />
        </template>
      </v-card-item>
      <v-card-text>
        <video
          v-if="isVideo(item)"
          class="overlay-video"
          :src="$carabassa.getItemContentURL(item.id)"
          controls
          autoplay
        />
        <v-img
          v-else
          width="500"
          max-height="500"
          :src="$carabassa.getItemContentURL(item.id)"
          @click="$emit('update:modelValue', false)" 
        />
      </v-card-text>
      <v-card-actions>
        <v-tooltip text="Enter detail" location="top">
          <template #activator="{ props }">
            <v-btn
              v-bind="props"
              icon="mdi-magnify"
              color="orange-darken-2"
              @click="navigateDetail"
            />
          </template>
        </v-tooltip>
        <v-tooltip text="Download" location="top">
          <template #activator="{ props }">
            <v-btn
              v-bind="props"
              icon="mdi-download"
              color="orange-darken-2"
              :href="$carabassa.getItemContentURL(item.id)"
            />
          </template>
        </v-tooltip>
        <v-tooltip text="Delete" location="top">
          <template #activator="{ props }">
            <v-btn
              v-bind="props"
              icon="mdi-delete"
              color="orange-darken-2"
              @click="$emit('delete', item)"
            />
          </template>
        </v-tooltip>
      </v-card-actions>
    </v-card>
  </v-overlay>
</template>

<script setup>
import { isVideo, formatDate } from '~/utils/itemUtils'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  item: {
    type: Object,
    default: null
  },
  hasPrevious: {
    type: Boolean,
    default: false
  },
  hasNext: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'previous', 'next', 'delete'])

const router = useRouter()

const navigateDetail = () => {
  emit('update:modelValue', false)
  router.push(`/item/${props.item.id}`)
}
</script>

<style scoped>
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
.overlay-video {
  width: 100%;
  max-width: 900px;
  max-height: 70vh;
  display: block;
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
