import Vue from 'vue';
import VueI18n from 'vue-i18n';
import en from '../i18n/en.json';
import fi from '../i18n/fi.json';
import sv from '../i18n/sv.json';

Vue.use(VueI18n);
export const i18n = new VueI18n({
  locale: 'fi',
  fallbackLocale: 'fi',
  messages: {
    en: en,
    fi: fi,
    sv: sv
  }
});
