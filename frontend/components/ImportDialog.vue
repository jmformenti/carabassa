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
        <!-- Pending uploads warning -->
        <v-alert
          v-if="showPendingBanner"
          type="warning"
          variant="tonal"
          class="mb-4"
          density="compact"
        >
          <div class="d-flex align-center justify-space-between gap-2">
            <span>Uploads were interrupted.</span>
            <v-btn
              v-if="canResumeNow"
              size="small"
              variant="tonal"
              color="orange-darken-2"
              @click="resumeNow"
            >
              Resume now
            </v-btn>
          </div>
        </v-alert>

        <!-- File source selector -->
        <div v-if="!started">
          <v-radio-group v-model="mode" inline class="mb-4">
            <v-radio label="Files" value="files" />
            <v-radio label="Directory" value="directory" />
          </v-radio-group>

          <!-- Files picker -->
          <v-btn
            v-if="mode === 'files'"
            variant="tonal"
            color="orange-darken-2"
            prepend-icon="mdi-folder-open"
            @click="openPicker"
          >
            Choose files
          </v-btn>

          <!-- Directory picker -->
          <div v-else>
            <div v-if="hasSavedHandle" class="d-flex flex-wrap gap-2">
              <v-btn
                variant="tonal"
                color="orange-darken-2"
                prepend-icon="mdi-folder-open"
                :loading="loadingDirectory"
                @click="openDirectory"
              >
                Load folder
              </v-btn>
              <v-btn
                variant="text"
                color="orange-darken-2"
                prepend-icon="mdi-folder-edit"
                :disabled="loadingDirectory"
                @click="changeDirectory"
              >
                Change folder
              </v-btn>
            </div>
            <v-btn
              v-else
              variant="tonal"
              color="orange-darken-2"
              prepend-icon="mdi-folder-open"
              :loading="loadingDirectory"
              @click="openDirectory"
            >
              Select folder
            </v-btn>
            <div v-if="hasSavedHandle" class="mt-2 text-caption text-medium-emphasis">
              <v-icon size="x-small">mdi-information</v-icon>
              Folder selected: {{ savedFolderName }}
            </div>
          </div>

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
        <div v-if="results.length > 0" class="results-log mt-2">
          <v-list density="compact" lines="one" max-height="240" style="overflow-y:auto">
            <v-list-item
              v-for="(r, i) in results"
              :key="i"
              :prepend-icon="r.icon"
              :base-color="r.color"
            >
              <v-list-item-title class="text-body-2">
                <div class="font-weight-medium text-truncate">{{ r.filename }}</div>
                <div v-if="r.message" class="text-caption text-medium-emphasis message-wrap">
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

  <v-dialog v-model="confirmUploadDialog" max-width="420">
    <v-card>
      <v-card-title class="d-flex align-center">
        <v-icon class="mr-2" color="orange-darken-2">mdi-checkbox-marked-circle-outline</v-icon>
        Confirm upload
      </v-card-title>
      <v-card-text>
        Upload
        <span class="text-orange text-body-1 font-weight-bold">{{ confirmCount }}</span>
        item(s) to
        <span class="text-orange text-body-1 font-weight-bold">{{ confirmDatasetName }}</span>
        ?
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn variant="text" color="orange-darken-2" @click="confirmUploadDialog = false">
          Cancel
        </v-btn>
        <v-btn variant="tonal" color="orange-darken-2" @click="confirmUpload">
          Confirm
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import SparkMD5 from 'spark-md5'

const ACCEPTED_TYPES = /^(image|video)\//

const model = defineModel({ type: Boolean, default: false })
const emit = defineEmits(['imported'])

const { $carabassa } = useNuxtApp()
const datasetStore = useDatasetStore()
const {
  getPendingFromDB,
  saveToQueue,
  markDone,
  clearQueue,
  isFSASupported,
  pickDirectory,
  getSavedDirectory,
  getFilesFromHandle
} = useUploadQueue()

const mode = ref('files')
const fileInput = ref(null)
const dirInput = ref(null)

const pendingFiles = ref([])
const results = ref([])
const started = ref(false)
const finished = ref(false)
const done = ref(0)
const total = ref(0)
const hasSavedHandle = ref(false)
const loadingDirectory = ref(false)
const pendingFromDb = ref([])
const directoryHandle = ref(null)
const savedFolderName = ref('')
const confirmUploadDialog = ref(false)
const confirmCount = ref(0)
const confirmDatasetName = ref('this dataset')

const progress = computed(() => total.value > 0 ? (done.value / total.value) * 100 : 0)
const successCount = computed(() => results.value.filter(r => r.status === 'success').length)
const warnCount = computed(() => results.value.filter(r => r.status === 'warn').length)
const errorCount = computed(() => results.value.filter(r => r.status === 'error').length)
const isPersistent = computed(() => started.value && !finished.value)
const showPendingBanner = computed(() => pendingFromDb.value.length > 0 && !started.value)
const canResumeNow = computed(() =>
  !started.value && !!directoryHandle.value && pendingFromDb.value.length > 0
)

onMounted(async () => {
  const pending = await getPendingFromDB()
  pendingFromDb.value = pending
  // Check if a saved directory handle exists
  if (isFSASupported()) {
    const handle = await getSavedDirectory().catch(() => null)
    directoryHandle.value = handle
    hasSavedHandle.value = !!handle
    savedFolderName.value = handle?.name || ''
  }
})

const hasReadPermission = async (handle) => {
  if (!handle || !handle.queryPermission) return true
  const perm = await handle.queryPermission({ mode: 'read' })
  return perm === 'granted'
}

const maybeAutoResumeFromDirectory = async () => {
  if (started.value) return
  if (!directoryHandle.value) return
  if (!pendingFromDb.value.length) return
  if (!(await hasReadPermission(directoryHandle.value))) return

  const files = await getFilesFromHandle(directoryHandle.value)
  if (!files.length) return
  const pendingKeys = new Set(
    pendingFromDb.value.map(p => `${p.fileName}::${p.fileSize}::${p.lastModified}`)
  )
  const filesToResume = files.filter(f => pendingKeys.has(getFileId(f)))
  if (filesToResume.length === 0) return

  mode.value = 'directory'
  pendingFiles.value = filesToResume
  await beginImport()
  return true
}

const resumeNow = async () => {
  const resumed = await maybeAutoResumeFromDirectory()
  if (!resumed) {
    await openDirectory()
  }
}

const openPicker = () => {
  fileInput.value.value = ''
  fileInput.value.click()
}

const getFileId = (file) => `${file.name}::${file.size}::${file.lastModified}`

const enqueuePendingFiles = async (files) => {
  for (const file of files) {
    const id = getFileId(file)
    await saveToQueue(id, file.name, file.size, file.lastModified)
  }
  pendingFromDb.value = await getPendingFromDB()
}

const loadDirectory = async (forcePick = false) => {
  loadingDirectory.value = true
  try {
    if (isFSASupported()) {
      // Try saved handle; request new if missing
      let handle = forcePick ? null : await getSavedDirectory().catch(() => null)
      if (!handle) {
        handle = await pickDirectory()
        hasSavedHandle.value = true
      }
      directoryHandle.value = handle
      savedFolderName.value = handle?.name || ''
      const files = await getFilesFromHandle(handle)
      pendingFiles.value = files
      return
    }

    // Fallback for browsers without FSA support
    dirInput.value.value = ''
    dirInput.value.click()
  } catch (err) {
    // User has cancelled the picker or has denied the permission
    if (err.name !== 'AbortError') {
      console.error('[directory] Error accessing folder:', err)
    }
  } finally {
    loadingDirectory.value = false
  }
}

const openDirectory = async () => loadDirectory(false)
const changeDirectory = async () => loadDirectory(true)

const onFilesSelected = (event) => {
  const all = Array.from(event.target.files)
  pendingFiles.value = all
    .filter(f => ACCEPTED_TYPES.test(f.type))
    .sort((a, b) => a.name.localeCompare(b.name))
}

const beginImport = async () => {
  if (pendingFiles.value.length === 0) return

  started.value = true
  finished.value = false
  results.value = []
  done.value = 0
  total.value = pendingFiles.value.length

  await enqueuePendingFiles(pendingFiles.value)
  const hadOfflineError = await processFiles(pendingFiles.value)
  if (!hadOfflineError) {
    await clearQueue()
  }
  pendingFromDb.value = await getPendingFromDB()
  finished.value = true
}

const startImport = async () => {
  if (pendingFiles.value.length === 0 || started.value) return
  confirmCount.value = pendingFiles.value.length
  confirmDatasetName.value = datasetStore?.dataset?.name || 'this dataset'
  confirmUploadDialog.value = true
}

const confirmUpload = async () => {
  confirmUploadDialog.value = false
  await beginImport()
}

const processFiles = async (files) => {
  let anySuccess = false
  let hadOfflineError = false

  for (const file of files) {
    const queueId = getFileId(file)
    const hash = SparkMD5.ArrayBuffer.hash(await file.arrayBuffer())

    try {
      const exists = await $carabassa.itemExists(hash)
      if (exists) {
        results.value.push({
          filename: file.name,
          status: 'warn',
          icon: 'mdi-alert-circle',
          color: 'warning',
          message: null
        })
        await markDone(queueId)
        done.value++
        continue
      }

      await $carabassa.addItem(file)
      await markDone(queueId)

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
        await markDone(queueId)
      } else {
        const isOffline = typeof navigator !== 'undefined' && navigator.onLine === false
        if (isOffline) {
          hadOfflineError = true
        } else {
          await markDone(queueId)
        }
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

  if (anySuccess) emit('imported')
  return hadOfflineError
}

const close = () => {
  model.value = false
  resetState()
}

const onModelUpdate = (val) => {
  if (!val) resetState()
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
