<template>
  <div class="section-heading">
    <div class="section-icon">
      <q-icon :name="icon" size="22px" />
    </div>
    <div>
      <div class="section-title">{{ title }}</div>
      <div class="section-caption">{{ caption }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  icon: string;
  title: string;
  caption: string;
}>();
</script>

<style scoped lang="scss">
.section-heading {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.section-icon {
  display: grid;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  place-items: center;
  color: var(--lv-paper);
  background: var(--lv-ink);
  border-radius: 50%;
}

.section-title {
  color: var(--lv-ink);
  font-family: var(--lv-font-serif);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.1;
}

.section-caption {
  color: var(--lv-ink-soft);
  font-size: 13px;
  line-height: 1.6;
}
</style>
