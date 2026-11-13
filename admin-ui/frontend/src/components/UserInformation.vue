<template>
  <div class="user-info-container">
    <v-container class="pa-0" fluid>
      <v-row align="center" no-gutters>
        <v-col cols="3">
          <v-icon icon="mdi-account-circle"></v-icon>
        </v-col>
        <v-col>
          <div class="d-flex flex-column">
            <span>
              {{ user?.fullname || '' }}
            </span>
            <span>
              {{ localisedOrganisation }}
            </span>
          </div>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

const props = defineProps({
  user: {
    type: Object,
    default: () => ({})
  }
});

const { locale } = useI18n();

const localisedOrganisation = computed(() => {
  const org = props.user?.userOrganisation;
  return org?.name?.values?.[locale.value] || '';
});
</script>

<style lang="css" scoped>
.user-info-container {
  min-width: 220px;
  max-width: 420px;
  padding-left: 12px;
}
</style>
