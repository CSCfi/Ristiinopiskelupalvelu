<template>
  <div class="user-info-container">
    <v-container class="pa-0" grid-list-md text-xs-center fill-height>
      <v-layout row wrap align-center>
        <v-flex xs3>
          <v-icon>account_circle</v-icon>
        </v-flex>
        <v-flex>
          <v-layout row wrap justify-start>
            <span>
              {{ user.fullname }}
            </span>
          </v-layout>
          <v-layout row wrap justify-start>
            <span>
              {{ localisedOrganisation }}
            </span>
          </v-layout>
        </v-flex>
      </v-layout>
    </v-container>
  </div>
</template>

<script>
export default {
  props: {
    user: Object
  },
  computed: {
    localisedOrganisation: function() {
      return this.user.userOrganisation && this.user.userOrganisation.name.values[this.$i18n.locale]
        ? this.user.userOrganisation.name.values[this.$i18n.locale]
        : '';
    }
  }
};
</script>

<style lang="css" scoped>
.user-info-container {
  min-width: 200px;
  max-width: 250px;
  padding-left: 12px;
}
</style>
