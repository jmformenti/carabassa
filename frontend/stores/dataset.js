import { defineStore } from 'pinia'

export const useDatasetStore = defineStore('dataset', {
  state: () => ({
    dataset: null,
    datasetsLoaded: false,
    listState: {
      datasetName: null,
      items: [],
      currentPage: 0,
      totalItems: 0,
      totalPages: 0,
      searchString: '',
      sortField: 'archiveTime',
      sortDirection: 'desc',
      scrollPosition: 0,
      searched: false
    }
  }),
  actions: {
    setListState(state) {
      this.listState = { ...this.listState, ...state };
    },
    resetListState() {
      this.listState = {
        datasetName: null,
        items: [],
        currentPage: 0,
        totalItems: 0,
        totalPages: 0,
        searchString: '',
        sortField: 'archiveTime',
        sortDirection: 'desc',
        scrollPosition: 0,
        searched: false
      };
    }
  }
});
