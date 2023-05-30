<template>
  <div>
    <v-toolbar>
      <v-toolbar-title>{{ $t('editor') }}</v-toolbar-title>
    </v-toolbar>
    <div ref="jsoneditor" style="height: 650px;"></div>
    <v-btn
      v-if="superuser === true"
      @click="update"
      small
      color="success"
      :disabled="activeItem == '' ? true : false"
    >
      {{ $t('save') }}
    </v-btn>
    <v-alert :value="success" type="success">
      {{ $t('codesetUpdated') }}
    </v-alert>
    <v-alert :value="error" type="error">
      {{ $t('codesetUpdateError') }}
    </v-alert>
  </div>
</template>

<script>
import JSONEditor from 'jsoneditor/dist/jsoneditor.min';
import axios from 'axios';
const modes = ['tree', 'view', 'form', 'code', 'text'];
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
    }
  },
  data() {
    return {
      jsoneditor: null,
      success: false,
      error: false,
      dialog: false
    };
  },
  watch: {
    value(value) {
      this.jsoneditor.set(value);
    }
  },
  methods: {
    getOptions() {
      const defaults = {
        escapeUnicode: false,
        history: true,
        indentation: 2,
        mode: this.superuser ? modes[2] : 'view',
        modes: this.superuser ? modes.slice(0) : ['view'],
        navigationBar: false,
        search: true,
        statusBar: false,
        sortObjectKeys: false
      };

      return Object.assign({}, defaults, this.options);
    },
    update() {
      let formData = new FormData();
      formData.append('file', JSON.stringify(this.jsoneditor.get()));
      axios
        .post('/admin-ui/api/codes/update', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        .then(() => {
          this.success = true;
          this.$emit('saved');
          setTimeout(() => {
            this.success = false;
          }, 2000);
        })
        .catch(() => {
          this.error = true;
          setTimeout(() => {
            this.error = false;
          }, 2000);
        });
    }
  },
  mounted() {
    this.jsoneditor = new JSONEditor(this.$refs.jsoneditor, this.getOptions(), this.value);
  },
  beforeDestroy() {
    if (this.jsoneditor) {
      this.jsoneditor.destroy();
      this.jsoneditor = null;
    }
  }
};
</script>
