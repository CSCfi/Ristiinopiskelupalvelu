<template>
  <div v-if="currentOrganisation">
    <v-alert class="alert" v-if="success" type="success">
      {{ $t('organisation.saveSuccess') }}
    </v-alert>
    <v-alert class="alert" v-if="error" type="error">
      {{ $t('organisation.saveError') }}
    </v-alert>
    <v-alert class="alert" v-if="errorEmptyFields" type="error">
      {{ $t('organisation.requiredFieldsMissing') }}
    </v-alert>

    <v-form id="organisation-form" ref="organisationForm" :lazy-validation="false">
      <div class="d-flex justify-end mb-4 pr-3">
        <v-btn small color="success" :disabled="saving" class="save-btn" @click="saveOrganisation">
          {{ !newOrganisation ? $t('save') : $t('saveNew') }}
        </v-btn>
      </div>

      <v-text-field
        :readonly="!isSuperUser || !newOrganisation"
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
        :color="isSuperUser ? 'primary' : undefined"
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
        item-title="text"
        item-value="value"
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
        <template #item="{ item }">
          <tr>
            <td>{{ item.id }}</td>
            <td>{{ item.abbreviation }}</td>
            <td>{{ item.name?.values?.fi }}</td>
            <td>{{ item.name?.values?.en }}</td>
            <td>{{ item.name?.values?.sv }}</td>
            <td>{{ item.validity?.continuity }}</td>
            <td>{{ item.validity?.start }}</td>
            <td>{{ item.validity?.end }}</td>
          </tr>
        </template>
        <template #pageText="props">
          {{ props.pageStart }} - {{ props.pageStop }} / {{ props.itemsLength }}
        </template>
      </v-data-table>
    </v-form>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import cloneDeep from 'lodash/cloneDeep';
import LocalisedTextField from '../LocalisedTextField.vue';
import NestedObjectTextField from '../NestedObjectTextField.vue';
import NestedObjectSelect from '../NestedObjectSelect.vue';
import { useOrganisationStore } from '../../stores/organisations';

const props = defineProps({
  active: {
    default: ''
  },
  locale: {
    type: String,
    default: 'fi'
  },
  newOrganisation: {
    type: Boolean,
    default: false
  },
  isSuperUser: {
    type: Boolean,
    default: false
  },
  selectedOrganisation: {
    type: Object,
    default: () => null
  }
});

const emit = defineEmits(['savedOrganisation']);
const { t } = useI18n();
const organisationStore = useOrganisationStore();

const organisationForm = ref(null);
const currentOrganisation = ref(cloneDeep(props.selectedOrganisation));
const success = ref(false);
const error = ref(false);
const errorEmptyFields = ref(false);
const countries = ref([]);
const schemaVersions = ref([]);
const saving = ref(false);

const notificationsEnabled = computed({
  get() {
    return !currentOrganisation.value?.notificationsEnabled
      ? false
      : currentOrganisation.value.notificationsEnabled;
  },
  set(newValue) {
    if (!currentOrganisation.value) return;
    currentOrganisation.value.notificationsEnabled = newValue;
  }
});

watch(
  () => props.selectedOrganisation,
  newOrg => {
    currentOrganisation.value = cloneDeep(newOrg);
  }
);

watch(
  () => props.locale,
  () => {
    localizeTexts();
  }
);

onMounted(() => {
  localizeTexts();
  loadSchemaVersions();
});

const nameRequired = name => {
  if (!name || !name.values) {
    return t('nameRequired');
  }

  const hasValue = Object.values(name.values).some(val => !!val);
  return hasValue ? true : t('nameRequired');
};

const requiredRule = value => !!value || t('requiredField');

const emailRule = value => /.+@.+\..+/.test(value) || t('emailNotValid');

const validate = async () => {
  if (!organisationForm.value) {
    return false;
  }
  const validationResult = await organisationForm.value.validate();
  const isValid =
    typeof validationResult === 'boolean'
      ? validationResult
      : validationResult?.valid ?? false;

  if (isValid) {
    return true;
  }

  errorEmptyFields.value = true;
  setTimeout(() => {
    errorEmptyFields.value = false;
  }, 4000);
  return false;
};

const saveOrganisation = async () => {
  saving.value = true;
  if (await validate()) {
    try {
      let response;
      if (!props.newOrganisation) {
        const { networks, ...orgData } = currentOrganisation.value || {};
        response = await organisationStore.updateOrganisation(currentOrganisation.value.id, {
          ...orgData
        });
      } else {
        response = await organisationStore.addOrganisation(currentOrganisation.value);
      }
      currentOrganisation.value = cloneDeep(response);
      emit('savedOrganisation', response);
      success.value = true;
      setTimeout(() => {
        success.value = false;
      }, 5000);
    } catch (err) {
      error.value = true;
      setTimeout(() => {
        error.value = false;
      }, 5000);
    }
  }
  saving.value = false;
};

const localizeTexts = () => {
  countries.value = [
    { text: '', value: null },
    { text: t('organisation.country.finland'), value: '246' },
    { text: t('organisation.country.sweden'), value: '222' }
  ];
};

const loadSchemaVersions = async () => {
  try {
    const versions = await organisationStore.fetchSchemaVersions();
    schemaVersions.value = versions.map(sv => ({
      text: sv,
      value: sv
    }));
  } catch (err) {
    console.log(`Unable to load schema version values`);
  }
};

const getOrganisationNetworkHeaders = () => {
  return [
    {
      title: t('organisation.networks.id'),
      key: 'id'
    },
    {
      title: t('organisation.networks.abbreviation'),
      key: 'abbreviation'
    },
    {
      title: t('organisation.networks.name.fi'),
      key: 'name.values.fi'
    },
    {
      title: t('organisation.networks.name.en'),
      key: 'name.values.en'
    },
    {
      title: t('organisation.networks.name.sv'),
      key: 'name.values.sv'
    },
    {
      title: t('organisation.networks.validity.continuity'),
      key: 'validity.continuity'
    },
    {
      title: t('organisation.networks.validity.start'),
      key: 'validity.start'
    },
    {
      title: t('organisation.networks.validity.end'),
      key: 'validity.end'
    }
  ];
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
  width: auto;
  float: none;
}
.header-left {
  width: auto;
  float: none;
  margin: 0;
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
.layout {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.save-btn {
  margin-left: 8px;
}
</style>
