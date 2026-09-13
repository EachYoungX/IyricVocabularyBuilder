declare namespace NodeJS {
  interface ProcessEnv {
    APP_VERSION: string;
    NODE_ENV: string;
    VUE_ROUTER_MODE: 'hash' | 'history' | 'abstract' | undefined;
    VUE_ROUTER_BASE: string | undefined;
  }
}

interface ImportMetaEnv {
  readonly DEV: boolean;
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

interface DesktopDatasetDescriptor {
  source: 'managed' | 'external';
  path: string;
  fileName: string;
  datasetVersion?: string;
  schemaVersion?: string;
  size: number;
  lastModified: number;
  status: 'valid' | 'invalid' | 'missing' | 'incompatible';
  message?: string;
}

interface DesktopDatasetState {
  active: DesktopDatasetDescriptor | null;
  managed: DesktopDatasetDescriptor[];
  dictionaryEnabled: boolean;
  autoSelected: boolean;
}

interface DesktopRuntimeInfo {
  appVersion: string;
  backendUrl: string;
  backendVersion: string;
  mode: 'development' | 'installed' | 'portable';
  lanEnabled: boolean;
  lanUrls: string[];
}

interface DesktopBridge {
  clearAllLocalData(): Promise<void>;
  getRuntimeInfo(): Promise<DesktopRuntimeInfo>;
  getDatasetState(): Promise<DesktopDatasetState>;
  openDataDirectory(): Promise<void>;
  openDatasetDirectory(): Promise<void>;
  rescanDatasets(): Promise<DesktopDatasetState>;
  importDatasetToManagedDirectory(): Promise<DesktopDatasetState | null>;
  selectExternalDataset(): Promise<DesktopDatasetState | null>;
  activateManagedDataset(fileName: string): Promise<DesktopDatasetState>;
  clearExternalDataset(): Promise<DesktopDatasetState>;
  removeManagedDataset(fileName: string): Promise<DesktopDatasetState>;
  restartBackend(): Promise<DesktopRuntimeInfo>;
  setLanEnabled(enabled: boolean): Promise<DesktopRuntimeInfo>;
}

interface Window {
  desktopBridge?: DesktopBridge;
}

// Vue 类型声明
declare module 'vue' {
  export interface Ref<T> {
    value: T;
  }
  export function ref<T>(value: T): Ref<T>;
  export function computed<T>(getter: () => T): Ref<T>;
  export function watch<T>(
    source: T | (() => T),
    callback: (newValue: T, oldValue: T) => void,
    options?: { immediate?: boolean; deep?: boolean },
  ): void;
  export function onMounted(callback: () => void): void;
  export function onBeforeUnmount(callback: () => void): void;
  export function onUnmounted(callback: () => void): void;
  export function getCurrentInstance(): {
    appContext: { config: { globalProperties: Record<string, unknown> } };
  } | null;
}

// 为全局属性添加类型声明
declare module '@vue/runtime-core' {
  interface ComponentCustomProperties {
    $changeLocale: (locale: 'en-US' | 'zh-CN') => void;
    $currentLocale: { value: 'en-US' | 'zh-CN' };
    $t: (key: string) => string;
  }
}
