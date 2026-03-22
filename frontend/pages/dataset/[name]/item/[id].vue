<template>
  <v-container v-if="item">
    <v-row>
      <v-col>
        <div class="d-flex align-center mb-4">
          <v-btn icon="mdi-arrow-left" variant="text" class="mr-4" @click="$router.back()" />
          <h2 class="text-h4 mr-2">{{ item.filename }}</h2>
          <ItemActions
            :item="item"
            @delete="confirmDeleteItem"
            @copy-link="copyLink"
          />
        </div>
      </v-col>
    </v-row>

    <v-row>
      <v-col cols="12" md="8">
        <v-card class="media-container pa-4" flat color="transparent">
          <video
            v-if="isVideo"
            class="media-content"
            :src="$carabassa.getItemContentURL(item.id)"
            controls
            autoplay
          />
          <div
            v-else
            class="media-wrapper"
            @mousedown="startDrawing"
            @mousemove="draw"
            @mouseup="endDrawing"
            @mouseleave="endDrawing"
          >
            <img
              ref="mediaImage"
              class="media-content"
              :src="$carabassa.getItemContentURL(item.id)"
              @load="onImageLoad"
              draggable="false"
            />
            <svg
              class="drawing-layer"
              :viewBox="imageNaturalWidth && imageNaturalHeight
                ? '0 0 ' + imageNaturalWidth + ' ' + imageNaturalHeight
                : '0 0 100 100'"
              preserveAspectRatio="none"
              style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; pointer-events: none;"
            >
              <rect
                v-if="drawingBox"
                :x="Math.min(drawingBox.start.x, drawingBox.current.x)"
                :y="Math.min(drawingBox.start.y, drawingBox.current.y)"
                :width="Math.abs(drawingBox.start.x - drawingBox.current.x)"
                :height="Math.abs(drawingBox.start.y - drawingBox.current.y)"
                fill="rgba(255, 152, 0, 0.2)"
                stroke="rgba(255, 152, 0, 1)"
                :stroke-width="bboxStrokeWidth"
              />
              <rect
                v-if="tagForm.boundingBox"
                :x="tagForm.boundingBox.minX"
                :y="tagForm.boundingBox.minY"
                :width="tagForm.boundingBox.width"
                :height="tagForm.boundingBox.height"
                fill="none"
                stroke="rgba(255, 152, 0, 1)"
                :stroke-width="bboxStrokeWidth"
                stroke-dasharray="2"
              />
              <rect
                v-for="tag in tagsWithBoundingBox"
                :key="'existing-' + tag.id"
                :x="tag.boundingBox.minX"
                :y="tag.boundingBox.minY"
                :width="tag.boundingBox.width"
                :height="tag.boundingBox.height"
                fill="none"
                stroke="rgba(33, 150, 243, 0.6)"
                :stroke-width="bboxStrokeWidth"
                class="existing-bbox"
              />
              <text
                v-for="tag in tagsWithBoundingBox"
                :key="'label-' + tag.id"
                :x="tag.boundingBox.minX + bboxStrokeWidth"
                :y="tag.boundingBox.minY + bboxFontSize * 1.2"
                fill="rgba(33, 150, 243, 1)"
                :font-size="bboxFontSize"
                style="pointer-events: none; text-shadow: 0 0 2px white; font-weight: bold;"
              >
                {{ tag.value ? formatDisplayValue(tag.value) : getTagDisplayName(tag.name) }}
              </text>
            </svg>
          </div>
        </v-card>
      </v-col>

      <v-col cols="12" md="4">
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

        <v-card flat color="grey-lighten-4">
          <v-card-title class="d-flex align-center">
            Tags
            <v-spacer />
            <v-tooltip text="Add Tag" location="top">
              <template #activator="{ props }">
                <v-btn
                  v-bind="props"
                  icon="mdi-plus"
                  color="orange-darken-2"
                  variant="text"
                  size="small"
                  @click="addTagDialog = true"
                />
              </template>
            </v-tooltip>
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
                {{ getTagDisplayName(tag.name) }}: {{ formatDisplayValue(tag.value) }}
                <v-tooltip text="Filter by tag" location="top">
                  <template #activator="{ props }">
                    <v-icon
                      v-bind="props"
                      end
                      icon="mdi-magnify"
                      size="x-small"
                      class="ml-2 cursor-pointer"
                      @click.stop="filterByTag(tag)"
                    />
                  </template>
                </v-tooltip>
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

  <div v-else-if="error" class="d-flex justify-center align-center fill-height" style="min-height: 50vh;">
    <v-alert type="error" variant="tonal">
      {{ error }}
    </v-alert>
  </div>

  <div v-else class="d-flex justify-center align-center fill-height" style="min-height: 50vh;">
    <v-progress-circular indeterminate color="primary" size="64" />
  </div>

  <v-dialog v-model="addTagDialog" max-width="500px">
    <v-card>
      <v-card-title>
        <span class="text-h5">Add Tag</span>
      </v-card-title>
      <v-card-text>
        <v-form ref="form" v-model="valid">
          <v-combobox
            v-model="tagForm.nameSelection"
            :items="tagInfoOptions"
            item-title="title"
            item-value="value"
            :return-object="true"
            label="Name"
            :rules="[rules.required]"
            required
            variant="underlined"
            hint="You can type a new name to create a custom tag."
            persistent-hint
            @update:model-value="onTagNameChange"
          >
            <template #item="{ props, item }">
              <v-list-item v-bind="props" :title="undefined" :subtitle="undefined">
                <v-list-item-title>{{ item.title }}</v-list-item-title>
                <v-list-item-subtitle
                  v-if="item.raw?.description"
                  class="text-caption"
                >
                  {{ item.raw.description }}
                </v-list-item-subtitle>
              </v-list-item>
            </template>
          </v-combobox>
          <v-select
            v-model="tagForm.type"
            :items="tagTypes"
            label="Type"
            variant="underlined"
            required
            :disabled="!!fixedTagType"
          />

          <div v-if="tagForm.type === 'BOOLEAN'">
            <v-switch
              v-model="tagForm.value"
              color="primary"
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

          <div v-if="!isVideo" class="mt-4">
            <div class="text-caption text-medium-emphasis d-flex align-center">
              Bounding Box
              <v-spacer />
              <v-btn
                v-if="tagForm.boundingBox"
                icon="mdi-delete"
                size="x-small"
                variant="text"
                color="error"
                @click="tagForm.boundingBox = null"
              />
            </div>
            <div v-if="tagForm.boundingBox" class="text-body-2 border pa-2 rounded bg-grey-lighten-4">
              <template v-if="boundingBoxDisplay">
                X: {{ boundingBoxDisplay.minX }},
                Y: {{ boundingBoxDisplay.minY }},
                W: {{ boundingBoxDisplay.width }},
                H: {{ boundingBoxDisplay.height }}
              </template>
              <template v-else>
                Loading...
              </template>
            </div>
            <div v-else class="text-caption italic font-italic">
              Draw on the image to select a region (optional)
            </div>
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

  <v-dialog v-model="deleteTagDialog" max-width="400px">
    <v-card>
      <v-card-title class="text-h5">Delete Tag</v-card-title>
      <v-card-text>
        Are you sure you want to delete the tag <strong>{{ getTagDisplayName(tagToDelete?.name) }}</strong>?
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn color="orange-darken-2" variant="text" @click="deleteTagDialog = false">Cancel</v-btn>
        <v-btn color="orange-darken-2" variant="text" :loading="deletingTag" @click="deleteTag">Delete</v-btn>
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

  <v-dialog
    v-model="deleteItemDialog"
    max-width="400"
  >
    <v-card>
      <v-card-title>Delete item</v-card-title>
      <v-card-text>Are you sure you want to delete this item?</v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn
          color="orange-darken-2"
          @click="deleteItemDialog = false"
        >
          Cancel
        </v-btn>
        <v-btn
          color="orange-darken-2"
          :loading="deletingItem"
          @click="deleteItem"
        >
          Delete
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import ItemActions from '~/components/ItemActions.vue'
import { isVideo, formatSize, formatDate, parseTagValue } from '~/utils/itemUtils'

export default {
  components: {
    ItemActions
  },
  setup() {
    const datasetStore = useDatasetStore()
    const { copyItemLink } = useItemActions()
    return { datasetStore, copyItemLink }
  },
  data() {
    return {
      item: null,
      error: null,
      loadingItem: false,
      addTagDialog: false,
      valid: false,
      savingTag: false,
      tagInfos: [],
      publicTagInfos: [],
      fixedTagType: null,
      tagForm: {
        name: '',
        nameSelection: '',
        value: '',
        type: 'STRING',
        boundingBox: null
      },
      imageNaturalWidth: null,
      imageNaturalHeight: null,
      drawing: false,
      drawingBox: null,
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
      deleteItemDialog: false,
      deletingItem: false,
      rules: {
        required: value => !!value || 'Required.',
        numeric: value => !isNaN(parseFloat(value)) && isFinite(value) || 'Must be a number.'
      }
    }
  },
  computed: {
    isVideo() {
      return isVideo(this.item)
    },
    formattedArchiveDate() {
      if (!this.item || !this.item.archiveTime) return ''
      return parseTagValue(this.item.archiveTime)
    },
    formattedCreationDate() {
      if (!this.item || !this.item.creation) return ''
      return parseTagValue(this.item.creation)
    },
    formattedSize() {
      return this.item ? formatSize(this.item.size) : ''
    },
    boundingBoxDisplay() {
      if (!this.tagForm.boundingBox) {
        return null
      }

      const bbox = this.tagForm.boundingBox
      return {
        minX: Math.round(bbox.minX),
        minY: Math.round(bbox.minY),
        width: Math.round(bbox.width),
        height: Math.round(bbox.height)
      }
    },
    filteredTags() {
      if (!this.item || !this.item.tags) return []
      return this.item.tags.filter(tag => !tag.name.startsWith('meta.'))
    },
    tagsWithBoundingBox() {
      if (!this.filteredTags.length) return []

      if (!this.imageNaturalWidth || !this.imageNaturalHeight) return []

      return this.filteredTags.filter(tag => tag.boundingBox)
    },
    tagInfoOptions() {
      return this.publicTagInfos.map(tagInfo => {
        return {
          title: tagInfo.alias || tagInfo.tagName,
          value: tagInfo.tagName,
          description: tagInfo.description || ''
        }
      })
    },
    bboxStrokeWidth() {
      if (!this.imageNaturalWidth) return 1
      return Math.max(2, this.imageNaturalWidth / 400)
    },
    bboxFontSize() {
      if (!this.imageNaturalWidth) return 16
      return Math.max(16, this.imageNaturalWidth / 50)
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
  mounted() {
    if (!this.item) {
      this.fetchItem()
    }
    this.loadTagInfos()
  },
  methods: {
    async fetchItem() {
      if (this.loadingItem) return
      this.loadingItem = true
      this.error = null
      try {
        const itemId = this.$route.params.id
        const routeDatasetName = this.$route.params.name

        if (routeDatasetName) {
          const dataset = await this.$carabassa.getDatasetByName(routeDatasetName)
          this.datasetStore.dataset = dataset
          this.item = await this.$carabassa.getItem(itemId)
          return
        }

        if (this.datasetStore.dataset) {
          this.item = await this.$carabassa.getItem(itemId)
          return
        }

        this.error = 'Dataset not specified.'
      } catch (err) {
        console.error('Error fetching item details:', err)
        this.error = 'Failed to load item details.'
        this.$notification.alert('Failed to load item details: ' + (err.message || 'Unknown error'))
      } finally {
        this.loadingItem = false
      }
    },
    startDrawing(e) {
      if (this.isVideo) return
      if (!this.imageNaturalWidth || !this.imageNaturalHeight) return

      this.drawing = true
      const rect = e.currentTarget.getBoundingClientRect()
      const scaleX = this.imageNaturalWidth / rect.width
      const scaleY = this.imageNaturalHeight / rect.height
      const x = (e.clientX - rect.left) * scaleX
      const y = (e.clientY - rect.top) * scaleY

      this.drawingBox = {
        start: { x, y },
        current: { x, y }
      }
    },
    draw(e) {
      if (!this.drawing || !this.drawingBox) return

      const rect = e.currentTarget.getBoundingClientRect()
      const scaleX = this.imageNaturalWidth / rect.width
      const scaleY = this.imageNaturalHeight / rect.height
      const maxX = this.imageNaturalWidth
      const maxY = this.imageNaturalHeight
      const x = Math.max(0, Math.min(maxX, (e.clientX - rect.left) * scaleX))
      const y = Math.max(0, Math.min(maxY, (e.clientY - rect.top) * scaleY))

      this.drawingBox.current = { x, y }
    },
    endDrawing() {
      if (!this.drawing) return
      this.drawing = false

      if (this.drawingBox) {
        const x1 = this.drawingBox.start.x
        const y1 = this.drawingBox.start.y
        const x2 = this.drawingBox.current.x
        const y2 = this.drawingBox.current.y

        const width = Math.abs(x1 - x2)
        const height = Math.abs(y1 - y2)

        if (width > 1 && height > 1) {
          this.tagForm.boundingBox = {
            minX: Math.min(x1, x2),
            minY: Math.min(y1, y2),
            width: width,
            height: height
          }
          this.addTagDialog = true
        }
      }
      this.drawingBox = null
    },
    closeAddTagDialog() {
      this.addTagDialog = false
      if (this.$refs.form) {
        this.$refs.form.reset()
      }
      this.resetTagForm()
    },
    async submitTag() {
      const result = await this.$refs.form.validate()
      if (!result?.valid) return

      this.savingTag = true
      try {
        let value = this.tagForm.value
        let type = this.tagForm.type

        if (this.fixedTagType) {
          if (this.fixedTagType === 'LONG' || this.fixedTagType === 'DOUBLE') {
            value = Number(value)
          }
          type = this.fixedTagType
        } else if (type === 'NUMBER') {
          const numValue = Number(value)
          value = numValue
          type = Number.isInteger(numValue) && !String(this.tagForm.value).includes('.') ? 'LONG' : 'DOUBLE'
        }

        const tagData = {
          name: this.resolveTagName(this.tagForm.name),
          value: value,
          type: type
        }

        const boundingBox = this.buildBoundingBoxPayload()
        if (boundingBox) {
          tagData.boundingBox = boundingBox
        }

        await this.$carabassa.addItemTag(this.item.id, tagData)
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
        const d = new Date(val);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        this.tagForm.value = `${year}-${month}-${day}`;
      }
      this.dateMenu = false;
    },
    formatDisplayValue(val) {
      return parseTagValue(val)
    },
    resetTagForm() {
      const defaultName = this.publicTagInfos.length ? this.publicTagInfos[0].tagName : ''
      this.tagForm = {
        name: defaultName,
        nameSelection: defaultName,
        value: '',
        type: 'STRING',
        boundingBox: null
      }
      this.onTagNameChange(this.tagForm.nameSelection)
    },
    async loadTagInfos() {
      try {
        const allTags = await this.$carabassa.getTagInfos()
        this.tagInfos = Array.isArray(allTags) ? allTags : []
        const publicTags = await this.$carabassa.getPublicTagInfos()
        this.publicTagInfos = Array.isArray(publicTags) ? publicTags : []
        if (!this.tagForm.name && this.publicTagInfos.length) {
          this.tagForm.name = this.publicTagInfos[0].tagName
        }
        this.tagForm.nameSelection = this.tagForm.name
        this.onTagNameChange(this.tagForm.nameSelection)
      } catch (err) {
        console.warn('Failed to load tag info:', err)
      }
    },
    onTagNameChange(selection) {
      const tagNameOrAlias = this.extractTagName(selection)
      this.tagForm.name = tagNameOrAlias
      const match = this.publicTagInfos.find(tagInfo =>
        tagInfo.tagName === tagNameOrAlias || tagInfo.alias === tagNameOrAlias
      )
      if (match?.type) {
        this.fixedTagType = match.type
        this.tagForm.type = this.mapTagInfoTypeToFormType(match.type)
        const option = this.tagInfoOptions.find(tagInfo => tagInfo.value === match.tagName)
        if (option && this.tagForm.nameSelection !== option) {
          this.tagForm.nameSelection = option
        }
        return
      }
      this.fixedTagType = null
    },
    extractTagName(selection) {
      if (!selection) return ''
      if (typeof selection === 'string') return selection
      if (typeof selection === 'object') {
        return selection.value || selection.tagName || selection.title || ''
      }
      return ''
    },
    mapTagInfoTypeToFormType(valueType) {
      if (valueType === 'LONG' || valueType === 'DOUBLE') {
        return 'NUMBER'
      }
      return valueType || 'STRING'
    },
    getTagDisplayName(tagName) {
      if (!tagName) return ''
      const match = this.tagInfos.find(tagInfo => tagInfo.tagName === tagName)
      return match?.alias || tagName
    },
    resolveTagName(tagNameOrAlias) {
      if (!tagNameOrAlias) return ''
      const match = this.tagInfos.find(tagInfo => tagInfo.alias === tagNameOrAlias)
      return match?.tagName || tagNameOrAlias
    },
    buildBoundingBoxPayload() {
      if (!this.tagForm.boundingBox) return null

      const bbox = this.tagForm.boundingBox
      return {
        minX: Math.round(bbox.minX),
        minY: Math.round(bbox.minY),
        width: Math.round(bbox.width),
        height: Math.round(bbox.height)
      }
    },
    onImageLoad() {
      const imageEl = this.$refs.mediaImage
      if (!imageEl) return
      this.imageNaturalWidth = imageEl.naturalWidth || null
      this.imageNaturalHeight = imageEl.naturalHeight || null
    },
    async copyLink() {
      try {
        const ok = await this.copyItemLink({
          datasetName: this.$route?.params?.name,
          itemId: this.item?.id,
          fallbackUrl: window.location.href
        })
        if (ok) {
          this.snackbar = true
        }
      } catch (err) {
        console.error('Failed to copy text: ', err)
      }
    },
    filterByTag(tag) {
      const datasetName = this.$route?.params?.name
      if (!datasetName) return

      const tagValue = tag.value
      const tagSearchName = this.getTagDisplayName(tag.name)
      const searchValue = tagValue === null || tagValue === undefined || tagValue === ''
        ? tagSearchName
        : `${tagSearchName}:${tagValue}`

      this.$router.push({
        path: `/dataset/${datasetName}`,
        query: { search: searchValue }
      })
    },
    confirmDeleteItem() {
      this.deleteItemDialog = true
    },
    async deleteItem() {
      this.deletingItem = true
      try {
        await this.$carabassa.deleteItem(this.item.id)
        this.deleteItemDialog = false
        this.$router.back()
      } catch (err) {
        console.error('Error deleting item:', err)
        this.$notification.alert('Failed to delete item: ' + (err.message || 'Unknown error'))
      } finally {
        this.deletingItem = false
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
.media-wrapper {
  position: relative;
  display: inline-block;
  line-height: 0;
}
.media-content {
  max-width: 100%;
  max-height: 70vh;
  display: block;
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
.drawing-layer {
  cursor: crosshair;
}
.drawing-layer rect {
  pointer-events: none;
}
.existing-bbox:hover {
  stroke: rgba(33, 150, 243, 1);
  stroke-width: 0.8;
  filter: drop-shadow(0 0 2px rgba(33, 150, 243, 0.5));
}
</style>
