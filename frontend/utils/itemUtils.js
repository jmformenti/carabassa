export const isVideo = (item) => {
  return item && item.type && item.type.toLowerCase() === 'video'
}

export const formatSize = (bytes) => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

export const formatDate = (dateString, options = {
  weekday: 'long',
  year: 'numeric',
  month: 'long',
  day: 'numeric'
}) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleDateString(undefined, options)
}

export const parseTagValue = (val) => {
  if (val === null || val === undefined) return ''
  if (typeof val === 'boolean') return val.toString()
  if (typeof val === 'number') {
    // Only treat as date if it's a very large number (likely epoch ms > year 2000)
    if (val > 946684800000) {
      const d = new Date(val)
      const iso = d.toISOString()
      return iso.endsWith('T00:00:00.000Z') || iso.endsWith('T00:00:00Z') ? iso.split('T')[0] : d.toLocaleString()
    }
    return val.toString()
  }

  const strVal = String(val)
  // Check for YYYY-MM-DD or ISO pattern
  const isDateString = /^\d{4}-\d{2}-\d{2}/.test(strVal)

  if (isDateString) {
    const d = new Date(strVal)
    if (!isNaN(d.getTime())) {
      const iso = d.toISOString()
      if (iso.endsWith('T00:00:00.000Z') || iso.endsWith('T00:00:00Z') || strVal.includes('T00:00:00')) {
        return iso.split('T')[0]
      }
      return d.toLocaleString()
    }
  }
  return strVal
}
