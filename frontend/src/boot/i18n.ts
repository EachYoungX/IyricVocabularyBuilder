import { defineBoot } from '#q-app/wrappers';
import { createI18n } from 'vue-i18n';

import messages from 'src/i18n';

export type MessageLanguages = keyof typeof messages;
// Type-define 'en-US' as the master schema for the resource
export type MessageSchema = (typeof messages)['en-US'];

// See https://vue-i18n.intlify.dev/guide/advanced/typescript.html#global-resource-schema-type-definition
/* eslint-disable @typescript-eslint/no-empty-object-type */
declare module 'vue-i18n' {
  // define the locale messages schema
  export interface DefineLocaleMessage extends MessageSchema {}

  // define the datetime format schema
  export interface DefineDateTimeFormat {}

  // define the number format schema
  export interface DefineNumberFormat {}
}
/* eslint-enable @typescript-eslint/no-empty-object-type */

export default defineBoot(({ app }) => {
  // 从localStorage获取保存的语言设置，默认为英文
  const savedLocale = (localStorage.getItem('app-locale') as MessageLanguages) || 'en-US';

  const i18n = createI18n({
    locale: savedLocale,
    legacy: false,
    messages,
  });

  // 创建语言切换函数
  const changeLocale = (locale: MessageLanguages) => {
    i18n.global.locale.value = locale;
    localStorage.setItem('app-locale', locale);
  };

  // 提供全局语言切换函数
  app.config.globalProperties.$changeLocale = changeLocale;

  // 提供全局语言状态
  app.config.globalProperties.$currentLocale = i18n.global.locale.value;

  // Set i18n instance on app
  app.use(i18n);
});
