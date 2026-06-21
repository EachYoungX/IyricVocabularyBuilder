declare namespace NodeJS {
  interface ProcessEnv {
    NODE_ENV: string;
    VUE_ROUTER_MODE: 'hash' | 'history' | 'abstract' | undefined;
    VUE_ROUTER_BASE: string | undefined;
  }
}

// Vue类型声明
declare module 'vue' {
  export interface Ref<T> {
    value: T;
  }
  export function ref<T>(value: T): Ref<T>;
  export function computed<T>(getter: () => T): Ref<T>;
  export function watch<T>(source: T | (() => T), callback: (newValue: T, oldValue: T) => void, options?: { immediate?: boolean; deep?: boolean }): void;
  export function onMounted(callback: () => void): void;
  export function getCurrentInstance(): { appContext: { config: { globalProperties: Record<string, unknown> } } } | null;
}

// 为全局属性添加类型声明
declare module '@vue/runtime-core' {
  interface ComponentCustomProperties {
    $changeLocale: (locale: 'en-US' | 'zh-CN') => void;
    $currentLocale: { value: 'en-US' | 'zh-CN' };
    $t: (key: string) => string;
  }
}
