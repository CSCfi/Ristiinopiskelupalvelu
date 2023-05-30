<template>
  <div v-if="currentOrganisation">
    <v-alert class="alert" :value="success" type="success">
      {{ $t('organisation.saveSuccess') }}
    </v-alert>
    <v-alert class="alert" :value="error" type="error">
      {{ $t('organisation.saveError') }}
    </v-alert>
    <v-alert class="alert" :value="errorEmptyFields" type="error">
      {{ $t('organisation.requiredFieldsMissing') }}
    </v-alert>

    <v-form id="organisation-form" ref="organisationForm" :lazy-validation="false">
      <v-btn small color="success" :disabled="saving" class="save right" @click="saveOrganisation">
        {{ !newOrganisation ? $t('save') : $t('saveNew') }}
      </v-btn>

      <v-text-field
        :disabled="!isSuperUser || !newOrganisation"
        v-model="currentOrganisation.id"
        :label="$t('organisation.organisationTkCode')"
        :rules="[requiredRule]"
      ></v-text-field>

      <v-text-field
        v-model="currentOrganisation.organisationIdentifier"
        :label="$t('organisation.organisationIdentifier')"
        :rules="[]"
      ></v-text-field>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('organisation.organisationName') }}</h2>
      </div>
      <LocalisedTextField
        v-model="currentOrganisation.organisationName"
        :rules="[nameRequired(currentOrganisation.organisationName)]"
      ></LocalisedTextField>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('organisation.unitName') }}</h2>
      </div>
      <LocalisedTextField v-model="currentOrganisation.unitName"></LocalisedTextField>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('organisation.nameDescription') }}</h2>
      </div>
      <LocalisedTextField v-model="currentOrganisation.nameDescription"></LocalisedTextField>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('organisation.postalAddress') }}</h2>
      </div>
      <NestedObjectTextField
        v-model="currentOrganisation.postalAddress"
        field="street"
        label="organisation.street"
      ></NestedObjectTextField>
      <NestedObjectTextField
        v-model="currentOrganisation.postalAddress"
        field="postalCode"
        label="organisation.postalCode"
      ></NestedObjectTextField>
      <NestedObjectTextField
        v-model="currentOrganisation.postalAddress"
        field="postOffice"
        label="organisation.postOffice"
      ></NestedObjectTextField>
      <NestedObjectSelect
        v-model="currentOrganisation.postalAddress"
        :items="countries"
        field="country"
        label="organisation.country"
      ></NestedObjectSelect>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('organisation.streetAddress') }}</h2>
      </div>
      <NestedObjectTextField
        v-model="currentOrganisation.streetAddress"
        field="street"
        label="organisation.street"
      ></NestedObjectTextField>
      <NestedObjectTextField
        v-model="currentOrganisation.streetAddress"
        field="postalCode"
        label="organisation.postalCode"
      ></NestedObjectTextField>
      <NestedObjectTextField
        v-model="currentOrganisation.streetAddress"
        field="postOffice"
        label="organisation.postOffice"
      ></NestedObjectTextField>
      <NestedObjectSelect
        v-model="currentOrganisation.streetAddress"
        :items="countries"
        field="country"
        label="organisation.country"
      ></NestedObjectSelect>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('organisation.phone') }}</h2>
      </div>
      <NestedObjectTextField
        v-model="currentOrganisation.phone"
        field="number"
        label="organisation.phonenumber"
      ></NestedObjectTextField>
      <NestedObjectTextField
        v-model="currentOrganisation.phone"
        field="description"
        label="organisation.phonedescription"
      ></NestedObjectTextField>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('organisation.url') }}</h2>
      </div>
      <v-text-field
        v-model="currentOrganisation.url"
        :label="$t('organisation.url')"
        :rules="[]"
      ></v-text-field>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('organisation.settings') }}</h2>
      </div>
      <v-switch
        :disabled="!isSuperUser"
        v-model="notificationsEnabled"
        :label="$t('organisation.notificationsEnabled')"
      ></v-switch>
      <v-text-field
        v-model="currentOrganisation.queue"
        :disabled="!isSuperUser"
        :label="$t('organisation.queue')"
        :rules="[requiredRule]"
      ></v-text-field>
      <v-text-field
        v-model="currentOrganisation.administratorEmail"
        :label="$t('organisation.administratorEmail')"
        :rules="[requiredRule, emailRule]"
      ></v-text-field>
      <v-select
        v-model="currentOrganisation.schemaVersion"
        :items="schemaVersions"
        filled
        :label="$t('organisation.schemaVersion')"
        :rules="[requiredRule]"
        :disabled="!isSuperUser"
      ></v-select>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('organisation.networks') }}</h2>
      </div>
      <v-data-table
        :headers="getOrganisationNetworkHeaders()"
        :items="currentOrganisation.networks"
        :items-per-page="5"
        :no-data-text="$t('organisation.networks.no-data-text')"
        :no-results-text="$t('organisation.networks.no-resuls-text')"
        :rows-per-page-text="$t('organisation.networks.results-per-page-text')"
        class="elevation-1"
      >
        <template v-slot:items="props">
          <td>{{ props.item.id }}</td>
          <td>{{ props.item.abbreviation }}</td>
          <td>{{ props.item.name.values.fi }}</td>
          <td>{{ props.item.name.values.en }}</td>
          <td>{{ props.item.name.values.sv }}</td>
          <td>{{ props.item.validity.continuity }}</td>
          <td>{{ props.item.validity.start }}</td>
          <td>{{ props.item.validity.end }}</td>
        </template>
        <template v-slot:pageText="props">
          {{ props.pageStart }} - {{ props.pageStop }} / {{ props.itemsLength }}
        </template>
      </v-data-table>
    </v-form>
  </div>
</template>

<script>
import axios from 'axios';
import LocalisedTextField from '../LocalisedTextField';
import NestedObjectTextField from '../NestedObjectTextField';
import NestedObjectSelect from '../NestedObjectSelect';

export default {
  components: {
    LocalisedTextField,
    NestedObjectTextField,
    NestedObjectSelect
  },
  computed: {
    notificationsEnabled: {
      get: function() {
        return !this.currentOrganisation.notificationsEnabled
          ? false
          : this.currentOrganisation.notificationsEnabled;
      },
      set: function(newValue) {
        this.currentOrganisation.notificationsEnabled = newValue;
      }
    }
  },
  props: {
    active: {
      default() {
        return '';
      }
    },
    locale: {
      default() {
        return 'fi';
      }
    },
    newOrganisation: {
      default() {
        return false;
      }
    },
    isSuperUser: {
      default() {
        return false;
      },
      type: Boolean
    },
    selectedOrganisation: Object
  },
  data() {
    return {
      currentOrganisation: this.selectedOrganisation,
      success: false,
      error: false,
      errorEmptyFields: false,
      countries: [],
      schemaVersions: [],
      saving: false
    };
  },
  watch: {
    selectedOrganisation: function() {
      this.currentOrganisation = this.selectedOrganisation;
    },
    locale: function() {
      this.localizeTexts();
    }
  },
  created: async function() {
    this.localizeTexts();
    this.loadSchemaVersions();
  },
  methods: {
    nameRequired(name) {
      if (!name) {
        return this.$t('nameRequired');
      }

      let noNamesGiven = true;

      Object.keys(name.values).forEach(function(key) {
        if (name.values[key]) {
          noNamesGiven = false;
          return;
        }
      });
      return noNamesGiven ? this.$t('nameRequired') : false;
    },
    requiredRule(value) {
      return !!value || this.$t('requiredField');
    },
    emailRule(value) {
      return /.+@.+\..+/.test(value) || this.$t('emailNotValid');
    },
    validate() {
      if (this.$refs.organisationForm.validate()) {
        return true;
      } else {
        this.errorEmptyFields = true;
        setTimeout(() => {
          this.errorEmptyFields = false;
        }, 4000);
        return false;
      }
    },
    async saveOrganisation() {
      this.saving = true;
      if (this.validate()) {
        try {
          let response;
          if (!this.newOrganisation) {
            // eslint-disable-next-line no-unused-vars
            const { networks, ...orgData } = this.currentOrganisation;
            response = await axios.put(
              '/admin-ui/api/organisations/' + this.currentOrganisation.id,
              orgData
            );
          } else {
            response = await axios.post('/admin-ui/api/organisations/', this.currentOrganisation);
          }
          this.$emit('savedOrganisation', response.data);
          this.success = true;
          setTimeout(() => {
            this.success = false;
          }, 5000);
        } catch (err) {
          this.error = true;
          setTimeout(() => {
            this.error = false;
          }, 5000);
        }
      }
      this.saving = false;
    },
    localizeTexts() {
      this.countries = [
        { text: '', value: null },
        { text: this.$t('organisation.country.finland'), value: '246' },
        { text: this.$t('organisation.country.sweden'), value: '222' }
      ];
    },
    async loadSchemaVersions() {
      try {
        const schemaVersionsResponse = await axios.get(
          '/admin-ui/api/organisations/schemaversions'
        );
        if (schemaVersionsResponse.data) {
          this.schemaVersions = schemaVersionsResponse.data.map(sv => {
            return {
              text: sv,
              value: sv
            };
          });
        }
      } catch (err) {
        console.log(`Unable to load schema version values`);
      }
    },
    getOrganisationNetworkHeaders() {
      return [
        {
          text: this.$t('organisation.networks.id'),
          value: 'id'
        },
        {
          text: this.$t('organisation.networks.abbreviation'),
          value: 'abbreviation'
        },
        {
          text: this.$t('organisation.networks.name.fi'),
          value: 'name.values.fi'
        },
        {
          text: this.$t('organisation.networks.name.en'),
          value: 'name.values.en'
        },
        {
          text: this.$t('organisation.networks.name.sv'),
          value: 'name.values.sv'
        },
        {
          text: this.$t('organisation.networks.validity.continuity'),
          value: 'validity.continuity'
        },
        {
          text: this.$t('organisation.networks.validity.start'),
          value: 'validity.start'
        },
        {
          text: this.$t('organisation.networks.validity.end'),
          value: 'validity.end'
        }
      ];
    }
  }
};
</script>

<style lang="css">
.organisation {
  border: 2px solid #d4d4d4;
  padding: 5px !important;
  margin-bottom: 5px !important;
}
#organisation-form {
  margin-top: 0;
  padding: 8px 0px;
}
.btn-right {
  width: 30%;
  float: right;
}
.header-left {
  width: 60%;
  float: left;
  margin-top: 8px;
}
.error-validation {
  border: 1px solid rgb(255, 88, 88) !important;
  border-radius: 3px;
}
label {
  padding: 0 8px;
}
.alert {
  margin-top: 0;
}
.theme--light.v-select .v-select__selection--disabled {
  color: inherit
}
</style>
