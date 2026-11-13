<template>
  <v-app>
    <v-app-bar flat>
      <v-app-bar-title class="text-uppercase">
        <span>{{ $t('ripa') }} - </span>
        <span class="font-weight-light">Admin UI</span>
      </v-app-bar-title>
      <v-spacer></v-spacer>
      <LanguageSwitcher></LanguageSwitcher>
      <UserInformation :user="user"></UserInformation>
      <form action="/admin-ui/logout" method="post">
        <input type="hidden" name="_csrf" :value="$cookies.get('XSRF-TOKEN')" />
        <v-btn type="submit" variant="text">
          <span class="mr-2">Logout</span>
        </v-btn>
      </form>
    </v-app-bar>

    <v-main>
      <v-container fluid>
        <v-tabs v-model="tab" grow>
          <v-tab v-for="item in items" :key="item.key" :value="item.key">
            {{ $t(item.title) }}
          </v-tab>
        </v-tabs>
        <v-window v-model="tab" class="mt-4">
          <v-window-item :value="0">
            <v-row justify="center">
              <v-col cols="12" sm="10">
                <Networks :user="user" />
              </v-col>
            </v-row>
          </v-window-item>
          <v-window-item :value="1">
            <v-row justify="center">
              <v-col cols="12" sm="10">
                <Codesets />
              </v-col>
            </v-row>
          </v-window-item>
          <v-window-item :value="2">
            <v-row justify="center">
              <v-col cols="12" sm="10">
                <Organisations :user="user" :tabKey="2" :selectedTab="tab" />
              </v-col>
            </v-row>
          </v-window-item>
        </v-window>
      </v-container>
    </v-main>
    <v-footer class="pa-3">
      <v-spacer></v-spacer>
      <div>{{ version }}</div>
    </v-footer>
  </v-app>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import Codesets from './views/Codesets.vue';
import Networks from './views/Networks.vue';
import Organisations from './views/Organisations.vue';
import LanguageSwitcher from './components/LanguageSwitcher.vue';
import UserInformation from './components/UserInformation.vue';
import { useUserStore } from './stores/user';
import { storeToRefs } from 'pinia';

const version = import.meta.env.VITE_APP_VERSION || '';
const items = [
  { key: 0, title: 'app.networks' },
  { key: 1, title: 'app.codesets' },
  { key: 2, title: 'app.organisations' }
];
const tab = ref(0);

const userStore = useUserStore();
const { user } = storeToRefs(userStore);

onMounted(() => {
  userStore.fetchCurrentUser();
});
</script>

<style lang="scss" scoped></style>
