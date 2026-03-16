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
      <v-text-field
        v-model="searchString"
        label="Search"
        variant="underlined"
        clear-icon="mdi-close-circle"
        clearable
        @keyup.enter="onSearch"
      >
        <template #prepend>
          <v-tooltip location="bottom">
            <template #activator="{ props }">
              <v-icon
                small
                class="with-pointer no-opacity"
                color="orange-darken-2"
                v-bind="props"
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

onMounted(loadTagInfos)

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
