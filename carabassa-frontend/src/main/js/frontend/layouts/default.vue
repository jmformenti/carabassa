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
          :title="item.title"
          :to="item.to"
          router
          exact
        >
          <template #prepend>
            <v-icon :icon="item.icon" />
          </template>
        </v-list-item>
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
    </v-app-bar>
    <v-main>
      <v-container>
        <NuxtPage />
      </v-container>
    </v-main>
    <v-footer
      class="bg-orange-lighten-4"
      app
    >
      <span>&copy; {{ new Date().getFullYear() }}</span>
    </v-footer>
  </v-app>
</template>

<script>
export default {
  data () {
    return {
      drawer: false,
      items: [
        {
          title: 'Search',
          icon: 'mdi-apps',
          to: '/'
        }
      ],
      title: 'Carabassa'
    }
  }
}
</script>
