<template>
  <div>
    <v-toolbar>
      <v-toolbar-title>{{ $t('editor') }}</v-toolbar-title>
    </v-toolbar>

    <v-form v-if="value != null" id="code-form" ref="codeForm" :lazy-validation="false">
      <v-text-field v-model="value.id" :label="$t('codeIdentifier')" disabled></v-text-field>
      <v-text-field v-model="value.codeUri" :label="$t('codeUri')"></v-text-field>
      <v-text-field v-model="value.resourceUri" :label="$t('codeResourceUri')"></v-text-field>
      <v-text-field v-model="value.key" :label="$t('codeKey')"></v-text-field>

      <v-text-field
        v-model="value.codeVersion"
        type="number"
        :label="$t('codeVersion')"
      ></v-text-field>

      <v-text-field v-model="value.updateDate" :label="$t('updateDate')" disabled></v-text-field>

      <v-menu
        :close-on-content-click="true"
        :nudge-right="40"
        lazy
        transition="scale-transition"
        offset-y
        full-width
        min-width="290px"
      >
        <template v-slot:activator="{ on }">
          <v-text-field
            v-model="value.validityStartDate"
            :label="$t('startDate')"
            prepend-icon="event"
            readonly
            :rules="[requiredRule]"
            v-on="on"
          ></v-text-field>
        </template>
        <v-date-picker :locale="locale" v-model="value.validityStartDate" no-title scrollable>
        </v-date-picker>
      </v-menu>

      <v-menu
        :close-on-content-click="true"
        :nudge-right="40"
        lazy
        transition="scale-transition"
        offset-y
        full-width
        min-width="290px"
      >
        <template v-slot:activator="{ on }">
          <v-text-field
            v-model="value.validityEndDate"
            :label="$t('endDate')"
            prepend-icon="event"
            readonly
            :rules="[requiredRule]"
            v-on="on"
          ></v-text-field>
        </template>
        <v-date-picker :locale="locale" v-model="value.validityEndDate" no-title scrollable>
        </v-date-picker>
      </v-menu>

      <v-text-field v-model="value.status" :label="$t('codeStatus')"></v-text-field>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('codeValues') }}</h2>
        <v-btn class="btn-right" flat @click="addCodeValue">
          {{ $t('addCodeValue') }}
        </v-btn>
      </div>

      <v-layout row wrap v-for="(codeValue, index) in value.codeValues" :key="index + '-codeValue'">
        <v-flex xs4>
          <v-select
            v-model="codeValue.language"
            :items="['fi', 'sv', 'en']"
            :label="$t('codeValueLanguage')"
            :rules="[requiredRule]"
          ></v-select>
        </v-flex>

        <v-flex xs4>
          <v-text-field
            v-model="codeValue.value"
            :label="$t('codeValueValue')"
            :rules="[requiredRule]"
          ></v-text-field>
        </v-flex>

        <v-flex xs4>
          <v-text-field
            v-model="codeValue.description"
            :label="$t('codeValueDescription')"
          ></v-text-field>
        </v-flex>

        <v-flex xs4>
          <v-btn color="error" flat @click="deleteCodeValue(index)">
            {{ $t('delete') }}
          </v-btn>
        </v-flex>
      </v-layout>

      <v-btn
        v-if="superuser === true"
        @click="update"
        small
        color="success"
        :disabled="activeItem == '' ? true : false"
      >
        {{ $t('save') }}
      </v-btn>
    </v-form>
    <v-alert :value="success" type="success">
      {{ $t('codesetUpdated') }}
    </v-alert>
    <v-alert :value="error" type="error">
      {{ $t('codesetUpdateError') }}
    </v-alert>
  </div>
</template>

<script>
import axios from 'axios';
export default {
  props: {
    options: {
      type: Object,
      default() {
        return {};
      }
    },
    value: {
      default() {
        return {};
      }
    },
    activeItem: {
      default() {
        return '';
      }
    },
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
    locale: {
      default() {
        return 'fi';
      }
    }
  },
  data() {
    return {
      success: false,
      error: false,
      dialog: false
    };
  },
  watch: {
    value() {}
  },
  methods: {
    getOptions() {
      const defaults = {
        escapeUnicode: false,
        history: true,
        indentation: 2,
        navigationBar: false,
        search: true,
        statusBar: false,
        sortObjectKeys: false
      };

      return Object.assign({}, defaults, this.options);
    },
    update() {
      if (this.$refs.codeForm.validate()) {
        axios
          .post('/admin-ui/api/codes/update', JSON.stringify(this.value), {
            headers: {
              'Content-Type': 'application/json;charset=UTF-8'
            }
          })
          .then(() => {
            this.success = true;
            this.$emit('saved');
            setTimeout(() => {
              this.success = false;
            }, 5000);
          })
          .catch(() => {
            this.error = true;
            setTimeout(() => {
              this.error = false;
            }, 5000);
          });
      }
    },
    addCodeValue() {
      if (!this.value.codeValues) {
        this.value.codeValues = [];
      }
      this.value.codeValues.push({ language: '', value: '', description: '' });
    },
    deleteCodeValue(index) {
      if (this.value.codeValues && this.value.codeValues.length > 0) {
        this.value.codeValues.splice(index, 1);
      }
    },
    requiredRule(value) {
      return !!value || this.$t('requiredField');
    }
  },
  mounted() {},
  beforeDestroy() {}
};
</script>
