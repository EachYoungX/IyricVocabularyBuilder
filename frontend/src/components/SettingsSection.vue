<template>
  <section class="settings-section">
    <SettingsSectionHeading :icon="icon" :title="title" :caption="caption" />
    <div class="settings-section-body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
import SettingsSectionHeading from 'components/SettingsSectionHeading.vue';

defineProps<{
  icon: string;
  title: string;
  caption: string;
}>();
</script>

<style scoped lang="scss">
.settings-section {
  padding: 22px 24px 10px;
  background: var(--lv-surface-solid);
  border: 1px solid var(--lv-line);
  border-radius: var(--lv-radius-md);
  box-shadow: var(--lv-shadow-soft);
}

.settings-section-body {
  margin-top: 12px;
}
</style>
