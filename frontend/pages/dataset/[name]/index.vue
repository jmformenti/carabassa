<template>
  <ItemCollection :key="`dataset-${$route.params.name}`" />
</template>

<script setup>
import ItemCollection from '~/components/ItemCollection.vue'
</script>
