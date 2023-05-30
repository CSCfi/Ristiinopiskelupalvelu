<template>
  <div>
    <v-text-field v-model="nestedObjectValue" :label="$t(this.label)" :rules="[]"></v-text-field>
  </div>
</template>

<script>
import _ from 'lodash';
export default {
  props: {
    field: String,
    value: Object,
    label: String
  },
  computed: {
    nestedObjectValue: {
      get: function() {
        return this.value?.[this.field];
      },
      set: function(newValue) {
        let newValueToEmit = this.value;
        if (!newValueToEmit) {
          newValueToEmit = {};
        }
        newValueToEmit[this.field] = newValue;

        if (!newValue) {
          if (this.isEmptyObject(this.value)) {
            newValueToEmit = null;
          }
        }
        this.$emit('input', newValueToEmit);
      }
    }
  },
  methods: {
    isEmptyObject(values) {
      return _.keys(values).every(key => {
        return !values[key];
      });
    }
  }
};
</script>
