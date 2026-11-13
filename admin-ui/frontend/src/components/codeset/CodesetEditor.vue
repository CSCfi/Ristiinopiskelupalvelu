<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>{{ $t('editor') }}</v-toolbar-title>
    </v-toolbar>

    <v-form v-if="localValue" id="code-form" ref="codeForm" :lazy-validation="false">
      <v-text-field v-model="localValue.id" :label="$t('codeIdentifier')" disabled></v-text-field>
      <v-text-field v-model="localValue.codeUri" :label="$t('codeUri')"></v-text-field>
      <v-text-field v-model="localValue.resourceUri" :label="$t('codeResourceUri')"></v-text-field>
      <v-text-field v-model="localValue.key" :label="$t('codeKey')"></v-text-field>

      <v-text-field
        v-model="localValue.codeVersion"
        type="number"
        :label="$t('codeVersion')"
      ></v-text-field>

      <v-text-field v-model="localValue.updateDate" :label="$t('updateDate')" disabled></v-text-field>

      <v-menu
        v-model="startDateMenu"
        :close-on-content-click="false"
        :nudge-right="40"
        transition="scale-transition"
        offset-y
        full-width
        min-width="290px"
      >
        <template #activator="{ props }">
          <v-text-field
            v-model="localValue.validityStartDate"
            :label="$t('startDate')"
            prepend-icon="mdi-calendar"
            readonly
            :rules="[requiredRule]"
            v-bind="props"
          ></v-text-field>
        </template>
        <v-date-picker :locale="locale" v-model="localValue.validityStartDate" />
      </v-menu>

      <v-menu
        v-model="endDateMenu"
        :close-on-content-click="false"
        :nudge-right="40"
        transition="scale-transition"
        offset-y
        full-width
        min-width="290px"
      >
        <template #activator="{ props }">
          <v-text-field
            v-model="localValue.validityEndDate"
            :label="$t('endDate')"
            prepend-icon="mdi-calendar"
            readonly
            :rules="[requiredRule]"
            v-bind="props"
          ></v-text-field>
        </template>
        <v-date-picker :locale="locale" v-model="localValue.validityEndDate" />
      </v-menu>

      <v-text-field v-model="localValue.status" :label="$t('codeStatus')"></v-text-field>

      <div class="d-flex align-center justify-space-between mb-3">
        <h2 class="mb-0">{{ $t('codeValues') }}</h2>
        <v-btn class="btn-right" variant="text" @click="addCodeValue">
          {{ $t('addCodeValue') }}
        </v-btn>
      </div>

      <div v-if="localValue.codeValues && localValue.codeValues.length > 0">
        <div
          class="code-value-row"
          v-for="(codeValue, index) in localValue.codeValues"
          :key="index"
        >
          <v-select
            v-model="codeValue.language"
            :items="['fi', 'sv', 'en']"
            :label="$t('codeValueLanguage')"
            :rules="[requiredRule]"
            class="mb-3 full-width"
          ></v-select>

          <v-text-field
            v-model="codeValue.value"
            :label="$t('codeValueValue')"
            :rules="[requiredRule]"
            class="mb-3 full-width"
          ></v-text-field>

          <v-text-field
            v-model="codeValue.description"
            :label="$t('codeValueDescription')"
            class="mb-3 full-width"
          ></v-text-field>

          <div class="d-flex justify-end">
            <v-btn color="error" variant="text" @click="deleteCodeValue(index)">
              {{ $t('delete') }}
            </v-btn>
          </div>
        </div>
      </div>
      <div v-else class="mt-2 mb-4">
        <em>{{ $t('noValues') }}</em>
        <v-divider class="mt-2"></v-divider>
      </div>
    </v-form>
    <div class="d-flex justify-end mt-4 mb-4 pr-3">
      <v-btn
        v-if="superuser === true"
        @click="update"
        size="small"
        color="success"
        :disabled="activeItem == '' ? true : false"
      >
        {{ $t('save') }}
      </v-btn>
    </div>
    <v-alert v-if="success" type="success" class="mt-3">
      {{ $t('codesetUpdated') }}
    </v-alert>
    <v-alert v-if="error" type="error" class="mt-3">
      {{ $t('codesetUpdateError') }}
    </v-alert>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import cloneDeep from 'lodash/cloneDeep';
import { useCodesetStore } from '../../stores/codesets';

const props = defineProps({
  options: {
    type: Object,
    default: () => ({})
  },
  modelValue: {
    type: Object,
    default: () => ({})
  },
  activeItem: {
    type: [String, Number, Boolean],
    default: ''
  },
  admin: {
    type: Boolean,
    default: false
  },
  superuser: {
    type: Boolean,
    default: false
  },
  locale: {
    type: String,
    default: 'fi'
  }
});

const emit = defineEmits(['saved', 'update:modelValue']);
const { t } = useI18n();
const codesetStore = useCodesetStore();

const codeForm = ref(null);
const success = ref(false);
const error = ref(false);
const startDateMenu = ref(false);
const endDateMenu = ref(false);

const normalizeValue = val => {
  const merged = { ...(cloneDeep(val) || {}) };
  merged.codeValues = Array.isArray(merged.codeValues) ? [...merged.codeValues] : [];
  return merged;
};

const localValue = ref(normalizeValue(props.modelValue));

watch(
  () => props.modelValue,
  newVal => {
    localValue.value = normalizeValue(newVal);
  },
  { deep: true }
);

const requiredRule = value => !!value || t('requiredField');

const update = async () => {
  if (!codeForm.value) {
    return;
  }
  const validationResult = await codeForm.value.validate();
  const isValid =
    typeof validationResult === 'boolean'
      ? validationResult
      : validationResult?.valid ?? false;

  if (!isValid) {
    return;
  }

  codesetStore
    .updateCode(localValue.value)
    .then(() => {
      success.value = true;
      emit('update:modelValue', cloneDeep(localValue.value));
      emit('saved');
      setTimeout(() => {
        success.value = false;
      }, 5000);
    })
    .catch(() => {
      error.value = true;
      setTimeout(() => {
        error.value = false;
      }, 5000);
    });
};

const addCodeValue = () => {
  if (!localValue.value.codeValues) {
    localValue.value.codeValues = [];
  }
  localValue.value.codeValues.push({ language: '', value: '', description: '' });
};

const deleteCodeValue = index => {
  if (localValue.value.codeValues && localValue.value.codeValues.length > 0) {
    localValue.value.codeValues.splice(index, 1);
  }
  emit('update:modelValue', cloneDeep(localValue.value));
};
</script>

<style scoped>
.code-value-row {
  max-width: 640px;
}

.code-value-row:not(:last-child) {
  margin-bottom: 16px;
}

.full-width {
  width: 100%;
}
.btn-right {
  width: auto;
}
</style>
