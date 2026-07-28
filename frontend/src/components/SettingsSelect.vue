<template>
  <q-select
    ref="selectRef"
    v-bind="$attrs"
    @popup-show="handlePopupShow"
    @popup-hide="handlePopupHide"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import type { QSelect } from 'quasar';

defineOptions({
  inheritAttrs: false,
});

const selectRef = ref<QSelect | null>(null);

function closePopupOnScroll() {
  selectRef.value?.hidePopup();
}

function handlePopupShow() {
  window.addEventListener('scroll', closePopupOnScroll, true);
}

function handlePopupHide() {
  window.removeEventListener('scroll', closePopupOnScroll, true);
}

</script>
