<template>
  <v-container v-if="item">
    <v-row>
      <v-col>
        <div class="d-flex align-center mb-4">
          <v-btn icon="mdi-arrow-left" variant="text" class="mr-4" @click="$router.back()" />
          <h2 class="text-h4 mr-2">{{ item.filename }}</h2>
          <v-tooltip text="Copy internal link" location="top">
            <template #activator="{ props }">
              <v-btn
                v-bind="props"
                icon="mdi-link"
                variant="text"
                color="primary"
                @click="copyLink"
              />
            </template>
          </v-tooltip>
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
          <v-list lines="one" bg-color="transparent" density="compact" class="info-list pt-0 pb-2">
            <v-list-item class="py-0">
              <template #prepend>
                <v-tooltip text="Archive Date" location="top">
                  <template #activator="{ props }">
                    <v-icon v-bind="props" icon="mdi-calendar" class="mr-3" />
                  </template>
                </v-tooltip>
              </template>
              <div class="text-body-2">{{ formattedArchiveDate }}</div>
            </v-list-item>
            <v-list-item class="py-0">
              <template #prepend>
                <v-tooltip text="Upload Date" location="top">
                  <template #activator="{ props }">
                    <v-icon v-bind="props" icon="mdi-upload" class="mr-3" />
                  </template>
                </v-tooltip>
              </template>
              <div class="text-body-2">{{ formattedCreationDate }}</div>
            </v-list-item>
            <v-list-item class="py-0">
              <template #prepend>
                <v-tooltip text="Type" location="top">
                  <template #activator="{ props }">
                    <v-icon v-bind="props" icon="mdi-file" class="mr-3" />
                  </template>
                </v-tooltip>
              </template>
              <div class="text-body-2">{{ item.type }}</div>
            </v-list-item>
            <v-list-item class="py-0">
              <template #prepend>
                <v-tooltip text="Format" location="top">
                  <template #activator="{ props }">
                    <v-icon v-bind="props" icon="mdi-information-outline" class="mr-3" />
                  </template>
                </v-tooltip>
              </template>
              <div class="text-body-2">{{ item.format }}</div>
            </v-list-item>
            <v-list-item class="py-0">
              <template #prepend>
                <v-tooltip text="Size" location="top">
                  <template #activator="{ props }">
                    <v-icon v-bind="props" icon="mdi-harddisk" class="mr-3" />
                  </template>
                </v-tooltip>
              </template>
              <div class="text-body-2">{{ formattedSize }}</div>
            </v-list-item>
          </v-list>
        </v-card>

        <!-- Tags -->
        <v-card flat color="grey-lighten-4">
          <v-card-title class="d-flex align-center">
            Tags
            <v-spacer />
            <v-btn
              color="primary"
              variant="text"
              size="small"
              prepend-icon="mdi-plus"
              @click="addTagDialog = true"
            >
              Add Tag
            </v-btn>
          </v-card-title>
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
                {{ tag.name }}: {{ formatDisplayValue(tag.value) }}
                <v-icon
                  end
                  icon="mdi-close"
                  size="x-small"
                  class="ml-2 cursor-pointer"
                  @click.stop="confirmDeleteTag(tag)"
                />
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

  <!-- Add Tag Dialog -->
  <v-dialog v-model="addTagDialog" max-width="500px">
    <v-card>
      <v-card-title>
        <span class="text-h5">Add Tag</span>
      </v-card-title>
      <v-card-text>
        <v-form ref="form" v-model="valid">
          <v-text-field
            v-model="tagForm.name"
            label="Name"
            :rules="[rules.required]"
            required
            variant="underlined"
          />
          <v-select
            v-model="tagForm.type"
            :items="tagTypes"
            label="Type"
            variant="underlined"
            required
          />

          <!-- Value Input based on Type -->
          <div v-if="tagForm.type === 'BOOLEAN'">
            <v-switch
              v-model="tagForm.value"
              color="primary"
              label="Value"
              inset
              hide-details
            />
          </div>

          <div v-else-if="tagForm.type === 'DATE'">
            <v-menu
              v-model="dateMenu"
              :close-on-content-click="false"
              transition="scale-transition"
              offset-y
              min-width="auto"
            >
              <template #activator="{ props }">
                <v-text-field
                  v-model="tagForm.value"
                  label="Value"
                  prepend-icon="mdi-calendar"
                  readonly
                  v-bind="props"
                  variant="underlined"
                  :rules="[rules.required]"
                />
              </template>
              <v-date-picker
                v-model="datePickerValue"
                color="primary"
                @update:model-value="onDateSelected"
              />
            </v-menu>
          </div>

          <div v-else>
            <v-text-field
              v-model="tagForm.value"
              label="Value"
              :rules="[rules.required, ...(tagForm.type === 'NUMBER' ? [rules.numeric] : [])]"
              required
              variant="underlined"
            />
          </div>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn color="primary" variant="text" @click="closeAddTagDialog">Cancel</v-btn>
        <v-btn color="primary" variant="text" :loading="savingTag" @click="submitTag">Save</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>

  <!-- Delete Tag Confirmation Dialog -->
  <v-dialog v-model="deleteTagDialog" max-width="400px">
    <v-card>
      <v-card-title class="text-h5">Delete Tag</v-card-title>
      <v-card-text>
        Are you sure you want to delete the tag <strong>{{ tagToDelete?.name }}</strong>?
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn color="primary" variant="text" @click="deleteTagDialog = false">Cancel</v-btn>
        <v-btn color="primary" variant="text" :loading="deletingTag" @click="deleteTag">Delete</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>

  <v-snackbar
    v-model="snackbar"
    :timeout="2000"
    color="primary"
    location="top"
  >
    Link copied to clipboard
  </v-snackbar>
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
      error: null,
      addTagDialog: false,
      valid: false,
      savingTag: false,
      tagForm: {
        name: '',
        value: '',
        type: 'STRING'
      },
      tagTypes: [
        { title: 'String', value: 'STRING' },
        { title: 'Boolean', value: 'BOOLEAN' },
        { title: 'Date', value: 'DATE' },
        { title: 'Number', value: 'NUMBER' }
      ],
      dateMenu: false,
      datePickerValue: null,
      deleteTagDialog: false,
      tagToDelete: null,
      deletingTag: false,
      snackbar: false,
      rules: {
        required: value => !!value || 'Required.',
        numeric: value => !isNaN(parseFloat(value)) && isFinite(value) || 'Must be a number.'
      }
    }
  },
  computed: {
    isVideo() {
      return this.item && this.item.type && this.item.type.toLowerCase() === 'video'
    },
    formattedArchiveDate() {
      if (!this.item || !this.item.archiveTime) return ''
      return this.formatDisplayValue(this.item.archiveTime)
    },
    formattedCreationDate() {
      if (!this.item || !this.item.creation) return ''
      return this.formatDisplayValue(this.item.creation)
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
    },
    'tagForm.type': {
      handler(newType) {
        // Reset value to default for the new type
        if (newType === 'BOOLEAN') {
          this.tagForm.value = false
        } else if (newType === 'DATE') {
          this.tagForm.value = ''
          this.datePickerValue = null
        } else {
          this.tagForm.value = ''
        }
      }
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
    },
    closeAddTagDialog() {
      this.addTagDialog = false
      if (this.$refs.form) {
        this.$refs.form.reset()
      }
    },
    async submitTag() {
      const { valid } = await this.$refs.form.validate()
      if (!valid) return

      this.savingTag = true
      let value = this.tagForm.value
      let type = this.tagForm.type

      if (type === 'NUMBER') {
        const numValue = Number(value)
        value = numValue
        type = Number.isInteger(numValue) ? 'LONG' : 'DOUBLE'
      }

      try {
        await this.$carabassa.addItemTag(this.item.id, {
          name: this.tagForm.name,
          value: value,
          type: type
        })
        await this.fetchItem()
        this.closeAddTagDialog()
      } catch (err) {
        console.error('Error adding tag:', err)
        this.$notification.alert('Failed to add tag: ' + (err.message || 'Unknown error'))
      } finally {
        this.savingTag = false
      }
    },
    confirmDeleteTag(tag) {
      this.tagToDelete = tag
      this.deleteTagDialog = true
    },
    async deleteTag() {
      if (!this.tagToDelete) return

      this.deletingTag = true
      try {
        await this.$carabassa.deleteItemTag(this.item.id, this.tagToDelete.id)
        await this.fetchItem()
        this.deleteTagDialog = false
        this.tagToDelete = null
      } catch (err) {
        console.error('Error deleting tag:', err)
        this.$notification.alert('Failed to delete tag: ' + (err.message || 'Unknown error'))
      } finally {
        this.deletingTag = false
      }
    },
    onDateSelected(val) {
      if (val) {
        // v-date-picker returns a Date object. Normalize to YYYY-MM-DD
        const d = new Date(val);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        this.tagForm.value = `${year}-${month}-${day}`;
      }
      this.dateMenu = false;
    },
    formatDisplayValue(val) {
      if (val === null || val === undefined || val === '') return ''
      
      // If it's a boolean or a "small" number, return as is
      if (typeof val === 'boolean') return val.toString()
      if (typeof val === 'number') {
        // Only treat as date if it's a very large number (likely epoch ms > year 2000)
        if (val > 946684800000) { 
          const d = new Date(val)
          const iso = d.toISOString()
          return iso.endsWith('T00:00:00.000Z') || iso.endsWith('T00:00:00Z') ? iso.split('T')[0] : d.toLocaleString()
        }
        return val.toString()
      }

      const strVal = String(val)
      // Check for YYYY-MM-DD or ISO pattern
      const isDateString = /^\d{4}-\d{2}-\d{2}/.test(strVal)
      
      if (isDateString) {
        const d = new Date(strVal)
        if (!isNaN(d.getTime())) {
          const iso = d.toISOString()
          if (iso.endsWith('T00:00:00.000Z') || iso.endsWith('T00:00:00Z') || strVal.includes('T00:00:00')) {
            return iso.split('T')[0]
          }
          return d.toLocaleString()
        }
      }
      return strVal
    },
    async copyLink() {
      try {
        await navigator.clipboard.writeText(window.location.href)
        this.snackbar = true
      } catch (err) {
        console.error('Failed to copy text: ', err)
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
.cursor-pointer {
  cursor: pointer;
}
.info-list :deep(.v-list-item) {
  min-height: 28px !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
}
.info-list :deep(.v-list-item__prepend) {
  height: 28px;
}
</style>
