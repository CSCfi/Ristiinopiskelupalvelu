<template>
  <div class="codeset-container">
    <v-card>
      <v-card-title class="d-flex align-center">
        <span>{{ $t('codeValues') }}</span>
        <v-spacer></v-spacer>
        <v-btn
          variant="text"
          icon
          v-if="superuser"
          title="Lisää"
          @click="addItem"
          :disabled="!activeCodeSetId"
        >
          <v-icon icon="mdi-plus"></v-icon>
        </v-btn>
      </v-card-title>
      <v-list>
        <v-list-item
          v-for="item in codesetItems"
          :key="item.id"
          active-class="highlighted"
          :class="['list-tile', item.id === activeMessage ? 'highlighted' : '']"
          @click="onClickRow(item.id)"
        >
          <template #append>
            <v-btn
              v-if="superuser === true"
              variant="text"
              color="red"
              icon
              title="Poista"
              @click.stop="removeDialog(item.id)"
            >
              <v-icon icon="mdi-delete"></v-icon>
            </v-btn>
          </template>
          <v-list-item-title v-text="item.title" :id="item.id"></v-list-item-title>
        </v-list-item>
      </v-list>
    </v-card>
    <v-dialog v-model="dialog" width="600px">
      <v-card>
        <v-card-title class="headline">{{ $t('codeRemoveConfirmation') }}</v-card-title>
        <v-card-text>
        {{ $t('codeRemoveConfirmationInfo') }}
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="green" variant="text" @click="dialog = false">
          {{ $t('cancel') }}
        </v-btn>
        <v-btn color="green" variant="text" @click="onDelete">
          {{ $t('yes') }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';

const props = defineProps({
  admin: {
    type: Boolean,
    default: false
  },
  superuser: {
    type: Boolean,
    default: false
  },
  codesetItems: {
    type: Array,
    default: () => []
  },
  activeItem: {
    type: [Boolean, String, Number],
    default: false
  },
  activeCodeSetId: String
});

const emit = defineEmits(['itemRowClicked', 'addItem', 'itemRowDelete']);

const dialog = ref(false);
const removeItem = ref(null);

const activeMessage = computed(() => props.activeItem);

const onClickRow = id => {
  emit('itemRowClicked', id);
};

const addItem = () => {
  emit('addItem');
};

const onDelete = () => {
  emit('itemRowDelete', removeItem.value);
  dialog.value = false;
};

const removeDialog = item => {
  removeItem.value = item;
  dialog.value = true;
};
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
