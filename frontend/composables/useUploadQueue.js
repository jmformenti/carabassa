const DB_NAME = 'carabassa-uploads'
const DB_VERSION = 2
const STORE_NAME = 'queue'
const HANDLE_STORE = 'handles'
const DIRECTORY_HANDLE_ID = 'directory'

function openDB() {
  return new Promise((resolve, reject) => {
    if (typeof indexedDB === 'undefined') {
      reject(new Error('IndexedDB not available'))
      return
    }
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = (e) => {
      const db = e.target.result
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'id' })
      }
      if (!db.objectStoreNames.contains(HANDLE_STORE)) {
        db.createObjectStore(HANDLE_STORE, { keyPath: 'id' })
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

async function dbGetAll() {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly')
    const req = tx.objectStore(STORE_NAME).getAll()
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

async function dbPut(item) {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const req = tx.objectStore(STORE_NAME).put(item)
    req.onsuccess = () => resolve()
    req.onerror = () => reject(req.error)
  })
}

async function dbDelete(id) {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const req = tx.objectStore(STORE_NAME).delete(id)
    req.onsuccess = () => resolve()
    req.onerror = () => reject(req.error)
  })
}

async function dbClear() {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const req = tx.objectStore(STORE_NAME).clear()
    req.onsuccess = () => resolve()
    req.onerror = () => reject(req.error)
  })
}
async function dbPutHandle(id, handle) {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(HANDLE_STORE, 'readwrite')
    const req = tx.objectStore(HANDLE_STORE).put({ id, handle })
    req.onsuccess = () => resolve()
    req.onerror = () => reject(req.error)
  })
}

async function dbGetHandle(id) {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(HANDLE_STORE, 'readonly')
    const req = tx.objectStore(HANDLE_STORE).get(id)
    req.onsuccess = () => resolve(req.result?.handle || null)
    req.onerror = () => reject(req.error)
  })
}

export const useUploadQueue = () => {
  function isFSASupported() {
    return typeof window !== 'undefined' && 'showDirectoryPicker' in window
  }

  async function pickDirectory() {
    if (!isFSASupported()) throw new Error('File System Access API not supported')
    const handle = await window.showDirectoryPicker({
      id: 'carabassa-directory',
      mode: 'read',
      startIn: 'documents'
    })
    await dbPutHandle(DIRECTORY_HANDLE_ID, handle)
    return handle
  }

  async function getSavedDirectory() {
    if (!isFSASupported()) return null
    return await dbGetHandle(DIRECTORY_HANDLE_ID)
  }

  async function getFilesFromHandle(handle) {
    if (!handle) return []
    if (handle.queryPermission && handle.requestPermission) {
      const perm = await handle.queryPermission({ mode: 'read' })
      if (perm !== 'granted') {
        const reqPerm = await handle.requestPermission({ mode: 'read' })
        if (reqPerm !== 'granted') {
          throw new Error('Permission denied')
        }
      }
    }

    const files = []
    for await (const entry of handle.values()) {
      if (entry.kind !== 'file') continue
      const file = await entry.getFile()
      if (file && file.type && /^(image|video)\//.test(file.type)) {
        files.push(file)
      }
    }
    files.sort((a, b) => a.name.localeCompare(b.name))
    return files
  }

  async function getPendingFromDB() {
    const all = await dbGetAll()
    return all.filter(item => item.status === 'pending')
  }

  async function saveToQueue(id, fileName, fileSize, lastModified) {
    await dbPut({ id, fileName, fileSize, lastModified, status: 'pending' })
  }

  async function markDone(id) {
    await dbDelete(id)
  }

  async function clearQueue() {
    await dbClear()
  }

  return {
    getPendingFromDB,
    saveToQueue,
    markDone,
    clearQueue,
    isFSASupported,
    pickDirectory,
    getSavedDirectory,
    getFilesFromHandle
  }
}
