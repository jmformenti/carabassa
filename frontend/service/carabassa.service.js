import { $fetch } from 'ofetch'
import { useRuntimeConfig } from '#imports'

export class CarabassaService {
  constructor(datasetStore, authStore) {
    const runtimeConfig = useRuntimeConfig()
    this.apiBaseURL = runtimeConfig.public.apiBaseUrl || ''
    this.datasetStore = datasetStore
    this.authStore = authStore
    this.tagInfosCache = null
    this.tagInfosCachePromise = null
    this.fetch = $fetch.create({
      onResponseError: ({ response }) => {
        if (response && response.status === 401) {
          this._handleUnauthorized()
        }
      }
    })
  }

  _handleUnauthorized() {
    if (this.authStore) {
      this.authStore.logout()
    }
    if (typeof window !== 'undefined') {
      window.location.href = '/login'
    }
  }

  _headers() {
    return this.authStore ? this.authStore.authHeaders : {}
  }

  getDatasetByName(datasetName) {
    return this.fetch(
      `${this.apiBaseURL}/api/dataset/name/${datasetName}`,
      { headers: this._headers() }
    ).then((data) => {
      return Promise.resolve(data)
    })
  }

  getDatasets() {
    return this.fetch(
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
    const query = {
      size: pageSize,
      page: currentPage
    }
    if (searchString) {
      query.search = searchString
    }
    if (sort) {
      query.sort = sort
    }
    return this.fetch(
      `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item`,
      {
        headers: this._headers(),
        query: query
      }
    )
  }

  getFavorites(currentPage, pageSize, sort) {
    const query = {
      size: pageSize,
      page: currentPage
    }
    if (sort) {
      query.sort = sort
    }
    return this.fetch(
      `${this.apiBaseURL}/api/user/favorite`,
      {
        headers: this._headers(),
        query: query
      }
    )
  }

  getTagInfos(page = 0, size = 200, { forceRefresh = false } = {}) {
    if (!forceRefresh && Array.isArray(this.tagInfosCache)) {
      return Promise.resolve(this.tagInfosCache)
    }

    if (!forceRefresh && this.tagInfosCachePromise) {
      return this.tagInfosCachePromise
    }

    this.tagInfosCachePromise = this.fetch(
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

  getItem(item) {
    return this.fetch(
      `${this.apiBaseURL}/api/dataset/${item.datasetId}/item/${item.id}`,
      { headers: this._headers() }
    )
  }

  async itemExists(hash) {
    try {
      await this.fetch(
        `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/exists/${hash}`,
        { headers: this._headers() }
      )
      return true
    } catch (err) {
      if (err?.response?.status === 404) return false
      if (err?.data?.message) {
        throw new Error(err.data.message)
      }
      if (err?.response?.status) {
        throw new Error(`Error ${err.response.status}`)
      }
      throw err
    }
  }

  addItemTag(item, tagRepresentation) {
    return this.fetch(
      `${this.apiBaseURL}/api/dataset/${item.datasetId}/item/${item.id}/tag`,
      {
        method: 'POST',
        headers: this._headers(),
        body: tagRepresentation
      }
    )
  }

  getItemThumbnailURL(item) {
    const url = `${this.apiBaseURL}/api/dataset/${item.datasetId}/item/${item.id}/thumbnail`
    if (this.authStore && this.authStore.token) {
      return `${url}?token=${this.authStore.token}`
    }
    return url
  }

  getItemContentURL(item) {
    const url = `${this.apiBaseURL}/api/dataset/${item.datasetId}/item/${item.id}/content`
    if (this.authStore && this.authStore.token) {
      return `${url}?token=${this.authStore.token}`
    }
    return url
  }

  async deleteItem(item) {
    try {
      await this.fetch(
        `${this.apiBaseURL}/api/dataset/${item.datasetId}/item/${item.id}`,
        { method: 'DELETE', headers: this._headers() }
      )
    } catch (err) {
      if (err?.data?.message) {
        throw new Error(err.data.message)
      }
      if (err?.response?.status) {
        throw new Error(`Delete failed: ${err.response.status}`)
      }
      throw err
    }
  }

  async deleteItemTag(itemId, tagId) {
    try {
      await this.fetch(
        `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/${itemId}/tag/${tagId}`,
        { method: 'DELETE', headers: this._headers() }
      )
    } catch (err) {
      if (err?.data?.message) {
        throw new Error(err.data.message)
      }
      if (err?.response?.status) {
        throw new Error(`Delete tag failed: ${err.response.status}`)
      }
      throw err
    }
  }

  async addItem(file) {
    const formData = new FormData()
    formData.append('file', file, file.name)
    try {
      return await this.fetch(
        `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item`,
        { method: 'POST', headers: this._headers(), body: formData }
      )
    } catch (err) {
      if (err?.response?.status === 409) {
        const duplicate = new Error('Duplicate')
        duplicate.isDuplicate = true
        throw duplicate
      }
      if (err?.data?.message) {
        throw new Error(err.data.message)
      }
      if (err?.response?.status) {
        throw new Error(`Error ${err.response.status}`)
      }
      throw err
    }
  }

  async getItemTagValues(tagName) {
    let currentPage = 0;
    let totalPages = 1;
    let allValues = [];
    const size = 100;

    while (currentPage < totalPages) {
      const data = await this.fetch(
        `${this.apiBaseURL}/api/dataset/${this.datasetStore.dataset.id}/item/tag/${tagName}/values`,
        {
          headers: this._headers(),
          query: {
            page: currentPage,
            size: size
          }
        }
      );
      const values = data.content || (data._embedded && data._embedded.stringList) || [];
      allValues = allValues.concat(values);
      totalPages = data.page ? data.page.totalPages : 1;
      currentPage++;
    }
    allValues.sort((a, b) => String(a || '').localeCompare(String(b || ''), undefined, { sensitivity: 'base' }));
    return allValues;
  }

  changeMyDefaultDataset(defaultDataset) {
    return this.fetch(
      `${this.apiBaseURL}/api/auth/me/default-dataset`,
      {
        method: 'PUT',
        headers: this._headers(),
        body: { defaultDataset }
      }
    )
  }

  refreshToken() {
    return this.fetch(
      `${this.apiBaseURL}/api/auth/refresh`,
      {
        method: 'POST',
        headers: this._headers()
      }
    )
  }

  // -------------------------------------------------------------------------
  // User Management (Admin only)
  // -------------------------------------------------------------------------

  getUsers() {
    return this.fetch(
      `${this.apiBaseURL}/api/auth/users`,
      { headers: this._headers() }
    )
  }

  createUser(user) {
    return this.fetch(
      `${this.apiBaseURL}/api/auth/users`,
      {
        method: 'POST',
        headers: this._headers(),
        body: user
      }
    )
  }

  updateUser(id, user) {
    return this.fetch(
      `${this.apiBaseURL}/api/auth/users/${id}`,
      {
        method: 'PUT',
        headers: this._headers(),
        body: user
      }
    )
  }

  deleteUser(id) {
    return this.fetch(
      `${this.apiBaseURL}/api/auth/users/${id}`,
      {
        method: 'DELETE',
        headers: this._headers()
      }
    )
  }

  changeMyPassword(password) {
    return this.fetch(
      `${this.apiBaseURL}/api/auth/me/password`,
      {
        method: 'PUT',
        headers: this._headers(),
        body: { password }
      }
    )
  }
}
