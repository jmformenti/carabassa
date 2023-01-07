import { defineNuxtPlugin } from "nuxt/app"
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import colors from 'vuetify/lib/util/colors'

export default defineNuxtPlugin(nuxtApp => {
  const vuetify = createVuetify({
    components,
    directives,
    customVariables: ['~/assets/variables.scss'],
    theme: {
      themes: {
        light: {
          dark: false,
          colors: {
            primary: colors.orange.darken1,
            accent: colors.orange.accent4,
            secondary: colors.orange.lighten2,
            info: colors.teal.lighten1,
            warning: colors.amber.base,
            error: colors.deepOrange.accent4,
            success: colors.green.accent3
          }
        }
      }
    }
  })

  nuxtApp.vueApp.use(vuetify)
})
