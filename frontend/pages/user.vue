<template>
  <v-container>
    <v-row>
      <v-col
        cols="12"
        sm="10"
        md="8"
        lg="7"
      >
        <div class="mt-10">
          <h2 class="text-h5 text-orange-darken-4 mb-4">Profile</h2>

          <div class="d-flex align-center mb-4">
            <v-avatar
              color="orange-lighten-2"
              size="56"
              class="mr-4"
            >
              <v-icon
                size="34"
                color="white"
              >
                mdi-account
              </v-icon>
            </v-avatar>
            <div>
              <div class="text-h8 font-weight-bold">{{ authStore.username }}</div>
              <div class="text-caption text-grey-darken-1">
                Role: {{ authStore.role || 'Unknown' }}
              </div>
            </div>
          </div>

          <v-btn
            variant="text"
            class="text-decoration-underline px-0 text-body-2"
            color="orange-darken-4"
            @click="dialog = true"
          >
            Change password
          </v-btn>

          <div class="mt-6 text-body-2 text-grey-darken-1">
            Default dataset:
            <v-btn
              variant="text"
              class="text-decoration-underline px-1 text-body-2"
              color="orange-darken-4"
              @click="openDefaultDatasetDialog"
            >
              {{ authStore.defaultDataset || 'Not set' }}
            </v-btn>
          </div>

          <v-dialog
            v-model="dialog"
            max-width="460"
          >
            <v-card class="rounded-lg">
              <v-card-title class="text-h6">Change Password</v-card-title>
              <v-form
                ref="form"
                v-model="valid"
                lazy-validation
                @submit.prevent="updatePassword"
              >
                <v-card-text>
                  <v-text-field
                    v-model="password"
                    color="orange-darken-4"
                    label="New Password"
                    prepend-inner-icon="mdi-lock"
                    type="password"
                    :rules="passwordRules"
                    required
                  />
                  <v-text-field
                    v-model="confirmPassword"
                    color="orange-darken-4"
                    label="Confirm New Password"
                    prepend-inner-icon="mdi-lock-check"
                    type="password"
                    :rules="confirmPasswordRules"
                    required
                  />
                </v-card-text>
                <v-card-actions>
                  <v-spacer />
                  <v-btn
                    color="orange-darken-4"
                    @click="dialog = false"
                  >
                    Cancel
                  </v-btn>
                  <v-btn
                    :disabled="!valid"
                    color="orange-darken-4"
                    :loading="loading"
                    type="submit"
                  >
                    Update
                  </v-btn>
                  <v-spacer />
                </v-card-actions>
              </v-form>
            </v-card>
          </v-dialog>

          <v-dialog
            v-model="defaultDatasetDialog"
            max-width="460"
          >
            <v-card class="rounded-lg">
              <v-card-title class="text-h6">Default Dataset</v-card-title>
              <v-card-text>
                <v-select
                  v-model="defaultDataset"
                  :items="datasets"
                  item-title="name"
                  item-value="name"
                  label="Dataset shown after login"
                  clearable
                  color="orange-darken-4"
                  class="mb-2"
                />
              </v-card-text>
              <v-card-actions>
                <v-spacer />
                <v-btn
                  color="orange-darken-4"
                  @click="defaultDatasetDialog = false"
                >
                  Cancel
                </v-btn>
                <v-btn
                  color="orange-darken-4"
                  :loading="savingDefaultDataset"
                  :disabled="savingDefaultDataset"
                  @click="updateDefaultDataset"
                >
                  Save
                </v-btn>
                <v-spacer />
              </v-card-actions>
            </v-card>
          </v-dialog>
        </div>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '~/stores/auth'
import { useNuxtApp } from '#app'
import AWN from 'awesome-notifications'

const authStore = useAuthStore()
const { $carabassa } = useNuxtApp()
const notifier = new AWN()

const form = ref(null)
const valid = ref(true)
const loading = ref(false)
const dialog = ref(false)
const defaultDatasetDialog = ref(false)
const password = ref('')
const confirmPassword = ref('')
const datasets = ref([])
const defaultDataset = ref(authStore.defaultDataset || null)
const savingDefaultDataset = ref(false)

const passwordRules = [
  v => !!v || 'Password is required',
  v => (v && v.length >= 4) || 'Password must be at least 4 characters'
]

const confirmPasswordRules = [
  v => !!v || 'Confirmation is required',
  v => v === password.value || 'Passwords must match'
]

const updatePassword = async () => {
  loading.value = true
  try {
    await $carabassa.changeMyPassword(password.value)
    notifier.success('Password updated successfully')
    if (form.value) {
      form.value.reset()
    }
    dialog.value = false
  } catch (err) {
    notifier.alert('Error updating password: ' + (err.message || 'Unknown error'))
  } finally {
    loading.value = false
  }
}

const loadDatasets = async () => {
  try {
    datasets.value = await $carabassa.getDatasets()
  } catch (err) {
    notifier.alert('Error loading datasets: ' + (err.message || 'Unknown error'))
  }
}

const updateDefaultDataset = async () => {
  savingDefaultDataset.value = true
  try {
    await $carabassa.changeMyDefaultDataset(defaultDataset.value || null)
    authStore.defaultDataset = defaultDataset.value || null
    if (import.meta.client) {
      localStorage.setItem(
        'carabassa_auth',
        JSON.stringify({
          token: authStore.token,
          username: authStore.username,
          role: authStore.role,
          defaultDataset: authStore.defaultDataset,
        })
      )
    }
    notifier.success('Default dataset updated successfully')
    defaultDatasetDialog.value = false
  } catch (err) {
    notifier.alert('Error updating default dataset: ' + (err.message || 'Unknown error'))
  } finally {
    savingDefaultDataset.value = false
  }
}

const openDefaultDatasetDialog = () => {
  defaultDataset.value = authStore.defaultDataset || null
  defaultDatasetDialog.value = true
}

onMounted(() => {
  loadDatasets()
})
</script>
