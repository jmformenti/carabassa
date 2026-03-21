import { $fetch } from 'ofetch'
import { useRuntimeConfig } from '#imports'

export class CarabassaService {
  constructor(datasetStore) {
    const runtimeConfig = useRuntimeConfig()
    this.apiBaseURL = runtimeConfig.public.apiBaseUrl || ''
    console.log(`API base url: ${this.apiBaseURL}`)
    this.datasetStore = datasetStore
    this.tagInfosCache = null
    this.tagInfosCachePromise = null
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

  getItems(currentPage, pageSize, searchString, sort) {
    const sortParam = sort ? `&sort=${sort}` : ''
    const searchParam = searchString ? `&search=${searchString}` : ''
    return $fetch(
      `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item?size=${pageSize}&page=${currentPage}${searchParam}${sortParam}`
    )
  }

  getTagInfos(page = 0, size = 200, { forceRefresh = false } = {}) {
    if (!forceRefresh && Array.isArray(this.tagInfosCache)) {
      return Promise.resolve(this.tagInfosCache)
    }

    if (!forceRefresh && this.tagInfosCachePromise) {
      return this.tagInfosCachePromise
    }

    this.tagInfosCachePromise = $fetch(
      `${this.apiBaseURL}/api/tag-info?page=${page}&size=${size}`
    ).then((data) => {
      let tagInfos = []
      if (data && data._embedded) {
        tagInfos = data._embedded.tagInfoEntityRepresentationList || []
      }
      this.tagInfosCache = tagInfos
      return tagInfos
    }).finally(() => {
      this.tagInfosCachePromise = null
    })

    return this.tagInfosCachePromise
  }

  getPublicTagInfos(page = 0, size = 200, options = {}) {
    return this.getTagInfos(page, size, options).then((tagInfos) =>
      tagInfos.filter((tagInfo) => !tagInfo.internal)
    )
  }

  getItem(itemId) {
    return $fetch(
      `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/${itemId}`
    )
  }

  addItemTag(itemId, tagRepresentation) {
    return $fetch(
      `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/${itemId}/tag`,
      {
        method: 'POST',
        body: tagRepresentation
      }
    )
  }

  getItemThumbnailURL(itemId) {
    return `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/${itemId}/thumbnail`
  }

  getItemContentURL(itemId) {
    return `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/${itemId}/content`
  }

  deleteItem(itemId) {
    return fetch(
      `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/${itemId}`,
      { method: 'DELETE' }
    ).then((response) => {
      if (!response.ok) {
        throw new Error(`Delete failed: ${response.status}`)
      }
    })
  }

  deleteItemTag(itemId, tagId) {
    return fetch(
      `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/${itemId}/tag/${tagId}`,
      { method: 'DELETE' }
    ).then((response) => {
      if (!response.ok) {
        throw new Error(`Delete tag failed: ${response.status}`)
      }
    })
  }
}
