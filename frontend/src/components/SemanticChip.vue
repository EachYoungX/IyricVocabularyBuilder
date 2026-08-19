<template>
  <q-chip dense :class="['semantic-chip', `semantic-chip--${tone}`]">
    <slot />
  </q-chip>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  tone?: 'neutral' | 'status' | 'warning' | 'excluded' | 'count';
}>(), {
  tone: 'neutral',
});
</script>

<style scoped lang="scss">
.semantic-chip {
  min-height: 26px;
  padding: 0 9px;
  border: 1px solid var(--lv-line);
  border-radius: 999px;
  color: var(--lv-ink);
  background: var(--lv-surface-muted);
  font-size: 12px;
  line-height: 1.2;
}

.semantic-chip--status {
  color: var(--lv-drawer-active-ink);
  background: var(--lv-drawer-active-bg);
  border-color: var(--lv-line-strong);
}

.semantic-chip--warning,
.semantic-chip--excluded {
  color: var(--lv-warning-ink);
  background: var(--lv-warning-bg);
  border-color: var(--lv-warning-line);
}

.semantic-chip--count {
  color: var(--lv-primary-ink);
  background: var(--lv-primary-soft);
}
</style>
