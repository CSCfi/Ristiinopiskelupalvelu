<template>
  <div>
    <v-select
      v-model="nestedObjectValue"
      :items="items"
      filled
      :rules="[]"
      :label="$t(this.label)"
    ></v-select>
  </div>
</template>

<script>
import _ from 'lodash';
export default {
  props: {
    field: String,
    value: Object,
    label: String,
    items: Array
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
