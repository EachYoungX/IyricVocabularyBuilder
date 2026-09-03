<template>
  <q-select
    ref="selectRef"
    v-bind="$attrs"
    @popup-show="handlePopupShow"
    @popup-hide="handlePopupHide"
  />
</template>

<script setup lang="ts">
import { onUnmounted, ref } from 'vue';
import type { QSelect } from 'quasar';

defineOptions({
  inheritAttrs: false,
});

const selectRef = ref<QSelect | null>(null);

function closePopupOnScroll(event: Event) {
  const target = event.target;
  if (target instanceof HTMLElement && target.closest('.q-menu')) return;
  selectRef.value?.hidePopup();
}

function handlePopupShow() {
  window.addEventListener('scroll', closePopupOnScroll, true);
}

function handlePopupHide() {
  window.removeEventListener('scroll', closePopupOnScroll, true);
}

onUnmounted(handlePopupHide);

</script>
