import { createI18n } from 'vue-i18n';
import en from '../i18n/en.json';
import fi from '../i18n/fi.json';
import sv from '../i18n/sv.json';

export const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: 'fi',
  fallbackLocale: 'fi',
  messages: {
    en: en,
    fi: fi,
    sv: sv
  }
});
