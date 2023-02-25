<template>
  <v-app>
    <v-navigation-drawer
      v-model="drawer"
      color="orange-darken-1"
    >
      <v-list>
        <v-list-item
          v-for="(item, i) in items"
          :key="i"
          :prepend-icon="item.icon"
          :title="item.title"
          :to="item.to"
        />
      </v-list>
    </v-navigation-drawer>
    <v-app-bar
      color="orange-darken-4"
    >
      <v-app-bar-nav-icon
        color="orange-lighten-1"
        @click.stop="drawer = !drawer" 
      />
      <v-app-bar-title>{{ title }}</v-app-bar-title>
      <v-spacer />
      <v-menu
        v-if="datasets"
      >
        <template #activator="{ props }">
          <v-btn
            text
            v-bind="props"
          >
            {{ datasetStore.dataset.name }}
          </v-btn>
        </template>
        <v-list>
          <v-list-item
            v-for="(dataset, index) in datasets"
            :key="index"
            :value="dataset.id"
            dense
            @click="changeDataset(dataset)"
          >
            <v-list-item-title>{{ dataset.name }}</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </v-app-bar>
    <v-main>
      <v-container>
        <NuxtPage />
      </v-container>
    </v-main>
    <v-footer
      class="bg-orange-lighten-4"
      app
      :padless="true"
    >
      <v-card
        flat
        tile
        width="100%"
        class="bg-orange-lighten-4 text-center"
      >
        <v-card-text class="py-1 font-weight-light">
          &copy; {{ new Date().getFullYear() }}
        </v-card-text>
      </v-card>
    </v-footer>
  </v-app>
</template>

<script>
export default {
  setup () {
    const datasetStore = useDatasetStore()
    return { datasetStore }
  },

  data () {
    return {
      title: 'Carabassa',
      apiBaseURL: null,
      drawer: false,
      datasets: [],
      items: [
        {
          title: 'Search',
          icon: 'mdi-magnify',
          to: '/'
        },
        {
          title: 'Test',
          icon: 'mdi-test-tube',
          to: '/test'
        }
      ]
    }
  },

  mounted () {
    this.$carabassa.getDatasets()
      .then(data => {
        this.datasets = data
        this.initDataset()
      }
    )
  },

  methods: {
    changeDataset (dataset) {
      this.datasetStore.dataset = dataset
    },

    initDataset () {
      const datasetName = this.$route.query.dataset
      if (datasetName) {
        this.$carabassa.getDatasetByName(datasetName)
          .then(data => {
            this.changeDataset(data)
          })
          .catch(() => {
            this.changeDataset(this.datasets[0])
          })
      } else {
        this.changeDataset(this.datasets[0])
      }
    }
  }
}
</script>
