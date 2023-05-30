<template>
  <div class="codeset-container">
    <v-card>
      <v-toolbar>
        <v-toolbar-title>{{ $t('codeValues') }}</v-toolbar-title>
        <v-spacer></v-spacer>
        <v-toolbar-items>
          <v-btn flat v-if="superuser" title="Lisää" @click="addItem" :disabled="!activeCodeSetId">
            <v-icon>add</v-icon>
          </v-btn>
        </v-toolbar-items>
      </v-toolbar>
      <v-list>
        <v-list-tile
          v-for="item in codesetItems"
          :key="item.id"
          active-class="highlighted"
          :class="['list-tile', item.id === activeMessage ? 'highlighted' : '']"
        >
          <v-list-tile-action
            v-if="superuser === true"
            title="Poista"
            @click="removeDialog(item.id)"
          >
            <v-icon color="red">delete</v-icon>
          </v-list-tile-action>
          <v-list-tile-content @click="onClickRow(item.id)">
            <v-list-tile-title v-text="item.title" :id="item.id"></v-list-tile-title>
          </v-list-tile-content>
        </v-list-tile>
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
          <v-btn color="green darken-1" flat="flat" @click="dialog = false">
            {{ $t('cancel') }}
          </v-btn>
          <v-btn color="green darken-1" flat="flat" @click="onDelete">
            {{ $t('yes') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script>
export default {
  props: {
    admin: {
      default() {
        return false;
      }
    },
    superuser: {
      default() {
        return false;
      }
    },
    codesetItems: {
      default() {
        return [];
      }
    },
    activeItem: {
      default() {
        return false;
      }
    },
    activeCodeSetId: String
  },
  data() {
    return {
      dialog: false,
      removeItem: null
    };
  },
  created: function() {},
  computed: {
    activeMessage: function() {
      return this.activeItem;
    }
  },
  methods: {
    onClickRow(id) {
      this.$emit('itemRowClicked', id);
    },
    addItem() {
      this.$emit('addItem');
    },
    onDelete() {
      this.$emit('itemRowDelete', this.removeItem);
      this.dialog = false;
    },
    removeDialog(item) {
      this.removeItem = item;
      this.dialog = true;
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
.v-toolbar__content {
  padding: 0 0 0 24px;
}
.list-tile:hover {
  background: rgba(0, 0, 0, 0.1);
}
</style>
