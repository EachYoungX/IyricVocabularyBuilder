<template>
  <div class="setting-row">
    <div class="setting-copy">
      <div class="setting-title">{{ title }}</div>
      <div v-if="description" class="setting-description">{{ description }}</div>
    </div>
    <div class="setting-control">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
}>();
</script>

<style scoped lang="scss">
.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 72px;
  padding: 14px 0;
  border-bottom: 1px solid var(--lv-line);
}

.setting-row:last-child {
  border-bottom: 0;
}

.setting-copy {
  min-width: 0;
}

.setting-title {
  color: var(--lv-ink);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

.setting-description {
  max-width: 620px;
  margin-top: 4px;
  color: var(--lv-ink-soft);
  font-size: 13px;
  line-height: 1.5;
}

.setting-control {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
}

@media (max-width: 700px) {
  .setting-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

  .setting-control {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
