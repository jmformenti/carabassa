<template>
  <v-tooltip :text="isFavorite(item) ? 'Remove favorite' : 'Add favorite'" location="top">
    <template #activator="{ props }">
      <v-btn
        v-bind="props"
        :icon="isFavorite(item) ? 'mdi-star' : 'mdi-star-outline'"
        :color="isFavorite(item) ? 'orange' : 'orange-darken-2'"
        variant="text"
        @click="toggleFavorite(item)"
      />
    </template>
  </v-tooltip>

  <v-tooltip text="Download" location="top">
    <template #activator="{ props }">
      <v-btn
        v-bind="props"
        icon="mdi-download"
        color="orange-darken-2"
        variant="text"
        :href="$carabassa.getItemContentURL(item)"
        target="_blank"
      />
    </template>
  </v-tooltip>

  <v-tooltip text="Delete" location="top">
    <template #activator="{ props }">
      <v-btn
        v-bind="props"
        icon="mdi-delete"
        color="orange-darken-2"
        variant="text"
        @click="$emit('delete', item)"
      />
    </template>
  </v-tooltip>

  <v-tooltip text="Copy link" location="top">
    <template #activator="{ props }">
      <v-btn
        v-bind="props"
        icon="mdi-link"
        color="orange-darken-2"
        variant="text"
        @click="$emit('copy-link')"
      />
    </template>
  </v-tooltip>
</template>

<script setup>
const { isFavorite, toggleFavorite } = useItemActions()

defineProps({
  item: {
    type: Object,
    required: true
  }
})

defineEmits(['delete', 'copy-link'])
</script>
