<template>
  <router-view />
</template>

<script setup lang="ts">
//
</script>

<!-- 在 App.vue 或全局布局里加上这个 -->
<style lang="scss" scoped>
@media (max-width: 768px) {
  // 1. 歌曲管理页：强制让 splitter 垂直布局
  .q-splitter {
    flex-direction: column !important;

    // 左侧表格占满宽度，高度固定 40vh
    :deep(.q-splitter__before) {
      height: 40vh !important;
      min-height: 40vh !important;
    }

    // 右侧编辑区占剩余空间
    :deep(.q-splitter__after) {
      height: calc(100vh - 40vh - 120px) !important; // 减去顶部栏和底部安全区
    }
  }

  // 2. 导入弹窗太高 → 限制最大高度 + 内部滚动
  .q-dialog__inner--minimized {
    padding: 0 !important;
  }

  .q-dialog {
    :deep(.q-card) {
      max-height: 90vh;
      width: 95vw !important;
      max-width: 95vw !important;

      .q-card__section {
        max-height: 70vh;
        overflow-y: auto;
      }
    }
  }

  // 3. 表格在手机上字体太小
  :deep(.q-table) {
    font-size: 14px;

    th, td {
      padding: 8px 12px !important;
    }
  }
}
</style>
<!-- App.vue -->
<style lang="scss">
/* 1. 基础全屏布局（所有页面都必须） */
html, body, #q-app {
  height: 100%;
  overflow: hidden;
}

/* 2. q-page-container 必须是 flex 容器 */
.q-page-container {
  height: 100dvh;
  display: flex;
  flex-direction: column;
}

/* 3. q-page 默认行为：伸缩填充 + 内部允许滚动 */
.q-page {
  flex: 1;
  min-height: 0;                    // 关键！允许 flex 子项收缩
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

/* 4. 当你手动加 window-height 时，强化 flex 行为（你正在用的写法） */
.window-height {
  height: 100dvh !important;
  display: flex !important;
  flex-direction: column !important;
}

/* 5. 移动端 dialog 优化（保留你原来的需求） */
.q-dialog {
  :deep(.q-card) {
    max-height: 92dvh !important;
    width: 95vw !important;
    max-width: 95vw !important;
    margin: 16px;
    border-radius: 12px;
  }
}

/* 6. 可选：如果你在手机上觉得页面太“贴边”，加点安全区 */
@supports (padding-bottom: env(safe-area-inset-bottom)) {
  .q-page {
    padding-bottom: env(safe-area-inset-bottom);
  }
}
</style>
