<template>
  <v-container>
    <v-row justify="center">
      <v-col
        cols="12"
        sm="8"
        md="6"
      >
        <v-card class="elevation-4 mt-12 rounded-lg">
          <v-toolbar
            color="orange-darken-1"
            dark
            flat
          >
            <v-toolbar-title>User Profile</v-toolbar-title>
          </v-toolbar>
          <v-card-text class="pt-6">
            <div class="d-flex align-center mb-6">
              <v-avatar
                color="orange-lighten-2"
                size="64"
                class="mr-4"
              >
                <v-icon
                  size="40"
                  color="white"
                >
                  mdi-account
                </v-icon>
              </v-avatar>
              <div>
                <div class="text-h5 font-weight-bold">{{ authStore.user }}</div>
                <div class="text-subtitle-1 text-grey-darken-1">{{ authStore.role }}</div>
              </div>
            </div>

            <v-divider class="mb-6" />

            <h3 class="text-h6 mb-4 orange--text text--darken-4">Change Password</h3>
            <v-form
              ref="form"
              v-model="valid"
              lazy-validation
              @submit.prevent="updatePassword"
            >
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

              <v-btn
                :disabled="!valid"
                color="orange-darken-4"
                class="mt-4 white--text"
                block
                :loading="loading"
                type="submit"
              >
                Update Password
              </v-btn>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '~/stores/auth'
import { useNuxtApp } from '#app'
import AWN from 'awesome-notifications'

const authStore = useAuthStore()
const { $carabassa } = useNuxtApp()
const notifier = new AWN()

const form = ref(null)
const valid = ref(true)
const loading = ref(false)
const password = ref('')
const confirmPassword = ref('')

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
  } catch (err) {
    notifier.alert('Error updating password: ' + (err.message || 'Unknown error'))
  } finally {
    loading.value = false
  }
}
</script>
