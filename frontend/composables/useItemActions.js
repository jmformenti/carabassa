export const useItemActions = () => {
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
    buildItemLink,
    copyItemLink
  }
}
