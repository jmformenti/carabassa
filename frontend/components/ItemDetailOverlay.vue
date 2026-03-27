<template>
  <v-overlay
    :model-value="modelValue"
    class="align-center justify-center"
    @update:modelValue="$emit('update:modelValue', $event)"
  >
    <v-card
      v-if="item"
      class="navigation-card overlay-card"
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
      <v-card-text
        class="overlay-content"
        :class="{ 'overlay-content-image': !isVideo(item) }"
        @touchstart.passive="onTouchStart"
        @touchmove.passive="onTouchMove"
        @touchend="onTouchEnd"
        @touchcancel="onTouchCancel"
      >
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
          max-height="65vh"
          :src="$carabassa.getItemContentURL(item.id)"
          class="overlay-image"
          @click="$emit('update:modelValue', false)" 
        />
      </v-card-text>
      <v-card-actions class="overlay-actions">
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
        <ItemActions
          :item="item"
          @delete="$emit('delete', item)"
          @copy-link="copyLink"
        />
      </v-card-actions>
    </v-card>

    <v-snackbar
      v-model="snackbar"
      :timeout="2000"
      color="primary"
      location="top"
    >
      Link copied to clipboard
    </v-snackbar>
  </v-overlay>
</template>

<script setup>
import { isVideo, formatDate } from '~/utils/itemUtils'
import ItemActions from '~/components/ItemActions.vue'

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
const route = useRoute()
const { $notification } = useNuxtApp()
const snackbar = ref(false)
const { copyItemLink } = useItemActions()
const touchState = ref({
  active: false,
  startX: 0,
  startY: 0,
  lastX: 0,
  lastY: 0
})

const swipeThreshold = 50
const swipeAngleRatio = 1.5

const onTouchStart = (event) => {
  if (!event.touches || event.touches.length !== 1) return
  const touch = event.touches[0]
  touchState.value = {
    active: true,
    startX: touch.clientX,
    startY: touch.clientY,
    lastX: touch.clientX,
    lastY: touch.clientY
  }
}

const onTouchMove = (event) => {
  if (!touchState.value.active || !event.touches || event.touches.length !== 1) return
  const touch = event.touches[0]
  touchState.value.lastX = touch.clientX
  touchState.value.lastY = touch.clientY
}

const onTouchEnd = () => {
  if (!touchState.value.active) return
  const { startX, startY, lastX, lastY } = touchState.value
  touchState.value.active = false

  const deltaX = lastX - startX
  const deltaY = lastY - startY
  if (Math.abs(deltaX) < swipeThreshold) return
  if (Math.abs(deltaX) < Math.abs(deltaY) * swipeAngleRatio) return

  if (deltaX > 0 && props.hasPrevious) {
    emit('previous')
  } else if (deltaX < 0 && props.hasNext) {
    emit('next')
  }
}

const onTouchCancel = () => {
  touchState.value.active = false
}

const navigateDetail = () => {
  emit('update:modelValue', false)
  const datasetName = route.params.name
  if (datasetName) {
    router.push(`/dataset/${datasetName}/item/${props.item.id}`)
    return
  }
  $notification.alert('No dataset in context. Open the item from a dataset page.')
}

const copyLink = async () => {
  const ok = await copyItemLink({
    datasetName: route.params.name,
    itemId: props.item?.id,
    fallbackUrl: window.location.href
  })
  if (ok) {
    snackbar.value = true
  }
}
</script>

<style scoped>
.navigation-card {
  overflow: visible !important;
}
.overlay-card {
  max-width: 90vw;
  max-height: 90vh;
  width: 100%;
  display: flex;
  flex-direction: column;
}
.overlay-content {
  overflow: auto;
  min-height: 0;
  flex: 1 1 auto;
}
.overlay-content-image {
  padding: 0 !important;
  background-color: transparent !important;
}
.overlay-image {
  width: 100%;
  max-width: 90vw;
  display: block;
  box-shadow: none !important;
  background-color: transparent !important;
}
.overlay-actions {
  border-top: none !important;
  box-shadow: none !important;
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
