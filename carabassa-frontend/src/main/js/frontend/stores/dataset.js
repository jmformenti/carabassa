import { defineStore } from 'pinia'

export const useDatasetStore = defineStore('dataset', {
  state: () => ({
    dataset: { name: '' }
  }),
  actions: {
  }
});
