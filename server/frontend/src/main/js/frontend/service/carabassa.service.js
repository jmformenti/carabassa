import { $fetch } from 'ofetch'
import { useRuntimeConfig } from '#imports'

export class CarabassaService {
  constructor(datasetStore) {
    const runtimeConfig = useRuntimeConfig()
    this.apiBaseURL = runtimeConfig.public.apiBaseURL
    this.datasetStore = datasetStore
  }

  getDatasetByName(datasetName) {
    return $fetch(
      `${this.apiBaseURL}/api/dataset/name/${datasetName}`
    ).then((data) => {
      return Promise.resolve(data)
    })
  }

  getDatasets() {
    return $fetch(
      `${this.apiBaseURL}/api/dataset`
    ).then((data) => {
      let datasets = []
      if (data._embedded) {
        datasets = data._embedded.datasetEntityRepresentationList
      }
      return Promise.resolve(datasets)
    })
  }

  getItems(currentPage, pageSize, searchString) {
    return $fetch(
      `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item?size=${pageSize}&page=${currentPage}&search=${searchString} type:I`
    )
  }

  getItemThumbnailURL(itemId) {
    return `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/${itemId}/thumbnail`
  }

  getItemContentURL(itemId) {
    return `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/${itemId}/content`
  }
}