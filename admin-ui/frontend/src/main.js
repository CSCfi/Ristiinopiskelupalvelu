import { createApp } from 'vue';
import VueCookies from 'vue-cookies';
import '@mdi/font/css/materialdesignicons.css';
import App from './App.vue';
import router from './router';
import pinia from './store';
import { i18n } from './plugins/i18n';
import vuetify from './plugins/vuetify';
import { registerLegacyComponents } from './plugins/legacySupport';
import './assets/legacy.css';
import axios from 'axios';

axios.defaults.withCredentials = true;
axios.defaults.baseURL = '/';

const app = createApp(App);

app.use(router);
app.use(pinia);
app.use(i18n);
app.use(vuetify);
app.use(VueCookies);
registerLegacyComponents(app);

app.mount('#app');
