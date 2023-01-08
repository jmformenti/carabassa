<template>
  <div>
    <v-container>
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
            @keyup.enter="search"
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
          <div
            v-if="searched"
            class="text-body-2"
          >
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
          <v-img
            :src="`${apiBaseURL}/api/dataset/1/item/${item.id}/thumbnail`"
            :lazy-src="`${apiBaseURL}/api/dataset/1/item/${item.id}/thumbnail`"
            :aspect-ratio="1"
            cover
            class="grey lighten-2"
            :title="`${item.id} - ${item.archiveTime}`"
            @click="expandImage(item)"
          >
            <template #placeholder>
              <div class="d-flex align-center justify-center fill-height">
                <v-progress-circular
                  indeterminate
                  color="grey-lighten-5"
                />
              </div>
            </template>
          </v-img>
        </v-col>
      </v-row>
      <v-row>
        <v-col>
          <div
            v-if="leftItems > 0"
            class="text-body-2 text-center"
          >
            <v-icon>
              mdi-chevron-double-down
            </v-icon>
            {{ leftItems }} images left (loading)
          </div>
        </v-col>
      </v-row>
    </v-container>
    <v-overlay
      v-model="overlay"
      class="align-center justify-center"
    >
      <v-card>
        <v-card-item>
          <v-card-title>{{ selectedItem.filename }}</v-card-title>
          <v-card-subtitle>{{ new Date(selectedItem.archiveTime).toLocaleDateString(undefined, dateFormatOptions) }}</v-card-subtitle>
        </v-card-item>
        <v-card-text>
          <v-img
            width="500"
            max-height="500"
            :src="selectedItem? `${apiBaseURL}/api/dataset/1/item/${selectedItem.id}/content` :''"
            @click="selectedItem = null" 
          />
        </v-card-text>
        <v-card-actions>
          <v-btn
            icon="mdi-download"
            :href="`${apiBaseURL}/api/dataset/1/item/${selectedItem.id}/content`"
          />
        </v-card-actions>
      </v-card>
    </v-overlay>
  </div>
</template>

<script>
export default {
  data () {
    return {
      apiBaseURL: null,
      searchString: '',
      totalItems: 0,
      items: [],
      currentPage: 0,
      pageSize: 24,
      totalPages: 0,
      searched: false,
      overlay: false,
      selectedItem: null,
      dateFormatOptions: {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      }
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
    const runtimeConfig = useRuntimeConfig()
    this.apiBaseURL = runtimeConfig.public.apiBaseURL
  },

  methods: {
    async getItems () {
      const newItems = await $fetch(
        `${this.apiBaseURL}/api/dataset/1/item?type=image&size=${this.pageSize}&page=${this.currentPage}&search=${this.searchString} type:I`
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
    },

    expandImage (item) {
      this.overlay = true
      this.selectedItem = item
    }
  }
}
</script>

<style scoped>
.with-pointer {
  cursor: pointer;
}
.no-opacity {
  opacity: 1;
}
.download-link {
  width: 100%;
}
</style>
