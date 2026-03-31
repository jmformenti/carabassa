<template>
  <div>
    <v-row class="mb-4">
      <v-col class="d-flex align-center">
        <h1 class="text-h4">User Management</h1>
        <v-spacer />
        <v-btn
          color="orange-darken-2"
          variant="tonal"
          prepend-icon="mdi-plus"
          class="ml-2"
          @click="openCreateDialog"
        >
          Add User
        </v-btn>
      </v-col>
    </v-row>

    <v-card>
      <v-toolbar
        color="orange-darken-1"
        dark
        flat
      >
        <v-toolbar-title>User List</v-toolbar-title>
      </v-toolbar>
      <v-data-table
        :headers="headers"
        :items="users"
        :loading="loading"
        class="elevation-1"
      >
        <template #[`item.enabled`]="{ item }">
          <v-chip
            :color="item.enabled ? 'success' : 'error'"
            size="small"
          >
            {{ item.enabled ? 'Active' : 'Disabled' }}
          </v-chip>
        </template>

        <template #[`item.createdAt`]="{ item }">
          {{ formatDate(item.createdAt) }}
        </template>

        <template #[`item.actions`]="{ item }">
          <v-icon
            small
            class="mr-2"
            color="orange-darken-2"
            @click="editUser(item)"
          >
            mdi-pencil
          </v-icon>
          <v-icon
            v-if="item.username !== 'admin'"
            small
            color="orange-darken-4"
            @click="confirmDelete(item)"
          >
            mdi-delete
          </v-icon>
        </template>
      </v-data-table>
    </v-card>

    <!-- Create/Edit Dialog -->
    <v-dialog
      v-model="dialog"
      max-width="500px"
    >
      <v-card>
        <v-card-title>
          <span class="text-h5">{{ formTitle }}</span>
        </v-card-title>

        <v-card-text>
          <v-container>
            <v-row>
              <v-col cols="12">
                <v-text-field
                  v-model="editedItem.username"
                  label="Username"
                  required
                />
              </v-col>
              <v-col cols="12">
                <v-text-field
                  v-model="editedItem.password"
                  label="Password"
                  type="password"
                  :placeholder="editedIndex === -1 ? '' : '(leave blank to keep current)'"
                />
              </v-col>
              <v-col cols="12" sm="6">
                <v-select
                  v-model="editedItem.role"
                  :items="['USER', 'ADMIN', 'TAGGER']"
                  label="Role"
                  required
                />
              </v-col>
              <v-col cols="12" sm="6">
                <v-switch
                  v-model="editedItem.enabled"
                  label="Enabled"
                  color="success"
                />
              </v-col>
            </v-row>
          </v-container>
        </v-card-text>

        <v-card-actions>
          <v-spacer />
          <v-btn
            color="orange-darken-4"
            variant="text"
            @click="close"
          >
            Cancel
          </v-btn>
          <v-btn
            color="orange-darken-4"
            variant="text"
            :loading="saving"
            @click="save"
          >
            Save
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Delete Confirmation -->
    <v-dialog
      v-model="dialogDelete"
      max-width="500px"
    >
      <v-card>
        <v-card-title class="text-h5 text-center">Are you sure you want to delete this user?</v-card-title>
        <v-card-actions>
          <v-spacer />
          <v-btn
            color="orange-darken-4"
            variant="text"
            @click="closeDelete"
          >
            Cancel
          </v-btn>
          <v-btn
            color="error"
            variant="text"
            :loading="deleting"
            @click="deleteItemConfirm"
          >
            OK
          </v-btn>
          <v-spacer />
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useNuxtApp } from '#app'
import AWN from 'awesome-notifications'

const { $carabassa } = useNuxtApp()
const notifier = new AWN()

const loading = ref(false)
const dialog = ref(false)
const dialogDelete = ref(false)
const saving = ref(false)
const deleting = ref(false)

const users = ref([])
const headers = [
  { title: 'ID', key: 'id' },
  { title: 'Username', key: 'username' },
  { title: 'Role', key: 'role' },
  { title: 'Status', key: 'enabled' },
  { title: 'Created At', key: 'createdAt' },
  { title: 'Actions', key: 'actions', sortable: false }
]

const editedIndex = ref(-1)
const editedItem = ref({
  username: '',
  password: '',
  role: 'USER',
  enabled: true
})
const defaultItem = {
  username: '',
  password: '',
  role: 'USER',
  enabled: true
}

const formTitle = computed(() => {
  return editedIndex.value === -1 ? 'New User' : 'Edit User'
})

const fetchUsers = async () => {
  loading.value = true
  try {
    users.value = await $carabassa.getUsers()
  } catch (err) {
    notifier.alert('Failed to load users: ' + (err.message || 'Unknown error'))
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  editedIndex.value = -1
  editedItem.value = Object.assign({}, defaultItem)
  dialog.value = true
}

const editUser = (item) => {
  editedIndex.value = users.value.indexOf(item)
  editedItem.value = Object.assign({}, item)
  editedItem.value.password = '' // Don't show hash or current password
  dialog.value = true
}

const confirmDelete = (item) => {
  editedIndex.value = users.value.indexOf(item)
  editedItem.value = Object.assign({}, item)
  dialogDelete.value = true
}

const close = () => {
  dialog.value = false
  setTimeout(() => {
    editedItem.value = Object.assign({}, defaultItem)
    editedIndex.value = -1
  }, 300)
}

const closeDelete = () => {
  dialogDelete.value = false
  setTimeout(() => {
    editedItem.value = Object.assign({}, defaultItem)
    editedIndex.value = -1
  }, 300)
}

const save = async () => {
  saving.value = true
  try {
    if (editedIndex.value > -1) {
      await $carabassa.updateUser(editedItem.value.id, editedItem.value)
      notifier.success('User updated successfully')
    } else {
      await $carabassa.createUser(editedItem.value)
      notifier.success('User created successfully')
    }
    close()
    await fetchUsers()
  } catch (err) {
    notifier.alert('Error: ' + (err.message || 'Action failed'))
  } finally {
    saving.value = false
  }
}

const deleteItemConfirm = async () => {
  deleting.value = true
  try {
    await $carabassa.deleteUser(editedItem.value.id)
    notifier.success('User deleted successfully')
    closeDelete()
    await fetchUsers()
  } catch (err) {
    notifier.alert('Error: ' + (err.message || 'Delete failed'))
  } finally {
    deleting.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

onMounted(fetchUsers)
</script>
