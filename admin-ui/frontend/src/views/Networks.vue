<template>
  <v-card>
    <v-container fluid grid-list-md>
      <v-layout row wrap>
        <v-flex xs4>
          <v-card>
            <v-card>
              <v-toolbar>
                <v-toolbar-title>{{ $t('networks') }}</v-toolbar-title>
                <v-spacer></v-spacer>
                <v-toolbar-items>
                  <v-btn flat :title="$t('addNetwork')" @click="addNetwork">
                    <v-icon>add</v-icon>
                  </v-btn>
                </v-toolbar-items>
              </v-toolbar>
              <v-list>
                <v-list-tile
                  v-model="networks"
                  v-for="item in networks"
                  :key="item.id"
                  active-class="highlighted"
                  :class="['list-tile', item.id === active ? 'highlighted' : '']"
                >
                  <v-list-tile-action
                    v-if="!item.published"
                    title="Poista"
                    @click="removeDialog(item)"
                  >
                    <v-icon color="red">delete</v-icon>
                  </v-list-tile-action>
                  <v-list-tile-action v-if="item.published" title="Poista">
                    <v-icon color="grey">delete</v-icon>
                  </v-list-tile-action>
                  <v-list-tile-content @click="rowClicked(item.id)">
                    <v-list-tile-title
                      v-text="getNetworkItemText(item)"
                      :id="item.id"
                    ></v-list-tile-title>
                  </v-list-tile-content>
                </v-list-tile>
              </v-list>
            </v-card>
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
                  <v-btn color="green darken-1" flat="flat" @click="dialog = false">
                    {{ $t('cancel') }}
                  </v-btn>
                  <v-btn color="green darken-1" flat="flat" @click="rowDelete">
                    {{ $t('yes') }}
                  </v-btn>
                </v-card-actions>
              </v-card>
            </v-dialog>
          </v-card>
        </v-flex>
        <v-flex xs8>
          <v-card>
            <NetworkEditor
              :newNetwork="newNetwork"
              :locale="this.$i18n.locale"
              :user="user"
              v-bind:active="active"
              v-model="jsonNetwork"
              @savedNetwork="savedNetwork"
              @savedNewNetwork="savedNewNetwork"
            ></NetworkEditor>
          </v-card>
        </v-flex>
      </v-layout>
    </v-container>
  </v-card>
</template>

<script>
import axios from 'axios';
import NetworkEditor from '../components/network/NetworkEditor';
import _ from 'lodash';
import languageUtils from '../utils/languageUtils';

export default {
  props: {
    user: Object
  },
  components: {
    NetworkEditor
  },
  data() {
    return {
      dialog: false,
      removeItem: {},
      jsonNetwork: {},
      active: false,
      newNetwork: false,
      networks: []
    };
  },
  mounted() {
    this.init();
  },
  methods: {
    rowClicked(id) {
      if (id) {
        this.jsonNetwork = _.cloneDeep(this.networks.find(n => n.id === id));
        this.active = id;
        this.newNetwork = false;
      }
    },
    rowDelete() {
      this.dialog = false;
      axios.delete('/admin-ui/api/networks/' + this.removeItem.id).then(() => {
        this.networks.splice(_.findIndex(this.networks, n => n.id === this.removeItem.id), 1);
        if (this.jsonNetwork && this.jsonNetwork.id === this.removeItem.id) {
          this.jsonNetwork = null;
        }
      });
    },
    removeDialog(item) {
      this.removeItem = item;
      this.dialog = true;
    },

    getNetworkItemText(item) {
      let name = languageUtils.resolveText(this.$i18n.locale, item.name.values);
      return item.name ? `${name} (${item.abbreviation})` : '';
    },

    addNetwork() {
      axios.get('/admin-ui/api/networks/new').then(response => {
        this.newNetwork = true;
        this.active = null;
        this.jsonNetwork = response.data;
      });
    },

    savedNetwork() {
      this.getNetworks();
      this.active = this.jsonNetwork.id;
    },

    savedNewNetwork(event, newNetworkId) {
      this.getNetworks();
      this.active = newNetworkId;
      this.newNetwork = false;
    },

    getNetworks() {
      axios.get('/admin-ui/api/networks').then(response => {
        this.networks = _.sortBy(response.data, n => this.getNetworkItemText(n));
      });
    },

    init() {
      this.getNetworks();
    }
  }
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
.v-list__tile__content {
  cursor: pointer;
}
.v-list__tile__action {
  cursor: pointer;
}
.v-list__tile {
  padding: 0 0 0 16px;
}
</style>
