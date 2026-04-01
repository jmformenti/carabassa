<template>
  <v-app>
    <v-navigation-drawer
      v-if="$route.path !== '/login'"
      v-model="drawer"
      color="orange-darken-1"
    >
      <v-list v-if="authStore.isAuthenticated">
        <v-list-item
          prepend-icon="mdi-account"
          :title="authStore.user"
          :subtitle="authStore.role"
          class="bg-orange-darken-2 mb-2"
        />
        <v-divider />
      </v-list>
      <v-list>
        <v-list-item
          v-for="(item, i) in menuItems"
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
        v-if="$route.path !== '/login'"
        color="orange-lighten-1"
        @click.stop="drawer = !drawer" 
      />
      <v-app-bar-title>{{ title }}</v-app-bar-title>
      <v-spacer />
      <v-menu
        v-if="datasets.length > 0 && datasetStore.dataset"
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

      <v-btn
        v-if="authStore.isAuthenticated"
        icon
        title="Logout"
        @click="logout"
      >
        <v-icon>mdi-logout</v-icon>
      </v-btn>
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
          <span class="version">
            v.{{ $config.public.appVersion }}
          </span>
        </v-card-text>
      </v-card>
    </v-footer>
  </v-app>
</template>

<script>
import { useAuthStore } from '~/stores/auth'

export default {
  setup () {
    const datasetStore = useDatasetStore()
    const authStore = useAuthStore()
    return { datasetStore, authStore }
  },

  data () {
    return {
      title: 'Carabassa',
      apiBaseURL: null,
      drawer: false,
      datasets: [],
      showPwaDebug: false,
      items: [
        {
          title: 'Search',
          icon: 'mdi-magnify',
          to: '/'
        },
        {
          title: 'Profile',
          icon: 'mdi-account-cog',
          to: '/user'
        },
        {
          title: 'Users',
          icon: 'mdi-account-group',
          to: '/admin/users',
          adminOnly: true
        },
        {
          title: 'PWA Debug',
          icon: 'mdi-bug',
          to: '/pwa-debug'
        }
      ]
    }
  },

  mounted () {
    this.updatePwaDebugVisibility()
    if (this.$route.path === '/login' || !this.authStore.isAuthenticated) return
    this.$carabassa.getDatasets()
      .then(async data => {
        this.datasets = data
        await this.initDataset()
        this.datasetStore.datasetsLoaded = true
      })
  },

  computed: {
    menuItems () {
      return this.items.filter(item => {
        if (item.adminOnly && !this.authStore.isAdmin) return false
        if (item.to === '/pwa-debug') return this.showPwaDebug
        return true
      })
    }
  },

  watch: {
    'authStore.isAuthenticated': {
      immediate: true,
      handler: async function (isAuth) {
        if (isAuth && !this.datasetStore.datasetsLoaded) {
          const data = await this.$carabassa.getDatasets()
          this.datasets = data
          await this.initDataset()
          this.datasetStore.datasetsLoaded = true
          console.log('datasetsLoaded set to true')
        }
      }
    }
  },

  methods: {
    logout () {
      this.authStore.logout()
      this.$router.push('/login')
    },
    updatePwaDebugVisibility () {
      if (typeof window === 'undefined') return
      const isAndroid = /Android/i.test(window.navigator?.userAgent || '')
      const isBrowserMode = window.matchMedia && window.matchMedia('(display-mode: browser)').matches
      this.showPwaDebug = isAndroid && isBrowserMode
    },
    changeDataset (dataset) {
      if (dataset) {
        this.$router.push(`/dataset/${dataset.name}`)
      }
    },

    async initDataset () {
      if (this.datasets.length === 0) {
        this.datasetStore.dataset = null
        return
      }
      
      const datasetNameFromRoute = this.$route.params.name
      const datasetNameFromQuery = this.$route.query.dataset
      const targetDatasetName = datasetNameFromRoute || datasetNameFromQuery

      if (targetDatasetName) {
        try {
          this.datasetStore.dataset = await this.$carabassa.getDatasetByName(targetDatasetName)
        } catch {
          // Dataset not found for the route name — leave it null so the page shows "not found"
          this.datasetStore.dataset = null
        }
      } else {
        // On root or login, pick the first dataset
        this.datasetStore.dataset = this.datasets[0]
        if (this.$route.path !== '/login') {
          this.changeDataset(this.datasets[0])
        }
      }
    }
  }
}
</script>

<style scoped>
.version {
  float: right;
  margin-right: 20px;
}
</style>
