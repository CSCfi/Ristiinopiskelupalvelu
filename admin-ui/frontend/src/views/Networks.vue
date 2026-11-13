<template>
  <v-card>
    <v-container fluid>
      <v-row>
        <v-col cols="12" md="4">
          <v-card>
            <v-card-title class="d-flex align-center">
              <span>{{ $t('networks') }}</span>
              <v-spacer></v-spacer>
              <v-btn variant="text" :title="$t('addNetwork')" @click="addNetwork" icon>
                <v-icon icon="mdi-plus"></v-icon>
              </v-btn>
            </v-card-title>
            <v-card-text>
              <v-text-field v-model="listFilter" clearable :label="$t('filter')"></v-text-field>
              <v-list>
                <v-list-item
                  v-for="item in filteredNetworks"
                  :key="item.id || `unsaved-${index}`"
                  :value="item.id"
                  :active="normalizeId(item.id) === active"
                  active-class="highlighted"
                  :class="['list-tile', normalizeId(item.id) === active ? 'highlighted' : '']"
                  @click="rowClicked(item.id)"
                >
                  <template #append>
                    <v-btn
                      v-if="!item.published"
                      variant="text"
                      color="red"
                      icon
                      @click.stop="removeDialog(item)"
                      :title="$t('networkRemoveConfirmation')"
                    >
                      <v-icon icon="mdi-delete"></v-icon>
                    </v-btn>
                    <v-icon v-else icon="mdi-delete-off" color="grey"></v-icon>
                  </template>
                  <v-list-item-title v-text="getNetworkItemText(item)" :id="item.id" />
                </v-list-item>
              </v-list>
            </v-card-text>
            <v-dialog v-model="dialog" width="600px">
              <v-card>
                <v-card-title class="headline">
                  {{ $t('networkRemoveConfirmation') }}
                </v-card-title>
                <v-card-text>
                  {{ $t('networkRemoveConfirmationInfo') }}
                </v-card-text>
                <v-card-actions>
                  <v-spacer></v-spacer>
                  <v-btn color="green" variant="text" @click="dialog = false">
                    {{ $t('cancel') }}
                  </v-btn>
                  <v-btn color="green" variant="text" @click="rowDelete">
                    {{ $t('yes') }}
                  </v-btn>
                </v-card-actions>
              </v-card>
            </v-dialog>
          </v-card>
        </v-col>
        <v-col cols="12" md="8">
          <v-card>
            <NetworkEditor
              :key="active || (isNewNetwork ? 'new-network' : 'network-editor')"
              :newNetwork="isNewNetwork"
              :locale="$i18n.locale"
              :user="user"
              v-bind:active="active"
              v-model="jsonNetwork"
              @savedNetwork="savedNetwork"
              @savedNewNetwork="savedNewNetwork"
            ></NetworkEditor>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </v-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import NetworkEditor from '../components/network/NetworkEditor.vue';
import _ from 'lodash';
import languageUtils from '../utils/languageUtils';
import { useNetworkStore } from '../stores/networks';
import emptyNetwork from '../assets/networkSchemas/emptyNetwork.json';

const props = defineProps({
  user: Object
});

const { locale } = useI18n();

const dialog = ref(false);
const removeItem = ref({});
const jsonNetwork = ref({});
const active = ref(null);
const newNetwork = ref(false);
const networks = ref([]);
const listFilter = ref('');
const networkStore = useNetworkStore();

const normalizeId = id => {
  if (id === null || id === undefined) return null;
  const num = Number(id);
  return isNaN(num) ? id : num;
};

const isTempId = id => !id;
const isNewNetwork = computed(() => newNetwork.value || isTempId(active.value) || !jsonNetwork.value?.id);

const filteredNetworks = computed(() => {
  if (listFilter.value) {
    const upperFilter = listFilter.value.toUpperCase();
    return networks.value.filter(network => {
      if (
        network.abbreviation &&
        network.abbreviation.toUpperCase().indexOf(upperFilter) >= 0
      ) {
        return true;
      }
      if (network?.name?.values) {
        if (
          Object.keys(network.name.values).find(
            language =>
              (network.name.values[language] || '').toUpperCase().indexOf(upperFilter) >= 0
          )
        ) {
          return true;
        }
      }
      return false;
    });
  }
  return networks.value;
});

onMounted(() => {
  init();
});

const syncCurrentToList = () => {
  if (!jsonNetwork.value) return;
  const currentId = normalizeId(active.value);
  const idx = networks.value.findIndex(n => {
    const nid = normalizeId(n.id);
    return (nid === null && currentId === null) || nid === currentId;
  });
  if (idx >= 0) {
    networks.value.splice(idx, 1, _.cloneDeep(jsonNetwork.value));
  }
};

const setActiveById = id => {
  syncCurrentToList();
  const targetId = normalizeId(id);
  active.value = targetId;
  const found = networks.value.find(n => normalizeId(n.id) === targetId);
  jsonNetwork.value = found ? _.cloneDeep(found) : null;
};

const rowClicked = id => {
  const targetId = normalizeId(id);
  setActiveById(targetId);
  newNetwork.value = !targetId;
};

const rowDelete = () => {
  dialog.value = false;
  const targetId = normalizeId(removeItem.value.id);

  if (isTempId(targetId)) {
    const tempIndex = networks.value.findIndex(n => !n.id);
    if (tempIndex >= 0) {
      networks.value.splice(tempIndex, 1);
    }
    if (!jsonNetwork.value?.id) {
      jsonNetwork.value = null;
      active.value = null;
      newNetwork.value = false;
    }
    if (!jsonNetwork.value && networks.value.length > 0) {
      setActiveById(networks.value[0].id);
    }
    return;
  }

  networkStore.deleteNetwork(targetId).then(() => {
    networks.value.splice(
      _.findIndex(networks.value, n => normalizeId(n.id) === targetId),
      1
    );
    if (jsonNetwork.value && normalizeId(jsonNetwork.value.id) === targetId) {
      jsonNetwork.value = null;
      active.value = null;
    }
    if (!jsonNetwork.value && networks.value.length > 0) {
      setActiveById(networks.value[0].id);
    }
  });
};

const removeDialog = item => {
  removeItem.value = item;
  dialog.value = true;
};

const getNetworkItemText = item => {
  const values = item?.name?.values || {};
  let name = languageUtils.resolveText(locale.value, values) || '';
  const abbr = item?.abbreviation || '';
  const text = name || abbr ? `${name} ${abbr ? `(${abbr})` : ''}`.trim() : '';
  return text || item?.id || '';
};

const addNetwork = () => {
  syncCurrentToList();
  jsonNetwork.value = _.cloneDeep({ ...emptyNetwork, id: '' });
  newNetwork.value = true;
  active.value = 'new-network';
};

const savedNetwork = async () => {
  const targetId = normalizeId(jsonNetwork.value?.id);
  if (targetId) {
    // merge into local list
    const existingIndex = networks.value.findIndex(n => normalizeId(n.id) === targetId);
    if (existingIndex >= 0) {
      networks.value.splice(existingIndex, 1, _.cloneDeep(jsonNetwork.value));
    }
    setActiveById(targetId);
    newNetwork.value = false;
    await getNetworks();
  } else {
    await getNetworks();
  }
};

const savedNewNetwork = async (newNetworkId, createdNetwork) => {
  const created = createdNetwork || jsonNetwork.value;
  const createdId = normalizeId(created?.id || newNetworkId);
  await getNetworks();
  newNetwork.value = false;
  if (createdId) {
    setActiveById(createdId);
  } else if (networks.value.length) {
    setActiveById(networks.value[0].id);
  } else {
    jsonNetwork.value = null;
    active.value = null;
  }
};

const getNetworks = async () => {
  const response = await networkStore.fetchNetworks();
  const sorted = _.sortBy(response, n => getNetworkItemText(n));
  networks.value = sorted;
  if (networks.value.length > 0 && (!active.value || !jsonNetwork.value?.id)) {
    setActiveById(networks.value[0].id);
    newNetwork.value = false;
  }
  return networks.value;
};

const init = () => {
  getNetworks();
};
</script>

<style lang="css" scoped>
.highlighted {
  background: rgba(0, 0, 0, 0.1);
}
.list-tile:hover {
  background: rgba(0, 0, 0, 0.1);
}
.v-icon.material-icons.theme--light.red--text:hover {
  font-size: 1.7em;
}
.v-list-item__content {
  cursor: pointer;
}
.v-list-item__append {
  cursor: pointer;
}
.v-list-item {
  padding: 0 0 0 16px;
}
</style>
