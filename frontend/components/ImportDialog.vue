<template>
  <v-dialog
    v-model="model"
    max-width="600"
    :persistent="isPersistent"
    @update:model-value="onModelUpdate"
  >
    <v-card>
      <v-card-title class="d-flex align-center">
        <v-icon class="mr-2" color="orange-darken-2">mdi-upload</v-icon>
        Upload Items
      </v-card-title>

      <v-card-text>
        <!-- File source selector -->
        <div v-if="!started">
          <v-radio-group
            v-model="mode"
            inline
            class="mb-4"
          >
            <v-radio label="Files" value="files" />
            <v-radio label="Directory" value="directory" />
          </v-radio-group>

          <v-btn
            variant="tonal"
            color="orange-darken-2"
            prepend-icon="mdi-folder-open"
            @click="openPicker"
          >
            {{ mode === 'directory' ? 'Choose directory' : 'Choose files' }}
          </v-btn>

          <!-- Hidden file inputs -->
          <input
            ref="fileInput"
            type="file"
            multiple
            accept="image/*,video/*"
            style="display:none"
            @change="onFilesSelected"
          />
          <input
            ref="dirInput"
            type="file"
            webkitdirectory
            style="display:none"
            @change="onFilesSelected"
          />

          <div v-if="pendingFiles.length > 0" class="mt-4 text-body-2 text-medium-emphasis">
            <v-icon size="small">mdi-file-multiple</v-icon>
            {{ pendingFiles.length }} file{{ pendingFiles.length !== 1 ? 's' : '' }} selected
          </div>
        </div>

        <!-- Progress -->
        <div v-if="started">
          <div class="d-flex justify-space-between text-body-2 mb-1">
            <span>{{ done }} / {{ total }}</span>
            <span>{{ Math.round(progress) }}%</span>
          </div>
          <v-progress-linear
            :model-value="progress"
            color="orange-darken-2"
            height="8"
            rounded
            class="mb-4"
          />
        </div>

        <!-- Results log -->
        <div
          v-if="results.length > 0"
          class="results-log mt-2"
        >
          <v-list
            density="compact"
            lines="one"
            max-height="240"
            style="overflow-y:auto"
          >
            <v-list-item
              v-for="(r, i) in results"
              :key="i"
              :prepend-icon="r.icon"
              :base-color="r.color"
            >
              <v-list-item-title class="text-body-2">
                <div class="font-weight-medium text-truncate">{{ r.filename }}</div>
                <div
                  v-if="r.message"
                  class="text-caption text-medium-emphasis message-wrap"
                >
                  {{ r.message }}
                </div>
              </v-list-item-title>
            </v-list-item>
          </v-list>
        </div>

        <!-- Summary after completion -->
        <div v-if="finished" class="mt-4 d-flex gap-4">
          <v-chip color="success" size="small" prepend-icon="mdi-check">
            {{ successCount }} uploaded
          </v-chip>
          <v-chip v-if="warnCount > 0" color="warning" size="small" prepend-icon="mdi-alert">
            {{ warnCount }} duplicates
          </v-chip>
          <v-chip v-if="errorCount > 0" color="error" size="small" prepend-icon="mdi-close">
            {{ errorCount }} errors
          </v-chip>
        </div>
      </v-card-text>

      <v-card-actions>
        <v-spacer />
        <v-btn
          v-if="!started || finished"
          color="orange-darken-2"
          variant="text"
          @click="close"
        >
          {{ finished ? 'Close' : 'Cancel' }}
        </v-btn>
        <v-btn
          v-if="!started && pendingFiles.length > 0"
          color="orange-darken-2"
          variant="tonal"
          prepend-icon="mdi-upload"
          @click="startImport"
        >
          Upload
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import SparkMD5 from 'spark-md5'

const ACCEPTED_TYPES = /^(image|video)\//

const model = defineModel({ type: Boolean, default: false })
const emit = defineEmits(['imported'])

const { $carabassa } = useNuxtApp()

const mode = ref('files')
const fileInput = ref(null)
const dirInput = ref(null)

const pendingFiles = ref([])
const results = ref([])
const started = ref(false)
const finished = ref(false)
const done = ref(0)
const total = ref(0)

const progress = computed(() => total.value > 0 ? (done.value / total.value) * 100 : 0)
const successCount = computed(() => results.value.filter(r => r.status === 'success').length)
const warnCount = computed(() => results.value.filter(r => r.status === 'warn').length)
const errorCount = computed(() => results.value.filter(r => r.status === 'error').length)

const isPersistent = computed(() => started.value && !finished.value)

const openPicker = () => {
  if (mode.value === 'directory') {
    dirInput.value.value = ''
    dirInput.value.click()
  } else {
    fileInput.value.value = ''
    fileInput.value.click()
  }
}

const onFilesSelected = (event) => {
  const all = Array.from(event.target.files)
  // Filter to images and videos only (directories might contain other things)
  pendingFiles.value = all
    .filter(f => ACCEPTED_TYPES.test(f.type))
    .sort((a, b) => a.name.localeCompare(b.name))
}

const startImport = async () => {
  if (pendingFiles.value.length === 0) return

  started.value = true
  finished.value = false
  results.value = []
  done.value = 0
  total.value = pendingFiles.value.length

  let anySuccess = false

  for (const file of pendingFiles.value) {
    try {
      const hash = SparkMD5.ArrayBuffer.hash(await file.arrayBuffer())
      const exists = await $carabassa.itemExists(hash)
      if (exists) {
        results.value.push({
          filename: file.name,
          status: 'warn',
          icon: 'mdi-alert-circle',
          color: 'warning',
          message: null
        })
        done.value++
        continue
      }
      await $carabassa.addItem(file)
      results.value.push({
        filename: file.name,
        status: 'success',
        icon: 'mdi-check-circle',
        color: 'success',
        message: null
      })
      anySuccess = true
    } catch (err) {
      if (err.isDuplicate) {
        results.value.push({
          filename: file.name,
          status: 'warn',
          icon: 'mdi-alert-circle',
          color: 'warning',
          message: null
        })
      } else {
        results.value.push({
          filename: file.name,
          status: 'error',
          icon: 'mdi-close-circle',
          color: 'error',
          message: err.message || 'Unknown error'
        })
      }
    }
    done.value++
  }

  finished.value = true
  if (anySuccess) emit('imported')
}

const close = () => {
  model.value = false
  // Reset state
  resetState()
}

const onModelUpdate = (val) => {
  if (!val) {
    resetState()
  }
}

const resetState = () => {
  pendingFiles.value = []
  results.value = []
  started.value = false
  finished.value = false
  done.value = 0
  total.value = 0
}
</script>

<style scoped>
.message-wrap {
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
}
</style>
