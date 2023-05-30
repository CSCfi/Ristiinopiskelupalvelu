<template>
  <div>
    <v-text-field v-model="fi" :label="$t('fi')" :rules="rules"></v-text-field>
    <v-text-field v-model="en" :label="$t('en')" :rules="rules"></v-text-field>
    <v-text-field v-model="sv" :label="$t('sv')" :rules="rules"></v-text-field>
  </div>
</template>

<script>
import _ from 'lodash';
export default {
  props: {
    value: Object,
    rules: {
      default() {
        return [];
      },
      type: Array
    }
  },
  computed: {
    fi: {
      get: function() {
        return this.value?.values?.fi;
      },
      set: function(newValue) {
        let localisedObject = this.getUpdatedValue(newValue, this.fi_key);
        this.$emit('input', localisedObject);
      }
    },
    en: {
      get: function() {
        return this.value?.values?.en;
      },
      set: function(newValue) {
        let localisedObject = this.getUpdatedValue(newValue, this.en_key);
        this.$emit('input', localisedObject);
      }
    },
    sv: {
      get: function() {
        return this.value?.values?.sv;
      },
      set: function(newValue) {
        let localisedObject = this.getUpdatedValue(newValue, this.sv_key);
        this.$emit('input', localisedObject);
      }
    }
  },
  methods: {
    getUpdatedValue(newValue, lang) {
      let localisedObject = {
        values: {
          [lang]: !newValue ? null : newValue
        }
      };

      _.filter(this.languages, langKey => langKey !== lang).forEach(langKey => {
        localisedObject.values[langKey] = !this.value?.values?.[langKey]
          ? null
          : this.value.values[langKey];
      });

      if (!newValue) {
        if (this.isEmptyObject(localisedObject.values)) {
          return null;
        }
      }
      return localisedObject;
    },
    isEmptyObject(values) {
      return _.keys(values).every(lang => {
        return !values[lang];
      });
    }
  },
  data() {
    return {
      fi_key: 'fi',
      en_key: 'en',
      sv_key: 'sv',
      languages: []
    };
  },
  created() {
    this.languages = [this.fi_key, this.en_key, this.sv_key];
  }
};
</script>
