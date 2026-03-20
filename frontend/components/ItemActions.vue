<template>
  <v-tooltip text="Download" location="top">
    <template #activator="{ props }">
      <v-btn
        v-bind="props"
        icon="mdi-download"
        color="orange-darken-2"
        variant="text"
        :href="$carabassa.getItemContentURL(item.id)"
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

  <v-tooltip text="Copy internal link" location="top">
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
defineProps({
  item: {
    type: Object,
    required: true
  }
})

defineEmits(['delete', 'copy-link'])
</script>
