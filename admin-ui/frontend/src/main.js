import Vue from 'vue';
import Vuetify from 'vuetify';
import VueCookies from 'vue-cookies';
import 'vuetify/dist/vuetify.min.css'; // Ensure you are using css-loader
import App from './App.vue';
import router from './router';
import store from './store';
import { i18n } from './plugins/i18n.js';

Vue.use(Vuetify);
Vue.use(VueCookies);

Vue.config.productionTip = false;

new Vue({
  router,
  store,
  i18n,
  render: h => h(App)
}).$mount('#app');
