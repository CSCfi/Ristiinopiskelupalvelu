<template>
  <v-app>
    <v-toolbar app>
      <v-toolbar-title class="headline text-uppercase">
        <span>{{ $t('ripa') }} - </span>
        <span class="font-weight-light">Admin UI</span>
      </v-toolbar-title>
      <v-spacer></v-spacer>
      <LanguageSwitcher></LanguageSwitcher>
      <UserInformation :user="user"></UserInformation>
      <form action="/admin-ui/logout" method="post">
        <input type="hidden" name="_csrf" :value="this.$cookies.get('XSRF-TOKEN')" />
        <v-btn type="submit" flat>
          <span class="mr-2">Logout</span>
        </v-btn>
      </form>
    </v-toolbar>

    <v-content>
      <template>
        <v-tabs fixed-tabs v-model="tab">
          <v-tab v-for="item in items" :key="item.key">
            {{ $t(item.title) }}
          </v-tab>
        </v-tabs>
        <v-tabs-items v-model="tab">
          <v-tab-item key="0">
            <v-layout justify-center>
              <v-flex xs12 sm10>
                <Networks :user="user" />
              </v-flex>
            </v-layout>
          </v-tab-item>
          <v-tab-item key="1">
            <v-layout justify-center>
              <v-flex xs12 sm10>
                <Codesets />
              </v-flex>
            </v-layout>
          </v-tab-item>
          <v-tab-item key="2">
            <v-layout justify-center>
              <v-flex xs12 sm10>
                <Organisations :user="user" :tabKey="2" :selectedTab="tab" />
              </v-flex>
            </v-layout>
          </v-tab-item>
        </v-tabs-items>
      </template>
    </v-content>
    <v-footer class="pa-3">
      <v-spacer></v-spacer>
      <div>{{ version }}</div>
    </v-footer>
  </v-app>
</template>

<script>
import axios from 'axios';
import Codesets from './views/Codesets';
import Networks from './views/Networks';
import Organisations from './views/Organisations';
import LanguageSwitcher from './components/LanguageSwitcher';
import UserInformation from './components/UserInformation';

export default {
  name: 'App',
  components: {
    Codesets,
    Networks,
    Organisations,
    LanguageSwitcher,
    UserInformation
  },
  data() {
    return {
      version: process.env.VUE_APP_VERSION,
      tab: null,
      items: [
        { key: 0, title: 'app.networks' },
        { key: 1, title: 'app.codesets' },
        { key: 2, title: 'app.organisations' }
      ],
      user: {
        userOrganisation: '',
        givenname: '',
        firstnames: '',
        lastname: '',
        email: ''
      }
    };
  },
  mounted() {
    axios.get('/admin-ui/api/user/').then(response => {
      this.user = response.data;
    });
  }
};
</script>

<style lang="scss" scoped></style>
