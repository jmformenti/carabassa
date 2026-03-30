<template>
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>

<style>
/* Global overlay blur for premium feel */
.v-overlay__scrim {
  backdrop-filter: blur(10px) !important;
  -webkit-backdrop-filter: blur(10px) !important;
  background-color: rgba(0, 0, 0, 0.65) !important;
  opacity: 1 !important;
}
</style>
