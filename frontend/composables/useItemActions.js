export const useItemActions = () => {
  const { $carabassa } = useNuxtApp()
  const authStore = useAuthStore()

  const getFavoriteTag = (item) => {
    if (!item || !item.tags || !authStore.username) return null
    return item.tags.find(t => t.name === 'favorite' && t.value === authStore.username)
  }

  const isFavorite = (item) => {
    if (item?.favorite !== undefined) return item.favorite
    return !!getFavoriteTag(item)
  }

  const toggleFavorite = async (item) => {
    if (!item || !authStore.username) return

    if (item.favorite && (!Array.isArray(item.tags) || item.tags.length === 0)) {
      const detailed = await $carabassa.getItem(item)
      Object.assign(item, detailed)
    }

    const favTag = getFavoriteTag(item)
    if (favTag) {
      await $carabassa.deleteItemTag(item.id, favTag.id)
      if (item.tags) {
        item.tags = item.tags.filter(t => t.id !== favTag.id)
      }
      if ('favorite' in item) item.favorite = false
    } else {
      const resp = await $carabassa.addItemTag(item, {
        name: 'favorite',
        value: authStore.username
      })
      if (!item.tags) item.tags = []
      item.tags.push({
        id: resp.id,
        name: 'favorite',
        value: authStore.username
      })
      if ('favorite' in item) item.favorite = true
    }
  }

  const buildItemLink = ({ datasetName, itemId, origin } = {}) => {
    const baseOrigin = origin || (typeof window !== 'undefined' ? window.location.origin : '')
    if (!baseOrigin || !datasetName || !itemId) return null
    return `${baseOrigin}/dataset/${encodeURIComponent(datasetName)}/item/${encodeURIComponent(itemId)}`
  }

  const copyText = async (text) => {
    if (!text) return false

    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(text)
      return true
    }

    return false
  }

  const copyItemLink = async ({ datasetName, itemId, fallbackUrl } = {}) => {
    try {
      const link = buildItemLink({ datasetName, itemId }) || fallbackUrl
      return await copyText(link)
    } catch (err) {
      console.error('Failed to copy text: ', err)
      return false
    }
  }

  return {
    isFavorite,
    toggleFavorite,
    buildItemLink,
    copyItemLink
  }
}
