import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },
      // --- [新增] 歌曲管理页面路由 ---
      {
        path: '/songs',
        name: 'SongsManager',
        component: () => import('pages/SongsManagerPage.vue'),
      },
      {
        path: '/song/import',
        name: 'SongImport',
        component: () => import('pages/SongImportPage.vue'),
      },
      {
        path: '/my-vocabulary',
        name: 'MyVocabulary',
        component: () => import('pages/MyVocabularyPage.vue'),
      },
      {
        path: '/settings',
        name: 'Settings',
        component: () => import('pages/SettingsPage.vue'),
      },
    ],
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
];

export default routes;
