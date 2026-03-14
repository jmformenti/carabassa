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
            <span>
              Cheatsheet for searching:<br>
              <b>from:</b> YYYY-MM-DD<br>
              <b>to:</b> YYYY-MM-DD<br>
              <b>on:</b> YYYY-MM-DD
            </span>
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
        @update:modelValue="onSearch"
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
import { ref, watch } from 'vue'

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

const searchString = ref(props.modelValue)
const localSortField = ref(props.sortField)
const localSortDirection = ref(props.sortDirection)

const sortOptions = [
  { text: 'date', value: 'archiveTime' },
  { text: 'size', value: 'size' },
  { text: 'duplicated', value: 'duplicated.group' }
]

watch(() => props.modelValue, (newVal) => { searchString.value = newVal })
watch(() => props.sortField, (newVal) => { localSortField.value = newVal })
watch(() => props.sortDirection, (newVal) => { localSortDirection.value = newVal })

watch(searchString, (newVal) => emit('update:modelValue', newVal))
watch(localSortField, (newVal) => emit('update:sortField', newVal))
watch(localSortDirection, (newVal) => emit('update:sortDirection', newVal))

const onSearch = () => {
  emit('search')
}

const toggleSortDirection = () => {
  localSortDirection.value = localSortDirection.value === 'asc' ? 'desc' : 'asc'
  onSearch()
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
