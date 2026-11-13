<template>
  <v-card>
    <v-container fluid>
      <v-row>
        <v-col cols="12">
          <v-card>
            <div v-if="superuser === false">
              <v-card-title class="py-4">
                <h3>{{ $t('noPermissions') }}</h3>
              </v-card-title>
              <v-card-text>
                <span>{{ $t('noPermissions2') }}</span>
              </v-card-text>
            </div>
            <CodesetUpload v-if="superuser === true" @refresh="refresh" />
          </v-card>
        </v-col>
        <v-col cols="12" md="3">
          <v-card>
            <v-card-title class="d-flex align-center">
              <span>{{ $t('codesets') }}</span>
              <v-spacer></v-spacer>
              <v-btn
                v-if="superuser"
                variant="text"
                title="Lisää"
                icon
                @click="showAddNewCodeSetDialog()"
              >
                <v-icon icon="mdi-plus"></v-icon>
              </v-btn>
            </v-card-title>
            <v-card-text>
              <v-list>
                <v-list-item
                  v-for="item in codesets"
                  :key="item.title"
                  active-class="highlighted"
                  :class="['list-tile', item.title === activeMessage ? 'highlighted' : '']"
                  @click="rowClicked(item.title)"
                >
                  <template #append>
                    <v-btn
                      v-if="superuser === true"
                      variant="text"
                      color="red"
                      icon
                      title="Poista"
                      @click.stop="showRemoveCodeSetDialog(item.title)"
                    >
                      <v-icon icon="mdi-delete"></v-icon>
                    </v-btn>
                  </template>
                  <v-list-item-title v-text="itemHeader(item)" :id="item.title" />
                </v-list-item>
              </v-list>
            </v-card-text>
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
                    <v-btn color="red" variant="text" @click="hideAddNewCodeSetDialog()">
                      {{ $t('cancel') }}
                    </v-btn>
                    <v-btn color="green" variant="text" @click="addNewCodeSet()">
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
                  <v-btn color="red" variant="text" @click="hideRemoveCodeSetDialog()">
                    {{ $t('cancel') }}
                  </v-btn>
                  <v-btn color="green" variant="text" @click="removeCodeSet()">
                    {{ $t('yes') }}
                  </v-btn>
                </v-card-actions>
              </v-card>
            </v-dialog>
          </v-card>
        </v-col>
        <v-col cols="12" md="3">
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
        </v-col>
        <v-col cols="12" md="6">
          <v-card height="100%">
            <CodesetEditor
              :locale="$i18n.locale"
              v-bind:activeItem="activeCodeSetItem"
              v-bind:superuser="superuser"
              v-bind:admin="admin"
              v-model="selectedCodeSetItem"
              v-if="isRolesLoaded"
              @refresh="refresh"
              @saved="saved"
            ></CodesetEditor>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </v-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import CodesetUpload from '../components/codeset/CodesetUpload.vue';
import CodesetItems from '../components/codeset/CodesetItems.vue';
import CodesetEditor from '../components/codeset/CodesetEditor.vue';
import { useCodesetStore } from '../stores/codesets';
import { useUserStore } from '../stores/user';
import { storeToRefs } from 'pinia';

const { t } = useI18n();

const activeCodeSetId = ref(null);
const activeCodeSetItem = ref(false);
const selectedCodeSetItem = ref(null);
const codesets = ref([]);
const codesetItems = ref([]);
const addNewCodeSetDialogVisible = ref(false);
const addNewCodeSetName = ref(null);
const removeCodeSetDialogVisible = ref(false);
const removeCodeSetName = ref(null);
const superuser = ref(false);
const admin = ref(false);
const isRolesLoaded = ref(false);
const addNewCodeSetForm = ref(null);

const activeMessage = computed(() => activeCodeSetId.value);

const codesetStore = useCodesetStore();
const userStore = useUserStore();
const { roles } = storeToRefs(userStore);

onMounted(() => {
  initCodeSets();
  userStore.fetchRoles().then(() => {
    roles.value?.forEach(role => {
      if (role === 'SUPERUSER') superuser.value = true;
      if (role === 'ADMIN') admin.value = true;
    });
    isRolesLoaded.value = true;
  });
});

const showAddNewCodeSetDialog = () => {
  addNewCodeSetDialogVisible.value = true;
};
const hideAddNewCodeSetDialog = () => {
  addNewCodeSetDialogVisible.value = false;
};
const showRemoveCodeSetDialog = codeset => {
  removeCodeSetName.value = codeset;
  removeCodeSetDialogVisible.value = true;
};
const hideRemoveCodeSetDialog = () => {
  removeCodeSetDialogVisible.value = false;
};
const itemHeader = item => item.title + ' (' + item.count + ')';

const rowClicked = value => {
  if (value !== '' && value != null) {
    activeCodeSetItem.value = false;
    activeCodeSetId.value = value;
    codesetStore.fetchCodesetItems(value).then(responseItems => {
      codesetItems.value = [];
      responseItems.forEach(item => {
        codesetItems.value.push({ title: item.codeUri, id: item.id });
      });
    });
    selectedCodeSetItem.value = null;
  }
};

const itemRowClicked = value => {
  selectedCodeSetItem.value = null;
  activeCodeSetItem.value = value;
  codesetStore.fetchCode(value).then(data => {
    selectedCodeSetItem.value = data
      ? {
          ...data,
          codeValues: Array.isArray(data.codeValues) ? data.codeValues : []
        }
      : null;
  });
};

const addNewCodeSet = async () => {
  const form = addNewCodeSetForm.value;
  if (!form) return;
  const validationResult = await form.validate();
  const isValid =
    typeof validationResult === 'boolean'
      ? validationResult
      : validationResult?.valid ?? false;
  if (!isValid) return;

  hideAddNewCodeSetDialog();
  await codesetStore.addCodeset(addNewCodeSetName.value);
  refresh();
  form.reset();
};

const removeCodeSet = () => {
  hideRemoveCodeSetDialog();
  codesetStore.deleteCodeset(removeCodeSetName.value).then(() => refresh());
};

const itemRowDelete = value => {
  codesetStore.deleteCode(value).then(() => {
    activeCodeSetItem.value = false;
    initCodeSets();
    rowClicked(activeCodeSetId.value);
  });
};

const addItem = async () => {
  const response = await codesetStore.addCodeToCodeset(activeCodeSetId.value);
  codesetItems.value.push({ title: response.codeUri, id: response.id });
  itemRowClicked(response.id);
  initCodeSets();
};

const refresh = () => {
  activeCodeSetId.value = null;
  activeCodeSetItem.value = false;
  codesetItems.value = [];
  selectedCodeSetItem.value = null;
  initCodeSets();
};

const saved = () => {
  codesetStore.fetchCodesetItems(activeCodeSetId.value).then(responseItems => {
    codesetItems.value = [];
    responseItems.forEach(item => {
      codesetItems.value.push({ title: item.codeUri, id: item.id });
    });
    const tmp = activeCodeSetItem.value;
    activeCodeSetItem.value = false;
    itemRowClicked(tmp);
  });
};

const initCodeSets = () => {
  codesetStore.fetchCodesets().then(data => {
    codesets.value = [];
    if (data) {
      data.forEach(item => {
        codesets.value.push({ title: item.key, count: item.count });
      });
    }
  });
};

const requiredRule = value => !!value || t('requiredField');
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
.v-list-item__append {
  cursor: pointer;
}
.v-list-item {
  padding: 0 0 0 16px;
}
.list-tile:hover {
  background: rgba(0, 0, 0, 0.1);
}
</style>
