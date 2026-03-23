<template>
  <v-row
    justify="center" 
    align="center"
  >
    <v-col
      cols="12"
      sm="8"
      md="6"
    >
      <v-menu
        v-model="menu"
        :close-on-content-click="false"
        location="bottom"
        activator="#search-input"
        max-height="300"
        :open-on-click="false"
        :open-on-focus="false"
        :offset="[0, 5]"
      >
        <template #activator="{ props: menuProps }">
          <v-text-field
            ref="textField"
            id="search-input"
            v-model="searchString"
            label="Search"
            variant="underlined"
            clear-icon="mdi-close-circle"
            clearable
            @keyup.enter="onSearch"
            @input="handleInput"
            @click="handleInput"
          >
            <template #prepend>
              <v-tooltip location="bottom">
                <template #activator="{ props: tooltipProps }">
                  <v-icon
                    small
                    class="with-pointer no-opacity"
                    color="orange-darken-2"
                    v-bind="tooltipProps"
                  >
                    mdi-help-circle
                  </v-icon>
                </template>
                <div>
                  Cheatsheet for searching:<br>
                  <b>from:</b> YYYY-MM-DD<br>
                  <b>to:</b> YYYY-MM-DD<br>
                  <b>on:</b> YYYY-MM-DD
                  <template v-if="tagInfos.length">
                    <br>
                    <span
                      v-for="tagInfo in tagInfos"
                      :key="tagInfo.id || tagInfo.tagName"
                    >
                      <b>{{ tagInfo.alias || tagInfo.tagName }}:</b>
                      {{ tagInfo.description || 'No description' }}<br>
                    </span>
                  </template>
                </div>
              </v-tooltip>
            </template>

            <template #append>
              <v-icon
                class="no-opacity"
                color="orange-darken-2"
                @click="onSearch"
              >
                mdi-send
              </v-icon>
            </template>
          </v-text-field>
        </template>
        <v-list v-if="suggestions.length > 0" density="compact">
          <v-list-item
            v-for="(item, i) in suggestions"
            :key="i"
            @click="selectSuggestion(item)"
          >
            <v-list-item-title>{{ item }}</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </v-col>
    <v-col
      cols="12"
      sm="4"
      md="3"
    >
      <v-select
        v-model="localSortField"
        :items="sortOptions"
        item-title="text"
        item-value="value"
        label="Order by"
        variant="underlined"
        @update:modelValue="onSortFieldChange"
      >
        <template #append>
          <v-btn
            :icon="localSortDirection === 'asc' ? 'mdi-sort-ascending' : 'mdi-sort-descending'"
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
</template>

<script setup>
import { ref, watch, nextTick, onMounted, computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  sortField: {
    type: String,
    default: 'archiveTime'
  },
  sortDirection: {
    type: String,
    default: 'desc'
  }
})

const emit = defineEmits(['update:modelValue', 'update:sortField', 'update:sortDirection', 'search'])
const { $carabassa } = useNuxtApp()

const searchString = ref(props.modelValue)
const localSortField = ref(props.sortField)
const localSortDirection = ref(props.sortDirection || 'desc')
const tagInfos = ref([])
const textField = ref(null)
const menu = ref(false)
const suggestions = ref([])
const currentTagMatch = ref(null)

const baseSortOptions = [
  { text: 'date', value: 'archiveTime' },
  { text: 'size', value: 'size' }
]

const sortOptions = computed(() => {
  const sortableTagOptions = tagInfos.value
    .filter((tagInfo) => tagInfo.sortable)
    .map((tagInfo) => ({
      text: tagInfo.alias || tagInfo.tagName,
      value: tagInfo.tagName
    }))

  const merged = [...baseSortOptions, ...sortableTagOptions]
  const seen = new Set()
  return merged.filter((option) => {
    if (seen.has(option.value)) return false
    seen.add(option.value)
    return true
  })
})

watch(() => props.modelValue, (newVal) => { searchString.value = newVal })
watch(() => props.sortField, (newVal) => { localSortField.value = newVal })
watch(() => props.sortDirection, (newVal) => { localSortDirection.value = newVal })

watch(searchString, (newVal) => emit('update:modelValue', newVal))
watch(localSortField, (newVal) => emit('update:sortField', newVal))
watch(localSortDirection, (newVal) => emit('update:sortDirection', newVal))

const onSearch = () => {
  emit('search')
}

const onSortFieldChange = async () => {
  await nextTick()
  onSearch()
}

const loadTagInfos = async () => {
  try {
    const data = await $carabassa.getPublicTagInfos()
    tagInfos.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.warn('Failed to load tag info:', error)
  }
}

const onMountedActions = async () => {
  await loadTagInfos()
}

onMounted(onMountedActions)

const handleInput = async () => {
  await nextTick()
  if (!textField.value) {
    menu.value = false
    return
  }
  const el = textField.value.$el
  if (!el) return
  const input = el.querySelector('input')
  if (!input) return
  
  const pos = input.selectionStart
  const textBefore = (searchString.value || '').substring(0, pos)
  
  // Match tagName:partialValue
  const match = textBefore.match(/(\w+):([^:\s]*)$/)
  if (match) {
    const key = match[1]
    const val = match[2]
    
    // Find tag info
    const tagInfo = (tagInfos.value || []).find(t => t.tagName === key || t.alias === key)
    if (tagInfo && tagInfo.type === 'STRING') {
      try {
        const values = await $carabassa.getItemTagValues(tagInfo.tagName)
        suggestions.value = values.filter(v => 
          String(v || '').toLowerCase().startsWith(val.toLowerCase())
        ).slice(0, 10)
        
        if (suggestions.value.length > 0) {
          currentTagMatch.value = {
            tagName: key,
            value: val,
            start: pos - val.length,
            end: pos
          }
          menu.value = true
          return
        }
      } catch (err) {
        console.error('Failed to load suggestions:', err)
      }
    }
  }
  menu.value = false
}

const selectSuggestion = (suggestion) => {
  const match = currentTagMatch.value
  if (!match) return
  
  const text = searchString.value
  const newText = text.substring(0, match.start) + suggestion + ' ' + text.substring(match.end)
  searchString.value = newText
  menu.value = false
  
  // Refocus input
  nextTick(() => {
    textField.value.$el.querySelector('input').focus()
  })
}

const toggleSortDirection = () => {
  localSortDirection.value = localSortDirection.value === 'asc' ? 'desc' : 'asc'
  nextTick().then(onSearch)
}
</script>

<style scoped>
.with-pointer {
  cursor: pointer;
}
.no-opacity {
  opacity: 1;
}
</style>
