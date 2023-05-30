<template>
  <v-card>
    <v-container fluid grid-list-md>
      <v-layout row wrap>
        <v-flex xs12>
          <v-card>
            <div v-if="superuser === false">
              <v-card-title primary-title>
                <h3>{{ $t('noPermissions') }}</h3>
              </v-card-title>
              <v-card-text>
                <span>{{ $t('noPermissions2') }}</span>
              </v-card-text>
            </div>
            <CodesetUpload v-if="superuser === true" @refresh="refresh" />
          </v-card>
        </v-flex>
        <v-flex xs3>
          <v-card>
            <v-toolbar>
              <v-toolbar-title>{{ $t('codesets') }}</v-toolbar-title>
              <v-toolbar-items>
                <v-btn flat v-if="superuser" title="Lisää" @click="showAddNewCodeSetDialog()">
                  <v-icon>add</v-icon>
                </v-btn>
              </v-toolbar-items>
            </v-toolbar>
            <v-list>
              <v-list-tile
                v-for="item in codesets"
                :key="item.title"
                active-class="highlighted"
                :class="['list-tile', item.title === activeMessage ? 'highlighted' : '']"
              >
                <v-list-tile-action
                  v-if="superuser === true"
                  title="Poista"
                  @click="showRemoveCodeSetDialog(item.title)"
                >
                  <v-icon color="red">delete</v-icon>
                </v-list-tile-action>
                <v-list-tile-content @click="rowClicked(item.title)">
                  <v-list-tile-title v-text="itemHeader(item)" :id="item.title"></v-list-tile-title>
                </v-list-tile-content>
              </v-list-tile>
            </v-list>
            <v-dialog v-model="addNewCodeSetDialogVisible" width="600px">
              <v-card>
                <v-form id="add-new-codeset-form" ref="addNewCodeSetForm" :lazy-validation="false">
                  <v-card-title class="headline">{{ $t('addNewCodeSetDialogTitle') }}</v-card-title>
                  <v-card-text>
                    <v-text-field
                      v-model="addNewCodeSetName"
                      :label="$t('addNewCodeSetName')"
                      :rules="[requiredRule]"
                      required
                    ></v-text-field>
                  </v-card-text>
                  <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn color="red darken-1" flat="flat" @click="hideAddNewCodeSetDialog()">
                      {{ $t('cancel') }}
                    </v-btn>
                    <v-btn color="green darken-1" flat="flat" @click="addNewCodeSet()">
                      {{ $t('yes') }}
                    </v-btn>
                  </v-card-actions>
                </v-form>
              </v-card>
            </v-dialog>
            <v-dialog v-model="removeCodeSetDialogVisible" width="600px">
              <v-card>
                <v-card-title class="headline"
                  >{{ $t('codesetRemoveConfirmation') }} "{{ this.removeCodeSetName }}"?
                </v-card-title>
                <v-card-text>
                  {{ $t('codesetRemoveConfirmationInfo') }}
                </v-card-text>
                <v-card-actions>
                  <v-spacer></v-spacer>
                  <v-btn color="red darken-1" flat="flat" @click="hideRemoveCodeSetDialog()">
                    {{ $t('cancel') }}
                  </v-btn>
                  <v-btn color="green darken-1" flat="flat" @click="removeCodeSet()">
                    {{ $t('yes') }}
                  </v-btn>
                </v-card-actions>
              </v-card>
            </v-dialog>
          </v-card>
        </v-flex>
        <v-flex xs3>
          <v-card>
            <CodesetItems
              :activeCodeSetId="activeCodeSetId"
              :codesetItems="codesetItems"
              :activeItem="activeCodeSetItem"
              :superuser="superuser"
              :admin="admin"
              v-model="activeCodeSetItem"
              @itemRowClicked="itemRowClicked"
              @itemRowDelete="itemRowDelete"
              @addItem="addItem"
            />
          </v-card>
        </v-flex>
        <v-flex xs6>
          <v-card height="100%">
            <CodesetEditor
              :locale="this.$i18n.locale"
              v-bind:activeItem="activeCodeSetItem"
              v-bind:superuser="superuser"
              v-bind:admin="admin"
              v-model="selectedCodeSetItem"
              v-if="isRolesLoaded"
              @refresh="refresh"
              @saved="saved"
            ></CodesetEditor>
          </v-card>
        </v-flex>
      </v-layout>
    </v-container>
  </v-card>
</template>

<script>
import axios from 'axios';
import CodesetUpload from '../components/codeset/CodesetUpload';
import CodesetItems from '../components/codeset/CodesetItems';
import CodesetEditor from '../components/codeset/CodesetEditor';

export default {
  components: {
    CodesetUpload,
    CodesetItems,
    CodesetEditor
  },
  data() {
    return {
      activeCodeSetId: null,
      activeCodeSetItem: false,
      selectedCodeSetItem: null,
      codesets: [],
      codesetItems: [],
      addNewCodeSetDialogVisible: false,
      addNewCodeSetName: null,
      removeCodeSetDialogVisible: false,
      removeCodeSetName: null,
      superuser: false,
      admin: false,
      isRolesLoaded: false
    };
  },
  computed: {
    activeMessage: function() {
      return this.activeCodeSetId;
    }
  },
  mounted() {
    this.initCodeSets();
    axios.get('/admin-ui/api/user/roles').then(response => {
      for (let role in response.data) {
        if (response.data[role] === 'SUPERUSER') this.superuser = true;
        if (response.data[role] === 'ADMIN') this.admin = true;
      }
      this.isRolesLoaded = true;
    });
  },
  methods: {
    showAddNewCodeSetDialog() {
      this.addNewCodeSetDialogVisible = true;
    },
    hideAddNewCodeSetDialog() {
      this.addNewCodeSetDialogVisible = false;
    },
    showRemoveCodeSetDialog(codeset) {
      this.removeCodeSetName = codeset;
      this.removeCodeSetDialogVisible = true;
    },
    hideRemoveCodeSetDialog() {
      this.removeCodeSetDialogVisible = false;
    },
    itemHeader(item) {
      return item.title + ' (' + item.count + ')';
    },
    rowClicked(value) {
      if (value != '' && value != null && value != undefined) {
        this.activeCodeSetItem = false;
        this.activeCodeSetId = value;
        axios.get('/admin-ui/api/codes/codesets/' + value).then(response => {
          this.codesetItems = [];
          response.data.forEach(item => {
            this.codesetItems.push({ title: item.codeUri, id: item.id });
          });
        });
        this.selectedCodeSetItem = null;
      }
    },
    itemRowClicked(value) {
      this.selectedCodeSetItem = null;
      this.activeCodeSetItem = value;
      axios.get('/admin-ui/api/codes/code/' + value).then(response => {
        this.selectedCodeSetItem = response.data;
      });
    },
    addNewCodeSet() {
      if (this.$refs.addNewCodeSetForm.validate()) {
        this.hideAddNewCodeSetDialog();
        axios.put('/admin-ui/api/codes/codesets/' + this.addNewCodeSetName).then(() => {
          this.refresh();
        });
        this.$refs.addNewCodeSetForm.reset();
      }
    },
    removeCodeSet() {
      this.hideRemoveCodeSetDialog();
      axios.delete('/admin-ui/api/codes/codesets/' + this.removeCodeSetName).then(() => {
        this.refresh();
      });
    },
    itemRowDelete(value) {
      axios.delete('/admin-ui/api/codes/code/' + value).then(() => {
        this.activeCodeSetItem = false;
        this.initCodeSets();
        this.rowClicked(this.activeCodeSetId);
      });
    },
    addItem() {
      axios.put('/admin-ui/api/codes/codesets/' + this.activeCodeSetId).then(response => {
        this.codesetItems.push({ title: response.data.codeUri, id: response.data.id });
        this.itemRowClicked(response.data.id);
        this.initCodeSets();
      });
    },
    refresh() {
      this.activeCodeSetId = null;
      this.activeCodeSetItem = false;
      this.codesetItems = [];
      this.selectedCodeSetItem = null;
      this.initCodeSets();
    },
    saved() {
      axios.get('/admin-ui/api/codes/codesets/' + this.activeCodeSetId).then(response => {
        this.codesetItems = [];
        response.data.forEach(item => {
          this.codesetItems.push({ title: item.codeUri, id: item.id });
        });
        var tmp = this.activeCodeSetItem;
        this.activeCodeSetItem = false;
        this.itemRowClicked(tmp);
      });
    },
    initCodeSets() {
      axios.get('/admin-ui/api/codes/codesets').then(response => {
        this.codesets = [];
        if (response.data) {
          response.data.forEach(item => {
            this.codesets.push({ title: item.key, count: item.count });
          });
        }
      });
    },
    requiredRule(value) {
      return !!value || this.$t('requiredField');
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
.v-list__tile__action {
  cursor: pointer;
}
.v-list__tile {
  padding: 0 0 0 16px;
}
.list-tile:hover {
  background: rgba(0, 0, 0, 0.1);
}
</style>
