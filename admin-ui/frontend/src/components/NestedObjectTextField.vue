<template>
  <div>
    <v-text-field v-model="nestedObjectValue" :label="$t(label)" :rules="[]"></v-text-field>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import _ from 'lodash';

const props = defineProps({
  field: String,
  modelValue: Object,
  label: String
});

const emit = defineEmits(['update:modelValue']);

const isEmptyObject = values =>
  _.keys(values).every(key => {
    return !values[key];
  });

const nestedObjectValue = computed({
  get() {
    return props.modelValue?.[props.field];
  },
  set(newValue) {
    const current = props.modelValue ? { ...props.modelValue } : {};
    current[props.field] = newValue;

    let newValueToEmit = current;
    if (!newValue && isEmptyObject(current)) {
      newValueToEmit = null;
    }
    emit('update:modelValue', newValueToEmit);
  }
});
</script>
