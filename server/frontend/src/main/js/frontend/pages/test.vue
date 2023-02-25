<template>
  <div class="text-center">
    <v-btn
      color="error"
      @click="overlay = !overlay"
    >
      Show Overlay
    </v-btn>

    <v-overlay v-model="overlay" />

    <v-menu>
      <template #activator="{ props }">
        <v-btn
          text 
          v-bind="props"
        >
          {{ selectedDataset }}
        </v-btn>
      </template>
      <v-list>
        <v-list-item
          v-for="(item, index) in datasets"
          :key="index"
          dense
        >
          <v-list-item-title>{{ item.name }}</v-list-item-title>
        </v-list-item>
      </v-list>
    </v-menu>
  </div>
</template>

<script>
  export default {
    data: () => ({
      overlay: false,
      selectedDataset: 'All',
      datasets: [
        {
          name: 'All'
        },
        {
          name: 'Maria'
        },
        {
          name: 'Arnau'
        }
      ]
    }),

    watch: {
      overlay (val) {
        val && setTimeout(() => {
          this.overlay = false
        }, 2000)
      },
    },
  }
</script>
