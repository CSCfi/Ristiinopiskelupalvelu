<template>
  <v-card>
    <v-container fluid>
      <v-row>
        <v-col cols="12" md="4">
          <v-card>
            <v-card-title class="d-flex align-center">
              <span>{{ $t('organisation.toolbarTitle') }}</span>
              <v-spacer></v-spacer>
              <v-btn
                v-if="isSuperUser"
                variant="text"
                icon
                :title="$t('organisation.addNewOrganisationTitle')"
                @click="addNewOrganisation"
              >
                <v-icon icon="mdi-plus"></v-icon>
              </v-btn>
            </v-card-title>
            <v-card-text>
              <v-list>
                <v-list-item
                  v-for="org in organisations"
                  :key="org.id"
                  active-class="highlighted"
                  :class="['list-tile', isSelected(org.id) ? 'highlighted' : '']"
                  @click="selectOrganisation(org)"
                >
                  <template #append>
                    <v-btn
                      v-if="isSuperUser"
                      variant="text"
                      color="red"
                      icon
                      :title="$t('organisation.deleteBtnTitle')"
                      @click.stop="showRemoveDialog(org)"
                    >
                      <v-icon icon="mdi-delete"></v-icon>
                    </v-btn>
                  </template>
                  <v-list-item-title
                    v-text="getLocalisedOrganisationName(org)"
                    :id="org.id"
                  ></v-list-item-title>
                </v-list-item>
              </v-list>
            </v-card-text>
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
                <v-btn color="green" variant="text" @click="closeRemoveDialog()">
                  {{ $t('cancel') }}
                </v-btn>
                <v-btn color="green" variant="text" @click="deleteOrganisation(organisationToRemove.id)">
                  {{ $t('yes') }}
                </v-btn>
              </v-card-actions>
            </v-card>
          </v-dialog>
        </v-col>
        <v-col cols="12" md="8">
          <v-card height="100%">
            <OrganisationEditor
              :locale="$i18n.locale"
              :selectedOrganisation="selectedOrganisation"
              :newOrganisation="newOrganisation"
              :isSuperUser="isSuperUser"
              @savedOrganisation="savedOrganisation"
            />
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </v-card>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import _ from 'lodash';
import OrganisationEditor from '../components/organisation/OrganisationEditor.vue';
import languageUtils from '../utils/languageUtils';
import { useOrganisationStore } from '../stores/organisations';
import { storeToRefs } from 'pinia';

const props = defineProps({
  user: Object,
  selectedTab: null,
  tabKey: null
});

const { locale } = useI18n();

const dialog = ref(false);
const organisationStore = useOrganisationStore();
const { organisations } = storeToRefs(organisationStore);
const selectedOrganisation = ref(null);
const newOrganisation = ref(false);
const organisationToRemove = ref(null);

const isSuperUser = computed(() => _.includes(props.user?.roles, 'SUPERUSER'));

const showRemoveDialog = organisation => {
  organisationToRemove.value = organisation;
  dialog.value = true;
};

const closeRemoveDialog = () => {
  dialog.value = false;
  organisationToRemove.value = null;
};

const isSelected = id => id === selectedOrganisation.value?.id;

const selectOrganisation = organisation => {
  selectedOrganisation.value = organisation;
  newOrganisation.value = false;
};

const addNewOrganisation = () => {
  selectedOrganisation.value = {};
  newOrganisation.value = true;
};

const savedOrganisation = savedOrg => {
  getOrganisations();
  selectedOrganisation.value = savedOrg;
  newOrganisation.value = false;
};

const getLocalisedOrganisationName = organisation =>
  languageUtils.resolveText(locale.value, organisation?.organisationName?.values);

const getOrganisations = () => {
  organisationStore.fetchOrganisations().then(list => {
    organisations.value = _.sortBy(list, o => getLocalisedOrganisationName(o).toLowerCase());
  });
};

const deleteOrganisation = id => {
  organisationStore.deleteOrganisation(id).then(() => {
    getOrganisations();
    selectedOrganisation.value = null;
    closeRemoveDialog();
  });
};

watch(
  () => props.selectedTab,
  newTab => {
    if (newTab === props.tabKey) {
      getOrganisations();
    }
  }
);

onMounted(() => {
  getOrganisations();
});
</script>

<style lang="css" scoped>
.highlighted {
  background: rgba(0, 0, 0, 0.1);
}
.v-icon.material-icons.theme--light.red--text:hover {
  font-size: 1.7em;
}
.v-list-item__content {
  cursor: pointer;
}
.list-tile:hover {
  background: rgba(0, 0, 0, 0.1);
}
.v-list-item__append {
  cursor: pointer;
}
.v-list-item {
  padding: 0 0 0 16px;
}
</style>
