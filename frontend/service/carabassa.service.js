import { $fetch } from 'ofetch'
import { useRuntimeConfig } from '#imports'

export class CarabassaService {
  constructor(datasetStore, authStore) {
    const runtimeConfig = useRuntimeConfig()
    this.apiBaseURL = runtimeConfig.public.apiBaseUrl || ''
    console.log(`API base url: ${this.apiBaseURL}`)
    this.datasetStore = datasetStore
    this.authStore = authStore
    this.tagInfosCache = null
    this.tagInfosCachePromise = null
  }

  _headers() {
    return this.authStore ? this.authStore.authHeaders : {}
  }

  getDatasetByName(datasetName) {
    return $fetch(
      `${this.apiBaseURL}/api/dataset/name/${datasetName}`,
      { headers: this._headers() }
    ).then((data) => {
      return Promise.resolve(data)
    })
  }

  getDatasets() {
    return $fetch(
      `${this.apiBaseURL}/api/dataset`,
      { headers: this._headers() }
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
      `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item?size=${pageSize}&page=${currentPage}${searchParam}${sortParam}`,
      { headers: this._headers() }
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
      `${this.apiBaseURL}/api/tag-info?page=${page}&size=${size}`,
      { headers: this._headers() }
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
      `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/${itemId}`,
      { headers: this._headers() }
    )
  }

  async itemExists(hash) {
    const response = await fetch(
      `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/exists/${hash}`,
      { headers: this._headers() }
    )
    if (response.status === 404) return false
    if (!response.ok) {
      let msg = `Error ${response.status}`
      try {
        const body = await response.json()
        if (body && body.message) msg = body.message
      } catch (_) { /* ignore */ }
      throw new Error(msg)
    }
    return true
  }

  addItemTag(itemId, tagRepresentation) {
    return $fetch(
      `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/${itemId}/tag`,
      {
        method: 'POST',
        headers: this._headers(),
        body: tagRepresentation
      }
    )
  }

  getItemThumbnailURL(itemId) {
    const url = `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/${itemId}/thumbnail`
    if (this.authStore && this.authStore.token) {
      return `${url}?token=${this.authStore.token}`
    }
    return url
  }

  getItemContentURL(itemId) {
    const url = `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/${itemId}/content`
    if (this.authStore && this.authStore.token) {
      return `${url}?token=${this.authStore.token}`
    }
    return url
  }

  deleteItem(itemId) {
    return fetch(
      `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/${itemId}`,
      { method: 'DELETE', headers: this._headers() }
    ).then((response) => {
      if (!response.ok) {
        throw new Error(`Delete failed: ${response.status}`)
      }
    })
  }

  deleteItemTag(itemId, tagId) {
    return fetch(
      `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/${itemId}/tag/${tagId}`,
      { method: 'DELETE', headers: this._headers() }
    ).then((response) => {
      if (!response.ok) {
        throw new Error(`Delete tag failed: ${response.status}`)
      }
    })
  }

  async addItem(file) {
    const formData = new FormData()
    formData.append('file', file, file.name)
    const response = await fetch(
      `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item`,
      { method: 'POST', headers: this._headers(), body: formData }
    )
    if (response.status === 409) {
      // Duplicate - item already exists
      const err = new Error('Duplicate')
      err.isDuplicate = true
      throw err
    }
    if (!response.ok) {
      let msg = `Error ${response.status}`
      try {
        const body = await response.json()
        if (body && body.message) msg = body.message
      } catch (_) { /* ignore */ }
      throw new Error(msg)
    }
    return response.json()
  }

  getItemTagValues(tagName, page = 0, size = 100) {
    return $fetch(
      `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/tag/${tagName}/values?page=${page}&size=${size}`,
      { headers: this._headers() }
    ).then((data) => {
      if (data.content) return data.content
      if (data._embedded && data._embedded.stringList) {
        return data._embedded.stringList
      }
      return []
    })
  }

  // -------------------------------------------------------------------------
  // User Management (Admin only)
  // -------------------------------------------------------------------------

  getUsers() {
    return $fetch(
      `${this.apiBaseURL}/api/auth/users`,
      { headers: this._headers() }
    )
  }

  createUser(user) {
    return $fetch(
      `${this.apiBaseURL}/api/auth/users`,
      {
        method: 'POST',
        headers: this._headers(),
        body: user
      }
    )
  }

  updateUser(id, user) {
    return $fetch(
      `${this.apiBaseURL}/api/auth/users/${id}`,
      {
        method: 'PUT',
        headers: this._headers(),
        body: user
      }
    )
  }

  deleteUser(id) {
    return $fetch(
      `${this.apiBaseURL}/api/auth/users/${id}`,
      {
        method: 'DELETE',
        headers: this._headers()
      }
    )
  }

  changeMyPassword(password) {
    return $fetch(
      `${this.apiBaseURL}/api/auth/me/password`,
      {
        method: 'PUT',
        headers: this._headers(),
        body: { password }
      }
    )
  }
}
