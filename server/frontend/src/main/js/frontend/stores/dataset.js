import { defineStore } from 'pinia'

export const useDatasetStore = defineStore('dataset', {
  state: () => ({
    dataset: null,
    datasetsLoaded: false
  }),
  actions: {
  }
});
