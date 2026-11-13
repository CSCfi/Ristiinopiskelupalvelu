<template>
  <div v-if="currNetwork">
    <v-alert class="alert" v-if="success" type="success">
      {{ $t('networkUpdated') }}
    </v-alert>
    <v-alert class="alert" v-if="error" type="error">
      {{ $t('networkUpdateError') }}
    </v-alert>
    <v-alert class="alert" v-if="errorEmptyFields" type="error">
      {{ $t('networkFormError') }}
    </v-alert>
    <v-alert class="alert" v-if="errorOrganisations" type="error">
      {{ $t('networkFormErrorOrganisations') }}
    </v-alert>

    <v-form id="network-form" ref="form" :lazy-validation="false" v-model="valid">
      <div class="d-flex justify-end mb-4 pr-3">
        <v-btn
          v-if="!newNetwork"
          small
          color="success"
          class="save-btn"
          @click="saveNetworkClicked"
        >
          {{ $t('save') }}
        </v-btn>
        <v-btn
          v-if="newNetwork"
          small
          color="success"
          class="save-btn"
          @click="saveNewNetworkClicked"
        >
          {{ $t('saveNew') }}
        </v-btn>
      </div>

      <v-text-field
        :model-value="currNetwork.id || $t('unsaved')"
        :label="$t('networkIdentifier')"
        readonly
        :disabled="false"
      >
        <template v-if="currNetwork.id" #append-inner>
          <v-tooltip :text="$t('copyToClipboard')" location="bottom">
            <template #activator="{ props }">
              <v-icon
                v-bind="props"
                icon="mdi-content-copy"
                class="copy-icon"
                @click.stop="copyNetworkId"
              />
            </template>
          </v-tooltip>
        </template>
      </v-text-field>

      <v-switch
        v-model="currNetwork.published"
        :label="$t('networkPublished')"
        :disabled="alreadyPublished"
        :color="!alreadyPublished ? 'primary' : undefined"
      ></v-switch>

        <v-select
          v-model="currNetwork.networkType"
          :items="networkTypes"
          item-title="text"
          item-value="value"
          filled
          :rules="[requiredRule]"
          :label="$t('network.type')"
          required
        ></v-select>

      <v-text-field
        v-model="currNetwork.abbreviation"
        :rules="[requiredRule]"
        :label="$t('abbreviation')"
        required
      ></v-text-field>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('network.name') }}</h2>
      </div>
      <v-text-field
        v-model="currNetwork.name.values.fi"
        :label="$t('fi')"
        :rules="[nameRequired(currNetwork.name)]"
      ></v-text-field>
      <v-text-field
        v-model="currNetwork.name.values.sv"
        :label="$t('sv')"
        :rules="[nameRequired(currNetwork.name)]"
      ></v-text-field>
      <v-text-field
        v-model="currNetwork.name.values.en"
        :label="$t('en')"
        :rules="[nameRequired(currNetwork.name)]"
      ></v-text-field>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('network.description') }}</h2>
      </div>
      <v-textarea v-model="currNetwork.description.values.fi" :label="$t('fi')"></v-textarea>
      <v-textarea v-model="currNetwork.description.values.sv" :label="$t('sv')"></v-textarea>
      <v-textarea v-model="currNetwork.description.values.en" :label="$t('en')"></v-textarea>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('network.furtherInformation') }}</h2>
      </div>
      <v-textarea v-model="currNetwork.furtherInformation.values.fi" :label="$t('fi')"></v-textarea>
      <v-textarea v-model="currNetwork.furtherInformation.values.sv" :label="$t('sv')"></v-textarea>
      <v-textarea v-model="currNetwork.furtherInformation.values.en" :label="$t('en')"></v-textarea>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('organisations') }}</h2>
        <v-btn class="btn-right" variant="text" @click="addOrg">
          {{ $t('addOrganisation') }}
        </v-btn>
      </div>
      <div v-if="currNetwork.organisations && currNetwork.organisations.length">
        <div
          class="organisation"
          v-for="(org, ix) in currNetwork.organisations"
          :key="ix"
        >
          <v-select
            v-model="org.organisationTkCode"
            :items="organisationMenuItems"
            item-title="text"
            item-value="value"
            filled
            :label="$t('organisationIdentifier')"
            :rules="[requiredRule]"
            required
            class="mb-3"
            @change="filterOrganisationList()"
          ></v-select>

          <v-menu
            v-model="menuItems[ix + 'modelMenuStart']"
            :close-on-content-click="false"
            :nudge-right="40"
            transition="scale-transition"
            offset-y
            full-width
            min-width="290px"
          >
            <template #activator="{ props }">
              <v-text-field
                v-model="org.validityInNetwork.start"
                :label="$t('startDate')"
                prepend-icon="mdi-calendar"
                readonly
                :rules="[requiredRule]"
                v-bind="props"
              ></v-text-field>
            </template>
            <v-date-picker
              :locale="locale"
              v-model="org.validityInNetwork.start"
              @update:model-value="
                val => (org.validityInNetwork.start = computedDateFormattedMomentjs(val))
              "
            >
            </v-date-picker>
          </v-menu>

          <v-menu
            v-model="menuItems[ix + 'modelMenuEnd']"
            :close-on-content-click="false"
            :nudge-right="40"
            transition="scale-transition"
            offset-y
            full-width
            min-width="290px"
          >
          <template #activator="{ props }">
            <v-text-field
              v-model="org.validityInNetwork.end"
              :label="$t('endDate')"
              :rules="[() => validityEndRequired(org.validityInNetwork)]"
              prepend-icon="mdi-calendar"
              readonly
              :disabled="org.validityInNetwork.continuity === continuityIndetifitely"
              v-bind="props"
              clearable
              @click:clear="org.validityInNetwork.end = ''"
            ></v-text-field>
          </template>
          <v-date-picker
            :locale="locale"
            v-model="org.validityInNetwork.end"
              @update:model-value="
                val => (org.validityInNetwork.end = computedDateFormattedMomentjs(val))
              "
            >
            </v-date-picker>
          </v-menu>

          <v-select
            v-model="org.validityInNetwork.continuity"
            :items="continuity"
            item-title="text"
            item-value="value"
            filled
            :label="$t('continuity')"
            :rules="[requiredRule]"
            @change="validityContinuityChanged($event, org.validityInNetwork)"
            required
            class="mb-3"
          ></v-select>

          <div class="d-flex align-center justify-space-between px-2 org-actions">
            <v-switch
              v-model="org.isCoordinator"
              :label="$t('isCoordinator')"
              class="mb-2"
              density="compact"
              color="primary"
            ></v-switch>
            <v-btn
              class="btn-right"
              variant="text"
              color="error"
              @click="deleteOrg(ix)"
            >
              {{ $t('delete') }}
            </v-btn>
          </div>
        </div>
      </div>
      <div v-else class="mt-2 mb-4">
        <em>{{ $t('noOrganisations') }}</em>
        <v-divider class="mt-2"></v-divider>
      </div>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('network.validity') }}</h2>
      </div>
      <div>
        <v-menu
          v-model="menuItems['modelMenuValidityStart']"
          :close-on-content-click="false"
          :nudge-right="40"
          transition="scale-transition"
          offset-y
          full-width
          min-width="290px"
        >
          <template #activator="{ props }">
            <v-text-field
              v-model="currNetwork.validity.start"
              :label="$t('startDate')"
              prepend-icon="mdi-calendar"
              :rules="[requiredRule]"
              readonly
              v-bind="props"
            ></v-text-field>
          </template>
          <v-date-picker
            :locale="locale"
            v-model="currNetwork.validity.start"
            @update:model-value="
              val => (currNetwork.validity.start = computedDateFormattedMomentjs(val))
            "
          >
          </v-date-picker>
        </v-menu>

        <v-menu
          v-model="menuItems['modelMenuValidityEnd']"
          :close-on-content-click="false"
          :nudge-right="40"
          transition="scale-transition"
          offset-y
          full-width
          min-width="290px"
        >
          <template #activator="{ props }">
            <v-text-field
            v-model="currNetwork.validity.end"
            :label="$t('endDate')"
            prepend-icon="mdi-calendar"
            :rules="[() => validityEndRequired(currNetwork.validity)]"
            :disabled="currNetwork.validity.continuity === continuityIndetifitely"
            readonly
            clearable
            v-bind="props"
          ></v-text-field>
          </template>
          <v-date-picker
            :locale="locale"
            v-model="currNetwork.validity.end"
            @update:model-value="
              val => (currNetwork.validity.end = computedDateFormattedMomentjs(val))
            "
          >
          </v-date-picker>
        </v-menu>

        <v-select
          v-model="currNetwork.validity.continuity"
          :items="continuity"
          item-title="text"
          item-value="value"
          filled
          :label="$t('continuity')"
          :rules="[requiredRule]"
          required
          @change="validityContinuityChanged($event, currNetwork.validity)"
        ></v-select>
      </div>
      <div class="layout wrap">
        <h2 class="header-left">{{ $t('targetGroups') }}</h2>
        <v-btn class="btn-right" variant="text" @click="addTargetGroup">
          {{ $t('addTargetGroup') }}
        </v-btn>
      </div>
      <div v-if="currNetwork.targetGroups && currNetwork.targetGroups.length">
        <v-layout
          class="targetGroup"
          row
          wrap
          v-for="(group, index) in currNetwork.targetGroups"
          :key="index + '-targetGroup'"
        >
          <v-flex xs4>
            <v-select
              v-model="group.key"
              :items="studyRightTypeCodeKeyValues"
              item-title="text"
              item-value="value"
              :label="$t('codeKey')"
              :rules="[requiredRule]"
            ></v-select>
          </v-flex>
          <v-flex xs4>
            <v-text-field
              v-model="group.codeSetKey"
              :label="$t('codeSetKey')"
              :disabled="true"
              :readonly="true"
              :rules="[requiredRule]"
            ></v-text-field>
          </v-flex>

          <v-flex xs4>
            <v-btn color="error" variant="text" @click="deleteTargetGroup(index)">
              {{ $t('delete') }}
            </v-btn>
          </v-flex>
        </v-layout>
      </div>
      <div v-else class="mt-2 mb-4">
        <em>{{ $t('noValues') }}</em>
        <v-divider class="mt-2"></v-divider>
      </div>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('restrictions') }}</h2>
        <v-btn class="btn-right" variant="text" @click="addRestriction">
          {{ $t('addRestriction') }}
        </v-btn>
      </div>
      <div v-if="currNetwork.restrictions && currNetwork.restrictions.length">
        <v-layout
          class="restriction"
          row
          wrap
          align-center
          justify-space-between
          v-for="(restriction, index) in currNetwork.restrictions"
          :key="index + '-restriction'"
        >
          <v-flex xs4>
            <v-select
              v-model="restriction.key"
              :items="studyRightTypeCodeKeyValues"
              item-title="text"
              item-value="value"
              :label="$t('codeKey')"
              :rules="[requiredRule]"
            ></v-select>
          </v-flex>

          <v-flex xs4>
            <v-text-field
              v-model="restriction.codeSetKey"
              :disabled="true"
              :readonly="true"
              :label="$t('codeSetKey')"
              :rules="[requiredRule]"
            ></v-text-field>
          </v-flex>

          <v-flex xs4>
            <v-btn color="error" variant="text" @click="deleteRestriction(index)">
              {{ $t('delete') }}
            </v-btn>
          </v-flex>
        </v-layout>
      </div>
      <div v-else class="mt-2 mb-4">
        <em>{{ $t('noValues') }}</em>
        <v-divider class="mt-2"></v-divider>
      </div>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('expenses') }}</h2>
      </div>
      <v-switch
        v-model="currNetwork.expenses.pay"
        :label="$t('pay')"
        color="primary"
        @change="payChanged"
      ></v-switch>
      <v-select
        v-model="currNetwork.expenses.pricingBasis"
        :items="pricingBasis"
        item-title="text"
        item-value="value"
        filled
        :label="$t('pricingBasis')"
        :rules="[expensesRequired]"
        :disabled="!currNetwork.expenses.pay"
      ></v-select>
      <v-text-field
        v-model="currNetwork.expenses.price"
        :label="$t('price')"
        :rules="[expensesRequired]"
        :disabled="!currNetwork.expenses.pay"
      ></v-text-field>
    </v-form>
    <v-dialog v-model="publishDialogVisible" width="600px">
      <v-card>
        <v-card-title class="headline">
          {{ $t('networkPublishConfirmation') }}
        </v-card-title>
        <v-card-text>
          {{ $t('networkPublishConfirmationInfo') }}
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="green" variant="text" @click="publishDialogVisible = false">
            {{ $t('cancel') }}
          </v-btn>
          <v-btn v-if="!newNetwork" color="green" variant="text" @click="saveNetwork">
            {{ $t('yes') }}
          </v-btn>
          <v-btn v-if="newNetwork" color="green" variant="text" @click="saveNewNetwork">
            {{ $t('yes') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-snackbar
      v-model="copyNotice"
      location="bottom right"
      timeout="1500"
      color="success"
    >
      {{ $t('copiedToClipboard') }}
    </v-snackbar>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import emptyNetwork from '../../assets/networkSchemas/emptyNetwork.json';
import moment from 'moment';
import _ from 'lodash';
import languageUtils from '../../utils/languageUtils';
import { useI18n } from 'vue-i18n';
import { useNetworkStore } from '../../stores/networks';
import { useOrganisationStore } from '../../stores/organisations';
import { useCodesetStore } from '../../stores/codesets';

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({ id: '' })
  },
  active: {
    default: ''
  },
  locale: {
    type: String,
    default: 'fi'
  },
  newNetwork: {
    type: Boolean,
    default: false
  },
  user: Object
});

const emit = defineEmits(['savedNetwork', 'savedNewNetwork', 'update:modelValue']);
const { t } = useI18n();
const networkStore = useNetworkStore();
const organisationStore = useOrganisationStore();
const codesetStore = useCodesetStore();

const continuityFixed = 'FIXED';
const continuityIndetifitely = 'INDEFINITELY';
const studyRightTypeCodeSet = 'study_right_type';

const studyRightTypeCodeKeyValues = ref([]);
const menuItems = reactive({});
const valid = ref(false);
const currNetwork = ref(null);
const updatingFromParent = ref(false);
const error = ref(false);
const errorEmptyFields = ref(false);
const errorOrganisations = ref(false);
const success = ref(false);
const copyNotice = ref(false);
const organisations = ref([]);
const organisationMenuItems = ref([]);
const networkTypes = ref([]);
const continuity = ref([]);
const pricingBasis = ref([]);
const alreadyPublished = ref(false);
const lastLoadedId = ref(null);
const publishDialogVisible = ref(false);

const form = ref(null);

const expensesRequired = value => {
  if (currNetwork.value?.expenses?.pay && !value) {
    return t('expensesRequired');
  }
  return true;
};

const requiredRule = value => !!value || t('requiredField');

const nameRequired = name => {
  if (!name || !name.values) {
    return t('nameRequired');
  }
  const hasValue = ['fi', 'sv', 'en'].some(lang => {
    const val = name.values[lang];
    return val !== undefined && val !== null && String(val).trim() !== '';
  });
  return hasValue ? true : t('nameRequired');
};

const validityEndRequired = validity => {
  if (validity.continuity === continuityFixed && !validity.end) {
    return t('requiredField');
  }
  return true;
};

const resetValidation = () => {
  if (!form.value) {
    return;
  }
  form.value.resetValidation();
};

const computedDateFormattedMomentjs = date =>
  date ? moment(date).format('YYYY-MM-DDTHH:mm:ssZ') : '';

const cleanUpData = () => {
  if (!currNetwork.value) return;
  delete currNetwork.value.version;
  delete currNetwork.value.deleted;
  delete currNetwork.value.deletedTimestamp;
};

const saveNetworkClicked = () => {
  if (currNetwork.value.published && !alreadyPublished.value) {
    publishDialogVisible.value = true;
  } else {
    saveNetwork();
  }
};

const saveNetwork = async () => {
  publishDialogVisible.value = false;
  cleanUpData();
  if (await validate()) {
    try {
      await networkStore.updateNetwork(currNetwork.value);
      emit('savedNetwork');
      alreadyPublished.value = !!(currNetwork.value.id && currNetwork.value.published);
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
};

const saveNewNetworkClicked = () => {
  if (currNetwork.value.published) {
    publishDialogVisible.value = true;
  } else {
    saveNewNetwork();
  }
};

const saveNewNetwork = async () => {
  publishDialogVisible.value = false;
  cleanUpData();
  if (await validate()) {
    try {
      const payload = { ...currNetwork.value };
      if (typeof payload.id === 'string' && payload.id.startsWith('new-')) {
        delete payload.id;
      }
      if (!payload.id && typeof crypto !== 'undefined' && crypto.randomUUID) {
        payload.id = crypto.randomUUID();
      }

      const created = await networkStore.addNetwork(payload);
      if (created?.id) {
        currNetwork.value.id = created.id;
      } else if (payload.id) {
        currNetwork.value.id = payload.id;
      }

      emit('savedNewNetwork', currNetwork.value.id, created || payload);
      alreadyPublished.value = !!(currNetwork.value.id && currNetwork.value.published);
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
};

const addOrg = async () => {
  await updateOrganisations();
  
  if (!currNetwork.value.organisations) {
    currNetwork.value.organisations = [];
  }

  const isFirstOrganisation = currNetwork.value.organisations.length === 0;
  const newOrganisation = {
    organisationTkCode: '',
    isCoordinator: isFirstOrganisation,
    validityInNetwork: !isFirstOrganisation ? currNetwork.value.organisations[currNetwork.value.organisations.length - 1].validityInNetwork : {
      continuity: '',
      start: '',
      end: ''
    }
  };

  currNetwork.value.organisations.push(newOrganisation);
};

const deleteOrg = index => {
  if (currNetwork.value.organisations && currNetwork.value.organisations.length > 0) {
    currNetwork.value.organisations.splice(index, 1);
    filterOrganisationList();
  }
};

const addTargetGroup = () => {
  if (!currNetwork.value.targetGroups) {
    currNetwork.value.targetGroups = [];
  }
  currNetwork.value.targetGroups.push({ key: '', codeSetKey: studyRightTypeCodeSet });
};

const deleteTargetGroup = index => {
  if (currNetwork.value.targetGroups && currNetwork.value.targetGroups.length > 0) {
    currNetwork.value.targetGroups.splice(index, 1);
  }
};

const addRestriction = () => {
  if (!currNetwork.value.restrictions) {
    currNetwork.value.restrictions = [];
  }
  currNetwork.value.restrictions.push({ key: '', codeSetKey: studyRightTypeCodeSet });
};

const deleteRestriction = index => {
  if (currNetwork.value.restrictions || currNetwork.value.restrictions.length > 0) {
    currNetwork.value.restrictions.splice(index, 1);
  }
};

const filterOrganisationList = () => {
  _.forEach(organisationMenuItems.value, menuItem => {
    menuItem.disabled = false;
  });

  _.forEach(currNetwork.value.organisations, org => {
    let menuItem = _.find(
      organisationMenuItems.value,
      menuOrg => menuOrg.value === org.organisationTkCode
    );

    if (menuItem) {
      menuItem.disabled = true;
    }
  });
};

const validate = async () => {
  if (!form.value) {
    return false;
  }
  const validationResult = await form.value.validate();
  const isValid =
    typeof validationResult === 'boolean'
      ? validationResult
      : validationResult?.valid ?? false;

  if (!currNetwork.value?.organisations || currNetwork.value.organisations.length < 2) {
    errorOrganisations.value = true;
    setTimeout(() => {
      errorOrganisations.value = false;
    }, 4000);
    return false;
  }

  if (isValid) {
    errorOrganisations.value = false;
    return true;
  } else {
    errorEmptyFields.value = true;
    setTimeout(() => {
      errorEmptyFields.value = false;
    }, 4000);
    return false;
  }
};

const validityContinuityChanged = (item, validity) => {
  if (item === continuityIndetifitely) {
    validity.end = '';
  }
};

const payChanged = pay => {
  if (!pay) {
    currNetwork.value.expenses.pricingBasis = null;
    currNetwork.value.expenses.price = null;
    resetValidation();
  }
};

const copyNetworkId = async () => {
  if (!currNetwork.value?.id) return;
  try {
    await navigator.clipboard.writeText(String(currNetwork.value.id));
    copyNotice.value = true;
  } catch (err) {
    console.log('Unable to copy network id', err);
  }
};

const localizeTexts = () => {
  networkTypes.value = [
    { text: t('curriculumLevel'), value: 'CURRICULUM_LEVEL' },
    { text: t('freedomOfChoice'), value: 'FREEDOM_OF_CHOICE' }
  ];

  continuity.value = [
    { text: t('indefinitely'), value: continuityIndetifitely },
    { text: t('fixed'), value: continuityFixed }
  ];

  pricingBasis.value = [
    { text: t('euroPerCredit'), value: 'EURO_PER_CREDIT' },
    { text: t('otherPricingBasis'), value: 'OTHER_PRICING_BASIS' }
  ];
};

const updateOrganisations = async () => {
  try {
    organisations.value = await organisationStore.fetchOrganisations();
  } catch (err) {
    console.log('Unable to get organisations.', err);
  }

  organisationMenuItems.value = [];
  _.forEach(organisations.value, org => {
    let name = languageUtils.resolveText(props.locale, org.organisationName.values);

    organisationMenuItems.value.push({
      text: `${name} (${org.id})`,
      value: org.id
    });
  });

  organisationMenuItems.value = _.sortBy(organisationMenuItems.value, 'text');
};

watch(
  () => props.modelValue,
  val => {
    updatingFromParent.value = true;
    if (!val) {
      currNetwork.value = null;
      lastLoadedId.value = null;
      updatingFromParent.value = false;
      return;
    }
    currNetwork.value = { ...emptyNetwork, ...val };
    if (!currNetwork.value.name) {
      currNetwork.value.name = { values: { fi: '', sv: '', en: '' } };
    }
    if (currNetwork.value.published == null) {
      currNetwork.value.published = false;
    }
    if (currNetwork.value.furtherInformation == null) {
      currNetwork.value.furtherInformation = emptyNetwork.furtherInformation;
    }
    if (currNetwork.value.description == null) {
      currNetwork.value.description = emptyNetwork.description;
    }
    if (currNetwork.value.validity == null) {
      currNetwork.value.validity = emptyNetwork.validity;
    }
    if (currNetwork.value.expenses == null) {
      currNetwork.value.expenses = emptyNetwork.expenses;
    }
    if (currNetwork.value.id !== lastLoadedId.value) {
      alreadyPublished.value = !!(currNetwork.value.id && currNetwork.value.published);
      lastLoadedId.value = currNetwork.value.id;
    }
    resetValidation();
    filterOrganisationList();
    nextTick(() => {
      updatingFromParent.value = false;
    });
  },
  { immediate: true }
);

watch(
  currNetwork,
  val => {
    if (updatingFromParent.value) return;
    emit('update:modelValue', _.cloneDeep(val));
  },
  { deep: true }
);

watch(
  () => props.locale,
  () => {
    localizeTexts();
  }
);

onMounted(async () => {
  await updateOrganisations();
  try {
    let studyRightTypeCodesResponse = await codesetStore.fetchCodesetValues(
      studyRightTypeCodeSet
    );
    studyRightTypeCodeKeyValues.value = studyRightTypeCodesResponse.map(code => ({
      text: code.key,
      value: code.key
    }));
  } catch (err) {
    console.log(`Unable to get codeset '${studyRightTypeCodeSet}' values`);
  }
  localizeTexts();
});
</script>

<style lang="css">
form {
  margin: 10px;
}
.organisation {
  border: 2px solid #d4d4d4;
  padding: 5px !important;
  margin-bottom: 5px !important;
}
.org-actions .v-switch {
  margin: 0;
}
.org-actions .v-btn {
  align-self: flex-start;
  margin-top: 2px;
}
.copy-icon {
  cursor: pointer;
}
#form-base {
  display: block !important;
}
#network-form {
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
