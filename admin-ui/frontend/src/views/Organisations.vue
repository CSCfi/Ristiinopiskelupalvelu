<template>
  <v-card>
    <v-container fluid grid-list-md>
      <v-layout row wrap>
        <v-flex xs4>
          <v-card>
            <v-toolbar>
              <v-toolbar-title>{{ $t('organisation.toolbarTitle') }}</v-toolbar-title>
              <v-spacer></v-spacer>
              <v-toolbar-items v-if="isSuperUser">
                <v-btn
                  flat
                  :title="$t('organisation.addNewOrganisationTitle')"
                  @click="addNewOrganisation"
                >
                  <v-icon>add</v-icon>
                </v-btn>
              </v-toolbar-items>
            </v-toolbar>
            <v-list>
              <v-list-tile
                v-model="organisations"
                v-for="org in organisations"
                :key="org.id"
                active-class="highlighted"
                :class="['list-tile', isSelected(org.id) ? 'highlighted' : '']"
              >
                <v-list-tile-action
                  v-if="isSuperUser"
                  :title="$t('organisation.deleteBtnTitle')"
                  @click="showRemoveDialog(org)"
                >
                  <v-icon color="red">delete</v-icon>
                </v-list-tile-action>
                <v-list-tile-content @click="selectOrganisation(org)">
                  <v-list-tile-title
                    v-text="getLocalisedOrganisationName(org)"
                    :id="org.id"
                  ></v-list-tile-title>
                </v-list-tile-content>
              </v-list-tile>
            </v-list>
          </v-card>
          <v-dialog v-model="dialog" width="600px">
            <v-card>
              <v-card-title class="headline">
                {{ $t('organisation.removeConfirmation') }}
              </v-card-title>
              <v-card-text>
                {{ $t('organisation.removeConfirmationInfo') }}
              </v-card-text>
              <v-card-actions>
                <v-spacer></v-spacer>
                <v-btn color="green darken-1" flat="flat" @click="closeRemoveDialog()">
                  {{ $t('cancel') }}
                </v-btn>
                <v-btn
                  color="green darken-1"
                  flat="flat"
                  @click="deleteOrganisation(organisationToRemove.id)"
                >
                  {{ $t('yes') }}
                </v-btn>
              </v-card-actions>
            </v-card>
          </v-dialog>
        </v-flex>
        <v-flex xs8>
          <v-card height="100%">
            <OrganisationEditor
              :locale="this.$i18n.locale"
              :selectedOrganisation="selectedOrganisation"
              :newOrganisation="newOrganisation"
              :isSuperUser="isSuperUser"
              @savedOrganisation="savedOrganisation"
            />
          </v-card>
        </v-flex>
      </v-layout>
    </v-container>
  </v-card>
</template>

<script>
import _ from 'lodash';
import axios from 'axios';
import OrganisationEditor from '../components/organisation/OrganisationEditor';
import languageUtils from '../utils/languageUtils';

export default {
  props: {
    user: Object,
    selectedTab: null,
    tabKey: null
  },
  components: {
    OrganisationEditor
  },
  computed: {
    isSuperUser: function() {
      return _.includes(this.user?.roles, 'SUPERUSER');
    }
  },
  watch: {
    selectedTab(newTab) {
      if (newTab === this.tabKey) {
        this.getOrganisations();
      }
    }
  },
  data() {
    return {
      dialog: false,
      organisations: [1, 2, 3, 4],
      selectedOrganisation: null,
      newOrganisation: false,
      organisationToRemove: null
    };
  },
  methods: {
    showRemoveDialog(organisation) {
      this.organisationToRemove = organisation;
      this.dialog = true;
    },

    closeRemoveDialog() {
      this.dialog = false;
      this.organisationToRemove = null;
    },

    isSelected(id) {
      return id === this.selectedOrganisation?.id;
    },

    selectOrganisation(organisation) {
      this.selectedOrganisation = organisation;
      this.newOrganisation = false;
    },

    addNewOrganisation() {
      this.selectedOrganisation = {};
      this.newOrganisation = true;
    },

    savedOrganisation(savedOrganisation) {
      this.getOrganisations();
      this.selectedOrganisation = savedOrganisation;
      this.newOrganisation = false;
    },

    getLocalisedOrganisationName(organisation) {
      return languageUtils.resolveText(this.$i18n.locale, organisation?.organisationName?.values);
    },

    getOrganisations() {
      axios.get('/admin-ui/api/organisations/').then(response => {
        this.organisations = _.sortBy(response.data, o =>
          this.getLocalisedOrganisationName(o).toLowerCase()
        );
      });
    },

    deleteOrganisation(id) {
      axios.delete('/admin-ui/api/organisations/' + id).then(() => {
        this.getOrganisations();
        this.selectedOrganisation = null;
        this.closeRemoveDialog();
      });
    }
  }
};
</script>

<style lang="css" scoped>
.highlighted {
  background: rgba(0, 0, 0, 0.1);
}
.v-icon.material-icons.theme--light.red--text:hover {
  font-size: 1.7em;
}
.v-list__tile__content {
  cursor: pointer;
}
.list-tile:hover {
  background: rgba(0, 0, 0, 0.1);
}
.v-list__tile__action {
  cursor: pointer;
}
.v-list__tile {
  padding: 0 0 0 16px;
}
</style>
