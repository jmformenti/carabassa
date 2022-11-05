<template>
  <div>
    <v-row justify="center" align="center">
      <v-col cols="12" sm="8" md="6">
        <v-text-field
          v-model="searchString"
          label="Search"
          clear-icon="mdi-close-circle"
          clearable
          @keyup.enter="search"
        >
          <template #prepend>
            <v-tooltip bottom>
              <template #activator="{ on, attrs }">
                <v-icon
                  small
                  color="orange darken-2"
                  v-bind="attrs"
                  v-on="on"
                >
                  mdi-help-circle
                </v-icon>
              </template>
              <span>
                Cheatsheet for searching:<br />
                <b>from:</b> YYYY-MM-DD<br />
                <b>to:</b> YYYY-MM-DD<br />
                <b>on:</b> YYYY-MM-DD
              </span>
            </v-tooltip>
          </template>
          <template #append-outer>
            <v-icon
              color="orange darken-2"
              @click="search"
            >
              mdi-send
            </v-icon>
          </template>
        </v-text-field>
      </v-col>
    </v-row>
    <v-row>
      <v-col>
        <div v-if="searched" class="text-body-2">
          {{ totalItems }} found
        </div>
      </v-col>
    </v-row>
    <v-row v-if="items.length">
      <v-col
        v-for="item of items"
        :key="item.id"
        class="d-flex child-flex"
        cols="2"
      >
        <a :href="`${$axios.defaults.baseURL}/api/dataset/1/item/${item.id}/content`" download>
          <v-img
            :src="`${$axios.defaults.baseURL}/api/dataset/1/item/${item.id}/thumbnail`"
            :lazy-src="`${$axios.defaults.baseURL}/api/dataset/1/item/${item.id}/thumbnail`"
            aspect-ratio="1"
            class="grey lighten-2"
            :title="`${item.id} - ${item.archiveTime}`"
          >
            <template #placeholder>
              <v-row
                class="fill-height ma-0"
                align="center"
                justify="center"
              >
                <v-progress-circular
                  indeterminate
                  color="grey lighten-5"
                />
              </v-row>
            </template>
          </v-img>
        </a>
      </v-col>
    </v-row>
    <v-row>
      <v-col>
        <div v-if="leftItems > 0" class="text-body-2 text-center">
          <v-icon>
            mdi-chevron-double-down
          </v-icon>
          {{ leftItems }} images left (loading)
        </div>
      </v-col>
    </v-row>
  </div>
</template>

<script>
export default {
  data () {
    return {
      searchString: '',
      totalItems: 0,
      items: [],
      currentPage: 0,
      pageSize: 24,
      totalPages: 0,
      searched: false
    }
  },
  computed: {
    leftItems () {
      const leftItems = this.totalItems - this.currentPage * this.pageSize
      if (leftItems > this.pageSize) {
        return leftItems
      } else {
        return 0
      }
    }
  },
  mounted () {
    this.nextPage()
  },
  methods: {
    async getItems () {
      const newItems = await this.$axios.$get(
        this.$axios.defaults.baseURL + '/api/dataset/1/item?type=image&size=' + this.pageSize + '&page=' + this.currentPage + '&search=' + this.searchString + ' type:I'
      ).then((data) => {
        if (data._embedded) {
          this.totalItems = data.page.totalElements
          this.totalPages = data.page.totalPages
          return data._embedded.itemRepresentationList
        } else {
          return []
        }
      }
      )
      this.items.push(...newItems)
      this.searched = true
    },
    search () {
      this.currentPage = 0
      this.totalItems = 0
      this.totalPages = 0
      this.items = []
      this.getItems()
    },
    nextPage () {
      window.onscroll = () => {
        const bottomOfWindow = document.documentElement.scrollTop + window.innerHeight === document.documentElement.offsetHeight
        if (bottomOfWindow) {
          this.currentPage++
          if (this.currentPage < this.totalPages) {
            this.getItems()
          }
        }
      }
    }
  }
}
</script>
