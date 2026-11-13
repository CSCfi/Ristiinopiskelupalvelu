<template>
  <div>
    <v-text-field v-model="fi" :label="$t('fi')" :rules="rules"></v-text-field>
    <v-text-field v-model="en" :label="$t('en')" :rules="rules"></v-text-field>
    <v-text-field v-model="sv" :label="$t('sv')" :rules="rules"></v-text-field>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import _ from 'lodash';

const props = defineProps({
  modelValue: Object,
  rules: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['update:modelValue']);

const languages = ['fi', 'en', 'sv'];

const getUpdatedValue = (newValue, lang) => {
  const localisedObject = {
    values: {
      ...(props.modelValue?.values || {}),
      [lang]: newValue || null
    }
  };
  return localisedObject;
};

const fi = computed({
  get() {
    return props.modelValue?.values?.fi;
  },
  set(newValue) {
    emit('update:modelValue', getUpdatedValue(newValue, 'fi'));
  }
});

const en = computed({
  get() {
    return props.modelValue?.values?.en;
  },
  set(newValue) {
    emit('update:modelValue', getUpdatedValue(newValue, 'en'));
  }
});

const sv = computed({
  get() {
    return props.modelValue?.values?.sv;
  },
  set(newValue) {
    emit('update:modelValue', getUpdatedValue(newValue, 'sv'));
  }
});
</script>
