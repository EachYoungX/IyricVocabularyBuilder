<template>
  <router-view v-slot="{ Component, route }">
    <transition name="lv-page" mode="out-in" appear>
      <component :is="Component" :key="route.fullPath" />
    </transition>
  </router-view>
  <FirstLaunchDialog />
</template>

<script setup lang="ts">
import FirstLaunchDialog from 'components/FirstLaunchDialog.vue';
</script>
